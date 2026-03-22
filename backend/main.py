from fastapi import FastAPI, HTTPException, Depends
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from sqlalchemy import create_engine, Column, Integer, String, Text, DateTime
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker, Session
from datetime import datetime
from groq import Groq
from dotenv import load_dotenv
import httpx
import json
import os

load_dotenv()

# ── App Setup ──────────────────────────────────────────────────────────────────
app = FastAPI(title="Code Review API", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# ── Database ───────────────────────────────────────────────────────────────────
DATABASE_URL = os.getenv("DATABASE_URL", "sqlite:///./reviews.db")
engine = create_engine(DATABASE_URL, connect_args={"check_same_thread": False})
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()

class ReviewRecord(Base):
    __tablename__ = "reviews"
    id         = Column(Integer, primary_key=True, index=True)
    language   = Column(String(50))
    code       = Column(Text)
    feedback   = Column(Text)
    score      = Column(Integer)
    created_at = Column(DateTime, default=datetime.utcnow)

Base.metadata.create_all(bind=engine)

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

# ── Schemas ────────────────────────────────────────────────────────────────────
class ReviewRequest(BaseModel):
    code: str
    language: str = "auto-detect"

class ReviewResponse(BaseModel):
    id: int
    language: str
    summary: str
    issues: list[str]
    suggestions: list[str]
    score: int          # 1–10
    created_at: str

class HistoryItem(BaseModel):
    id: int
    language: str
    summary: str
    score: int
    created_at: str

# ── AI Client ──────────────────────────────────────────────────────────────────
client = Groq(
    api_key=os.getenv("GROQ_API_KEY"),
    http_client=httpx.Client(verify=False)
)

SYSTEM_PROMPT = """You are an expert code reviewer. Analyse the submitted code and respond
with a JSON object ONLY (no markdown) with this exact structure:
{
  "language": "<detected language>",
  "summary": "<2-sentence overall summary>",
  "issues": ["<issue 1>", "<issue 2>", ...],
  "suggestions": ["<suggestion 1>", "<suggestion 2>", ...],
  "score": <integer 1-10>
}"""

# ── Routes ─────────────────────────────────────────────────────────────────────
@app.post("/review", response_model=ReviewResponse)
async def review_code(req: ReviewRequest, db: Session = Depends(get_db)):
    MAX_CODE_LENGTH = 20_000
    if not req.code.strip():
        raise HTTPException(status_code=400, detail="Code cannot be empty")
    if len(req.code) > MAX_CODE_LENGTH:
        raise HTTPException(status_code=400, detail=f"Code exceeds maximum length of {MAX_CODE_LENGTH} characters")

    # Call Groq
    try:
        completion = client.chat.completions.create(
            model="llama-3.3-70b-versatile",
            response_format={"type": "json_object"},
            messages=[
                {"role": "system", "content": SYSTEM_PROMPT},
                {"role": "user",   "content": f"Language hint: {req.language}\n\n{req.code}"}
            ]
        )
        result = json.loads(completion.choices[0].message.content)
    except Exception as e:  # noqa: BLE001
        raise HTTPException(status_code=502, detail=f"AI service error: {str(e)}")

    # Persist to DB
    record = ReviewRecord(
        language=result.get("language", req.language),
        code=req.code,
        feedback=result.get("summary", ""),
        score=result.get("score", 5)
    )
    db.add(record)
    db.commit()
    db.refresh(record)

    return ReviewResponse(
        id=record.id,
        language=record.language,
        summary=result.get("summary", ""),
        issues=result.get("issues", []),
        suggestions=result.get("suggestions", []),
        score=record.score,
        created_at=record.created_at.isoformat()
    )


@app.get("/history", response_model=list[HistoryItem])
def get_history(limit: int = 20, db: Session = Depends(get_db)):
    rows = db.query(ReviewRecord).order_by(ReviewRecord.created_at.desc()).limit(limit).all()
    return [
        HistoryItem(
            id=r.id,
            language=r.language,
            summary=r.feedback,
            score=r.score,
            created_at=r.created_at.isoformat()
        ) for r in rows
    ]


@app.get("/history/{review_id}", response_model=ReviewResponse)
def get_review(review_id: int, db: Session = Depends(get_db)):
    row = db.query(ReviewRecord).filter(ReviewRecord.id == review_id).first()
    if not row:
        raise HTTPException(status_code=404, detail="Review not found")
    return ReviewResponse(
        id=row.id,
        language=row.language,
        summary=row.feedback,
        issues=[],
        suggestions=[],
        score=row.score,
        created_at=row.created_at.isoformat()
    )


@app.get("/health")
def health():
    return {"status": "ok"}

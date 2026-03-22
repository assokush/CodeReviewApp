import json
from unittest.mock import MagicMock, patch

import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

from main import app, Base, get_db

# ── Test database (in-memory SQLite) ───────────────────────────────────────────
TEST_DB_URL = "sqlite:///./test_reviews.db"
test_engine = create_engine(TEST_DB_URL, connect_args={"check_same_thread": False})
TestSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=test_engine)


def override_get_db():
    db = TestSessionLocal()
    try:
        yield db
    finally:
        db.close()


@pytest.fixture(autouse=True)
def reset_db():
    Base.metadata.create_all(bind=test_engine)
    app.dependency_overrides[get_db] = override_get_db
    yield
    Base.metadata.drop_all(bind=test_engine)
    app.dependency_overrides.clear()


client = TestClient(app)

MOCK_AI_RESPONSE = {
    "language": "python",
    "summary": "Well-structured code with minor issues.",
    "issues": ["Missing type hints"],
    "suggestions": ["Add docstrings"],
    "score": 7,
}


# ── Health ──────────────────────────────────────────────────────────────────────

def test_health():
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "ok"}


# ── POST /review ────────────────────────────────────────────────────────────────

def test_review_empty_code_rejected():
    response = client.post("/review", json={"code": "   ", "language": "python"})
    assert response.status_code == 400
    assert "empty" in response.json()["detail"].lower()


def test_review_blank_string_rejected():
    response = client.post("/review", json={"code": "", "language": "python"})
    assert response.status_code == 400


def test_review_code_too_long_rejected():
    response = client.post("/review", json={"code": "x" * 20_001, "language": "python"})
    assert response.status_code == 400
    assert "exceeds" in response.json()["detail"].lower()


def test_review_exactly_at_limit_allowed():
    """Code exactly at the 20 000 character limit should not be rejected for length."""
    with patch("main.client") as mock_groq:
        mock_choice = MagicMock()
        mock_choice.message.content = json.dumps(MOCK_AI_RESPONSE)
        mock_groq.chat.completions.create.return_value = MagicMock(choices=[mock_choice])
        response = client.post("/review", json={"code": "x" * 20_000, "language": "python"})
    assert response.status_code == 200


@patch("main.client")
def test_review_success_returns_correct_fields(mock_groq):
    mock_choice = MagicMock()
    mock_choice.message.content = json.dumps(MOCK_AI_RESPONSE)
    mock_groq.chat.completions.create.return_value = MagicMock(choices=[mock_choice])

    response = client.post("/review", json={"code": "print('hello')", "language": "python"})

    assert response.status_code == 200
    data = response.json()
    assert data["language"] == "python"
    assert data["score"] == 7
    assert data["summary"] == MOCK_AI_RESPONSE["summary"]
    assert data["issues"] == MOCK_AI_RESPONSE["issues"]
    assert data["suggestions"] == MOCK_AI_RESPONSE["suggestions"]
    assert "id" in data
    assert "created_at" in data


@patch("main.client")
def test_review_persists_to_db(mock_groq):
    mock_choice = MagicMock()
    mock_choice.message.content = json.dumps(MOCK_AI_RESPONSE)
    mock_groq.chat.completions.create.return_value = MagicMock(choices=[mock_choice])

    client.post("/review", json={"code": "print('hello')", "language": "python"})

    history = client.get("/history").json()
    assert len(history) == 1


@patch("main.client")
def test_review_ai_error_returns_502(mock_groq):
    mock_groq.chat.completions.create.side_effect = Exception("AI service unavailable")

    response = client.post("/review", json={"code": "print('hello')", "language": "python"})
    assert response.status_code == 502
    assert "AI service error" in response.json()["detail"]


# ── GET /history ────────────────────────────────────────────────────────────────

def test_get_history_empty():
    response = client.get("/history")
    assert response.status_code == 200
    assert response.json() == []


@patch("main.client")
def test_get_history_returns_items(mock_groq):
    mock_choice = MagicMock()
    mock_choice.message.content = json.dumps(MOCK_AI_RESPONSE)
    mock_groq.chat.completions.create.return_value = MagicMock(choices=[mock_choice])

    client.post("/review", json={"code": "print('hello')", "language": "python"})
    client.post("/review", json={"code": "print('world')", "language": "python"})

    response = client.get("/history")
    assert response.status_code == 200
    assert len(response.json()) == 2


@patch("main.client")
def test_get_history_respects_limit(mock_groq):
    mock_choice = MagicMock()
    mock_choice.message.content = json.dumps(MOCK_AI_RESPONSE)
    mock_groq.chat.completions.create.return_value = MagicMock(choices=[mock_choice])

    for i in range(5):
        client.post("/review", json={"code": f"code_{i}", "language": "python"})

    response = client.get("/history?limit=3")
    assert response.status_code == 200
    assert len(response.json()) == 3


@patch("main.client")
def test_get_history_ordered_newest_first(mock_groq):
    mock_choice = MagicMock()
    mock_choice.message.content = json.dumps(MOCK_AI_RESPONSE)
    mock_groq.chat.completions.create.return_value = MagicMock(choices=[mock_choice])

    client.post("/review", json={"code": "first", "language": "python"})
    client.post("/review", json={"code": "second", "language": "python"})

    history = client.get("/history").json()
    assert history[0]["id"] > history[1]["id"]


# ── GET /history/{id} ───────────────────────────────────────────────────────────

@patch("main.client")
def test_get_review_by_id_success(mock_groq):
    mock_choice = MagicMock()
    mock_choice.message.content = json.dumps(MOCK_AI_RESPONSE)
    mock_groq.chat.completions.create.return_value = MagicMock(choices=[mock_choice])

    post_response = client.post("/review", json={"code": "print('hello')", "language": "python"})
    review_id = post_response.json()["id"]

    get_response = client.get(f"/history/{review_id}")
    assert get_response.status_code == 200
    data = get_response.json()
    assert data["id"] == review_id
    assert data["language"] == "python"
    assert data["score"] == 7


def test_get_review_not_found():
    response = client.get("/history/99999")
    assert response.status_code == 404
    assert response.json()["detail"] == "Review not found"

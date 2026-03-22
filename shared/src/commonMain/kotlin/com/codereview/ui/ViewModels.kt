package com.codereview.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codereview.models.*
import com.codereview.repository.CodeReviewRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// ── Review Screen ──────────────────────────────────────────────────────────────

data class ReviewUiState(
    val code: String = "",
    val language: String = "auto-detect",
    val result: ReviewResponse? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class ReviewViewModel(
    private val repository: CodeReviewRepository = CodeReviewRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState

    fun onCodeChanged(code: String) {
        _uiState.value = _uiState.value.copy(code = code, error = null)
    }

    fun onLanguageChanged(language: String) {
        _uiState.value = _uiState.value.copy(language = language)
    }

    fun submitReview() {
        val state = _uiState.value
        _uiState.value = state.copy(isLoading = true, error = null, result = null)

        viewModelScope.launch {
            when (val result = repository.submitReview(state.code, state.language)) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    result = result.data
                )
                is ApiResult.Error   -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.message
                )
                is ApiResult.Loading -> Unit
            }
        }
    }

    fun clearResult() {
        _uiState.value = ReviewUiState()
    }
}

// ── Detail Screen ──────────────────────────────────────────────────────────────

data class DetailUiState(
    val review: ReviewResponse? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class DetailViewModel(
    private val repository: CodeReviewRepository = CodeReviewRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState

    fun load(id: Int) {
        _uiState.value = DetailUiState(isLoading = true)
        viewModelScope.launch {
            when (val result = repository.getReviewDetail(id)) {
                is ApiResult.Success -> _uiState.value = DetailUiState(review = result.data)
                is ApiResult.Error   -> _uiState.value = DetailUiState(error = result.message)
                is ApiResult.Loading -> Unit
            }
        }
    }
}

// ── History Screen ─────────────────────────────────────────────────────────────

data class HistoryUiState(
    val items: List<HistoryItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class HistoryViewModel(
    private val repository: CodeReviewRepository = CodeReviewRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState

    init { loadHistory() }

    fun loadHistory() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            when (val result = repository.loadHistory()) {
                is ApiResult.Success -> _uiState.value = HistoryUiState(items = result.data)
                is ApiResult.Error   -> _uiState.value = HistoryUiState(error = result.message)
                is ApiResult.Loading -> Unit
            }
        }
    }
}

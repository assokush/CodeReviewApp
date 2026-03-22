package com.codereview.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── ReviewViewModel ─────────────────────────────────────────────────────────

    @Test
    fun reviewViewModel_initialState() {
        val vm = ReviewViewModel()
        val state = vm.uiState.value
        assertEquals("", state.code)
        assertEquals("auto-detect", state.language)
        assertNull(state.result)
        assertNull(state.error)
        assertFalse(state.isLoading)
    }

    @Test
    fun reviewViewModel_onCodeChanged_updatesCode() {
        val vm = ReviewViewModel()
        vm.onCodeChanged("fun main() {}")
        assertEquals("fun main() {}", vm.uiState.value.code)
    }

    @Test
    fun reviewViewModel_onCodeChanged_clearsError() {
        val vm = ReviewViewModel()
        // Change code twice — error stays null (it's set only after a failed submit)
        vm.onCodeChanged("code v1")
        vm.onCodeChanged("code v2")
        assertNull(vm.uiState.value.error)
        assertEquals("code v2", vm.uiState.value.code)
    }

    @Test
    fun reviewViewModel_onLanguageChanged_updatesLanguage() {
        val vm = ReviewViewModel()
        vm.onLanguageChanged("kotlin")
        assertEquals("kotlin", vm.uiState.value.language)
    }

    @Test
    fun reviewViewModel_onLanguageChanged_doesNotClearCode() {
        val vm = ReviewViewModel()
        vm.onCodeChanged("some code")
        vm.onLanguageChanged("python")
        assertEquals("some code", vm.uiState.value.code)
        assertEquals("python", vm.uiState.value.language)
    }

    @Test
    fun reviewViewModel_clearResult_resetsToInitialState() {
        val vm = ReviewViewModel()
        vm.onCodeChanged("some code")
        vm.onLanguageChanged("python")
        vm.clearResult()
        val state = vm.uiState.value
        assertEquals("", state.code)
        assertEquals("auto-detect", state.language)
        assertNull(state.result)
        assertNull(state.error)
        assertFalse(state.isLoading)
    }

    // ── DetailViewModel ─────────────────────────────────────────────────────────

    @Test
    fun detailViewModel_initialState() {
        val vm = DetailViewModel()
        val state = vm.uiState.value
        assertNull(state.review)
        assertNull(state.error)
        assertFalse(state.isLoading)
    }

    // ── ReviewUiState ───────────────────────────────────────────────────────────

    @Test
    fun reviewUiState_copy_preservesUnchangedFields() {
        val original = ReviewUiState(code = "hello", language = "kotlin")
        val updated = original.copy(code = "world")
        assertEquals("world", updated.code)
        assertEquals("kotlin", updated.language)
    }

    @Test
    fun reviewUiState_isLoading_false_by_default() {
        val state = ReviewUiState()
        assertFalse(state.isLoading)
    }
}

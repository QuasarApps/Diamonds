package com.example.diamonds.ui.base

import com.example.diamonds.common.util.ConnectivityState
import com.example.diamonds.data.connectivity.ConnectivityObserver
import com.example.diamonds.ui.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Concrete subclass used only for testing [BaseViewModel]. */
private data class TestUiState(val value: Int = 0, val error: String? = null)

private class TestViewModel(connectivityObserver: ConnectivityObserver) :
    BaseViewModel<TestUiState>(connectivityObserver, TestUiState()) {

    fun increment() = updateState { it.copy(value = it.value + 1) }
    fun triggerError(msg: String) = updateState { it.copy(error = msg) }
}

@OptIn(ExperimentalCoroutinesApi::class)
class BaseViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var connectivityObserver: ConnectivityObserver
    private val connectivityFlow = MutableStateFlow(ConnectivityState.ONLINE)

    @Before
    fun setUp() {
        connectivityObserver = mockk {
            every { observeConnectivityState() } returns connectivityFlow
            every { isOnline() } returns true
        }
    }

    @Test
    fun `initial uiState matches provided default`() {
        val vm = TestViewModel(connectivityObserver)
        assertEquals(0, vm.uiState.value.value)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `updateState applies transformation correctly`() {
        val vm = TestViewModel(connectivityObserver)
        vm.increment()
        assertEquals(1, vm.uiState.value.value)
    }

    @Test
    fun `updateState accumulates multiple calls`() {
        val vm = TestViewModel(connectivityObserver)
        vm.increment()
        vm.increment()
        vm.increment()
        assertEquals(3, vm.uiState.value.value)
    }

    @Test
    fun `isOnline starts as true when connectivity ONLINE`() = runTest {
        val vm = TestViewModel(connectivityObserver)
        advanceUntilIdle()
        assertTrue(vm.isOnline.value)
    }

    @Test
    fun `isOnline reflects OFFLINE connectivity state`() = runTest {
        val vm = TestViewModel(connectivityObserver)
        advanceUntilIdle()

        connectivityFlow.value = ConnectivityState.OFFLINE
        advanceUntilIdle()

        assertFalse(vm.isOnline.value)
    }

    @Test
    fun `isOnline reflects transition back to ONLINE`() = runTest {
        val vm = TestViewModel(connectivityObserver)
        connectivityFlow.value = ConnectivityState.OFFLINE
        advanceUntilIdle()
        assertFalse(vm.isOnline.value)

        connectivityFlow.value = ConnectivityState.ONLINE
        advanceUntilIdle()
        assertTrue(vm.isOnline.value)
    }

    @Test
    fun `triggerError stores error message in state`() {
        val vm = TestViewModel(connectivityObserver)
        vm.triggerError("Something went wrong")
        assertEquals("Something went wrong", vm.uiState.value.error)
    }
}

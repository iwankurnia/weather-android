package com.iwankurnia.weather.ui.main

import com.iwankurnia.weather.data.LoadResult
import com.iwankurnia.weather.data.WeatherRepository
import com.iwankurnia.weather.model.SampleData
import com.iwankurnia.weather.model.WeatherSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val snapshot = SampleData.dayPartlyCloudy

    private class FakeRepository(var next: LoadResult) : WeatherRepository {
        val forceFlags = mutableListOf<Boolean>()
        override suspend fun load(forceRefresh: Boolean): LoadResult {
            forceFlags += forceRefresh
            return next
        }

        override suspend fun cached(): WeatherSnapshot? = null
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.stateAfterLoad(result: LoadResult): MainUiState {
        val vm = MainViewModel(FakeRepository(result))
        vm.refresh(false)
        advanceUntilIdle()
        return vm.state.value
    }

    @Test
    fun viewModel_mapsLoadResults() = runTest(dispatcher) {
        assertEquals(MainUiState.Ready(snapshot, staleSince = null), stateAfterLoad(LoadResult.Fresh(snapshot)))
        assertEquals(
            MainUiState.Ready(snapshot, staleSince = snapshot.fetchedAt),
            stateAfterLoad(LoadResult.Stale(snapshot, "offline")),
        )
        assertEquals(MainUiState.LocationDenied, stateAfterLoad(LoadResult.NoLocation(permissionDenied = true)))
        assertEquals(MainUiState.Welcome, stateAfterLoad(LoadResult.NoLocation(permissionDenied = false)))
        assertEquals(MainUiState.Error("boom"), stateAfterLoad(LoadResult.Failed("boom")))
    }

    @Test
    fun viewModel_startsLoadingAndPassesForceFlag() = runTest(dispatcher) {
        val repo = FakeRepository(LoadResult.Fresh(snapshot))
        val vm = MainViewModel(repo)
        assertEquals(MainUiState.Loading, vm.state.value)
        vm.refresh(true)
        advanceUntilIdle()
        assertEquals(listOf(true), repo.forceFlags)
    }

    @Test
    fun viewModel_refreshKeepsSnapshotWhileRefreshing() = runTest(dispatcher) {
        val repo = FakeRepository(LoadResult.Fresh(snapshot))
        val vm = MainViewModel(repo)
        vm.refresh(false)
        advanceUntilIdle()
        vm.refresh(true)
        assertTrue(vm.refreshing.value)
        assertEquals(MainUiState.Ready(snapshot, null), vm.state.value)
        advanceUntilIdle()
        assertFalse(vm.refreshing.value)
    }

    @Test
    fun viewModel_failedRefreshKeepsSnapshotAsStale() = runTest(dispatcher) {
        val repo = FakeRepository(LoadResult.Fresh(snapshot))
        val vm = MainViewModel(repo)
        vm.refresh(false)
        advanceUntilIdle()
        repo.next = LoadResult.Failed("offline")
        vm.refresh(true)
        advanceUntilIdle()
        assertEquals(MainUiState.Ready(snapshot, staleSince = snapshot.fetchedAt), vm.state.value)
    }

    @Test
    fun viewModel_repositoryThrows_showsError() = runTest(dispatcher) {
        val vm = MainViewModel(object : WeatherRepository {
            override suspend fun load(forceRefresh: Boolean): LoadResult = error("kaput")
            override suspend fun cached(): WeatherSnapshot? = null
        })
        vm.refresh(false)
        advanceUntilIdle()
        assertEquals(MainUiState.Error("kaput"), vm.state.value)
    }

    @Test
    fun viewModel_permissionStates() = runTest(dispatcher) {
        val vm = MainViewModel(FakeRepository(LoadResult.Fresh(snapshot)))
        vm.showWelcome()
        assertEquals(MainUiState.Welcome, vm.state.value)
        vm.showLocationDenied()
        assertEquals(MainUiState.LocationDenied, vm.state.value)
    }
}

package com.example.diamonds.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll

/**
 * A thin wrapper around [PullToRefreshContainer] (Material3 1.2.x API).
 *
 * Usage:
 * ```
 * PullToRefreshLayout(
 *     isRefreshing = isRefreshing,
 *     onRefresh    = { viewModel.refresh() }
 * ) {
 *     // your scrollable content here
 * }
 * ```
 *
 * - While [isRefreshing] is true the indicator stays visible.
 * - Once the ViewModel sets [isRefreshing] back to false the indicator hides.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullToRefreshLayout(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val pullState = rememberPullToRefreshState()

    // When the user completes a drag past the threshold, invoke onRefresh
    if (pullState.isRefreshing) {
        LaunchedEffect(true) { onRefresh() }
    }

    // Keep the indicator in refreshing state while the caller is loading
    LaunchedEffect(isRefreshing) {
        if (!isRefreshing) pullState.endRefresh()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(pullState.nestedScrollConnection)
    ) {
        content()

        // Only render the indicator when it's actively being used
        // (dragging or refreshing). This prevents the resting-state
        // indicator from being visible when idle.
        if (pullState.isRefreshing || pullState.progress > 0f) {
            PullToRefreshContainer(
                state = pullState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

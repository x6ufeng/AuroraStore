/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.tv

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.Category
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.gplayapi.helpers.contracts.StreamContract
import com.aurora.gplayapi.helpers.contracts.TopChartsContract
import com.aurora.store.CategoryStash
import com.aurora.store.HomeStash
import com.aurora.store.R
import com.aurora.store.compose.navigation.Destination
import com.aurora.store.data.model.ViewState
import com.aurora.store.data.model.ViewState.Loading.getDataAs
import com.aurora.store.util.Preferences
import com.aurora.store.viewmodel.category.CategoryViewModel
import com.aurora.store.viewmodel.homestream.StreamViewModel
import com.aurora.store.viewmodel.topchart.TopChartViewModel

private const val LOAD_MORE_THRESHOLD = 2

private enum class TvAppsSection(@StringRes val titleRes: Int) {
    FOR_YOU(R.string.tab_for_you),
    TOP_CHARTS(R.string.tab_top_charts),
    CATEGORIES(R.string.tab_categories)
}

private fun category(pageType: Int): StreamContract.Category =
    if (pageType == 1) StreamContract.Category.GAME else StreamContract.Category.APPLICATION

/**
 * TV version of the Apps / Games pages: a row of section buttons followed by D-pad friendly
 * horizontal card rows (For you), a card grid (Top charts) or a category grid.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvAppsGamesScreen(
    pageType: Int,
    streamViewModel: StreamViewModel = hiltViewModel(key = "stream_$pageType"),
    topChartViewModel: TopChartViewModel = hiltViewModel(key = "topChart_$pageType"),
    categoryViewModel: CategoryViewModel = hiltViewModel(key = "category_$pageType"),
    onNavigateTo: (Destination) -> Unit = {}
) {
    val context = LocalContext.current
    val sections = buildList {
        if (Preferences.getBoolean(context, Preferences.PREFERENCE_FOR_YOU)) {
            add(TvAppsSection.FOR_YOU)
        }
        add(TvAppsSection.TOP_CHARTS)
        add(TvAppsSection.CATEGORIES)
    }
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val section = sections[selected.coerceIn(0, sections.lastIndex)]

    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(
                horizontal = TvDimens.ScreenHorizontal,
                vertical = 8.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(sections) { index, item ->
                Button(
                    onClick = { selected = index },
                    colors = if (item == section) {
                        ButtonDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        ButtonDefaults.colors()
                    }
                ) {
                    Text(
                        text = stringResource(item.titleRes),
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
        }

        when (section) {
            TvAppsSection.FOR_YOU -> TvForYou(
                pageType = pageType,
                viewModel = streamViewModel,
                onAppClick = { onNavigateTo(Destination.AppDetails(it.packageName)) }
            )

            TvAppsSection.TOP_CHARTS -> TvTopCharts(
                pageType = pageType,
                viewModel = topChartViewModel,
                onAppClick = { onNavigateTo(Destination.AppDetails(it.packageName)) }
            )

            TvAppsSection.CATEGORIES -> TvCategories(
                pageType = pageType,
                viewModel = categoryViewModel,
                onCategoryClick = { onNavigateTo(Destination.CategoryBrowse(it)) }
            )
        }
    }
}

@Composable
private fun TvForYou(pageType: Int, viewModel: StreamViewModel, onAppClick: (App) -> Unit) {
    val category = category(pageType)
    val state by viewModel.liveData.observeAsState()

    LaunchedEffect(category) {
        viewModel.getStreamBundle(category, StreamContract.Type.HOME)
    }

    @Suppress("UNCHECKED_CAST")
    val bundle = ((state as? ViewState.Success<*>)?.data as? HomeStash)?.get(category)
    val listState: LazyListState = rememberLazyListState()

    val clusters = remember(bundle) {
        bundle?.streamClusters?.values
            ?.map {
                it.copy(clusterAppList = it.clusterAppList.distinctBy { app -> app.packageName })
            }
            ?.filter { it.clusterAppList.size > 1 && it.clusterTitle.isNotBlank() }
            .orEmpty()
    }

    val reachedEnd by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            last >= listState.layoutInfo.totalItemsCount - LOAD_MORE_THRESHOLD
        }
    }
    LaunchedEffect(reachedEnd, bundle) {
        if (reachedEnd && bundle != null && bundle.hasNext()) {
            viewModel.observe(category, StreamContract.Type.HOME)
        }
    }

    if (bundle == null) {
        TvLoading()
        return
    }

    if (clusters.isEmpty()) {
        TvMessage(stringResource(R.string.no_apps_available))
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(vertical = TvDimens.ScreenVertical),
        verticalArrangement = Arrangement.spacedBy(TvDimens.RowSpacing)
    ) {
        items(count = clusters.size, key = { clusters[it].id }) { index ->
            TvClusterRow(
                cluster = clusters[index],
                onAppClick = onAppClick,
                onClusterScrolled = { viewModel.observeCluster(category, it) }
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvTopCharts(pageType: Int, viewModel: TopChartViewModel, onAppClick: (App) -> Unit) {
    val charts = listOf(
        TopChartsContract.Chart.TOP_SELLING_FREE,
        TopChartsContract.Chart.TOP_GROSSING,
        TopChartsContract.Chart.MOVERS_SHAKERS,
        TopChartsContract.Chart.TOP_SELLING_PAID
    )
    val titles = listOf(
        R.string.tab_top_free,
        R.string.tab_top_grossing,
        R.string.tab_trending,
        R.string.tab_top_paid
    )
    val chartType =
        if (pageType == 1) TopChartsContract.Type.GAME else TopChartsContract.Type.APPLICATION
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val cluster = state.getDataAs<StreamCluster?>()
    val gridState = rememberLazyGridState()

    LaunchedEffect(selected) {
        gridState.scrollToItem(0)
        viewModel.getStreamCluster(chartType, charts[selected])
    }

    val reachedEnd by remember {
        derivedStateOf {
            val last = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            last >= gridState.layoutInfo.totalItemsCount - LOAD_MORE_THRESHOLD * 6
        }
    }
    LaunchedEffect(reachedEnd) {
        if (reachedEnd && cluster?.hasNext() == true) {
            viewModel.nextCluster(chartType, charts[selected])
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(
                horizontal = TvDimens.ScreenHorizontal,
                vertical = 8.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(titles) { index, titleRes ->
                Button(
                    onClick = { selected = index },
                    colors = if (index == selected) {
                        ButtonDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        ButtonDefaults.colors()
                    }
                ) {
                    Text(
                        text = stringResource(titleRes),
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
        }

        val apps = cluster?.clusterAppList.orEmpty()
        when {
            state is ViewState.Error -> TvMessage(stringResource(R.string.error))
            apps.isEmpty() -> TvLoading()
            else -> LazyVerticalGrid(
                modifier = Modifier.fillMaxSize(),
                state = gridState,
                columns = GridCells.Adaptive(TvDimens.AppCardWidth + 16.dp),
                contentPadding = PaddingValues(
                    horizontal = TvDimens.ScreenHorizontal,
                    vertical = 16.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(TvDimens.CardSpacing),
                verticalArrangement = Arrangement.spacedBy(TvDimens.CardSpacing)
            ) {
                items(items = apps, key = { it.packageName }) { app ->
                    TvAppCard(app = app, onClick = { onAppClick(app) })
                }
            }
        }
    }
}

@Composable
private fun TvCategories(
    pageType: Int,
    viewModel: CategoryViewModel,
    onCategoryClick: (Category) -> Unit
) {
    val categoryType = if (pageType == 1) Category.Type.GAME else Category.Type.APPLICATION
    val state by viewModel.liveData.observeAsState()

    LaunchedEffect(categoryType) {
        viewModel.getCategoryList(categoryType)
    }

    if (state is ViewState.Error) {
        TvMessage(stringResource(R.string.error))
        return
    }

    @Suppress("UNCHECKED_CAST")
    val list = ((state as? ViewState.Success<*>)?.data as? CategoryStash)?.get(categoryType)
    if (list.isNullOrEmpty()) {
        TvLoading()
        return
    }

    LazyVerticalGrid(
        modifier = Modifier.fillMaxSize(),
        columns = GridCells.Adaptive(TvDimens.CategoryCardWidth),
        contentPadding = PaddingValues(
            horizontal = TvDimens.ScreenHorizontal,
            vertical = TvDimens.ScreenVertical
        ),
        horizontalArrangement = Arrangement.spacedBy(TvDimens.CardSpacing),
        verticalArrangement = Arrangement.spacedBy(TvDimens.CardSpacing)
    ) {
        items(items = list, key = { it.title }) { category ->
            TvCategoryCard(
                category = category,
                modifier = Modifier.padding(4.dp),
                onClick = { onCategoryClick(category) }
            )
        }
    }
}

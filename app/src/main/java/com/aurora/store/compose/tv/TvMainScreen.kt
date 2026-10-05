/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.tv

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
import com.aurora.store.R
import com.aurora.store.compose.navigation.Destination

enum class TvTab(
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int
) {
    APPS(R.string.title_apps, R.drawable.ic_apps),
    GAMES(R.string.title_games, R.drawable.ic_games),
    UPDATES(R.string.title_updates, R.drawable.ic_updates),
    SEARCH(R.string.action_search, R.drawable.ic_round_search),
    DOWNLOADS(R.string.title_download_manager, R.drawable.ic_download_manager),
    MORE(R.string.title_more, R.drawable.ic_settings_account)
}

/**
 * Top-navigation shell for TV. Search and Downloads open their own screens; the other tabs swap
 * the content area beneath the tab bar.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvMainScreen(
    initialTab: Int = 0,
    updateCount: Int = 0,
    onNavigateTo: (Destination) -> Unit = {},
    appsContent: @Composable () -> Unit,
    gamesContent: @Composable () -> Unit,
    updatesContent: @Composable () -> Unit
) {
    var selectedIndex by rememberSaveable {
        mutableIntStateOf(initialTab.coerceIn(0, TvTab.UPDATES.ordinal))
    }
    val tabs = TvTab.entries

    Surface(
        modifier = Modifier.fillMaxSize(),
        colors = SurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.background
        )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TabRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = TvDimens.ScreenHorizontal,
                        vertical = TvDimens.ScreenVertical
                    ),
                selectedTabIndex = selectedIndex
            ) {
                tabs.forEachIndexed { index, tab ->
                    // Search and Downloads are actions, so they never become the selected tab
                    val isAction = tab == TvTab.SEARCH || tab == TvTab.DOWNLOADS
                    Tab(
                        selected = index == selectedIndex,
                        onFocus = { if (!isAction) selectedIndex = index },
                        onClick = {
                            when (tab) {
                                TvTab.SEARCH -> onNavigateTo(Destination.Search)
                                TvTab.DOWNLOADS -> onNavigateTo(Destination.Downloads)
                                else -> selectedIndex = index
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                modifier = Modifier.padding(end = 2.dp),
                                painter = painterResource(tab.iconRes),
                                contentDescription = null
                            )
                            val label = stringResource(tab.labelRes)
                            Text(
                                text = if (tab == TvTab.UPDATES && updateCount > 0) {
                                    "$label ($updateCount)"
                                } else {
                                    label
                                },
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (tabs[selectedIndex]) {
                    TvTab.APPS -> appsContent()
                    TvTab.GAMES -> gamesContent()
                    TvTab.UPDATES -> updatesContent()
                    TvTab.MORE -> TvMoreScreen(onNavigateTo = onNavigateTo)
                    // Handled as actions above
                    TvTab.SEARCH, TvTab.DOWNLOADS -> Unit
                }
            }
        }
    }
}

private data class TvMoreItem(
    @StringRes val titleRes: Int,
    @DrawableRes val iconRes: Int,
    val destination: Destination
)

/**
 * TV replacement for the phone's "More" bottom sheet, as a grid of large cards.
 */
@Composable
fun TvMoreScreen(onNavigateTo: (Destination) -> Unit) {
    val items = listOf(
        TvMoreItem(
            R.string.title_notifications,
            R.drawable.ic_notifications,
            Destination.Notifications
        ),
        TvMoreItem(R.string.title_apps_games, R.drawable.ic_apps, Destination.Installed),
        TvMoreItem(
            R.string.title_blacklist_manager,
            R.drawable.ic_blacklist,
            Destination.Blacklist
        ),
        TvMoreItem(
            R.string.title_favourites_manager,
            R.drawable.ic_favorite_unchecked,
            Destination.Favourite
        ),
        TvMoreItem(R.string.title_spoof_manager, R.drawable.ic_spoof, Destination.Spoof),
        TvMoreItem(R.string.title_settings, R.drawable.ic_menu_settings, Destination.Settings),
        TvMoreItem(R.string.title_about, R.drawable.ic_menu_about, Destination.About),
        TvMoreItem(R.string.title_account_manager, R.drawable.ic_account, Destination.Accounts)
    )

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
        items(items) { item ->
            TvCategoryCard(
                modifier = Modifier.padding(4.dp),
                title = stringResource(item.titleRes),
                iconRes = item.iconRes,
                onClick = { onNavigateTo(item.destination) }
            )
        }
    }
}

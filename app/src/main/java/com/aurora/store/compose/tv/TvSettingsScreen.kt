/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.tv

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.aurora.store.R
import com.aurora.store.compose.navigation.Destination
import com.aurora.store.data.model.PermissionType

private data class TvSettingsItem(
    @StringRes val titleRes: Int,
    @DrawableRes val iconRes: Int,
    val destination: Destination
)

/**
 * TV settings landing page: a grid of large cards, one per preference group.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvSettingsScreen(onNavigateTo: (Destination) -> Unit) {
    val items = listOf(
        TvSettingsItem(
            R.string.onboarding_title_permissions,
            R.drawable.ic_list_check,
            Destination.PermissionRationale(PermissionType.entries.toSet())
        ),
        TvSettingsItem(
            R.string.title_installation,
            R.drawable.ic_installation,
            Destination.InstallationPreference
        ),
        TvSettingsItem(R.string.pref_ui_title, R.drawable.ic_ui, Destination.UIPreference),
        TvSettingsItem(
            R.string.title_notifications,
            R.drawable.ic_notification_settings,
            Destination.NotificationPreference
        ),
        TvSettingsItem(
            R.string.pref_network_title,
            R.drawable.ic_network,
            Destination.NetworkPreference
        ),
        TvSettingsItem(
            R.string.title_updates,
            R.drawable.ic_updates,
            Destination.UpdatesPreference
        ),
        TvSettingsItem(R.string.title_security, R.drawable.ic_lock, Destination.SecurityPreference)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = TvDimens.ScreenVertical)
    ) {
        Text(
            modifier = Modifier.padding(horizontal = TvDimens.ScreenHorizontal),
            text = stringResource(R.string.title_settings),
            style = MaterialTheme.typography.headlineLarge
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
}

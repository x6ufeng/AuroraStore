/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.tv

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.StandardCardContainer
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.StreamBundle
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.store.R
import com.aurora.store.compose.composable.app.AnimatedAppIcon
import com.aurora.store.compose.navigation.Destination
import com.aurora.store.compose.ui.details.menu.MenuItem
import com.aurora.store.data.model.AppState
import com.aurora.store.util.CommonUtil

/**
 * TV app details: a large header, one row of actions, then screenshots, links to the secondary
 * pages and similar-app rows. Install/update logic stays in the caller and is passed in through
 * [actions] and [onMenuItem], so the TV and phone layouts can't drift apart in behaviour.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvAppDetailsPane(
    app: App,
    state: AppState,
    isFavorite: Boolean,
    canManualDownload: Boolean,
    canUseOtherAccount: Boolean,
    suggestionsBundle: StreamBundle?,
    actions: @Composable () -> Unit,
    onMenuItem: (MenuItem) -> Unit,
    onShowDeveloper: () -> Unit,
    onShowScreenshot: (index: Int) -> Unit,
    onShowMore: () -> Unit,
    onShowReviews: () -> Unit,
    onShowPermissions: (() -> Unit)?,
    onShowPrivacy: (() -> Unit)?,
    onNavigateTo: (Destination) -> Unit,
    onLoadMoreCluster: (StreamCluster) -> Unit
) {
    val clusters = suggestionsBundle?.streamClusters?.values
        ?.filter { it.clusterTitle.isNotBlank() && it.clusterAppList.isNotEmpty() }
        .orEmpty()
    val screenshots = app.screenshots.distinctBy { it.url }
    val manualEnabled = canManualDownload && !state.inProgress()
    val installedLike = state is AppState.Installed || state is AppState.Updatable

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = TvDimens.ScreenVertical),
        verticalArrangement = Arrangement.spacedBy(TvDimens.RowSpacing)
    ) {
        item(key = "header") {
            TvDetailsHeader(
                app = app,
                state = state,
                onShowDeveloper = onShowDeveloper
            )
        }

        item(key = "actions") {
            Column(
                modifier = Modifier.padding(horizontal = TvDimens.ScreenHorizontal),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                actions()
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        TvSecondaryButton(
                            text = stringResource(R.string.action_favourite),
                            highlighted = isFavorite,
                            onClick = { onMenuItem(MenuItem.FAVORITE) }
                        )
                    }
                    item {
                        TvSecondaryButton(
                            text = stringResource(R.string.action_share),
                            onClick = { onMenuItem(MenuItem.SHARE) }
                        )
                    }
                    if (manualEnabled) {
                        item {
                            TvSecondaryButton(
                                text = stringResource(R.string.title_manual_download),
                                onClick = { onMenuItem(MenuItem.MANUAL_DOWNLOAD) }
                            )
                        }
                    }
                    if (canUseOtherAccount && !state.inProgress()) {
                        item {
                            TvSecondaryButton(
                                text = stringResource(R.string.action_switch_account),
                                onClick = { onMenuItem(MenuItem.INSTALL_OTHER_ACCOUNT) }
                            )
                        }
                    }
                    if (installedLike) {
                        item {
                            TvSecondaryButton(
                                text = stringResource(R.string.action_info),
                                onClick = { onMenuItem(MenuItem.APP_INFO) }
                            )
                        }
                    }
                }
            }
        }

        if (app.shortDescription.isNotBlank() || app.changes.isNotBlank()) {
            item(key = "about") {
                Column(
                    modifier = Modifier.padding(horizontal = TvDimens.ScreenHorizontal),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (app.shortDescription.isNotBlank()) {
                        Text(
                            text = app.shortDescription,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (app.changes.isNotBlank()) {
                        Text(
                            text = stringResource(R.string.details_changelog),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = app.changes.replace(Regex("<[^>]*>"), " ").trim(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 5,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        if (screenshots.isNotEmpty()) {
            item(key = "screenshots") {
                LazyRow(
                    contentPadding = PaddingValues(
                        horizontal = TvDimens.ScreenHorizontal,
                        vertical = 12.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(TvDimens.CardSpacing)
                ) {
                    itemsIndexed(screenshots, key = { _, it -> it.url }) { index, artwork ->
                        TvScreenshotCard(
                            url = "${artwork.url}=rw-w640-v1-e15",
                            onClick = { onShowScreenshot(index) }
                        )
                    }
                }
            }
        }

        item(key = "links") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = TvDimens.ScreenHorizontal),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    TvSecondaryButton(
                        text = stringResource(R.string.details_more_about_app),
                        onClick = onShowMore
                    )
                }
                item {
                    TvSecondaryButton(
                        text = stringResource(R.string.details_ratings),
                        onClick = onShowReviews
                    )
                }
                if (onShowPermissions != null) {
                    item {
                        TvSecondaryButton(
                            text = stringResource(R.string.details_permission),
                            onClick = onShowPermissions
                        )
                    }
                }
                if (onShowPrivacy != null) {
                    item {
                        TvSecondaryButton(
                            text = stringResource(R.string.exodus_view_report),
                            onClick = onShowPrivacy
                        )
                    }
                }
            }
        }

        items(count = clusters.size, key = { "cluster-${clusters[it].id}" }) { index ->
            TvClusterRow(
                cluster = clusters[index],
                onAppClick = { onNavigateTo(Destination.AppDetails(it.packageName)) },
                onClusterScrolled = onLoadMoreCluster
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvDetailsHeader(app: App, state: AppState, onShowDeveloper: () -> Unit) {
    val context = LocalContext.current
    val versionName = if (state is AppState.Installed) state.versionName else app.versionName
    val versionCode = if (state is AppState.Installed) state.versionCode else app.versionCode

    val status = when (state) {
        is AppState.Downloading ->
            "${Formatter.formatShortFileSize(context, state.speed)}/s, " +
                CommonUtil.getETAString(context, state.timeRemaining)

        is AppState.Installing -> stringResource(R.string.action_installing)
        AppState.Queued -> stringResource(R.string.status_queued)
        AppState.Purchasing -> stringResource(R.string.preparing_to_download)
        AppState.Verifying -> stringResource(R.string.verifying_downloads)
        else -> stringResource(R.string.version, versionName, versionCode)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TvDimens.ScreenHorizontal),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedAppIcon(
            modifier = Modifier.requiredSize(160.dp),
            iconUrl = app.iconArtwork.url,
            inProgress = state.inProgress(),
            progress = state.progress()
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = app.displayName,
                style = MaterialTheme.typography.displaySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Button(
                onClick = onShowDeveloper,
                colors = ButtonDefaults.colors(
                    containerColor = Color.Transparent
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = app.developerName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = status,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvSecondaryButton(text: String, onClick: () -> Unit, highlighted: Boolean = false) {
    Button(
        onClick = onClick,
        colors = if (highlighted) {
            ButtonDefaults.colors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        } else {
            ButtonDefaults.colors()
        }
    ) {
        Text(text = text, style = MaterialTheme.typography.titleSmall, maxLines = 1)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvScreenshotCard(url: String, onClick: () -> Unit) {
    StandardCardContainer(
        imageCard = { interactionSource ->
            Card(
                onClick = onClick,
                interactionSource = interactionSource,
                shape = CardDefaults.shape(RoundedCornerShape(12.dp)),
                scale = CardDefaults.scale(focusedScale = 1.06f)
            ) {
                AsyncImage(
                    modifier = Modifier.height(220.dp),
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(url)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.FillHeight
                )
            }
        },
        title = {}
    )
}

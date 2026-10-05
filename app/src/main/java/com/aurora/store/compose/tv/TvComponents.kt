/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.tv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ClassicCard
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.StandardCardContainer
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.Category
import com.aurora.gplayapi.data.models.StreamCluster

private const val LOAD_MORE_THRESHOLD = 3

/**
 * Focusable app card with a large icon and name, used in horizontal rows.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvAppCard(app: App, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    StandardCardContainer(
        modifier = modifier.width(TvDimens.AppCardWidth),
        imageCard = { interactionSource ->
            ClassicCard(
                onClick = onClick,
                interactionSource = interactionSource,
                shape = CardDefaults.shape(RoundedCornerShape(16.dp)),
                scale = CardDefaults.scale(focusedScale = 1.08f),
                image = {
                    AsyncImage(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f),
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(app.iconArtwork.url)
                            .crossfade(true)
                            .build(),
                        contentDescription = app.displayName,
                        contentScale = ContentScale.Crop
                    )
                },
                title = {}
            )
        },
        title = {
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = app.displayName,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    )
}

/**
 * Wide card for categories and menu entries: icon on the left, big label on the right.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvCategoryCard(
    title: String,
    modifier: Modifier = Modifier,
    imageUrl: String? = null,
    iconRes: Int? = null,
    onClick: () -> Unit = {}
) {
    ClassicCard(
        modifier = modifier,
        onClick = onClick,
        shape = CardDefaults.shape(RoundedCornerShape(16.dp)),
        scale = CardDefaults.scale(focusedScale = 1.06f),
        image = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
                contentAlignment = Alignment.Center
            ) {
                val tint = ColorFilter.tint(MaterialTheme.colorScheme.primary)
                when {
                    iconRes != null -> Icon(
                        modifier = Modifier.padding(24.dp).fillMaxSize(),
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )

                    imageUrl != null -> AsyncImage(
                        modifier = Modifier.padding(24.dp).fillMaxSize(),
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        colorFilter = tint,
                        contentScale = ContentScale.Fit
                    )
                }
            }
        },
        title = {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    )
}

@Composable
fun TvCategoryCard(category: Category, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    TvCategoryCard(
        modifier = modifier,
        title = category.title,
        imageUrl = category.imageUrl,
        onClick = onClick
    )
}

/**
 * Titled horizontal row of app cards for a [StreamCluster], loading more as the end is approached.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvClusterRow(
    cluster: StreamCluster,
    modifier: Modifier = Modifier,
    onAppClick: (App) -> Unit = {},
    onClusterScrolled: (StreamCluster) -> Unit = {}
) {
    val rowState = rememberLazyListState()
    val reachedEnd by remember {
        derivedStateOf {
            val last = rowState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            last >= rowState.layoutInfo.totalItemsCount - LOAD_MORE_THRESHOLD
        }
    }
    LaunchedEffect(reachedEnd) {
        if (reachedEnd && cluster.hasNext()) onClusterScrolled(cluster)
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            modifier = Modifier.padding(horizontal = TvDimens.ScreenHorizontal),
            text = cluster.clusterTitle,
            style = MaterialTheme.typography.headlineMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        LazyRow(
            state = rowState,
            // Vertical padding leaves room for the focus scale-up so cards aren't clipped
            contentPadding = PaddingValues(
                horizontal = TvDimens.ScreenHorizontal,
                vertical = 12.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(TvDimens.CardSpacing)
        ) {
            itemsIndexed(
                items = cluster.clusterAppList,
                key = { _, app -> app.packageName }
            ) { _, app ->
                TvAppCard(app = app, onClick = { onAppClick(app) })
            }
        }
    }
}

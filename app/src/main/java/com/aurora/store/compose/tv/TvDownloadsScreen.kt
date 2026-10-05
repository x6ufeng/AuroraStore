/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.tv

import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.aurora.store.R
import com.aurora.store.compose.composable.app.AnimatedAppIcon
import com.aurora.store.compose.navigation.Destination
import com.aurora.store.data.model.DownloadStatus
import com.aurora.store.data.room.download.Download
import com.aurora.store.util.CommonUtil.getETAString
import com.aurora.store.util.PackageUtil
import com.aurora.store.viewmodel.downloads.DownloadsViewModel

/**
 * TV download manager: bulk actions on top, then one large row per download. Selecting a row
 * reveals its actions inline instead of opening a bottom sheet, which is awkward with a D-pad.
 * APK export is left out because it needs the system file picker.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvDownloadsScreen(
    onNavigateTo: (Destination) -> Unit,
    viewModel: DownloadsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val downloads = viewModel.downloads.collectAsLazyPagingItems()
    var selected by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = TvDimens.ScreenVertical)
    ) {
        Text(
            modifier = Modifier.padding(horizontal = TvDimens.ScreenHorizontal),
            text = stringResource(R.string.title_download_manager),
            style = MaterialTheme.typography.headlineLarge
        )

        when {
            downloads.loadState.refresh is LoadState.Loading && downloads.itemCount == 0 ->
                TvLoading()

            downloads.itemCount == 0 -> TvMessage(stringResource(R.string.download_none))

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = TvDimens.ScreenHorizontal,
                    vertical = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "bulk") {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(onClick = { viewModel.cancelAll() }) {
                            Text(
                                text = stringResource(R.string.download_cancel_all),
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Button(onClick = { viewModel.clearFinished() }) {
                            Text(
                                text = stringResource(R.string.download_clear_finished),
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }
                }
                items(
                    count = downloads.itemCount,
                    key = downloads.itemKey { it.packageName }
                ) { index ->
                    downloads[index]?.let { download ->
                        val expanded = selected == download.packageName
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            TvDownloadRow(
                                download = download,
                                onClick = {
                                    selected = if (expanded) null else download.packageName
                                }
                            )
                            if (expanded) {
                                Row(
                                    modifier = Modifier.padding(start = 24.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            onNavigateTo(
                                                Destination.AppDetails(download.packageName)
                                            )
                                        }
                                    ) {
                                        Text(
                                            text = stringResource(R.string.action_info),
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                    }
                                    if (download.isRunning) {
                                        Button(onClick = {
                                            viewModel.cancel(download.packageName)
                                        }) {
                                            Text(
                                                text = stringResource(R.string.action_cancel),
                                                style = MaterialTheme.typography.titleSmall
                                            )
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                viewModel.clear(
                                                    download.packageName,
                                                    download.versionCode
                                                )
                                                selected = null
                                            }
                                        ) {
                                            Text(
                                                text = stringResource(R.string.action_clear),
                                                style = MaterialTheme.typography.titleSmall
                                            )
                                        }
                                    }
                                    if (!PackageUtil.isInstalled(context, download.packageName) &&
                                        download.canInstall(context)
                                    ) {
                                        Button(onClick = { viewModel.install(download) }) {
                                            Text(
                                                text = stringResource(R.string.action_install),
                                                style = MaterialTheme.typography.titleSmall
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvDownloadRow(download: Download, onClick: () -> Unit) {
    val context = LocalContext.current
    val status = stringResource(download.status.localized)
    val detail = if (download.status in DownloadStatus.running) {
        "${download.progress}% • " +
            "${Formatter.formatShortFileSize(context, download.speed)}/s • " +
            getETAString(context, download.timeRemaining)
    } else {
        DateUtils.formatDateTime(context, download.downloadedAt, DateUtils.FORMAT_SHOW_DATE)
    }

    Button(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            AnimatedAppIcon(
                modifier = Modifier.requiredSize(72.dp),
                iconUrl = download.iconURL,
                inProgress = download.isRunning,
                progress = download.progress.toFloat()
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = download.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$status • $detail",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

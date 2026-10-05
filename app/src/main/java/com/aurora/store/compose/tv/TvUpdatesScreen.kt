/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.tv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.aurora.store.R
import com.aurora.store.compose.navigation.Destination
import com.aurora.store.data.room.update.Update
import com.aurora.store.viewmodel.all.UpdatesViewModel

/**
 * TV updates list: one large row per update with a single focusable action button. The update
 * flow itself (tracker warnings, OBB permission, storage checks) stays in the caller, so this
 * screen only reports intents through the callbacks.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvUpdatesScreen(
    viewModel: UpdatesViewModel = hiltViewModel(),
    onNavigateTo: (Destination) -> Unit = {},
    onRequestUpdate: (Update) -> Unit = {},
    onRequestUpdateAll: (List<Update>) -> Unit = {},
    onCancelUpdate: (String) -> Unit = {},
    onCancelAll: () -> Unit = {},
    checkingPackages: Set<String> = emptySet()
) {
    val context = LocalContext.current
    val updates by viewModel.updates.collectAsStateWithLifecycle()
    val downloads by viewModel.downloadsList.collectAsStateWithLifecycle()

    // Incompatible updates can't be installed, so they're left out of the TV list
    val installable = remember(updates) { updates.orEmpty().filterNot { it.isIncompatible } }
    val inProgress = remember(downloads) {
        downloads.filter { !it.isFinished || it.isActive }.map { it.packageName }.toSet()
    }

    when {
        updates == null -> TvLoading()

        installable.isEmpty() -> Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.details_no_updates),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                modifier = Modifier.padding(top = 24.dp),
                onClick = { viewModel.fetchUpdates() }
            ) {
                Text(
                    text = stringResource(R.string.check_updates),
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }

        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = TvDimens.ScreenHorizontal,
                vertical = TvDimens.ScreenVertical
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "actions") {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(onClick = {
                        onRequestUpdateAll(installable.filterNot { it.isSelfUpdate(context) })
                    }) {
                        Text(
                            text = stringResource(R.string.action_update_all),
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    if (inProgress.isNotEmpty()) {
                        Button(onClick = onCancelAll) {
                            Text(
                                text = stringResource(R.string.action_cancel),
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }
                }
            }
            items(items = installable, key = { it.packageName }) { update ->
                val busy = update.packageName in inProgress ||
                    update.packageName in checkingPackages
                TvUpdateRow(
                    update = update,
                    busy = busy,
                    onOpen = { onNavigateTo(Destination.AppDetails(update.packageName)) },
                    onAction = {
                        if (busy) onCancelUpdate(update.packageName) else onRequestUpdate(update)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvUpdateRow(update: Update, busy: Boolean, onOpen: () -> Unit, onAction: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // The icon + text act as one focus target that opens the details page
        Button(
            modifier = Modifier.weight(1f),
            onClick = onOpen
        ) {
            Row(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                AsyncImage(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    model = update.iconURL,
                    contentDescription = null
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = update.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${update.versionName} (${update.versionCode})",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Button(onClick = onAction) {
            Text(
                text = stringResource(if (busy) R.string.action_cancel else R.string.action_update),
                style = MaterialTheme.typography.titleSmall
            )
        }
    }
}

/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.tv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

/**
 * TV counterpart of the details screen's primary/secondary action pair. The primary action comes
 * first so it takes initial focus when the page opens.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvActions(
    primaryActionDisplayName: String,
    secondaryActionDisplayName: String,
    isPrimaryActionEnabled: Boolean = true,
    isSecondaryActionEnabled: Boolean = true,
    onPrimaryAction: () -> Unit = {},
    onSecondaryAction: () -> Unit = {}
) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Button(
            modifier = Modifier.widthIn(min = 220.dp),
            onClick = onPrimaryAction,
            enabled = isPrimaryActionEnabled,
            colors = ButtonDefaults.colors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                text = primaryActionDisplayName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Button(
            modifier = Modifier.widthIn(min = 220.dp),
            onClick = onSecondaryAction,
            enabled = isSecondaryActionEnabled
        ) {
            Text(
                text = secondaryActionDisplayName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

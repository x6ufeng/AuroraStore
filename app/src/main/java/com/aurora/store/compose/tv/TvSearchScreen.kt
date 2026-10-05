/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.tv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.aurora.store.R
import com.aurora.store.compose.navigation.Destination
import com.aurora.store.viewmodel.search.SearchViewModel

/**
 * TV search: on-screen-keyboard friendly query field, suggestion buttons and a card grid of results.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvSearchScreen(
    viewModel: SearchViewModel = hiltViewModel(),
    onNavigateTo: (Destination) -> Unit = {}
) {
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val results = viewModel.apps.collectAsLazyPagingItems()
    var query by rememberSaveable { mutableStateOf("") }
    var hasSearched by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(query) { viewModel.fetchSuggestions(query) }

    fun submit(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        query = trimmed
        hasSearched = true
        viewModel.search(trimmed)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = TvDimens.ScreenVertical),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TvDimens.ScreenHorizontal),
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            textStyle = MaterialTheme.typography.titleLarge,
            placeholder = {
                Text(
                    text = stringResource(R.string.search_hint),
                    style = MaterialTheme.typography.titleLarge
                )
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { submit(query) })
        )

        if (suggestions.isNotEmpty() && query.isNotBlank()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = TvDimens.ScreenHorizontal),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(suggestions.take(8)) { suggestion ->
                    Button(onClick = { submit(suggestion.title) }) {
                        Text(
                            text = suggestion.title,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }
        }

        when {
            !hasSearched -> Unit

            results.loadState.refresh is LoadState.Loading -> TvLoading()

            results.loadState.refresh is LoadState.Error ->
                TvMessage(stringResource(R.string.error))

            results.itemCount == 0 -> TvMessage(stringResource(R.string.no_apps_available))

            else -> LazyVerticalGrid(
                modifier = Modifier.fillMaxSize(),
                columns = GridCells.Adaptive(TvDimens.AppCardWidth + 16.dp),
                contentPadding = PaddingValues(
                    horizontal = TvDimens.ScreenHorizontal,
                    vertical = 16.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(TvDimens.CardSpacing),
                verticalArrangement = Arrangement.spacedBy(TvDimens.CardSpacing)
            ) {
                items(count = results.itemCount) { index ->
                    results[index]?.let { app ->
                        TvAppCard(
                            app = app,
                            onClick = { onNavigateTo(Destination.AppDetails(app.packageName)) }
                        )
                    }
                }
            }
        }
    }
}

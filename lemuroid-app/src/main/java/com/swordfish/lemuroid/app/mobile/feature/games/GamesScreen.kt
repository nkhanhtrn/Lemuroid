package com.swordfish.lemuroid.app.mobile.feature.games

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.feature.games.GamesViewModel.Layout
import com.swordfish.lemuroid.app.mobile.feature.games.GamesViewModel.SortOrder
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.LemuroidEmptyView
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.LemuroidGameGridCell
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.LemuroidGameListRow
import com.swordfish.lemuroid.lib.library.db.entity.Game

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GamesScreen(
    modifier: Modifier = Modifier,
    viewModel: GamesViewModel,
    onGameClick: (Game) -> Unit,
    onGameLongClick: (Game) -> Unit,
    onGameFavoriteToggle: (Game, Boolean) -> Unit,
) {
    val games = viewModel.games.collectAsLazyPagingItems()
    val layout by viewModel.layout.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        GamesToolbar(
            searchQuery = searchQuery,
            onSearchQueryChange = viewModel::setSearchQuery,
            sortOrder = sortOrder,
            onSortOrderChange = viewModel::setSortOrder,
            layout = layout,
            onLayoutChange = viewModel::setLayout,
        )

        if (games.itemCount == 0) {
            LemuroidEmptyView(
                modifier = Modifier.weight(1f),
                text =
                    if (searchQuery.isBlank()) {
                        stringResource(id = R.string.empty_view_default)
                    } else {
                        stringResource(id = R.string.game_list_no_results)
                    },
            )
            return@Column
        }

        when (layout) {
            Layout.GRID ->
                LazyVerticalGrid(
                    modifier = Modifier.weight(1f),
                    columns = GridCells.Adaptive(minSize = 128.dp),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(games.itemCount, key = { games[it]?.id ?: it }) { index ->
                        val game = games[index] ?: return@items
                        LemuroidGameGridCell(
                            modifier = Modifier.animateItem(),
                            game = game,
                            onClick = { onGameClick(game) },
                            onLongClick = { onGameLongClick(game) },
                        )
                    }
                }

            Layout.LIST ->
                LazyColumn(
                    modifier = Modifier.weight(1f),
                ) {
                    items(games.itemCount, key = { games[it]?.id ?: it }) { index ->
                        val game = games[index] ?: return@items
                        LemuroidGameListRow(
                            modifier = Modifier.animateItem(),
                            game = game,
                            onClick = { onGameClick(game) },
                            onLongClick = { onGameLongClick(game) },
                            onFavoriteToggle = { isFavorite -> onGameFavoriteToggle(game, isFavorite) },
                        )
                    }
                }
        }
    }
}

@Composable
private fun GamesToolbar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    sortOrder: SortOrder,
    onSortOrderChange: (SortOrder) -> Unit,
    layout: Layout,
    onLayoutChange: (Layout) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            modifier =
                Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp),
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            singleLine = true,
            placeholder = { Text(stringResource(R.string.game_page_search_suggestion)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = null)
                    }
                }
            },
        )

        var sortMenuExpanded by remember { mutableStateOf(false) }
        IconButton(onClick = { sortMenuExpanded = true }) {
            Icon(Icons.Filled.Sort, contentDescription = stringResource(R.string.game_list_sort_last_played))
        }
        DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
            SortMenuItem(stringResource(R.string.game_list_sort_title_az), sortOrder == SortOrder.TITLE_ASC) {
                onSortOrderChange(SortOrder.TITLE_ASC)
                sortMenuExpanded = false
            }
            SortMenuItem(stringResource(R.string.game_list_sort_title_za), sortOrder == SortOrder.TITLE_DESC) {
                onSortOrderChange(SortOrder.TITLE_DESC)
                sortMenuExpanded = false
            }
            SortMenuItem(stringResource(R.string.game_list_sort_last_played), sortOrder == SortOrder.LAST_PLAYED) {
                onSortOrderChange(SortOrder.LAST_PLAYED)
                sortMenuExpanded = false
            }
        }

        IconButton(onClick = { onLayoutChange(if (layout == Layout.GRID) Layout.LIST else Layout.GRID) }) {
            if (layout == Layout.GRID) {
                Icon(Icons.Filled.ViewList, contentDescription = null)
            } else {
                Icon(Icons.Filled.GridView, contentDescription = null)
            }
        }
    }
}

@Composable
private fun SortMenuItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = { RadioButton(selected = selected, onClick = null) },
        onClick = onClick,
    )
}

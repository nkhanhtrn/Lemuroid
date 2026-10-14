package com.swordfish.lemuroid.app.mobile.feature.games

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import com.swordfish.lemuroid.common.paging.buildFlowPaging
import com.swordfish.lemuroid.lib.library.MetaSystemID
import com.swordfish.lemuroid.lib.library.db.RetrogradeDatabase
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper
import com.swordfish.lemuroid.lib.R as LibR
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.combine

class GamesViewModel(
    private val applicationContext: Context,
    private val retrogradeDb: RetrogradeDatabase,
    initialMetaSystem: MetaSystemID,
) : ViewModel() {
    enum class Layout { GRID, LIST }

    enum class SortOrder { TITLE_ASC, TITLE_DESC, LAST_PLAYED }

    class Factory(
        private val applicationContext: Context,
        private val retrogradeDb: RetrogradeDatabase,
        private val initialMetaSystem: MetaSystemID,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GamesViewModel(applicationContext, retrogradeDb, initialMetaSystem) as T
        }
    }

    private val metaSystemId = MutableStateFlow(initialMetaSystem)

    private val prefs by lazy { SharedPreferencesHelper.getSharedPreferences(applicationContext) }

    private val layoutMutable =
        MutableStateFlow(
            prefs
                .getString(applicationContext.getString(LibR.string.pref_key_game_list_layout), null)
                ?.let { runCatching { Layout.valueOf(it) }.getOrNull() }
                ?: Layout.GRID,
        )

    val layout: StateFlow<Layout> = layoutMutable

    private val sortOrderMutable =
        MutableStateFlow(
            prefs
                .getString(applicationContext.getString(LibR.string.pref_key_game_list_sort), null)
                ?.let { runCatching { SortOrder.valueOf(it) }.getOrNull() }
                ?: SortOrder.TITLE_ASC,
        )

    val sortOrder: StateFlow<SortOrder> = sortOrderMutable

    private val searchQueryMutable = MutableStateFlow("")

    val searchQuery: StateFlow<String> = searchQueryMutable

    fun setLayout(value: Layout) {
        layoutMutable.value = value
        prefs.edit().putString(applicationContext.getString(LibR.string.pref_key_game_list_layout), value.name).apply()
    }

    fun setSortOrder(value: SortOrder) {
        sortOrderMutable.value = value
        prefs.edit().putString(applicationContext.getString(LibR.string.pref_key_game_list_sort), value.name).apply()
    }

    fun setSearchQuery(value: String) {
        searchQueryMutable.value = value
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val games: Flow<PagingData<Game>> =
        combine(metaSystemId, sortOrderMutable, searchQueryMutable) { metaSystem, sort, query ->
            Triple(metaSystem, sort, query)
        }.flatMapLatest { (metaSystem, sort, query) ->
            val systemIds = metaSystem.systemIDs.map { it.dbname }
            if (systemIds.isEmpty()) {
                emptyFlow()
            } else {
                buildFlowPaging(20, viewModelScope) {
                    when (sort) {
                        SortOrder.TITLE_ASC -> retrogradeDb.gameDao().selectBySystemsTitleAsc(systemIds, query.trim())
                        SortOrder.TITLE_DESC -> retrogradeDb.gameDao().selectBySystemsTitleDesc(systemIds, query.trim())
                        SortOrder.LAST_PLAYED -> retrogradeDb.gameDao().selectBySystemsLastPlayed(systemIds, query.trim())
                    }
                }
            }
        }
}

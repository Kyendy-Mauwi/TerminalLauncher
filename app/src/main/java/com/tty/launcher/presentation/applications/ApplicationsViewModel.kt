package com.tty.launcher.presentation.applications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tty.launcher.data.repositories.AliasRepository
import com.tty.launcher.data.repositories.AppRepository
import com.tty.launcher.data.repositories.FavoritesRepository
import com.tty.launcher.data.repositories.LaunchResult
import com.tty.launcher.domain.models.AppInfo
import com.tty.launcher.domain.models.FavoriteApp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ApplicationsUiState(
    val applications: List<AppInfo> = emptyList(),
    val favorites: List<FavoriteApp> = emptyList(),
    val aliases: Map<String, String> = emptyMap(),
    val query: String = "",
    val isLoading: Boolean = true,
    val message: String? = null
) {
    val filtered: List<AppInfo>
        get() = if (query.isBlank()) {
            applications
        } else {
            applications.filter {
                it.label.contains(query, ignoreCase = true) ||
                    it.packageName.contains(query, ignoreCase = true) ||
                    aliases[it.packageName]?.contains(query, ignoreCase = true) == true
            }
        }

    fun isFavorite(packageName: String): Boolean = favorites.any { it.packageName == packageName }

    fun displayName(app: AppInfo): String = aliases[app.packageName] ?: app.label
}

@HiltViewModel
class ApplicationsViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val favoritesRepository: FavoritesRepository,
    aliasRepository: AliasRepository
) : ViewModel() {

    private val applications = MutableStateFlow<List<AppInfo>>(emptyList())
    private val query = MutableStateFlow("")
    private val loading = MutableStateFlow(true)
    private val message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ApplicationsUiState> = combine(
        applications,
        favoritesRepository.favorites,
        aliasRepository.aliases,
        query
    ) { apps, favorites, aliases, queryText ->
        ApplicationsUiState(
            applications = apps,
            favorites = favorites,
            aliases = aliases,
            query = queryText,
            isLoading = loading.value
        )
    }.combine(message) { state, msg -> state.copy(message = msg) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ApplicationsUiState())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            loading.value = true
            applications.value = appRepository.installedApps(forceRefresh = true)
            loading.value = false
        }
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun launch(app: AppInfo) {
        viewModelScope.launch {
            when (val result = appRepository.launch(app)) {
                is LaunchResult.Success -> message.value = null
                is LaunchResult.Failure -> message.value = result.reason
            }
        }
    }

    fun toggleFavorite(app: AppInfo) {
        viewModelScope.launch {
            if (favoritesRepository.isFavorite(app.packageName)) {
                favoritesRepository.remove(app.packageName)
                message.value = "${app.label} removed from favourites."
            } else {
                favoritesRepository.add(app)
                message.value = "${app.label} added to favourites."
            }
        }
    }

    fun dismissMessage() {
        message.value = null
    }

    fun clearQuery() {
        query.value = ""
    }

    fun refreshMessageState() {
        message.update { it }
    }
}

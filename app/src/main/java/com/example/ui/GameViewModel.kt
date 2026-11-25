package com.example.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.GamePreferences
import com.example.data.GamePreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WebError(
    val code: Int,
    val description: String,
    val failingUrl: String,
)

data class GameUiState(
    val targetUrl: String = GAME_URL,
    val currentUrl: String = GAME_URL,
    val pageTitle: String = "Nuncat Scapetime",
    val progress: Int = 0,
    val isInitialLoading: Boolean = true,
    val isPageLoading: Boolean = true,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val errorState: WebError? = null,
    val isMenuExpanded: Boolean = false,
    val showExitDialog: Boolean = false,
    val preferences: GamePreferences = GamePreferences(),
    val isOnline: Boolean = true,
) {
    companion object {
        const val GAME_URL = "https://nuncatscapetime-editionbeta.ai.studio/"
    }
}

class GameViewModel(
    private val preferencesRepository: GamePreferencesRepository,
    private val connectivityManager: ConnectivityManager?,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        GameUiState(isOnline = checkCurrentConnectivity())
    )
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _uiState.update { it.copy(isOnline = true) }
        }

        override fun onLost(network: Network) {
            _uiState.update { it.copy(isOnline = checkCurrentConnectivity()) }
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities,
        ) {
            val validated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            _uiState.update { it.copy(isOnline = validated) }
        }
    }

    init {
        viewModelScope.launch {
            preferencesRepository.preferencesFlow.collect { prefs ->
                _uiState.update { it.copy(preferences = prefs) }
            }
        }
        try {
            connectivityManager?.registerDefaultNetworkCallback(networkCallback)
        } catch (_: SecurityException) {
            // Fallback gracefully if connectivity callback cannot be registered
        }
    }

    private fun checkCurrentConnectivity(): Boolean {
        val cm = connectivityManager ?: return true
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun onPageStarted(url: String?) {
        _uiState.update {
            it.copy(
                currentUrl = url ?: it.currentUrl,
                isPageLoading = true,
                errorState = null,
            )
        }
    }

    fun onPageFinished(url: String?, canGoBack: Boolean, canGoForward: Boolean) {
        _uiState.update {
            it.copy(
                currentUrl = url ?: it.currentUrl,
                isPageLoading = false,
                isInitialLoading = false,
                progress = 100,
                canGoBack = canGoBack,
                canGoForward = canGoForward,
            )
        }
    }

    fun onProgressChanged(newProgress: Int) {
        _uiState.update { state ->
            val dismissInitial = state.isInitialLoading && newProgress >= 100
            state.copy(
                progress = newProgress.coerceIn(0, 100),
                isInitialLoading = if (dismissInitial) false else state.isInitialLoading,
            )
        }
    }

    fun onTitleReceived(title: String?) {
        if (!title.isNullOrBlank()) {
            _uiState.update { it.copy(pageTitle = title) }
        }
    }

    fun onNavigationStateChanged(canGoBack: Boolean, canGoForward: Boolean) {
        _uiState.update {
            it.copy(
                canGoBack = canGoBack,
                canGoForward = canGoForward,
            )
        }
    }

    fun onMainFrameError(code: Int, description: String, failingUrl: String) {
        _uiState.update {
            it.copy(
                isPageLoading = false,
                isInitialLoading = false,
                errorState = WebError(
                    code = code,
                    description = description,
                    failingUrl = failingUrl,
                ),
            )
        }
    }

    fun dismissInitialOverlay() {
        _uiState.update { it.copy(isInitialLoading = false) }
    }

    fun clearErrorForRetry() {
        _uiState.update {
            it.copy(
                errorState = null,
                isPageLoading = true,
                progress = 0,
            )
        }
    }

    fun setMenuExpanded(expanded: Boolean) {
        _uiState.update { it.copy(isMenuExpanded = expanded) }
    }

    fun setShowExitDialog(show: Boolean) {
        _uiState.update { it.copy(showExitDialog = show) }
    }

    fun toggleImmersiveMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setImmersiveMode(enabled)
        }
    }

    fun toggleKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setKeepScreenOn(enabled)
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            connectivityManager?.unregisterNetworkCallback(networkCallback)
        } catch (_: IllegalArgumentException) {
            // Already unregistered
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val repo = GamePreferencesRepository(appContext)
                    val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                    return GameViewModel(repo, cm) as T
                }
            }
        }
    }
}

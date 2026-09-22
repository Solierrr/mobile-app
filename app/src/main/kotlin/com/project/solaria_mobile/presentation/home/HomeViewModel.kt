package com.project.solaria_mobile.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Immutable snapshot of everything the Home screen needs to render.
 *
 * Analogy for a React/Vue dev: this is the shape of the state you'd keep in a
 * Zustand/Pinia store or a `useReducer` state object. The screen never mutates
 * fields on it directly; it always receives a brand-new copy (`data class` + `copy()`),
 * the same discipline as returning a new object from a Redux reducer.
 */
data class HomeUiState(
    val userName: String = "",
    val isLoading: Boolean = false
)

/**
 * ViewModel = a screen-scoped store/controller.
 *
 * Compare to the frontend world:
 *  - It survives configuration changes (rotation) the same way a Pinia/Redux
 *    store survives a component remount - the state doesn't live inside the
 *    "component" (the Composable), so it isn't wiped when the UI is recreated.
 *  - `StateFlow` here plays the role of an observable store value (like a Pinia
 *    `ref`/Vuex getter, or a Redux selector) - the UI subscribes to it instead
 *    of owning the state itself.
 *  - `viewModelScope` is a coroutine scope tied to the ViewModel's lifecycle,
 *    similar to firing an async effect that is automatically cancelled when the
 *    "store" is torn down.
 *
 * In a real app this class would receive its dependencies (a repository/use case)
 * through the constructor - that's exactly where Hilt (this project's DI, see
 * ARCHITECTURE.md) plays the role Spring's `@Autowired`/constructor injection plays
 * for a `@Service`.
 */
class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun onNameChanged(newName: String) {
        _uiState.update { it.copy(userName = newName) }
    }

    /**
     * Example of a one-shot load, the kind of thing you'd normally trigger from
     * `LaunchedEffect` on the Composable side (~ `useEffect(() => {...}, [])`).
     * Kept here to show that side effects / async work belong in the ViewModel,
     * not scattered across Composables.
     */
    fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // TODO: call a use case / repository from `domain` or `data` once they exist.
            _uiState.update { it.copy(isLoading = false, userName = "Solaria") }
        }
    }
}

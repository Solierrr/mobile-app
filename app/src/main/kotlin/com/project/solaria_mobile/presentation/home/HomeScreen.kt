package com.project.solaria_mobile.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.solaria_mobile.core.designsystem.theme.SolariamobileTheme

/**
 * "Screen-level" (route) Composable. It owns the ViewModel and wires it to the
 * "dumb"/presentational Composable below. This split is the same instinct as a
 * React "container" component that calls a hook (`useHomeViewModel()`-ish) and
 * passes plain props down to a stateless child component.
 *
 * `viewModel()` looks up (or creates) the ViewModel scoped to this navigation
 * destination - conceptually similar to injecting a Pinia/Redux store into a
 * page component instead of instantiating it inline.
 */
@Composable
fun HomeRoute(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel()
) {
    // collectAsStateWithLifecycle: subscribe to the StateFlow and turn it into
    // Compose state, pausing collection when the screen isn't visible.
    // Analogy: like a computed/reactive ref in Vue, or the return value of a
    // `useSelector`/`useStore` hook in React-Redux - the Composable recomposes
    // whenever the underlying store value changes.
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // LaunchedEffect ~ useEffect(() => { ... }, [key]).
    // Runs once when this Composable enters the composition (key = Unit),
    // and is cancelled automatically if the Composable leaves the screen.
    LaunchedEffect(Unit) {
        viewModel.loadInitialData()
    }

    HomeScreen(
        uiState = uiState,
        onNameChanged = viewModel::onNameChanged,
        modifier = modifier
    )
}

/**
 * Stateless/"presentational" Composable: receives everything it needs as
 * parameters and reports events back via callbacks. It knows nothing about
 * ViewModel, StateFlow or navigation.
 *
 * This is state hoisting - the direct equivalent of "lifting state up" in
 * React/Vue: instead of the child owning `userName`, the parent (HomeRoute /
 * the ViewModel) owns it and passes it down as a prop (`uiState`) plus an
 * event handler (`onNameChanged`), the same shape as `<Input value={x} onChange={setX} />`.
 * Being stateless also makes it trivial to unit test and to preview (see
 * `HomeScreenPreview` below) without needing a real ViewModel.
 */
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onNameChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator()
        } else {
            Text(text = "Hello, ${uiState.userName}!")
        }
    }

    // `onNameChanged` is unused by this minimal example UI (no text field yet),
    // but it's kept in the signature to make the state-hoisting shape explicit:
    // a real form field would call it on every keystroke, e.g. onValueChange = onNameChanged.
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    SolariamobileTheme {
        HomeScreen(
            uiState = HomeUiState(userName = "Android"),
            onNameChanged = {}
        )
    }
}

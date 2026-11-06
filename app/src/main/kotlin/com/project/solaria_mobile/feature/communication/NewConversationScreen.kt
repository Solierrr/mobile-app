package com.project.solaria_mobile.feature.communication

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.solaria_mobile.R
import com.project.solaria_mobile.core.designsystem.components.userMessage
import com.project.solaria_mobile.feature.profile.ConnectionsRoute

/** Escolha de quem receberá a conversa: a lista de conexões em modo de escolha. */
@Composable
fun NewConversationRoute(onBack: () -> Unit, onOpenChat: (String) -> Unit, viewModel: NewConversationViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state) {
        (state as? NewConversation.Ready)?.let { onOpenChat(it.conversationId); viewModel.consumed() }
    }
    val failed = state as? NewConversation.Failed
    ConnectionsRoute(
        onBack = onBack, onOpenProfile = {},
        pick = true, onPick = viewModel::start,
        status = when (val current = state) {
            is NewConversation.Working -> stringResource(R.string.cv_abrindo_conversa, current.name)
            is NewConversation.Failed -> current.kind.userMessage()
            else -> null
        },
        statusIsError = failed != null,
    )
}

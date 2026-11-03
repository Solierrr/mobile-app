package com.project.solaria_mobile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.feature.communication.ChatScreen
import com.project.solaria_mobile.feature.communication.ChatUiState
import com.project.solaria_mobile.feature.communication.RemoveMemberSheet
import com.project.solaria_mobile.feature.communication.domain.AvatarTone
import com.project.solaria_mobile.feature.communication.domain.CommunityGroup
import com.project.solaria_mobile.feature.communication.domain.CommunityPerson
import com.project.solaria_mobile.feature.communication.domain.Conversation
import com.project.solaria_mobile.feature.communication.domain.ProjectCommunity
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ChatCommunityUiTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun archivedCommunityReplacesTheComposerWithAReadOnlyNotice() {
        val conversation = Conversation("c1", "E", "Equipe", "", "", AvatarTone.BLUE, group = true, communityId = "community", communityStatus = "ARCHIVED")
        compose.setContent { AppTheme { ChatScreen(ChatUiState(conversation), {}, {}, {}) } }
        compose.onNodeWithText(context.getString(R.string.ch_comunidade_arquivada)).assertIsDisplayed()
        compose.onNodeWithContentDescription(context.getString(R.string.ch_enviar_mensagem)).assertDoesNotExist()
    }

    @Test fun activeCommunityStillOffersTheComposer() {
        val conversation = Conversation("c1", "E", "Equipe", "", "", AvatarTone.BLUE, group = true, communityId = "community", communityStatus = "ACTIVE")
        compose.setContent { AppTheme { ChatScreen(ChatUiState(conversation), {}, {}, {}) } }
        compose.onNodeWithContentDescription(context.getString(R.string.ch_enviar_mensagem)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.ch_comunidade_arquivada)).assertDoesNotExist()
    }

    @Test fun removingAManagerRequiresASuccessorForEverySubgroup() {
        val person = CommunityPerson("leaving", "Gestor", true, role = "MEMBER")
        val community = ProjectCommunity("community", "project", groups = listOf(
            CommunityGroup("g1", "Instalação", 2, true, true, "leaving", listOf("leaving", "ana")),
            CommunityGroup("g2", "Manutenção", 2, true, true, "leaving", listOf("leaving", "bia")),
        ), people = listOf(person, CommunityPerson("ana", "Ana", true), CommunityPerson("bia", "Bia", true)))
        var successors: Map<String, String>? = null
        compose.setContent { AppTheme { RemoveMemberSheet(person, community, {}, { successors = it }) } }
        val remove = context.getString(R.string.cm_remover)
        compose.onNodeWithText(remove).assertIsNotEnabled()
        compose.onNodeWithText("Ana").performClick()
        compose.onNodeWithText(remove).assertIsNotEnabled()
        compose.onNodeWithText("Bia").performClick()
        compose.onNodeWithText(remove).assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(mapOf("g1" to "ana", "g2" to "bia"), successors) }
    }
}

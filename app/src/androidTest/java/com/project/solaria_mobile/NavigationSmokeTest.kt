package com.project.solaria_mobile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class NavigationSmokeTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun awaitHome() {
        rule.waitUntil(5_000) { runCatching { rule.onNodeWithText("Opções").assertIsDisplayed(); true }.getOrDefault(false) }
    }

    @Test fun homeOpensProjectsAndReturns() {
        awaitHome()
        rule.onNodeWithText("Projetos").performClick()
        rule.onNodeWithText("Seus projetos").assertIsDisplayed()
        rule.onNodeWithContentDescription("Voltar").performClick()
        rule.onNodeWithText("Opções").assertIsDisplayed()
    }

    @Test fun homeOpensConversationsAndReturns() {
        awaitHome()
        rule.onNodeWithText("Conversas").performClick()
        rule.onNodeWithText("Todas as conversas").assertIsDisplayed()
        rule.onNodeWithContentDescription("Voltar").performClick()
        rule.onNodeWithText("Opções").assertIsDisplayed()
    }

    @Test fun homeOpensScheduleAndReturns() {
        awaitHome()
        rule.onNodeWithText("Agenda").performClick()
        rule.onNodeWithText("Janeiro, 2026").assertIsDisplayed()
        rule.onNodeWithContentDescription("Voltar").performClick()
        rule.onNodeWithText("Opções").assertIsDisplayed()
    }
}

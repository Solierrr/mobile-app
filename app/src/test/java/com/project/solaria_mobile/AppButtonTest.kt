package com.project.solaria_mobile

import com.project.solaria_mobile.core.designsystem.components.ButtonSize
import com.project.solaria_mobile.core.designsystem.components.ButtonVariant
import com.project.solaria_mobile.core.designsystem.components.buttonInteractive
import com.project.solaria_mobile.core.designsystem.components.buttonMinHeight
import com.project.solaria_mobile.core.designsystem.components.buttonPalette
import com.project.solaria_mobile.core.designsystem.theme.LightAppColors
import com.project.solaria_mobile.core.designsystem.theme.Sizing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppButtonTest {
    @Test
    fun primaryUsesActionColor() {
        val palette = buttonPalette(ButtonVariant.Primary, LightAppColors)
        assertEquals(LightAppColors.action, palette.container)
        assertEquals(LightAppColors.onAccent, palette.content)
        assertNull(palette.border)
    }

    @Test
    fun destructiveUsesDangerColor() {
        assertEquals(LightAppColors.danger, buttonPalette(ButtonVariant.Destructive, LightAppColors).container)
    }

    @Test
    fun secondaryHasBorder() {
        assertNotNull(buttonPalette(ButtonVariant.Secondary, LightAppColors).border)
    }

    @Test
    fun loadingBlocksClicks() {
        assertTrue(buttonInteractive(enabled = true, loading = false))
        assertFalse(buttonInteractive(enabled = true, loading = true))
        assertFalse(buttonInteractive(enabled = false, loading = false))
    }

    @Test
    fun sizesRespectTouchTarget() {
        assertEquals(Sizing.actionHeight, buttonMinHeight(ButtonSize.Large))
        assertEquals(Sizing.touchTarget, buttonMinHeight(ButtonSize.Medium))
    }
}

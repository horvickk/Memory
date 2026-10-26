package com.horvickk.memory

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w400dp-h860dp-xhdpi")
class AppNavigationTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun exists(text: String) = rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    private fun pressBack() = rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }

    @Test
    fun navigatesBetweenScreens() {
        assertTrue(exists("УПРАЖНЕНИЯ"))

        // Открыть упражнение и вернуться системной кнопкой «назад».
        rule.onNodeWithText("МАТРИЦА").performClick()
        rule.waitForIdle()
        assertTrue(exists("НАЧАТЬ"))
        pressBack()
        rule.waitForIdle()
        assertTrue(exists("УПРАЖНЕНИЯ"))

        // Последнее упражнение в списке доступно прокруткой.
        rule.onNode(hasScrollAction()).performScrollToNode(hasText("ОЧЕРЁДНОСТЬ"))
        rule.onNodeWithText("ОЧЕРЁДНОСТЬ").performClick()
        rule.waitForIdle()
        assertTrue(exists("НАЧАТЬ"))
        rule.onNodeWithContentDescription("Закрыть").performClick()
        rule.waitForIdle()

        // История и настройки.
        rule.onNode(hasScrollAction()).performScrollToNode(hasText("ВСПЫШКИ"))
        rule.onAllNodes(hasContentDescription("История")).onFirst().performClick()
        rule.waitForIdle()
        assertTrue(exists("ИСТОРИЯ"))
        pressBack()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Настройки").performClick()
        rule.waitForIdle()
        assertTrue(exists("НАСТРОЙКИ"))
        rule.onNodeWithText("ТЕМА").performClick()
        rule.waitForIdle()
        assertTrue(exists("СВЕТЛАЯ"))
    }
}

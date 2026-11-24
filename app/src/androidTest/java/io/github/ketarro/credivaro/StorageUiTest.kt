package io.github.ketarro.credivaro

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import java.util.UUID

/** Synthetic data on isolated CI emulators. No real records. */
class StorageUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private fun label(id: Int) = rule.activity.getString(id)
    private fun waitFor(text: String) = rule.waitUntil(30000) {
        rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    }

    @Test fun saveRecreateReadAndDelete() {
        val title = "Synthetic UI " + UUID.randomUUID().toString().take(8)
        waitFor(label(R.string.add_obligation))
        rule.onNodeWithText(label(R.string.obligations_nav)).performClick()
        waitFor(label(R.string.add_obligation))
        rule.onNodeWithText(label(R.string.add_obligation)).performClick()
        rule.onNodeWithTag("field-${R.string.field_title}").performScrollTo().performTextInput(title)
        rule.onNodeWithTag("save-obligation").performScrollTo().performClick()
        waitFor(title)
        waitFor(label(R.string.edit_obligation))
        rule.onNodeWithText(label(R.string.home)).performClick()
        rule.onNodeWithText(label(R.string.obligations_nav)).performClick()
        waitFor(title)
        rule.activityRule.scenario.recreate()
        waitFor(title)
        rule.onNodeWithText(title).performClick()
        waitFor(label(R.string.edit_obligation))
        rule.onNodeWithText(label(R.string.delete_obligation)).performScrollTo().performClick()
        rule.onNodeWithText(label(R.string.delete), substring = false).performClick()
        rule.waitUntil(30000) { rule.onAllNodesWithText(title).fetchSemanticsNodes().isEmpty() }
        waitFor(label(R.string.add_obligation))
    }
}

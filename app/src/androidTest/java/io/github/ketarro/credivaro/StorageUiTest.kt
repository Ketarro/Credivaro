package io.github.ketarro.credivaro

import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.AnnotatedString
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

    // Semantic actions exercise application behavior without unreliable emulator touch injection.
    // Physical touch/keyboard behavior stays in the manual regression checklist.
    private fun SemanticsNodeInteraction.activate(): SemanticsNodeInteraction =
        performSemanticsAction(SemanticsActions.OnClick) { check(it()) }

    @Test fun saveRecreateReadAndDelete() {
        val title = "Synthetic UI " + UUID.randomUUID().toString().take(8)
        waitFor(label(R.string.add_obligation))
        rule.onNodeWithText(label(R.string.obligations_nav)).activate()
        waitFor(label(R.string.add_obligation))
        rule.onNodeWithText(label(R.string.add_obligation)).activate()
        rule.onNodeWithTag("field-${R.string.field_title}").performScrollTo().performSemanticsAction(SemanticsActions.SetText) { check(it(AnnotatedString(title))) }
        rule.onNodeWithTag("save-obligation").performScrollTo().activate()
        waitFor(title)
        waitFor(label(R.string.edit_obligation))
        rule.onNodeWithText(label(R.string.home)).activate()
        rule.onNodeWithText(label(R.string.obligations_nav)).activate()
        waitFor(title)
        rule.activityRule.scenario.recreate()
        waitFor(title)
        rule.onNodeWithText(title).activate()
        waitFor(label(R.string.edit_obligation))
        rule.onNodeWithText(label(R.string.delete_obligation)).performScrollTo().activate()
        rule.onNodeWithText(label(R.string.delete), substring = false).activate()
        rule.waitUntil(30000) { rule.onAllNodesWithText(title).fetchSemanticsNodes().isEmpty() }
        waitFor(label(R.string.add_obligation))
    }

    @Test fun localeSwitchAndRecreate() {
        // Restore the initial selection after the test to avoid changing other tests' locale.
        rule.onNodeWithText(label(R.string.settings)).activate()
        val initial = rule.onAllNodesWithText(label(R.string.language_short)).fetchSemanticsNodes()
        check(initial.isNotEmpty())
        try {
            rule.onNodeWithText(label(R.string.language_short)).activate()
            rule.onNodeWithText("Русский").activate()
            waitFor("Настройки")
            rule.onNodeWithText("Язык").activate()
            rule.onNodeWithText("English").activate()
            waitFor("Settings")
            rule.activityRule.scenario.recreate()
            waitFor("Settings")
        } finally {
            // Test fixtures start with system locale; restore system via the visible selector.
            rule.onNodeWithText(label(R.string.language_short)).activate()
            rule.onNodeWithText(label(R.string.language_system)).activate()
        }
    }
}

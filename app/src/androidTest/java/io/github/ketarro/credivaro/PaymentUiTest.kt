package io.github.ketarro.credivaro

import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import java.util.UUID

/** Synthetic UI regression. Real account data is never used. */
class PaymentUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private fun label(id: Int) = rule.activity.getString(id)
    private fun waitText(text: String) = rule.waitUntil(30000) {
        rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    }
    private fun waitTag(tag: String) = rule.waitUntil(30000) {
        rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    }
    private fun SemanticsNodeInteraction.activate() =
        performSemanticsAction(SemanticsActions.OnClick) { check(it()) }

    @Test fun recordAndReadPaymentAfterRecreate() {
        val title = "Synthetic payment " + UUID.randomUUID().toString().take(8)
        val note = "Synthetic payment note " + UUID.randomUUID().toString().take(8)
        // The dashboard only offers Add when no obligations exist. Navigate independently.
        waitText(label(R.string.obligations_nav))
        rule.onNodeWithText(label(R.string.obligations_nav)).activate()
        waitText(label(R.string.add_obligation))
        rule.onNodeWithText(label(R.string.add_obligation)).activate()
        rule.onNodeWithTag("field-${R.string.field_title}").performScrollTo()
            .performSemanticsAction(SemanticsActions.SetText) { check(it(AnnotatedString(title))) }
        rule.onNodeWithTag("save-obligation").performScrollTo().activate()
        waitText(label(R.string.payment_empty))
        rule.onNodeWithTag("add-payment").performScrollTo().activate()
        rule.onNodeWithTag("save-payment").activate()
        waitTag("payment-invalid")
        rule.onNodeWithTag("payment-amount").performScrollTo()
            .performSemanticsAction(SemanticsActions.SetText) { check(it(AnnotatedString("123.45"))) }
        rule.onNodeWithTag("payment-notes").performScrollTo()
            .performSemanticsAction(SemanticsActions.SetText) { check(it(AnnotatedString(note))) }
        rule.onNodeWithTag("save-payment").activate()
        waitText(note)
        // Dialog dismissal and IME insets may settle asynchronously on API 26.
        waitText(label(R.string.home))
        rule.onNodeWithText(label(R.string.home)).activate()
        waitText(label(R.string.obligations_nav))
        rule.onNodeWithText(label(R.string.obligations_nav)).activate()
        waitText(title)
        rule.activityRule.scenario.recreate()
        waitText(title)
        rule.onNodeWithText(title).activate()
        waitText(note)
        rule.onNodeWithText(label(R.string.delete_obligation)).performScrollTo().activate()
        rule.onNodeWithText(label(R.string.delete), substring = false).activate()
        rule.waitUntil(30000) { rule.onAllNodesWithText(title).fetchSemanticsNodes().isEmpty() }
    }
}

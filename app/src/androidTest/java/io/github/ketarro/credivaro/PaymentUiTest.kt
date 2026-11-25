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
        // Navigate to the list independently of any earlier synthetic fixture.
        waitText(label(R.string.obligations_nav))
        rule.onNodeWithText(label(R.string.obligations_nav)).activate()
        waitText(label(R.string.add_obligation))
        rule.onNodeWithText(label(R.string.add_obligation)).activate()
        rule.onNodeWithTag("field-${R.string.field_title}").performScrollTo()
            .performSemanticsAction(SemanticsActions.SetText) { check(it(AnnotatedString(title))) }
        rule.onNodeWithTag("save-obligation").performScrollTo().activate()
        waitText(label(R.string.ux_actions))
        rule.onNodeWithTag("credit-tab-payments").performScrollTo().activate()
        waitText(label(R.string.payment_empty))
        rule.onNodeWithTag("add-payment").performScrollTo().activate()
        rule.onNodeWithTag("save-payment").activate()
        waitTag("payment-invalid")
        rule.onNodeWithTag("payment-amount").performScrollTo()
            .performSemanticsAction(SemanticsActions.SetText) { check(it(AnnotatedString("123.45"))) }
        rule.onNodeWithTag("payment-advanced").performScrollTo().activate()
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
        waitText(note)
        // Correction is a new fact, not an overwrite; cancellation retains both rows.
        val replacementNote = "Corrected " + UUID.randomUUID().toString().take(8)
        rule.onNodeWithTag("correct-payment").performScrollTo().activate()
        rule.onNodeWithTag("payment-amount").performScrollTo()
            .performSemanticsAction(SemanticsActions.SetText) { check(it(AnnotatedString("100.00"))) }
        rule.onNodeWithTag("payment-notes").performScrollTo()
            .performSemanticsAction(SemanticsActions.SetText) { check(it(AnnotatedString(replacementNote))) }
        rule.onNodeWithTag("correction-reason").performScrollTo()
            .performSemanticsAction(SemanticsActions.SetText) { check(it(AnnotatedString("Synthetic correction"))) }
        rule.onNodeWithTag("save-payment").activate()
        waitText(replacementNote)
        waitTag("payment-status-REPLACED")
        waitText(note)
        rule.onNodeWithTag("cancel-payment").performScrollTo().activate()
        rule.onNodeWithTag("cancel-reason").performSemanticsAction(SemanticsActions.SetText) { check(it(AnnotatedString("Synthetic cancel"))) }
        rule.onNodeWithTag("confirm-cancel-payment").activate()
        waitTag("payment-status-CANCELLED")
        rule.onAllNodesWithTag("payment-status-ACTIVE").assertCountEquals(0)
        rule.activityRule.scenario.recreate()
        waitText(note)
        waitText(replacementNote)
        waitTag("payment-status-REPLACED")
        waitTag("payment-status-CANCELLED")
        rule.onNodeWithTag("credit-actions").performScrollTo().activate()
        rule.onNodeWithText(label(R.string.delete_obligation)).activate()
        rule.onNodeWithText(label(R.string.delete), substring = false).activate()
        rule.waitUntil(30000) { rule.onAllNodesWithText(title).fetchSemanticsNodes().isEmpty() }
    }
}

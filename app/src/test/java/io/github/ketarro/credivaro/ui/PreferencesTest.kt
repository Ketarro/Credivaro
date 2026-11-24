package io.github.ketarro.credivaro.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class PreferencesTest {
    @Test fun appearanceRoundTrip() {
        Appearance.entries.forEach { assertEquals(it, Appearance.fromStored(it.stored)) }
    }
    @Test fun invalidAppearanceFallsBackToSystem() {
        assertEquals(Appearance.SYSTEM, Appearance.fromStored(null))
        assertEquals(Appearance.SYSTEM, Appearance.fromStored("unknown"))
    }
    @Test fun languageRoundTrip() {
        AppLanguage.entries.forEach { assertEquals(it, AppLanguage.fromTag(it.tag)) }
    }
    @Test fun unknownLanguageFallsBackToSystem() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTag(null))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTag("de"))
    }
}

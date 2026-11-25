package io.github.ketarro.credivaro

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SelfCheckTest {
    @Test fun runSameChecksAsPhysicalDevice() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        SelfCheckSession.start(context)
        val report = withTimeout(300_000) { SelfCheckSession.state.first { !it.running } }
        assertTrue("Self-check incomplete", report.complete)
        assertEquals(SelfCheckSession.ids, report.results.map { it.id })
        assertTrue(SelfCheckSession.reportText(report), report.results.all { it.passed })
    }
}

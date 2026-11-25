package io.github.ketarro.credivaro.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.ketarro.credivaro.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.*
import java.util.Currency
import java.util.UUID
import java.io.File
import java.security.KeyStore

@RunWith(AndroidJUnit4::class)
class EventMigrationCandidateTest {
    @Test fun populatedActualV3RoomDatabaseMigratesAndReopens() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "event-migration-test-" + UUID.randomUUID() + ".db"
        val date = LocalDate.of(2028, 2, 29)
        val clock = Clock.fixed(date.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)
        val owner = UUID.randomUUID()
        val obligation = Obligation(owner, "Synthetic v3", currency = Currency.getInstance("RUB"),
            bankZone = ZoneOffset.UTC, terms = ObligationTerms.Other(), balance = BalanceSnapshot(date, Money("RUB", 0)))
        val payment = RecordedPayment(UUID.randomUUID(), UUID.randomUUID(), owner, date, Money("RUB", 100))
        val password = DatabaseKey.loadOrCreate(context, name)
        try {
            val old = EncryptedStorage.build(context, name, password)
            try {
                ObligationRepository(old, clock).save(obligation)
                PaymentRepository(old, clock).record(payment)
                assertEquals(3, old.openHelper.writableDatabase.version)
            } finally { old.close() }
            fun open() = Room.databaseBuilder(context, EventMigrationCandidateDatabase::class.java, name)
                .openHelperFactory(PreserveFileFactory(password))
                .addMigrations(EVENT_MIGRATION_CANDIDATE_3_4)
                .setJournalMode(androidx.room.RoomDatabase.JournalMode.TRUNCATE).build()
            repeat(2) {
                val candidate = open()
                try {
                    val db = candidate.openHelper.writableDatabase
                    assertEquals(4, db.version)
                    assertEquals("Synthetic v3", candidate.dao().find(owner.toString())?.row?.title)
                    assertEquals(0L, candidate.dao().find(owner.toString())?.totalDebt)
                    assertEquals(100L, candidate.paymentDao().findPayment(payment.id.toString())?.amount)
                    db.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
                    db.query("PRAGMA integrity_check").use { assertTrue(it.moveToFirst()); assertEquals("ok", it.getString(0)) }
                    val window = EventWindow(date, date.plusDays(2))
                    val rule = EventRule(UUID.nameUUIDFromBytes("series".toByteArray()), UUID.nameUUIDFromBytes("revision".toByteArray()),
                        owner, EventType.REQUIRED_PAYMENT, MonetaryRole.MANDATORY, Recurrence.OneOff(date),
                        ZoneOffset.UTC, date, currency = "RUB", amount = Money("RUB",100))
                    val store = EventStorageCandidate(db, clock)
                    store.appendRule(rule, window)
                    store.reconcile(owner, window)
                    store.linkPayment(OccurrenceKey(rule.seriesId,"once").id, payment.id)
                    db.query("SELECT COUNT(*) FROM event_payment_link").use { assertTrue(it.moveToFirst()); assertEquals(1,it.getInt(0)) }
                } finally { candidate.close() }
            }
        } finally {
            password.fill(0)
            context.deleteDatabase(name)
            val envelope = DatabaseKey.envelope(context,name)
            listOf("",".bak",".new").forEach { File(envelope.path + it).delete() }
            KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry(DatabaseKey.alias(name)) }
        }
    }
}

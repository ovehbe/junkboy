package com.ovehbe.junkboy

import com.ovehbe.junkboy.database.AutoReplyLog
import com.ovehbe.junkboy.database.AutoReplyLogDao
import com.ovehbe.junkboy.utils.AutoReplyEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoReplyEngineTest {

    private class FakeDao : AutoReplyLogDao {
        val rows = mutableListOf<AutoReplyLog>()
        override suspend fun insert(log: AutoReplyLog): Long { rows.add(log); return rows.size.toLong() }
        override suspend fun getLastReplyAt(senderNumber: String): Long? =
            rows.filter { it.senderNumber == senderNumber }.maxOfOrNull { it.repliedAt }
        override fun getRecent(): Flow<List<AutoReplyLog>> = flowOf(rows.reversed())
        override suspend fun clear() = rows.clear()
    }

    private val dao = FakeDao()
    private val sent = mutableListOf<Pair<String, String>>()
    private var now = 1_000_000_000L
    private var enabled = true
    private var permitted = true

    private val engine = AutoReplyEngine(
        dao = dao,
        isEnabled = { enabled },
        hasSendPermission = { permitted },
        dispatch = { number, text -> sent.add(number to text) },
        clock = { now }
    )

    @Test
    fun sendsOnceAndLogs() = runBlocking {
        assertTrue(engine.sendStopReply("12345"))
        assertEquals(listOf("12345" to "STOP"), sent)
        assertEquals(1, dao.rows.size)
    }

    @Test
    fun secondReplyWithin24hIsBlocked() = runBlocking {
        engine.sendStopReply("+1 (555) 123-4567")
        now += AutoReplyEngine.COOLDOWN_MS - 1
        assertFalse(engine.sendStopReply("+15551234567"))
        assertEquals(1, sent.size)
    }

    @Test
    fun replyAllowedAfter24h() = runBlocking {
        engine.sendStopReply("12345")
        now += AutoReplyEngine.COOLDOWN_MS
        assertTrue(engine.sendStopReply("12345"))
        assertEquals(2, sent.size)
    }

    @Test
    fun cooldownIsPerSender() = runBlocking {
        engine.sendStopReply("12345")
        assertTrue(engine.sendStopReply("67890"))
    }

    @Test
    fun respectsSettingAndPermission() = runBlocking {
        enabled = false
        assertFalse(engine.sendStopReply("12345"))
        enabled = true
        permitted = false
        assertFalse(engine.sendStopReply("12345"))
        assertTrue(sent.isEmpty())
    }

    @Test
    fun rejectsNonNumericSenders() = runBlocking {
        assertFalse(engine.sendStopReply("BANKNAME"))
        assertFalse(engine.sendStopReply("Unknown"))
        assertTrue(sent.isEmpty())
    }

    @Test
    fun failedDispatchIsNotLogged() = runBlocking {
        val failing = AutoReplyEngine(dao, { true }, { true }, { _, _ -> error("radio off") }, { now })
        assertFalse(failing.sendStopReply("12345"))
        assertTrue(dao.rows.isEmpty())
    }

    @Test
    fun normalizeTarget() {
        assertEquals("+15551234567", AutoReplyEngine.normalizeTarget("+1 (555) 123-4567"))
        assertEquals("88888", AutoReplyEngine.normalizeTarget("88888"))
        assertNull(AutoReplyEngine.normalizeTarget("AB-Shop"))
        assertNull(AutoReplyEngine.normalizeTarget("12"))
    }
}

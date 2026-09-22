package net.mada.lumea.domain.agenda

import net.mada.lumea.data.db.EventEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class RecurrenceTest {

    private val zone: ZoneId = ZoneId.systemDefault()

    private fun millis(iso: String) =
        LocalDateTime.parse(iso).atZone(zone).toInstant().toEpochMilli()

    private fun event(start: String, end: String, repeat: String) = EventEntity(
        id = 1,
        title = "Test",
        startAt = millis(start),
        endAt = millis(end),
        repeat = repeat,
    )

    @Test
    fun `un evenement ponctuel n'apparait que dans sa fenetre`() {
        val e = event("2026-03-10T09:00", "2026-03-10T10:00", "NONE")

        assertEquals(1, Recurrence.expand(e, millis("2026-03-01T00:00"), millis("2026-03-31T23:59")).size)
        assertTrue(Recurrence.expand(e, millis("2026-04-01T00:00"), millis("2026-04-30T23:59")).isEmpty())
    }

    @Test
    fun `une repetition hebdomadaire produit une occurrence par semaine`() {
        val e = event("2026-03-02T09:00", "2026-03-02T10:00", "WEEKLY")

        val occurrences = Recurrence.expand(e, millis("2026-03-01T00:00"), millis("2026-03-31T23:59"))

        assertEquals(5, occurrences.size) // 2, 9, 16, 23, 30 mars
        assertEquals(millis("2026-03-02T09:00"), occurrences.first().startAt)
    }

    @Test
    fun `la duree est conservee sur chaque occurrence`() {
        val e = event("2026-03-02T09:00", "2026-03-02T10:30", "DAILY")

        Recurrence.expand(e, millis("2026-03-05T00:00"), millis("2026-03-08T23:59")).forEach {
            assertEquals(90 * 60_000L, it.endAt - it.startAt)
        }
    }

    @Test
    fun `aucune occurrence n'est produite avant la premiere date`() {
        val e = event("2026-03-10T09:00", "2026-03-10T10:00", "DAILY")

        val occurrences = Recurrence.expand(e, millis("2026-01-01T00:00"), millis("2026-03-12T23:59"))

        assertTrue(occurrences.all { it.startAt >= e.startAt })
        assertEquals(3, occurrences.size) // 10, 11, 12 mars
    }

    @Test
    fun `la prochaine occurrence est strictement posterieure au moment donne`() {
        val e = event("2026-03-02T09:00", "2026-03-02T10:00", "WEEKLY")

        val next = Recurrence.nextOccurrence(e, millis("2026-03-09T09:00"))

        assertEquals(millis("2026-03-16T09:00"), next)
    }

    @Test
    fun `un evenement ponctuel deja passe n'a pas de prochaine occurrence`() {
        val e = event("2026-03-02T09:00", "2026-03-02T10:00", "NONE")

        assertNull(Recurrence.nextOccurrence(e, millis("2026-03-05T00:00")))
    }

    @Test
    fun `une repetition annuelle lointaine reste calculable`() {
        val e = event("2020-06-15T09:00", "2020-06-15T10:00", "YEARLY")

        val next = Recurrence.nextOccurrence(e, millis("2026-03-01T00:00"))

        assertEquals(millis("2026-06-15T09:00"), next)
    }
}

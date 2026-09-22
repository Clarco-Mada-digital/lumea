package net.mada.lumea.domain.agenda

import net.mada.lumea.data.db.EventEntity
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Développe un événement récurrent en occurrences concrètes.
 *
 * Les répétitions ne sont pas stockées en base : seule la première occurrence l'est,
 * avec sa règle. Les suivantes sont calculées à la volée pour l'intervalle affiché,
 * ce qui évite de remplir la base de milliers de lignes pour un anniversaire annuel.
 */
object Recurrence {

    private const val MAX_OCCURRENCES = 400

    fun expand(event: EventEntity, fromMillis: Long, toMillis: Long): List<EventEntity> {
        if (event.repeat == "NONE") {
            return if (event.startAt in fromMillis..toMillis) listOf(event) else emptyList()
        }

        val zone = ZoneId.systemDefault()
        val duration = (event.endAt - event.startAt).coerceAtLeast(0)
        var current = Instant.ofEpochMilli(event.startAt).atZone(zone)
        val to = Instant.ofEpochMilli(toMillis).atZone(zone)

        // On saute directement au voisinage de la fenêtre au lieu d'itérer depuis l'origine.
        current = fastForward(current, event.repeat, Instant.ofEpochMilli(fromMillis).atZone(zone))

        val result = mutableListOf<EventEntity>()
        var guard = 0
        while (!current.isAfter(to) && guard++ < MAX_OCCURRENCES) {
            val startMillis = current.toInstant().toEpochMilli()
            if (startMillis >= fromMillis && startMillis >= event.startAt) {
                result += event.copy(startAt = startMillis, endAt = startMillis + duration)
            }
            current = current.advance(event.repeat)
        }
        return result
    }

    /** Prochaine occurrence strictement après [afterMillis], ou null si la règle est « jamais ». */
    fun nextOccurrence(event: EventEntity, afterMillis: Long): Long? {
        if (event.repeat == "NONE") return event.startAt.takeIf { it > afterMillis }
        val zone = ZoneId.systemDefault()
        var current = Instant.ofEpochMilli(event.startAt).atZone(zone)
        val after = Instant.ofEpochMilli(afterMillis).atZone(zone)
        current = fastForward(current, event.repeat, after)
        var guard = 0
        while (guard++ < MAX_OCCURRENCES) {
            val millis = current.toInstant().toEpochMilli()
            if (millis > afterMillis) return millis
            current = current.advance(event.repeat)
        }
        return null
    }

    private fun fastForward(
        start: ZonedDateTime,
        repeat: String,
        target: ZonedDateTime,
    ): ZonedDateTime {
        if (!start.isBefore(target)) return start
        val days = java.time.temporal.ChronoUnit.DAYS.between(start, target)
        return when (repeat) {
            "DAILY" -> start.plusDays(days)
            "WEEKLY" -> start.plusWeeks(days / 7)
            "MONTHLY" -> start.plusMonths(days / 31)
            "YEARLY" -> start.plusYears(days / 366)
            else -> start
        }
    }

    private fun ZonedDateTime.advance(repeat: String): ZonedDateTime = when (repeat) {
        "DAILY" -> plusDays(1)
        "WEEKLY" -> plusWeeks(1)
        "MONTHLY" -> plusMonths(1)
        "YEARLY" -> plusYears(1)
        else -> plusYears(100) // règle inconnue : on sort de la boucle au tour suivant
    }
}

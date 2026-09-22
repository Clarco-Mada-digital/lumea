package net.mada.lumea.data.repo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import net.mada.lumea.domain.agenda.Recurrence
import net.mada.lumea.data.db.DailyLogDao
import net.mada.lumea.data.db.DailyLogEntity
import net.mada.lumea.data.db.EventDao
import net.mada.lumea.data.db.EventEntity
import net.mada.lumea.data.db.FolderDao
import net.mada.lumea.data.db.FolderEntity
import net.mada.lumea.data.db.HabitCheckEntity
import net.mada.lumea.data.db.HabitDao
import net.mada.lumea.data.db.HabitEntity
import net.mada.lumea.data.db.NoteDao
import net.mada.lumea.data.db.NoteEntity
import net.mada.lumea.data.db.PeriodDao
import net.mada.lumea.data.db.PregnancyDao
import net.mada.lumea.data.db.PrenatalCareDao
import net.mada.lumea.data.db.PrenatalCareEntity
import net.mada.lumea.data.db.PregnancyEntity
import net.mada.lumea.data.db.PeriodEntity
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class NoteRepository(private val notes: NoteDao, private val folders: FolderDao) {

    fun observeNotes(query: String, folderId: Long?, favoritesOnly: Boolean, archived: Boolean) =
        notes.observeNotes(query, folderId, favoritesOnly, archived)

    fun observeNote(id: Long) = notes.observeNote(id)
    fun observeNotesForDate(date: LocalDate) = notes.observeNotesForDate(date)
    fun observeFolders() = folders.observeAll()
    fun countActive() = notes.countActive()

    suspend fun get(id: Long) = notes.getNote(id)
    suspend fun save(note: NoteEntity) = notes.upsert(note.copy(updatedAt = System.currentTimeMillis()))
    suspend fun delete(id: Long) = notes.delete(id)

    suspend fun setPinned(note: NoteEntity, pinned: Boolean) = notes.upsert(note.copy(isPinned = pinned))
    suspend fun setFavorite(note: NoteEntity, favorite: Boolean) = notes.upsert(note.copy(isFavorite = favorite))
    suspend fun setArchived(note: NoteEntity, archived: Boolean) = notes.upsert(note.copy(isArchived = archived))

    suspend fun saveFolder(folder: FolderEntity) = folders.upsert(folder)
    suspend fun deleteFolder(folder: FolderEntity) {
        folders.detachNotes(folder.id)
        folders.delete(folder)
    }
}

class EventRepository(private val dao: EventDao) {

    fun observeForMonth(anchor: LocalDate): Flow<List<EventEntity>> {
        val first = anchor.withDayOfMonth(1).minusDays(7)
        val last = anchor.withDayOfMonth(anchor.lengthOfMonth()).plusDays(7)
        return observeRange(first.atStartOfDayMillis(), last.endOfDayMillis())
    }

    fun observeForDay(day: LocalDate): Flow<List<EventEntity>> =
        observeRange(day.atStartOfDayMillis(), day.endOfDayMillis())

    /**
     * Les événements ponctuels viennent de la base, les répétés sont développés
     * à la volée sur l'intervalle demandé puis fusionnés dans l'ordre horaire.
     */
    private fun observeRange(from: Long, to: Long): Flow<List<EventEntity>> =
        combine(dao.observeBetween(from, to), dao.observeRecurring()) { single, recurring ->
            val expanded = recurring.flatMap { Recurrence.expand(it, from, to) }
            // `single` peut déjà contenir la première occurrence d'un événement répété.
            val singleWithoutRecurring = single.filter { it.repeat == "NONE" }
            (singleWithoutRecurring + expanded).sortedBy { it.startAt }
        }

    fun observeUpcoming(limit: Int = 5): Flow<List<EventEntity>> {
        val now = System.currentTimeMillis()
        val horizon = now + 90L * 24 * 3_600_000
        return observeRange(now, horizon).map { it.take(limit) }
    }

    suspend fun get(id: Long) = dao.get(id)
    suspend fun save(event: EventEntity) = dao.upsert(event)
    suspend fun setDone(id: Long, done: Boolean) = dao.setDone(id, done)
    suspend fun delete(id: Long) = dao.delete(id)

    /**
     * Les alarmes à (re)poser : les événements ponctuels à venir, plus la prochaine
     * occurrence de chaque série.
     */
    suspend fun pendingReminders(): List<EventEntity> {
        val now = System.currentTimeMillis()
        val single = dao.getPendingReminders(now).filter { it.repeat == "NONE" }
        val recurring = dao.getRecurring().mapNotNull { event ->
            if (event.reminderMinutes == null) return@mapNotNull null
            val next = Recurrence.nextOccurrence(event, now) ?: return@mapNotNull null
            event.copy(startAt = next, endAt = next + (event.endAt - event.startAt))
        }
        return single + recurring
    }
}

/**
 * Les tests de grossesse et le suivi qui peut en découler.
 *
 * Volontairement à part du cycle : une grossesse n'est pas un cycle, et mélanger
 * les deux rendrait les deux illisibles.
 */
class PregnancyRepository(
    private val dao: PregnancyDao,
    private val careDao: PrenatalCareDao,
) {

    fun observeCare(pregnancyId: Long) = careDao.observeFor(pregnancyId)

    /** Coche ou décoche un acte du carnet. Décocher efface la ligne, pas l'historique. */
    suspend fun setCareDone(
        pregnancyId: Long,
        code: String,
        done: Boolean,
        on: LocalDate = LocalDate.now(),
    ) {
        if (!done) {
            careDao.remove(pregnancyId, code)
            return
        }
        val existing = careDao.find(pregnancyId, code)
        careDao.upsert(
            existing?.copy(doneOn = on)
                ?: PrenatalCareEntity(pregnancyId = pregnancyId, code = code, doneOn = on)
        )
    }

    /** Enregistre un résultat : groupe sanguin, terme d'échographie, hémoglobine. */
    suspend fun setCareNote(pregnancyId: Long, code: String, note: String) {
        val existing = careDao.find(pregnancyId, code)
        careDao.upsert(
            existing?.copy(note = note)
                ?: PrenatalCareEntity(
                    pregnancyId = pregnancyId,
                    code = code,
                    doneOn = LocalDate.now(),
                    note = note,
                )
        )
    }

    /** Coordonnées du soignant qui suit la grossesse. */
    suspend fun setCaregiver(
        id: Long,
        name: String,
        role: String,
        phone: String,
        facility: String,
    ) {
        val current = dao.getAll().firstOrNull { it.id == id } ?: return
        dao.upsert(
            current.copy(
                caregiverName = name,
                caregiverRole = role,
                caregiverPhone = phone,
                facility = facility,
            )
        )
    }

    /**
     * L'enfant est né : le suivi ne s'arrête pas, il change de nature.
     *
     * On passe en POSTPARTUM plutôt qu'en ENDED, parce que c'est précisément la
     * période où la question « est-ce que je peux retomber enceinte ? » se pose, et
     * où presque aucune application ne répond.
     */
    suspend fun recordBirth(id: Long, on: LocalDate) {
        val current = dao.getAll().firstOrNull { it.id == id } ?: return
        dao.upsert(current.copy(status = "POSTPARTUM", birthDate = on))
    }

    /**
     * Le retour de couches : les premières règles après l'accouchement.
     *
     * C'est le point de bouclage de toute l'app. Il clôt le suivi et ouvre une
     * nouvelle période de règles, ce qui fait repartir le moteur de cycle et
     * rétablit les vues d'avant — anneau, calendrier des risques, prévisions.
     */
    suspend fun endPostpartum(id: Long, on: LocalDate) {
        val current = dao.getAll().firstOrNull { it.id == id } ?: return
        dao.upsert(current.copy(status = "ENDED", endedOn = on))
    }

    suspend fun setRiskFactors(id: Long, codes: Set<String>) {
        val current = dao.getAll().firstOrNull { it.id == id } ?: return
        dao.upsert(current.copy(riskFactors = codes.joinToString(",")))
    }

    fun observeOngoing() = dao.observeOngoing()
    fun observeAll() = dao.observeAll()
    suspend fun latest() = dao.latest()

    /**
     * Enregistre un test. Un résultat positif ouvre le suivi ; les autres se
     * contentent d'être notés, pour savoir qu'on a déjà testé et quand.
     */
    suspend fun recordTest(
        result: String,
        lastPeriodStart: LocalDate?,
        testedOn: LocalDate = LocalDate.now(),
    ): Long = dao.upsert(
        PregnancyEntity(
            testedOn = testedOn,
            result = result,
            lastPeriodStart = lastPeriodStart,
            status = if (result == "POSITIVE") "ONGOING" else "NONE",
        )
    )

    /**
     * Clôt le suivi, sans jamais demander pourquoi.
     *
     * Une grossesse s'arrête pour des raisons qui ne regardent pas une application.
     * Le suivi se ferme, les données restent, et rien ne s'affiche de plus.
     */
    suspend fun endFollowUp(id: Long, on: LocalDate = LocalDate.now()) {
        val all = dao.getAll().firstOrNull { it.id == id } ?: return
        dao.upsert(all.copy(status = "ENDED", endedOn = on))
    }

    suspend fun delete(id: Long) = dao.delete(id)
}

class CycleRepository(private val periods: PeriodDao, private val logs: DailyLogDao) {

    fun observePeriods() = periods.observeAll()
    suspend fun allPeriods() = periods.getAll()

    fun observeLog(date: LocalDate) = logs.observe(date)
    fun observeLogs(from: LocalDate, to: LocalDate) = logs.observeBetween(from, to)

    suspend fun log(date: LocalDate) = logs.get(date)
    suspend fun saveLog(log: DailyLogEntity) = logs.upsert(log.copy(updatedAt = System.currentTimeMillis()))

    /**
     * Clôt les règles en cours au [endDate] donné.
     *
     * Une période sans date de fin ne compte pas dans la durée moyenne : tant
     * qu'elle n'est pas fermée, l'app ne sait pas combien de temps les règles ont
     * duré. C'est ce que « Mes règles sont finies » vient renseigner.
     */
    suspend fun endCurrentPeriod(endDate: LocalDate) {
        if (endDate.isAfter(LocalDate.now())) return
        val open = periods.getAll()
            .filter { it.endDate == null }
            .maxByOrNull { it.startDate } ?: return
        if (endDate.isBefore(open.startDate)) return
        periods.upsert(open.copy(endDate = endDate))
    }

    /** Rouvre les dernières règles : on les avait déclarées finies trop tôt. */
    suspend fun reopenLastPeriod() {
        val last = periods.getAll().maxByOrNull { it.startDate } ?: return
        periods.upsert(last.copy(endDate = null))
    }

    /**
     * Enregistre un nouveau premier jour de règles, sans date de fin.
     *
     * Une date future est refusée. On ne confirme que ce qui est arrivé : marquer
     * des règles à venir fausserait la moyenne des cycles et ferait disparaître le
     * retard, puisque le moteur croirait la confirmation déjà donnée.
     */
    suspend fun startPeriod(date: LocalDate) {
        if (date.isAfter(LocalDate.now())) return
        val all = periods.getAll()
        if (all.any { it.startDate == date }) return
        periods.upsert(PeriodEntity(startDate = date, endDate = null))
    }

    /**
     * Bascule le marquage « règles » d'un jour.
     *
     * Marquer un jour l'étend à la période existante s'il la touche (la veille ou le
     * lendemain), sinon il ouvre une nouvelle période. Démarquer coupe la période :
     * début, fin, ou scission en deux si le jour était au milieu.
     */
    suspend fun togglePeriodDay(date: LocalDate, defaultLength: Int) {
        // Même règle que pour [startPeriod] : on n'enregistre pas un saignement
        // qui n'a pas encore eu lieu. Décocher un jour passé reste possible.
        val existing = periods.getAll()
        val alreadyMarked = existing.any { p ->
            val end = p.endDate ?: p.startDate.plusDays((defaultLength - 1).toLong())
            !date.isBefore(p.startDate) && !date.isAfter(end)
        }
        if (date.isAfter(LocalDate.now()) && !alreadyMarked) return

        val all = existing.sortedBy { it.startDate }
        val containing = all.firstOrNull { p ->
            val end = p.endDate ?: p.startDate.plusDays((defaultLength - 1).toLong())
            !date.isBefore(p.startDate) && !date.isAfter(end)
        }

        if (containing != null) {
            val end = containing.endDate ?: containing.startDate.plusDays((defaultLength - 1).toLong())
            when {
                containing.startDate == end -> periods.delete(containing.id)
                date == containing.startDate ->
                    periods.upsert(containing.copy(startDate = date.plusDays(1), endDate = end))
                date == end ->
                    periods.upsert(containing.copy(endDate = date.minusDays(1)))
                else -> {
                    periods.upsert(containing.copy(endDate = date.minusDays(1)))
                    periods.upsert(PeriodEntity(startDate = date.plusDays(1), endDate = end))
                }
            }
            return
        }

        val before = all.lastOrNull { (it.endDate ?: it.startDate) == date.minusDays(1) }
        val after = all.firstOrNull { it.startDate == date.plusDays(1) }
        when {
            before != null && after != null -> {
                periods.upsert(before.copy(endDate = after.endDate ?: after.startDate))
                periods.delete(after.id)
            }
            before != null -> periods.upsert(before.copy(endDate = date))
            after != null -> {
                periods.delete(after.id)
                periods.upsert(after.copy(id = 0, startDate = date))
            }
            // Pas de date de fin : les règles viennent de commencer, on ne sait pas
            // encore combien de temps elles vont durer. Le calendrier affiche la durée
            // habituelle, et la moyenne ne compte que les périodes réellement closes —
            // sinon marquer le jour 1 ferait croire à des règles d'un seul jour.
            else -> periods.upsert(PeriodEntity(startDate = date, endDate = null))
        }
    }

    suspend fun deletePeriod(id: Long) = periods.delete(id)

    /**
     * Enregistre une période complète. Si une période démarre déjà ce jour-là, on la
     * met à jour au lieu d'en créer une seconde — l'index unique sur `startDate`
     * refuserait le doublon.
     */
    suspend fun savePeriod(start: LocalDate, end: LocalDate?) {
        val existing = periods.getAll().firstOrNull { it.startDate == start }
        periods.upsert(
            existing?.copy(endDate = end) ?: PeriodEntity(startDate = start, endDate = end)
        )
    }
}

class JournalRepository(private val logs: DailyLogDao, private val habits: HabitDao) {

    fun observeEntries(limit: Int = 200) = logs.observeJournal(limit)
    fun observeLog(date: LocalDate) = logs.observe(date)
    fun observeLogs(from: LocalDate, to: LocalDate) = logs.observeBetween(from, to)
    suspend fun saveLog(log: DailyLogEntity) = logs.upsert(log.copy(updatedAt = System.currentTimeMillis()))

    fun observeHabits() = habits.observeActive()
    fun observeChecks(date: LocalDate) = habits.observeChecks(date)
    fun observeChecksBetween(from: LocalDate, to: LocalDate) = habits.observeChecksBetween(from, to)

    suspend fun saveHabit(habit: HabitEntity) = habits.upsert(habit)
    suspend fun deleteHabit(id: Long) = habits.delete(id)

    suspend fun toggleHabit(habitId: Long, date: LocalDate, checked: Boolean) {
        if (checked) habits.check(HabitCheckEntity(habitId, date)) else habits.uncheck(habitId, date)
    }
}

fun LocalDate.atStartOfDayMillis(): Long =
    atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

fun LocalDate.endOfDayMillis(): Long =
    atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

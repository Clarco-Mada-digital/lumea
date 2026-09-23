package net.mada.lumea.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY position, name")
    fun observeAll(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders ORDER BY position, name")
    suspend fun getAll(): List<FolderEntity>

    @Upsert
    suspend fun upsert(folder: FolderEntity): Long

    @Delete
    suspend fun delete(folder: FolderEntity)

    /** Les notes du dossier supprimé retournent dans « Sans dossier ». */
    @Query("UPDATE notes SET folderId = NULL WHERE folderId = :folderId")
    suspend fun detachNotes(folderId: Long)
}

@Dao
interface NoteDao {
    @Query(
        """
        SELECT * FROM notes
        WHERE isArchived = :archived
          AND (:folderId IS NULL OR folderId = :folderId)
          AND (:favoritesOnly = 0 OR isFavorite = 1)
          AND (:query = '' OR title LIKE '%' || :query || '%'
               OR body LIKE '%' || :query || '%'
               OR tags LIKE '%' || :query || '%')
        ORDER BY isPinned DESC, updatedAt DESC
        """
    )
    fun observeNotes(
        query: String,
        folderId: Long?,
        favoritesOnly: Boolean,
        archived: Boolean,
    ): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    fun observeNote(id: Long): Flow<NoteEntity?>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNote(id: Long): NoteEntity?

    @Query("SELECT * FROM notes WHERE linkedDate = :date AND isArchived = 0 ORDER BY updatedAt DESC")
    fun observeNotesForDate(date: LocalDate): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes")
    suspend fun getAll(): List<NoteEntity>

    @Query("SELECT COUNT(*) FROM notes WHERE isArchived = 0")
    fun countActive(): Flow<Int>

    @Upsert
    suspend fun upsert(note: NoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notes: List<NoteEntity>)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM notes")
    suspend fun clear()
}

@Dao
interface EventDao {
    @Query("SELECT * FROM events WHERE startAt BETWEEN :from AND :to ORDER BY startAt")
    fun observeBetween(from: Long, to: Long): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE startAt >= :from ORDER BY startAt LIMIT :limit")
    fun observeUpcoming(from: Long, limit: Int): Flow<List<EventEntity>>

    /** Les événements répétés vivent hors de la fenêtre interrogée : on les prend tous. */
    @Query("SELECT * FROM events WHERE repeat != 'NONE'")
    fun observeRecurring(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE repeat != 'NONE'")
    suspend fun getRecurring(): List<EventEntity>

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun get(id: Long): EventEntity?

    @Query("SELECT * FROM events")
    suspend fun getAll(): List<EventEntity>

    @Query("SELECT * FROM events WHERE reminderMinutes IS NOT NULL AND startAt >= :from ORDER BY startAt LIMIT 60")
    suspend fun getPendingReminders(from: Long): List<EventEntity>

    @Upsert
    suspend fun upsert(event: EventEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<EventEntity>)

    @Query("UPDATE events SET isDone = :done WHERE id = :id")
    suspend fun setDone(id: Long, done: Boolean)

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM events")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM events WHERE source = :source")
    suspend fun countBySource(source: String): Int

    @Query("DELETE FROM events WHERE source = :source")
    suspend fun deleteBySource(source: String)
}

@Dao
interface PeriodDao {
    @Query("SELECT * FROM periods ORDER BY startDate DESC")
    fun observeAll(): Flow<List<PeriodEntity>>

    @Query("SELECT * FROM periods ORDER BY startDate DESC")
    suspend fun getAll(): List<PeriodEntity>

    @Query("SELECT * FROM periods ORDER BY startDate DESC LIMIT 1")
    suspend fun getLatest(): PeriodEntity?

    @Upsert
    suspend fun upsert(period: PeriodEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(periods: List<PeriodEntity>)

    @Query("DELETE FROM periods WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM periods")
    suspend fun clear()
}

@Dao
interface DailyLogDao {
    @Query("SELECT * FROM daily_logs WHERE date = :date")
    fun observe(date: LocalDate): Flow<DailyLogEntity?>

    @Query("SELECT * FROM daily_logs WHERE date = :date")
    suspend fun get(date: LocalDate): DailyLogEntity?

    @Query("SELECT * FROM daily_logs WHERE date BETWEEN :from AND :to ORDER BY date DESC")
    fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<DailyLogEntity>>

    @Query("SELECT * FROM daily_logs WHERE journal != '' OR gratitude != '' OR mood > 0 ORDER BY date DESC LIMIT :limit")
    fun observeJournal(limit: Int): Flow<List<DailyLogEntity>>

    @Query("SELECT * FROM daily_logs")
    suspend fun getAll(): List<DailyLogEntity>

    @Upsert
    suspend fun upsert(log: DailyLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<DailyLogEntity>)

    @Query("DELETE FROM daily_logs")
    suspend fun clear()
}

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE isActive = 1 ORDER BY position, id")
    fun observeActive(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits")
    suspend fun getAll(): List<HabitEntity>

    @Upsert
    suspend fun upsert(habit: HabitEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(habits: List<HabitEntity>)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM habit_checks WHERE date = :date")
    fun observeChecks(date: LocalDate): Flow<List<HabitCheckEntity>>

    @Query("SELECT * FROM habit_checks WHERE date BETWEEN :from AND :to")
    fun observeChecksBetween(from: LocalDate, to: LocalDate): Flow<List<HabitCheckEntity>>

    @Query("SELECT * FROM habit_checks")
    suspend fun getAllChecks(): List<HabitCheckEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun check(check: HabitCheckEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllChecks(checks: List<HabitCheckEntity>)

    @Query("DELETE FROM habit_checks WHERE habitId = :habitId AND date = :date")
    suspend fun uncheck(habitId: Long, date: LocalDate)

    @Query("DELETE FROM habits")
    suspend fun clearHabits()

    @Query("DELETE FROM habit_checks")
    suspend fun clearChecks()
}

@Dao
interface PregnancyDao {

    @Query("SELECT * FROM pregnancies ORDER BY testedOn DESC")
    fun observeAll(): Flow<List<PregnancyEntity>>

    @Query("SELECT * FROM pregnancies ORDER BY testedOn DESC")
    suspend fun getAll(): List<PregnancyEntity>

    @Query("SELECT * FROM pregnancies WHERE status = 'ONGOING' ORDER BY testedOn DESC LIMIT 1")
    fun observeOngoing(): Flow<PregnancyEntity?>

    @Query("SELECT * FROM pregnancies ORDER BY testedOn DESC LIMIT 1")
    suspend fun latest(): PregnancyEntity?

    @Upsert
    suspend fun upsert(pregnancy: PregnancyEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PregnancyEntity>)

    @Query("DELETE FROM pregnancies WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM pregnancies")
    suspend fun clear()
}

@Dao
interface PrenatalCareDao {

    @Query("SELECT * FROM prenatal_care WHERE pregnancyId = :pregnancyId ORDER BY doneOn")
    fun observeFor(pregnancyId: Long): Flow<List<PrenatalCareEntity>>

    @Query("SELECT * FROM prenatal_care")
    suspend fun getAll(): List<PrenatalCareEntity>

    @Query("SELECT * FROM prenatal_care WHERE pregnancyId = :pregnancyId AND code = :code LIMIT 1")
    suspend fun find(pregnancyId: Long, code: String): PrenatalCareEntity?

    @Upsert
    suspend fun upsert(care: PrenatalCareEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PrenatalCareEntity>)

    @Query("DELETE FROM prenatal_care WHERE pregnancyId = :pregnancyId AND code = :code")
    suspend fun remove(pregnancyId: Long, code: String)

    @Query("DELETE FROM prenatal_care")
    suspend fun clear()
}

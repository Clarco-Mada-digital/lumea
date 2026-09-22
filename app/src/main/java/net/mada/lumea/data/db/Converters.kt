package net.mada.lumea.data.db

import androidx.room.TypeConverter
import java.time.LocalDate

class Converters {
    /** Les dates sont stockées en jours epoch : trié naturellement, comparable en SQL. */
    @TypeConverter
    fun dateToLong(value: LocalDate?): Long? = value?.toEpochDay()

    @TypeConverter
    fun longToDate(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)
}

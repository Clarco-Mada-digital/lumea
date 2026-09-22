package net.mada.lumea.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import net.mada.lumea.data.security.DatabaseKeyProvider
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [
        NoteEntity::class,
        FolderEntity::class,
        EventEntity::class,
        PeriodEntity::class,
        DailyLogEntity::class,
        HabitEntity::class,
        HabitCheckEntity::class,
        PregnancyEntity::class,
        PrenatalCareEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class LumeaDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun folderDao(): FolderDao
    abstract fun eventDao(): EventDao
    abstract fun periodDao(): PeriodDao
    abstract fun dailyLogDao(): DailyLogDao
    abstract fun habitDao(): HabitDao
    abstract fun pregnancyDao(): PregnancyDao
    abstract fun prenatalCareDao(): PrenatalCareDao

    companion object {
        private const val NAME = "lumea.db"

        /** Ajout du verrou par note et par journée. Aucune donnée existante n'est touchée. */
        internal val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN isLocked INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE daily_logs ADD COLUMN isLocked INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * Ajout du suivi de grossesse. Table neuve : rien d'existant n'est modifié,
         * et une base qui n'en a jamais eu se retrouve simplement avec zéro ligne.
         */
        internal val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS pregnancies (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        testedOn INTEGER NOT NULL,
                        result TEXT NOT NULL,
                        lastPeriodStart INTEGER,
                        status TEXT NOT NULL DEFAULT 'NONE',
                        endedOn INTEGER,
                        note TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * Toutes les migrations, en un seul endroit.
         *
         * Exposée pour que le test de migration utilise exactement celles que
         * l'application embarque : les lister à part dans le test revenait à
         * vérifier du code qui n'est pas celui qui tourne sur le téléphone.
         */
        /**
         * Le carnet de suivi prénatal : coordonnées du soignant, facteurs de risque,
         * et la table des actes réellement faits. Les colonnes ajoutées ont toutes
         * une valeur par défaut, donc les grossesses déjà enregistrées restent valides.
         */
        internal val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                listOf(
                    "caregiverName", "caregiverRole", "caregiverPhone",
                    "facility", "riskFactors",
                ).forEach { column ->
                    db.execSQL("ALTER TABLE pregnancies ADD COLUMN $column TEXT NOT NULL DEFAULT ''")
                }
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS prenatal_care (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        pregnancyId INTEGER NOT NULL,
                        code TEXT NOT NULL,
                        doneOn INTEGER NOT NULL,
                        note TEXT NOT NULL DEFAULT ''
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_prenatal_care_pregnancyId_code " +
                        "ON prenatal_care (pregnancyId, code)"
                )
            }
        }

        /** La date de naissance, qui ouvre le suivi d'après la naissance. */
        internal val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pregnancies ADD COLUMN birthDate INTEGER")
            }
        }

        internal val MIGRATIONS =
            arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)

        fun build(context: Context): LumeaDatabase {
            // SQLCipher chiffre le fichier de base ; la clé vit dans le Keystore Android.
            net.sqlcipher.database.SQLiteDatabase.loadLibs(context)
            // SupportFactory efface la phrase de passe du tableau après ouverture.
            val factory = SupportFactory(DatabaseKeyProvider.passphrase(context))
            return Room.databaseBuilder(context, LumeaDatabase::class.java, NAME)
                .openHelperFactory(factory)
                .addMigrations(*MIGRATIONS)
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
        }
    }
}

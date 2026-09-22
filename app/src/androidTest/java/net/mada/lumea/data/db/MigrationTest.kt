package net.mada.lumea.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Vérifie que la migration 1 → 2 ajoute les colonnes de verrou **sans toucher aux
 * données existantes**.
 *
 * C'est le test le plus important du projet : une migration ratée, ce n'est pas un
 * écran mal aligné, c'est le journal intime de quelqu'un qui disparaît. Il tourne
 * sur un vrai appareil parce que la base est chiffrée par SQLCipher, dont le code
 * natif n'existe pas sur la JVM de développement.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private companion object {
        const val TEST_DB = "migration-test.db"
        // Phrase fixe : c'est une base jetable, pas les données de l'utilisatrice.
        val PASSPHRASE: ByteArray = SQLiteDatabase.getBytes("test-passphrase".toCharArray())
    }

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        LumeaDatabase::class.java,
        emptyList(),
        SupportFactory(PASSPHRASE, null, false),
    )

    @Before
    fun loadNativeLibrary() {
        SQLiteDatabase.loadLibs(InstrumentationRegistry.getInstrumentation().targetContext)
    }

    @Test
    fun migration1To2_preserveLesNotesEtAjouteLeVerrou() {
        helper.createDatabase(TEST_DB, 1).use { db ->
            db.execSQL(
                """
                INSERT INTO notes
                  (id, title, body, folderId, tags, colorIndex, isPinned, isFavorite,
                   isChecklist, isArchived, linkedDate, createdAt, updatedAt)
                VALUES (1, 'Mon secret', 'Texte à ne pas perdre', NULL, 'perso', 2,
                        1, 0, 0, 0, NULL, 1000, 2000)
                """.trimIndent()
            )
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, *LumeaDatabase.MIGRATIONS)

        db.query("SELECT title, body, isPinned, isLocked FROM notes WHERE id = 1").use { cursor ->
            assertTrue("la note a survécu à la migration", cursor.moveToFirst())
            assertEquals("Mon secret", cursor.getString(0))
            assertEquals("Texte à ne pas perdre", cursor.getString(1))
            assertEquals(1, cursor.getInt(2))
            // La nouvelle colonne existe et vaut « non protégé » par défaut.
            assertEquals(0, cursor.getInt(3))
        }
    }

    @Test
    fun migration1To2_preserveLesJourneesDeJournal() {
        helper.createDatabase(TEST_DB, 1).use { db ->
            db.execSQL(
                """
                INSERT INTO daily_logs
                  (date, flow, symptoms, mood, energy, journal, gratitude,
                   waterGlasses, sleepHours, updatedAt)
                VALUES (20000, 2, 'Crampes', 4, 3, 'Belle journée', 'Le soleil',
                        6, 7.5, 3000)
                """.trimIndent()
            )
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, *LumeaDatabase.MIGRATIONS)

        db.query("SELECT journal, gratitude, mood, sleepHours, isLocked FROM daily_logs").use { cursor ->
            assertTrue("la journée a survécu à la migration", cursor.moveToFirst())
            assertEquals("Belle journée", cursor.getString(0))
            assertEquals("Le soleil", cursor.getString(1))
            assertEquals(4, cursor.getInt(2))
            assertEquals(7.5f, cursor.getFloat(3), 0.001f)
            assertEquals(0, cursor.getInt(4))
        }
    }

    @Test
    fun migration2To3_ajouteLeSuiviDeGrossesseSansToucherAuReste() {
        helper.createDatabase(TEST_DB, 2).use { db ->
            db.execSQL(
                """
                INSERT INTO notes
                  (id, title, body, folderId, tags, colorIndex, isPinned, isFavorite,
                   isChecklist, isArchived, isLocked, linkedDate, createdAt, updatedAt)
                VALUES (1, 'Journal', 'Rien ne doit disparaître', NULL, '', 0,
                        0, 0, 0, 0, 1, NULL, 1000, 2000)
                """.trimIndent()
            )
            db.execSQL("INSERT INTO periods (id, startDate, endDate) VALUES (1, 20000, 20004)")
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 3, true, *LumeaDatabase.MIGRATIONS)

        // Les données d'avant sont intactes.
        db.query("SELECT title, isLocked FROM notes WHERE id = 1").use { cursor ->
            assertTrue("la note a survécu", cursor.moveToFirst())
            assertEquals("Journal", cursor.getString(0))
            assertEquals(1, cursor.getInt(1))
        }
        db.query("SELECT startDate, endDate FROM periods WHERE id = 1").use { cursor ->
            assertTrue("la période a survécu", cursor.moveToFirst())
            assertEquals(20000, cursor.getLong(0))
            assertEquals(20004, cursor.getLong(1))
        }

        // La table neuve existe et accepte une ligne.
        db.execSQL(
            """
            INSERT INTO pregnancies
              (id, testedOn, result, lastPeriodStart, status, endedOn, note, createdAt)
            VALUES (1, 20030, 'POSITIVE', 20000, 'ONGOING', NULL, '', 5000)
            """.trimIndent()
        )
        db.query("SELECT result, status, lastPeriodStart FROM pregnancies WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("POSITIVE", cursor.getString(0))
            assertEquals("ONGOING", cursor.getString(1))
            assertEquals(20000, cursor.getLong(2))
        }
    }

    @Test
    fun migration3To5_ajouteLeCarnetEtLApresNaissance() {
        helper.createDatabase(TEST_DB, 3).use { db ->
            db.execSQL(
                """
                INSERT INTO pregnancies
                  (id, testedOn, result, lastPeriodStart, status, endedOn, note, createdAt)
                VALUES (1, 20030, 'POSITIVE', 20000, 'ONGOING', NULL, '', 5000)
                """.trimIndent()
            )
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 5, true, *LumeaDatabase.MIGRATIONS)

        // La grossesse enregistrée en v3 survit, avec ses nouvelles colonnes vides.
        db.query(
            "SELECT result, caregiverName, riskFactors, birthDate FROM pregnancies WHERE id = 1"
        ).use { cursor ->
            assertTrue("la grossesse a survécu", cursor.moveToFirst())
            assertEquals("POSITIVE", cursor.getString(0))
            assertEquals("", cursor.getString(1))
            assertEquals("", cursor.getString(2))
            assertTrue("birthDate doit être nul", cursor.isNull(3))
        }

        // Le carnet de suivi accepte une ligne, et l'index empêche les doublons.
        db.execSQL(
            "INSERT INTO prenatal_care (id, pregnancyId, code, doneOn, note) " +
                "VALUES (1, 1, 'CPN1', 20040, 'TA 11/7')"
        )
        db.query("SELECT code, note FROM prenatal_care WHERE pregnancyId = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("CPN1", cursor.getString(0))
            assertEquals("TA 11/7", cursor.getString(1))
        }
    }
}

package net.mada.lumea.domain.agenda

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HolidaysTest {

    @Test
    fun `paques tombe aux bonnes dates`() {
        // Références connues, pour valider l'algorithme de Butcher.
        assertEquals(LocalDate.parse("2024-03-31"), easterSunday(2024))
        assertEquals(LocalDate.parse("2025-04-20"), easterSunday(2025))
        assertEquals(LocalDate.parse("2026-04-05"), easterSunday(2026))
        assertEquals(LocalDate.parse("2027-03-28"), easterSunday(2027))
    }

    @Test
    fun `les feries malgaches incluent le 26 juin et pas le 14 juillet`() {
        val noms = holidays(2026, HolidayCountry.MADAGASCAR).map { it.name }
        val dates = holidays(2026, HolidayCountry.MADAGASCAR).map { it.date }

        assertTrue(LocalDate.parse("2026-06-26") in dates)
        assertTrue("Fête de l'Indépendance" in noms)
        // La fête de la République malgache, absente des calendriers français.
        assertTrue(LocalDate.parse("2026-12-11") in dates)
        assertTrue(LocalDate.parse("2026-07-14") !in dates)
    }

    @Test
    fun `les feries francais incluent le 14 juillet et pas le 26 juin`() {
        val dates = holidays(2026, HolidayCountry.FRANCE).map { it.date }

        assertTrue(LocalDate.parse("2026-07-14") in dates)
        assertTrue(LocalDate.parse("2026-06-26") !in dates)
    }

    @Test
    fun `les fetes mobiles suivent paques`() {
        val easter = easterSunday(2026)
        val mada = holidays(2026, HolidayCountry.MADAGASCAR).associateBy { it.name }

        assertEquals(easter.plusDays(1), mada.getValue("Lundi de Pâques").date)
        assertEquals(easter.plusDays(39), mada.getValue("Ascension").date)
        assertEquals(easter.plusDays(50), mada.getValue("Lundi de Pentecôte").date)
    }

    @Test
    fun `un intervalle a cheval sur deux annees les couvre toutes les deux`() {
        val map = holidaysBetween(
            from = LocalDate.parse("2026-12-20"),
            to = LocalDate.parse("2027-01-10"),
            country = HolidayCountry.MADAGASCAR,
        )

        assertTrue("Noël manquant", LocalDate.parse("2026-12-25") in map)
        assertTrue("Nouvel An manquant", LocalDate.parse("2027-01-01") in map)
        // Rien en dehors de l'intervalle demandé.
        assertTrue(map.keys.all { it >= LocalDate.parse("2026-12-20") })
    }

    @Test
    fun `le choix aucun n'affiche rien`() {
        assertTrue(holidays(2026, HolidayCountry.AUCUN).isEmpty())
        assertTrue(
            holidaysBetween(
                LocalDate.parse("2026-01-01"),
                LocalDate.parse("2026-12-31"),
                HolidayCountry.AUCUN,
            ).isEmpty()
        )
    }
}

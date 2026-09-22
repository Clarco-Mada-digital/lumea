package net.mada.lumea.domain.cycle

import net.mada.lumea.data.db.PeriodEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CycleEngineTest {

    private val engine = CycleEngine()

    private fun period(start: String, end: String? = null) =
        PeriodEntity(startDate = LocalDate.parse(start), endDate = end?.let(LocalDate::parse))

    @Test
    fun `sans historique, tout est inconnu`() {
        val insight = engine.insight(emptyList(), LocalDate.parse("2026-03-10"))

        assertEquals(CyclePhase.INCONNUE, insight.phase)
        assertEquals(null, insight.nextPeriodStart)
        assertTrue(insight.isEstimate)
        // On retombe sur les valeurs par défaut plutôt que sur zéro.
        assertEquals(28, insight.averageCycleLength)
    }

    @Test
    fun `avec un seul cycle enregistre, on utilise la duree par defaut`() {
        val insight = engine.insight(
            listOf(period("2026-03-01", "2026-03-05")),
            LocalDate.parse("2026-03-03"),
        )

        assertEquals(28, insight.averageCycleLength)
        assertEquals(3, insight.dayOfCycle)
        assertEquals(CyclePhase.MENSTRUATION, insight.phase)
        assertEquals(LocalDate.parse("2026-03-29"), insight.nextPeriodStart)
    }

    @Test
    fun `la duree moyenne vient des ecarts entre debuts de regles`() {
        // Écarts de 30 et 30 jours : la moyenne doit être 30, pas 28.
        val insight = engine.insight(
            listOf(
                period("2026-01-01", "2026-01-05"),
                period("2026-01-31", "2026-02-04"),
                period("2026-03-02", "2026-03-06"),
            ),
            LocalDate.parse("2026-03-10"),
        )

        assertEquals(30, insight.averageCycleLength)
        assertEquals(LocalDate.parse("2026-04-01"), insight.nextPeriodStart)
        assertFalse(insight.isEstimate)
    }

    @Test
    fun `les ecarts aberrants sont ignores`() {
        // Un « écart » de 200 jours (saisie oubliée) ne doit pas fausser la moyenne.
        val insight = engine.insight(
            listOf(
                period("2025-06-01"),
                period("2025-12-18"),
                period("2026-01-15"),
                period("2026-02-12"),
            ),
            LocalDate.parse("2026-02-20"),
        )

        assertEquals(28, insight.averageCycleLength)
    }

    @Test
    fun `l'ovulation tombe une phase luteale avant les regles suivantes`() {
        val insight = engine.insight(
            listOf(period("2026-03-01", "2026-03-05")),
            LocalDate.parse("2026-03-10"),
        )

        val next = insight.nextPeriodStart!!
        assertEquals(next.minusDays(14), insight.ovulationDate)
        // Fenêtre fertile : 5 jours avant l'ovulation, 1 jour après.
        assertEquals(insight.ovulationDate!!.minusDays(5), insight.fertileWindow!!.start)
        assertEquals(insight.ovulationDate!!.plusDays(1), insight.fertileWindow!!.endInclusive)
    }

    @Test
    fun `un retard important ne produit pas un jour de cycle absurde`() {
        // Dernières règles il y a 70 jours : sans garde-fou on afficherait « jour 71 ».
        val insight = engine.insight(
            listOf(period("2026-01-01", "2026-01-05")),
            LocalDate.parse("2026-03-12"),
        )

        assertTrue("jour de cycle = ${insight.dayOfCycle}", insight.dayOfCycle <= 56)
    }

    @Test
    fun `la regularite mesure la dispersion des cycles`() {
        val regulier = engine.insight(
            listOf(period("2026-01-01"), period("2026-01-29"), period("2026-02-26")),
            LocalDate.parse("2026-03-01"),
        )
        assertEquals(0, regulier.regularityDays)

        val irregulier = engine.insight(
            listOf(period("2026-01-01"), period("2026-01-22"), period("2026-02-26")),
            LocalDate.parse("2026-03-01"),
        )
        assertTrue(irregulier.regularityDays > 3)
    }

    @Test
    fun `les jours enregistres priment sur les jours prevus`() {
        val periods = listOf(period("2026-03-01", "2026-03-05"))
        val predictions = engine.predictions(
            periods,
            LocalDate.parse("2026-03-01"),
            LocalDate.parse("2026-04-30"),
            LocalDate.parse("2026-03-10"),
        )

        val jourEnregistre = predictions[LocalDate.parse("2026-03-03")]!!
        assertTrue(jourEnregistre.isLoggedPeriod)
        assertFalse(jourEnregistre.isPredictedPeriod)

        val jourPrevu = predictions[LocalDate.parse("2026-03-29")]!!
        assertFalse(jourPrevu.isLoggedPeriod)
        assertTrue(jourPrevu.isPredictedPeriod)
    }

    @Test
    fun `la fenetre fertile est marquee dans le calendrier`() {
        val predictions = engine.predictions(
            listOf(period("2026-03-01", "2026-03-05")),
            LocalDate.parse("2026-03-01"),
            LocalDate.parse("2026-03-31"),
            LocalDate.parse("2026-03-10"),
        )

        // Cycle de 28 jours à partir du 1er mars : ovulation le 15 mars.
        val ovulation = predictions.values.single { it.isOvulation && it.date.month.value == 3 }
        assertEquals(LocalDate.parse("2026-03-15"), ovulation.date)
        assertTrue(predictions[LocalDate.parse("2026-03-12")]!!.isFertile)
        assertFalse(predictions[LocalDate.parse("2026-03-22")]!!.isFertile)
    }

    @Test
    fun `des regles en cours ne faussent pas la duree moyenne`() {
        // Marquer « mes règles ont commencé » crée une période sans date de fin.
        // Elle ne doit pas compter comme des règles d'un seul jour.
        val insight = engine.insight(
            listOf(period("2026-03-01", "2026-03-06"), period("2026-03-29")),
            LocalDate.parse("2026-03-29"),
        )

        assertEquals(6, insight.averagePeriodLength)
    }

    @Test
    fun `sans aucune periode close, on garde la duree par defaut`() {
        val insight = engine.insight(
            listOf(period("2026-03-10")),
            LocalDate.parse("2026-03-10"),
        )

        assertEquals(5, insight.averagePeriodLength)
    }

    @Test
    fun `des regles en cours sont peintes sur la duree habituelle`() {
        val predictions = engine.predictions(
            listOf(period("2026-03-10")),
            LocalDate.parse("2026-03-01"),
            LocalDate.parse("2026-03-31"),
            LocalDate.parse("2026-03-10"),
        )

        // 5 jours par défaut, du 10 au 14 mars.
        assertTrue(predictions[LocalDate.parse("2026-03-10")]!!.isLoggedPeriod)
        assertTrue(predictions[LocalDate.parse("2026-03-14")]!!.isLoggedPeriod)
        assertFalse(predictions[LocalDate.parse("2026-03-15")]!!.isLoggedPeriod)
    }

    @Test
    fun `rien n'est predit avant les premieres regles enregistrees`() {
        val predictions = engine.predictions(
            listOf(period("2026-03-10")),
            LocalDate.parse("2026-02-01"),
            LocalDate.parse("2026-03-31"),
            LocalDate.parse("2026-03-10"),
        )

        val avant = predictions.values.filter { it.date.isBefore(LocalDate.parse("2026-03-10")) }
        assertTrue(avant.isNotEmpty())
        assertTrue(
            "aucune fenêtre fertile inventée avant la première saisie",
            avant.none { it.isFertile || it.isOvulation || it.isPredictedPeriod },
        )
        assertTrue(avant.all { it.phase == CyclePhase.INCONNUE })
    }

    @Test
    fun `le jour d'ovulation depend de la phase luteale configuree`() {
        val parDefaut = engine.insight(listOf(period("2026-03-01")), LocalDate.parse("2026-03-05"))
        assertEquals(14, parDefaut.ovulationDayOfCycle)

        val luteale12 = CycleEngine(lutealLength = 12)
            .insight(listOf(period("2026-03-01")), LocalDate.parse("2026-03-05"))
        assertEquals(16, luteale12.ovulationDayOfCycle)
    }

    @Test
    fun `les predictions couvrent chaque jour de l'intervalle demande`() {
        val from = LocalDate.parse("2026-03-01")
        val to = LocalDate.parse("2026-03-31")
        val predictions = engine.predictions(listOf(period("2026-03-01")), from, to)

        assertEquals(31, predictions.size)
        assertTrue(predictions.containsKey(from))
        assertTrue(predictions.containsKey(to))
    }

    @Test
    fun `des regles en retard sont signalees, pas absorbees par un cycle de plus`() {
        // Cycles de 28 jours, dernières règles le 1er mars : prévues le 29 mars.
        val periods = listOf(
            period("2026-01-04", "2026-01-08"),
            period("2026-02-01", "2026-02-05"),
            period("2026-03-01", "2026-03-05"),
        )

        val insight = engine.insight(periods, LocalDate.parse("2026-04-03"))

        // Avant, le moteur glissait d'un cycle et annonçait les prochaines règles
        // au 26 avril : le retard disparaissait sans que personne ne l'ait confirmé.
        assertEquals(LocalDate.parse("2026-03-29"), insight.expectedPeriodStart)
        assertEquals(LocalDate.parse("2026-03-29"), insight.nextPeriodStart)
        assertTrue(insight.isLate)
        assertEquals(5, insight.daysLate)
        assertFalse(insight.isStale)
        // Le jour du cycle continue de compter au lieu de repartir à 1.
        assertEquals(34, insight.dayOfCycle)
    }

    @Test
    fun `au-dela d'un cycle entier sans nouvelle, on decale et on le signale`() {
        val periods = listOf(
            period("2026-01-04", "2026-01-08"),
            period("2026-02-01", "2026-02-05"),
        )

        // Plus de deux mois sans rien noter : ce n'est plus un retard, c'est un oubli.
        val insight = engine.insight(periods, LocalDate.parse("2026-05-10"))

        assertTrue(insight.isStale)
        assertEquals(0, insight.daysLate)
        assertFalse(insight.isLate)
        // La prévision reste utilisable au lieu d'annoncer 70 jours de retard.
        assertTrue(insight.dayOfCycle <= insight.averageCycleLength)
    }

    @Test
    fun `des regles ouvertes sont suivies jour par jour`() {
        val periods = listOf(
            period("2026-02-01", "2026-02-05"),
            period("2026-03-01"), // pas de date de fin : en cours
        )

        val insight = engine.insight(periods, LocalDate.parse("2026-03-03"))

        assertEquals(LocalDate.parse("2026-03-01"), insight.currentPeriodStart)
        assertEquals(3, insight.currentPeriodDay)
        assertEquals(CyclePhase.MENSTRUATION, insight.phase)
        // La période close précédente donne la durée de référence.
        assertEquals(5, insight.lastPeriodLength)
    }

    @Test
    fun `une periode ouverte oubliee depuis des semaines n'est plus consideree en cours`() {
        val periods = listOf(period("2026-01-01"))

        val insight = engine.insight(periods, LocalDate.parse("2026-02-20"))

        assertEquals(null, insight.currentPeriodStart)
        assertEquals(null, insight.currentPeriodDay)
    }

    @Test
    fun `un jour de regles enregistre prime sur la prevision de fertilite`() {
        // Cycle court de 21 jours : la fenêtre fertile mord sur les règles suivantes.
        val periods = listOf(
            period("2026-02-01", "2026-02-07"),
            period("2026-02-22", "2026-02-28"),
        )

        val predictions = engine.predictions(
            periods,
            LocalDate.parse("2026-02-01"),
            LocalDate.parse("2026-03-10"),
            LocalDate.parse("2026-02-25"),
        )

        val overlapping = predictions.values.filter { it.isLoggedPeriod && (it.isFertile || it.isOvulation) }
        overlapping.forEach {
            assertEquals(PregnancyRisk.REGLES, it.pregnancyRisk)
            assertTrue(it.fertileDuringPeriod)
        }
    }

    // ------------------------------------------------ Cohérence des dates

    @Test
    fun `un cycle ne se calcule pas sur des regles enregistrees dans le futur`() {
        // Garde-fou du moteur : même si une date future se glissait en base, les
        // écarts aberrants sont déjà filtrés et la moyenne reste plausible.
        val periods = listOf(
            period("2026-02-01", "2026-02-05"),
            period("2026-03-01", "2026-03-05"),
        )
        val insight = engine.insight(periods, LocalDate.parse("2026-03-10"))

        assertEquals(28, insight.averageCycleLength)
        assertTrue(insight.dayOfCycle in 1..40)
    }

    @Test
    fun `le jour du cycle n'est jamais nul ou negatif`() {
        val periods = listOf(period("2026-03-01", "2026-03-05"))
        listOf("2026-03-01", "2026-03-15", "2026-04-20").forEach { day ->
            val insight = engine.insight(periods, LocalDate.parse(day))
            assertTrue("jour de cycle invalide le $day", insight.dayOfCycle >= 1)
        }
    }
}

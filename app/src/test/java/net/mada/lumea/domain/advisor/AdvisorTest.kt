package net.mada.lumea.domain.advisor

import net.mada.lumea.domain.cycle.CycleInsight
import net.mada.lumea.domain.cycle.CyclePhase
import net.mada.lumea.domain.pregnancy.postpartumProgress
import net.mada.lumea.domain.pregnancy.pregnancyProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AdvisorTest {

    private val today = LocalDate.parse("2026-09-22")

    private fun insight(daysLate: Int) = CycleInsight(
        lastPeriodStart = today.minusDays((28 + daysLate).toLong()),
        dayOfCycle = 28 + daysLate,
        phase = CyclePhase.LUTEALE,
        averageCycleLength = 28,
        averagePeriodLength = 5,
        regularityDays = 1,
        nextPeriodStart = today.minusDays(daysLate.toLong()),
        daysUntilNextPeriod = -daysLate,
        ovulationDate = today.minusDays(14),
        ovulationDayOfCycle = 14,
        fertileWindow = today.minusDays(19)..today.minusDays(13),
        isEstimate = false,
        expectedPeriodStart = today.minusDays(daysLate.toLong()),
        daysLate = daysLate,
    )

    // ------------------------------------------------------ Reconnaissance

    @Test
    fun `les mots du quotidien sont reconnus`() {
        assertEquals(Topic.RETARD, detectTopic("pourquoi mes règles ne sont pas arrivées ?"))
        assertEquals(Topic.SAIGNEMENT, detectTopic("j'ai des saignements"))
        assertEquals(Topic.FIEVRE, detectTopic("j'ai de la fievre depuis hier"))
        assertEquals(Topic.TEST, detectTopic("est-ce que je dois faire un test"))
        assertEquals(Topic.INCONNU, detectTopic("quelle est la capitale du Japon"))
    }

    @Test
    fun `une phrase qui melange plusieurs sujets bascule vers le plus grave`() {
        // « je saigne et j'ai du retard » doit partir sur le saignement.
        assertEquals(Topic.SAIGNEMENT, detectTopic("j'ai du retard et je saigne un peu"))
    }

    // ---------------------------------------------- Le retard, cas par cas

    @Test
    fun `deux jours de retard rassurent au lieu d'alarmer`() {
        val advice = adviseOn(Topic.RETARD, AdvisorContext(insight = insight(2)))

        assertEquals(AdviceLevel.RASSURANT, advice.level)
        assertTrue(advice.body.contains("2 jours"))
        // Surtout : on ne propose pas de test à ce stade.
        assertTrue(AdviceAction.FAIRE_TEST !in advice.actions)
        assertTrue(advice.facts.any { it.first == "Retard" && it.second == "2 j" })
    }

    @Test
    fun `une semaine de retard propose le test`() {
        val advice = adviseOn(Topic.RETARD, AdvisorContext(insight = insight(8)))

        assertEquals(AdviceLevel.ATTENTION, advice.level)
        assertTrue(AdviceAction.FAIRE_TEST in advice.actions)
    }

    @Test
    fun `un retard de plus de trois semaines envoie consulter`() {
        val advice = adviseOn(Topic.RETARD, AdvisorContext(insight = insight(30)))

        assertEquals(AdviceLevel.CONSULTER, advice.level)
        assertTrue(AdviceAction.FAIRE_TEST in advice.actions)
    }

    // ------------------------------------------------------ Les urgences

    @Test
    fun `un saignement pendant la grossesse est toujours une urgence`() {
        val context = AdvisorContext(
            pregnancy = pregnancyProgress(today.minusWeeks(9), today),
            caregiverName = "Mme Rasoa",
            caregiverPhone = "0340000000",
        )
        val advice = adviseOn(Topic.SAIGNEMENT, context)

        assertEquals(AdviceLevel.URGENT, advice.level)
        // Le numéro enregistré remonte en premier.
        assertEquals(AdviceAction.APPELER_SOIGNANT, advice.actions.first())
        // Au premier trimestre, la grossesse extra-utérine doit être nommée.
        assertTrue(advice.body.contains("hors de l'utérus"))
    }

    @Test
    fun `sans soignant enregistre on ne propose pas d'appeler`() {
        val context = AdvisorContext(pregnancy = pregnancyProgress(today.minusWeeks(9), today))
        val advice = adviseOn(Topic.SAIGNEMENT, context)

        assertEquals(AdviceLevel.URGENT, advice.level)
        assertTrue(AdviceAction.APPELER_SOIGNANT !in advice.actions)
    }

    @Test
    fun `une fievre est toujours urgente en zone de paludisme`() {
        assertEquals(AdviceLevel.URGENT, adviseOn(Topic.FIEVRE, AdvisorContext()).level)
        assertEquals(
            AdviceLevel.URGENT,
            adviseOn(
                Topic.FIEVRE,
                AdvisorContext(pregnancy = pregnancyProgress(today.minusWeeks(20), today)),
            ).level,
        )
    }

    @Test
    fun `les mouvements du bebe ne sont urgents qu'a partir de 20 SA`() {
        val early = AdvisorContext(pregnancy = pregnancyProgress(today.minusWeeks(16), today))
        val late = AdvisorContext(pregnancy = pregnancyProgress(today.minusWeeks(30), today))

        assertEquals(AdviceLevel.RASSURANT, adviseOn(Topic.MOUVEMENTS, early).level)
        assertEquals(AdviceLevel.URGENT, adviseOn(Topic.MOUVEMENTS, late).level)
    }

    // ------------------------------------------ Grossesse et après-naissance

    @Test
    fun `enceinte, la question du retard ne parle plus de test`() {
        val context = AdvisorContext(pregnancy = pregnancyProgress(today.minusWeeks(12), today))
        val advice = adviseOn(Topic.RETARD, context)

        assertEquals(AdviceLevel.RASSURANT, advice.level)
        assertTrue(AdviceAction.FAIRE_TEST !in advice.actions)
        assertTrue(advice.body.contains("aménorrhée"))
    }

    @Test
    fun `apres la naissance, la fertilite depend de la MAMA`() {
        val base = AdvisorContext(postpartum = postpartumProgress(today.minusWeeks(8), today))

        val protege = adviseOn(Topic.FERTILITE, base.copy(mamaProtected = true))
        val expose = adviseOn(Topic.FERTILITE, base.copy(mamaProtected = false))

        assertEquals(AdviceLevel.INFO, protege.level)
        assertEquals(AdviceLevel.ATTENTION, expose.level)
        assertNotEquals(protege.title, expose.title)
        // Le point qui n'est jamais expliqué doit l'être.
        assertTrue(expose.body.contains("avant les règles"))
    }

    // ------------------------------------------------------- Hors champ

    @Test
    fun `hors champ, le conseiller le dit au lieu d'inventer`() {
        val advice = advise("comment marche un moteur à explosion ?", AdvisorContext())

        assertTrue(advice.title.contains("ne sais pas"))
        assertTrue(AdviceAction.DEMANDER_IA in advice.actions)
    }

    @Test
    fun `les questions proposees s'adaptent a la situation`() {
        val cycle = suggestedQuestions(AdvisorContext(insight = insight(0)))
        val enceinte = suggestedQuestions(
            AdvisorContext(pregnancy = pregnancyProgress(today.minusWeeks(20), today))
        )
        val apres = suggestedQuestions(
            AdvisorContext(postpartum = postpartumProgress(today.minusWeeks(4), today))
        )

        assertTrue(cycle.any { it.second == Topic.TEST })
        assertTrue(enceinte.any { it.second == Topic.MOUVEMENTS })
        assertTrue(apres.any { it.second == Topic.ALLAITEMENT })
        // Une question sur les mouvements du bébé n'a aucun sens hors grossesse.
        assertTrue(cycle.none { it.second == Topic.MOUVEMENTS })
    }
}

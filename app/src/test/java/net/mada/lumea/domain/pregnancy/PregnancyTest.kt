package net.mada.lumea.domain.pregnancy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PregnancyTest {

    private val ddr = LocalDate.parse("2026-01-05")

    @Test
    fun `les semaines d'amenorrhee se comptent depuis les dernieres regles`() {
        // 8 semaines et 3 jours après le premier jour des dernières règles.
        val progress = pregnancyProgress(ddr, ddr.plusDays(59))

        assertEquals(8, progress.weeks)
        assertEquals(3, progress.days)
        assertEquals("8 SA + 3 j", progress.label)
        // Les semaines de grossesse réelles ont deux semaines de moins que les SA.
        assertEquals(6, progress.weeksOfPregnancy)
    }

    @Test
    fun `le terme tombe 280 jours apres les dernieres regles`() {
        val progress = pregnancyProgress(ddr, ddr)

        assertEquals(LocalDate.parse("2026-10-12"), progress.dueDate)
        assertEquals(280, progress.daysUntilDue)
        // Le jour même de la DDR, on est à 0 SA.
        assertEquals(0, progress.weeks)
    }

    @Test
    fun `les trimestres se decoupent a 15 et 29 SA`() {
        assertEquals(1, pregnancyProgress(ddr, ddr.plusWeeks(14)).trimester)
        assertEquals(2, pregnancyProgress(ddr, ddr.plusWeeks(15)).trimester)
        assertEquals(2, pregnancyProgress(ddr, ddr.plusWeeks(28)).trimester)
        assertEquals(3, pregnancyProgress(ddr, ddr.plusWeeks(29)).trimester)
    }

    @Test
    fun `au-dela de 42 SA le calcul n'est plus plausible`() {
        assertTrue(pregnancyProgress(ddr, ddr.plusWeeks(41)).isPlausible)
        assertFalse(pregnancyProgress(ddr, ddr.plusWeeks(43)).isPlausible)
    }

    @Test
    fun `un retard de quelques jours ne declenche aucune proposition de test`() {
        // Le cas le plus fréquent de tous : on rassure, on ne propose rien.
        (0 until TEST_SUGGESTION_DAYS).forEach { late ->
            assertFalse(
                "un test ne devrait pas être proposé à $late jour(s) de retard",
                testReliability(late).reliable,
            )
        }
    }

    @Test
    fun `a partir d'une semaine de retard le test est franchement conseille`() {
        assertTrue(testReliability(TEST_SUGGESTION_DAYS).reliable)
        assertTrue(testReliability(TEST_RECOMMENDED_DAYS).reliable)
        assertEquals("Un test a du sens maintenant", testReliability(7).title)
        // Passé trois semaines, on ajoute l'avis médical au test.
        assertEquals("Un test, puis un avis médical", testReliability(25).title)
    }

    @Test
    fun `chaque semaine de grossesse tombe dans une etape du suivi`() {
        (0..42).forEach { week ->
            assertTrue("aucune étape pour $week SA", milestoneFor(week) != null)
        }
    }

    // ------------------------------------------------ Après la naissance (MAMA)

    @Test
    fun `la MAMA ne protege que si les trois conditions tiennent`() {
        val all = MamaCondition.entries.map { it.code }.toSet()
        assertTrue(mamaStatus(all).protected)

        // Une seule condition qui tombe suffit à annuler la protection.
        all.forEach { missing ->
            val partial = all - missing
            assertFalse(
                "la MAMA ne devrait pas protéger sans $missing",
                mamaStatus(partial).protected,
            )
        }
        assertFalse(mamaStatus(emptySet()).protected)
    }

    @Test
    fun `le suivi d'apres naissance compte les jours, semaines et mois`() {
        val birth = LocalDate.parse("2026-03-01")
        val progress = postpartumProgress(birth, LocalDate.parse("2026-04-15"))

        assertEquals(45, progress.daysSince)
        assertEquals(6, progress.weeksSince)
        assertEquals(1, progress.monthsSince)
        // Passé six semaines, on sort de la période la plus à risque pour la mère.
        assertFalse(progress.inImmediatePostpartum)
        assertTrue(progress.withinSixMonths)
    }

    @Test
    fun `la fenetre de la MAMA se ferme a six mois`() {
        val birth = LocalDate.parse("2026-03-01")
        assertTrue(postpartumProgress(birth, birth.plusDays(181)).withinSixMonths)
        assertFalse(postpartumProgress(birth, birth.plusDays(182)).withinSixMonths)
    }

    // ---------------------------------------- Le planning posé sur le calendrier

    @Test
    fun `les rendez-vous tombent bien sur le calendrier depuis la date d'ancrage`() {
        val schedule = careSchedule(ddr, ALL_CARE_ACTS)

        // La 2e CPN est attendue à 18 SA : 18 semaines après les dernières règles.
        val cpn2 = ANC_VISITS.first { it.code == "CPN2" }
        val expected = ddr.plusWeeks(18)
        assertTrue(
            "la 2e CPN devrait tomber le $expected",
            schedule[expected].orEmpty().any { it.code == cpn2.code },
        )

        // La 1re dose de TPIg ne doit jamais être proposée avant 13 SA.
        val tpi1Date = schedule.entries.first { e -> e.value.any { it.code == "TPI1" } }.key
        assertEquals(ddr.plusWeeks(13), tpi1Date)
    }

    @Test
    fun `les actes permanents ne sont pas poses sur un jour precis`() {
        val schedule = careSchedule(ddr, ALL_CARE_ACTS)
        val placed = schedule.values.flatten().map { it.code }.toSet()

        // Le fer quotidien et la moustiquaire courent toute la grossesse : les
        // afficher sur une case du calendrier n'aurait aucun sens.
        assertFalse("FAF" in placed)
        assertFalse("MILD" in placed)
        // Les rendez-vous datés, eux, sont bien présents.
        assertTrue("CPN1" in placed)
        assertTrue("VAT2" in placed)
    }

    @Test
    fun `apres la naissance le planning part de la date de naissance`() {
        val birth = LocalDate.parse("2026-10-12")
        val schedule = careSchedule(birth, POSTNATAL_VISITS + INFANT_VACCINES)

        // La première visite postnatale est le jour même.
        assertTrue(schedule[birth].orEmpty().any { it.code == "CPON1" })
        // Penta 1 tombe à 6 semaines.
        assertTrue(schedule[birth.plusWeeks(6)].orEmpty().any { it.code == "PENTA1" })
        // Rougeole à 9 mois, soit 39 semaines.
        assertTrue(schedule[birth.plusWeeks(39)].orEmpty().any { it.code == "ROUGEOLE" })
    }

    // ------------------------------------------- Le suivi réel, pas la théorie

    @Test
    fun `une CPN cochee n'est plus reclamee`() {
        // 10 SA : la première CPN est le rendez-vous du moment.
        val before = nextCareAction(ddr, ALL_CARE_ACTS, doneCodes = emptySet(), currentWeek = 10)
        assertEquals("CPN1", before?.act?.code)

        // Une fois cochée, l'app doit passer à la suite au lieu de la répéter.
        val after = nextCareAction(ddr, ALL_CARE_ACTS, doneCodes = setOf("CPN1"), currentWeek = 10)
        assertTrue("CPN1 ne doit plus être proposée", after?.act?.code != "CPN1")
    }

    @Test
    fun `un rendez-vous manque passe devant un rendez-vous a venir`() {
        // 20 SA sans rien avoir coché : la 1re CPN est en retard depuis longtemps.
        val next = nextCareAction(ddr, ALL_CARE_ACTS, doneCodes = emptySet(), currentWeek = 20)

        assertEquals(CareStatus.EN_RETARD, next?.status)
        assertEquals("CPN1", next?.act?.code)
        assertTrue("plusieurs actes devraient être en retard", (next?.lateCount ?: 0) > 1)
        assertTrue(next!!.headline.contains("retard"))
    }

    @Test
    fun `tout coche ne propose plus rien`() {
        val everything = ALL_CARE_ACTS.map { it.code }.toSet()
        assertEquals(null, nextCareAction(ddr, ALL_CARE_ACTS, everything, currentWeek = 20))
    }

    @Test
    fun `les actes continus ne sont jamais proposes comme prochaine etape`() {
        // Le fer quotidien et la moustiquaire n'ont pas de rendez-vous à rappeler.
        val next = nextCareAction(ddr, ALL_CARE_ACTS, doneCodes = emptySet(), currentWeek = 2)
        assertTrue(next?.act?.code != "FAF")
        assertTrue(next?.act?.code != "MILD")
    }

    @Test
    fun `une date de naissance future ne produit pas de progression negative`() {
        val birth = LocalDate.now().plusDays(30)
        val progress = postpartumProgress(birth, LocalDate.now())

        // On borne à zéro plutôt que de compter à rebours.
        assertEquals(0, progress.daysSince)
        assertEquals(0, progress.weeksSince)
    }

    @Test
    fun `une DDR future ne produit pas de semaines negatives`() {
        val future = LocalDate.now().plusWeeks(2)
        val progress = pregnancyProgress(future, LocalDate.now())

        assertEquals(0, progress.weeks)
        assertTrue(progress.daysUntilDue > 0)
    }
}

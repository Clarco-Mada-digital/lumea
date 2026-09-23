package net.mada.lumea.ui.learn

import net.mada.lumea.domain.learn.lessonTopics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Les figures sont rattachées aux leçons par leur identifiant.
 *
 * Un identifiant qui ne correspond à rien ne casse rien : la leçon s'affiche
 * simplement sans schéma, et personne ne s'en aperçoit. D'où ce test, qui vérifie
 * que chaque figure pointe bien vers une leçon qui existe.
 */
class LessonFiguresTest {

    @Test
    fun `les lecons ciblees ont bien une figure`() {
        assertEquals(LessonFigure.CYCLE_ROUE, figureFor("cycle-basics"))
        assertEquals(LessonFigure.PHASES, figureFor("phases"))
        assertEquals(LessonFigure.FENETRE_FERTILE, figureFor("ovulation"))
    }

    @Test
    fun `chaque figure correspond a une lecon existante`() {
        val ids = lessonTopics.flatMap { it.lessons }.map { it.id }.toSet()
        listOf("cycle-basics", "phases", "ovulation", "contraception", "condoms")
            .forEach { id ->
                assertNotNull("aucune figure pour $id", figureFor(id))
                assertTrue("identifiant de leçon inconnu : $id", id in ids)
            }
    }

    @Test
    fun `une lecon sans figure n'en invente pas`() {
        assertEquals(null, figureFor("irregular"))
        assertEquals(null, figureFor("inexistant"))
    }
}

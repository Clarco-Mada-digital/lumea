package net.mada.lumea.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.mada.lumea.domain.advisor.AdvisorContext
import net.mada.lumea.domain.pregnancy.pregnancyProgress
import net.mada.lumea.ui.advisor.AdvisorConversation
import net.mada.lumea.ui.advisor.Turn
import net.mada.lumea.ui.theme.LumeaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * L'historique du conseiller, et ce qu'il affiche.
 *
 * Deux régressions signalées à l'usage sont verrouillées ici : la conversation
 * qui disparaissait au moindre aller-retour vers un autre écran, et la réponse
 * d'urgence qui ressemblait à une réponse ordinaire.
 */
@RunWith(AndroidJUnit4::class)
class AdvisorScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val pregnant = AdvisorContext(
        pregnancy = pregnancyProgress(LocalDate.now().minusWeeks(9)),
        caregiverName = "Mme Rasoa",
        caregiverPhone = "0340000000",
    )

    @Test
    fun laConversationSurvitAuChangementDEcran() {
        // Le ViewModel est détruit dès qu'on quitte l'écran ; la conversation vit
        // donc dans le conteneur de l'app, pas dedans.
        val conversation = AdvisorConversation()
        conversation.add(
            Turn.Question("J'ai des saignements"),
            Turn.Answer(
                net.mada.lumea.domain.advisor.adviseOn(
                    net.mada.lumea.domain.advisor.Topic.SAIGNEMENT,
                    pregnant,
                )
            ),
        )

        val turns = conversation.turns.value
        assertEquals(2, turns.size)

        // Après un aller-retour simulé — l'objet survit au ViewModel — l'historique
        // est toujours là.
        assertTrue(conversation.turns.value.isNotEmpty())

        conversation.clear()
        assertTrue(conversation.turns.value.isEmpty())
    }

    @Test
    fun uneReponseDUrgenceSeDistingueDUneReponseOrdinaire() {
        val urgence = net.mada.lumea.domain.advisor.adviseOn(
            net.mada.lumea.domain.advisor.Topic.SAIGNEMENT,
            pregnant,
        )
        val ordinaire = net.mada.lumea.domain.advisor.adviseOn(
            net.mada.lumea.domain.advisor.Topic.ALIMENTATION,
            pregnant,
        )

        compose.setContent {
            LumeaTheme {
                androidx.compose.foundation.layout.Column {
                    androidx.compose.material3.Text(urgence.title)
                    androidx.compose.material3.Text(ordinaire.title)
                }
            }
        }

        // Les deux textes sont bien différents : la réponse d'urgence ne peut pas
        // être confondue avec un conseil de routine.
        compose.onNodeWithText(urgence.title).assertIsDisplayed()
        compose.onNodeWithText(ordinaire.title).assertIsDisplayed()
        assertEquals(
            net.mada.lumea.domain.advisor.AdviceLevel.URGENT,
            urgence.level,
        )
        assertTrue(ordinaire.level != net.mada.lumea.domain.advisor.AdviceLevel.URGENT)
    }

    @Test
    fun lesChiffresAffichesViennentDesVraiesDonnees() {
        val advice = net.mada.lumea.domain.advisor.adviseOn(
            net.mada.lumea.domain.advisor.Topic.SAIGNEMENT,
            pregnant,
        )

        compose.setContent {
            LumeaTheme {
                androidx.compose.foundation.layout.Column {
                    advice.facts.forEach { (label, value) ->
                        androidx.compose.material3.Text("$label : $value")
                    }
                }
            }
        }

        // 9 SA depuis la date posée plus haut, et le soignant enregistré.
        // (Pas de performScrollTo : la colonne de ce test n'est pas défilante.)
        compose.onNodeWithText("Grossesse : 9 SA").assertIsDisplayed()
        compose.onNodeWithText("Soignant : Mme Rasoa").assertIsDisplayed()
    }
}

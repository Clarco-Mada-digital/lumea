package net.mada.lumea.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import net.mada.lumea.ui.components.DebouncedTextField
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Le champ qui avalait les lettres.
 *
 * Symptôme signalé à l'usage : en tapant vite dans les réglages, des lettres
 * disparaissaient et le curseur reculait. Cause : chaque frappe écrivait dans
 * DataStore, et la valeur revenait par le flux avec un temps de retard, écrasant
 * ce qui avait été tapé entre-temps.
 *
 * Ces tests verrouillent le comportement attendu : la frappe est locale et
 * immédiate, l'enregistrement est différé, et le champ ne se laisse pas réécrire
 * par la valeur d'origine tant qu'on tape.
 */
@RunWith(AndroidJUnit4::class)
class DebouncedTextFieldTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun laFrappeResteAffichee_memeSiLaSourceNeSuitPas() {
        // La source ne bouge jamais : c'est exactement le cas où l'écriture
        // asynchrone n'a pas encore abouti.
        compose.setContent {
            DebouncedTextField(
                value = "",
                onValueChange = { },
                label = "Nom",
            )
        }

        compose.onNodeWithText("Nom").performTextInput("Rasoanaivo")
        compose.waitForIdle()

        // Rien ne doit avoir été avalé.
        compose.onNodeWithText("Rasoanaivo").assertExists()
    }

    @Test
    fun l_enregistrementEstDiffere_etNeParPasUneFoisParLettre() = runBlocking {
        val saved = mutableListOf<String>()

        compose.setContent {
            DebouncedTextField(
                value = "",
                onValueChange = { saved += it },
                label = "Nom",
            )
        }

        compose.onNodeWithText("Nom").performTextInput("Hery")
        compose.waitForIdle()

        // Le délai de retombée n'est pas encore écoulé : rien n'est parti.
        assertEquals(emptyList<String>(), saved)

        // Une fois la frappe retombée, une seule écriture, avec le texte complet.
        compose.mainClock.advanceTimeBy(1_000)
        compose.waitForIdle()
        delay(100)

        assertEquals(listOf("Hery"), saved)
    }

    @Test
    fun uneValeurVenueDAilleursEstReprise_quandOnLeDemande() {
        var source by mutableStateOf("")
        var reset by mutableStateOf(0)

        compose.setContent {
            DebouncedTextField(
                value = source,
                onValueChange = { source = it },
                label = "Métier",
                resetKey = reset,
            )
        }

        compose.onNodeWithText("Métier").performTextInput("Infi")
        compose.waitForIdle()

        // Une puce de suggestion écrit la valeur et signale le changement :
        // sans ce signal, le champ resterait sur « Infi ».
        source = "Infirmier"
        reset += 1
        compose.waitForIdle()

        compose.onNodeWithText("Infirmier").assertExists()
    }

    @Test
    fun leChampAfficheLaValeurInitiale_arriveeApresLePremierRendu() {
        var source by mutableStateOf("")

        compose.setContent {
            DebouncedTextField(
                value = source,
                onValueChange = { source = it },
                label = "Nom",
            )
        }

        // DataStore répond après le premier rendu : la valeur doit s'afficher.
        source = "Lumi"
        compose.waitForIdle()

        compose.onNodeWithText("Lumi").assertExists()
    }

    @Test
    fun effacerEntierementLeChampEstPossible() {
        var source by mutableStateOf("Texte")

        compose.setContent {
            DebouncedTextField(
                value = source,
                onValueChange = { source = it },
                label = "Nom",
            )
        }

        compose.onNodeWithText("Texte").performTextReplacement("")
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(1_000)
        compose.waitForIdle()

        // Un champ vidé ne doit pas se remplir tout seul avec l'ancienne valeur.
        assertEquals("", source)
    }
}

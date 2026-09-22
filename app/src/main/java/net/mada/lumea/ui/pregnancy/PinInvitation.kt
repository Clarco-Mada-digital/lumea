package net.mada.lumea.ui.pregnancy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.mada.lumea.data.db.PrenatalCareEntity

/**
 * L'invitation à poser un code, une fois que la fiche contient quelque chose.
 *
 * Demander un code dès l'installation ne marche pas : on ne protège pas un
 * carnet vide, et l'écran de création se ferme d'un geste. Le bon moment est
 * celui où la fiche devient réellement sensible — quand elle contient un groupe
 * sanguin, un traitement, un statut, un numéro de sage-femme.
 *
 * L'invitation est donc déclenchée par le contenu, pas par le calendrier. Elle
 * est refusable, et ne revient pas : insister transformerait un conseil en
 * harcèlement, et la personne finirait par l'ignorer le jour où ça compte.
 */

/** Les champs dont la présence rend la fiche digne d'être protégée. */
private val SENSITIVE_CODES = listOf(
    "GROUPE_SANGUIN",
    "ALLERGIES",
    "TRAITEMENTS",
    "VIH_SYPHILIS",
    "HEMOGLOBINE",
    "CONTACT_URGENCE",
)

/**
 * Vrai dès que l'application affiche quelque chose qu'on ne laisse pas traîner.
 *
 * **Un suivi ouvert suffit à lui seul.** L'écran d'accueil affiche « 12 SA » en
 * gros dès l'ouverture : sans code, une grossesse est révélée à la première
 * personne qui prend le téléphone, avant même d'avoir touché à quoi que ce soit.
 * C'est le cas le plus exposant de toute l'app, et il n'attend pas que la fiche
 * soit remplie.
 *
 * À défaut de suivi, deux informations sensibles suffisent : un statut
 * sérologique noté dans un téléphone sans code n'attend pas d'avoir de la
 * compagnie pour devenir un problème.
 */
fun deservesPin(
    hasOngoingFollowUp: Boolean,
    care: Map<String, PrenatalCareEntity> = emptyMap(),
    caregiverPhone: String = "",
    riskFactors: String = "",
): Boolean {
    if (hasOngoingFollowUp) return true

    var filled = SENSITIVE_CODES.count { care[it]?.note?.isNotBlank() == true }
    if (caregiverPhone.isNotBlank()) filled++
    if (riskFactors.isNotBlank()) filled++
    return filled >= 2
}

@Composable
fun PinInvitationCard(
    onSetPin: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row {
                Icon(
                    Icons.Rounded.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "Protège ce que l'app affiche",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Dès l'ouverture, l'accueil affiche où tu en es — en grand. Sans " +
                    "code, la première personne qui prend ton téléphone le voit avant " +
                    "même d'avoir touché à quoi que ce soit.\n\n" +
                    "Avec un code, l'app se verrouille dès qu'elle passe en " +
                    "arrière-plan, et tout redevient invisible.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onSetPin, modifier = Modifier.weight(1f)) {
                    Text("Définir un code")
                }
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text(
                        "Plus tard",
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }
        }
    }
}

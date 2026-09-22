package net.mada.lumea.ui.ai

import net.mada.lumea.data.prefs.AssistantTone
import net.mada.lumea.data.prefs.Settings

/**
 * Composer une introduction que l'utilisatrice colle elle-même.
 *
 * C'est la seule façon honnête de personnaliser un assistant qu'on ne contrôle
 * pas : l'application n'a aucun accès automatisé au service, donc elle ne peut
 * pas — et ne doit pas — écrire à la place de quelqu'un. Elle prépare le texte,
 * l'affiche en entier, et le copie dans le presse-papiers sur demande. Rien ne
 * part tant que la personne ne colle pas et n'envoie pas.
 *
 * Une règle tenue strictement : **aucune donnée de santé n'entre ici**. Pas de
 * dates de règles, pas de semaines de grossesse, pas de journal. Ce profil décrit
 * un métier et des centres d'intérêt, rien d'autre — et l'écran le dit.
 */
object AiProfile {

    /** Des exemples de métiers ou d'études, pour ne pas partir d'un champ vide. */
    val OCCUPATION_SUGGESTIONS = listOf(
        "Élève au lycée",
        "Étudiante en médecine",
        "Infirmier",
        "Sage-femme",
        "Développeur",
        "Enseignante",
        "Commerçante",
        "Agricultrice",
        "Étudiant en droit",
        "Comptable",
        "Sans emploi pour le moment",
    )

    val INTEREST_SUGGESTIONS = listOf(
        "Révisions et examens",
        "Informatique",
        "Santé",
        "Cuisine",
        "Musique",
        "Sport",
        "Histoire",
        "Langues",
        "Entrepreneuriat",
        "Actualité",
    )

    /** Le nom par défaut, si rien n'est choisi. */
    const val DEFAULT_NAME = "Lumi"

    fun assistantName(settings: Settings): String =
        settings.assistantName.ifBlank { DEFAULT_NAME }

    /** Vrai dès qu'au moins un élément du profil est renseigné. */
    fun isConfigured(settings: Settings): Boolean =
        settings.assistantName.isNotBlank() ||
            settings.userAlias.isNotBlank() ||
            settings.occupation.isNotBlank() ||
            settings.interests.isNotBlank() ||
            settings.assistantNotes.isNotBlank()

    /**
     * Le texte d'introduction, en français, tutoyant l'assistant.
     *
     * Il est volontairement court : une consigne longue se fait oublier au bout de
     * quelques échanges, et personne ne relit un pavé avant de le coller.
     */
    fun introduction(settings: Settings): String = buildString {
        val name = assistantName(settings)
        append("Bonjour. À partir de maintenant, tu es $name, mon assistant personnel.")

        if (settings.userAlias.isNotBlank()) {
            append(" Je m'appelle ${settings.userAlias.trim()}.")
        }
        if (settings.occupation.isNotBlank()) {
            append(" Je suis ${settings.occupation.trim()}.")
        }

        val interests = settings.interests
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (interests.isNotEmpty()) {
            append(" Ce qui m'intéresse : ")
            append(interests.joinToString(", "))
            append(".")
        }

        append("\n\n")
        append(settings.assistantTone.instruction)

        if (settings.occupation.isNotBlank()) {
            append(" Quand c'est possible, prends tes exemples dans mon domaine.")
        }
        append(" Réponds en français.")

        if (settings.assistantNotes.isNotBlank()) {
            append("\n\n")
            append(settings.assistantNotes.trim())
        }

        append("\n\nSi tu n'es pas sûr d'une réponse, dis-le plutôt que d'inventer.")
    }

    /**
     * Le rappel affiché sous l'aperçu.
     *
     * Séparé du texte lui-même pour qu'il ne parte pas dans la conversation : c'est
     * une consigne pour la personne, pas pour l'assistant.
     */
    const val PRIVACY_REMINDER =
        "Ce texte ne contient rien sur ta santé : ni tes règles, ni ta grossesse, " +
            "ni ton journal. Il ne part que si tu le colles toi-même dans la " +
            "conversation."
}

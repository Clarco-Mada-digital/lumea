package net.mada.lumea.domain

import net.mada.lumea.domain.advisor.AdvisorContext
import net.mada.lumea.domain.advisor.Topic
import net.mada.lumea.domain.advisor.adviseOn
import net.mada.lumea.domain.learn.FERTILITY_DISCLAIMER
import net.mada.lumea.domain.learn.lessonTopics
import net.mada.lumea.domain.pregnancy.ALL_CARE_ACTS
import net.mada.lumea.domain.pregnancy.FOOD_AVOID
import net.mada.lumea.domain.pregnancy.FOOD_RECOMMENDED
import net.mada.lumea.domain.pregnancy.INFANT_VACCINES
import net.mada.lumea.domain.pregnancy.MamaCondition
import net.mada.lumea.domain.pregnancy.NEGATIVE_CAUSES
import net.mada.lumea.domain.pregnancy.OPTIONS_MADAGASCAR
import net.mada.lumea.domain.pregnancy.POSTNATAL_VISITS
import net.mada.lumea.domain.pregnancy.POSTPARTUM_ADVICE
import net.mada.lumea.domain.pregnancy.POSTPARTUM_CONTRACEPTION
import net.mada.lumea.domain.pregnancy.PREGNANCY_DISCLAIMER
import net.mada.lumea.domain.pregnancy.PREGNANCY_MILESTONES
import net.mada.lumea.domain.pregnancy.RISK_FACTORS
import net.mada.lumea.domain.pregnancy.SYMPTOM_TRIAGE
import net.mada.lumea.domain.pregnancy.TEST_HOW_TO
import net.mada.lumea.domain.pregnancy.TEST_RECOMMENDED_DAYS
import net.mada.lumea.domain.pregnancy.TEST_SUGGESTION_DAYS
import net.mada.lumea.domain.pregnancy.WARNING_SIGNS
import net.mada.lumea.domain.pregnancy.WHERE_TO_GO
import net.mada.lumea.domain.pregnancy.label
import net.mada.lumea.domain.pregnancy.title
import net.mada.lumea.domain.pregnancy.hint
import net.mada.lumea.domain.pregnancy.Urgency
import net.mada.lumea.domain.pregnancy.testReliability
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Rassemble tout le contenu médical de l'app dans un seul document relisible.
 *
 * Ce contenu est aujourd'hui éparpillé dans six fichiers Kotlin. Or il doit être
 * **relu par une sage-femme ou un médecin malgache avant toute diffusion** : on
 * ne peut pas demander à un soignant de lire du code, et lui envoyer une copie
 * manuelle garantit qu'elle sera périmée au premier changement.
 *
 * D'où ce test : il écrit le document à partir des constantes elles-mêmes. Il ne
 * peut donc jamais diverger de ce que l'application affiche réellement. Le
 * document se régénère avec :
 *
 * ```
 * ./gradlew testDebugUnitTest --tests '*MedicalContentExportTest*'
 * ```
 *
 * Il atterrit dans `docs/CONTENU-MEDICAL.md`.
 */
class MedicalContentExportTest {

    @Test
    fun `le document de relecture medicale est a jour`() {
        val doc = buildDocument()
        val out = File("../docs/CONTENU-MEDICAL.md")
        out.parentFile?.mkdirs()
        out.writeText(doc)

        // Garde-fou minimal : si une section disparaît du code, elle disparaît du
        // document, et la relecture porterait sur un texte incomplet sans qu'on
        // le voie.
        listOf(
            "Consultations prénatales",
            "Vaccination",
            "Paludisme",
            "Signes qui doivent alerter",
            "Après la naissance",
            "Réponses du conseiller",
        ).forEach { section ->
            assertTrue("section manquante dans l'export : $section", section in doc)
        }
        assertTrue("le document semble trop court", doc.length > 15_000)
    }

    private fun buildDocument(): String = buildString {
        appendLine("# Lumea — contenu médical à relire")
        appendLine()
        appendLine("> **Document généré automatiquement depuis le code.** Ne pas le modifier")
        appendLine("> à la main : les corrections doivent être reportées dans les sources")
        appendLine("> (`domain/pregnancy/`, `domain/advisor/`, `domain/learn/`), puis le")
        appendLine("> document régénéré. Il reflète donc toujours ce que l'application")
        appendLine("> affiche réellement.")
        appendLine()
        appendLine("## À l'attention du relecteur")
        appendLine()
        appendLine("Lumea est une application de suivi de cycle, de grossesse et d'après-")
        appendLine("naissance destinée à un public jeune, à Madagascar. Le contenu suit les")
        appendLine("recommandations de l'OMS reprises par Madagascar (8 contacts prénatals,")
        appendLine("TPIg dès 13 SA, VAT, fer et acide folique).")
        appendLine()
        appendLine("**Ce qui mérite votre attention en priorité :**")
        appendLine()
        appendLine("1. Les seuils de tri d'urgence (section « Quand appeler, quand partir »).")
        appendLine("2. Le calendrier des CPN, vaccins et doses de TPIg.")
        appendLine("3. Les conditions de la MAMA et ce qui est annoncé comme protecteur.")
        appendLine("4. Tout ce qui nomme un médicament, une dose ou un délai.")
        appendLine("5. La formulation sur l'interruption de grossesse, au regard de la loi.")
        appendLine()
        appendLine("---")
        appendLine()

        section("Avertissements affichés dans l'application")
        appendLine("**Écran grossesse :** $PREGNANCY_DISCLAIMER")
        appendLine()
        appendLine("**Écran cycle :** $FERTILITY_DISCLAIMER")
        appendLine()

        section("Test de grossesse — à partir de quand")
        appendLine("L'application ne propose aucun test avant **$TEST_SUGGESTION_DAYS jours**")
        appendLine("de retard, et ne le conseille vraiment qu'à partir de")
        appendLine("**$TEST_RECOMMENDED_DAYS jours**.")
        appendLine()
        listOf(0, 3, 5, 8, 25).forEach { late ->
            val r = testReliability(late)
            appendLine("### À $late jour(s) de retard — « ${r.title} »")
            appendLine()
            appendLine(r.body)
            appendLine()
        }
        appendLine("### Mode d'emploi affiché")
        appendLine()
        TEST_HOW_TO.forEach { (t, b) -> appendLine("- **$t** — $b") }
        appendLine()
        appendLine("### Causes d'un test négatif malgré le retard")
        appendLine()
        NEGATIVE_CAUSES.forEach { (t, b) -> appendLine("- **$t** — $b") }
        appendLine()

        section("Carnet de suivi prénatal")
        ALL_CARE_ACTS.groupBy { it.category }.forEach { (category, acts) ->
            appendLine("### ${category.label}")
            appendLine()
            acts.forEach { act ->
                appendLine("**${act.title}**  _(${act.fromWeek} à ${act.toWeek} SA — code `${act.code}`)_")
                appendLine()
                appendLine(act.why)
                appendLine()
            }
        }

        section("Étapes affichées en résumé")
        PREGNANCY_MILESTONES.forEach { step ->
            appendLine("**${step.title}**  _(${step.fromWeek} à ${step.toWeek} SA)_")
            appendLine()
            appendLine(step.body)
            appendLine()
        }

        section("Alimentation")
        appendLine("### Recommandé")
        appendLine()
        FOOD_RECOMMENDED.forEach { appendLine("- **${it.title}** — ${it.body}") }
        appendLine()
        appendLine("### À éviter")
        appendLine()
        FOOD_AVOID.forEach { appendLine("- **${it.title}** — ${it.body}") }
        appendLine()

        section("Signes qui doivent alerter")
        WARNING_SIGNS.forEach { appendLine("- **${it.sign}** — ${it.what}") }
        appendLine()

        section("Quand appeler, quand partir")
        Urgency.entries.forEach { urgency ->
            appendLine("### ${urgency.title}")
            appendLine()
            appendLine("_${urgency.hint}_")
            appendLine()
            SYMPTOM_TRIAGE.filter { it.urgency == urgency }.forEach {
                appendLine("- **${it.label}** — ${it.detail}")
            }
            appendLine()
        }

        section("Où aller")
        WHERE_TO_GO.forEach { (t, b) -> appendLine("- **$t** — $b") }
        appendLine()

        section("Facteurs de risque proposés")
        RISK_FACTORS.forEach { appendLine("- **${it.label}** — ${it.advice}") }
        appendLine()

        section("Options après un test positif (cadre légal malgache)")
        OPTIONS_MADAGASCAR.forEach { (t, b) -> appendLine("- **$t** — $b") }
        appendLine()

        section("Après la naissance")
        appendLine("### Contraception par allaitement (MAMA) — les trois conditions")
        appendLine()
        MamaCondition.entries.forEach {
            appendLine("- **${it.question}** — ${it.why}")
        }
        appendLine()
        appendLine("### Consultations postnatales")
        appendLine()
        POSTNATAL_VISITS.forEach { appendLine("- **${it.title}** — ${it.why}") }
        appendLine()
        appendLine("### Vaccins du nourrisson")
        appendLine()
        INFANT_VACCINES.forEach { appendLine("- **${it.title}** — ${it.why}") }
        appendLine()
        appendLine("### Conseils à la mère")
        appendLine()
        POSTPARTUM_ADVICE.forEach { (t, b) -> appendLine("- **$t** — $b") }
        appendLine()
        appendLine("### Contraceptions compatibles avec l'allaitement")
        appendLine()
        POSTPARTUM_CONTRACEPTION.forEach { (t, b) -> appendLine("- **$t** — $b") }
        appendLine()

        section("Réponses du conseiller")
        appendLine("Le conseiller répond localement, à partir des données de la personne.")
        appendLine("Les réponses ci-dessous sont celles d'une grossesse en cours ; elles")
        appendLine("changent selon la situation (cycle, grossesse, après-naissance).")
        appendLine()
        val context = AdvisorContext(
            pregnancy = net.mada.lumea.domain.pregnancy.pregnancyProgress(
                java.time.LocalDate.now().minusWeeks(10)
            ),
        )
        Topic.entries.filter { it != Topic.INCONNU }.forEach { topic ->
            val advice = adviseOn(topic, context)
            appendLine("### ${topic.name} — niveau ${advice.level}")
            appendLine()
            appendLine("**${advice.title}**")
            appendLine()
            appendLine(advice.body)
            appendLine()
        }

        section("Leçons éducatives")
        lessonTopics.forEach { topic ->
            appendLine("## ${topic.title}")
            appendLine()
            appendLine("_${topic.subtitle}_")
            appendLine()
            topic.lessons.forEach { lesson ->
                appendLine("### ${lesson.emoji} ${lesson.title}  _(${lesson.minutes} min)_")
                appendLine()
                appendLine("> ${lesson.summary}")
                appendLine()
                lesson.sections.forEach { s ->
                    appendLine("**${s.heading}**")
                    appendLine()
                    appendLine(s.body)
                    appendLine()
                }
            }
        }
    }

    private fun StringBuilder.section(title: String) {
        appendLine("---")
        appendLine()
        appendLine("# $title")
        appendLine()
    }
}

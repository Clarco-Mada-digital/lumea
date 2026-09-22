package net.mada.lumea.domain.advisor

import net.mada.lumea.domain.cycle.CycleInsight
import net.mada.lumea.domain.pregnancy.PostpartumProgress
import net.mada.lumea.domain.pregnancy.PregnancyProgress
import net.mada.lumea.domain.pregnancy.TEST_RECOMMENDED_DAYS
import net.mada.lumea.domain.pregnancy.TEST_SUGGESTION_DAYS
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Le conseiller du cycle : un moteur de règles, pas un modèle de langage.
 *
 * Ce choix est délibéré. Les questions posées ici — « pourquoi mes règles ne sont
 * pas arrivées », « je saigne alors que je suis enceinte » — sont du **tri
 * médical**. Trois raisons de ne pas les confier à une IA générative :
 *
 * 1. **La sécurité.** Une hallucination qui répond « ce n'est probablement rien »
 *    à un saignement du premier trimestre peut coûter une vie. Ici chaque réponse
 *    est écrite à l'avance et relue ; elle ne peut pas dériver.
 * 2. **La confidentialité.** Répondre suppose de connaître les dates, la
 *    grossesse, parfois le statut VIH. Rien de tout cela ne doit quitter le
 *    téléphone, et rien ne le quitte : tout est calculé en local.
 * 3. **La disponibilité.** Ça marche sans connexion, sans forfait, instantanément.
 *    À Madagascar, c'est décisif.
 *
 * En échange, le conseiller ne fait pas semblant de tout comprendre : quand une
 * question sort de son champ, il le dit et propose l'assistant général.
 */

// ------------------------------------------------------------------ Contexte

/** Ce que le conseiller sait de la personne au moment de répondre. */
data class AdvisorContext(
    val insight: CycleInsight? = null,
    val pregnancy: PregnancyProgress? = null,
    val postpartum: PostpartumProgress? = null,
    val mamaProtected: Boolean = false,
    val caregiverName: String = "",
    val caregiverPhone: String = "",
    val nextCareTitle: String? = null,
    val nextCareLate: Boolean = false,
) {
    val isPregnant: Boolean get() = pregnancy != null && postpartum == null
    val isPostpartum: Boolean get() = postpartum != null
    val hasCaregiver: Boolean get() = caregiverPhone.isNotBlank()
}

// ------------------------------------------------------------------ Réponses

/** L'urgence de la réponse. Elle décide de la couleur et de l'ordre des actions. */
enum class AdviceLevel { URGENT, CONSULTER, ATTENTION, INFO, RASSURANT }

/** Ce que le conseiller propose de faire après avoir répondu. */
enum class AdviceAction {
    APPELER_SOIGNANT,
    OUVRIR_SUIVI,
    OUVRIR_CARNET,
    NOTER_JOURNEE,
    FAIRE_TEST,
    OUVRIR_LECONS,
    DEMANDER_IA,
}

val AdviceAction.label: String
    get() = when (this) {
        AdviceAction.APPELER_SOIGNANT -> "Appeler ma sage-femme"
        AdviceAction.OUVRIR_SUIVI -> "Ouvrir mon suivi"
        AdviceAction.OUVRIR_CARNET -> "Voir mon carnet"
        AdviceAction.NOTER_JOURNEE -> "Noter ma journée"
        AdviceAction.FAIRE_TEST -> "Test de grossesse"
        AdviceAction.OUVRIR_LECONS -> "En savoir plus"
        AdviceAction.DEMANDER_IA -> "Poser à l'assistant IA"
    }

data class Advice(
    val level: AdviceLevel,
    val title: String,
    val body: String,
    /** Les chiffres tirés des données réelles, affichés à part : « Retard : 2 j ». */
    val facts: List<Pair<String, String>> = emptyList(),
    val actions: List<AdviceAction> = emptyList(),
)

// ------------------------------------------------------------- Reconnaissance

/** Les sujets que le conseiller sait traiter. */
enum class Topic {
    RETARD, SAIGNEMENT, DOULEUR, FIEVRE, TEST, FERTILITE, CONTRACEPTION,
    NAUSEES, MOUVEMENTS, RENDEZ_VOUS, ALIMENTATION, ALLAITEMENT, HUMEUR, INCONNU,
}

private val KEYWORDS: List<Pair<Topic, List<String>>> = listOf(
    // L'ordre compte : les urgences d'abord, pour qu'une phrase qui mélange
    // plusieurs sujets bascule vers le plus grave.
    Topic.SAIGNEMENT to listOf("saign", "sang", "perte de sang", "hémorragie", "hemorragie"),
    Topic.DOULEUR to listOf("douleur", "mal au ventre", "mal de ventre", "crampe", "ça fait mal", "souffr"),
    Topic.FIEVRE to listOf("fièvre", "fievre", "chaud", "frisson", "palu", "température", "temperature"),
    Topic.MOUVEMENTS to listOf("bouge", "mouvement", "remue", "coup de pied"),
    Topic.RETARD to listOf("retard", "pas arrivé", "pas arrive", "pas venu", "tardent", "absente", "aménorrhée", "amenorrhee"),
    Topic.TEST to listOf("test", "enceinte ?", "suis-je enceinte", "je suis enceinte", "hcg", "prise de sang"),
    Topic.FERTILITE to listOf("fertile", "ovulation", "risque", "tomber enceinte", "rapport", "danger"),
    Topic.CONTRACEPTION to listOf("contracept", "pilule", "préservatif", "preservatif", "implant", "stérilet", "sterilet", "injection"),
    Topic.NAUSEES to listOf("nausée", "nausee", "vomi", "envie de vomir", "fatigue", "seins"),
    Topic.RENDEZ_VOUS to listOf("rendez-vous", "rdv", "cpn", "consultation", "vaccin", "vat", "tpi", "échographie", "echographie"),
    Topic.ALIMENTATION to listOf("manger", "aliment", "nourriture", "boire", "régime", "regime", "vitamine", "fer"),
    Topic.ALLAITEMENT to listOf("allait", "sein", "lait", "tétée", "tetee", "mama"),
    Topic.HUMEUR to listOf("triste", "déprim", "deprim", "moral", "anxieu", "stress", "pleure"),
)

fun detectTopic(question: String): Topic {
    val normalized = question.lowercase(Locale.FRENCH)
    return KEYWORDS.firstOrNull { (_, words) -> words.any { it in normalized } }?.first
        ?: Topic.INCONNU
}

// ----------------------------------------------------------------- Le moteur

private val dayMonth = DateTimeFormatter.ofPattern("d MMMM", Locale.FRENCH)

fun advise(question: String, context: AdvisorContext): Advice =
    adviseOn(detectTopic(question), context)

fun adviseOn(topic: Topic, context: AdvisorContext): Advice = when (topic) {
    Topic.SAIGNEMENT -> bleeding(context)
    Topic.DOULEUR -> pain(context)
    Topic.FIEVRE -> fever(context)
    Topic.MOUVEMENTS -> movements(context)
    Topic.RETARD -> lateness(context)
    Topic.TEST -> test(context)
    Topic.FERTILITE -> fertility(context)
    Topic.CONTRACEPTION -> contraception(context)
    Topic.NAUSEES -> nausea(context)
    Topic.RENDEZ_VOUS -> appointments(context)
    Topic.ALIMENTATION -> food(context)
    Topic.ALLAITEMENT -> breastfeeding(context)
    Topic.HUMEUR -> mood(context)
    Topic.INCONNU -> unknown()
}

/** Les actions d'urgence : appeler si le numéro est connu, puis ouvrir le suivi. */
private fun urgentActions(context: AdvisorContext): List<AdviceAction> = buildList {
    if (context.hasCaregiver) add(AdviceAction.APPELER_SOIGNANT)
    if (context.isPregnant || context.isPostpartum) add(AdviceAction.OUVRIR_SUIVI)
    add(AdviceAction.OUVRIR_LECONS)
}

// --- Saignement -------------------------------------------------------------

private fun bleeding(c: AdvisorContext): Advice = when {
    c.isPregnant -> Advice(
        level = AdviceLevel.URGENT,
        title = "Un saignement pendant la grossesse se fait toujours voir",
        body = buildString {
            append("Une grossesse ne saigne pas normalement. Beaucoup de saignements ")
            append("sont sans gravité, mais ce n'est pas à toi — ni à une application — ")
            append("d'en décider.\n\n")
            if ((c.pregnancy?.weeks ?: 0) < 15) {
                append("Au premier trimestre, un saignement accompagné d'une douleur ")
                append("d'un seul côté du bas-ventre peut signaler une grossesse hors ")
                append("de l'utérus : c'est une urgence chirurgicale.\n\n")
            } else {
                append("Après le premier trimestre, un saignement peut venir du ")
                append("placenta. Il se vérifie en maternité, pas au téléphone.\n\n")
            }
            append("Si le saignement est abondant, ne téléphone pas d'abord : pars au ")
            append("CSB-II ou à la maternité, de jour comme de nuit, et fais prévenir ")
            append("en chemin.")
        },
        facts = pregnancyFacts(c),
        actions = urgentActions(c),
    )

    c.isPostpartum && (c.postpartum?.inImmediatePostpartum == true) -> Advice(
        level = AdviceLevel.URGENT,
        title = "Après l'accouchement, un saignement abondant est une urgence",
        body = "L'hémorragie est la première cause de décès maternel, et c'est celle " +
            "qui va le plus vite. Des pertes qui diminuent progressivement sont " +
            "normales ; un saignement qui reprend, qui devient abondant, ou qui " +
            "s'accompagne de fièvre ou d'une mauvaise odeur, ne l'est pas. Va au CSB.",
        facts = listOf("Depuis la naissance" to "${c.postpartum?.daysSince ?: 0} jours"),
        actions = urgentActions(c),
    )

    c.isPostpartum -> Advice(
        level = AdviceLevel.ATTENTION,
        title = "C'est peut-être le retour de couches",
        body = "Les premières règles après un accouchement s'appellent le retour de " +
            "couches. Si c'est le cas, indique-le dans ton suivi : l'allaitement ne " +
            "te protège plus d'une grossesse à partir de maintenant, et le suivi de " +
            "cycle repartira de cette date.\n\nSi le saignement est abondant, " +
            "douloureux, ou accompagné de fièvre, va au CSB : ce n'est pas un retour " +
            "de couches ordinaire.",
        facts = listOf("Depuis la naissance" to "${c.postpartum?.daysSince ?: 0} jours"),
        actions = listOf(AdviceAction.OUVRIR_SUIVI, AdviceAction.OUVRIR_LECONS),
    )

    else -> Advice(
        level = AdviceLevel.INFO,
        title = "Ça dépend du moment de ton cycle",
        body = buildString {
            val day = c.insight?.dayOfCycle
            if (day != null && day > 0) {
                append("Tu es au jour $day de ton cycle. ")
            }
            append("Des saignements légers en milieu de cycle, ou juste avant les ")
            append("règles, sont fréquents et rarement inquiétants. De même après un ")
            append("rapport : le col est plus fragile.\n\n")
            append("Ce qui mérite un avis : un saignement abondant qui dure plus de ")
            append("sept jours, des saignements entre les règles qui reviennent tous ")
            append("les mois, ou un saignement accompagné de douleurs fortes.\n\n")
            append("Si ce sont tes règles, marque-les dans le calendrier : c'est ce ")
            append("qui garde les prévisions justes.")
        },
        facts = cycleFacts(c),
        actions = listOf(AdviceAction.NOTER_JOURNEE, AdviceAction.OUVRIR_LECONS),
    )
}

// --- Douleur ----------------------------------------------------------------

private fun pain(c: AdvisorContext): Advice = when {
    c.isPregnant -> Advice(
        level = AdviceLevel.CONSULTER,
        title = "Quelle douleur, et où ?",
        body = "Des tiraillements dans le bas-ventre, surtout en fin de grossesse, " +
            "sont ordinaires : l'utérus s'étire.\n\nCe qui n'est pas ordinaire et se " +
            "fait voir le jour même : une douleur forte, une douleur qui ne passe " +
            "pas, une douleur d'un seul côté en début de grossesse, ou des " +
            "contractions régulières avant 37 semaines.\n\nUn dernier point : " +
            "l'ibuprofène et tous les anti-inflammatoires sont interdits à partir de " +
            "24 SA et déconseillés avant. Le paracétamol est l'option de première " +
            "intention.",
        facts = pregnancyFacts(c),
        actions = urgentActions(c),
    )

    else -> Advice(
        level = AdviceLevel.INFO,
        title = "Les douleurs de règles sont fréquentes, pas obligatoires",
        body = "Des crampes pendant les règles touchent la majorité des femmes. La " +
            "chaleur sur le ventre, le mouvement doux et le paracétamol aident " +
            "réellement.\n\nCe qui justifie une consultation : une douleur qui " +
            "t'empêche d'aller en cours ou au travail, qui empire d'un cycle à " +
            "l'autre, ou qui ne cède à rien. Ce n'est pas « normal d'avoir mal » à " +
            "ce point, et ça se traite.",
        facts = cycleFacts(c),
        actions = listOf(AdviceAction.NOTER_JOURNEE, AdviceAction.OUVRIR_LECONS),
    )
}

// --- Fièvre -----------------------------------------------------------------

private fun fever(c: AdvisorContext): Advice = Advice(
    level = AdviceLevel.URGENT,
    title = "Une fièvre se fait tester le jour même",
    body = buildString {
        append("À Madagascar, toute fièvre chez une femme enceinte ou qui vient ")
        append("d'accoucher doit faire chercher le paludisme sans attendre : il est ")
        append("bien plus grave pendant la grossesse, pour la mère comme pour le bébé.")
        if (c.isPregnant) {
            append("\n\nUne fièvre peut aussi venir d'une infection urinaire, fréquente ")
            append("et capable de déclencher un accouchement prématuré si on la laisse ")
            append("traîner.")
        }
        append("\n\nVa au CSB. Ne prends pas de médicament au hasard en attendant.")
    },
    facts = if (c.isPregnant) pregnancyFacts(c) else emptyList(),
    actions = urgentActions(c),
)

// --- Mouvements du bébé -----------------------------------------------------

private fun movements(c: AdvisorContext): Advice {
    val weeks = c.pregnancy?.weeks ?: 0
    return when {
        !c.isPregnant -> unknown()
        weeks < 20 -> Advice(
            level = AdviceLevel.RASSURANT,
            title = "C'est encore tôt pour les sentir",
            body = "Les premiers mouvements se perçoivent en général entre 18 et 22 " +
                "semaines, parfois plus tard pour une première grossesse. Ne pas " +
                "encore les sentir à $weeks SA n'a rien d'anormal.",
            facts = pregnancyFacts(c),
            actions = listOf(AdviceAction.OUVRIR_SUIVI),
        )
        else -> Advice(
            level = AdviceLevel.URGENT,
            title = "Un bébé qui bouge moins se vérifie sur place",
            body = "À $weeks SA, tu dois sentir ton bébé bouger régulièrement. Une " +
                "baisse nette des mouvements sur une journée ne s'attend pas : va à " +
                "la maternité pour un contrôle.\n\nOn préfère mille fois un " +
                "déplacement pour rien qu'une heure perdue.",
            facts = pregnancyFacts(c),
            actions = urgentActions(c),
        )
    }
}

// --- Retard -----------------------------------------------------------------

private fun lateness(c: AdvisorContext): Advice = when {
    c.isPregnant -> Advice(
        level = AdviceLevel.RASSURANT,
        title = "Tu n'attends pas de règles",
        body = "Ton suivi de grossesse est en cours : les règles s'arrêtent pendant " +
            "toute la grossesse, c'est d'ailleurs de là que vient le mot " +
            "« aménorrhée » dans « semaines d'aménorrhée ».\n\nEn revanche, un " +
            "saignement pendant la grossesse n'est jamais à ignorer.",
        facts = pregnancyFacts(c),
        actions = listOf(AdviceAction.OUVRIR_SUIVI),
    )

    c.isPostpartum -> Advice(
        level = AdviceLevel.RASSURANT,
        title = "C'est normal après un accouchement",
        body = buildString {
            append("L'allaitement suspend les règles, souvent pendant plusieurs mois. ")
            append("Leur retour — le retour de couches — peut mettre du temps.\n\n")
            if (c.mamaProtected) {
                append("Attention : l'allaitement te protège actuellement d'une ")
                append("grossesse, mais l'ovulation revient AVANT les règles. On peut ")
                append("retomber enceinte sans avoir eu un seul cycle entre les deux.")
            } else {
                append("Attention : l'allaitement ne te protège plus d'une grossesse, ")
                append("et l'ovulation revient avant les règles. Un retard n'a donc ")
                append("rien de rassurant en soi.")
            }
        },
        facts = listOf("Depuis la naissance" to "${c.postpartum?.monthsSince ?: 0} mois"),
        actions = listOf(AdviceAction.OUVRIR_SUIVI, AdviceAction.OUVRIR_LECONS),
    )

    c.insight?.lastPeriodStart == null -> Advice(
        level = AdviceLevel.INFO,
        title = "Je n'ai pas encore tes dates",
        body = "Pour te répondre sur un retard, il me faut au moins la date de tes " +
            "dernières règles. Enregistre-la et je pourrai te dire si tu es " +
            "réellement en retard, et de combien.",
        actions = listOf(AdviceAction.NOTER_JOURNEE),
    )

    else -> {
        val late = c.insight.daysLate
        when {
            late == 0 -> Advice(
                level = AdviceLevel.RASSURANT,
                title = "Elles sont attendues aujourd'hui",
                body = "D'après ton cycle moyen, c'est aujourd'hui. Un jour ou deux " +
                    "d'écart n'a rien d'inhabituel. Marque-les dès le premier jour de " +
                    "saignement : c'est ce qui garde les prévisions justes.",
                facts = cycleFacts(c),
                actions = listOf(AdviceAction.NOTER_JOURNEE),
            )

            late < TEST_SUGGESTION_DAYS -> Advice(
                level = AdviceLevel.RASSURANT,
                title = "Pas de quoi s'alarmer",
                body = "Tu es à $late jour${plural(late)} de retard. Un décalage de un " +
                    "à quatre jours arrive à presque tout le monde, et plusieurs fois " +
                    "par an : fatigue, stress, sommeil décalé, un peu plus de sport, " +
                    "un changement de rythme.\n\nUn test de grossesse n'a de sens " +
                    "qu'à partir de $TEST_SUGGESTION_DAYS jours de retard. En faire " +
                    "un maintenant, c'est surtout dépenser pour un doute qui reste.",
                facts = cycleFacts(c),
                actions = listOf(AdviceAction.NOTER_JOURNEE, AdviceAction.OUVRIR_LECONS),
            )

            late < TEST_RECOMMENDED_DAYS -> Advice(
                level = AdviceLevel.ATTENTION,
                title = "Le retard commence à se voir",
                body = "Tu es à $late jours de retard. Ça reste dans ce qui arrive " +
                    "couramment, mais si un rapport non protégé a eu lieu, un test " +
                    "urinaire est fiable dès maintenant. Rien ne presse : attendre " +
                    "d'arriver à une semaine pleine évite de le faire pour rien.",
                facts = cycleFacts(c),
                actions = listOf(AdviceAction.FAIRE_TEST, AdviceAction.NOTER_JOURNEE),
            )

            late < 21 -> Advice(
                level = AdviceLevel.ATTENTION,
                title = "Une semaine de retard : le test a du sens",
                body = "Tu es à $late jours de retard. C'est le moment de faire un " +
                    "test, avec les premières urines du matin — elles sont plus " +
                    "concentrées, le résultat est plus net.\n\nS'il est négatif et " +
                    "que les règles ne viennent toujours pas, refais-en un dans 3 à " +
                    "5 jours.",
                facts = cycleFacts(c),
                actions = listOf(AdviceAction.FAIRE_TEST, AdviceAction.OUVRIR_LECONS),
            )

            else -> Advice(
                level = AdviceLevel.CONSULTER,
                title = "Un test, puis un avis médical",
                body = "Tu es à $late jours de retard. Fais un test, et prends " +
                    "rendez-vous quel que soit le résultat.\n\nUn retard qui dure " +
                    "avec un test négatif mérite d'être expliqué : thyroïde, ovaires " +
                    "polykystiques, effet d'une contraception, perte de poids. Ça se " +
                    "diagnostique et ça se traite. Un médecin généraliste, une " +
                    "sage-femme ou un centre de santé suffisent pour commencer.",
                facts = cycleFacts(c),
                actions = buildList {
                    add(AdviceAction.FAIRE_TEST)
                    if (c.hasCaregiver) add(AdviceAction.APPELER_SOIGNANT)
                    add(AdviceAction.OUVRIR_LECONS)
                },
            )
        }
    }
}

// --- Test -------------------------------------------------------------------

private fun test(c: AdvisorContext): Advice = when {
    c.isPregnant -> Advice(
        level = AdviceLevel.RASSURANT,
        title = "Ton suivi est déjà ouvert",
        body = "Un test positif a été enregistré et le suivi de grossesse est en " +
            "cours. Si tu as un doute sur le résultat, une prise de sang en " +
            "laboratoire dose l'hormone précisément.",
        facts = pregnancyFacts(c),
        actions = listOf(AdviceAction.OUVRIR_SUIVI),
    )
    else -> {
        val late = c.insight?.daysLate ?: 0
        Advice(
            level = if (late >= TEST_RECOMMENDED_DAYS) AdviceLevel.ATTENTION else AdviceLevel.INFO,
            title = if (late >= TEST_SUGGESTION_DAYS) "Oui, c'est le moment"
            else "Un test maintenant ne t'apprendrait pas grand-chose",
            body = buildString {
                if (late >= TEST_SUGGESTION_DAYS) {
                    append("Tu es à $late jours de retard : un test urinaire est ")
                    append("fiable. Fais-le le matin.\n\n")
                } else if (late > 0) {
                    append("Tu n'es qu'à $late jour${plural(late)} de retard. Un test ")
                    append("serait techniquement fiable, mais il n'y a pas de raison ")
                    append("d'en faire un si tôt.\n\n")
                } else {
                    append("Tu n'es pas en retard pour l'instant. Avant la date ")
                    append("prévue, un test peut être négatif à tort.\n\n")
                }
                append("Les tests s'achètent en pharmacie ou en supermarché, sans ")
                append("ordonnance et sans justification d'âge. Les moins chers ")
                append("détectent la même hormone que les plus chers. Une deuxième ")
                append("ligne, même très pâle, est un résultat positif.")
            },
            facts = cycleFacts(c),
            actions = listOf(AdviceAction.FAIRE_TEST, AdviceAction.OUVRIR_LECONS),
        )
    }
}

// --- Fertilité --------------------------------------------------------------

private fun fertility(c: AdvisorContext): Advice = when {
    c.isPregnant -> Advice(
        level = AdviceLevel.INFO,
        title = "Tu es déjà enceinte",
        body = "La question du risque de grossesse ne se pose plus pour le moment. " +
            "Le préservatif reste utile pendant la grossesse : il protège des " +
            "infections sexuellement transmissibles, qui peuvent atteindre le bébé.",
        facts = pregnancyFacts(c),
        actions = listOf(AdviceAction.OUVRIR_SUIVI),
    )

    c.isPostpartum -> Advice(
        level = if (c.mamaProtected) AdviceLevel.INFO else AdviceLevel.ATTENTION,
        title = if (c.mamaProtected) "L'allaitement te protège, sous conditions"
        else "Tu peux tomber enceinte dès maintenant",
        body = if (c.mamaProtected) {
            "Les trois conditions de la MAMA sont réunies : bébé de moins de 6 mois, " +
                "allaitement exclusif jour et nuit, pas de règles revenues. Tant " +
                "qu'elles tiennent toutes les trois, la protection est d'environ 98 %." +
                "\n\nDès qu'une seule tombe — les 6 mois, une première nuit complète " +
                "de sommeil, le moindre saignement — elle s'arrête le jour même."
        } else {
            "Les conditions de l'allaitement protecteur ne sont plus réunies. " +
                "L'ovulation revient avant les règles : on peut retomber enceinte " +
                "sans avoir eu un seul cycle entre les deux.\n\nL'OMS conseille " +
                "d'attendre environ deux ans avant une nouvelle grossesse. Plusieurs " +
                "contraceptions sont compatibles avec l'allaitement — demande-les à " +
                "la consultation postnatale."
        },
        actions = listOf(AdviceAction.OUVRIR_SUIVI, AdviceAction.OUVRIR_LECONS),
    )

    else -> Advice(
        level = AdviceLevel.INFO,
        title = "Ça dépend du jour, mais aucun jour n'est à zéro",
        body = buildString {
            val ovulation = c.insight?.ovulationDate
            val fertile = c.insight?.fertileWindow
            if (ovulation != null && fertile != null) {
                append("D'après tes cycles, ta fenêtre fertile estimée va du ")
                append(fertile.start.format(dayMonth))
                append(" au ")
                append(fertile.endInclusive.format(dayMonth))
                append(", avec l'ovulation vers le ")
                append(ovulation.format(dayMonth))
                append(".\n\n")
            }
            append("Ce sont des estimations calculées sur tes cycles passés, pas des ")
            append("certitudes : un cycle se décale sans prévenir, et les ")
            append("spermatozoïdes vivent jusqu'à cinq jours. C'est pour ça que la ")
            append("méthode du calendrier n'est pas une contraception.\n\n")
            append("Le préservatif est le seul moyen qui protège à la fois d'une ")
            append("grossesse et des IST.")
        },
        facts = cycleFacts(c),
        actions = listOf(AdviceAction.OUVRIR_LECONS),
    )
}

// --- Contraception ----------------------------------------------------------

private fun contraception(c: AdvisorContext): Advice = Advice(
    level = AdviceLevel.INFO,
    title = if (c.isPostpartum) "Compatibles avec l'allaitement"
    else "Ce qui marche vraiment",
    body = if (c.isPostpartum) {
        "Le préservatif est disponible tout de suite. La pilule sans œstrogène, " +
            "l'implant, l'injection et le stérilet sont tous compatibles avec " +
            "l'allaitement ; ils se demandent au CSB, et la consultation postnatale " +
            "de six semaines est prévue pour ça.\n\nAucun n'a d'effet sur ton lait."
    } else {
        "Les méthodes les plus efficaces sont celles qu'on ne peut pas oublier : " +
            "implant et stérilet. La pilule demande une prise très régulière pour " +
            "tenir ses promesses.\n\nLe préservatif est le seul à protéger aussi des " +
            "infections sexuellement transmissibles — d'où l'intérêt de le combiner.\n\n" +
            "Les leçons de l'app détaillent les chiffres d'efficacité en usage réel, " +
            "pas en usage parfait."
    },
    actions = listOf(AdviceAction.OUVRIR_LECONS),
)

// --- Nausées et signes de grossesse -----------------------------------------

private fun nausea(c: AdvisorContext): Advice = when {
    c.isPregnant -> Advice(
        level = AdviceLevel.INFO,
        title = "Désagréable, mais ordinaire",
        body = "Nausées, seins tendus, fatigue et envies fréquentes d'uriner sont les " +
            "signes classiques du début de grossesse. Ils s'atténuent le plus souvent " +
            "après le premier trimestre.\n\nCe qui n'est pas ordinaire : ne plus rien " +
            "garder du tout pendant 24 heures. Ça déshydrate, et ça se traite — " +
            "appelle ou va au CSB.",
        facts = pregnancyFacts(c),
        actions = urgentActions(c),
    )
    else -> Advice(
        level = AdviceLevel.INFO,
        title = "Ça peut être hormonal",
        body = "Fatigue, seins sensibles et nausées légères arrivent souvent juste " +
            "avant les règles : les hormones montent et descendent au fil du cycle.\n\n" +
            "Si ces signes s'accompagnent d'un retard, un test de grossesse répondra " +
            "mieux que moi.",
        facts = cycleFacts(c),
        actions = listOf(AdviceAction.NOTER_JOURNEE, AdviceAction.FAIRE_TEST),
    )
}

// --- Rendez-vous ------------------------------------------------------------

private fun appointments(c: AdvisorContext): Advice = when {
    c.nextCareTitle != null -> Advice(
        level = if (c.nextCareLate) AdviceLevel.ATTENTION else AdviceLevel.INFO,
        title = if (c.nextCareLate) "Tu as du retard dans ton suivi"
        else "Ta prochaine étape",
        body = buildString {
            append(c.nextCareTitle)
            append("\n\n")
            if (c.nextCareLate) {
                append("Un rendez-vous manqué ne se rattrape pas tout seul, mais il ")
                append("se rattrape : va au CSB dès que tu peux, on reprendra là où ")
                append("tu en es.")
            } else {
                append("Tu peux programmer tous tes rendez-vous dans l'agenda depuis ")
                append("ton carnet de suivi : l'app te préviendra la veille.")
            }
        },
        actions = buildList {
            add(AdviceAction.OUVRIR_CARNET)
            if (c.hasCaregiver) add(AdviceAction.APPELER_SOIGNANT)
        },
    )
    c.isPregnant || c.isPostpartum -> Advice(
        level = AdviceLevel.RASSURANT,
        title = "Tout est coché",
        body = "Il n'y a rien en attente dans ton carnet pour l'instant. La prochaine " +
            "étape apparaîtra quand son moment sera venu.",
        actions = listOf(AdviceAction.OUVRIR_CARNET),
    )
    else -> Advice(
        level = AdviceLevel.INFO,
        title = "Le carnet de suivi s'ouvre avec une grossesse",
        body = "Les consultations prénatales, les vaccins et le traitement préventif " +
            "du paludisme apparaissent dès qu'un suivi de grossesse est ouvert.",
        actions = listOf(AdviceAction.OUVRIR_LECONS),
    )
}

// --- Alimentation -----------------------------------------------------------

private fun food(c: AdvisorContext): Advice = when {
    c.isPregnant || c.isPostpartum -> Advice(
        level = AdviceLevel.INFO,
        title = "Le fer avant tout",
        body = "L'anémie est la complication la plus fréquente ici, et elle augmente " +
            "le risque d'hémorragie à l'accouchement. Les comprimés fer + acide " +
            "folique sont gratuits à la CPN : demande-les si on ne t'en donne pas.\n\n" +
            "Côté assiette : brèdes, feuilles de manioc, moringa (ananambo), " +
            "lentilles, haricots, petits poissons entiers, abats. Un fruit acide au " +
            "repas aide à absorber le fer ; le thé et le café pris en mangeant le " +
            "bloquent.\n\nÀ éviter : alcool (aucune dose n'est sans risque), viande " +
            "et poisson crus, lait non bouilli, eau non traitée.",
        facts = if (c.isPregnant) pregnancyFacts(c) else emptyList(),
        actions = listOf(AdviceAction.OUVRIR_SUIVI),
    )
    else -> Advice(
        level = AdviceLevel.INFO,
        title = "Le fer compte surtout pendant les règles",
        body = "Les règles font perdre du fer chaque mois, et la fatigue qui va avec " +
            "est souvent mise sur le compte du stress. Brèdes, lentilles, haricots, " +
            "viande, petits poissons entiers.\n\nUne fatigue importante, un " +
            "essoufflement ou des vertiges méritent une prise de sang : l'anémie se " +
            "corrige simplement quand on la connaît.",
        actions = listOf(AdviceAction.OUVRIR_LECONS),
    )
}

// --- Allaitement ------------------------------------------------------------

private fun breastfeeding(c: AdvisorContext): Advice = when {
    c.isPostpartum -> Advice(
        level = if (c.mamaProtected) AdviceLevel.INFO else AdviceLevel.ATTENTION,
        title = "L'allaitement, et ce qu'il protège vraiment",
        body = buildString {
            append("L'allaitement exclusif est recommandé jusqu'aux 6 mois du bébé : ")
            append("rien d'autre que le sein, pas même de l'eau. Ensuite on continue ")
            append("en ajoutant d'autres aliments, idéalement jusqu'à 2 ans.\n\n")
            if (c.mamaProtected) {
                append("Côté contraception, les trois conditions de la MAMA sont ")
                append("réunies chez toi : la protection est d'environ 98 %, mais ")
                append("elle s'arrête dès qu'une seule condition tombe.")
            } else {
                append("Côté contraception, les conditions ne sont plus réunies : ")
                append("l'allaitement ne te protège plus d'une grossesse.")
            }
            append("\n\nUn sein rouge et douloureux avec de la fièvre se fait voir : ")
            append("c'est une mastite, et ça se soigne sans arrêter l'allaitement.")
        },
        actions = listOf(AdviceAction.OUVRIR_SUIVI, AdviceAction.OUVRIR_LECONS),
    )
    else -> unknown()
}

// --- Humeur -----------------------------------------------------------------

private fun mood(c: AdvisorContext): Advice = Advice(
    level = AdviceLevel.INFO,
    title = "Ce n'est ni dans ta tête, ni un défaut de caractère",
    body = buildString {
        append("Les hormones du cycle agissent sur le sommeil, l'appétit, la ")
        append("concentration et l'humeur. Se sentir à fleur de peau à certains ")
        append("moments du mois a une cause physique.\n\n")
        if (c.isPostpartum) {
            append("Après un accouchement, une tristesse passagère dans les premiers ")
            append("jours est très fréquente. Si elle dure au-delà de deux semaines, ")
            append("si tu n'arrives plus à t'occuper de toi ou du bébé, il faut en ")
            append("parler : la dépression du post-partum se soigne, et ce n'est pas ")
            append("un échec.\n\n")
        }
        append("Noter ton humeur chaque jour dans le journal finit par montrer un ")
        append("rythme — et voir ce rythme aide souvent plus qu'on ne le croit.")
    },
    actions = listOf(AdviceAction.NOTER_JOURNEE, AdviceAction.OUVRIR_LECONS),
)

// --- Hors champ -------------------------------------------------------------

private fun unknown(): Advice = Advice(
    level = AdviceLevel.INFO,
    title = "Je ne sais pas répondre à celle-là",
    body = "Je ne traite que ce qui touche au cycle, à la grossesse et à l'après-" +
        "naissance, et uniquement à partir de tes données enregistrées ici. Je " +
        "préfère te le dire plutôt que d'inventer une réponse.\n\nPour une question " +
        "générale, l'assistant IA de l'app peut t'aider — mais il s'ouvre dans un " +
        "service extérieur : n'y écris pas tes données de santé.",
    actions = listOf(AdviceAction.DEMANDER_IA, AdviceAction.OUVRIR_LECONS),
)

// ------------------------------------------------------------------- Chiffres

private fun cycleFacts(c: AdvisorContext): List<Pair<String, String>> = buildList {
    val insight = c.insight ?: return@buildList
    if (insight.dayOfCycle > 0) add("Jour du cycle" to "${insight.dayOfCycle}")
    if (insight.daysLate > 0) add("Retard" to "${insight.daysLate} j")
    insight.expectedPeriodStart?.let { add("Règles attendues" to it.format(dayMonth)) }
    add("Cycle moyen" to "${insight.averageCycleLength} j")
}

private fun pregnancyFacts(c: AdvisorContext): List<Pair<String, String>> = buildList {
    val p = c.pregnancy ?: return@buildList
    add("Grossesse" to p.label)
    add("Trimestre" to "${p.trimester}ᵉ")
    add("Terme prévu" to p.dueDate.format(dayMonth))
    if (c.caregiverName.isNotBlank()) add("Soignant" to c.caregiverName)
}

private fun plural(n: Int) = if (n > 1) "s" else ""

/**
 * Les questions proposées d'emblée, adaptées à la situation.
 *
 * Écrire une question est un effort ; en choisir une ne l'est pas. Les propositions
 * changent selon qu'on suit un cycle, une grossesse ou un après-naissance.
 */
fun suggestedQuestions(c: AdvisorContext): List<Pair<String, Topic>> = when {
    c.isPregnant -> listOf(
        "J'ai des saignements, c'est grave ?" to Topic.SAIGNEMENT,
        "J'ai mal au ventre" to Topic.DOULEUR,
        "J'ai de la fièvre" to Topic.FIEVRE,
        "Mon bébé bouge moins" to Topic.MOUVEMENTS,
        "C'est quand mon prochain rendez-vous ?" to Topic.RENDEZ_VOUS,
        "Qu'est-ce que je peux manger ?" to Topic.ALIMENTATION,
        "J'ai des nausées" to Topic.NAUSEES,
    )

    c.isPostpartum -> listOf(
        "Est-ce que je peux tomber enceinte ?" to Topic.FERTILITE,
        "Pourquoi mes règles ne reviennent pas ?" to Topic.RETARD,
        "J'ai des saignements" to Topic.SAIGNEMENT,
        "Questions sur l'allaitement" to Topic.ALLAITEMENT,
        "Quelle contraception maintenant ?" to Topic.CONTRACEPTION,
        "Je me sens triste" to Topic.HUMEUR,
    )

    else -> listOf(
        "Pourquoi mes règles ne sont pas arrivées ?" to Topic.RETARD,
        "Est-ce que je dois faire un test ?" to Topic.TEST,
        "Je peux tomber enceinte aujourd'hui ?" to Topic.FERTILITE,
        "J'ai mal pendant mes règles" to Topic.DOULEUR,
        "J'ai des saignements inhabituels" to Topic.SAIGNEMENT,
        "Quelle contraception choisir ?" to Topic.CONTRACEPTION,
        "Je me sens à fleur de peau" to Topic.HUMEUR,
    )
}

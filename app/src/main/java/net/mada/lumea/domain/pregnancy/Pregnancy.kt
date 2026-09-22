package net.mada.lumea.domain.pregnancy

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Le suivi de grossesse, calculé à partir du premier jour des dernières règles.
 *
 * Tout part de la **date des dernières règles (DDR)**, pas de la date de conception :
 * c'est la convention obstétricale, parce que c'est la seule date qu'on connaisse
 * avec certitude. D'où le nom « semaines d'aménorrhée » (SA) — littéralement
 * « semaines sans règles ». La grossesse réelle commence environ deux semaines
 * plus tard : 8 SA = 6 semaines de grossesse.
 *
 * Rien ici ne remplace une consultation. L'échographie du premier trimestre
 * redate la grossesse et fait foi : si elle donne un autre terme, c'est elle qui a
 * raison, pas ce calcul.
 */

/** Durée d'une grossesse à terme, comptée depuis la DDR : 40 SA pile. */
private const val TERM_DAYS = 280L

data class PregnancyProgress(
    val lastPeriodStart: LocalDate,
    /** Semaines d'aménorrhée révolues. */
    val weeks: Int,
    /** Jours en plus des semaines : on écrit « 12 SA + 3 j ». */
    val days: Int,
    /** Date prévue d'accouchement : DDR + 280 jours. */
    val dueDate: LocalDate,
    val trimester: Int,
    val daysUntilDue: Int,
) {
    /** L'écriture obstétricale habituelle : « 12 SA + 3 j ». */
    val label: String get() = if (days == 0) "$weeks SA" else "$weeks SA + $days j"

    /** Semaines de grossesse réelles, deux de moins que les SA. */
    val weeksOfPregnancy: Int get() = (weeks - 2).coerceAtLeast(0)

    /** Au-delà de 42 SA le calcul ne veut plus rien dire : c'est un suivi abandonné. */
    val isPlausible: Boolean get() = weeks in 0..42
}

fun pregnancyProgress(lastPeriodStart: LocalDate, today: LocalDate = LocalDate.now()): PregnancyProgress {
    val elapsed = ChronoUnit.DAYS.between(lastPeriodStart, today).coerceAtLeast(0)
    val dueDate = lastPeriodStart.plusDays(TERM_DAYS)
    val weeks = (elapsed / 7).toInt()
    return PregnancyProgress(
        lastPeriodStart = lastPeriodStart,
        weeks = weeks,
        days = (elapsed % 7).toInt(),
        dueDate = dueDate,
        // Découpage usuel : T1 jusqu'à 14 SA, T2 jusqu'à 28 SA, T3 ensuite.
        trimester = when {
            weeks < 15 -> 1
            weeks < 29 -> 2
            else -> 3
        },
        daysUntilDue = ChronoUnit.DAYS.between(today, dueDate).toInt(),
    )
}

// ------------------------------------------------------------------ Le test

/**
 * Faut-il faire un test aujourd'hui ?
 *
 * Deux questions différentes, qu'on confond tout le temps :
 *
 * - **Un test serait-il fiable ?** Oui, dès le premier jour de retard.
 * - **Est-ce le moment d'en faire un ?** Non, pas au premier jour.
 *
 * Un décalage d'un à quatre jours arrive à presque tout le monde, plusieurs fois
 * par an : proposer un test à chaque fois, c'est fabriquer de l'angoisse et faire
 * dépenser de l'argent pour rien. L'app attend donc [TEST_SUGGESTION_DAYS] jours
 * avant d'en parler, et ne le conseille vraiment qu'à partir de
 * [TEST_RECOMMENDED_DAYS] — une semaine pleine de retard.
 *
 * Une seule exception à cette patience : une contraception d'urgence se prend
 * dans les jours qui suivent le rapport, pas après un retard. Ce n'est donc pas
 * le test qui est urgent, c'est d'en parler — ce que dit le texte le cas échéant.
 */

/** En dessous, l'app ne propose même pas de test : le retard est banal. */
const val TEST_SUGGESTION_DAYS = 5

/** À partir d'une semaine de retard, un test a vraiment du sens. */
const val TEST_RECOMMENDED_DAYS = 7

data class TestReliability(val title: String, val body: String, val reliable: Boolean)

fun testReliability(daysLate: Int): TestReliability = when {
    daysLate < TEST_SUGGESTION_DAYS -> TestReliability(
        "Ce n'est pas encore le moment",
        "Un retard de un à quatre jours arrive à presque tout le monde, et plusieurs " +
            "fois par an : fatigue, stress, sommeil décalé, un peu plus de sport. Un " +
            "test serait techniquement fiable, mais il n'y a pas de raison d'en faire " +
            "un maintenant. Laisse passer quelques jours.",
        reliable = false,
    )
    daysLate < TEST_RECOMMENDED_DAYS -> TestReliability(
        "Tu peux, si tu veux être fixée",
        "À $daysLate jours de retard, un test urinaire est fiable. Rien ne presse " +
            "pour autant : attendre d'arriver à une semaine pleine évite de le faire " +
            "pour rien. Si l'attente te pèse plus que le test, fais-le.",
        reliable = true,
    )
    daysLate < 21 -> TestReliability(
        "Un test a du sens maintenant",
        "Une semaine pleine de retard, c'est le moment. Fais-le avec les premières " +
            "urines du matin : elles sont plus concentrées, le résultat est plus net. " +
            "S'il est négatif et que les règles ne viennent toujours pas, refais-en un " +
            "dans 3 à 5 jours.",
        reliable = true,
    )
    else -> TestReliability(
        "Un test, puis un avis médical",
        "Plus de trois semaines de retard : fais un test, et prends rendez-vous quel " +
            "que soit le résultat. Un retard qui dure avec un test négatif mérite " +
            "d'être expliqué — thyroïde, ovaires polykystiques, autre chose. Ça se " +
            "diagnostique et ça se traite.",
        reliable = true,
    )
}

/** Comment faire le test, en quatre points. Ça paraît évident, ça ne l'est pas. */
val TEST_HOW_TO: List<Pair<String, String>> = listOf(
    "Où s'en procurer" to
        "En pharmacie, en supermarché ou en ligne, sans ordonnance et sans " +
            "justification d'âge. Les tests les moins chers détectent la même hormone " +
            "que les plus chers.",
    "Quand le faire" to
        "Avec les premières urines du matin, plus concentrées. À n'importe quelle " +
            "heure si tu ne peux pas attendre, mais évite de boire beaucoup avant.",
    "Comment le lire" to
        "Respecte le temps d'attente indiqué sur la notice, ni avant ni longtemps " +
            "après. Une deuxième ligne, même très pâle, est un résultat positif.",
    "En cas de doute" to
        "Une prise de sang en laboratoire dose l'hormone précisément et détecte une " +
            "grossesse plus tôt qu'un test urinaire. Elle se fait sans ordonnance, " +
            "et le résultat n'est pas discutable.",
)

/** Ce que veut dire un test négatif alors que le retard persiste. */
val NEGATIVE_CAUSES: List<Pair<String, String>> = listOf(
    "Un test fait trop tôt" to
        "C'est la cause numéro un. Refais-en un dans 3 à 5 jours si les règles ne " +
            "sont toujours pas là.",
    "Le stress et la fatigue" to
        "Examens, deuil, déménagement, gros coup de stress : le cycle est l'une des " +
            "premières choses qui se dérègle.",
    "Un changement de poids ou de sport" to
        "Une perte de poids rapide ou un entraînement intense peut suspendre " +
            "l'ovulation, donc les règles.",
    "Une cause médicale" to
        "Thyroïde, syndrome des ovaires polykystiques, certains médicaments. Rien " +
            "de dramatique, mais ça se diagnostique et ça se traite.",
    "Un changement de contraception" to
        "Démarrer, arrêter ou changer de contraception décale souvent le cycle " +
            "pendant quelques mois.",
)

// --------------------------------------------------------- Suivi trimestriel

data class PregnancyStep(
    val fromWeek: Int,
    val toWeek: Int,
    val title: String,
    val body: String,
)

/**
 * Les grandes étapes du suivi, adaptées à Madagascar.
 *
 * Le détail des actes (CPN, VAT, TPIg, fer) vit dans `MadagascarCare` ; ici on ne
 * garde que le repère affiché en gros : « où j'en suis ».
 */
val PREGNANCY_MILESTONES: List<PregnancyStep> = listOf(
    PregnancyStep(
        0, 12,
        "1ʳᵉ CPN — le plus tôt possible, avant 12 SA",
        "Au CSB, gratuitement. On confirme la grossesse, on ouvre le carnet de santé " +
            "mère-enfant, on fait le groupe sanguin et les dépistages, et on commence " +
            "le fer. Y aller tôt est ce qui prédit le mieux le fait d'aller jusqu'au " +
            "bout du suivi.",
    ),
    PregnancyStep(
        13, 17,
        "Début du traitement préventif du paludisme",
        "À partir de 13 SA, jamais avant : trois comprimés de sulfadoxine-" +
            "pyriméthamine en une prise, avalés devant le soignant, puis une fois par " +
            "mois. Et une moustiquaire imprégnée toutes les nuits.",
    ),
    PregnancyStep(
        18, 25,
        "2ᵉ et 3ᵉ CPN — vers 20 et 26 SA",
        "Tension, poids, hauteur utérine, urines, et les doses suivantes de TPIg. " +
            "C'est aussi le moment du déparasitage et, si elle est disponible, de " +
            "l'échographie du 2ᵉ trimestre.",
    ),
    PregnancyStep(
        26, 32,
        "4ᵉ CPN — vers 30 SA",
        "Le seuil que moins d'une femme sur deux atteint dans certains districts. " +
            "C'est pourtant là que se dépistent l'anémie de fin de grossesse et les " +
            "premiers signes de pré-éclampsie.",
    ),
    PregnancyStep(
        33, 37,
        "5ᵉ et 6ᵉ CPN — préparer l'accouchement",
        "Vers 34 et 36 SA. Trois questions à régler : dans quel CSB tu accouches, " +
            "comment tu y vas la nuit, et qui t'accompagne. La plupart des décès " +
            "maternels arrivent faute d'être partie à temps.",
    ),
    PregnancyStep(
        38, 42,
        "7ᵉ et 8ᵉ CPN — jusqu'au terme",
        "Vers 38 et 40 SA. Un accouchement à terme va de 37 à 42 SA. Accoucher " +
            "auprès d'un soignant qualifié est ce qui change le plus les chances en " +
            "cas d'hémorragie.",
    ),
)

/** L'étape en cours, d'après les semaines d'aménorrhée. */
fun milestoneFor(weeks: Int): PregnancyStep? =
    PREGNANCY_MILESTONES.firstOrNull { weeks in it.fromWeek..it.toWeek }
        ?: PREGNANCY_MILESTONES.lastOrNull()

// ------------------------------------------------------------- Alimentation

data class FoodAdvice(val title: String, val body: String)

/** Ce qu'il vaut mieux avoir dans l'assiette. */
val FOOD_RECOMMENDED: List<FoodAdvice> = listOf(
    FoodAdvice(
        "Fer et acide folique (FAF), tous les jours",
        "Les comprimés FAF sont fournis gratuitement à la CPN : 30 à 60 mg de fer et " +
            "de l'acide folique par jour, toute la grossesse. Demande-les si on ne " +
            "t'en donne pas. L'acide folique protège le cerveau et la colonne " +
            "vertébrale du bébé pendant les tout premiers mois.",
    ),
    FoodAdvice(
        "Fer — la priorité numéro un ici",
        "L'anémie est la complication la plus fréquente à Madagascar, et elle " +
            "augmente le risque d'hémorragie à l'accouchement. Brèdes (anamamy, " +
            "anantsonga), feuilles de manioc, lentilles, haricots, viande, abats, " +
            "petits poissons entiers. Un fruit acide ou du citron au repas aide à " +
            "absorber le fer ; le thé et le café pris en mangeant le bloquent.",
    ),
    FoodAdvice(
        "Ce qui se trouve facilement et qui compte",
        "Les feuilles de moringa (ananambo) sont parmi les aliments les plus riches " +
            "en fer, en calcium et en vitamine A. Ajoute aussi l'arachide, le " +
            "haricot, l'œuf bien cuit, la patate douce à chair orange et les fruits " +
            "de saison.",
    ),
    FoodAdvice(
        "Calcium et iode",
        "Petits poissons mangés avec les arêtes, brèdes, lait bouilli. Pour l'iode, " +
            "qui participe au développement du cerveau du bébé : utilise du sel iodé " +
            "et mange du poisson.",
    ),
    FoodAdvice(
        "Fibres et eau",
        "La constipation est l'un des désagréments les plus fréquents. Fruits, " +
            "légumes, céréales complètes, et 1,5 à 2 litres d'eau par jour.",
    ),
)

/** Ce qu'il faut écarter, et pourquoi — le « pourquoi » est ce qui fait tenir. */
val FOOD_AVOID: List<FoodAdvice> = listOf(
    FoodAdvice(
        "Alcool : zéro, vraiment zéro",
        "Il n'existe aucune dose sans risque, à aucun moment de la grossesse. " +
            "L'alcool passe directement au bébé et touche son cerveau. C'est la " +
            "première cause évitable de handicap mental à la naissance.",
    ),
    FoodAdvice(
        "Tabac, cannabis, vape",
        "Ils réduisent l'oxygène qui arrive au bébé : prématurité, petit poids. " +
            "Arrêter, même tard, améliore les choses immédiatement. Demande de l'aide " +
            "plutôt que de culpabiliser : Tabac info service, 39 89.",
    ),
    FoodAdvice(
        "Viande, poisson et lait crus ou mal cuits",
        "Tout ce qui est bien cuit ne pose pas de problème. Évite la viande saignante, " +
            "le poisson cru, le lait non bouilli et les produits laitiers non " +
            "pasteurisés : ils exposent à des infections plus graves pendant la " +
            "grossesse.",
    ),
    FoodAdvice(
        "Eau non traitée",
        "Bois de l'eau bouillie, filtrée ou traitée. Une diarrhée sévère pendant la " +
            "grossesse déshydrate vite et fragilise. Lave-toi les mains avant de " +
            "cuisiner et avant de manger.",
    ),
    FoodAdvice(
        "Toxoplasmose, si tu n'es pas immunisée",
        "Ta première prise de sang le dira. Si tu ne l'es pas : viande bien cuite, " +
            "fruits et légumes soigneusement lavés, gants pour jardiner, et confie la " +
            "litière du chat à quelqu'un d'autre.",
    ),
    FoodAdvice(
        "Gros poissons prédateurs",
        "Espadon, requin, marlin : à éviter, ils concentrent le mercure. Les petits " +
            "poissons — sardines, poissons de rivière — restent recommandés deux fois " +
            "par semaine.",
    ),
    FoodAdvice(
        "Foie, œufs crus, caféine en excès",
        "Le foie concentre trop de vitamine A. Les œufs crus (mayonnaise maison, " +
            "mousse au chocolat) exposent à la salmonelle. Pour la caféine, reste " +
            "sous 200 mg par jour — environ deux cafés.",
    ),
    FoodAdvice(
        "Médicaments et compléments sans avis",
        "L'ibuprofène et tous les anti-inflammatoires sont formellement contre-" +
            "indiqués à partir de 24 SA, et déconseillés avant. Le paracétamol reste " +
            "l'option de première intention, à la dose la plus faible. Demande à ton " +
            "pharmacien avant toute automédication, plantes comprises.",
    ),
)

// ------------------------------------------------------------------ Urgences

data class WarningSign(val sign: String, val what: String)

/**
 * Les signes qui font consulter sans attendre le prochain rendez-vous.
 *
 * Écrits sans dramatiser : la plupart du temps ce n'est rien. Mais ce sont
 * exactement les situations où on hésite à déranger, et où il ne faut pas hésiter.
 */
val WARNING_SIGNS: List<WarningSign> = listOf(
    WarningSign(
        "Saignements, surtout avec des douleurs",
        "Beaucoup de grossesses saignent un peu sans conséquence. Mais des saignements " +
            "avec une douleur d'un seul côté du bas-ventre, en début de grossesse, " +
            "peuvent signaler une grossesse extra-utérine : c'est une urgence.",
    ),
    WarningSign(
        "Douleur abdominale forte ou continue",
        "Différente des tiraillements habituels : intense, qui ne passe pas, ou qui " +
            "réveille la nuit.",
    ),
    WarningSign(
        "Fièvre, surtout avec des frissons",
        "En zone de paludisme, une fièvre chez une femme enceinte se fait tester et " +
            "traiter le jour même : le paludisme est bien plus grave pendant la " +
            "grossesse.",
    ),
    WarningSign(
        "Vomissements qui empêchent de boire",
        "Les nausées du début sont normales. Ne plus rien garder pendant 24 h ne " +
            "l'est pas : ça déshydrate et ça se traite.",
    ),
    WarningSign(
        "Maux de tête intenses, vision trouble, gonflement du visage",
        "Après 20 SA, ce trio peut signaler une pré-éclampsie, liée à la tension. " +
            "Consultation le jour même.",
    ),
    WarningSign(
        "Bébé qui bouge moins",
        "Au troisième trimestre, une baisse nette des mouvements sur une journée se " +
            "vérifie à la maternité. On préfère mille fois un dérangement pour rien.",
    ),
    WarningSign(
        "Perte de liquide, contractions régulières avant 37 SA",
        "Liquide clair qui coule sans pouvoir se retenir, ou contractions qui " +
            "reviennent toutes les dix minutes : direction la maternité.",
    ),
)

/**
 * Toutes les options, telles qu'elles existent à Madagascar.
 *
 * Le contenu vit dans `MadagascarCare.OPTIONS_MADAGASCAR` : recopier ici des
 * informations françaises (droit à l'IVG, numéro vert, délais légaux) exposerait
 * quelqu'un à des poursuites ou à une pratique dangereuse, puisque la loi malgache
 * dit le contraire.
 */
val OPTIONS_AFTER_POSITIVE: List<Pair<String, String>> get() = OPTIONS_MADAGASCAR

/** L'avertissement affiché partout dans l'écran de grossesse. */
const val PREGNANCY_DISCLAIMER =
    "Ces informations suivent les recommandations de l'OMS reprises par Madagascar " +
        "(8 contacts prénatals, TPIg dès 13 SA, VAT). Elles sont éducatives et ne " +
        "remplacent pas la consultation au CSB. Le calcul des semaines part de tes " +
        "dernières règles ; si une échographie donne un autre terme, c'est elle qui " +
        "fait foi."

package net.mada.lumea.domain.pregnancy

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Le suivi après la naissance, jusqu'au retour des règles.
 *
 * C'est la période la plus mal couverte par les applications de cycle : le suivi
 * de grossesse s'arrête à l'accouchement, et le suivi de cycle ne reprend qu'au
 * retour de couches. Entre les deux, plusieurs mois sans rien — alors que c'est là
 * que se concentrent une grande partie des décès maternels et néonatals, et que se
 * joue une question très concrète : **est-ce qu'on peut retomber enceinte ?**
 *
 * La réponse tient dans la MAMA, la méthode de l'allaitement maternel et de
 * l'aménorrhée. Correctement appliquée, c'est une contraception efficace à environ
 * 98 % — mais seulement si **les trois conditions sont réunies en même temps**.
 * Dès qu'une seule tombe, l'efficacité s'effondre, et c'est très exactement là que
 * surviennent les grossesses rapprochées.
 */

/** Une grossesse rapprochée expose la mère et l'enfant : l'OMS conseille d'attendre. */
const val RECOMMENDED_SPACING_MONTHS = 24

data class PostpartumProgress(
    val birthDate: LocalDate,
    val daysSince: Int,
    val weeksSince: Int,
    val monthsSince: Int,
) {
    /** Les six premières semaines : la période à risque pour la mère. */
    val inImmediatePostpartum: Boolean get() = daysSince <= 42

    /** La MAMA ne vaut que pendant les six premiers mois. */
    val withinSixMonths: Boolean get() = daysSince < 182
}

fun postpartumProgress(birthDate: LocalDate, today: LocalDate = LocalDate.now()): PostpartumProgress {
    val days = ChronoUnit.DAYS.between(birthDate, today).coerceAtLeast(0).toInt()
    return PostpartumProgress(
        birthDate = birthDate,
        daysSince = days,
        weeksSince = days / 7,
        monthsSince = ChronoUnit.MONTHS.between(birthDate, today).toInt().coerceAtLeast(0),
    )
}

/**
 * Les trois conditions de la MAMA.
 *
 * Elles sont présentées comme des cases à cocher parce qu'elles se vérifient
 * vraiment, une par une, et qu'il faut les trois. Une réponse « non » quelque part
 * et la méthode ne protège plus.
 */
enum class MamaCondition(val code: String, val question: String, val why: String) {
    UNDER_SIX_MONTHS(
        "MAMA_AGE",
        "Mon bébé a moins de 6 mois",
        "Au-delà de 6 mois, la fertilité revient même si les règles ne sont pas " +
            "réapparues et même si l'allaitement continue. C'est la condition qui " +
            "tombe la première, et souvent sans qu'on s'en rende compte.",
    ),
    EXCLUSIVE_BREASTFEEDING(
        "MAMA_ALLAITEMENT",
        "J'allaite exclusivement, jour et nuit",
        "Le sein seul, à la demande : ni eau, ni tisane, ni bouillie, ni biberon. " +
            "Sans jamais dépasser 4 heures entre deux tétées le jour, ni 6 heures la " +
            "nuit. C'est la succion fréquente qui bloque l'ovulation — espacer les " +
            "tétées suffit à la relancer.",
    ),
    NO_PERIODS(
        "MAMA_REGLES",
        "Mes règles ne sont pas revenues",
        "Aucun saignement après le 56ᵉ jour suivant l'accouchement. Les pertes des " +
            "premières semaines (les lochies) ne comptent pas comme des règles.",
    ),
}

/** Le verdict, une fois les trois conditions passées en revue. */
data class MamaStatus(
    val conditionsMet: Int,
    val protected: Boolean,
    val title: String,
    val body: String,
)

fun mamaStatus(met: Set<String>): MamaStatus {
    val all = MamaCondition.entries
    val count = all.count { it.code in met }
    val protected = count == all.size

    return if (protected) {
        MamaStatus(
            conditionsMet = count,
            protected = true,
            title = "MAMA : tu es protégée, pour l'instant",
            body = "Les trois conditions sont réunies : l'allaitement te protège à " +
                "environ 98 %, autant qu'une pilule bien prise. Mais c'est temporaire. " +
                "Reviens vérifier ici chaque mois : dès qu'une seule condition tombe — " +
                "les 6 mois du bébé, une nuit où il fait son premier long sommeil, " +
                "le moindre saignement — la protection s'arrête le jour même.",
        )
    } else {
        val missing = all.filter { it.code !in met }
        MamaStatus(
            conditionsMet = count,
            protected = false,
            title = "MAMA : tu n'es plus protégée",
            body = buildString {
                append("Il manque : ")
                append(missing.joinToString(", ") { it.question.lowercase() })
                append(". Dès qu'une seule des trois conditions n'est plus remplie, ")
                append("l'allaitement ne protège plus d'une grossesse. ")
                append("L'ovulation revient avant les règles : on peut retomber ")
                append("enceinte sans avoir eu un seul cycle entre les deux. ")
                append("Parles-en à ta sage-femme lors de la consultation postnatale : ")
                append("plusieurs méthodes sont compatibles avec l'allaitement.")
            },
        )
    }
}

/**
 * Les consultations postnatales recommandées par l'OMS, suivies à Madagascar.
 *
 * La première est la plus importante et la plus souvent manquée : la majorité des
 * décès maternels et néonatals surviennent dans les 24 heures qui suivent la
 * naissance.
 */
val POSTNATAL_VISITS: List<CareAct> = listOf(
    CareAct(
        "CPON1", "1ʳᵉ visite — dans les 24 heures",
        "La plus importante de toutes. On surveille les saignements de la mère, on " +
            "pèse le bébé, on vérifie la première tétée, et on fait les vaccins de " +
            "naissance (BCG et polio 0).",
        0, 0, CareCategory.ACCOUCHEMENT,
    ),
    CareAct(
        "CPON2", "2ᵉ visite — vers le 3ᵉ jour",
        "Surveillance de l'infection, de l'ictère du bébé (la peau qui jaunit) et de " +
            "la mise en route de l'allaitement. C'est le moment où les difficultés " +
            "de tétée se règlent le plus facilement.",
        0, 1, CareCategory.ACCOUCHEMENT,
    ),
    CareAct(
        "CPON3", "3ᵉ visite — entre le 7ᵉ et le 14ᵉ jour",
        "Contrôle du poids du bébé, du cordon, et de l'état de la mère. On y parle " +
            "aussi fatigue et moral : le baby blues est fréquent et se dit.",
        1, 2, CareCategory.ACCOUCHEMENT,
    ),
    CareAct(
        "CPON4", "4ᵉ visite — à 6 semaines",
        "Bilan complet de la mère, reprise de la contraception si tu le souhaites, " +
            "et début des vaccins du bébé (Penta 1). C'est la consultation qui clôt " +
            "officiellement les suites de couches.",
        6, 8, CareCategory.ACCOUCHEMENT,
    ),
)

/**
 * Les vaccins du nourrisson dans le calendrier malgache (PEV).
 *
 * Les rattrapages sont possibles : un retard n'annule jamais la protection, il la
 * décale. Le dire évite que quelqu'un abandonne le calendrier en se croyant
 * disqualifié.
 */
val INFANT_VACCINES: List<CareAct> = listOf(
    CareAct(
        "BCG", "BCG et polio 0 — à la naissance",
        "Contre la tuberculose et la poliomyélite, dès les premiers jours. Le BCG " +
            "laisse une petite cicatrice sur le bras : c'est normal.",
        0, 0, CareCategory.VACCIN,
    ),
    CareAct(
        "PENTA1", "Penta 1, polio et pneumo — à 6 semaines",
        "Le vaccin combiné protège contre cinq maladies à la fois. Il se donne en " +
            "même temps que la 4ᵉ consultation postnatale.",
        6, 8, CareCategory.VACCIN,
    ),
    CareAct(
        "PENTA2", "Penta 2 — à 10 semaines",
        "Deuxième dose, quatre semaines après la première.",
        10, 12, CareCategory.VACCIN,
    ),
    CareAct(
        "PENTA3", "Penta 3 — à 14 semaines",
        "Troisième dose. C'est elle qui donne la protection durable : ne t'arrête " +
            "pas avant.",
        14, 17, CareCategory.VACCIN,
    ),
    CareAct(
        "ROUGEOLE", "Rougeole — à 9 mois",
        "La rougeole reste une cause importante de décès chez le jeune enfant à " +
            "Madagascar, et les épidémies reviennent régulièrement.",
        39, 52, CareCategory.VACCIN,
    ),
)

/** Ce qui compte pour la mère elle-même pendant ces semaines-là. */
val POSTPARTUM_ADVICE: List<Pair<String, String>> = listOf(
    "Continue le fer" to
        "Encore au moins trois mois après l'accouchement. L'accouchement fait perdre " +
            "du sang, et l'anémie post-partum entretient la fatigue qu'on met souvent " +
            "sur le compte du bébé.",
    "Allaitement exclusif jusqu'à 6 mois" to
        "Rien d'autre que le sein, pas même de l'eau — le lait maternel en contient " +
            "assez, même quand il fait chaud. Ensuite, on continue d'allaiter en " +
            "ajoutant d'autres aliments, idéalement jusqu'à 2 ans.",
    "Mange et bois plus que d'habitude" to
        "Allaiter demande environ un repas de plus par jour. Ce n'est pas le moment " +
            "de se restreindre : le lait se fabrique avec ce que tu manges et ce que " +
            "tu bois.",
    "Dors sous moustiquaire, avec le bébé" to
        "Le paludisme reste dangereux pour toi et l'est encore plus pour un " +
            "nourrisson. La moustiquaire imprégnée sert autant après qu'avant.",
    "Ce qui n'est pas normal" to
        "Saignement abondant, fièvre, pertes malodorantes, douleur ou rougeur d'un " +
            "sein avec fièvre, tristesse profonde qui dure. Aucun de ces signes n'est " +
            "« la fatigue d'une jeune mère » : ils se soignent.",
    "Espacer les grossesses" to
        "L'OMS conseille d'attendre environ $RECOMMENDED_SPACING_MONTHS mois avant " +
            "une nouvelle grossesse. Des grossesses rapprochées augmentent le risque " +
            "de prématurité, de petit poids et d'anémie pour la mère.",
)

/**
 * Les méthodes compatibles avec l'allaitement, pour la conversation avec la
 * sage-femme.
 *
 * L'app ne prescrit rien : elle donne le vocabulaire pour pouvoir demander.
 */
val POSTPARTUM_CONTRACEPTION: List<Pair<String, String>> = listOf(
    "Le préservatif" to
        "Disponible tout de suite, sans consultation, et le seul à protéger aussi " +
            "des IST.",
    "La pilule sans œstrogène (microprogestative)" to
        "Compatible avec l'allaitement et utilisable dès 6 semaines, voire plus tôt " +
            "selon l'avis du soignant. Elle demande une prise très régulière.",
    "L'implant et l'injection" to
        "Compatibles avec l'allaitement. L'implant protège plusieurs années, " +
            "l'injection quelques mois. Tous deux sont disponibles dans les CSB.",
    "Le stérilet (DIU)" to
        "Posable juste après l'accouchement ou à partir de 4 à 6 semaines. Il " +
            "protège plusieurs années et n'a aucun effet sur le lait.",
    "À demander en consultation postnatale" to
        "La 4ᵉ visite, à 6 semaines, est prévue pour ça. Tu peux aussi en parler " +
            "avant : rien n'oblige à attendre.",
)

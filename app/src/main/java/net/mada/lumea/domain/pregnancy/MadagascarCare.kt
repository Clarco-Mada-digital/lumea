package net.mada.lumea.domain.pregnancy

import java.time.LocalDate

/**
 * Le suivi prénatal tel qu'il se pratique à Madagascar.
 *
 * Madagascar suit le modèle de l'OMS 2016 : **huit contacts prénatals** au lieu
 * des quatre de l'ancien schéma, plus un contact ajouté entre 13 et 16 SA pour
 * démarrer la prévention du paludisme. Trois choses n'existent pas dans les
 * recommandations européennes et sont ici essentielles :
 *
 * - le **TPIg** : sulfadoxine-pyriméthamine à partir de 13 SA, une prise par mois,
 *   trois doses au minimum, avalée devant le soignant ;
 * - le **VAT** : la vaccination antitétanique, qui protège la mère et le nouveau-né
 *   du tétanos néonatal ;
 * - la **MILD** : dormir sous moustiquaire imprégnée toute la grossesse.
 *
 * Les consultations prénatales sont gratuites dans les CSB. La couverture réelle
 * reste faible — moins d'une femme sur deux atteint la quatrième CPN dans certains
 * districts — et c'est précisément le trou que ce suivi cherche à combler : ce
 * n'est pas l'information qui manque, c'est le rappel au bon moment.
 *
 * Sources : lignes directrices OMS 2016 sur les soins prénatals, directives
 * nationales malgaches de prise en charge du paludisme, guide OMS du TPIg
 * communautaire (2022). À faire valider par un soignant avant diffusion.
 */

/** Un acte de suivi à faire à une période donnée, et à cocher une fois fait. */
data class CareAct(
    /** Identifiant stable, stocké en base : ne jamais le renommer. */
    val code: String,
    val title: String,
    val why: String,
    /** Semaine d'aménorrhée à partir de laquelle l'acte est attendu. */
    val fromWeek: Int,
    /** Dernière semaine raisonnable pour le faire. */
    val toWeek: Int,
    val category: CareCategory,
)

enum class CareCategory { CPN, VACCIN, PALUDISME, SUPPLEMENT, ACCOUCHEMENT }

val CareCategory.label: String
    get() = when (this) {
        CareCategory.CPN -> "Consultations prénatales"
        CareCategory.VACCIN -> "Vaccination"
        CareCategory.PALUDISME -> "Paludisme"
        CareCategory.SUPPLEMENT -> "Suppléments et déparasitage"
        CareCategory.ACCOUCHEMENT -> "Accouchement"
    }

val CareCategory.emoji: String
    get() = when (this) {
        CareCategory.CPN -> "🩺"
        CareCategory.VACCIN -> "💉"
        CareCategory.PALUDISME -> "🦟"
        CareCategory.SUPPLEMENT -> "💊"
        CareCategory.ACCOUCHEMENT -> "🏥"
    }

/**
 * Les huit contacts prénatals du modèle OMS, retenus par Madagascar.
 *
 * Les semaines sont celles de la recommandation : 12, 20, 26, 30, 34, 36, 38, 40.
 * Le premier compte double : une CPN précoce est le meilleur prédicteur du fait
 * d'aller jusqu'au bout des huit.
 */
val ANC_VISITS: List<CareAct> = listOf(
    CareAct(
        "CPN1", "1ʳᵉ CPN — avant 12 SA",
        "La plus importante de toutes. On y confirme la grossesse, on pèse, on prend " +
            "la tension, on fait le groupe sanguin, le dépistage du VIH et de la " +
            "syphilis, et on ouvre le carnet de santé mère-enfant. Y aller tôt est ce " +
            "qui prédit le mieux le fait de faire toutes les consultations ensuite.",
        0, 12, CareCategory.CPN,
    ),
    CareAct(
        "CPN_TPI", "Contact de 13 à 16 SA — début du TPIg",
        "Un contact ajouté par l'OMS et suivi à Madagascar, précisément pour démarrer " +
            "la prévention du paludisme dès le début du 2ᵉ trimestre.",
        13, 16, CareCategory.CPN,
    ),
    CareAct(
        "CPN2", "2ᵉ CPN — vers 20 SA",
        "Contrôle de la tension, du poids, de la hauteur utérine et des urines " +
            "(albumine, sucre). C'est aussi le moment de la 2ᵉ dose de TPIg.",
        18, 23, CareCategory.CPN,
    ),
    CareAct(
        "CPN3", "3ᵉ CPN — vers 26 SA",
        "Suivi de la croissance et dépistage de l'anémie, fréquente à ce stade. " +
            "3ᵉ dose de TPIg si ce n'est pas déjà fait.",
        24, 28, CareCategory.CPN,
    ),
    CareAct(
        "CPN4", "4ᵉ CPN — vers 30 SA",
        "On vérifie la position du bébé, la tension et les signes de pré-éclampsie. " +
            "C'est le seuil que moins d'une femme sur deux atteint dans certains " +
            "districts : ne t'arrête pas là.",
        29, 32, CareCategory.CPN,
    ),
    CareAct(
        "CPN5", "5ᵉ CPN — vers 34 SA",
        "Suivi rapproché. On commence à parler du lieu d'accouchement et du plan " +
            "d'urgence : comment tu iras au CSB, avec qui, et avec quel argent.",
        33, 35, CareCategory.CPN,
    ),
    CareAct(
        "CPN6", "6ᵉ CPN — vers 36 SA",
        "Position définitive du bébé, dernier dépistage de l'anémie, préparation de " +
            "la trousse d'accouchement.",
        36, 37, CareCategory.CPN,
    ),
    CareAct(
        "CPN7", "7ᵉ CPN — vers 38 SA",
        "Surveillance de fin de grossesse. Les mouvements du bébé et la tension sont " +
            "les deux choses à signaler sans attendre.",
        38, 39, CareCategory.CPN,
    ),
    CareAct(
        "CPN8", "8ᵉ CPN — vers 40 SA",
        "Dernier contact avant le terme. Un accouchement à terme va de 37 à 42 SA : " +
            "dépasser la date prévue de quelques jours est normal, mais le suivi " +
            "continue.",
        40, 42, CareCategory.CPN,
    ),
)

/**
 * Le vaccin antitétanique.
 *
 * Le schéma complet compte cinq doses réparties sur plusieurs années — il protège
 * alors à vie. Pendant une grossesse, l'essentiel est d'atteindre **VAT2** au moins
 * quatre semaines après VAT1, et au moins 90 jours avant l'accouchement : c'est ce
 * qui protège le nouveau-né du tétanos néonatal. Si tu as déjà reçu des doses lors
 * d'une grossesse précédente, montre ton carnet : on reprend là où tu en étais, on
 * ne recommence pas à zéro.
 */
val VACCINE_ACTS: List<CareAct> = listOf(
    CareAct(
        "VAT1", "VAT 1 — à la 1ʳᵉ CPN",
        "Première dose d'anatoxine tétanique, sauf si ton carnet montre que tu es " +
            "déjà à jour. Elle prépare la protection, elle ne suffit pas seule.",
        0, 14, CareCategory.VACCIN,
    ),
    CareAct(
        "VAT2", "VAT 2 — au moins 4 semaines après VAT 1",
        "C'est la dose qui compte vraiment : elle protège le bébé du tétanos à la " +
            "naissance. À faire au moins 90 jours (environ 3 mois) avant " +
            "l'accouchement pour que la protection passe au nouveau-né.",
        8, 28, CareCategory.VACCIN,
    ),
    CareAct(
        "VAT3", "VAT 3 — 6 mois après VAT 2",
        "Souvent après l'accouchement. Elle porte la protection à environ 5 ans. " +
            "Les doses VAT 4 et VAT 5, un an d'intervalle chacune, complètent le " +
            "schéma et protègent alors pour toute la période de procréation.",
        26, 42, CareCategory.VACCIN,
    ),
)

/**
 * La prévention du paludisme, spécifique aux zones endémiques comme Madagascar.
 *
 * Le paludisme pendant la grossesse provoque anémie sévère, fausse couche,
 * prématurité et petit poids de naissance. La sulfadoxine-pyriméthamine reste
 * bénéfique même là où le parasite y résiste en partie.
 */
val MALARIA_ACTS: List<CareAct> = listOf(
    CareAct(
        "MILD", "Dormir sous moustiquaire imprégnée",
        "Toutes les nuits, toute la grossesse, et ensuite avec le bébé. C'est la " +
            "protection la plus simple et la plus efficace. Les MILD sont distribuées " +
            "gratuitement à la CPN et lors des campagnes.",
        0, 42, CareCategory.PALUDISME,
    ),
    CareAct(
        "TPI1", "TPIg — 1ʳᵉ dose, dès 13 SA",
        "Trois comprimés de sulfadoxine-pyriméthamine en une seule prise, avalés " +
            "devant le soignant. Jamais avant 13 SA. C'est gratuit à la CPN.",
        13, 17, CareCategory.PALUDISME,
    ),
    CareAct(
        "TPI2", "TPIg — 2ᵉ dose, un mois après",
        "Une prise par mois, à chaque CPN. Deux doses valent mieux qu'une, trois " +
            "valent mieux que deux.",
        17, 24, CareCategory.PALUDISME,
    ),
    CareAct(
        "TPI3", "TPIg — 3ᵉ dose",
        "Le minimum recommandé est de trois doses. Il n'y a pas de maximum tant que " +
            "l'espacement d'un mois est respecté jusqu'à l'accouchement.",
        24, 32, CareCategory.PALUDISME,
    ),
    CareAct(
        "TPI4", "TPIg — doses suivantes",
        "Continue une prise par mois jusqu'à l'accouchement. Chaque dose " +
            "supplémentaire réduit encore le risque d'anémie et de petit poids.",
        32, 42, CareCategory.PALUDISME,
    ),
)

/** Fer, acide folique, déparasitage : le socle nutritionnel de la CPN. */
val SUPPLEMENT_ACTS: List<CareAct> = listOf(
    CareAct(
        "FAF", "Fer et acide folique, tous les jours",
        "30 à 60 mg de fer et de l'acide folique chaque jour, toute la grossesse. " +
            "L'anémie est la complication la plus fréquente ici, et elle augmente le " +
            "risque d'hémorragie à l'accouchement. Les comprimés sont fournis à la " +
            "CPN : demande-les si on ne t'en donne pas.",
        0, 42, CareCategory.SUPPLEMENT,
    ),
    CareAct(
        "DEPARASITAGE", "Déparasitage, à partir du 4ᵉ mois",
        "Un comprimé antiparasitaire après le 1ᵉʳ trimestre. Les vers intestinaux " +
            "entretiennent l'anémie : traiter les parasites fait remonter le fer.",
        16, 30, CareCategory.SUPPLEMENT,
    ),
)

/** La fin du parcours : où accoucher, et ce qu'il faut avoir prévu avant. */
val BIRTH_ACTS: List<CareAct> = listOf(
    CareAct(
        "PLAN_URGENCE", "Préparer le plan d'accouchement",
        "Trois questions à régler avant 36 SA : dans quel CSB ou maternité tu " +
            "accouches, comment tu y vas la nuit, et qui t'accompagne. La plupart des " +
            "décès maternels arrivent faute d'avoir pu partir à temps.",
        30, 38, CareCategory.ACCOUCHEMENT,
    ),
    CareAct(
        "ACCOUCHEMENT_ASSISTE", "Accoucher avec un soignant qualifié",
        "Sage-femme, infirmier ou médecin, au CSB-II ou à la maternité. C'est ce qui " +
            "change le plus les chances en cas d'hémorragie — la première cause de " +
            "décès maternel, et celle qui va le plus vite.",
        37, 42, CareCategory.ACCOUCHEMENT,
    ),
    CareAct(
        "CPON", "Consultations après la naissance",
        "Dans les 24 heures, puis vers le 3ᵉ jour, vers le 7ᵉ jour et à 6 semaines. " +
            "On y surveille les saignements, l'infection, l'allaitement, et on y " +
            "parle de contraception si tu le souhaites.",
        40, 42, CareCategory.ACCOUCHEMENT,
    ),
)

/** Tout le carnet, dans l'ordre où les choses arrivent. */
val ALL_CARE_ACTS: List<CareAct> =
    (ANC_VISITS + VACCINE_ACTS + MALARIA_ACTS + SUPPLEMENT_ACTS + BIRTH_ACTS)
        .sortedBy { it.fromWeek }

fun careAct(code: String): CareAct? = ALL_CARE_ACTS.firstOrNull { it.code == code }

/** L'état d'un acte pour une grossesse donnée. */
enum class CareStatus { FAIT, A_FAIRE, BIENTOT, EN_RETARD, PLUS_TARD }

fun careStatus(act: CareAct, currentWeek: Int, done: Boolean): CareStatus = when {
    done -> CareStatus.FAIT
    currentWeek > act.toWeek -> CareStatus.EN_RETARD
    currentWeek in act.fromWeek..act.toWeek -> CareStatus.A_FAIRE
    currentWeek >= act.fromWeek - 2 -> CareStatus.BIENTOT
    else -> CareStatus.PLUS_TARD
}

/** La date approximative d'un acte, pour le poser dans l'agenda. */
fun careDate(act: CareAct, lastPeriodStart: LocalDate): LocalDate =
    lastPeriodStart.plusWeeks(act.fromWeek.toLong().coerceAtLeast(1))

/**
 * Les actes répartis sur le calendrier, à partir d'une date d'ancrage.
 *
 * L'ancrage est le premier jour des dernières règles pendant la grossesse, et la
 * date de naissance après. Les semaines de chaque acte sont comptées depuis cette
 * date : c'est ce qui permet de voir « ma 3ᵉ CPN, c'est ce jeudi » dans la grille
 * plutôt que d'avoir à convertir des semaines d'aménorrhée de tête.
 */
fun careSchedule(anchor: LocalDate, acts: List<CareAct>): Map<LocalDate, List<CareAct>> =
    acts
        .filterNot { it.isContinuous }
        .groupBy { anchor.plusWeeks(it.fromWeek.toLong()) }

/**
 * Un acte qui court sur toute la période plutôt que d'avoir un jour à lui : le fer
 * quotidien, la moustiquaire toutes les nuits.
 *
 * Le seuil est volontairement haut. Une première version excluait tout ce qui
 * s'étalait sur vingt semaines ou plus, ce qui faisait disparaître du calendrier
 * la dose **VAT 2** (8 à 28 SA) — précisément celle qui protège le nouveau-né du
 * tétanos. Une fenêtre large n'est pas la même chose qu'un acte continu.
 */
internal val CareAct.isContinuous: Boolean
    get() = toWeek - fromWeek >= 30

/**
 * La prochaine chose à faire, en tenant compte de ce qui est déjà coché.
 *
 * Sans ça, l'app répète « fais ta première CPN » pendant des semaines après
 * qu'elle a été faite : elle affiche l'étape de la semaine en cours au lieu de
 * suivre le carnet. Ici on regarde ce qui reste, dans l'ordre d'urgence — un
 * rendez-vous manqué passe devant un rendez-vous à venir.
 */
data class NextCare(
    val act: CareAct,
    val date: LocalDate,
    val status: CareStatus,
    /** Combien d'actes sont en retard en tout, celui-ci compris. */
    val lateCount: Int,
)

fun nextCareAction(
    anchor: LocalDate,
    acts: List<CareAct>,
    doneCodes: Set<String>,
    currentWeek: Int,
): NextCare? {
    val pending = acts
        .filterNot { it.code in doneCodes }
        .map { it to careStatus(it, currentWeek, done = false) }

    val lateCount = pending.count { it.second == CareStatus.EN_RETARD }

    // L'ordre de priorité : ce qui est en retard, puis ce qui est à faire
    // maintenant, puis ce qui arrive. Un acte continu (fer, moustiquaire) ne
    // remonte jamais ici : il n'a pas de date à rappeler.
    val ordered = listOf(
        CareStatus.EN_RETARD,
        CareStatus.A_FAIRE,
        CareStatus.BIENTOT,
        CareStatus.PLUS_TARD,
    )
    val chosen = ordered.firstNotNullOfOrNull { wanted ->
        pending
            .filter { it.second == wanted && !it.first.isContinuous }
            .minByOrNull { it.first.fromWeek }
    } ?: return null

    return NextCare(
        act = chosen.first,
        date = anchor.plusWeeks(chosen.first.fromWeek.toLong()),
        status = chosen.second,
        lateCount = lateCount,
    )
}

/** La phrase affichée sur l'accueil et en tête du suivi. */
val NextCare.headline: String
    get() = when (status) {
        CareStatus.EN_RETARD ->
            if (lateCount > 1) "$lateCount rendez-vous en retard · ${act.title}"
            else "En retard : ${act.title}"
        CareStatus.A_FAIRE -> "À faire maintenant : ${act.title}"
        CareStatus.BIENTOT -> "Bientôt : ${act.title}"
        else -> "Prochaine étape : ${act.title}"
    }

/**
 * Où aller, et quand.
 *
 * Il n'existe pas de numéro d'urgence santé national unique à Madagascar : le
 * recours est géographique. Le dire franchement vaut mieux que d'afficher un
 * numéro qui ne répondra pas.
 */
val WHERE_TO_GO: List<Pair<String, String>> = listOf(
    "CSB-I" to
        "Vaccinations et soins de base, tenus par un personnel paramédical — " +
            "infirmier, sage-femme ou aide-soignant.",
    "CSB-II" to
        "Le niveau de référence pour la grossesse : CPN, accouchement assisté, " +
            "soins obstétricaux essentiels, dirigé par un médecin. C'est là que se " +
            "fait l'essentiel du suivi, et les CPN y sont gratuites.",
    "CHRD / CHD" to
        "L'hôpital de district, vers lequel le CSB t'oriente si la grossesse " +
            "présente un risque ou si une césarienne est nécessaire.",
    "En urgence, la nuit" to
        "Va directement au CSB-II ou à la maternité la plus proche : ce sont eux " +
            "qui organisent l'évacuation vers l'hôpital. N'attends pas le matin " +
            "pour un saignement abondant, une fièvre ou des douleurs fortes.",
)

/**
 * Ce que dit la loi malgache, sans jugement et sans approximation.
 *
 * L'avortement est interdit à Madagascar, y compris pour raison médicale : les
 * articles sur l'interruption thérapeutique ont été retirés de la loi 2017-043
 * lors de son passage au Sénat, et l'article 317 du Code pénal reste applicable
 * à la femme comme à toute personne qui l'aiderait.
 *
 * L'app n'a pas à commenter cette loi, mais elle ne peut pas non plus recopier
 * des informations françaises qui exposeraient quelqu'un à des poursuites ou à
 * une pratique dangereuse. Ce qu'elle doit dire en revanche, c'est que **les soins
 * après avortement sont des soins d'urgence comme les autres** : les complications
 * d'avortement sont la deuxième cause de décès maternel dans les formations
 * sanitaires malgaches, et l'immense majorité de ces décès vient d'un retard à
 * consulter par peur.
 */
val OPTIONS_MADAGASCAR: List<Pair<String, String>> = listOf(
    "Poursuivre la grossesse" to
        "Le suivi commence par une CPN, le plus tôt possible et avant 12 SA. Les " +
            "consultations prénatales sont gratuites dans les CSB, tout comme les " +
            "vaccins et le traitement préventif du paludisme.",
    "Ce que dit la loi malgache" to
        "L'interruption volontaire de grossesse est interdite à Madagascar, y " +
            "compris pour raison médicale : l'article 317 du Code pénal s'applique à " +
            "la femme comme à toute personne qui l'aiderait. C'est un fait juridique, " +
            "pas un jugement sur ta situation.",
    "Saignements, fièvre, douleurs : va au CSB" to
        "Quelle que soit l'origine du problème, les soins après avortement sont des " +
            "soins d'urgence et tu y as droit. Les complications d'avortement sont la " +
            "deuxième cause de décès maternel dans les formations sanitaires " +
            "malgaches, et c'est presque toujours le retard à consulter qui tue, pas " +
            "la complication elle-même. N'attends pas.",
    "En parler à quelqu'un" to
        "Une sage-femme au CSB, un centre de planification familiale, ou une " +
            "association de santé de la reproduction. Tu peux demander conseil sans " +
            "avoir décidé quoi que ce soit, et le secret professionnel s'applique.",
    "Prévoir la suite" to
        "Après l'accouchement, la consultation postnatale est le moment pour parler " +
            "contraception si tu le souhaites — la loi 2017-043 garantit l'accès à la " +
            "planification familiale, y compris pour les jeunes.",
)

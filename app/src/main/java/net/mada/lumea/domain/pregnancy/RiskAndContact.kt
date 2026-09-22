package net.mada.lumea.domain.pregnancy

/**
 * Ce qui justifie d'appeler, et ce qui peut attendre.
 *
 * Une sage-femme de CSB suit des dizaines de femmes ; elle n'a pas de secrétariat
 * et son téléphone est souvent son numéro personnel. Donner le bouton « appeler »
 * sans dire quand s'en servir, c'est garantir qu'il sera utilisé pour des questions
 * qui pouvaient attendre la prochaine CPN — et qu'il ne le sera plus le jour où il
 * faut vraiment.
 *
 * D'où trois niveaux nets, du plus grave au plus banal. Le premier ne dit même pas
 * d'appeler : il dit de partir.
 */
enum class Urgency { PARTIR_MAINTENANT, APPELER_AUJOURDHUI, PROCHAINE_CPN }

val Urgency.title: String
    get() = when (this) {
        Urgency.PARTIR_MAINTENANT -> "Va au CSB tout de suite"
        Urgency.APPELER_AUJOURDHUI -> "Appelle ta sage-femme aujourd'hui"
        Urgency.PROCHAINE_CPN -> "À dire à la prochaine CPN"
    }

val Urgency.hint: String
    get() = when (this) {
        Urgency.PARTIR_MAINTENANT ->
            "Ne perds pas de temps à téléphoner d'abord. Pars, et fais prévenir en " +
                "chemin si quelqu'un peut le faire à ta place."
        Urgency.APPELER_AUJOURDHUI ->
            "Ça ne peut pas attendre la prochaine consultation, mais tu as le temps " +
                "d'un appel pour savoir quoi faire."
        Urgency.PROCHAINE_CPN ->
            "Note-le pour ne pas l'oublier le jour J. Ce n'est pas une raison " +
                "d'appeler entre deux consultations."
    }

data class Symptom(val label: String, val urgency: Urgency, val detail: String)

/**
 * Le tri des symptômes.
 *
 * Volontairement court : une liste de quarante lignes ne se lit pas quand on a
 * peur. Ce sont les motifs les plus fréquents de décès maternels évitables, plus
 * les plaintes banales qu'on croit à tort graves.
 */
val SYMPTOM_TRIAGE: List<Symptom> = listOf(
    Symptom(
        "Saignement abondant",
        Urgency.PARTIR_MAINTENANT,
        "L'hémorragie est la première cause de décès maternel, et c'est celle qui va " +
            "le plus vite. Pars immédiatement, de jour comme de nuit.",
    ),
    Symptom(
        "Douleur forte au ventre, d'un seul côté",
        Urgency.PARTIR_MAINTENANT,
        "En début de grossesse, avec ou sans saignement, cela peut être une grossesse " +
            "hors de l'utérus. C'est une urgence chirurgicale.",
    ),
    Symptom(
        "Convulsions, ou maux de tête violents avec vision trouble",
        Urgency.PARTIR_MAINTENANT,
        "Après 20 SA, ce sont les signes de l'éclampsie et de la pré-éclampsie, liées " +
            "à la tension. C'est la deuxième grande cause de décès évitable.",
    ),
    Symptom(
        "Fièvre élevée avec frissons",
        Urgency.PARTIR_MAINTENANT,
        "En zone de paludisme, une fièvre chez une femme enceinte se traite le jour " +
            "même : le paludisme est plus grave pendant la grossesse.",
    ),
    Symptom(
        "Perte de liquide, ou contractions régulières avant 37 SA",
        Urgency.PARTIR_MAINTENANT,
        "La poche peut s'être rompue, ou le travail commencer trop tôt. Direction la " +
            "maternité, même si tu ne souffres pas.",
    ),
    Symptom(
        "Le bébé bouge beaucoup moins que d'habitude",
        Urgency.PARTIR_MAINTENANT,
        "Au troisième trimestre, une baisse nette des mouvements sur une journée se " +
            "vérifie sur place. Mieux vaut cent déplacements pour rien.",
    ),
    Symptom(
        "Vomissements qui empêchent de boire depuis un jour",
        Urgency.APPELER_AUJOURDHUI,
        "Les nausées du début sont normales ; ne plus rien garder du tout déshydrate " +
            "et se soigne.",
    ),
    Symptom(
        "Grande fatigue, essoufflement, vertiges",
        Urgency.APPELER_AUJOURDHUI,
        "Souvent l'anémie, très fréquente ici. Elle se corrige avec du fer, mais elle " +
            "se vérifie par une prise de sang.",
    ),
    Symptom(
        "Brûlures en urinant",
        Urgency.APPELER_AUJOURDHUI,
        "Une infection urinaire non traitée peut déclencher un accouchement " +
            "prématuré. Elle se traite simplement quand on la prend tôt.",
    ),
    Symptom(
        "Gonflement rapide des mains et du visage",
        Urgency.APPELER_AUJOURDHUI,
        "Des chevilles gonflées en fin de journée sont banales ; un gonflement du " +
            "visage qui apparaît vite se vérifie, à cause de la tension.",
    ),
    Symptom(
        "Nausées du matin, seins tendus, envies fréquentes d'uriner",
        Urgency.PROCHAINE_CPN,
        "Ce sont les signes ordinaires du début de grossesse. Désagréables, pas " +
            "inquiétants.",
    ),
    Symptom(
        "Constipation, brûlures d'estomac, crampes dans les jambes",
        Urgency.PROCHAINE_CPN,
        "Très fréquents, surtout au 2ᵉ et 3ᵉ trimestre. Il existe des solutions " +
            "simples — demande-les en consultation.",
    ),
    Symptom(
        "Petites pertes de sang après un rapport",
        Urgency.PROCHAINE_CPN,
        "Souvent sans gravité, le col étant plus fragile. Mais si le saignement " +
            "devient abondant ou douloureux, la règle du haut de liste s'applique.",
    ),
    Symptom(
        "Questions sur l'alimentation, le travail, les voyages",
        Urgency.PROCHAINE_CPN,
        "Note tes questions au fil des semaines : on oublie toujours la moitié une " +
            "fois devant la sage-femme.",
    ),
)

/**
 * Les situations qui font surveiller une grossesse de plus près.
 *
 * L'app ne diagnostique rien : elle retient ce que la personne a déclaré, pour le
 * rappeler au bon moment et pour adapter ce qu'elle affiche. Le vrai destinataire
 * de cette liste est la sage-femme, à la première CPN.
 */
data class RiskFactor(val code: String, val label: String, val advice: String)

val RISK_FACTORS: List<RiskFactor> = listOf(
    RiskFactor(
        "AGE_JEUNE", "J'ai moins de 18 ans",
        "Une grossesse avant 18 ans expose davantage à l'hypertension, à l'anémie et " +
            "à un accouchement difficile. Ce n'est pas une fatalité : c'est une raison " +
            "de faire toutes les CPN et d'accoucher en structure, pas à la maison.",
    ),
    RiskFactor(
        "AGE_35", "J'ai plus de 35 ans",
        "Le suivi est un peu plus rapproché, notamment pour la tension et le diabète " +
            "de grossesse. Signale-le dès la première CPN.",
    ),
    RiskFactor(
        "PREMIERE", "C'est ma première grossesse",
        "La pré-éclampsie est plus fréquente lors d'une première grossesse. La prise " +
            "de tension à chaque CPN prend deux minutes et c'est ce qui la dépiste.",
    ),
    RiskFactor(
        "MULTIPARE", "J'ai déjà eu 4 grossesses ou plus",
        "Le risque d'hémorragie après l'accouchement augmente avec le nombre de " +
            "grossesses. Accoucher auprès d'un soignant qualifié est d'autant plus " +
            "important.",
    ),
    RiskFactor(
        "JUMEAUX", "On m'a dit que j'attends des jumeaux",
        "Suivi rapproché, besoins en fer plus élevés, et accouchement à prévoir en " +
            "maternité équipée. L'échographie confirme et oriente.",
    ),
    RiskFactor(
        "CESARIENNE", "J'ai déjà eu une césarienne",
        "Le lieu d'accouchement doit être choisi à l'avance, dans une structure " +
            "capable d'opérer. À décider avec ta sage-femme bien avant 36 SA.",
    ),
    RiskFactor(
        "PERTE", "J'ai déjà perdu une grossesse ou un bébé",
        "Ça mérite d'être dit dès la première consultation, pour le suivi comme pour " +
            "l'accompagnement. Ce n'était pas ta faute, et ça ne prédit pas la suite.",
    ),
    RiskFactor(
        "TENSION", "J'ai de la tension, ou j'en ai eu enceinte",
        "C'est le facteur qui demande la surveillance la plus serrée. Tension à " +
            "chaque CPN, et tout mal de tête violent se signale le jour même.",
    ),
    RiskFactor(
        "DIABETE", "J'ai du diabète",
        "Le suivi de la glycémie fait partie du suivi de grossesse. À signaler dès " +
            "la première CPN pour adapter le traitement.",
    ),
    RiskFactor(
        "ANEMIE", "On m'a déjà dit que j'étais anémiée",
        "Le fer quotidien n'est alors pas optionnel, et le déparasitage compte " +
            "double : les vers entretiennent l'anémie.",
    ),
    RiskFactor(
        "VIH", "Je vis avec le VIH",
        "Le traitement bien suivi rend la transmission au bébé très improbable. Le " +
            "suivi et les médicaments sont gratuits, et le secret professionnel " +
            "s'applique.",
    ),
    RiskFactor(
        "LOIN", "Le centre de santé est loin de chez moi",
        "C'est un vrai facteur de risque, et le plus sous-estimé. Prépare le trajet " +
            "à l'avance : qui t'emmène, avec quoi, et où tu dors si le travail " +
            "commence la nuit.",
    ),
)

fun riskFactor(code: String): RiskFactor? = RISK_FACTORS.firstOrNull { it.code == code }

/**
 * Les examens dont le résultat mérite d'être noté.
 *
 * Les comptes rendus se perdent, les carnets s'abîment, et on oublie toujours son
 * groupe sanguin au moment où on en a besoin. Ces valeurs sont saisies à la main
 * et ne sont jamais interprétées par l'app.
 */
val RECORDED_RESULTS: List<CareAct> = listOf(
    CareAct(
        "GROUPE_SANGUIN", "Mon groupe sanguin",
        "À connaître par cœur. En cas d'hémorragie, chaque minute gagnée compte. " +
            "Note aussi le rhésus : un rhésus négatif demande une surveillance " +
            "particulière.",
        0, 14, CareCategory.CPN,
    ),
    CareAct(
        "ECHO1", "Échographie de datation",
        "Faite idéalement entre 11 et 14 SA quand elle est disponible. C'est elle " +
            "qui date la grossesse avec précision : si son terme diffère du calcul de " +
            "l'app, c'est elle qui a raison. Note le terme qu'elle donne.",
        11, 16, CareCategory.CPN,
    ),
    CareAct(
        "ECHO2", "Échographie du 2ᵉ trimestre",
        "Vers 22 SA. Elle examine les organes du bébé, le placenta et la quantité de " +
            "liquide. Note ce qui t'a été dit, même si tout va bien.",
        20, 26, CareCategory.CPN,
    ),
    CareAct(
        "ECHO3", "Échographie du 3ᵉ trimestre",
        "Vers 32 SA. Croissance, position du bébé et du placenta — de quoi décider " +
            "du lieu d'accouchement.",
        30, 36, CareCategory.CPN,
    ),
    CareAct(
        "VIH_SYPHILIS", "Dépistage VIH et syphilis",
        "Proposé à la première CPN, gratuit et confidentiel. Une syphilis non " +
            "traitée pendant la grossesse est grave pour le bébé et se soigne avec " +
            "une seule injection.",
        0, 16, CareCategory.CPN,
    ),
    CareAct(
        "ALLERGIES", "Mes allergies",
        "Médicaments, aliments, piqûres. C'est la première question posée aux " +
            "urgences, et celle à laquelle on ne sait plus répondre quand on ne va " +
            "pas bien. Écris « aucune » si c'est le cas : une case vide laisse le " +
            "doute.",
        0, 42, CareCategory.CPN,
    ),
    CareAct(
        "TRAITEMENTS", "Mes traitements en cours",
        "Tout ce que tu prends régulièrement, y compris le fer, le TPIg, une " +
            "contraception ou un traitement au long cours. Certains médicaments " +
            "s'associent mal entre eux.",
        0, 42, CareCategory.CPN,
    ),
    CareAct(
        "CONTACT_URGENCE", "Personne à prévenir",
        "Un nom et un numéro. Quelqu'un qui peut venir, décider avec toi, ou " +
            "simplement être là.",
        0, 42, CareCategory.CPN,
    ),
    CareAct(
        "HEMOGLOBINE", "Taux d'hémoglobine",
        "La mesure de l'anémie. En dessous de 11 g/dl on parle d'anémie ; en dessous " +
            "de 7, c'est sévère et ça se traite sans attendre.",
        0, 42, CareCategory.CPN,
    ),
)

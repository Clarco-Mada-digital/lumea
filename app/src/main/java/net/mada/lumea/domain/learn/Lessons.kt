package net.mada.lumea.domain.learn

/**
 * Le contenu éducatif de l'app.
 *
 * Tout est écrit en dur : pas de réseau, pas de base, donc rien à charger et rien
 * qui fuite. Les chiffres d'efficacité viennent des tableaux CDC / ACOG / NHS et
 * sont donnés en « usage réel » (ce qui arrive vraiment aux gens) plutôt qu'en
 * « usage parfait », parce que c'est l'usage réel qui décide des grossesses.
 *
 * Ton visé : factuel, chaleureux, sans jugement. Ce n'est pas un cours de morale,
 * et ça ne remplace pas un soignant.
 */

data class LessonSection(val heading: String, val body: String)

data class Lesson(
    val id: String,
    val emoji: String,
    val title: String,
    val summary: String,
    val minutes: Int,
    val sections: List<LessonSection>,
)

data class LessonTopic(val title: String, val subtitle: String, val lessons: List<Lesson>)

// ---------------------------------------------------------------- Mon corps

private val cycleBasics = Lesson(
    id = "cycle-basics",
    emoji = "🌙",
    title = "C'est quoi, un cycle ?",
    summary = "Le compte à rebours qui recommence chaque mois, et ce qu'il fabrique.",
    minutes = 3,
    sections = listOf(
        LessonSection(
            "Le jour 1, c'est le premier jour de saignement",
            "Le cycle menstruel ne commence pas quand les règles finissent, mais le jour " +
                "où elles commencent. C'est la convention utilisée partout, y compris dans " +
                "cette app. Le cycle se termine la veille des règles suivantes.",
        ),
        LessonSection(
            "Combien de temps ça dure",
            "On dit souvent « 28 jours ». En réalité, un cycle entre 21 et 35 jours est " +
                "tout à fait normal chez l'adulte, et la durée varie d'un mois à l'autre " +
                "chez la même personne. Une variation de quelques jours n'a rien " +
                "d'inquiétant.",
        ),
        LessonSection(
            "Ce que fait ton corps pendant ce temps",
            "Chaque mois, l'utérus prépare une muqueuse épaisse au cas où un ovule " +
                "fécondé viendrait s'y installer. Un ovaire libère un ovule au milieu du " +
                "cycle. S'il n'y a pas de fécondation, la muqueuse n'est plus utile : elle " +
                "se détache et s'évacue — ce sont les règles. Puis tout recommence.",
        ),
        LessonSection(
            "Pourquoi l'humeur et l'énergie bougent",
            "Deux hormones principales, l'œstrogène et la progestérone, montent et " +
                "descendent au fil du cycle. Elles n'agissent pas que sur l'utérus : elles " +
                "touchent le sommeil, l'appétit, la peau, la concentration et l'humeur. " +
                "Se sentir fatiguée ou à fleur de peau à certains moments du mois n'est " +
                "ni dans ta tête, ni un défaut de caractère.",
        ),
    ),
)

private val phases = Lesson(
    id = "phases",
    emoji = "🔄",
    title = "Les quatre phases",
    summary = "Règles, folliculaire, ovulation, lutéale — et ce que tu peux en attendre.",
    minutes = 4,
    sections = listOf(
        LessonSection(
            "1. Les règles (jours 1 à 5 environ)",
            "La muqueuse utérine s'évacue. Les hormones sont au plus bas. Beaucoup de " +
                "femmes se sentent fatiguées, ont froid, ou ont mal au ventre. C'est le " +
                "moment de lever le pied si tu peux : chaleur, sommeil, aliments riches " +
                "en fer.",
        ),
        LessonSection(
            "2. La phase folliculaire (jusqu'à l'ovulation)",
            "L'œstrogène remonte. L'énergie, l'humeur et la concentration suivent " +
                "souvent. Beaucoup décrivent cette période comme celle où on a envie de " +
                "lancer des projets, de bouger, de voir du monde.",
        ),
        LessonSection(
            "3. L'ovulation (un seul jour)",
            "Un ovaire libère un ovule. C'est bref : l'ovule ne survit que 12 à 24 heures. " +
                "Certaines ressentent une petite douleur d'un côté du bas-ventre, ou " +
                "remarquent des pertes plus claires et élastiques, comme du blanc d'œuf.",
        ),
        LessonSection(
            "4. La phase lutéale (après l'ovulation)",
            "La progestérone domine. Cette phase dure assez régulièrement 12 à 14 jours — " +
                "c'est d'ailleurs pour ça que l'app estime l'ovulation en comptant à " +
                "rebours depuis les règles suivantes, et non en avant depuis les " +
                "précédentes. C'est aussi la période du syndrome prémenstruel : seins " +
                "sensibles, ballonnements, irritabilité, fringales.",
        ),
        LessonSection(
            "Ce n'est pas une règle universelle",
            "Ces descriptions sont des tendances, pas un programme. Certaines ne " +
                "ressentent presque rien, d'autres beaucoup. En notant tes journées dans " +
                "l'app, tu découvriras ton propre schéma, qui vaut mieux que n'importe " +
                "quelle moyenne.",
        ),
    ),
)

private val ovulation = Lesson(
    id = "ovulation",
    emoji = "🥚",
    title = "L'ovulation et la fenêtre fertile",
    summary = "Pourquoi la période fertile dure six jours alors que l'ovule ne vit qu'un jour.",
    minutes = 4,
    sections = listOf(
        LessonSection(
            "L'ovule vit un jour, les spermatozoïdes cinq",
            "C'est le point-clé, et il surprend souvent. L'ovule ne survit que 12 à 24 " +
                "heures après avoir été libéré. Mais les spermatozoïdes peuvent survivre " +
                "jusqu'à 5 jours dans le corps. Un rapport ayant lieu cinq jours AVANT " +
                "l'ovulation peut donc mener à une grossesse.",
        ),
        LessonSection(
            "D'où les six jours de fenêtre fertile",
            "En additionnant : les 5 jours avant l'ovulation, le jour même, et celui qui " +
                "suit. C'est ce que l'app affiche en vert sur le calendrier. Une grossesse " +
                "est plus probable pendant ces jours-là.",
        ),
        LessonSection(
            "Mais cette fenêtre est une estimation, pas une certitude",
            "L'app calcule à partir de tes cycles passés. Or l'ovulation peut se décaler : " +
                "stress, maladie, voyage, manque de sommeil, changement de poids. Un " +
                "décalage de quelques jours suffit à déplacer toute la fenêtre. Personne — " +
                "aucune app — ne peut te dire avec certitude quand tu ovules.",
        ),
        LessonSection(
            "Conséquence directe",
            "Aucun jour du cycle ne peut être considéré comme « sans risque » de " +
                "grossesse. Si tu ne souhaites pas être enceinte, la protection doit être " +
                "la même tous les jours du mois. La leçon « Ce qui protège vraiment » " +
                "détaille les options.",
        ),
    ),
)

private val irregular = Lesson(
    id = "irregular",
    emoji = "〰️",
    title = "Cycles irréguliers",
    summary = "Très fréquent, surtout les premières années. Quand s'en inquiéter.",
    minutes = 3,
    sections = listOf(
        LessonSection(
            "Les premières années, c'est la norme",
            "Après les premières règles, il faut souvent 2 à 5 ans avant que les cycles " +
                "se régularisent. Pendant cette période, des cycles de 21 jours puis de 45 " +
                "jours sont courants, et des mois peuvent être sautés. Ce n'est pas un " +
                "problème de santé en soi.",
        ),
        LessonSection(
            "Ce qui peut dérégler un cycle",
            "Stress, examens, chagrin, voyage et décalage horaire, perte ou prise de " +
                "poids importante, sport intensif, maladie, manque de sommeil. Le cycle " +
                "est un bon baromètre de l'état général du corps.",
        ),
        LessonSection(
            "L'app est moins fiable sur un cycle irrégulier",
            "Les prévisions reposent sur la moyenne de tes derniers cycles. Si ceux-ci " +
                "varient beaucoup, l'indicateur « Régularité » affichera « Variable » ou " +
                "« Irrégulier » : c'est l'app qui te dit honnêtement que ses prévisions " +
                "sont approximatives. Ne t'appuie surtout pas dessus pour éviter une " +
                "grossesse.",
        ),
        LessonSection(
            "Quand en parler à un soignant",
            "Si tu n'as aucune règle pendant plus de 3 mois sans être enceinte, si les " +
                "cycles font régulièrement moins de 21 ou plus de 45 jours après " +
                "plusieurs années de règles, ou si les saignements sont très abondants " +
                "ou très douloureux.",
        ),
    ),
)

private val firstPeriod = Lesson(
    id = "first-period",
    emoji = "🌸",
    title = "Les premières règles",
    summary = "À quoi s'attendre, quoi préparer, et ce qui est normal.",
    minutes = 3,
    sections = listOf(
        LessonSection(
            "Quand ça arrive",
            "Le plus souvent entre 10 et 15 ans, généralement deux ans après le début " +
                "du développement de la poitrine. Il n'y a pas d'âge « correct » : très " +
                "tôt ou assez tard, les deux existent.",
        ),
        LessonSection(
            "À quoi ça ressemble",
            "Souvent peu abondant au début, parfois brun ou rosé plutôt que rouge vif — " +
                "c'est du sang qui a mis plus de temps à sortir, rien d'anormal. Les " +
                "premiers cycles sont souvent irréguliers et peuvent être espacés de " +
                "plusieurs mois.",
        ),
        LessonSection(
            "Ce qu'il est utile d'avoir",
            "Des serviettes ou des protections lavables pour commencer (les tampons et " +
                "la coupe menstruelle demandent un peu plus d'habitude), une trousse " +
                "discrète dans le sac, et un sous-vêtement de rechange. Marquer le " +
                "premier jour dans l'app dès maintenant permettra d'y voir clair d'ici " +
                "quelques mois.",
        ),
        LessonSection(
            "Ce n'est pas sale",
            "Les règles sont un processus biologique ordinaire, pas une saleté ni une " +
                "honte. Tu peux te laver, nager, faire du sport, vivre normalement. Il " +
                "n'y a aucune raison de se cacher.",
        ),
    ),
)

private val painAndFlow = Lesson(
    id = "pain-flow",
    emoji = "🤍",
    title = "Douleurs et flux",
    summary = "Ce qui soulage vraiment, et le seuil au-delà duquel il faut consulter.",
    minutes = 3,
    sections = listOf(
        LessonSection(
            "D'où vient la douleur",
            "L'utérus est un muscle qui se contracte pour évacuer sa muqueuse. Ces " +
                "contractions provoquent les crampes. Une douleur modérée les premiers " +
                "jours est fréquente.",
        ),
        LessonSection(
            "Ce qui aide",
            "La chaleur sur le ventre ou le bas du dos (bouillotte, douche chaude) est " +
                "efficace et sans effet secondaire. L'activité physique douce, " +
                "contre-intuitivement, réduit souvent les crampes. Les anti-inflammatoires " +
                "comme l'ibuprofène sont plus efficaces que le paracétamol pour ce type " +
                "de douleur — à prendre selon la notice, et en parler à un pharmacien en " +
                "cas de doute.",
        ),
        LessonSection(
            "Un flux abondant, c'est quoi au juste",
            "Changer de protection toutes les heures ou moins pendant plusieurs heures " +
                "d'affilée, devoir se lever la nuit pour changer, ou perdre des caillots " +
                "plus gros qu'une pièce de monnaie. Un flux vraiment abondant peut " +
                "provoquer une anémie : fatigue, pâleur, essoufflement.",
        ),
        LessonSection(
            "Une douleur qui t'empêche de vivre n'est pas normale",
            "Rater les cours ou le travail, vomir de douleur, ne pas être soulagée par " +
                "les médicaments courants : ce n'est pas « être douillette ». Cela peut " +
                "signaler une endométriose ou d'autres causes traitables, souvent " +
                "diagnostiquées avec des années de retard parce qu'on a appris aux filles " +
                "à endurer. Consulte, et insiste si on te minimise.",
        ),
    ),
)

private val seeADoctor = Lesson(
    id = "see-a-doctor",
    emoji = "🩺",
    title = "Quand consulter",
    summary = "Les signaux qui méritent un avis, sans paniquer pour autant.",
    minutes = 2,
    sections = listOf(
        LessonSection(
            "Prends rendez-vous si…",
            "• Aucune règle à 16 ans, ou aucun signe de puberté à 14 ans\n" +
                "• Plus de 3 mois sans règles sans être enceinte\n" +
                "• Douleurs qui t'empêchent d'aller en cours ou au travail\n" +
                "• Saignements très abondants, ou entre les règles\n" +
                "• Pertes inhabituelles : odeur forte, couleur verte ou grise, démangeaisons\n" +
                "• Douleur pendant les rapports\n" +
                "• Tout doute après un rapport non protégé",
        ),
        LessonSection(
            "À qui s'adresser",
            "Médecin généraliste, sage-femme, gynécologue, infirmerie scolaire, centre de " +
                "planning familial. Dans beaucoup de pays, les centres de planning " +
                "reçoivent les mineures gratuitement et de façon confidentielle.",
        ),
        LessonSection(
            "Ce que tu peux apporter",
            "Tes notes de l'app. Les dates de tes dernières règles, la durée des cycles " +
                "et tes symptômes récurrents font gagner du temps au soignant et rendent " +
                "le diagnostic plus précis. C'est l'une des raisons d'utiliser un suivi.",
        ),
    ),
)

// -------------------------------------------------------------- Se protéger

private val contraceptionOverview = Lesson(
    id = "contraception",
    emoji = "🛡️",
    title = "Ce qui protège vraiment",
    summary = "Les méthodes classées par efficacité réelle, sans langue de bois.",
    minutes = 5,
    sections = listOf(
        LessonSection(
            "Comment lire ces chiffres",
            "Ils indiquent, sur 100 personnes utilisant la méthode pendant un an, " +
                "combien tombent enceintes. On donne ici l'« usage réel » : celui où on " +
                "oublie parfois un comprimé, où le préservatif glisse. C'est le chiffre " +
                "honnête, souvent très différent de l'usage parfait en laboratoire.",
        ),
        LessonSection(
            "Très efficaces — moins de 1 grossesse sur 100",
            "• Implant (bâtonnet sous la peau du bras), 3 ans\n" +
                "• DIU hormonal ou au cuivre (stérilet), 5 à 10 ans\n\n" +
                "Leur force : une fois posés, il n'y a plus rien à penser. C'est " +
                "précisément ce qui explique l'écart avec la pilule. Contrairement à une " +
                "idée reçue, le DIU est possible même sans avoir eu d'enfant.",
        ),
        LessonSection(
            "Moyennement efficaces — 4 à 9 sur 100",
            "• Injection trimestrielle : environ 4\n" +
                "• Pilule, patch, anneau : environ 7 à 9\n\n" +
                "La pilule serait à 0,3 si elle était prise parfaitement chaque jour à " +
                "la même heure. L'écart entre 0,3 et 9, c'est la vraie vie.",
        ),
        LessonSection(
            "Moins efficaces — plus de 10 sur 100",
            "• Préservatif externe : environ 13\n" +
                "• Méthodes calendaires et applications : 12 à 24\n" +
                "• Retrait : environ 20\n" +
                "• Aucune méthode : environ 85",
        ),
        LessonSection(
            "La méthode du calendrier n'est pas une contraception",
            "Suivre son cycle dans une app — y compris celle-ci — fait partie des " +
                "méthodes les moins fiables : jusqu'à 24 personnes sur 100 enceintes en " +
                "un an. Et c'est encore pire quand les cycles sont irréguliers, ce qui " +
                "est le cas le plus fréquent chez les adolescentes. Lumea t'aide à " +
                "connaître ton corps ; elle ne te protège pas d'une grossesse.",
        ),
        LessonSection(
            "La combinaison la plus solide",
            "Une méthode très efficace pour la grossesse (implant, DIU ou pilule) ET un " +
                "préservatif pour les IST. Aucune contraception hormonale ne protège des " +
                "infections ; le préservatif est le seul à faire les deux à la fois.",
        ),
    ),
)

private val condoms = Lesson(
    id = "condoms",
    emoji = "🧤",
    title = "Le préservatif, bien utilisé",
    summary = "Le seul moyen qui protège à la fois de la grossesse et des IST.",
    minutes = 3,
    sections = listOf(
        LessonSection(
            "Pourquoi il reste indispensable",
            "C'est la seule méthode qui protège des infections sexuellement " +
                "transmissibles. Même avec un implant ou un stérilet, le préservatif " +
                "reste nécessaire tant qu'on n'est pas certain du statut des deux " +
                "partenaires.",
        ),
        LessonSection(
            "Les erreurs qui font échouer",
            "• Le mettre seulement à la fin, après avoir commencé\n" +
                "• Oublier de pincer le réservoir au bout pour chasser l'air\n" +
                "• Le dérouler dans le mauvais sens puis le retourner — il faut en " +
                "prendre un neuf\n" +
                "• Utiliser une huile ou une crème comme lubrifiant : le latex se perce. " +
                "Seuls les lubrifiants à base d'eau ou de silicone conviennent\n" +
                "• En mettre deux l'un sur l'autre : le frottement les déchire\n" +
                "• L'ouvrir avec les dents, ou le garder dans une poche serrée au chaud\n" +
                "• Vérifier la date de péremption : un préservatif périmé casse",
        ),
        LessonSection(
            "S'il craque",
            "Ça arrive et ce n'est pas une catastrophe si on réagit. Une contraception " +
                "d'urgence est possible jusqu'à 3 à 5 jours après, et plus elle est prise " +
                "tôt, plus elle marche. Un dépistage des IST est également conseillé. Voir " +
                "la leçon suivante.",
        ),
        LessonSection(
            "Où s'en procurer",
            "Pharmacies, supermarchés, distributeurs. Les centres de planning familial, " +
                "les infirmeries scolaires et de nombreuses associations en distribuent " +
                "gratuitement, sans questions et sans condition d'âge.",
        ),
    ),
)

private val emergency = Lesson(
    id = "emergency",
    emoji = "⏱️",
    title = "La contraception d'urgence",
    summary = "Que faire après un rapport non protégé — et dans quel délai.",
    minutes = 3,
    sections = listOf(
        LessonSection(
            "Chaque heure compte",
            "La pilule d'urgence (« pilule du lendemain ») est d'autant plus efficace " +
                "qu'elle est prise tôt. Selon la molécule, le délai maximal est de 3 ou 5 " +
                "jours, mais l'efficacité chute nettement au fil des heures. Ne pas " +
                "attendre le lendemain matin par gêne.",
        ),
        LessonSection(
            "L'option la plus efficace",
            "La pose d'un DIU au cuivre dans les 5 jours est la contraception d'urgence " +
                "la plus efficace qui existe — et elle devient ensuite ta contraception " +
                "durable. Elle nécessite un rendez-vous médical.",
        ),
        LessonSection(
            "Où l'obtenir",
            "En pharmacie, sans ordonnance. Dans de nombreux pays elle est délivrée " +
                "gratuitement et de manière anonyme aux mineures, y compris en pharmacie, " +
                "en infirmerie scolaire ou en centre de planning familial.",
        ),
        LessonSection(
            "Ce que ce n'est pas",
            "Ce n'est pas un avortement : la pilule d'urgence agit en retardant " +
                "l'ovulation, elle n'interrompt pas une grossesse déjà installée. Ce n'est " +
                "pas non plus une contraception régulière : elle est moins efficace et " +
                "moins bien tolérée qu'une méthode continue.",
        ),
        LessonSection(
            "Et après",
            "Fais un test de grossesse si les règles ont plus d'une semaine de retard. " +
                "Pense aussi au dépistage des IST : la contraception d'urgence n'en " +
                "protège pas.",
        ),
    ),
)

private val fertilityMyths = Lesson(
    id = "myths",
    emoji = "❌",
    title = "Les idées fausses qui font des bébés",
    summary = "Ce qu'on entend souvent, et pourquoi c'est faux.",
    minutes = 3,
    sections = listOf(
        LessonSection(
            "« On ne peut pas tomber enceinte pendant les règles »",
            "Faux. C'est moins probable, mais possible — surtout avec un cycle court. " +
                "Les spermatozoïdes survivent jusqu'à 5 jours : un rapport en fin de " +
                "règles peut rencontrer une ovulation précoce.",
        ),
        LessonSection(
            "« Pas la première fois »",
            "Faux. Une grossesse est possible dès le premier rapport, et même avant les " +
                "toutes premières règles — puisque l'ovulation précède les règles.",
        ),
        LessonSection(
            "« Le retrait suffit »",
            "Faux. Environ 20 personnes sur 100 tombent enceintes en un an avec le " +
                "retrait. Le liquide pré-éjaculatoire peut contenir des spermatozoïdes, " +
                "et le contrôle du moment est imparfait par nature.",
        ),
        LessonSection(
            "« Mon app me dit que je ne suis pas fertile aujourd'hui »",
            "Une app ne mesure rien : elle extrapole à partir de tes cycles passés. " +
                "L'ovulation se décale avec le stress, la maladie ou la fatigue. C'est " +
                "pour cette raison que Lumea n'affiche jamais de jour « sans danger ».",
        ),
        LessonSection(
            "« Une douche ou une position particulière évite la grossesse »",
            "Faux, et les douches vaginales sont même déconseillées : elles déséquilibrent " +
                "la flore et augmentent le risque d'infection. Aucune position, aucun " +
                "rinçage n'a d'effet contraceptif.",
        ),
    ),
)

// ------------------------------------------------------- Santé et relations

private val stis = Lesson(
    id = "stis",
    emoji = "🔬",
    title = "Les IST, sans dramatiser",
    summary = "Souvent sans symptôme, presque toujours traitables, faciles à dépister.",
    minutes = 4,
    sections = listOf(
        LessonSection(
            "Le plus important : souvent aucun symptôme",
            "La chlamydia, l'infection la plus fréquente chez les jeunes, est " +
                "silencieuse dans la majorité des cas. On peut la transmettre sans le " +
                "savoir. C'est pourquoi le dépistage ne se décide pas « quand ça fait " +
                "mal » mais régulièrement, et à chaque changement de partenaire.",
        ),
        LessonSection(
            "Pourquoi il ne faut pas laisser traîner",
            "Une chlamydia non traitée peut remonter et abîmer les trompes, ce qui " +
                "provoque des douleurs chroniques et peut compromettre une grossesse " +
                "future. Traitée, c'est souvent une simple cure d'antibiotiques.",
        ),
        LessonSection(
            "Les signes qui doivent alerter",
            "Pertes inhabituelles ou malodorantes, brûlures en urinant, boutons ou " +
                "plaies sur les parties génitales, démangeaisons, douleur au bas-ventre, " +
                "douleur pendant les rapports. Mais encore une fois : leur absence ne " +
                "prouve rien.",
        ),
        LessonSection(
            "Le vaccin contre le HPV",
            "Le papillomavirus se transmet très facilement et cause la quasi-totalité " +
                "des cancers du col de l'utérus. Le vaccin est recommandé aux filles " +
                "comme aux garçons, idéalement avant les premiers rapports, et reste " +
                "utile après. C'est l'un des rares vaccins qui prévient un cancer.",
        ),
        LessonSection(
            "Se faire dépister",
            "Un dépistage, c'est en général une prise de sang et/ou un prélèvement " +
                "urinaire : rapide et indolore. Centres de dépistage gratuits, planning " +
                "familial, médecin généraliste. Souvent gratuit et anonyme pour les " +
                "jeunes.",
        ),
    ),
)

private val consent = Lesson(
    id = "consent",
    emoji = "💬",
    title = "Le consentement",
    summary = "Ce que c'est, ce que ça n'est pas, et pourquoi ça se redemande.",
    minutes = 3,
    sections = listOf(
        LessonSection(
            "Un oui libre, clair, et réversible",
            "Le consentement, c'est un accord donné librement, sans pression ni chantage " +
                "affectif, par une personne en état de décider — donc ni endormie, ni " +
                "ivre, ni droguée. Et il peut être retiré à tout moment, même après avoir " +
                "commencé.",
        ),
        LessonSection(
            "Ce qui n'est pas un consentement",
            "Le silence. Le fait de ne pas oser dire non. Avoir dit oui la dernière " +
                "fois. Être en couple ou marié. Avoir accepté un baiser. Porter une " +
                "certaine tenue. Avoir bu. Céder pour avoir la paix.",
        ),
        LessonSection(
            "Ça marche dans les deux sens",
            "Demander n'est pas gênant, c'est la base : « tu as envie ? », « ça va ? », " +
                "« tu veux qu'on s'arrête ? ». Et personne ne te doit de justification " +
                "pour dire non.",
        ),
        LessonSection(
            "Retirer le préservatif en cachette est une agression",
            "L'enlever pendant le rapport sans que l'autre le sache annule le " +
                "consentement donné et expose à une grossesse et à des infections. C'est " +
                "reconnu comme une infraction dans un nombre croissant de pays.",
        ),
        LessonSection(
            "Si ça s'est mal passé",
            "Ce n'est jamais ta faute, quelles que soient les circonstances. Tu peux en " +
                "parler à un adulte de confiance, à un soignant, ou appeler un numéro " +
                "d'aide. Une contraception d'urgence et un dépistage restent possibles. " +
                "Un examen médical peut aussi conserver des preuves si tu décides plus " +
                "tard de porter plainte.",
        ),
    ),
)

private val gettingHelp = Lesson(
    id = "help",
    emoji = "🤝",
    title = "À qui en parler",
    summary = "Des interlocuteurs gratuits et confidentiels, même mineure.",
    minutes = 2,
    sections = listOf(
        LessonSection(
            "Le planning familial",
            "C'est l'endroit le plus adapté : contraception, dépistage, contraception " +
                "d'urgence, grossesse, violences. Gratuit, confidentiel, et accessible " +
                "aux mineures sans autorisation parentale dans de nombreux pays.",
        ),
        LessonSection(
            "L'infirmerie de ton établissement",
            "Souvent sous-estimée. L'infirmière scolaire est tenue au secret " +
                "professionnel, peut délivrer une contraception d'urgence et orienter " +
                "vers les bons services.",
        ),
        LessonSection(
            "Un pharmacien",
            "Accessible sans rendez-vous, il peut conseiller sur la contraception " +
                "d'urgence, les tests de grossesse et les douleurs de règles, et dire " +
                "quand il faut consulter.",
        ),
        LessonSection(
            "Un adulte de confiance",
            "Un parent, une tante, une grande sœur, un professeur. Ce n'est pas " +
                "toujours possible ni simple, mais ne reste pas seule avec une inquiétude " +
                "— la plupart des situations se règlent bien quand on en parle tôt.",
        ),
        LessonSection(
            "Et en cas d'urgence",
            "Si tu es en danger immédiat, appelle les secours. Beaucoup de pays " +
                "disposent de lignes d'écoute anonymes et gratuites pour les violences " +
                "sexuelles et conjugales ; une recherche au nom de ton pays te donnera le " +
                "numéro local.",
        ),
    ),
)

/** Ce que l'app répète partout où elle parle de fertilité. */
const val FERTILITY_DISCLAIMER =
    "Aucun jour n'est « sans risque ». Les prévisions de Lumea sont des estimations " +
        "basées sur tes cycles passés : elles ne remplacent pas une contraception."

/** Le contenu éducatif, regroupé par thème. Defini après les leçons pour éviter
 *  les références avant : chaque `private val` est initialisé avant d'être utilisé. */
val lessonTopics: List<LessonTopic> = listOf(
    LessonTopic(
        title = "Mon corps",
        subtitle = "Comprendre ce qui se passe, mois après mois",
        lessons = listOf(cycleBasics, phases, ovulation, irregular, firstPeriod, painAndFlow, seeADoctor),
    ),
    LessonTopic(
        title = "Se protéger",
        subtitle = "Ce qui marche vraiment, chiffres à l'appui",
        lessons = listOf(contraceptionOverview, condoms, emergency, fertilityMyths),
    ),
    LessonTopic(
        title = "Santé et relations",
        subtitle = "IST, consentement, et à qui en parler",
        lessons = listOf(stis, consent, gettingHelp),
    ),
)

fun findLesson(id: String): Lesson? =
    lessonTopics.flatMap { it.lessons }.firstOrNull { it.id == id }

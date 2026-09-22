package net.mada.lumea.domain.cycle

/**
 * Le cycle relu sous l'angle « quel est le risque de tomber enceinte aujourd'hui ».
 *
 * C'est la lecture qui compte pour le public de l'app. Les phases (folliculaire,
 * lutéale) sont de la biologie ; le risque, lui, est une décision à prendre le soir
 * même. Les deux coexistent : la phase explique, le risque prévient.
 *
 * Volontairement, aucun niveau ne s'appelle « nul » ou « sûr ». Un cycle peut
 * avancer ou reculer de plusieurs jours sans prévenir, des spermatozoïdes tiennent
 * cinq jours, et une ovulation précoce peut tomber pendant des règles longues :
 * [FAIBLE] veut dire « moins probable », jamais « impossible ».
 */
enum class PregnancyRisk { MAXIMAL, ELEVE, MODERE, FAIBLE, REGLES, INCONNU }

/**
 * Le niveau de risque d'un jour du calendrier.
 *
 * Les règles **enregistrées** passent devant tout le reste : c'est un fait vécu,
 * pas une prévision, et c'est le repère principal du calendrier. Les prévisions de
 * fertilité ne viennent qu'ensuite. Le cas où les deux se chevauchent (cycle court,
 * ovulation précoce) est rare mais réel : il est signalé à part par
 * [DayPrediction.fertileDuringPeriod] plutôt que d'effacer les règles.
 */
val DayPrediction.pregnancyRisk: PregnancyRisk
    get() = when {
        isLoggedPeriod -> PregnancyRisk.REGLES
        phase == CyclePhase.INCONNUE -> PregnancyRisk.INCONNU
        isOvulation -> PregnancyRisk.MAXIMAL
        isFertile -> PregnancyRisk.ELEVE
        isPredictedPeriod -> PregnancyRisk.REGLES
        isNearFertile -> PregnancyRisk.MODERE
        else -> PregnancyRisk.FAIBLE
    }

/**
 * Un jour de règles qui tombe aussi dans la fenêtre fertile estimée.
 *
 * Ça n'arrive que sur les cycles courts ou très irréguliers, mais c'est précisément
 * la situation où on croit ne rien risquer. Le calendrier garde le rose des règles
 * et ajoute l'avertissement par-dessus.
 */
val DayPrediction.fertileDuringPeriod: Boolean
    get() = isLoggedPeriod && (isFertile || isOvulation)

/** L'intitulé court affiché dans la légende et sur la fiche d'un jour. */
val PregnancyRisk.label: String
    get() = when (this) {
        PregnancyRisk.MAXIMAL -> "Risque maximal"
        PregnancyRisk.ELEVE -> "Risque élevé"
        PregnancyRisk.MODERE -> "Risque modéré"
        PregnancyRisk.FAIBLE -> "Risque plus faible"
        PregnancyRisk.REGLES -> "Règles"
        PregnancyRisk.INCONNU -> "Pas encore de prévision"
    }

/** Le symbole posé dans la case du calendrier : l'information ne passe pas que par la couleur. */
val PregnancyRisk.symbol: String
    get() = when (this) {
        PregnancyRisk.MAXIMAL -> "!!"
        PregnancyRisk.ELEVE -> "!"
        PregnancyRisk.MODERE -> "·"
        else -> ""
    }

/** Le conseil concret, à la deuxième personne, sans détour et sans morale. */
val PregnancyRisk.advice: String
    get() = when (this) {
        PregnancyRisk.MAXIMAL ->
            "C'est le jour où une grossesse est la plus probable de tout le cycle. " +
                "Rapport non protégé = risque réel. Préservatif, ou pas de rapport."
        PregnancyRisk.ELEVE ->
            "Tu es dans la fenêtre fertile. Les spermatozoïdes vivent jusqu'à 5 jours : " +
                "un rapport aujourd'hui peut féconder un ovule qui n'est pas encore là. " +
                "Préservatif à chaque fois."
        PregnancyRisk.MODERE ->
            "La fenêtre fertile est tout près, et elle peut se décaler de quelques jours " +
                "sans prévenir. Traite ces jours-là comme des jours à risque."
        PregnancyRisk.FAIBLE ->
            "Une grossesse est moins probable ce jour-là, mais aucun jour n'est à zéro. " +
                "Le préservatif reste la seule protection contre les IST."
        PregnancyRisk.REGLES ->
            "Jour de règles. Beaucoup de gens n'ont pas de rapport ces jours-là ; si c'est " +
                "le cas, le risque de grossesse est plus bas — pas nul, surtout si ton cycle " +
                "est court. Le préservatif reste utile : il protège des IST tous les jours."
        PregnancyRisk.INCONNU ->
            "Sans tes dates de règles, l'app ne peut rien prévoir pour ce jour. " +
                "En attendant : préservatif à chaque rapport."
    }

/** Une ligne de plus, pour la fiche détaillée d'un jour. */
val PregnancyRisk.protection: String
    get() = when (this) {
        PregnancyRisk.MAXIMAL, PregnancyRisk.ELEVE, PregnancyRisk.MODERE ->
            "Si un rapport non protégé a déjà eu lieu, la contraception d'urgence " +
                "reste possible jusqu'à 3 à 5 jours après, et elle agit d'autant mieux " +
                "qu'elle est prise tôt."
        else ->
            "Une contraception régulière (pilule, implant, stérilet) protège tous les " +
                "jours du cycle ; le préservatif, lui, est le seul à protéger des IST."
    }

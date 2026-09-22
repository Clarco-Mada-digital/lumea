package net.mada.lumea.domain.cycle

import net.mada.lumea.data.db.PeriodEntity
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

enum class CyclePhase { MENSTRUATION, FOLLICULAIRE, OVULATION, LUTEALE, INCONNUE }

/** Ce qu'il se passe un jour donné, tel qu'affiché dans le calendrier du cycle. */
data class DayPrediction(
    val date: LocalDate,
    val isLoggedPeriod: Boolean,
    val isPredictedPeriod: Boolean,
    val isFertile: Boolean,
    /**
     * Les jours qui bordent la fenêtre fertile. Une prévision se décale facilement
     * de deux ou trois jours : ces jours-là ne sont pas « sûrs », ils sont incertains,
     * et le calendrier doit le dire plutôt que de les peindre comme les autres.
     */
    val isNearFertile: Boolean = false,
    val isOvulation: Boolean,
    val phase: CyclePhase,
    /** 1 = premier jour des règles. 0 si inconnu. */
    val dayOfCycle: Int,
)

data class CycleInsight(
    val lastPeriodStart: LocalDate?,
    val dayOfCycle: Int,
    val phase: CyclePhase,
    val averageCycleLength: Int,
    val averagePeriodLength: Int,
    /** Écart-type des longueurs de cycle, en jours. Plus il est grand, moins la prévision est sûre. */
    val regularityDays: Int,
    val nextPeriodStart: LocalDate?,
    val daysUntilNextPeriod: Int?,
    val ovulationDate: LocalDate?,
    /** Jour du cycle où tombe l'ovulation (1 = premier jour des règles). */
    val ovulationDayOfCycle: Int?,
    val fertileWindow: ClosedRange<LocalDate>?,
    /** Vrai tant qu'on n'a pas assez d'historique pour une vraie moyenne. */
    val isEstimate: Boolean,

    /**
     * La date attendue des prochaines règles, **jamais décalée**.
     *
     * [nextPeriodStart] glisse d'un cycle entier dès que la date est dépassée, ce
     * qui fait disparaître le retard : l'app supposait que les règles étaient bien
     * arrivées le jour prévu sans jamais le demander. Celle-ci reste plantée sur
     * la prévision réelle, c'est elle qui permet de dire « en retard de 3 jours »
     * et de proposer la confirmation.
     */
    val expectedPeriodStart: LocalDate? = null,
    /** Nombre de jours de retard sur [expectedPeriodStart]. 0 si à l'heure ou en avance. */
    val daysLate: Int = 0,
    /** Plus d'un cycle complet sans rien noter : les prévisions ne veulent plus dire grand-chose. */
    val isStale: Boolean = false,
    /** Début des règles en cours (période ouverte, sans date de fin). */
    val currentPeriodStart: LocalDate? = null,
    /** Jour des règles en cours : 1 = premier jour. */
    val currentPeriodDay: Int? = null,
    /** Durée des dernières règles terminées, en jours. */
    val lastPeriodLength: Int? = null,
    /** Durée du dernier cycle complet (d'un début de règles au suivant). */
    val lastCycleLength: Int? = null,
) {
    val isLate: Boolean get() = daysLate > 0
}

/**
 * Calcule les phases et les prévisions du cycle.
 *
 * Méthode : la longueur moyenne du cycle est la moyenne des écarts entre débuts de
 * règles consécutifs (6 derniers cycles au plus, écarts aberrants < 15 ou > 60 jours
 * ignorés). L'ovulation est estimée à `lutealLength` jours avant les règles suivantes
 * — c'est la phase lutéale qui est stable, pas la phase folliculaire. La fenêtre
 * fertile couvre les 5 jours avant l'ovulation et le jour suivant (durée de vie des
 * spermatozoïdes et de l'ovule).
 *
 * Ces prévisions sont indicatives : ce n'est ni un diagnostic, ni un moyen de contraception.
 */
class CycleEngine(
    private val defaultCycleLength: Int = 28,
    private val defaultPeriodLength: Int = 5,
    private val lutealLength: Int = 14,
) {

    fun insight(periods: List<PeriodEntity>, today: LocalDate = LocalDate.now()): CycleInsight {
        val starts = periods.map { it.startDate }.sorted()
        val gaps = starts.zipWithNext { a, b -> ChronoUnit.DAYS.between(a, b).toInt() }
            .filter { it in 15..60 }
            .takeLast(6)

        val avgCycle = if (gaps.isEmpty()) defaultCycleLength else gaps.average().roundToInt()
        val regularity = if (gaps.size < 2) 0 else {
            val mean = gaps.average()
            Math.sqrt(gaps.sumOf { (it - mean) * (it - mean) } / gaps.size).roundToInt()
        }

        val loggedLengths = periods.mapNotNull { p ->
            p.endDate?.let { ChronoUnit.DAYS.between(p.startDate, it).toInt() + 1 }
        }.filter { it in 1..12 }.takeLast(6)
        val avgPeriod = if (loggedLengths.isEmpty()) defaultPeriodLength else loggedLengths.average().roundToInt()

        val lastStart = starts.lastOrNull()
        if (lastStart == null) {
            return CycleInsight(
                lastPeriodStart = null,
                dayOfCycle = 0,
                phase = CyclePhase.INCONNUE,
                averageCycleLength = avgCycle,
                averagePeriodLength = avgPeriod,
                regularityDays = regularity,
                nextPeriodStart = null,
                daysUntilNextPeriod = null,
                ovulationDate = null,
                ovulationDayOfCycle = null,
                fertileWindow = null,
                isEstimate = true,
            )
        }

        /*
         * La date attendue, telle qu'elle a été prévue : jour 1 des dernières
         * règles + longueur moyenne. On ne la déplace pas.
         */
        val expected = lastStart.plusDays(avgCycle.toLong())
        val rawLate = ChronoUnit.DAYS.between(expected, today).toInt().coerceAtLeast(0)

        /*
         * Au-delà d'un cycle entier de retard, ce n'est plus un retard : c'est que
         * l'app n'a pas été ouverte depuis longtemps. On reprend alors le décalage
         * d'un cycle sur l'autre pour ne pas afficher « en retard de 154 jours »,
         * et on le signale par [isStale].
         */
        val stale = rawLate > avgCycle
        var cycleStart: LocalDate = lastStart
        val cycle = avgCycle.toLong()
        if (stale) {
            while (cycleStart.plusDays(cycle).isBefore(today)) {
                cycleStart = cycleStart.plusDays(cycle)
            }
        }
        val daysLate = if (stale) 0 else rawLate

        // Les règles en cours : une période ouverte (pas encore terminée) qui a
        // commencé il y a moins de quinze jours — au-delà, c'est un oubli de clôture.
        val openPeriod = periods
            .filter { it.endDate == null }
            .maxByOrNull { it.startDate }
            ?.takeIf { !it.startDate.isAfter(today) && ChronoUnit.DAYS.between(it.startDate, today) < 15 }
        val currentPeriodDay = openPeriod?.let {
            ChronoUnit.DAYS.between(it.startDate, today).toInt() + 1
        }

        val lastClosed = periods.filter { it.endDate != null }.maxByOrNull { it.startDate }
        val lastPeriodLength = lastClosed?.endDate?.let {
            ChronoUnit.DAYS.between(lastClosed.startDate, it).toInt() + 1
        }
        val lastCycleLength = gaps.lastOrNull()

        val dayOfCycle = ChronoUnit.DAYS.between(cycleStart, today).toInt() + 1
        val nextStart = cycleStart.plusDays(cycle)
        val ovulation = nextStart.minusDays(lutealLength.toLong())
        val fertile = ovulation.minusDays(5)..ovulation.plusDays(1)

        val phase = when {
            openPeriod != null -> CyclePhase.MENSTRUATION
            dayOfCycle in 1..avgPeriod -> CyclePhase.MENSTRUATION
            today == ovulation -> CyclePhase.OVULATION
            today < ovulation -> CyclePhase.FOLLICULAIRE
            else -> CyclePhase.LUTEALE
        }

        return CycleInsight(
            lastPeriodStart = lastStart,
            dayOfCycle = dayOfCycle.coerceAtLeast(1),
            phase = phase,
            averageCycleLength = avgCycle,
            averagePeriodLength = avgPeriod,
            regularityDays = regularity,
            nextPeriodStart = nextStart,
            daysUntilNextPeriod = ChronoUnit.DAYS.between(today, nextStart).toInt(),
            ovulationDate = ovulation,
            ovulationDayOfCycle = avgCycle - lutealLength,
            fertileWindow = fertile,
            isEstimate = gaps.size < 2,
            expectedPeriodStart = expected,
            daysLate = daysLate,
            isStale = stale,
            currentPeriodStart = openPeriod?.startDate,
            currentPeriodDay = currentPeriodDay,
            lastPeriodLength = lastPeriodLength,
            lastCycleLength = lastCycleLength,
        )
    }

    /** Prévisions jour par jour sur l'intervalle demandé, pour peindre le calendrier. */
    fun predictions(
        periods: List<PeriodEntity>,
        from: LocalDate,
        to: LocalDate,
        today: LocalDate = LocalDate.now(),
    ): Map<LocalDate, DayPrediction> {
        val insight = insight(periods, today)
        val logged = buildSet {
            periods.forEach { p ->
                val end = p.endDate ?: p.startDate.plusDays((insight.averagePeriodLength - 1).toLong())
                var d = p.startDate
                while (!d.isAfter(end)) {
                    add(d); d = d.plusDays(1)
                }
            }
        }

        val predictedPeriod = mutableSetOf<LocalDate>()
        val fertileDays = mutableSetOf<LocalDate>()
        val nearFertileDays = mutableSetOf<LocalDate>()
        val ovulationDays = mutableSetOf<LocalDate>()
        val starts = periods.map { it.startDate }.sorted()
        val start = insight.lastPeriodStart

        if (start != null) {
            val cycle = insight.averageCycleLength.toLong()

            /** Marque l'ovulation et la fenêtre fertile d'un cycle qui commence le [cycleStart]. */
            fun markOvulation(cycleStart: LocalDate, nextStart: LocalDate) {
                val ov = nextStart.minusDays(lutealLength.toLong())
                if (ov.isBefore(cycleStart)) return
                ovulationDays += ov
                for (i in -5..1) fertileDays += ov.plusDays(i.toLong())
                // Marge d'incertitude de la prévision, de part et d'autre.
                for (i in -8..-6) nearFertileDays += ov.plusDays(i.toLong())
                for (i in 2..4) nearFertileDays += ov.plusDays(i.toLong())
            }

            // Cycles passés : bornés par deux débuts de règles réellement enregistrés,
            // donc l'ovulation affichée repose sur des dates connues, pas sur une moyenne.
            starts.zipWithNext { cycleStart, nextStart -> markOvulation(cycleStart, nextStart) }

            // Cycles futurs : projetés depuis le dernier début connu. On ne projette
            // jamais AVANT les premières règles enregistrées — ce serait inventer un
            // passé que l'utilisatrice n'a pas vécu dans l'app.
            var anchor: LocalDate = start
            while (anchor.isBefore(to.plusDays(cycle))) {
                val nextStart = anchor.plusDays(cycle)
                // Y compris pour le cycle en cours : si seul le jour 1 est enregistré,
                // les jours suivants s'affichent comme attendus. Les jours réellement
                // enregistrés prennent le dessus plus bas.
                for (i in 0 until insight.averagePeriodLength) {
                    predictedPeriod += anchor.plusDays(i.toLong())
                }
                markOvulation(anchor, nextStart)
                anchor = nextStart
            }
        }

        val result = LinkedHashMap<LocalDate, DayPrediction>()
        var d = from
        while (!d.isAfter(to)) {
            val isLogged = d in logged
            val isPredicted = !isLogged && d in predictedPeriod
            // Avant les toutes premières règles enregistrées, on ne sait rien.
            val known = starts.firstOrNull()?.let { !d.isBefore(it) } == true
            val dayOfCycle = if (known && start != null) {
                anchorDayOfCycle(start, insight.averageCycleLength, d)
            } else 0
            val phase = when {
                isLogged || isPredicted -> CyclePhase.MENSTRUATION
                d in ovulationDays -> CyclePhase.OVULATION
                !known -> CyclePhase.INCONNUE
                dayOfCycle < insight.averageCycleLength - lutealLength -> CyclePhase.FOLLICULAIRE
                else -> CyclePhase.LUTEALE
            }
            result[d] = DayPrediction(
                date = d,
                isLoggedPeriod = isLogged,
                isPredictedPeriod = isPredicted,
                isFertile = d in fertileDays,
                isNearFertile = d !in fertileDays && d in nearFertileDays,
                isOvulation = d in ovulationDays,
                phase = phase,
                dayOfCycle = dayOfCycle,
            )
            d = d.plusDays(1)
        }
        return result
    }

    private fun anchorDayOfCycle(start: LocalDate, cycleLength: Int, date: LocalDate): Int {
        val delta = ChronoUnit.DAYS.between(start, date)
        val mod = Math.floorMod(delta, cycleLength.toLong()).toInt()
        return mod + 1
    }
}

val CyclePhase.label: String
    get() = when (this) {
        CyclePhase.MENSTRUATION -> "Règles"
        CyclePhase.FOLLICULAIRE -> "Phase folliculaire"
        CyclePhase.OVULATION -> "Ovulation"
        CyclePhase.LUTEALE -> "Phase lutéale"
        CyclePhase.INCONNUE -> "À découvrir"
    }

/** Une phrase courte pour expliquer ce que le corps est en train de faire. */
val CyclePhase.hint: String
    get() = when (this) {
        CyclePhase.MENSTRUATION -> "Ton corps se repose. Chaleur, fer et douceur."
        CyclePhase.FOLLICULAIRE -> "L'énergie remonte : bon moment pour lancer des choses."
        CyclePhase.OVULATION -> "Pic d'énergie et de confiance. Fertilité maximale."
        CyclePhase.LUTEALE -> "L'énergie redescend doucement. Sommeil et calme font du bien."
        CyclePhase.INCONNUE -> "Enregistre tes premières règles pour voir tes prévisions."
    }

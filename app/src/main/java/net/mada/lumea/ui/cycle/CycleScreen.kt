package net.mada.lumea.ui.cycle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.mada.lumea.domain.cycle.CyclePhase
import net.mada.lumea.domain.cycle.DayPrediction
import net.mada.lumea.domain.pregnancy.ALL_CARE_ACTS
import net.mada.lumea.domain.pregnancy.INFANT_VACCINES
import net.mada.lumea.domain.pregnancy.POSTNATAL_VISITS
import net.mada.lumea.domain.pregnancy.careSchedule
import net.mada.lumea.domain.cycle.PregnancyRisk
import net.mada.lumea.domain.cycle.advice
import net.mada.lumea.domain.cycle.fertileDuringPeriod
import net.mada.lumea.domain.cycle.pregnancyRisk
import net.mada.lumea.domain.cycle.protection
import net.mada.lumea.domain.cycle.symbol
import net.mada.lumea.domain.cycle.hint
import net.mada.lumea.domain.cycle.label
import net.mada.lumea.domain.learn.FERTILITY_DISCLAIMER
import net.mada.lumea.domain.pregnancy.PREGNANCY_DISCLAIMER
import net.mada.lumea.domain.pregnancy.headline
import net.mada.lumea.ui.components.CycleRing
import net.mada.lumea.ui.components.FloatingNavBarSpace
import net.mada.lumea.ui.components.MonthCalendar
import net.mada.lumea.ui.components.cycleSegments
import net.mada.lumea.ui.containerViewModel
import net.mada.lumea.ui.pregnancy.NegativeResultSheet
import net.mada.lumea.ui.pregnancy.PregnancyTestSheet
import net.mada.lumea.ui.pregnancy.PregnancyViewModel
import net.mada.lumea.ui.pregnancy.TestResult
import net.mada.lumea.ui.theme.CycleColors
import net.mada.lumea.ui.theme.RiskColors
import net.mada.lumea.ui.theme.OvertitleStyle
import net.mada.lumea.ui.theme.heroBrush
import net.mada.lumea.ui.theme.onHeroColor
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CycleScreen(
    onOpenDayLog: (LocalDate) -> Unit,
    onOpenSetup: () -> Unit,
    onOpenLessons: () -> Unit = {},
    onOpenPregnancy: () -> Unit = {},
    onOpenAdvisor: () -> Unit = {},
) {
    val vm = containerViewModel { CycleViewModel(it.cycle, it.settings) }
    val appContext = androidx.compose.ui.platform.LocalContext.current.applicationContext
    vm.onDataChanged = { net.mada.lumea.widget.LumeaWidget.refreshAll(appContext) }
    val pregnancyVm = containerViewModel { PregnancyViewModel(it.pregnancy, it.cycle) }
    val pregnancy by pregnancyVm.state.collectAsStateWithLifecycle()
    val justRecorded by pregnancyVm.justRecorded.collectAsStateWithLifecycle()
    var testSheetOpen by remember { mutableStateOf(false) }

    /*
     * Le planning des soins projeté sur le calendrier.
     *
     * Pendant la grossesse il part du premier jour des dernières règles ; après la
     * naissance, de la date de naissance — ce sont alors les visites postnatales
     * et les vaccins du bébé qui s'affichent. Sans ça, le calendrier ne montrait
     * que des trimestres colorés, sans dire quand aller au CSB.
     */
    val careSchedule = remember(pregnancy.ongoing, pregnancy.isPostpartum) {
        val current = pregnancy.ongoing
        when {
            current == null -> emptyMap()
            pregnancy.isPostpartum -> current.birthDate
                ?.let { careSchedule(it, POSTNATAL_VISITS + INFANT_VACCINES) }
                .orEmpty()
            else -> current.lastPeriodStart
                ?.let { careSchedule(it, ALL_CARE_ACTS) }
                .orEmpty()
        }
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val month by vm.month.collectAsStateWithLifecycle()
    val today = LocalDate.now()

    // Toucher un jour ouvrait directement le carnet. On montre d'abord ce que ce
    // jour veut dire : le calendrier sert à savoir, pas seulement à remplir.
    var selectedDay by remember { mutableStateOf<LocalDate?>(null) }

    val feedback by vm.feedback.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(feedback) {
        feedback?.let {
            snackbar.showSnackbar(it)
            vm.clearFeedback()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mon cycle") },
                actions = {
                    IconButton(onClick = onOpenAdvisor) {
                        Icon(Icons.Rounded.Forum, contentDescription = "Mon conseiller")
                    }
                    IconButton(onClick = onOpenLessons) {
                        Icon(Icons.Rounded.MenuBook, contentDescription = "Leçons")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = FloatingNavBarSpace),
        ) {
            item {
                val insight = state.insight
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(heroBrush())
                            .padding(20.dp)
                    ) {
                        /*
                         * Pendant une grossesse, l'anneau compte les semaines
                         * d'aménorrhée sur les 40 du terme, pas les jours d'un cycle
                         * qui n'a plus cours. Une unité = une semaine.
                         */
                        val expecting = pregnancy.progress?.takeIf { !pregnancy.isPostpartum }
                        CycleRing(
                            dayOfCycle = expecting?.weeks ?: insight?.dayOfCycle ?: 0,
                            cycleLength = if (expecting != null) 40
                            else insight?.averageCycleLength ?: 28,
                            segments = when {
                                expecting != null -> trimesterSegments()
                                insight?.lastPeriodStart != null -> cycleSegments(
                                    periodLength = insight.averagePeriodLength,
                                    ovulationDay = insight.ovulationDayOfCycle,
                                    cycleLength = insight.averageCycleLength,
                                )
                                else -> emptyList()
                            },
                            size = 216.dp,
                            thickness = 18.dp,
                            // Passé la longueur moyenne, l'anneau est plein et le
                            // nombre continue de monter : il faut dire pourquoi,
                            // sinon « jour 37 » sur un cycle de 28 est incompréhensible.
                            label = when {
                                expecting != null -> "SEMAINES (SA) · ${expecting.trimester}ᵉ TRIMESTRE"
                                insight?.isLate == true -> "JOUR DU CYCLE · EN RETARD"
                                else -> "JOUR DU CYCLE"
                            },
                            contentColor = onHeroColor,
                            progressColor = onHeroColor,
                            trackColor = onHeroColor.copy(alpha = 0.16f),
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )

                        Spacer(Modifier.height(16.dp))

                        Text(
                            expecting?.label ?: insight?.phase?.label ?: "À découvrir",
                            style = MaterialTheme.typography.headlineSmall,
                            color = onHeroColor,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            when {
                                expecting != null ->
                                    "Terme prévu le ${expecting.dueDate.format(dayMonth)}" +
                                        if (expecting.daysUntilDue > 0)
                                            " · dans ${expecting.daysUntilDue} jours" else ""
                                else -> insight?.summaryLine()
                                    ?: "Marque tes premières règles pour démarrer"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = onHeroColor,
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            when {
                                pregnancy.nextCare != null -> pregnancy.nextCare!!.headline
                                expecting != null -> "Suivi à jour — rien à faire pour l'instant"
                                else -> insight?.phase?.hint ?: CyclePhase.INCONNUE.hint
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = onHeroColor.copy(alpha = 0.92f),
                        )

                        if (expecting != null) {
                            Spacer(Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                StatPill(
                                    label = "Semaines de grossesse",
                                    value = "${expecting.weeksOfPregnancy} sem.",
                                    modifier = Modifier.weight(1f),
                                )
                                StatPill(
                                    label = "Terme prévu",
                                    value = expecting.dueDate.format(dayMonth),
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        } else if (insight?.nextPeriodStart != null) {
                            Spacer(Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                /*
                                 * Une date déjà passée sous l'étiquette « prochaines
                                 * règles » n'a aucun sens : c'est ce que l'écran
                                 * affichait dès que la prévision était dépassée. Quand
                                 * la date est derrière nous, la tuile parle de retard.
                                 */
                                val expected = insight.expectedPeriodStart
                                StatPill(
                                    label = when {
                                        insight.isLate -> "Règles attendues le ${expected?.format(dayMonth)}"
                                        expected == today -> "Règles attendues"
                                        else -> "Prochaines règles"
                                    },
                                    value = when {
                                        insight.isLate -> "Retard de ${insight.daysLate} j"
                                        expected == today -> "Aujourd'hui"
                                        else -> insight.nextPeriodStart.format(dayMonth)
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                                val ovulation = insight.ovulationDate
                                StatPill(
                                    label = if (ovulation != null && ovulation.isBefore(today))
                                        "Ovulation passée" else "Ovulation estimée",
                                    value = ovulation?.format(dayMonth) ?: "—",
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                StatPill(
                                    // « 37 j » sorti de nulle part est déroutant : on dit
                                    // d'où vient le chiffre tant qu'il ne repose que sur
                                    // les deux dates saisies à la configuration.
                                    label = if (insight.isEstimate) "Cycle (1 seul écart connu)"
                                    else "Cycle moyen",
                                    value = "${insight.averageCycleLength} j",
                                    modifier = Modifier.weight(1f),
                                )
                                StatPill(
                                    label = "Régularité",
                                    value = when {
                                        insight.isEstimate -> "À affiner"
                                        insight.regularityDays <= 2 -> "Régulier"
                                        insight.regularityDays <= 5 -> "Variable"
                                        else -> "Irrégulier"
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }

            pregnancy.postpartum?.takeIf { pregnancy.isPostpartum }?.let { after ->
                item {
                    Card(
                        onClick = onOpenPregnancy,
                        colors = CardDefaults.cardColors(
                            containerColor = if (pregnancy.mama.protected)
                                MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                    ) {
                        val onCard = if (pregnancy.mama.protected)
                            MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onErrorContainer

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(16.dp),
                        ) {
                            Text("🤱", style = MaterialTheme.typography.headlineMedium)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    if (after.monthsSince < 1) "${after.weeksSince} semaines depuis la naissance"
                                    else "${after.monthsSince} mois depuis la naissance",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = onCard,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    pregnancy.nextCare?.headline ?: if (pregnancy.mama.protected)
                                        "L'allaitement te protège — vérifie les 3 conditions"
                                    else "L'allaitement ne te protège plus d'une grossesse",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = onCard.copy(alpha = 0.9f),
                                )
                            }
                            Icon(
                                Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = onCard,
                            )
                        }
                    }
                }
            }

            pregnancy.progress?.takeIf { !pregnancy.isPostpartum }?.let { progress ->
                item {
                    Card(
                        onClick = onOpenPregnancy,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(16.dp),
                        ) {
                            Text("🤰", style = MaterialTheme.typography.headlineMedium)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Suivi de grossesse · ${progress.label}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    pregnancy.nextCare?.headline
                                        ?: "Terme prévu le ${progress.dueDate.format(dayMonth)} · " +
                                        "${progress.trimester}ᵉ trimestre",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                                )
                            }
                            Icon(
                                Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
            }

            state.insight?.takeIf { it.isStale }?.let {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                    ) {
                        Text(
                            "Plus d'un cycle sans rien noter : les prévisions ci-dessous " +
                                "sont recalées automatiquement et ne valent pas grand-chose. " +
                                "Enregistre tes dernières règles pour repartir juste.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }

            item {
                Card(
                    onClick = onOpenAdvisor,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp),
                    ) {
                        Text("💬", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Poser une question à mon conseiller",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "Il connaît tes dates et ton suivi, et répond sans " +
                                    "rien envoyer sur Internet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(
                            Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Le suivi des règles vient avant tout le reste : confirmer ou clore
            // une période, c'est ce qui garde le reste de l'écran juste.
            state.insight?.let { insight ->
                if (insight.lastPeriodStart != null && pregnancy.ongoing == null) {
                    item {
                        PeriodTrackerCard(
                            insight = insight,
                            onConfirmStart = vm::confirmPeriodStarted,
                            onEndPeriod = vm::endPeriod,
                            onReopen = vm::reopenPeriod,
                            onOfferTest = { testSheetOpen = true },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        )
                    }
                }
            }

            /*
             * La bannière du jour. Elle reprend mot pour mot l'échelle du calendrier
             * — même couleur, même intitulé, même conseil — pour qu'une case rouge
             * vue dans la grille et l'avertissement du jour soient la même chose.
             */
            val insight = state.insight
            if (insight?.lastPeriodStart != null && pregnancy.ongoing == null) {
                item {
                    val risk = state.predictions[today]?.pregnancyRisk ?: PregnancyRisk.INCONNU
                    val color = riskColor(risk)
                    val alarming = risk == PregnancyRisk.MAXIMAL || risk == PregnancyRisk.ELEVE

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (alarming) color else color.copy(alpha = 0.16f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                    ) {
                        val onCard =
                            if (alarming) Color.White else MaterialTheme.colorScheme.onSurface

                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.padding(16.dp),
                        ) {
                            Text(
                                when (risk) {
                                    PregnancyRisk.MAXIMAL -> "🚨"
                                    PregnancyRisk.ELEVE -> "⚠️"
                                    PregnancyRisk.MODERE -> "🟡"
                                    PregnancyRisk.REGLES -> "🩸"
                                    else -> "🛡️"
                                },
                                style = MaterialTheme.typography.headlineMedium,
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Aujourd'hui · ${risk.label}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = onCard,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    risk.advice,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (alarming) onCard else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            // Bouton vers les leçons de prévention
            if (pregnancy.ongoing == null) item {
                Card(
                    onClick = onOpenLessons,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp),
                    ) {
                        Text("💡", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Guide pratique : ce qui marche vraiment",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "Chiffres d'efficacité, pilule d'urgence, préservatif sans tabou.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                            )
                        }
                        Icon(
                            Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Tant qu'aucune règle n'est enregistrée, l'assistant est le chemin le plus
            // simple : il pose des questions concrètes au lieu de demander des chiffres.
            if (state.insight?.lastPeriodStart == null) {
                item {
                    Card(
                        onClick = onOpenSetup,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(16.dp),
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Je ne connais pas mon cycle",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "Réponds à trois questions, je calcule tout.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                                )
                            }
                            Icon(
                                Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }
            }

            if (state.insight?.currentPeriodStart == null) item {
                FilledTonalButton(
                    onClick = { vm.togglePeriodDay(today) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    Icon(Icons.Rounded.WaterDrop, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (state.predictions[today]?.isLoggedPeriod == true) "Retirer les règles d'aujourd'hui"
                        else "Mes règles ont commencé aujourd'hui"
                    )
                }
            }

            item {
                val expecting = pregnancy.progress?.takeIf { !pregnancy.isPostpartum }
                Spacer(Modifier.height(12.dp))
                Column(Modifier.padding(horizontal = 20.dp)) {
                    val birth = pregnancy.ongoing?.birthDate?.takeIf { pregnancy.isPostpartum }
                    Text(
                        when {
                            expecting != null -> "Mon calendrier de grossesse"
                            birth != null -> "Mon calendrier après la naissance"
                            else -> "Mon calendrier des risques"
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        when {
                            expecting != null ->
                                "Les trois trimestres et tes rendez-vous — CPN, " +
                                    "vaccins, doses de TPIg. Touche un jour pour voir " +
                                    "ce qui est prévu et le cocher."
                            birth != null ->
                                "Les visites postnatales et les vaccins de ton bébé. " +
                                    "Touche un jour pour voir ce qui est prévu et le " +
                                    "cocher."
                            else ->
                                "Plus la case est rouge, plus une grossesse est " +
                                    "probable ce jour-là. Touche un jour pour savoir " +
                                    "quoi en faire."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(8.dp))
                MonthCalendar(
                    month = month,
                    onMonthChange = vm::setMonth,
                    onDayClick = { selectedDay = it },
                    modifier = Modifier.padding(horizontal = 12.dp),
                ) { date, inMonth ->
                    val hasLog = state.logs[date]
                        ?.let { it.mood > 0 || it.symptoms.isNotBlank() } == true
                    if (expecting != null) {
                        val dayActs = careSchedule[date].orEmpty()
                        PregnancyDayCell(
                            date = date,
                            inMonth = inMonth,
                            progress = expecting,
                            hasLog = hasLog,
                            acts = dayActs,
                            allDone = dayActs.isNotEmpty() &&
                                dayActs.all { pregnancy.care.containsKey(it.code) },
                        )
                    } else {
                        val birth = pregnancy.ongoing?.birthDate?.takeIf { pregnancy.isPostpartum }
                        val dayActs = careSchedule[date].orEmpty()
                        if (birth != null) {
                            PostpartumDayCell(
                                date = date,
                                inMonth = inMonth,
                                birthDate = birth,
                                hasLog = hasLog,
                                acts = dayActs,
                                allDone = dayActs.isNotEmpty() &&
                                    dayActs.all { pregnancy.care.containsKey(it.code) },
                            )
                        } else {
                            CycleDayCell(
                                date = date,
                                inMonth = inMonth,
                                prediction = state.predictions[date],
                                hasLog = hasLog,
                                isExpectedStart = date == state.insight?.expectedPeriodStart,
                            )
                        }
                    }
                }
            }

            item {
                val expecting = pregnancy.progress?.takeIf { !pregnancy.isPostpartum }
                val birth = pregnancy.ongoing?.birthDate?.takeIf { pregnancy.isPostpartum }
                when {
                    expecting != null -> PregnancyLegend(
                        progress = expecting,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    )
                    birth != null -> PostpartumLegend(
                        birthDate = birth,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    )
                    else -> Legend(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                    )
                }
            }

            item {
                Text(
                    if (pregnancy.ongoing != null) {
                        PREGNANCY_DISCLAIMER
                    } else {
                        "Ces prévisions sont indicatives. Ce n'est ni un avis médical, " +
                            "ni un moyen de contraception. $FERTILITY_DISCLAIMER"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 24.dp),
                )
            }
        }
    }

    if (testSheetOpen) {
        PregnancyTestSheet(
            daysLate = state.insight?.daysLate ?: 0,
            onResult = { result ->
                testSheetOpen = false
                pregnancyVm.recordTest(result)
            },
            onDismiss = { testSheetOpen = false },
        )
    }

    // Ce qu'on affiche dépend du résultat : un positif ouvre le suivi, un négatif
    // explique pourquoi le retard peut persister quand même.
    when (justRecorded) {
        TestResult.POSITIVE -> LaunchedEffect(Unit) {
            pregnancyVm.clearJustRecorded()
            onOpenPregnancy()
        }
        TestResult.NEGATIVE -> NegativeResultSheet(
            daysLate = state.insight?.daysLate ?: 0,
            onDismiss = pregnancyVm::clearJustRecorded,
        )
        TestResult.UNCLEAR, null -> Unit
    }

    selectedDay?.let { day ->
        val expecting = pregnancy.progress?.takeIf { !pregnancy.isPostpartum }
        if (expecting != null) {
            PregnancyDaySheet(
                date = day,
                progress = expecting,
                acts = careSchedule[day].orEmpty(),
                doneCodes = pregnancy.care.keys,
                onToggleAct = pregnancyVm::setCareDone,
                onWriteLog = {
                    selectedDay = null
                    onOpenDayLog(day)
                },
                onOpenPregnancy = {
                    selectedDay = null
                    onOpenPregnancy()
                },
                onDismiss = { selectedDay = null },
            )
            return@let
        }

        val birth = pregnancy.ongoing?.birthDate?.takeIf { pregnancy.isPostpartum }
        if (birth != null) {
            PostpartumDaySheet(
                date = day,
                birthDate = birth,
                acts = careSchedule[day].orEmpty(),
                doneCodes = pregnancy.care.keys,
                onToggleAct = pregnancyVm::setCareDone,
                onWriteLog = {
                    selectedDay = null
                    onOpenDayLog(day)
                },
                onOpenPregnancy = {
                    selectedDay = null
                    onOpenPregnancy()
                },
                onDismiss = { selectedDay = null },
            )
            return@let
        }
        DayRiskSheet(
            date = day,
            prediction = state.predictions[day],
            onConfirmPeriod = {
                selectedDay = null
                vm.confirmPeriodStarted(day)
            },
            onWriteLog = {
                selectedDay = null
                onOpenDayLog(day)
            },
            onLearnMore = {
                selectedDay = null
                onOpenLessons()
            },
            onDismiss = { selectedDay = null },
        )
    }
}

/**
 * La fiche d'un jour : ce qu'il vaut en termes de risque, et quoi faire.
 *
 * C'est le cœur du calendrier pour le public de l'app. Une case colorée ne veut
 * rien dire toute seule ; ici on nomme le risque, on donne le conseil, et on laisse
 * deux portes de sortie — noter sa journée, ou aller lire.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayRiskSheet(
    date: LocalDate,
    prediction: DayPrediction?,
    onConfirmPeriod: () -> Unit,
    onWriteLog: () -> Unit,
    onLearnMore: () -> Unit,
    onDismiss: () -> Unit,
) {
    val risk = prediction?.pregnancyRisk ?: PregnancyRisk.INCONNU
    val color = riskColor(risk)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                date.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH))
                    .replaceFirstChar { it.titlecase(Locale.FRENCH) },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(Modifier.width(10.dp))
                Text(risk.label, style = MaterialTheme.typography.headlineSmall)
            }

            if (prediction?.fertileDuringPeriod == true) {
                Spacer(Modifier.height(10.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "⚠️  Ce jour de règles tombe aussi dans ta fenêtre fertile estimée. " +
                            "Ça arrive sur les cycles courts, et c'est justement le moment " +
                            "où on croit ne rien risquer.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(14.dp),
                    )
                }
            }

            if (prediction != null && prediction.dayOfCycle > 0) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Jour ${prediction.dayOfCycle} du cycle · ${prediction.phase.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }

            Spacer(Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.14f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        risk.advice,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        risk.protection,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Confirmer depuis le calendrier : on se souvient souvent du jour exact
            // une fois la grille sous les yeux, pas au moment où l'app le demande.
            if (prediction?.isLoggedPeriod != true && !date.isAfter(LocalDate.now())) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onConfirmPeriod,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.WaterDrop, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Mes règles ont commencé ce jour-là")
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(onClick = onWriteLog, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Edit, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Noter ce jour")
                }
                OutlinedButton(onClick = onLearnMore, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.MenuBook, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("En savoir plus")
                }
            }
        }
    }
}

/** La couleur d'un niveau de risque. Les règles gardent leur rose, c'est leur repère. */
@Composable
private fun riskColor(risk: PregnancyRisk): Color = when (risk) {
    PregnancyRisk.MAXIMAL -> RiskColors.maximal
    PregnancyRisk.ELEVE -> RiskColors.eleve
    PregnancyRisk.MODERE -> RiskColors.modere
    PregnancyRisk.FAIBLE -> RiskColors.faible
    PregnancyRisk.REGLES -> CycleColors.period
    PregnancyRisk.INCONNU -> MaterialTheme.colorScheme.outlineVariant
}

/** Petite tuile de statistique posée sur le fond sombre de la carte héros. */
@Composable
private fun StatPill(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .background(onHeroColor.copy(alpha = 0.10f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(label, style = OvertitleStyle, color = onHeroColor.copy(alpha = 0.85f))
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, color = onHeroColor)
    }
}

@Composable
private fun CycleDayCell(
    date: LocalDate,
    inMonth: Boolean,
    prediction: DayPrediction?,
    hasLog: Boolean,
    isExpectedStart: Boolean = false,
) {
    val isToday = date == LocalDate.now()
    val risk = prediction?.pregnancyRisk ?: PregnancyRisk.INCONNU

    /*
     * Les jours à risque sont peints en plein, les autres à peine teintés : au
     * premier coup d'œil, on doit voir la zone rouge du mois et rien d'autre.
     */
    val fill = when (risk) {
        PregnancyRisk.MAXIMAL -> RiskColors.maximal
        PregnancyRisk.ELEVE -> RiskColors.eleve.copy(alpha = 0.85f)
        PregnancyRisk.MODERE -> RiskColors.modere.copy(alpha = 0.55f)
        PregnancyRisk.REGLES ->
            if (prediction?.isLoggedPeriod == true) CycleColors.period
            else CycleColors.predicted.copy(alpha = 0.45f)
        PregnancyRisk.FAIBLE -> RiskColors.faible.copy(alpha = 0.12f)
        PregnancyRisk.INCONNU -> Color.Transparent
    }

    // Texte blanc dès que le fond est franc, sinon la couleur normale du thème.
    val solidBackground = risk == PregnancyRisk.MAXIMAL ||
        risk == PregnancyRisk.ELEVE ||
        prediction?.isLoggedPeriod == true
    val onFill = when {
        solidBackground -> Color.White
        !inMonth -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        Modifier
            .fillMaxSize()
            .clip(CircleShape)
            // Les jours des mois voisins sont atténués. Multiplier l'alpha (plutôt que
            // de le fixer) garde les cellules sans donnée réellement transparentes.
            .background(if (inMonth) fill else fill.copy(alpha = fill.alpha * 0.35f))
            .then(
                when {
                    isToday -> Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    // Le jour 1 attendu des prochaines règles : un contour, pas un
                    // remplissage — c'est une prévision, pas un fait.
                    isExpectedStart -> Modifier.border(2.dp, CycleColors.period, CircleShape)
                    else -> Modifier
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = onFill,
            )
            // Le symbole double la couleur : daltonisme, écran en plein soleil,
            // capture d'écran en noir et blanc — l'avertissement passe quand même.
            val symbol = risk.symbol
            if (symbol.isNotEmpty() && inMonth) {
                Text(
                    symbol,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = onFill,
                )
            } else if (hasLog && inMonth) {
                Spacer(Modifier.height(1.dp))
                Box(
                    Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(onFill.copy(alpha = 0.7f))
                )
            }
        }
    }
}

@Composable
private fun Legend(modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LegendRow(
            RiskColors.maximal,
            "!!",
            "Risque maximal",
            "Ovulation — préservatif indispensable",
        )
        LegendRow(
            RiskColors.eleve.copy(alpha = 0.85f),
            "!",
            "Risque élevé",
            "Fenêtre fertile — préservatif à chaque rapport",
        )
        LegendRow(
            RiskColors.modere.copy(alpha = 0.55f),
            "·",
            "Risque modéré",
            "La fenêtre peut se décaler : prudence",
        )
        LegendRow(
            CycleColors.period,
            "",
            "Règles enregistrées",
            "Ce que tu as confirmé — prioritaire sur toute prévision",
        )
        LegendRow(
            CycleColors.predicted.copy(alpha = 0.45f),
            "",
            "Règles prévues",
            "Estimation d'après tes cycles passés",
        )
        LegendRow(
            Color.Transparent,
            "",
            "Jour 1 attendu (contour rose)",
            "À confirmer quand elles arrivent, pour garder le calcul juste",
            outline = CycleColors.period,
        )
        LegendRow(
            RiskColors.faible.copy(alpha = 0.35f),
            "",
            "Risque plus faible",
            "Aucun jour du cycle n'est à zéro",
        )
    }
}

@Composable
private fun LegendRow(
    color: Color,
    symbol: String,
    label: String,
    hint: String,
    outline: Color? = null,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(color)
                .then(if (outline != null) Modifier.border(2.dp, outline, CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            if (symbol.isNotEmpty()) {
                Text(
                    symbol,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private val dayMonth = DateTimeFormatter.ofPattern("d MMM", Locale.FRENCH)

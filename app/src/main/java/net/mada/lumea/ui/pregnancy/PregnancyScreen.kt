package net.mada.lumea.ui.pregnancy

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import net.mada.lumea.domain.pregnancy.FOOD_AVOID
import net.mada.lumea.domain.pregnancy.FOOD_RECOMMENDED
import net.mada.lumea.domain.pregnancy.OPTIONS_AFTER_POSITIVE
import net.mada.lumea.domain.pregnancy.PREGNANCY_DISCLAIMER
import net.mada.lumea.domain.pregnancy.PREGNANCY_MILESTONES
import net.mada.lumea.domain.pregnancy.PostpartumProgress
import net.mada.lumea.domain.pregnancy.PregnancyProgress
import net.mada.lumea.domain.pregnancy.WARNING_SIGNS
import net.mada.lumea.domain.pregnancy.CareStatus
import net.mada.lumea.domain.pregnancy.headline
import net.mada.lumea.ui.components.FloatingNavBarSpace
import net.mada.lumea.ui.containerViewModel
import net.mada.lumea.ui.theme.heroBrush
import net.mada.lumea.ui.theme.onHeroColor
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Le suivi de grossesse : où on en est, ce qui arrive, quoi manger, quand
 * s'inquiéter, et quelles options existent.
 *
 * Deux partis pris. D'abord, tout est présenté comme de l'information, jamais
 * comme une consigne : le suivi médical se fait chez un soignant, pas dans une
 * application. Ensuite, les options après un test positif sont toutes là, au même
 * niveau, sans que l'app en pousse une — ce n'est pas sa décision.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PregnancyScreen(onBack: () -> Unit, onOpenEmergency: () -> Unit = {}) {
    val vm = containerViewModel {
        PregnancyViewModel(it.pregnancy, it.cycle, it.events, it.settings)
    }
    val context = androidx.compose.ui.platform.LocalContext.current
    var pdfResult by remember { mutableStateOf<Boolean?>(null) }
    val pdfLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri -> if (uri != null) vm.exportPdf(context, uri) { pdfResult = it } }
    val state by vm.state.collectAsStateWithLifecycle()
    var menuOpen by remember { mutableStateOf(false) }
    var confirmEnd by remember { mutableStateOf(false) }
    var scheduled by remember { mutableStateOf<Int?>(null) }
    var confirmBirth by remember { mutableStateOf(false) }
    var confirmReturn by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isPostpartum) "Après la naissance" else "Ma grossesse") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenEmergency) {
                        Icon(
                            Icons.Rounded.MedicalServices,
                            contentDescription = "Fiche d'urgence",
                        )
                    }
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "Plus d'options")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        if (state.isPostpartum) {
                            DropdownMenuItem(
                                text = { Text("Mes règles sont revenues") },
                                onClick = {
                                    menuOpen = false
                                    confirmReturn = true
                                },
                            )
                        } else if (state.ongoing != null) {
                            DropdownMenuItem(
                                text = { Text("Mon bébé est né") },
                                onClick = {
                                    menuOpen = false
                                    confirmBirth = true
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Exporter mon carnet en PDF") },
                            onClick = {
                                menuOpen = false
                                pdfLauncher.launch(vm.suggestedPdfName())
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Fermer ce suivi") },
                            onClick = {
                                menuOpen = false
                                confirmEnd = true
                            },
                        )
                    }
                },
            )
        },
    ) { padding ->
        val postpartum = state.postpartum
        if (state.isPostpartum && postpartum != null) {
            PostpartumContent(
                progress = postpartum,
                state = state,
                vm = vm,
                padding = padding,
            )
            PostpartumDialogs(
                confirmReturn = confirmReturn,
                onDismissReturn = { confirmReturn = false },
                onConfirmReturn = {
                    vm.recordPeriodReturn(java.time.LocalDate.now())
                    confirmReturn = false
                    onBack()
                },
                confirmBirth = false,
                onDismissBirth = {},
                onConfirmBirth = {},
            )
            return@Scaffold
        }

        val progress = state.progress

        if (progress == null) {
            Box(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Aucun suivi en cours. Un test positif enregistré depuis l'écran " +
                        "Cycle ouvre le suivi.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = FloatingNavBarSpace),
        ) {
            item { ProgressHero(progress) }

            item {
                Card(
                    onClick = onOpenEmergency,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp),
                    ) {
                        Text("🚑", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Ma fiche d'urgence", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Groupe sanguin, allergies, qui appeler — tout sur un " +
                                    "écran, à montrer au soignant.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            if (!progress.isPlausible) {
                item {
                    Banner(
                        container = MaterialTheme.colorScheme.errorContainer,
                        content = MaterialTheme.colorScheme.onErrorContainer,
                        text = "Le calcul dépasse 42 semaines : la date de départ est " +
                            "probablement fausse, ou le suivi aurait dû être fermé. " +
                            "Vérifie avec un soignant.",
                    )
                }
            }

            item {
                // Ce qui reste à faire d'après le carnet, et non l'étape théorique
                // de la semaine en cours : cocher une CPN doit se voir ici.
                val next = state.nextCare
                Section("Où tu en es du suivi") {
                    if (next == null) {
                        Text(
                            "Tout est coché.",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Rien à faire pour l'instant. Le prochain rendez-vous " +
                                "apparaîtra ici quand son moment sera venu.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        Text(
                            next.headline,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (next.status == CareStatus.EN_RETARD)
                                MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Vers le ${next.date.format(longDate)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(next.act.why, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            state.ongoing?.let { pregnancy ->
                item {
                    Section("Mon carnet de suivi") {
                        Text(
                            "Coche au fur et à mesure. Une CPN ratée ne se rattrape " +
                                "pas, et personne ne retient de tête qu'il faut une " +
                                "troisième dose de TPIg.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        CareChecklist(
                            currentWeek = progress.weeks,
                            care = state.care,
                            onToggle = vm::setCareDone,
                        )
                        Spacer(Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = {
                                vm.scheduleCareInAgenda { count ->
                                    scheduled = count
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                Icons.Rounded.NotificationsActive,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Programmer les rappels dans l'agenda")
                        }
                    }
                }

                item {
                    Section("Ma sage-femme") {
                        CaregiverSection(
                            pregnancy = pregnancy,
                            onSave = vm::setCaregiver,
                        )
                    }
                }

                item {
                    Section("Ma situation") {
                        RiskFactorSection(
                            selected = state.riskCodes,
                            onToggle = vm::toggleRiskFactor,
                        )
                    }
                }

                item {
                    Section("Mes résultats") {
                        ResultsSection(care = state.care, onSaveNote = vm::setCareNote)
                    }
                }
            }

            item {
                Section("Le calendrier complet") {
                    PREGNANCY_MILESTONES.forEach { step ->
                        val done = progress.weeks > step.toWeek
                        val current = progress.weeks in step.fromWeek..step.toWeek
                        Row(Modifier.padding(vertical = 6.dp)) {
                            Box(
                                Modifier
                                    .padding(top = 5.dp)
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            current -> MaterialTheme.colorScheme.primary
                                            done -> MaterialTheme.colorScheme.outlineVariant
                                            else -> MaterialTheme.colorScheme.surfaceContainerHigh
                                        }
                                    )
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                step.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
                                color = if (done) MaterialTheme.colorScheme.outline
                                else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }

            item {
                Section("Ce qui aide, côté assiette") {
                    FOOD_RECOMMENDED.forEach { InfoBlock(it.title, it.body) }
                }
            }

            item {
                Section("Ce qu'il vaut mieux éviter") {
                    FOOD_AVOID.forEach { InfoBlock(it.title, it.body) }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Consulter sans attendre",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "La plupart du temps ce n'est rien. Mais ce sont les " +
                                "situations où on hésite à déranger, et où il ne faut pas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Spacer(Modifier.height(10.dp))
                        WARNING_SIGNS.forEach { sign ->
                            Column(Modifier.padding(vertical = 5.dp)) {
                                Text(
                                    "• ${sign.sign}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                                Text(
                                    sign.what,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "En cas d'urgence : le 15 (SAMU), ou la maternité la plus " +
                                "proche. Le 112 fonctionne dans toute l'Europe.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }

            item {
                Section("Tes options, toutes") {
                    Text(
                        "Un test positif n'est pas forcément une bonne nouvelle, et " +
                            "ce n'est pas à une application d'en décider. Voici ce qui " +
                            "existe, sans ordre de préférence.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    OPTIONS_AFTER_POSITIVE.forEach { (title, body) -> InfoBlock(title, body) }
                }
            }

            item {
                Text(
                    PREGNANCY_DISCLAIMER,
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

    pdfResult?.let { success ->
        AlertDialog(
            onDismissRequest = { pdfResult = null },
            title = { Text(if (success) "Carnet exporté" else "Échec de l'export") },
            text = {
                Text(
                    if (success) {
                        "Le PDF reprend tes rendez-vous, tes vaccins, tes doses de TPIg " +
                            "et les résultats que tu as notés — rien de ton journal.\n\n" +
                            "Tu peux le montrer en consultation ou l'imprimer. Il n'a " +
                            "aucune valeur officielle et ne remplace pas ton carnet de " +
                            "santé mère-enfant."
                    } else {
                        "Le fichier n'a pas pu être écrit. Vérifie l'emplacement choisi " +
                            "et réessaie."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = { pdfResult = null }) { Text("D'accord") }
            },
        )
    }

    scheduled?.let { count ->
        AlertDialog(
            onDismissRequest = { scheduled = null },
            title = { Text(if (count > 0) "Rappels programmés" else "Rien à programmer") },
            text = {
                Text(
                    if (count > 0) {
                        "$count rendez-vous ont été ajoutés à ton agenda, avec une " +
                            "notification la veille. Tu peux les modifier ou les " +
                            "supprimer depuis l'agenda."
                    } else {
                        "Tous les rendez-vous à venir sont déjà cochés, ou leur date " +
                            "est passée. Rien n'a été ajouté."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = { scheduled = null }) { Text("D'accord") }
            },
        )
    }

    PostpartumDialogs(
        confirmReturn = false,
        onDismissReturn = {},
        onConfirmReturn = {},
        confirmBirth = confirmBirth,
        onDismissBirth = { confirmBirth = false },
        onConfirmBirth = {
            vm.recordBirth(java.time.LocalDate.now())
            confirmBirth = false
        },
    )

    if (confirmEnd) {
        AlertDialog(
            onDismissRequest = { confirmEnd = false },
            title = { Text("Fermer ce suivi ?") },
            text = {
                Text(
                    "Le suivi disparaîtra de l'écran Cycle et le suivi de tes règles " +
                        "reprendra. L'app ne demande pas pourquoi, et n'affichera rien " +
                        "de plus à ce sujet."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.endFollowUp()
                    confirmEnd = false
                    onBack()
                }) { Text("Fermer le suivi") }
            },
            dismissButton = {
                TextButton(onClick = { confirmEnd = false }) { Text("Annuler") }
            },
        )
    }
}

@Composable
private fun ProgressHero(progress: PregnancyProgress) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(MaterialTheme.shapes.large)
            .background(heroBrush())
            .padding(24.dp),
    ) {
        Text(
            progress.label,
            style = MaterialTheme.typography.displaySmall,
            color = onHeroColor,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            "soit environ ${progress.weeksOfPregnancy} semaines de grossesse · " +
                "${progress.trimester}ᵉ trimestre",
            style = MaterialTheme.typography.bodyMedium,
            color = onHeroColor.copy(alpha = 0.85f),
        )

        Spacer(Modifier.height(18.dp))
        LinearProgressIndicator(
            progress = { (progress.weeks / 40f).coerceIn(0f, 1f) },
            color = onHeroColor,
            trackColor = onHeroColor.copy(alpha = 0.2f),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            HeroStat("Terme prévu", progress.dueDate.format(longDate))
            HeroStat(
                "Il reste",
                if (progress.daysUntilDue > 0) "${progress.daysUntilDue} jours" else "—",
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Les semaines d'aménorrhée (SA) se comptent depuis le premier jour de tes " +
                "dernières règles, pas depuis la conception : c'est la convention " +
                "médicale, et c'est pour ça qu'il y a deux semaines d'écart.",
            style = MaterialTheme.typography.bodySmall,
            color = onHeroColor.copy(alpha = 0.8f),
        )
    }
}

@Composable
private fun HeroStat(label: String, value: String) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = onHeroColor.copy(alpha = 0.7f),
        )
        Text(value, style = MaterialTheme.typography.titleMedium, color = onHeroColor)
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun Banner(container: Color, content: Color, text: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = container),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = content,
            modifier = Modifier.padding(16.dp),
        )
    }
}

private val longDate = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH)

/**
 * L'écran après la naissance.
 *
 * Le suivi ne s'arrête pas à l'accouchement : c'est justement là que se pose la
 * question de la contraception par allaitement, et que se concentrent les décès
 * maternels et néonatals évitables. On y reste jusqu'au retour des règles, qui
 * referme la boucle et rend la main au suivi de cycle.
 */
@Composable
private fun PostpartumContent(
    progress: PostpartumProgress,
    state: PregnancyUiState,
    vm: PregnancyViewModel,
    padding: androidx.compose.foundation.layout.PaddingValues,
) {
    LazyColumn(
        modifier = Modifier
            .padding(padding)
            .fillMaxSize(),
        contentPadding = PaddingValues(bottom = FloatingNavBarSpace),
    ) {
        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(heroBrush())
                    .padding(24.dp),
            ) {
                Text(
                    when {
                        progress.daysSince < 14 -> "${progress.daysSince} jours"
                        progress.monthsSince < 1 -> "${progress.weeksSince} semaines"
                        else -> "${progress.monthsSince} mois"
                    },
                    style = MaterialTheme.typography.displaySmall,
                    color = onHeroColor,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "depuis la naissance, le ${progress.birthDate.format(longDate)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = onHeroColor.copy(alpha = 0.85f),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    if (state.mama.protected) {
                        "L'allaitement te protège actuellement d'une grossesse — à " +
                            "condition que les trois règles tiennent. Vérifie-les " +
                            "ci-dessous."
                    } else {
                        "L'allaitement ne te protège plus d'une grossesse. " +
                            "Vois ci-dessous ce qui manque."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = onHeroColor,
                )
            }
        }

        if (progress.inImmediatePostpartum) {
            item {
                Banner(
                    container = MaterialTheme.colorScheme.errorContainer,
                    content = MaterialTheme.colorScheme.onErrorContainer,
                    text = "Les six premières semaines sont les plus à risque pour " +
                        "toi. Saignement abondant, fièvre, pertes malodorantes, " +
                        "douleur d'un sein avec fièvre : va au CSB, ce n'est pas " +
                        "« la fatigue d'une jeune mère ».",
                )
            }
        }

        item {
            Section("Allaitement et contraception (MAMA)") {
                MamaSection(
                    status = state.mama,
                    care = state.care,
                    onToggle = vm::setCareDone,
                )
            }
        }

        item {
            Section("Mon carnet après la naissance") {
                PostpartumChecklist(
                    progress = progress,
                    care = state.care,
                    onToggle = vm::setCareDone,
                )
            }
        }

        item { Section("Prendre soin de toi") { PostpartumAdviceSections() } }

        item { Section("Contraceptions compatibles") { ContraceptionSections() } }

        state.ongoing?.let { pregnancy ->
            item {
                Section("Ma sage-femme") {
                    CaregiverSection(pregnancy = pregnancy, onSave = vm::setCaregiver)
                }
            }
        }

        item {
            Text(
                "Quand tes règles reviendront, indique-le depuis le menu en haut : " +
                    "le suivi de cycle repartira de cette date, avec ton calendrier " +
                    "et tes prévisions. " + PREGNANCY_DISCLAIMER,
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

@Composable
private fun PostpartumDialogs(
    confirmReturn: Boolean,
    onDismissReturn: () -> Unit,
    onConfirmReturn: () -> Unit,
    confirmBirth: Boolean,
    onDismissBirth: () -> Unit,
    onConfirmBirth: () -> Unit,
) {
    if (confirmBirth) {
        AlertDialog(
            onDismissRequest = onDismissBirth,
            title = { Text("Ton bébé est né ?") },
            text = {
                Text(
                    "Le suivi ne s'arrête pas là : il passe à l'après-naissance. " +
                        "Consultations postnatales, vaccins du bébé, et surtout la " +
                        "question de l'allaitement comme contraception — celle que " +
                        "presque personne n'explique correctement."
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirmBirth) { Text("Oui, aujourd'hui") }
            },
            dismissButton = {
                TextButton(onClick = onDismissBirth) { Text("Annuler") }
            },
        )
    }

    if (confirmReturn) {
        AlertDialog(
            onDismissRequest = onDismissReturn,
            title = { Text("Tes règles sont revenues ?") },
            text = {
                Text(
                    "C'est le retour de couches. Le suivi d'après-naissance se " +
                        "ferme, ces règles sont enregistrées comme premier jour d'un " +
                        "nouveau cycle, et l'écran Cycle retrouve son anneau, son " +
                        "calendrier et ses prévisions.\n\n" +
                        "Attention : l'allaitement ne te protège plus d'une grossesse " +
                        "à partir de maintenant."
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirmReturn) { Text("Oui, aujourd'hui") }
            },
            dismissButton = {
                TextButton(onClick = onDismissReturn) { Text("Annuler") }
            },
        )
    }
}

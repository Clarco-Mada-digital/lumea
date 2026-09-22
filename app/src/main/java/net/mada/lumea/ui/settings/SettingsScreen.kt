package net.mada.lumea.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import net.mada.lumea.data.prefs.CornerStyle
import net.mada.lumea.data.prefs.Palette
import net.mada.lumea.data.prefs.TextScale
import net.mada.lumea.data.prefs.ThemeMode
import net.mada.lumea.ui.containerViewModel
import net.mada.lumea.ui.theme.paletteSwatch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenCycleSetup: () -> Unit,
    onOpenAiProfile: () -> Unit = {},
) {
    val context = LocalContext.current
    val vm = containerViewModel { SettingsViewModel(it.settings, it.lock, it.backup) }
    val settings by vm.settings.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    var showPinDialog by remember { mutableStateOf(false) }
    var showJournalTime by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    // La phrase de passe est choisie avant d'ouvrir le sélecteur de fichier :
    // on la garde le temps que l'utilisatrice choisisse où écrire.
    var pendingExportPassphrase by remember { mutableStateOf<CharArray?>(null) }
    val pendingImport by vm.pendingImport.collectAsStateWithLifecycle()
    val preview by vm.preview.collectAsStateWithLifecycle()
    val restoring by vm.restoring.collectAsStateWithLifecycle()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) vm.export(uri, pendingExportPassphrase)
        pendingExportPassphrase = null
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { vm.openBackup(it) } }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            vm.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Réglages") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Retour")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Group("Profil") {
                /*
                 * Le champ garde son propre état plutôt que de lire directement
                 * `settings.displayName` : DataStore répond de façon asynchrone, et
                 * afficher la valeur qui revient de la base faisait perdre des lettres
                 * quand on tape vite. On enregistre une fois la frappe posée.
                 */
                var name by rememberSaveable(settings.displayName.isEmpty()) {
                    mutableStateOf(settings.displayName)
                }
                LaunchedEffect(name) {
                    if (name != settings.displayName) {
                        delay(350)
                        vm.setDisplayName(name)
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Ton prénom") },
                    supportingText = { Text("Utilisé pour te dire bonjour sur l'accueil.") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Group("Apparence") {
                Text("Thème", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        ThemeMode.SYSTEM to "Système",
                        ThemeMode.LIGHT to "Clair",
                        ThemeMode.DARK to "Sombre",
                    ).forEach { (mode, label) ->
                        FilterChip(
                            selected = settings.themeMode == mode,
                            onClick = { vm.setThemeMode(mode) },
                            label = { Text(label) },
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
                Text("Couleur", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Palette.entries.forEach { palette ->
                        val selected = settings.palette == palette && !settings.dynamicColor
                        Box(
                            Modifier
                                .size(if (selected) 40.dp else 36.dp)
                                .clip(CircleShape)
                                .background(paletteSwatch(palette))
                                .then(
                                    if (selected) Modifier.border(
                                        3.dp, MaterialTheme.colorScheme.onSurface, CircleShape
                                    ) else Modifier
                                )
                                .clickable {
                                    vm.setDynamicColor(false)
                                    vm.setPalette(palette)
                                }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                SwitchRow(
                    title = "Couleurs du téléphone",
                    subtitle = "Reprend la palette de ton fond d'écran (Android 12+).",
                    checked = settings.dynamicColor,
                    onChange = vm::setDynamicColor,
                )

                Spacer(Modifier.height(20.dp))
                Text("Taille du texte", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextScale.entries.forEach { scale ->
                        FilterChip(
                            selected = settings.textScale == scale,
                            onClick = { vm.setTextScale(scale) },
                            label = { Text(scale.label) },
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Aperçu : une journée douce et tranquille.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(20.dp))
                Text("Coins", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CornerStyle.entries.forEach { corner ->
                        FilterChip(
                            selected = settings.cornerStyle == corner,
                            onClick = { vm.setCornerStyle(corner) },
                            label = { Text(corner.label) },
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
                SwitchRow(
                    title = "Halos colorés",
                    subtitle = "Dégradés diffus en arrière-plan des écrans.",
                    checked = settings.showGlow,
                    onChange = vm::setShowGlow,
                )
                Spacer(Modifier.height(8.dp))
                SwitchRow(
                    title = "Animations",
                    subtitle = "Transitions entre écrans et apparition des listes.",
                    checked = settings.animationsEnabled,
                    onChange = vm::setAnimationsEnabled,
                )
                Spacer(Modifier.height(8.dp))
                SwitchRow(
                    title = "Contraste élevé",
                    subtitle = "Noir et blanc francs. Utile en plein soleil ou si la vue fatigue.",
                    checked = settings.highContrast,
                    onChange = vm::setHighContrast,
                )
            }

            Group("Mon cycle") {
                ListItem(
                    headlineContent = { Text("Configurer avec l'assistant") },
                    supportingContent = {
                        Text(
                            "Trois questions simples — tes dernières règles, leur durée — " +
                                "et je calcule ton cycle à ta place."
                        )
                    },
                    trailingContent = {
                        Icon(Icons.Rounded.ChevronRight, contentDescription = null)
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable(onClick = onOpenCycleSetup),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Ou règle les valeurs à la main ci-dessous, si tu les connais.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                Spacer(Modifier.height(12.dp))

                SwitchRow(
                    title = "Afficher l'onglet Cycle",
                    subtitle = "Décoche pour une app de notes et d'agenda seulement.",
                    checked = settings.cycleTabVisible,
                    onChange = vm::setCycleTabVisible,
                )
                Spacer(Modifier.height(8.dp))
                NumberRow(
                    label = "Durée moyenne du cycle",
                    value = settings.cycleLength,
                    unit = "jours",
                    range = 20..45,
                    onChange = vm::setCycleLength,
                )
                NumberRow(
                    label = "Durée des règles",
                    value = settings.periodLength,
                    unit = "jours",
                    range = 1..12,
                    onChange = vm::setPeriodLength,
                )
                NumberRow(
                    label = "Phase lutéale",
                    value = settings.lutealLength,
                    unit = "jours",
                    range = 9..18,
                    onChange = vm::setLutealLength,
                )
                Text(
                    "La phase lutéale sépare l'ovulation des règles suivantes. " +
                        "14 jours est la valeur courante ; ne la change que si tu la connais.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }

            Group("Rappels") {
                SwitchRow(
                    title = "Prévenir avant mes règles",
                    subtitle = "Un rappel ${settings.periodReminderDaysBefore} jour(s) avant la date prévue.",
                    checked = settings.periodReminder,
                    onChange = vm::setPeriodReminder,
                )
                if (settings.periodReminder) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (0..5).forEach { days ->
                            FilterChip(
                                selected = settings.periodReminderDaysBefore == days,
                                onClick = { vm.setPeriodReminderDaysBefore(days) },
                                label = { Text(if (days == 0) "Le jour même" else "J-$days") },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                SwitchRow(
                    title = "Prévenir avant l'ovulation",
                    subtitle = "Un rappel la veille de l'ovulation estimée.",
                    checked = settings.fertileReminder,
                    onChange = vm::setFertileReminder,
                )
                Spacer(Modifier.height(8.dp))
                SwitchRow(
                    title = "Rappel de journal",
                    subtitle = "Chaque jour à %02d:%02d.".format(
                        settings.journalReminderMinute / 60,
                        settings.journalReminderMinute % 60,
                    ),
                    checked = settings.journalReminder,
                    onChange = { enabled ->
                        vm.setJournalReminder(context, enabled, settings.journalReminderMinute)
                    },
                )
                if (settings.journalReminder) {
                    OutlinedButton(onClick = { showJournalTime = true }) { Text("Changer l'heure") }
                }
            }

            Group("Confidentialité") {
                ListItem(
                    headlineContent = { Text(if (vm.isPinSet()) "Modifier mon code" else "Protéger par un code") },
                    supportingContent = {
                        Text("4 à 6 chiffres, demandé à chaque ouverture de l'app.")
                    },
                    colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                    modifier = Modifier.clickable { showPinDialog = true },
                )
                if (vm.isPinSet()) {
                    SwitchRow(
                        title = "Empreinte digitale",
                        subtitle = "Déverrouiller sans taper le code.",
                        checked = settings.biometricEnabled,
                        onChange = vm::setBiometricEnabled,
                    )
                    TextButton(onClick = { vm.removePin() }) { Text("Supprimer le code") }
                }
                Spacer(Modifier.height(8.dp))
                SwitchRow(
                    title = "Masquer le contenu",
                    subtitle = "Bloque les captures d'écran et masque l'aperçu dans les apps " +
                        "récentes. Attention : la recopie d'écran (PC, TV, DeX) n'affichera " +
                        "plus qu'un rectangle noir.",
                    checked = settings.hideFromRecents,
                    onChange = vm::setHideFromRecents,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Tes données restent sur ce téléphone, dans une base chiffrée. " +
                        "Rien n'est envoyé sur Internet : l'app ne demande même pas la permission réseau.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Group("Widget d'écran d'accueil") {
                SwitchRow(
                    title = "Mode discret",
                    subtitle = "Le widget n'affiche qu'un point et le nom de l'app. " +
                        "Désactive-le seulement si personne d'autre ne regarde ton " +
                        "écran d'accueil : sinon il annoncerait ton jour de cycle ou " +
                        "tes semaines de grossesse à qui passe à côté.",
                    checked = settings.widgetDiscreet,
                    onChange = vm::setWidgetDiscreet,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Ajoute le widget depuis l'écran d'accueil de ton téléphone : " +
                        "appui long sur un espace vide, puis « Widgets », puis Lumea.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Group("Assistant IA") {
                OutlinedButton(
                    onClick = onOpenAiProfile,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Personnaliser mon assistant") }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Donne-lui un nom et dis-lui qui tu es : il adaptera son " +
                        "vocabulaire et ses exemples. Ce profil ne contient rien sur " +
                        "ta santé, et ne part que si tu le colles toi-même dans la " +
                        "conversation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Group("Sauvegarde") {
                OutlinedButton(
                    onClick = { showExportDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Exporter mes données") }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("*/*")) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Restaurer une sauvegarde") }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Une sauvegarde contient tout : journal, symptômes, dates de règles, " +
                        "et tes réglages (prénom, thème, rappels). " +
                        "Protège-la par une phrase de passe si elle doit quitter le téléphone. " +
                        "À la restauration, tu choisis ce que tu reprends et rien n'est écrit " +
                        "avant ta confirmation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(24.dp))
            Text(
                "Lumea 1.0 · fait avec soin",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(40.dp))
        }
    }

    if (showExportDialog) {
        ExportDialog(
            onDismiss = { showExportDialog = false },
            onExport = { passphrase ->
                pendingExportPassphrase = passphrase
                showExportDialog = false
                exportLauncher.launch(vm.suggestedFileName(encrypted = passphrase != null))
            },
        )
    }

    preview?.let { backup ->
        RestoreSheet(
            backup = backup,
            restoring = restoring,
            onRestore = { selection, mode -> vm.restore(selection, mode) },
            onDismiss = vm::clearPreview,
        )
    }

    pendingImport?.let { uri ->
        PassphrasePrompt(
            title = "Sauvegarde protégée",
            message = "Ce fichier est chiffré. Entre la phrase de passe utilisée à l'export.",
            confirmLabel = "Restaurer",
            onDismiss = vm::clearPendingImport,
            onConfirm = { passphrase -> vm.openBackup(uri, passphrase) },
        )
    }

    if (showPinDialog) {
        PinDialog(
            onDismiss = { showPinDialog = false },
            onConfirm = { pin ->
                vm.setPin(pin)
                showPinDialog = false
            },
        )
    }

    if (showJournalTime) {
        val pickerState = rememberTimePickerState(
            initialHour = settings.journalReminderMinute / 60,
            initialMinute = settings.journalReminderMinute % 60,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showJournalTime = false },
            confirmButton = {
                TextButton(onClick = {
                    vm.setJournalReminder(context, true, pickerState.hour * 60 + pickerState.minute)
                    showJournalTime = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showJournalTime = false }) { Text("Annuler") } },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(),
                ) { TimePicker(state = pickerState) }
            },
        )
    }
}

@Composable
private fun PinDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val valid = pin.length in 4..6 && pin == confirm

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Code de l'application") },
        text = {
            Column {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pin = it },
                    label = { Text("Nouveau code (4 à 6 chiffres)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) confirm = it },
                    label = { Text("Confirmer") },
                    isError = confirm.isNotEmpty() && confirm != pin,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Si tu oublies ce code, il n'y a aucun moyen de le récupérer : " +
                        "tes données resteront verrouillées.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(pin) }, enabled = valid) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

/** Choix entre une sauvegarde chiffrée (recommandé) et un export en clair. */
@Composable
private fun ExportDialog(
    onDismiss: () -> Unit,
    onExport: (CharArray?) -> Unit,
) {
    var passphrase by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var confirmPlain by remember { mutableStateOf(false) }
    val valid = passphrase.length >= 8 && passphrase == confirm

    if (confirmPlain) {
        AlertDialog(
            onDismissRequest = { confirmPlain = false },
            title = { Text("Exporter en clair ?") },
            text = {
                Text(
                    "Le fichier sera lisible par n'importe qui : ton journal intime, " +
                        "tes dates de règles, ton suivi de grossesse, tout.\n\n" +
                        "Si tu le mets dans Téléchargements, sur une carte SD ou dans " +
                        "le cloud, d'autres applications peuvent le lire. Ne fais ça " +
                        "que si tu sais exactement où ce fichier va finir."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmPlain = false
                    onExport(null)
                }) {
                    Text(
                        "J'accepte, exporter en clair",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmPlain = false }) { Text("Annuler") }
            },
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Exporter mes données") },
        text = {
            Column {
                Text(
                    "Choisis une phrase de passe. Elle sera demandée pour restaurer " +
                        "la sauvegarde, et elle seule permet de la lire.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Une sauvegarde contient tout ce que l'app sait de toi. Chiffrée, " +
                        "elle ne vaut rien pour qui la trouve.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = { Text("Phrase de passe (8 caractères minimum)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { confirm = it },
                    label = { Text("Confirmer") },
                    isError = confirm.isNotEmpty() && confirm != passphrase,
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    "Oubliée, la phrase de passe ne peut pas être retrouvée : la " +
                        "sauvegarde serait définitivement illisible.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(14.dp))
                /*
                 * L'export en clair reste possible, mais il cesse d'être une
                 * option d'un seul geste.
                 *
                 * Une sauvegarde non chiffrée contient le journal intime, les
                 * dates de règles, la grossesse, parfois un statut VIH — en JSON
                 * lisible. Posée dans le dossier Téléchargements, elle est à la
                 * portée de n'importe quelle application ayant accès au stockage.
                 * Le bouton était juste en dessous du champ, aussi facile à
                 * toucher que l'autre : il passe derrière une confirmation qui
                 * nomme ce qu'on accepte.
                 */
                TextButton(
                    onClick = { confirmPlain = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "Exporter sans protection…",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onExport(passphrase.toCharArray()) },
                enabled = valid,
            ) { Text("Exporter chiffré") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

@Composable
private fun PassphrasePrompt(
    title: String,
    message: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (CharArray) -> Unit,
) {
    var passphrase by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(message, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = { Text("Phrase de passe") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(passphrase.toCharArray()) },
                enabled = passphrase.isNotEmpty(),
            ) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Spacer(Modifier.height(16.dp))
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
    )
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun NumberRow(
    label: String,
    value: Int,
    unit: String,
    range: IntRange,
    onChange: (Int) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        TextButton(onClick = { if (value > range.first) onChange(value - 1) }) { Text("−") }
        Text("$value $unit", style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = { if (value < range.last) onChange(value + 1) }) { Text("+") }
    }
}

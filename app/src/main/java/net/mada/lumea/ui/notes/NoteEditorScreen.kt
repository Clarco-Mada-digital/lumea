package net.mada.lumea.ui.notes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.mada.lumea.ui.components.ColorPickerRow
import net.mada.lumea.ui.components.richtext.RichTextToolbar
import net.mada.lumea.ui.containerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    noteId: Long,
    onBack: () -> Unit,
) {
    val vm = containerViewModel(key = "note-$noteId") { NoteEditorViewModel(it.notes, noteId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val folders by vm.folders.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    /*
     * Le corps est tenu en TextFieldValue (et pas en simple String) : sans la
     * position du curseur, la barre de formatage insérait toujours ses marqueurs
     * au tout début de la note au lieu de les poser là où on écrit.
     */
    var body by remember { mutableStateOf(TextFieldValue()) }
    LaunchedEffect(state.body) {
        // Ne se déclenche que sur un changement venu d'ailleurs (chargement de la
        // note, bascule liste ↔ texte) : après une frappe, les deux sont déjà égaux.
        if (state.body != body.text) {
            body = body.copy(text = state.body, selection = TextRange(state.body.length))
        }
    }

    fun editBody(value: TextFieldValue) {
        body = value
        vm.setBody(value.text)
    }

    // Enregistrement automatique : on sauve en quittant, il n'y a pas de bouton « OK ».
    fun leave() {
        vm.save()
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = { leave() }) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = vm::togglePin) {
                        Icon(
                            Icons.Rounded.PushPin,
                            contentDescription = "Épingler",
                            tint = if (state.isPinned) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = vm::toggleFavorite) {
                        Icon(
                            if (state.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Favori",
                            tint = if (state.isFavorite) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = vm::toggleLock) {
                        Icon(
                            if (state.isLocked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                            contentDescription = "Protéger cette note",
                            tint = if (state.isLocked) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = vm::toggleChecklist) {
                        Icon(
                            Icons.Rounded.Checklist,
                            contentDescription = "Mode liste",
                            tint = if (state.isChecklist) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (noteId != 0L) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Supprimer")
                        }
                    }
                },
            )
        },
        /*
         * La barre de formatage est posée en bas de l'écran et remonte avec le
         * clavier. Comme Scaffold réserve la hauteur de sa bottomBar au contenu,
         * la zone de saisie rétrécit d'autant : le curseur reste visible au lieu
         * de finir caché derrière le clavier.
         */
        bottomBar = {
            if (!state.isChecklist) {
                RichTextToolbar(
                    textFieldValue = body,
                    onValueChange = { editBody(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                        .padding(horizontal = 12.dp),
                )
            }
        },
        // safeDrawing inclut le clavier : le contenu se replace tout seul.
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            TextField(
                value = state.title,
                onValueChange = vm::setTitle,
                placeholder = {
                    Text("Titre", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.outline)
                },
                textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                colors = transparentFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                item {
                    FilterChip(
                        selected = state.folderId == null,
                        onClick = { vm.setFolder(null) },
                        label = { Text("Sans dossier") },
                    )
                }
                items(folders, key = { it.id }) { folder ->
                    FilterChip(
                        selected = state.folderId == folder.id,
                        onClick = { vm.setFolder(folder.id) },
                        label = { Text("${folder.emoji} ${folder.name}") },
                    )
                }
            }

            OutlinedTextField(
                value = state.tags,
                onValueChange = vm::setTags,
                label = { Text("Tags, séparés par des virgules") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))
            ColorPickerRow(selected = state.colorIndex, onSelect = vm::setColor)
            Spacer(Modifier.height(16.dp))

            if (state.isChecklist) {
                ChecklistEditor(
                    lines = state.checklistItems,
                    onToggle = vm::toggleChecklistItem,
                    onEdit = vm::editChecklistItem,
                    onRemove = vm::removeChecklistItem,
                    onAdd = vm::addChecklistItem,
                )
            } else {
                TextField(
                    value = body,
                    onValueChange = { editBody(it) },
                    placeholder = { Text("Écris ce que tu veux… (Gras, italique, photos…)") },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    colors = transparentFieldColors(),
                    // Hauteur minimale, pas fixe : le champ grandit avec le texte et
                    // c'est le défilement de l'écran qui suit le curseur.
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 240.dp),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    wordCount(body.text),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Supprimer cette note ?") },
            text = { Text("Cette action est définitive.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete()
                    confirmDelete = false
                    onBack()
                }) { Text("Supprimer") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Annuler") } },
        )
    }

    // Le geste « retour » doit enregistrer lui aussi, pas seulement la flèche.
    BackHandler { leave() }
}

/** « 128 mots · 720 caractères », ou rien du tout tant que la note est vide. */
private fun wordCount(text: String): String {
    val words = text.split(Regex("\\s+")).count { it.isNotBlank() }
    if (words == 0) return ""
    val mot = if (words > 1) "mots" else "mot"
    return "$words $mot · ${text.length} caractères"
}

@Composable
private fun ChecklistEditor(
    lines: List<ChecklistItem>,
    onToggle: (Int) -> Unit,
    onEdit: (Int, String) -> Unit,
    onRemove: (Int) -> Unit,
    onAdd: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        lines.forEachIndexed { index, item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onToggle(index) }) {
                    Icon(
                        if (item.checked) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
                        contentDescription = if (item.checked) "Décocher" else "Cocher",
                        tint = if (item.checked) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline,
                    )
                }
                TextField(
                    value = item.text,
                    onValueChange = { onEdit(index, it) },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        textDecoration = if (item.checked) TextDecoration.LineThrough else null,
                        color = if (item.checked) MaterialTheme.colorScheme.outline
                        else MaterialTheme.colorScheme.onSurface,
                    ),
                    colors = transparentFieldColors(),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onRemove(index) }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "Retirer",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
        TextButton(onClick = onAdd) { Text("+ Ajouter une ligne") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun transparentFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
)

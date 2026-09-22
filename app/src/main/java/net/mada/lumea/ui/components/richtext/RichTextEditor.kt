package net.mada.lumea.ui.components.richtext

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.FormatItalic
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.FormatStrikethrough
import androidx.compose.material.icons.rounded.FormatUnderlined
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

/**
 * Enregistre une image dans le stockage privé de l'application et renvoie son chemin file://.
 */
fun saveImageToInternalStorage(context: Context, uri: Uri): String? {
    return try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val mediaDir = File(context.filesDir, "media").apply { if (!exists()) mkdirs() }
        val file = File(mediaDir, "img_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(file)
        inputStream?.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Barre d'outils de formatage de texte (Gras, Italique, Souligné, Barré, Titre, Puces, Photo).
 */
@Composable
fun RichTextToolbar(
    textFieldValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            // La copie du fichier part sur un fil d'arrière-plan : une photo de
            // plusieurs mégaoctets gelait l'écran le temps de l'écriture.
            scope.launch {
                val localPath = withContext(Dispatchers.IO) {
                    saveImageToInternalStorage(context, uri)
                }
                if (localPath != null) {
                    val imageTag = "\n\n![Image]($localPath)\n\n"
                    val newText = textFieldValue.text + imageTag
                    onValueChange(
                        textFieldValue.copy(
                            text = newText,
                            selection = androidx.compose.ui.text.TextRange(newText.length)
                        )
                    )
                }
            }
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        modifier = modifier.padding(vertical = 4.dp),
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            item {
                ToolbarButton(
                    icon = Icons.Rounded.FormatBold,
                    description = "Gras",
                    onClick = { onValueChange(applyWrapper(textFieldValue, "**", "**")) }
                )
            }
            item {
                ToolbarButton(
                    icon = Icons.Rounded.FormatItalic,
                    description = "Italique",
                    onClick = { onValueChange(applyWrapper(textFieldValue, "*", "*")) }
                )
            }
            item {
                ToolbarButton(
                    icon = Icons.Rounded.FormatUnderlined,
                    description = "Souligné",
                    onClick = { onValueChange(applyWrapper(textFieldValue, "<u>", "</u>")) }
                )
            }
            item {
                ToolbarButton(
                    icon = Icons.Rounded.FormatStrikethrough,
                    description = "Barré",
                    onClick = { onValueChange(applyWrapper(textFieldValue, "~~", "~~")) }
                )
            }
            item {
                ToolbarButton(
                    icon = Icons.Rounded.Title,
                    description = "Titre",
                    onClick = { onValueChange(applyPrefix(textFieldValue, "# ")) }
                )
            }
            item {
                ToolbarButton(
                    icon = Icons.Rounded.FormatListBulleted,
                    description = "Liste à puces",
                    onClick = { onValueChange(applyPrefix(textFieldValue, "• ")) }
                )
            }
            item {
                ToolbarButton(
                    icon = Icons.Rounded.AddPhotoAlternate,
                    description = "Ajouter une photo",
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun ToolbarButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(38.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Entoure la sélection actuelle par un préfixe et un suffixe (ex: **texte**).
 */
private fun applyWrapper(value: TextFieldValue, prefix: String, suffix: String): TextFieldValue {
    val text = value.text
    val selection = value.selection
    return if (selection.collapsed) {
        val newText = text.substring(0, selection.start) + prefix + suffix + text.substring(selection.start)
        val newCursor = selection.start + prefix.length
        value.copy(
            text = newText,
            selection = androidx.compose.ui.text.TextRange(newCursor)
        )
    } else {
        val selectedText = text.substring(selection.start, selection.end)
        val newText = text.substring(0, selection.start) + prefix + selectedText + suffix + text.substring(selection.end)
        value.copy(
            text = newText,
            selection = androidx.compose.ui.text.TextRange(selection.start, selection.end + prefix.length + suffix.length)
        )
    }
}

/**
 * Insère un préfixe au début de la ligne actuelle.
 */
private fun applyPrefix(value: TextFieldValue, prefix: String): TextFieldValue {
    val text = value.text
    val cursor = value.selection.start
    val lineStart = text.lastIndexOf('\n', cursor - 1).let { if (it == -1) 0 else it + 1 }
    val newText = text.substring(0, lineStart) + prefix + text.substring(lineStart)
    return value.copy(
        text = newText,
        selection = androidx.compose.ui.text.TextRange(cursor + prefix.length)
    )
}

@Composable
fun MarkdownViewer(
    text: String,
    modifier: Modifier = Modifier,
    textColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    val lines = text.lines()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        lines.forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("![") && trimmed.contains("](") && trimmed.endsWith(")") -> {
                    val path = trimmed.substringAfter("](").dropLast(1)
                    LocalImageCard(path = path)
                }
                trimmed.startsWith("# ") -> {
                    Text(
                        text = parseFormattedText(trimmed.drop(2)),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                trimmed.startsWith("• ") -> {
                    Row(
                        modifier = Modifier.padding(start = 8.dp, top = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("• ", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = textColor)
                        Text(
                            text = parseFormattedText(trimmed.drop(2)),
                            style = MaterialTheme.typography.bodyLarge,
                            color = textColor
                        )
                    }
                }
                else -> {
                    if (trimmed.isNotBlank()) {
                        Text(
                            text = parseFormattedText(line),
                            style = MaterialTheme.typography.bodyLarge,
                            color = textColor
                        )
                    } else {
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

/**
 * Affiche une image de note, décodée hors du fil principal.
 *
 * La version précédente appelait `BitmapFactory.decodeFile` directement dans le
 * corps du composable : le fichier entier était relu et décodé à chaque
 * recomposition, sur le fil d'affichage. Une note avec deux ou trois photos
 * saccadait au défilement, et une grande image pouvait épuiser la mémoire.
 *
 * Ici le décodage part sur un fil d'arrière-plan, et l'image est sous-échantillonnée
 * à la volée : on ne charge jamais plus de pixels que l'écran n'en affiche.
 */
@Composable
private fun LocalImageCard(path: String) {
    var bitmap by remember(path) { mutableStateOf<android.graphics.Bitmap?>(null) }
    var failed by remember(path) { mutableStateOf(false) }

    LaunchedEffect(path) {
        val loaded = withContext(Dispatchers.IO) { decodeSampled(path, MAX_IMAGE_WIDTH) }
        if (loaded == null) failed = true else bitmap = loaded
    }

    if (failed) {
        Text(
            "Image introuvable",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        val image = bitmap
        if (image != null) {
            Image(
                bitmap = image.asImageBitmap(),
                contentDescription = "Image de la note",
                modifier = Modifier.fillMaxWidth(),
                contentScale = androidx.compose.ui.layout.ContentScale.FillWidth,
            )
        } else {
            // Réserve la place pendant le chargement : sans ça, la liste sursaute
            // quand chaque image arrive.
            Box(Modifier.fillMaxWidth().height(180.dp))
        }
    }
}

/** Largeur maximale retenue au décodage : au-delà, aucun écran n'y gagne. */
private const val MAX_IMAGE_WIDTH = 1440

/**
 * Décode une image en la réduisant à la volée.
 *
 * Première passe sans allouer de pixels (`inJustDecodeBounds`) pour connaître la
 * taille réelle, puis décodage avec le facteur de réduction adéquat.
 */
private fun decodeSampled(path: String, maxWidth: Int): android.graphics.Bitmap? {
    val file = File(path)
    if (!file.exists()) return null

    val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
    android.graphics.BitmapFactory.decodeFile(file.absolutePath, bounds)
    if (bounds.outWidth <= 0) return null

    var sample = 1
    while (bounds.outWidth / sample > maxWidth) sample *= 2

    return runCatching {
        android.graphics.BitmapFactory.decodeFile(
            file.absolutePath,
            android.graphics.BitmapFactory.Options().apply { inSampleSize = sample },
        )
    }.getOrNull()
}

/**
 * Analyse le texte pour appliquer les styles **gras**, *italique*, <u>souligné</u> et ~~barré~~.
 */
private fun parseFormattedText(text: String): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            when {
                text.startsWith("**", i) -> {
                    val end = text.indexOf("**", i + 2)
                    if (end != -1) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(text.substring(i + 2, end))
                        }
                        i = end + 2
                    } else {
                        append(text[i])
                        i++
                    }
                }
                text.startsWith("*", i) -> {
                    val end = text.indexOf("*", i + 1)
                    if (end != -1) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                text.startsWith("~~", i) -> {
                    val end = text.indexOf("~~", i + 2)
                    if (end != -1) {
                        withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                            append(text.substring(i + 2, end))
                        }
                        i = end + 2
                    } else {
                        append(text[i])
                        i++
                    }
                }
                text.startsWith("<u>", i, ignoreCase = true) -> {
                    val end = text.indexOf("</u>", i + 3, ignoreCase = true)
                    if (end != -1) {
                        withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                            append(text.substring(i + 3, end))
                        }
                        i = end + 4
                    } else {
                        append(text[i])
                        i++
                    }
                }
                else -> {
                    append(text[i])
                    i++
                }
            }
        }
    }
}
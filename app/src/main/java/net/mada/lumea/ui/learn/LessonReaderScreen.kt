package net.mada.lumea.ui.learn

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.mada.lumea.domain.learn.Lesson
import net.mada.lumea.domain.learn.findLesson
import net.mada.lumea.domain.learn.lessonTopics
import net.mada.lumea.ui.components.FloatingNavBarSpace

/**
 * Le texte d'une leçon, enfin lisible.
 *
 * La liste des leçons n'affichait qu'un titre et un résumé coupé à une ligne :
 * le contenu (les `LessonSection`) existait mais n'était jamais montré nulle part.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonReaderScreen(
    lessonId: String,
    onBack: () -> Unit,
    onOpenLesson: (String) -> Unit = {},
) {
    val lesson = findLesson(lessonId)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(lesson?.title ?: "Leçon", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Retour")
                    }
                },
            )
        },
    ) { padding ->
        if (lesson == null) {
            Box(
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text("Cette leçon n'existe plus.", style = MaterialTheme.typography.bodyLarge)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = FloatingNavBarSpace),
        ) {
            item { LessonHeader(lesson) }

            // La figure vient après le résumé et avant le texte : elle donne le
            // schéma d'ensemble qu'on relit ensuite en détail.
            figureFor(lesson.id)?.let { figure ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            LessonFigureView(figure)
                        }
                    }
                }
            }

            items(lesson.sections, key = { it.heading }) { section ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                ) {
                    Text(
                        section.heading,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        section.body,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            item { Spacer(Modifier.height(12.dp)) }

            // Enchaîner sur la suivante évite le retour arrière systématique.
            nextLesson(lesson.id)?.let { next ->
                item {
                    Card(
                        onClick = { onOpenLesson(next.id) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(16.dp),
                        ) {
                            Text(next.emoji, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Leçon suivante",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(next.title, style = MaterialTheme.typography.bodyLarge)
                            }
                            Icon(
                                Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    "Information éducative : elle ne remplace pas un avis médical. " +
                        "En cas de doute, parles-en à un soignant.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 24.dp),
                )
            }
        }
    }
}

@Composable
private fun LessonHeader(lesson: Lesson) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Box(
            Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Text(lesson.emoji, style = MaterialTheme.typography.headlineSmall)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                lesson.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${lesson.minutes} min de lecture",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

/** La leçon qui suit dans l'ordre d'affichage, tous thèmes confondus. */
private fun nextLesson(id: String): Lesson? {
    val all = lessonTopics.flatMap { it.lessons }
    val index = all.indexOfFirst { it.id == id }
    return all.getOrNull(index + 1)
}

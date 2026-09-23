package net.mada.lumea.ui.learn

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.mada.lumea.ui.theme.CycleColors
import net.mada.lumea.ui.theme.RiskColors

/**
 * Les figures des leçons, dessinées au Canvas.
 *
 * Quatorze leçons de texte continu, c'est beaucoup pour un public jeune dont le
 * français n'est pas toujours la première langue. Un schéma fait passer en trois
 * secondes ce qu'un paragraphe met à expliquer — surtout pour le cycle, qui est
 * une histoire de durées et de superpositions.
 *
 * **Pourquoi au Canvas et pas en images.** C'est déjà la langue graphique de
 * l'app (anneau de cycle, illustrations d'écran vide) : les figures suivent donc
 * la palette et le thème sombre sans effort, ne pèsent rien dans l'APK, et
 * restent nettes à toutes les densités. Des dessins bitmap auraient alourdi le
 * téléchargement — ce qui compte quand on installe en données mobiles — et
 * n'auraient pas suivi les cinq palettes.
 *
 * Elles sont volontairement schématiques. Une illustration anatomique approximative
 * serait pire que pas d'illustration du tout.
 */

/** Les figures disponibles, rattachées aux leçons par leur identifiant. */
enum class LessonFigure { CYCLE_ROUE, PHASES, FENETRE_FERTILE, EFFICACITE, PRESERVATIF }

/** La figure d'une leçon, s'il y en a une. Toutes n'en méritent pas. */
fun figureFor(lessonId: String): LessonFigure? = when (lessonId) {
    "cycle-basics" -> LessonFigure.CYCLE_ROUE
    "phases" -> LessonFigure.PHASES
    "ovulation" -> LessonFigure.FENETRE_FERTILE
    "contraception" -> LessonFigure.EFFICACITE
    "condoms" -> LessonFigure.PRESERVATIF
    else -> null
}

@Composable
fun LessonFigureView(figure: LessonFigure, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        when (figure) {
            LessonFigure.CYCLE_ROUE -> CycleWheel()
            LessonFigure.PHASES -> PhaseTimeline()
            LessonFigure.FENETRE_FERTILE -> FertileWindow()
            LessonFigure.EFFICACITE -> EfficacyChart()
            LessonFigure.PRESERVATIF -> CondomSteps()
        }
    }
}

// ------------------------------------------------------------ La roue du cycle

/**
 * Le cycle comme un cadran de 28 jours.
 *
 * L'idée que le jour 1 est le premier jour de saignement — et non la fin des
 * règles — est la plus mal comprise de toutes. Un cercle la montre mieux qu'une
 * phrase.
 */
@Composable
private fun CycleWheel() {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val track = MaterialTheme.colorScheme.surfaceContainerHigh

    Box(
        Modifier
            .fillMaxWidth()
            .height(190.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(170.dp)) {
            val stroke = 22.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(inset, inset)

            drawArc(track, -90f, 360f, false, topLeft, arcSize, style = Stroke(stroke))

            // Règles : jours 1 à 5 sur 28.
            drawArc(
                CycleColors.period, -90f, 5f / 28f * 360f, false,
                topLeft, arcSize, style = Stroke(stroke),
            )
            // Fenêtre fertile : jours 9 à 15.
            drawArc(
                RiskColors.eleve.copy(alpha = 0.85f),
                -90f + 8f / 28f * 360f, 7f / 28f * 360f, false,
                topLeft, arcSize, style = Stroke(stroke),
            )
            // Ovulation : jour 14.
            drawArc(
                RiskColors.maximal,
                -90f + 13f / 28f * 360f, 1f / 28f * 360f, false,
                topLeft, arcSize, style = Stroke(stroke),
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "28",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = onSurface,
            )
            Text(
                "jours",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    FigureLegend(
        listOf(
            CycleColors.period to "Jours 1 à 5 · règles (le jour 1 = premier saignement)",
            RiskColors.eleve.copy(alpha = 0.85f) to "Jours 9 à 15 · fenêtre fertile",
            RiskColors.maximal to "Jour 14 · ovulation",
        )
    )
}

// ------------------------------------------------------------ Les quatre phases

/** Les phases en bande horizontale : leur durée relative se voit. */
@Composable
private fun PhaseTimeline() {
    val phases = listOf(
        Triple("Règles", 5f, CycleColors.period),
        Triple("Folliculaire", 8f, MaterialTheme.colorScheme.tertiary),
        Triple("Ovulation", 1f, RiskColors.maximal),
        Triple("Lutéale", 14f, MaterialTheme.colorScheme.secondary),
    )
    val total = phases.sumOf { it.second.toDouble() }.toFloat()
    val surface = MaterialTheme.colorScheme.surface

    Canvas(
        Modifier
            .fillMaxWidth()
            .height(34.dp)
    ) {
        var x = 0f
        // Un écart de 2px en couleur de fond sépare les segments : c'est le vide
        // qui sépare, pas un contour — un trait ajouterait de l'encre sans donnée.
        val gap = 2.dp.toPx()
        phases.forEach { (_, days, color) ->
            val w = size.width * (days / total)
            drawRoundRect(
                color = color,
                topLeft = Offset(x, 0f),
                size = Size((w - gap).coerceAtLeast(1f), size.height),
                cornerRadius = CornerRadius(4.dp.toPx()),
            )
            x += w
        }
        drawRect(surface, Offset.Zero, Size(0f, 0f))
    }

    Spacer(Modifier.height(10.dp))
    FigureLegend(phases.map { it.third to "${it.first} · environ ${it.second.toInt()} jours" })
}

// ---------------------------------------------------- La fenêtre fertile

/**
 * Pourquoi la fenêtre fertile commence avant l'ovulation.
 *
 * C'est le point qui surprend le plus : les spermatozoïdes attendent jusqu'à
 * cinq jours. Une frise le montre d'un coup d'œil.
 */
@Composable
private fun FertileWindow() {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val track = MaterialTheme.colorScheme.surfaceContainerHigh

    Canvas(
        Modifier
            .fillMaxWidth()
            .height(96.dp)
    ) {
        val left = 0f
        val right = size.width
        val axisY = size.height * 0.62f
        val days = 9f            // jours 9 à 17
        val dayWidth = (right - left) / days

        drawLine(track, Offset(left, axisY), Offset(right, axisY), 2.dp.toPx(), StrokeCap.Round)

        // Survie des spermatozoïdes : 5 jours avant l'ovulation.
        val spermStart = left
        val spermEnd = left + dayWidth * 5.5f
        drawRoundRect(
            color = RiskColors.eleve.copy(alpha = 0.85f),
            topLeft = Offset(spermStart, axisY - 26.dp.toPx()),
            size = Size(spermEnd - spermStart, 14.dp.toPx()),
            cornerRadius = CornerRadius(4.dp.toPx()),
        )

        // Vie de l'ovule : environ 24 heures après l'ovulation.
        val ovuleStart = left + dayWidth * 5f
        drawRoundRect(
            color = RiskColors.maximal,
            topLeft = Offset(ovuleStart, axisY + 12.dp.toPx()),
            size = Size(dayWidth * 1.2f, 14.dp.toPx()),
            cornerRadius = CornerRadius(4.dp.toPx()),
        )

        // Repère de l'ovulation : un trait vertical discret.
        val ovuX = left + dayWidth * 5.4f
        drawLine(
            color = muted,
            start = Offset(ovuX, axisY - 34.dp.toPx()),
            end = Offset(ovuX, axisY + 32.dp.toPx()),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)),
        )
    }

    FigureLegend(
        listOf(
            RiskColors.eleve.copy(alpha = 0.85f) to
                "Les spermatozoïdes vivent jusqu'à 5 jours : un rapport avant " +
                "l'ovulation peut féconder",
            RiskColors.maximal to "L'ovule ne vit qu'environ 24 heures",
        )
    )
}

// ------------------------------------------------------- Efficacité comparée

/**
 * L'efficacité des contraceptions, en usage réel.
 *
 * Choix d'encodage : c'est une **magnitude comparée**, pas une identité. Une
 * seule teinte, la longueur porte l'information. Sept couleurs différentes
 * auraient laissé croire à sept catégories sans rapport.
 *
 * Le cas « aucune méthode » n'est pas distingué par la couleur mais par un
 * remplissage évidé : sur la palette rose, le rouge d'alerte et le rose principal
 * sont indiscernables en vision normale (ΔE 7,4, sous le plancher de 15). La
 * distinction passe donc par la forme et l'étiquette, qui survivent aux cinq
 * palettes et au daltonisme.
 *
 * Chiffres en usage réel — ce qui arrive vraiment aux gens — d'après les
 * tableaux CDC/ACOG, et non en usage parfait.
 */
@Composable
private fun EfficacyChart() {
    val methods = listOf(
        "Implant" to 99.9f,
        "Stérilet" to 99.2f,
        "Injection" to 94f,
        "Pilule" to 93f,
        "Préservatif" to 87f,
        "Retrait" to 78f,
        "Aucune méthode" to 15f,
    )
    val hue = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceContainerHigh
    val ink = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Column(Modifier.fillMaxWidth()) {
        methods.forEach { (name, value) ->
            val hollow = name == "Aucune méthode"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 3.dp),
            ) {
                Text(
                    name,
                    style = MaterialTheme.typography.bodySmall,
                    // Le texte ne porte jamais la couleur de la donnée.
                    color = if (hollow) muted else ink,
                    modifier = Modifier.width(96.dp),
                )
                Canvas(
                    Modifier
                        .weight(1f)
                        .height(16.dp)
                ) {
                    val radius = CornerRadius(4.dp.toPx())
                    drawRoundRect(track, size = size, cornerRadius = radius)
                    val w = size.width * (value / 100f)
                    if (hollow) {
                        drawRoundRect(
                            color = hue,
                            size = Size(w, size.height),
                            cornerRadius = radius,
                            style = Stroke(1.5.dp.toPx()),
                        )
                    } else {
                        drawRoundRect(hue, size = Size(w, size.height), cornerRadius = radius)
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "${value.toInt()} %",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (hollow) muted else ink,
                    modifier = Modifier.width(38.dp),
                )
            }
        }
    }

    Spacer(Modifier.height(8.dp))
    Text(
        "Efficacité en usage réel : sur 100 femmes pendant un an, le pourcentage " +
            "qui ne tombe pas enceinte. Le trait évidé, en bas, est l'absence de " +
            "contraception.",
        style = MaterialTheme.typography.bodySmall,
        color = muted,
    )
}

// ----------------------------------------------------------- Le préservatif

/** Les quatre gestes qui font la différence, en pictogrammes simples. */
@Composable
private fun CondomSteps() {
    val steps = listOf(
        "1" to "Vérifier la date et ouvrir avec les doigts, jamais avec les dents",
        "2" to "Pincer le bout pour chasser l'air avant de dérouler",
        "3" to "Dérouler jusqu'à la base, avant tout contact",
        "4" to "Se retirer en tenant la base, puis nouer et jeter",
    )
    val hue = MaterialTheme.colorScheme.primary

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        steps.forEach { (number, text) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .let { it },
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.size(26.dp)) { drawCircle(hue.copy(alpha = 0.18f)) }
                    Text(
                        number,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

// ------------------------------------------------------------------- Légende

/**
 * La légende d'une figure.
 *
 * Toujours présente dès qu'il y a deux séries : l'identité ne doit jamais
 * reposer sur la seule couleur. Le texte reste en encre neutre, c'est la pastille
 * à côté qui porte la couleur.
 */
@Composable
private fun FigureLegend(entries: List<Pair<Color, String>>) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        entries.forEach { (color, text) ->
            Row(verticalAlignment = Alignment.Top) {
                Canvas(
                    Modifier
                        .padding(top = 5.dp)
                        .size(9.dp)
                ) { drawCircle(color) }
                Spacer(Modifier.width(9.dp))
                Text(
                    text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

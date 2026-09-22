package net.mada.lumea.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp

/**
 * Illustrations d'écran vide, dessinées au trait plutôt qu'importées : elles
 * suivent la couleur du thème et pèsent zéro octet dans l'APK.
 */
enum class Illustration { NOTES, AGENDA, CYCLE, JOURNAL, HABITS }

@Composable
fun LumeaIllustration(
    kind: Illustration,
    tint: Color,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(132.dp)) {
        val s = size.minDimension
        val stroke = Stroke(width = s * 0.035f, cap = StrokeCap.Round)
        when (kind) {
            Illustration.NOTES -> drawNotes(s, tint, accent, stroke)
            Illustration.AGENDA -> drawAgenda(s, tint, accent, stroke)
            Illustration.CYCLE -> drawCycle(s, tint, accent, stroke)
            Illustration.JOURNAL -> drawJournal(s, tint, accent, stroke)
            Illustration.HABITS -> drawHabits(s, tint, accent, stroke)
        }
    }
}

/** Deux feuilles décalées, la première avec quelques lignes de texte. */
private fun DrawScope.drawNotes(s: Float, tint: Color, accent: Color, stroke: Stroke) {
    drawRoundRect(
        color = accent.copy(alpha = 0.30f),
        topLeft = Offset(s * 0.26f, s * 0.14f),
        size = Size(s * 0.52f, s * 0.66f),
        cornerRadius = corner(s * 0.06f),
    )
    drawRoundRect(
        color = tint,
        topLeft = Offset(s * 0.16f, s * 0.22f),
        size = Size(s * 0.52f, s * 0.66f),
        cornerRadius = corner(s * 0.06f),
        style = stroke,
    )
    listOf(0.36f, 0.48f, 0.60f).forEachIndexed { index, y ->
        val width = if (index == 2) 0.20f else 0.30f
        drawLine(
            color = tint.copy(alpha = 0.75f),
            start = Offset(s * 0.25f, s * y),
            end = Offset(s * (0.25f + width), s * y),
            strokeWidth = s * 0.03f,
            cap = StrokeCap.Round,
        )
    }
    drawCircle(color = accent, radius = s * 0.045f, center = Offset(s * 0.62f, s * 0.71f))
}

/** Un mois, avec un jour mis en avant. */
private fun DrawScope.drawAgenda(s: Float, tint: Color, accent: Color, stroke: Stroke) {
    drawRoundRect(
        color = tint,
        topLeft = Offset(s * 0.16f, s * 0.22f),
        size = Size(s * 0.68f, s * 0.60f),
        cornerRadius = corner(s * 0.07f),
        style = stroke,
    )
    drawLine(
        color = tint,
        start = Offset(s * 0.16f, s * 0.38f),
        end = Offset(s * 0.84f, s * 0.38f),
        strokeWidth = s * 0.035f,
    )
    // Anneaux du calendrier
    listOf(0.34f, 0.66f).forEach { x ->
        drawLine(
            color = tint,
            start = Offset(s * x, s * 0.14f),
            end = Offset(s * x, s * 0.26f),
            strokeWidth = s * 0.04f,
            cap = StrokeCap.Round,
        )
    }
    // Grille des jours
    for (row in 0..1) {
        for (col in 0..3) {
            val cx = s * (0.27f + col * 0.15f)
            val cy = s * (0.52f + row * 0.15f)
            val isToday = row == 1 && col == 1
            drawCircle(
                color = if (isToday) accent else tint.copy(alpha = 0.35f),
                radius = s * (if (isToday) 0.048f else 0.028f),
                center = Offset(cx, cy),
            )
        }
    }
}

/** Une lune croissante entourée d'un arc : le passage du temps. */
private fun DrawScope.drawCycle(s: Float, tint: Color, accent: Color, stroke: Stroke) {
    drawArc(
        color = tint.copy(alpha = 0.45f),
        startAngle = -90f,
        sweepAngle = 270f,
        useCenter = false,
        topLeft = Offset(s * 0.12f, s * 0.12f),
        size = Size(s * 0.76f, s * 0.76f),
        style = Stroke(width = s * 0.045f, cap = StrokeCap.Round),
    )
    drawArc(
        color = accent,
        startAngle = -90f,
        sweepAngle = 110f,
        useCenter = false,
        topLeft = Offset(s * 0.12f, s * 0.12f),
        size = Size(s * 0.76f, s * 0.76f),
        style = Stroke(width = s * 0.06f, cap = StrokeCap.Round),
    )
    // Goutte centrale
    val drop = Path().apply {
        moveTo(s * 0.5f, s * 0.34f)
        cubicTo(s * 0.68f, s * 0.52f, s * 0.64f, s * 0.68f, s * 0.5f, s * 0.68f)
        cubicTo(s * 0.36f, s * 0.68f, s * 0.32f, s * 0.52f, s * 0.5f, s * 0.34f)
        close()
    }
    drawPath(drop, color = accent.copy(alpha = 0.85f))
}

/** Un carnet ouvert avec une étoile : le journal du soir. */
private fun DrawScope.drawJournal(s: Float, tint: Color, accent: Color, stroke: Stroke) {
    val book = Path().apply {
        moveTo(s * 0.5f, s * 0.32f)
        cubicTo(s * 0.38f, s * 0.22f, s * 0.24f, s * 0.24f, s * 0.16f, s * 0.28f)
        lineTo(s * 0.16f, s * 0.74f)
        cubicTo(s * 0.26f, s * 0.70f, s * 0.40f, s * 0.70f, s * 0.5f, s * 0.78f)
        cubicTo(s * 0.60f, s * 0.70f, s * 0.74f, s * 0.70f, s * 0.84f, s * 0.74f)
        lineTo(s * 0.84f, s * 0.28f)
        cubicTo(s * 0.76f, s * 0.24f, s * 0.62f, s * 0.22f, s * 0.5f, s * 0.32f)
        close()
    }
    drawPath(book, color = accent.copy(alpha = 0.22f))
    drawPath(book, color = tint, style = stroke)
    drawLine(
        color = tint,
        start = Offset(s * 0.5f, s * 0.32f),
        end = Offset(s * 0.5f, s * 0.78f),
        strokeWidth = s * 0.03f,
    )
    drawStar(Offset(s * 0.74f, s * 0.22f), s * 0.08f, accent)
}

/** Trois cases dont deux cochées : les habitudes. */
private fun DrawScope.drawHabits(s: Float, tint: Color, accent: Color, stroke: Stroke) {
    for (i in 0..2) {
        val y = s * (0.26f + i * 0.22f)
        val done = i < 2
        if (done) {
            drawRoundRect(
                color = accent,
                topLeft = Offset(s * 0.18f, y),
                size = Size(s * 0.14f, s * 0.14f),
                cornerRadius = corner(s * 0.035f),
            )
            val check = Path().apply {
                moveTo(s * 0.215f, y + s * 0.072f)
                lineTo(s * 0.245f, y + s * 0.102f)
                lineTo(s * 0.295f, y + s * 0.042f)
            }
            drawPath(check, color = Color.White, style = Stroke(width = s * 0.022f, cap = StrokeCap.Round))
        } else {
            drawRoundRect(
                color = tint.copy(alpha = 0.5f),
                topLeft = Offset(s * 0.18f, y),
                size = Size(s * 0.14f, s * 0.14f),
                cornerRadius = corner(s * 0.035f),
                style = stroke,
            )
        }
        drawLine(
            color = tint.copy(alpha = if (done) 0.4f else 0.7f),
            start = Offset(s * 0.40f, y + s * 0.07f),
            end = Offset(s * (if (done) 0.70f else 0.80f), y + s * 0.07f),
            strokeWidth = s * 0.032f,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawStar(center: Offset, radius: Float, color: Color) {
    rotate(degrees = 0f, pivot = center) {
        val path = Path()
        repeat(8) { i ->
            val angle = Math.toRadians((i * 45).toDouble())
            val r = if (i % 2 == 0) radius else radius * 0.38f
            val x = center.x + (r * kotlin.math.cos(angle)).toFloat()
            val y = center.y + (r * kotlin.math.sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(path, color = color)
    }
}

private fun corner(radius: Float) =
    androidx.compose.ui.geometry.CornerRadius(radius, radius)

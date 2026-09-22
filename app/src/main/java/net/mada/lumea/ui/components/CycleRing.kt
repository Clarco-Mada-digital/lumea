package net.mada.lumea.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.mada.lumea.ui.theme.CycleColors
import net.mada.lumea.ui.theme.RiskColors
import net.mada.lumea.ui.theme.NumeralStyle
import net.mada.lumea.ui.theme.OvertitleStyle

/** Un arc coloré de l'anneau : une phase du cycle. */
data class RingSegment(val fromDay: Int, val toDay: Int, val color: Color)

/**
 * Anneau de progression du cycle.
 *
 * L'anneau complet représente un cycle entier. Les segments colorés situent les
 * phases (règles, fenêtre fertile, ovulation), le trait plein indique où on en est
 * aujourd'hui, et le repère marque le jour courant.
 *
 * Il démarre en haut (midi) et tourne dans le sens des aiguilles d'une montre :
 * c'est la lecture attendue d'un cadran.
 */
@Composable
fun CycleRing(
    dayOfCycle: Int,
    cycleLength: Int,
    segments: List<RingSegment>,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    thickness: Dp = 16.dp,
    trackColor: Color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.16f),
    progressColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    label: String = "JOUR DU CYCLE",
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    val safeLength = cycleLength.coerceAtLeast(1)
    val target = (dayOfCycle.coerceIn(0, safeLength)).toFloat() / safeLength
    val progress by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "cycleProgress",
    )

    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val strokePx = thickness.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            val topLeft = Offset(inset, inset)

            // Piste de fond
            drawArc(
                color = trackColor,
                startAngle = START_ANGLE,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Butt),
            )

            // Phases : un arc par segment, légèrement plus fin pour rester lisible
            // sous le trait de progression.
            segments.forEach { segment ->
                val from = (segment.fromDay - 1).coerceIn(0, safeLength).toFloat() / safeLength
                val to = segment.toDay.coerceIn(0, safeLength).toFloat() / safeLength
                val sweep = (to - from) * 360f
                if (sweep <= 0f) return@forEach
                drawArc(
                    color = segment.color,
                    startAngle = START_ANGLE + from * 360f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Butt),
                )
            }

            // Progression : un trait fin par-dessus, du début du cycle à aujourd'hui.
            if (progress > 0f) {
                drawArc(
                    color = progressColor,
                    startAngle = START_ANGLE,
                    sweepAngle = progress * 360f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = strokePx * 0.30f, cap = StrokeCap.Round),
                )

                // Repère du jour courant.
                val angle = Math.toRadians((START_ANGLE + progress * 360f).toDouble())
                val radius = (this.size.minDimension - strokePx) / 2f
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                val knob = Offset(
                    x = center.x + (radius * kotlin.math.cos(angle)).toFloat(),
                    y = center.y + (radius * kotlin.math.sin(angle)).toFloat(),
                )
                drawCircle(color = progressColor, radius = strokePx * 0.42f, center = knob)
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (dayOfCycle > 0) "%02d".format(dayOfCycle) else "—",
                style = NumeralStyle,
                color = contentColor,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                label,
                style = OvertitleStyle,
                color = contentColor.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Construit les segments à partir des repères du cycle. Les jours sont exprimés
 * en jour-de-cycle (1 = premier jour des règles).
 */
fun cycleSegments(
    periodLength: Int,
    ovulationDay: Int?,
    cycleLength: Int,
    /** Sur la carte héros (fond sombre), les teintes lumineuses passent mieux. */
    bright: Boolean = true,
): List<RingSegment> = buildList {
    add(
        RingSegment(
            1,
            periodLength.coerceAtLeast(1),
            if (bright) CycleColors.periodBright else CycleColors.period,
        )
    )
    if (ovulationDay != null && ovulationDay in 1..cycleLength) {
        add(
            RingSegment(
                fromDay = (ovulationDay - 5).coerceAtLeast(periodLength + 1),
                toDay = (ovulationDay + 1).coerceAtMost(cycleLength),
                // Même code couleur que le calendrier : la fenêtre fertile est
                // la zone à risque, pas une zone apaisante. La menthe d'origine
                // disait exactement le contraire de ce qu'on veut faire passer.
                color = (if (bright) RiskColors.eleveBright else RiskColors.eleve)
                    .copy(alpha = 0.75f),
            )
        )
        add(
            RingSegment(
                ovulationDay,
                ovulationDay,
                if (bright) RiskColors.maximalBright else RiskColors.maximal,
            )
        )
    }
}

/** 12 h sur un cadran : l'arc commence en haut, pas à droite comme par défaut. */
private const val START_ANGLE = -90f

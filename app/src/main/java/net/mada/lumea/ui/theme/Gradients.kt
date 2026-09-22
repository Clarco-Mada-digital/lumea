package net.mada.lumea.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * Dégradé de la carte héros : une base sombre teintée par la palette, en diagonale.
 *
 * Le fond est volontairement foncé dans les deux thèmes. C'est ce qui permet aux
 * couleurs vives de l'anneau (rose des règles, menthe de la fenêtre fertile) de
 * ressortir : sur un aplat rose clair, elles viraient au gris.
 */
@Composable
fun heroBrush(): Brush {
    // On part de la teinte vive de la palette, pas de `colorScheme.primary` : en
    // thème sombre celui-ci est un rose pâle, et le mélange donnait un brun terne.
    val accent = LocalLumeaStyle.current.accent
    return Brush.linearGradient(
        colors = listOf(
            lerp(HERO_BASE, accent, 0.58f),
            lerp(HERO_BASE, accent, 0.24f),
        ),
        start = Offset.Zero,
        end = Offset.Infinite,
    )
}

/** Presque noir, très légèrement chaud : la base de tous les dégradés héros. */
private val HERO_BASE = Color(0xFF140A10)

/** Couleur du texte et des traits posés sur [heroBrush]. */
val onHeroColor: Color = Color(0xFFFFF4F7)

/** Variante sombre et saturée, pour les cartes qui doivent vraiment accrocher l'œil. */
/**
 * Halo coloré diffus posé derrière le contenu d'un écran : donne de la profondeur
 * sans ajouter d'élément visible. Deux taches, en haut à droite et en bas à gauche.
 */
fun Modifier.screenGlow(primary: Color, secondary: Color): Modifier = drawBehind {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(primary.copy(alpha = 0.20f), Color.Transparent),
            center = Offset(size.width * 0.9f, size.height * 0.02f),
            radius = size.width * 0.75f,
        ),
        radius = size.width * 0.75f,
        center = Offset(size.width * 0.9f, size.height * 0.02f),
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(secondary.copy(alpha = 0.14f), Color.Transparent),
            center = Offset(size.width * 0.05f, size.height * 0.75f),
            radius = size.width * 0.7f,
        ),
        radius = size.width * 0.7f,
        center = Offset(size.width * 0.05f, size.height * 0.75f),
    )
}

/** Enveloppe un écran entier dans le halo de la palette courante. */
@Composable
fun GlowBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(modifier.screenGlow(scheme.primary, scheme.tertiary)) { content() }
}

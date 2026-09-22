package net.mada.lumea.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import net.mada.lumea.ui.theme.LocalLumeaStyle

/**
 * Apparition en cascade : chaque élément d'une liste monte et se révèle avec un
 * léger retard sur le précédent. Le retard est plafonné pour que les longues listes
 * ne fassent pas attendre.
 */
@Composable
fun Reveal(
    index: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val animations = LocalLumeaStyle.current.animations
    val visible = remember { MutableTransitionState(!animations) }
    LaunchedEffect(animations) {
        if (!animations) {
            visible.targetState = true
            return@LaunchedEffect
        }
        delay((index.coerceAtMost(8) * 45).toLong())
        visible.targetState = true
    }

    AnimatedVisibility(
        visibleState = visible,
        enter = fadeIn(tween(320, easing = FastOutSlowInEasing)) +
            slideInVertically(tween(360, easing = FastOutSlowInEasing)) { it / 6 },
        modifier = modifier,
    ) {
        content()
    }
}

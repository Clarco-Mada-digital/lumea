package net.mada.lumea.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.mada.lumea.ui.theme.LocalLumeaStyle

/** Un onglet de la barre flottante. */
data class NavBarItem(
    val key: String,
    val label: String,
    val selectedIcon: ImageVector,
    val icon: ImageVector,
)

/** Hauteur réservée sous le contenu pour que rien ne passe sous la barre. */
val FloatingNavBarSpace = 104.dp

/**
 * Barre de navigation flottante, en verre dépoli.
 *
 * Le contenu des écrans défile derrière elle : c'est ce qui donne l'effet de verre.
 * Android ne sait pas flouter l'arrière-plan d'une vue avant la version 12, et même
 * après c'est coûteux ; on obtient le même rendu avec une surface translucide, un
 * dégradé et un liseré clair sur le bord supérieur — la lumière qui accroche la
 * tranche du verre.
 */
@Composable
fun FloatingNavBar(
    items: List<NavBarItem>,
    selectedKey: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.background.luminanceIsDark()
    val shape = RoundedCornerShape(28.dp)
    val haptics = LocalHapticFeedback.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .height(66.dp)
            .shadow(
                elevation = 22.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.5f),
                spotColor = Color.Black.copy(alpha = 0.6f),
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        scheme.surfaceContainerHigh.copy(alpha = if (dark) 0.94f else 0.92f),
                        scheme.surfaceContainer.copy(alpha = if (dark) 0.86f else 0.84f),
                    )
                )
            )
            // Liseré : clair en haut, presque nul en bas.
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = if (dark) 0.16f else 0.55f),
                        Color.White.copy(alpha = 0.02f),
                    )
                ),
                shape = shape,
            )
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        items.forEach { item ->
            NavBarCell(
                item = item,
                selected = item.key == selectedKey,
                onClick = {
                    if (item.key != selectedKey) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    onSelect(item.key)
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun NavBarCell(
    item: NavBarItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = LocalLumeaStyle.current.accent
    val animations = LocalLumeaStyle.current.animations
    val spec = if (animations) {
        spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    } else {
        spring(stiffness = Spring.StiffnessHigh)
    }

    val scale by animateFloatAsState(if (selected) 1f else 0.92f, spec, label = "navScale")
    val pillWidth by animateDpAsState(
        targetValue = if (selected) 46.dp else 38.dp,
        animationSpec = tween(if (animations) 280 else 0),
        label = "navPill",
    )
    val iconTint by animateColorAsState(
        targetValue = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(if (animations) 240 else 0),
        label = "navTint",
    )

    Column(
        modifier = modifier
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                // Pas d'ondulation rectangulaire qui déborderait du gabarit arrondi.
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            )
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .width(pillWidth)
                .height(28.dp)
                .scale(scale)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (selected) {
                        Brush.horizontalGradient(
                            listOf(accent, androidx.compose.ui.graphics.lerp(accent, Color.White, 0.22f))
                        )
                    } else {
                        Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (selected) item.selectedIcon else item.icon,
                contentDescription = item.label,
                tint = iconTint,
                modifier = Modifier.size(18.dp),
            )
        }

        Spacer(Modifier.height(3.dp))

        Text(
            item.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Vrai si la couleur est assez sombre pour qu'un liseré blanc se voie. */
private fun Color.luminanceIsDark(): Boolean =
    (0.299f * red + 0.587f * green + 0.114f * blue) < 0.5f

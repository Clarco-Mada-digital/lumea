package net.mada.lumea.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isUnspecified
import androidx.core.view.WindowCompat
import net.mada.lumea.data.prefs.CornerStyle
import net.mada.lumea.data.prefs.Palette
import net.mada.lumea.data.prefs.Settings
import net.mada.lumea.data.prefs.TextScale
import net.mada.lumea.data.prefs.ThemeMode

/**
 * Réglages d'apparence dont les écrans ont besoin : halos, animations, et la teinte
 * vive de la palette (celle des dégradés héros, qui ne peut pas être lue depuis
 * `colorScheme` sans perdre en saturation).
 */
data class LumeaStyle(
    val showGlow: Boolean = true,
    val animations: Boolean = true,
    val accent: Color = Color(0xFFE5537E),
)

val LocalLumeaStyle = staticCompositionLocalOf { LumeaStyle() }

/**
 * Rayons de base, en style géométrique. Le réglage « coins » les multiplie pour
 * passer d'angles presque vifs à des cartes plus douces, sans casser les
 * proportions entre composants.
 */
private fun shapesFor(corner: CornerStyle) = Shapes(
    extraSmall = RoundedCornerShape((6 * corner.factor).dp),
    small = RoundedCornerShape((10 * corner.factor).dp),
    medium = RoundedCornerShape((14 * corner.factor).dp),
    large = RoundedCornerShape((20 * corner.factor).dp),
    extraLarge = RoundedCornerShape((26 * corner.factor).dp),
)

/** Applique la taille de texte choisie à toute la typographie d'un coup. */
private fun Typography.scaled(scale: TextScale): Typography {
    if (scale == TextScale.NORMAL) return this
    fun TextStyle.s() = copy(
        fontSize = fontSize * scale.factor,
        lineHeight = if (lineHeight.isUnspecified) lineHeight else lineHeight * scale.factor,
    )
    return copy(
        displayLarge = displayLarge.s(), displayMedium = displayMedium.s(), displaySmall = displaySmall.s(),
        headlineLarge = headlineLarge.s(), headlineMedium = headlineMedium.s(), headlineSmall = headlineSmall.s(),
        titleLarge = titleLarge.s(), titleMedium = titleMedium.s(), titleSmall = titleSmall.s(),
        bodyLarge = bodyLarge.s(), bodyMedium = bodyMedium.s(), bodySmall = bodySmall.s(),
        labelLarge = labelLarge.s(), labelMedium = labelMedium.s(), labelSmall = labelSmall.s(),
    )
}

@Composable
fun LumeaTheme(
    settings: Settings = Settings(),
    content: @Composable () -> Unit,
) {
    val dark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val context = LocalContext.current
    var colorScheme = when {
        settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        else -> schemeFor(settings.palette, dark)
    }

    if (settings.highContrast) {
        // Fond poussé au noir ou au blanc pur et texte ramené au maximum :
        // ce qui aide vraiment en plein soleil ou en cas de vue fatiguée.
        colorScheme = colorScheme.copy(
            background = if (dark) Color.Black else Color.White,
            surface = if (dark) Color.Black else Color.White,
            onBackground = if (dark) Color.White else Color.Black,
            onSurface = if (dark) Color.White else Color.Black,
            onSurfaceVariant = if (dark) Color(0xFFE4E4E4) else Color(0xFF1A1A1A),
            outline = if (dark) Color(0xFFBDBDBD) else Color(0xFF4A4A4A),
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }

    CompositionLocalProvider(
        LocalLumeaStyle provides LumeaStyle(
            showGlow = settings.showGlow && !settings.highContrast,
            animations = settings.animationsEnabled,
            accent = if (settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // En couleurs dynamiques, le conteneur primaire sombre est la teinte
                // la plus saturée que le système nous donne.
                if (dark) colorScheme.primaryContainer else colorScheme.primary
            } else {
                paletteSwatch(settings.palette)
            },
        )
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = LumeaTypography.scaled(settings.textScale),
            shapes = shapesFor(settings.cornerStyle),
            content = content,
        )
    }
}

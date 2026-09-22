package net.mada.lumea.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import net.mada.lumea.data.prefs.Palette

/** Les cinq teintes proposées dans les réglages, en version claire et sombre. */
private data class Tones(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
)

private val roseLight = Tones(
    primary = Color(0xFFB4004E), onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9E1), onPrimaryContainer = Color(0xFF3E0017),
    secondary = Color(0xFF75565C), secondaryContainer = Color(0xFFFFD9E1), onSecondaryContainer = Color(0xFF2B151A),
    tertiary = Color(0xFF7B5800), tertiaryContainer = Color(0xFFFFDEA6), onTertiaryContainer = Color(0xFF271900),
    background = Color(0xFFFFF8F8), onBackground = Color(0xFF201A1B),
    surface = Color(0xFFFFF8F8), onSurface = Color(0xFF201A1B),
    surfaceVariant = Color(0xFFF3DDE1), onSurfaceVariant = Color(0xFF524346),
    outline = Color(0xFF847376),
    surfaceContainer = Color(0xFFFCEAEC), surfaceContainerHigh = Color(0xFFF7E4E7),
)

private val roseDark = Tones(
    primary = Color(0xFFFFB1C4), onPrimary = Color(0xFF650028),
    primaryContainer = Color(0xFF8E003B), onPrimaryContainer = Color(0xFFFFD9E1),
    secondary = Color(0xFFE4BDC4), secondaryContainer = Color(0xFF5B3F44), onSecondaryContainer = Color(0xFFFFD9E1),
    tertiary = Color(0xFFF2BF48), tertiaryContainer = Color(0xFF5D4200), onTertiaryContainer = Color(0xFFFFDEA6),
    background = Color(0xFF191113), onBackground = Color(0xFFECDFE0),
    surface = Color(0xFF191113), onSurface = Color(0xFFECDFE0),
    surfaceVariant = Color(0xFF524346), onSurfaceVariant = Color(0xFFD6C1C5),
    outline = Color(0xFF9E8C90),
    surfaceContainer = Color(0xFF261D1F), surfaceContainerHigh = Color(0xFF312829),
)

private val lavandeLight = Tones(
    primary = Color(0xFF6750A4), onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF), onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71), secondaryContainer = Color(0xFFE8DEF8), onSecondaryContainer = Color(0xFF1D192B),
    tertiary = Color(0xFF7D5260), tertiaryContainer = Color(0xFFFFD8E4), onTertiaryContainer = Color(0xFF31111D),
    background = Color(0xFFFFFBFF), onBackground = Color(0xFF1D1B20),
    surface = Color(0xFFFFFBFF), onSurface = Color(0xFF1D1B20),
    surfaceVariant = Color(0xFFE7E0EC), onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
    surfaceContainer = Color(0xFFF3EDF7), surfaceContainerHigh = Color(0xFFECE6F0),
)

private val lavandeDark = Tones(
    primary = Color(0xFFD0BCFF), onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B), onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC), secondaryContainer = Color(0xFF4A4458), onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFEFB8C8), tertiaryContainer = Color(0xFF633B48), onTertiaryContainer = Color(0xFFFFD8E4),
    background = Color(0xFF141218), onBackground = Color(0xFFE6E0E9),
    surface = Color(0xFF141218), onSurface = Color(0xFFE6E0E9),
    surfaceVariant = Color(0xFF49454F), onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    surfaceContainer = Color(0xFF211F26), surfaceContainerHigh = Color(0xFF2B2930),
)

private val pecheLight = Tones(
    primary = Color(0xFF8F4C1F), onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBC8), onPrimaryContainer = Color(0xFF331200),
    secondary = Color(0xFF765848), secondaryContainer = Color(0xFFFFDBC8), onSecondaryContainer = Color(0xFF2B160A),
    tertiary = Color(0xFF5F6236), tertiaryContainer = Color(0xFFE5E8B0), onTertiaryContainer = Color(0xFF1C1D00),
    background = Color(0xFFFFFBFF), onBackground = Color(0xFF211A16),
    surface = Color(0xFFFFFBFF), onSurface = Color(0xFF211A16),
    surfaceVariant = Color(0xFFF4DED4), onSurfaceVariant = Color(0xFF52443C),
    outline = Color(0xFF85736B),
    surfaceContainer = Color(0xFFFBEBE3), surfaceContainerHigh = Color(0xFFF6E5DC),
)

private val pecheDark = Tones(
    primary = Color(0xFFFFB68C), onPrimary = Color(0xFF522300),
    primaryContainer = Color(0xFF723508), onPrimaryContainer = Color(0xFFFFDBC8),
    secondary = Color(0xFFE6BEAB), secondaryContainer = Color(0xFF5C4132), onSecondaryContainer = Color(0xFFFFDBC8),
    tertiary = Color(0xFFC9CC96), tertiaryContainer = Color(0xFF474B20), onTertiaryContainer = Color(0xFFE5E8B0),
    background = Color(0xFF19120E), onBackground = Color(0xFFEDE0DA),
    surface = Color(0xFF19120E), onSurface = Color(0xFFEDE0DA),
    surfaceVariant = Color(0xFF52443C), onSurfaceVariant = Color(0xFFD7C2B8),
    outline = Color(0xFF9F8D84),
    surfaceContainer = Color(0xFF261E19), surfaceContainerHigh = Color(0xFF312824),
)

private val mentheLight = Tones(
    primary = Color(0xFF006A62), onPrimary = Color.White,
    primaryContainer = Color(0xFF75F8EA), onPrimaryContainer = Color(0xFF00201D),
    secondary = Color(0xFF4A6360), secondaryContainer = Color(0xFFCCE8E4), onSecondaryContainer = Color(0xFF05201D),
    tertiary = Color(0xFF49607A), tertiaryContainer = Color(0xFFD1E4FF), onTertiaryContainer = Color(0xFF021D33),
    background = Color(0xFFFAFDFB), onBackground = Color(0xFF191C1B),
    surface = Color(0xFFFAFDFB), onSurface = Color(0xFF191C1B),
    surfaceVariant = Color(0xFFDAE5E2), onSurfaceVariant = Color(0xFF3F4947),
    outline = Color(0xFF6F7977),
    surfaceContainer = Color(0xFFEBF2F0), surfaceContainerHigh = Color(0xFFE5ECEA),
)

private val mentheDark = Tones(
    primary = Color(0xFF54DBCE), onPrimary = Color(0xFF003733),
    primaryContainer = Color(0xFF00504A), onPrimaryContainer = Color(0xFF75F8EA),
    secondary = Color(0xFFB0CCC8), secondaryContainer = Color(0xFF334B49), onSecondaryContainer = Color(0xFFCCE8E4),
    tertiary = Color(0xFFB1C8E7), tertiaryContainer = Color(0xFF304961), onTertiaryContainer = Color(0xFFD1E4FF),
    background = Color(0xFF101413), onBackground = Color(0xFFDFE3E1),
    surface = Color(0xFF101413), onSurface = Color(0xFFDFE3E1),
    surfaceVariant = Color(0xFF3F4947), onSurfaceVariant = Color(0xFFBEC9C6),
    outline = Color(0xFF889391),
    surfaceContainer = Color(0xFF1C201F), surfaceContainerHigh = Color(0xFF262B2A),
)

private val nuitLight = Tones(
    primary = Color(0xFF3F5BA9), onPrimary = Color.White,
    primaryContainer = Color(0xFFDBE1FF), onPrimaryContainer = Color(0xFF00174B),
    secondary = Color(0xFF5A5D72), secondaryContainer = Color(0xFFDFE1F9), onSecondaryContainer = Color(0xFF171B2C),
    tertiary = Color(0xFF75546F), tertiaryContainer = Color(0xFFFFD7F5), onTertiaryContainer = Color(0xFF2C1229),
    background = Color(0xFFFEFBFF), onBackground = Color(0xFF1B1B1F),
    surface = Color(0xFFFEFBFF), onSurface = Color(0xFF1B1B1F),
    surfaceVariant = Color(0xFFE2E1EC), onSurfaceVariant = Color(0xFF45464F),
    outline = Color(0xFF767680),
    surfaceContainer = Color(0xFFEFEDF4), surfaceContainerHigh = Color(0xFFE9E7EF),
)

private val nuitDark = Tones(
    primary = Color(0xFFB4C5FF), onPrimary = Color(0xFF032978),
    primaryContainer = Color(0xFF24428F), onPrimaryContainer = Color(0xFFDBE1FF),
    secondary = Color(0xFFC3C5DD), secondaryContainer = Color(0xFF424659), onSecondaryContainer = Color(0xFFDFE1F9),
    tertiary = Color(0xFFE4BADB), tertiaryContainer = Color(0xFF5B3C57), onTertiaryContainer = Color(0xFFFFD7F5),
    background = Color(0xFF0E0E12), onBackground = Color(0xFFE4E1E6),
    surface = Color(0xFF0E0E12), onSurface = Color(0xFFE4E1E6),
    surfaceVariant = Color(0xFF45464F), onSurfaceVariant = Color(0xFFC6C5D0),
    outline = Color(0xFF90909A),
    surfaceContainer = Color(0xFF1B1B1F), surfaceContainerHigh = Color(0xFF252529),
)

private fun Tones.toScheme(dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        outline = outline,
        outlineVariant = outline.copy(alpha = 0.4f),
        surfaceContainerLowest = if (dark) background else Color.White,
        surfaceContainerLow = surfaceContainer,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHigh,
    )
}

fun schemeFor(palette: Palette, dark: Boolean): ColorScheme = when (palette) {
    Palette.ROSE -> if (dark) roseDark else roseLight
    Palette.LAVANDE -> if (dark) lavandeDark else lavandeLight
    Palette.PECHE -> if (dark) pecheDark else pecheLight
    Palette.MENTHE -> if (dark) mentheDark else mentheLight
    Palette.NUIT -> if (dark) nuitDark else nuitLight
}.toScheme(dark)

/** Couleur d'aperçu d'une palette dans les réglages. */
fun paletteSwatch(palette: Palette): Color = when (palette) {
    Palette.ROSE -> Color(0xFFE5537E)
    Palette.LAVANDE -> Color(0xFF9A7DE0)
    Palette.PECHE -> Color(0xFFE8825A)
    Palette.MENTHE -> Color(0xFF3FB8AC)
    Palette.NUIT -> Color(0xFF5A73C4)
}

/** Les huit couleurs d'étiquette pour les notes, dossiers, événements et habitudes. */
val accentColors = listOf(
    Color(0xFFE5537E), // rose
    Color(0xFFF08A5D), // corail
    Color(0xFFF2B950), // miel
    Color(0xFF7FC08A), // sauge
    Color(0xFF3FB8AC), // menthe
    Color(0xFF5A9BD8), // ciel
    Color(0xFF9A7DE0), // lavande
    Color(0xFF9E9E9E), // gris
)

fun accent(index: Int): Color = accentColors[index.mod(accentColors.size)]

/** Couleurs sémantiques du calendrier de cycle. */
object CycleColors {
    val period = Color(0xFFE5537E)
    val predicted = Color(0xFFF2A0B8)
    val fertile = Color(0xFF7FC7B8)
    val ovulation = Color(0xFF2E9E86)

    /** Variantes plus lumineuses, pour l'anneau posé sur le fond sombre de la carte héros. */
    val periodBright = Color(0xFFFF6E97)
    val fertileBright = Color(0xFF6FE3C6)
    val ovulationBright = Color(0xFF2FD6A8)
}

/**
 * Échelle de risque de grossesse, façon feu tricolore.
 *
 * La menthe de [CycleColors.fertile] décrit bien la biologie, mais sur un
 * calendrier elle se lit « feu vert » — soit l'inverse exact du message, puisque
 * ce sont les jours les plus à risque. Ici la couleur monte avec le risque, et
 * aucun jour n'est peint en vert : il n'existe pas de jour à zéro risque.
 */
object RiskColors {
    /** Ovulation : le pic. */
    val maximal = Color(0xFFD32F2F)
    /** Fenêtre fertile. */
    val eleve = Color(0xFFF4784E)
    /** Marge d'incertitude autour de la fenêtre. */
    val modere = Color(0xFFF2C14E)
    /** Le reste du cycle : neutre, jamais vert. */
    val faible = Color(0xFF8E9AAF)

    /** Variantes lumineuses pour l'anneau, posé sur le fond sombre de la carte héros. */
    val maximalBright = Color(0xFFFF5A52)
    val eleveBright = Color(0xFFFF9057)
}

/** Dégradé de l'humeur, du plus bas (1) au plus haut (5). */
val moodColors = listOf(
    Color(0xFF7E8AA2),
    Color(0xFF6FA0C9),
    Color(0xFF7FC08A),
    Color(0xFFF2B950),
    Color(0xFFE5537E),
)

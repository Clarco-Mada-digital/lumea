package net.mada.lumea.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import net.mada.lumea.R

/**
 * Deux polices variables embarquées, pour que l'app ait la même allure sur tous les
 * téléphones (sans elles, Android impose sa police système — sur Samsung, une
 * écriture manuscrite qui n'a rien à voir avec l'identité voulue).
 *
 * Outfit : géométrique, large, pour les titres et les chiffres.
 * Inter : dessinée pour les écrans, pour tout ce qui se lit longtemps.
 *
 * Le poids passe par l'axe `wght` de la police variable : `Font(resId, weight)`
 * s'en charge tout seul à partir d'Android 8, notre version minimale.
 */
private val Outfit = FontFamily(
    Font(R.font.outfit_variable, FontWeight.Light),
    Font(R.font.outfit_variable, FontWeight.Normal),
    Font(R.font.outfit_variable, FontWeight.Medium),
    Font(R.font.outfit_variable, FontWeight.SemiBold),
    Font(R.font.outfit_variable, FontWeight.Bold),
    Font(R.font.outfit_variable, FontWeight.ExtraBold),
    Font(R.font.outfit_variable, FontWeight.Black),
)

private val Inter = FontFamily(
    Font(R.font.inter_variable, FontWeight.Normal),
    Font(R.font.inter_variable, FontWeight.Medium),
    Font(R.font.inter_variable, FontWeight.SemiBold),
    Font(R.font.inter_variable, FontWeight.Bold),
)

/** Pour les grands chiffres : jour du cycle, compteurs, statistiques. */
val NumeralStyle = TextStyle(
    fontFamily = Outfit,
    fontWeight = FontWeight.Black,
    fontSize = 64.sp,
    lineHeight = 64.sp,
    letterSpacing = (-3).sp,
)

/** Petites capitales espacées : les intitulés de section et les étiquettes. */
val OvertitleStyle = TextStyle(
    fontFamily = Outfit,
    fontWeight = FontWeight.Bold,
    fontSize = 11.sp,
    lineHeight = 14.sp,
    letterSpacing = 1.6.sp,
)

val LumeaTypography = Typography(
    displayLarge = TextStyle(Outfit, FontWeight.Black, 56.sp, (-2).sp, 60.sp),
    displayMedium = TextStyle(Outfit, FontWeight.Black, 44.sp, (-1.5).sp, 50.sp),
    displaySmall = TextStyle(Outfit, FontWeight.ExtraBold, 34.sp, (-1).sp, 40.sp),

    headlineLarge = TextStyle(Outfit, FontWeight.ExtraBold, 30.sp, (-0.8).sp, 36.sp),
    headlineMedium = TextStyle(Outfit, FontWeight.Bold, 25.sp, (-0.6).sp, 31.sp),
    headlineSmall = TextStyle(Outfit, FontWeight.Bold, 21.sp, (-0.4).sp, 27.sp),

    titleLarge = TextStyle(Outfit, FontWeight.Bold, 19.sp, (-0.3).sp, 25.sp),
    titleMedium = TextStyle(Outfit, FontWeight.SemiBold, 16.sp, (-0.1).sp, 22.sp),
    titleSmall = TextStyle(Outfit, FontWeight.SemiBold, 14.sp, 0.sp, 19.sp),

    // Le corps de texte passe à Inter : c'est là qu'on lit vraiment.
    bodyLarge = TextStyle(Inter, FontWeight.Normal, 16.sp, 0.1.sp, 25.sp),
    bodyMedium = TextStyle(Inter, FontWeight.Normal, 14.sp, 0.1.sp, 21.sp),
    bodySmall = TextStyle(Inter, FontWeight.Normal, 12.5.sp, 0.1.sp, 18.sp),

    labelLarge = TextStyle(Outfit, FontWeight.SemiBold, 14.sp, 0.2.sp, 18.sp),
    labelMedium = TextStyle(Inter, FontWeight.Medium, 12.sp, 0.3.sp, 16.sp),
    labelSmall = TextStyle(Inter, FontWeight.Medium, 11.sp, 0.4.sp, 15.sp),
)

private fun TextStyle(
    family: FontFamily,
    weight: FontWeight,
    size: androidx.compose.ui.unit.TextUnit,
    tracking: androidx.compose.ui.unit.TextUnit,
    lineHeight: androidx.compose.ui.unit.TextUnit,
) = TextStyle(
    fontFamily = family,
    fontWeight = weight,
    fontSize = size,
    letterSpacing = tracking,
    lineHeight = lineHeight,
)

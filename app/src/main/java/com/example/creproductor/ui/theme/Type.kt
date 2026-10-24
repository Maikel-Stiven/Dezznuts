package com.example.creproductor.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.creproductor.R

/** Fuentes variables (OFL): un solo archivo para todos los pesos (Android 8+; antes, el peso por defecto). */
@OptIn(ExperimentalTextApi::class)
private fun variable(res: Int, weight: FontWeight) =
    Font(res, weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))

private val Manrope = FontFamily(
    variable(R.font.manrope, FontWeight.Normal), variable(R.font.manrope, FontWeight.Medium),
    variable(R.font.manrope, FontWeight.SemiBold), variable(R.font.manrope, FontWeight.Bold),
    variable(R.font.manrope, FontWeight.ExtraBold)
)
private val Pixelify = FontFamily(variable(R.font.pixelify_sans, FontWeight.Bold))

/** Premium: Manrope en todo. Omarchy: pixel en títulos y monoespaciada en el resto (estilo terminal). */
private fun scale(titles: FontFamily, body: FontFamily, titleWeight: FontWeight, tracking: Double) = Typography().let { t ->
    fun TextStyle.t() = copy(fontFamily = titles, fontWeight = titleWeight, letterSpacing = tracking.sp)
    fun TextStyle.b() = copy(fontFamily = body)
    t.copy(
        displayLarge = t.displayLarge.t(), displayMedium = t.displayMedium.t(), displaySmall = t.displaySmall.t(),
        headlineLarge = t.headlineLarge.t(), headlineMedium = t.headlineMedium.t(), headlineSmall = t.headlineSmall.t(),
        titleLarge = t.titleLarge.t(), titleMedium = t.titleMedium.t(), titleSmall = t.titleSmall.t(),
        bodyLarge = t.bodyLarge.b(), bodyMedium = t.bodyMedium.b(), bodySmall = t.bodySmall.b(),
        labelLarge = t.labelLarge.t(), labelMedium = t.labelMedium.t(), labelSmall = t.labelSmall.b()
    )
}

val PremiumTypography = scale(Manrope, Manrope, FontWeight.Bold, 0.0)
val OmarchyTypography = scale(Pixelify, FontFamily.Monospace, FontWeight.Bold, 1.0)

val PremiumShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp), small = RoundedCornerShape(10.dp), medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp), extraLarge = RoundedCornerShape(28.dp)
)
/** Esquinas casi rectas, como una terminal. */
val OmarchyShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp), small = RoundedCornerShape(2.dp), medium = RoundedCornerShape(4.dp),
    large = RoundedCornerShape(4.dp), extraLarge = RoundedCornerShape(6.dp)
)

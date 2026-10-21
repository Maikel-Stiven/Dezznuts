package com.example.creproductor.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.creproductor.R

/** Manrope (OFL), fuente variable: un solo archivo para todos los pesos (Android 8+; antes, el peso por defecto). */
@OptIn(ExperimentalTextApi::class)
private fun manrope(weight: FontWeight) = Font(
    R.font.manrope, weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight))
)

val Manrope = FontFamily(
    manrope(FontWeight.Normal), manrope(FontWeight.Medium), manrope(FontWeight.SemiBold),
    manrope(FontWeight.Bold), manrope(FontWeight.ExtraBold)
)

private fun style(size: Int, line: Int, weight: FontWeight, tracking: Double = 0.0) = TextStyle(
    fontFamily = Manrope, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = tracking.sp
)

val Typography = Typography(
    displayLarge = style(52, 60, FontWeight.ExtraBold, -1.0),
    displayMedium = style(42, 50, FontWeight.ExtraBold, -0.8),
    displaySmall = style(34, 42, FontWeight.Bold, -0.5),
    headlineLarge = style(30, 38, FontWeight.Bold, -0.4),
    headlineMedium = style(26, 34, FontWeight.Bold, -0.3),
    headlineSmall = style(22, 30, FontWeight.Bold, -0.2),
    titleLarge = style(20, 28, FontWeight.Bold, -0.1),
    titleMedium = style(16, 24, FontWeight.SemiBold),
    titleSmall = style(15, 21, FontWeight.SemiBold),
    bodyLarge = style(16, 24, FontWeight.Normal),
    bodyMedium = style(14, 20, FontWeight.Normal),
    bodySmall = style(12, 17, FontWeight.Medium),
    labelLarge = style(14, 20, FontWeight.SemiBold, 0.1),
    labelMedium = style(12, 16, FontWeight.SemiBold, 0.3),
    labelSmall = style(11, 14, FontWeight.Bold, 0.4),
)

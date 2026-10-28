package com.example.creproductor.gui

import com.example.creproductor.i18n.tr

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.creproductor.data.QualityTier

/**
 * Cinta "LOSSLESS" / "HI-RES" en la base de una miniatura de portada; no dibuja nada para audio con pérdida.
 * Sustituye al distintivo que iba junto al título y le quitaba espacio. Una sola vista de texto, sin composición extra.
 */
@Composable
fun CoverQualityStrip(tier: QualityTier, modifier: Modifier = Modifier) {
    if (tier == QualityTier.LOSSY) return
    val hires = tier == QualityTier.HIRES
    val qualityDescription = tr(if (hires) "Calidad Hi-Res" else "Calidad sin pérdida")
    Text(
        text = if (hires) "HI-RES" else "LOSSLESS",
        fontSize = 8.sp,
        lineHeight = 10.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        maxLines = 1,
        color = if (hires) MaterialTheme.colorScheme.onPrimary else Color.White,
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = qualityDescription }
            .background(if (hires) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.6f))
            .padding(vertical = 1.dp)
    )
}

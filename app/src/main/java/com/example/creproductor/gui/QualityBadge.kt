package com.example.creproductor.gui

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.creproductor.data.QualityTier

/** Distintivo "Hi-Res" / "Lossless"; no dibuja nada para audio con pérdida. */
@Composable
fun QualityBadge(tier: QualityTier, modifier: Modifier = Modifier) {
    if (tier == QualityTier.LOSSY) return
    val hires = tier == QualityTier.HIRES
    Text(
        text = if (hires) "Hi-Res" else "Lossless",
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = if (hires) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier
            .semantics { contentDescription = if (hires) "Calidad Hi-Res" else "Calidad sin pérdida" }
            .background(
                if (hires) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 5.dp, vertical = 1.dp)
    )
}

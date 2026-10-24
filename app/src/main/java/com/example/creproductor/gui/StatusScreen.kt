package com.example.creproductor.gui

import com.example.creproductor.i18n.tr

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.creproductor.R

/**
 * Pantallas de estado (permiso, cargando, biblioteca vacía) con la bellota de la marca. Es pixel art, así que se
 * dibuja sin suavizado para que no se vea borrosa al ampliarla.
 */
@Composable
fun StatusScreen(
    title: String,
    message: String,
    loading: Boolean = false,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
    secondaryLabel: String? = null,
    onSecondary: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxSize().systemBarsPadding().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = BitmapPainter(ImageBitmap.imageResource(R.drawable.ic_launcher_foreground), filterQuality = FilterQuality.None),
            contentDescription = null,
            modifier = Modifier.size(180.dp)
        )
        Text(tr(title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            tr(message),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 360.dp)
        )
        Spacer(Modifier.height(28.dp))
        if (loading) LinearProgressIndicator(Modifier.widthIn(max = 200.dp))
        if (actionLabel != null) Button(onClick = onAction) { Text(tr(actionLabel)) }
        if (secondaryLabel != null) TextButton(onClick = onSecondary) { Text(tr(secondaryLabel)) }
    }
}

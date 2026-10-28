package com.example.creproductor.widget

import android.content.Context
import android.media.AudioManager
import android.view.KeyEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.creproductor.MainActivity
import com.example.creproductor.R

private val Purple = Color(0xFFBB86FC)
private val Surface = Color(0xFF1E1E1E)

/** Lo que muestra el widget: lo escribe el servicio al cambiar de pista o de estado (ver `MusicService.publishWidget`). */
internal const val KEY_TITLE = "widget_title"
internal const val KEY_ARTIST = "widget_artist"
internal const val KEY_PLAYING = "widget_playing"

/** Widget de pantalla de inicio: título, artista y anterior, reproducir/pausar y siguiente. Mismo morado de la app. */
class DezzWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = context.getSharedPreferences("dezznuts_prefs", Context.MODE_PRIVATE)
        val title = prefs.getString(KEY_TITLE, null) ?: "DEZZNUTS"
        val artist = prefs.getString(KEY_ARTIST, null) ?: "Toca para abrir"
        val playing = prefs.getBoolean(KEY_PLAYING, false)
        provideContent {
            Row(
                modifier = GlanceModifier.fillMaxSize().background(ColorProvider(Surface)).cornerRadius(20.dp)
                    .padding(horizontal = 14.dp, vertical = 8.dp).clickable(actionStartActivity<MainActivity>()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(title, maxLines = 1, style = TextStyle(color = ColorProvider(Color.White), fontSize = 15.sp, fontWeight = FontWeight.Bold))
                    Text(artist, maxLines = 1, style = TextStyle(color = ColorProvider(Color(0xFFB0B0B0)), fontSize = 12.sp))
                }
                Spacer(GlanceModifier.width(8.dp))
                Control(R.drawable.ic_w_prev, "Anterior", KeyEvent.KEYCODE_MEDIA_PREVIOUS, Color.White)
                Control(if (playing) R.drawable.ic_w_pause else R.drawable.ic_w_play, if (playing) "Pausar" else "Reproducir", KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, Purple)
                Control(R.drawable.ic_w_next, "Siguiente", KeyEvent.KEYCODE_MEDIA_NEXT, Color.White)
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun Control(icon: Int, description: String, keyCode: Int, tint: Color) {
    Image(
        provider = ImageProvider(icon),
        contentDescription = description,
        colorFilter = ColorFilter.tint(ColorProvider(tint)),
        modifier = GlanceModifier.size(40.dp).padding(8.dp)
            .clickable(actionRunCallback<MediaKeyAction>(actionParametersOf(KEY to keyCode)))
    )
}

private val KEY = ActionParameters.Key<Int>("media_key")

/** Envía la tecla multimedia a la sesión activa; si la app estaba cerrada, el sistema la reanuda (ver `onPlaybackResumption`). */
class MediaKeyAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val code = parameters[KEY] ?: return
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
    }
}

class DezzWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DezzWidget()
}

package com.example.creproductor.gui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.Normalizer

/** Inicial para el índice: sin tildes y en mayúscula ("Ñandú" → "N", "élan" → "E"); números y símbolos → "#". */
fun indexInitial(text: String): String {
    val c = Normalizer.normalize(text.trimStart().take(1), Normalizer.Form.NFD).firstOrNull()?.uppercaseChar() ?: return "#"
    return if (c in 'A'..'Z') c.toString() else "#"
}

/** Primera posición de cada inicial, en el orden en que aparecen en la lista (ya ordenada por ese mismo texto). */
fun indexPositions(keys: List<String>): List<Pair<String, Int>> {
    val seen = LinkedHashMap<String, Int>()
    keys.forEachIndexed { i, k -> seen.putIfAbsent(indexInitial(k), i) }
    return seen.toList()
}

/**
 * Barra A–Z al borde de una lista larga: tocar o arrastrar salta a la primera canción de esa letra, con la letra
 * grande al lado del dedo. Solo muestra las letras que existen, así ningún toque cae en el vacío.
 */
@Composable
fun AlphabetIndex(entries: List<Pair<String, Int>>, listState: LazyListState, modifier: Modifier = Modifier) {
    if (entries.size < 2) return
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    var active by remember { mutableStateOf<Int?>(null) }
    var barHeight by remember { mutableStateOf(1) }

    fun select(y: Float) {
        val i = (y / barHeight * entries.size).toInt().coerceIn(0, entries.lastIndex)
        if (i != active) {
            active = i
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            scope.launch { listState.scrollToItem(entries[i].second) }
        }
    }

    Box(modifier.fillMaxHeight()) {
        Column(
            Modifier.fillMaxHeight().width(28.dp).padding(vertical = 8.dp)
                .semantics { contentDescription = "A–Z" }
                .onSizeChanged { barHeight = it.height.coerceAtLeast(1) }
                .pointerInput(entries) {
                    detectTapGestures(onPress = { select(it.y); tryAwaitRelease(); active = null })
                }
                .pointerInput(entries) {
                    detectVerticalDragGestures(
                        onDragStart = { select(it.y) }, onDragEnd = { active = null }, onDragCancel = { active = null }
                    ) { change, _ -> select(change.position.y) }
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Cada letra ocupa la misma fracción del alto: caben todas en cualquier pantalla y el toque cae donde se ve.
            entries.forEachIndexed { i, (letter, _) ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        letter, fontSize = 10.sp, lineHeight = 10.sp, fontWeight = if (i == active) FontWeight.Bold else FontWeight.Medium,
                        color = if (i == active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        active?.let { i ->
            val y = (barHeight * (i + 0.5f) / entries.size).toInt()
            Box(
                Modifier.offset { IntOffset(-64.dp.roundToPx(), y - 28.dp.roundToPx()) }.size(56.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(entries[i].first, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

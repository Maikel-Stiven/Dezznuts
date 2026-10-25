package com.example.creproductor.gui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.creproductor.ui.theme.AppAccentPreset
import com.example.creproductor.ui.theme.AppStyle

@Composable
fun ThemeDialog(
    currentStyle: AppStyle,
    isAdaptiveAccent: Boolean,
    currentPreset: AppAccentPreset,
    customAccentColor: Color,
    onStyleSelected: (AppStyle) -> Unit,
    onAdaptiveAccentToggled: (Boolean) -> Unit,
    onAccentPresetSelected: (AppAccentPreset) -> Unit,
    onCustomAccentColorPicked: (Color) -> Unit,
    onDismiss: () -> Unit
){
    var selectedTab by remember { mutableIntStateOf(0) }
    var showColorPicker by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Cambiar aspectos",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp)
                ) {
                    TabButton(
                        text = "ASPECTOS",
                        selected = (selectedTab == 0),
                        modifier = Modifier.weight(1f),
                        onClick = {selectedTab = 0}
                    )
                    TabButton(
                        text = "COLORES",
                        selected = (selectedTab == 1),
                        modifier = Modifier.weight(1f),
                        onClick = {selectedTab = 1}
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (selectedTab == 0){
                        AppStyle.entries.forEach { style ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable{onStyleSelected(style)}
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (style == currentStyle),
                                    onClick = {onStyleSelected(style)}
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column{
                                    Text(
                                        text = style.label,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = style.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }else{
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Color adaptativo",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isAdaptiveAccent,
                                onCheckedChange = onAdaptiveAccentToggled
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        Text(
                            text = "Colores estáticos",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isAdaptiveAccent) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        AppAccentPreset.entries.forEach { preset ->
                            val isSelected = (preset == currentPreset) && !isAdaptiveAccent
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isAdaptiveAccent){onAccentPresetSelected(preset)}
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    enabled = !isAdaptiveAccent,
                                    onClick = {onAccentPresetSelected(preset)}
                                )
                                Spacer(modifier = Modifier.width(8.dp))

                                val previewColor = when(preset){
                                    AppAccentPreset.DEFAULT -> MaterialTheme.colorScheme.primary
                                    AppAccentPreset.CUSTOM -> customAccentColor
                                    else -> preset.defaultColor ?: MaterialTheme.colorScheme.primary
                                }
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(MaterialTheme.shapes.extraSmall)
                                        .background(
                                            if (isAdaptiveAccent) previewColor.copy(alpha = 0.3f)
                                            else previewColor
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.outlineVariant,
                                            shape = MaterialTheme.shapes.extraSmall
                                        )
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                Text(
                                    text = preset.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isAdaptiveAccent)
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        AnimatedVisibility(visible = (currentPreset == AppAccentPreset.CUSTOM) && !isAdaptiveAccent) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                Button(
                                    onClick = {showColorPicker = !showColorPicker},
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text("Seleccionar color")
                                }
                                if (showColorPicker){
                                    Spacer(modifier = Modifier.height(12.dp))
                                    ColorPickerPanel(
                                        currentColor = customAccentColor,
                                        onColorChanged = onCustomAccentColorPicked
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("Cancelar", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun TabButton(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
){
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ){
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ColorPickerPanel(
    currentColor: Color,
    onColorChanged: (Color) -> Unit
){
    val argb = currentColor.toArgb()
    var r by remember (currentColor) { mutableFloatStateOf((argb shr 16 and 0xFF).toFloat())}
    var g by remember (currentColor) { mutableFloatStateOf((argb shr 8 and 0xFF).toFloat()) }
    var  b by remember(currentColor) {mutableFloatStateOf((argb and 0xFF).toFloat())}

    fun update(){
        onColorChanged(Color(r.toInt(), g.toInt(), b.toInt()))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(Color(r.toInt(), g.toInt(), b.toInt()))
                    .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = String.format("#%02X%02X%02X", r.toInt(), g.toInt(), b.toInt()),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text("R: ${r.toInt()}", style = MaterialTheme.typography.bodySmall)
        Slider(
            value = r,
            onValueChange = {r = it; update()},
            valueRange = 0f..255f
        )

        Text("G: ${g.toInt()}", style = MaterialTheme.typography.bodySmall)
        Slider(
            value = g,
            onValueChange = {g = it; update()},
            valueRange = 0f..255f
        )


        Text("B: ${b.toInt()}", style = MaterialTheme.typography.bodySmall)
        Slider(
            value = b,
            onValueChange = {b = it; update()},
            valueRange = 0f..255f
        )

    }
}

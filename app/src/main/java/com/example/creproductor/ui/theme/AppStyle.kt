package com.example.creproductor.ui.theme

import androidx.compose.ui.graphics.Color

enum class AppStyle(val label: String, val description: String){
    MATERIAL_3("Material 3", "Colores del sistema y bordes redondeados (por defecto)"),
    OMARGE("Omarge", "Estética retro terminal"),
    OLED_DARK("OLED negro Puro", "Fondo totalmente negro (#000000) para pantallas AMOLED")
}

enum class AppAccentPreset(val label: String, val defaultColor: Color?){
    DEFAULT(" Predeterminado ", null),
    BLUE ("Azul", Color(0xFF4285F4)),
    PURPLE("Morado", Color(0xFFA142F4)),
    RED("Rojo", Color(0xFFE53935)),
    WHITE("Blanco", Color(0xFFFFFFFF)),
    CUSTOM("Personalizado", null)
}
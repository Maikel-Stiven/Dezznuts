package com.example.creproductor.gui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

sealed class DrawerMenuItem(
    val title: String,
    val icon : ImageVector
){
    object Equalizer : DrawerMenuItem("Ecualizador", Icons.Default.Equalizer)
    object ScanFiles : DrawerMenuItem("Escanear archivos",  Icons.Default.Radar)
    object SearchDuplicates : DrawerMenuItem("Buscar duplicados o audios de whatsapp", Icons.Default.FindInPage)
    object AudioCutter : DrawerMenuItem("Cortador de audio", Icons.Default.ContentCut)
    object SleepTimer: DrawerMenuItem("Temporizador", Icons.Default.Timer)
    object ClearImageCache : DrawerMenuItem("Borrar caché de imágenes", Icons.Default.DeleteSweep)
    object About: DrawerMenuItem("Acerca de", Icons.Default.Info)
    object  SendFeedback : DrawerMenuItem("Enviar comentarios", Icons.AutoMirrored.Filled.HelpOutline)
}

@Composable
fun AppDrawerContent(
    onMenuItem: (DrawerMenuItem) -> Unit,
    onCloseDrawer: () -> Unit
){
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.width(300.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column{
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp, horizontal = 12.dp)
                ){
                    Text(
                        text = "DEZZNUTS",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))

                val menuItems = listOf(
                    DrawerMenuItem.Equalizer,
                    DrawerMenuItem.ScanFiles,
                    DrawerMenuItem.SearchDuplicates,
                    DrawerMenuItem.AudioCutter,
                    DrawerMenuItem.SleepTimer,
                    DrawerMenuItem.ClearImageCache,
                    DrawerMenuItem.About,
                    DrawerMenuItem.SendFeedback
                )

                menuItems.forEach { item ->
                    NavigationDrawerItem(
                        label = {Text(item.title)},
                        icon = {Icon(item.icon, contentDescription = item.title)},
                        selected = false,
                        onClick = {
                            onCloseDrawer()
                            onMenuItem(item)
                        },
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))
                Text(
                    text = "DEZZNUTS Player Alpha.0.0.1",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
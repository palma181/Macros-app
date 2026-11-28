package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.MacroGoals
import com.example.ui.MacroUiState
import com.example.ui.components.bringIntoViewOnFocus

@Composable
fun GoalsScreen(
    uiState: MacroUiState,
    onSaveGoals: (MacroGoals) -> Unit,
    onExportBackup: (Uri) -> Unit,
    onImportBackup: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val saved = uiState.goals
    var kcalText by remember(saved) { mutableStateOf(saved.kcal.toString()) }
    var proteinText by remember(saved) { mutableStateOf(saved.protein.toString()) }
    var carbsText by remember(saved) { mutableStateOf(saved.carbs.toString()) }
    var fatText by remember(saved) { mutableStateOf(saved.fat.toString()) }

    LaunchedEffect(saved) {
        kcalText = saved.kcal.toString()
        proteinText = saved.protein.toString()
        carbsText = saved.carbs.toString()
        fatText = saved.fat.toString()
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) onExportBackup(uri)
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onImportBackup(uri)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.daily_goals_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GoalInputField(
                                value = kcalText,
                                onValueChange = { kcalText = it },
                                label = "Calorías (kcal)",
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("goal_kcal_input")
                            )
                            GoalInputField(
                                value = proteinText,
                                onValueChange = { proteinText = it },
                                label = "Proteína (g)",
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("goal_protein_input")
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GoalInputField(
                                value = carbsText,
                                onValueChange = { carbsText = it },
                                label = "Carbs (g)",
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("goal_carbs_input")
                            )
                            GoalInputField(
                                value = fatText,
                                onValueChange = { fatText = it },
                                label = "Grasa (g)",
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("goal_fat_input")
                            )
                        }

                        val previewKcal = kcalText.toIntOrNull() ?: 0
                        Text(
                            text = "Meta semanal: ${previewKcal * 7} kcal · Meta mensual (30d): ${previewKcal * 30} kcal",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = {
                                onSaveGoals(
                                    MacroGoals(
                                        kcal = kcalText.toIntOrNull() ?: 2000,
                                        protein = proteinText.toIntOrNull() ?: 150,
                                        carbs = carbsText.toIntOrNull() ?: 200,
                                        fat = fatText.toIntOrNull() ?: 60
                                    )
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("save_goals_button")
                        ) {
                            Text(stringResource(R.string.save_goals))
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.backup_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Exporta o importa tus comidas, pesos y metas en formato JSON (compatible con mis-macros.json). Tus claves de API nunca se exportan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { exportLauncher.launch("mis-macros.json") },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("export_json_button")
                            ) {
                                Icon(
                                    Icons.Outlined.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Exportar")
                            }
                            OutlinedButton(
                                onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("import_json_button")
                            ) {
                                Icon(
                                    Icons.Outlined.FileUpload,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Importar")
                            }
                        }
                    }
                }
            }

            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "App Android nativa instalable (APK)",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Puedes descargar el APK directamente desde el menú de configuración del proyecto para instalarlo en cualquier teléfono Android.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { raw -> onValueChange(raw.filter { it.isDigit() }.take(5)) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier.bringIntoViewOnFocus()
    )
}

package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.WeightEntity
import com.example.ui.MacroUiState
import com.example.ui.components.WeightLineChart
import com.example.util.DateUtils
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightScreen(
    uiState: MacroUiState,
    onSaveWeight: (date: String, kg: Double) -> Unit,
    onDeleteWeight: (date: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var kgText by rememberSaveable { mutableStateOf("") }
    var dateText by rememberSaveable { mutableStateOf(DateUtils.todayIso()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val weightsAsc = uiState.weights
    val weightsDesc = remember(weightsAsc) { weightsAsc.reversed() }
    val latest = weightsAsc.lastOrNull()
    val first = weightsAsc.firstOrNull()

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateUtils.isoToMillis(dateText)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            dateText = DateUtils.millisToIso(millis)
                        }
                        showDatePicker = false
                    }
                ) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize()
                .testTag("weight_lazy_column"),
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
                            text = stringResource(R.string.weight_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = kgText,
                                onValueChange = { raw ->
                                    val filtered = raw.replace(',', '.')
                                    if (filtered.isEmpty() || filtered.matches(Regex("^\\d{0,3}(\\.\\d{0,2})?$"))) {
                                        kgText = filtered
                                    }
                                },
                                label = { Text("Peso (kg)") },
                                placeholder = { Text("72.5") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("weight_kg_input")
                            )
                            OutlinedButton(
                                onClick = { showDatePicker = true },
                                modifier = Modifier
                                    .height(56.dp)
                                    .testTag("weight_date_button")
                            ) {
                                Icon(
                                    Icons.Outlined.CalendarMonth,
                                    contentDescription = "Elegir fecha",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(dateText)
                            }
                        }
                        Button(
                            onClick = {
                                val parsed = kgText.toDoubleOrNull()
                                if (parsed != null && parsed > 0.0) {
                                    onSaveWeight(dateText, parsed)
                                    kgText = ""
                                }
                            },
                            enabled = (kgText.toDoubleOrNull() ?: 0.0) > 0.0,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("save_weight_button")
                        ) {
                            Text(stringResource(R.string.save_weight))
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
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.weight_evolution),
                                style = MaterialTheme.typography.titleLarge
                            )
                            if (latest != null) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    val deltaText = if (first != null && weightsAsc.size > 1) {
                                        val diff = latest.kg - first.kg
                                        val sign = if (diff > 0) "+" else ""
                                        " · $sign${String.format(Locale.US, "%.1f", diff)} kg"
                                    } else ""
                                    Text(
                                        text = "${String.format(Locale.US, "%.1f", latest.kg)} kg$deltaText",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        if (weightsAsc.size >= 2) {
                            WeightLineChart(weights = weightsAsc)
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.MonitorWeight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                    modifier = Modifier.size(34.dp)
                                )
                                Text(
                                    text = if (weightsAsc.isEmpty()) {
                                        stringResource(R.string.no_weights_hint)
                                    } else {
                                        "Registra al menos 2 fechas distintas para ver la gráfica."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            if (weightsDesc.isNotEmpty()) {
                item {
                    Text(
                        text = "Historial",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                items(weightsDesc, key = { it.date }) { item ->
                    WeightHistoryRow(
                        item = item,
                        onDelete = { onDeleteWeight(item.date) }
                    )
                }
            }
        }
    }
}

@Composable
private fun WeightHistoryRow(
    item: WeightEntity,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("weight_row_${item.date}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "${String.format(Locale.US, "%.1f", item.kg)} kg",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = item.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_weight_${item.date}")
            ) {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    contentDescription = "Eliminar peso",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.MealEntity
import com.example.data.model.MealType
import com.example.ui.MacroUiState
import com.example.ui.components.bringIntoViewOnFocus
import com.example.ui.theme.LocalMacroColors
import com.example.util.DateUtils

private data class DayMealGroup(
    val date: String,
    val meals: List<MealEntity>,
    val totalKcal: Int,
    val totalProtein: Int,
    val totalCarbs: Int,
    val totalFat: Int
)

@Composable
fun MealsHistoryScreen(
    uiState: MacroUiState,
    onEditMeal: (MealEntity) -> Unit,
    onDeleteMeal: (Long) -> Unit,
    onRepeatMealToday: (MealEntity) -> Unit,
    onOpenAddMeal: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedType by rememberSaveable { mutableStateOf<String?>(null) }

    val filteredMeals = remember(uiState.allMeals, searchQuery, selectedType) {
        val q = searchQuery.trim()
        uiState.allMeals.filter { meal ->
            val matchesQuery = q.isBlank() || meal.name.contains(q, ignoreCase = true)
            val matchesType = selectedType == null || meal.mealType.equals(selectedType, ignoreCase = true)
            matchesQuery && matchesType
        }
    }

    val groupedByDay = remember(filteredMeals) {
        filteredMeals
            .groupBy { it.date }
            .entries
            .sortedByDescending { it.key }
            .map { (date, list) ->
                DayMealGroup(
                    date = date,
                    meals = list,
                    totalKcal = list.sumOf { it.kcal },
                    totalProtein = list.sumOf { it.protein },
                    totalCarbs = list.sumOf { it.carbs },
                    totalFat = list.sumOf { it.fat }
                )
            }
    }

    val totalDays = groupedByDay.size
    val avgDailyKcal = if (totalDays > 0) groupedByDay.sumOf { it.totalKcal } / totalDays else 0

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize()
                .testTag("meals_history_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Historial de comidas",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = if (totalDays > 0) {
                                    "$totalDays días · Promedio $avgDailyKcal kcal/día"
                                } else {
                                    "Tus comidas y calorías por día"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "${filteredMeals.size} comidas",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar comida en el historial...") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Outlined.Clear, contentDescription = "Limpiar búsqueda")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewOnFocus()
                            .testTag("history_search_input")
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedType == null,
                            onClick = { selectedType = null },
                            label = { Text("Todas") }
                        )
                        MealType.entries.forEach { type ->
                            FilterChip(
                                selected = selectedType == type.label,
                                onClick = {
                                    selectedType = if (selectedType == type.label) null else type.label
                                },
                                label = { Text(type.label) }
                            )
                        }
                    }
                }
            }

            if (groupedByDay.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                        ),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Outlined.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                modifier = Modifier.size(38.dp)
                            )
                            Text(
                                text = if (uiState.allMeals.isEmpty()) {
                                    "Tu historial de comidas está vacío"
                                } else {
                                    "No hay comidas que coincidan con tu filtro"
                                },
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Aquí verás todo lo que comiste cada día y sus calorías totales.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (uiState.allMeals.isEmpty()) {
                                Button(
                                    onClick = onOpenAddMeal,
                                    modifier = Modifier.testTag("history_add_meal_button")
                                ) {
                                    Icon(
                                        Icons.Outlined.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Agregar comida")
                                }
                            }
                        }
                    }
                }
            } else {
                items(groupedByDay, key = { it.date }) { group ->
                    DayHistoryCard(
                        group = group,
                        goalKcal = uiState.goals.kcal,
                        onEditMeal = onEditMeal,
                        onDeleteMeal = onDeleteMeal,
                        onRepeatMealToday = onRepeatMealToday
                    )
                }
            }
        }
    }
}

@Composable
private fun DayHistoryCard(
    group: DayMealGroup,
    goalKcal: Int,
    onEditMeal: (MealEntity) -> Unit,
    onDeleteMeal: (Long) -> Unit,
    onRepeatMealToday: (MealEntity) -> Unit
) {
    val macroColors = LocalMacroColors.current
    val overGoal = goalKcal > 0 && group.totalKcal > goalKcal

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_day_${group.date}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = DateUtils.formatSpanishDayLabel(group.date),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${group.date} · P ${group.totalProtein}g · C ${group.totalCarbs}g · G ${group.totalFat}g",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = if (overGoal) {
                        macroColors.carbs.copy(alpha = 0.16f)
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "${group.totalKcal} kcal",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (overGoal) {
                            macroColors.carbs
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            group.meals.forEachIndexed { index, meal ->
                if (index > 0) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onEditMeal(meal) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Text(
                                text = "${meal.mealType} · ${meal.kcal} kcal",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = meal.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "P ${meal.protein}g · C ${meal.carbs}g · G ${meal.fat}g",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { onRepeatMealToday(meal) },
                        modifier = Modifier.testTag("repeat_meal_${meal.id}")
                    ) {
                        Icon(
                            Icons.Outlined.Replay,
                            contentDescription = "Repetir hoy",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { onDeleteMeal(meal.id) },
                        modifier = Modifier.testTag("history_delete_${meal.id}")
                    ) {
                        Icon(
                            Icons.Outlined.DeleteOutline,
                            contentDescription = "Eliminar comida",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

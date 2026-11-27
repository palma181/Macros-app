package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BookmarkAdded
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.TipsAndUpdates
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.MealType
import com.example.data.model.RecipeCatalog
import com.example.data.model.RecipeDifficulty
import com.example.data.model.RecipeOption
import com.example.data.model.RecipeSection
import com.example.ui.MacroUiState
import com.example.ui.theme.LocalMacroColors
import com.example.util.DateUtils

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RecipesScreen(
    uiState: MacroUiState,
    onSelectSection: (RecipeSection) -> Unit,
    onToggleIngredient: (String) -> Unit,
    onAddCustomIngredient: (String) -> Unit,
    onClearIngredients: () -> Unit,
    onSetDifficulty: (RecipeDifficulty?) -> Unit,
    onGenerateAiRecipes: () -> Unit,
    onOpenRecipeManual: (RecipeOption) -> Unit,
    onCloseRecipeManual: () -> Unit,
    onSaveRecipeToFavorites: (RecipeOption) -> Unit,
    onDeleteSavedRecipe: (String) -> Unit,
    onLogRecipeAsMeal: (RecipeOption, MealType, String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val studio = uiState.recipeStudio
    var customIngredient by rememberSaveable { mutableStateOf("") }
    val isSnacksSection = studio.activeSection == RecipeSection.SNACKS_DRINKS

    val savedIds = remember(studio.savedRecipes) {
        studio.savedRecipes.map { it.id }.toSet()
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize()
                .testTag("recipes_lazy_column"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Section switcher: Platillos vs Snacks y Bebidas
            item {
                RecipeSectionSegmentedBar(
                    activeSection = studio.activeSection,
                    onSelectSection = onSelectSection
                )
            }

            // Header + Custom ingredient input + categorized chips
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isSnacksSection) {
                                    Icons.Outlined.LocalCafe
                                } else {
                                    Icons.Outlined.RestaurantMenu
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = if (isSnacksSection) {
                                        "Snacks, fruta, batidos y suplementos"
                                    } else {
                                        "Cocina por ingredientes"
                                    },
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = if (isSnacksSection) {
                                        "Combina frutas, suplementos y bases para preparar batidos o snacks paso a paso"
                                    } else {
                                        "Elige tus ingredientes, escoge receta y abre su manual paso a paso"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Custom ingredient input
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = customIngredient,
                                onValueChange = { customIngredient = it },
                                placeholder = {
                                    Text(
                                        if (isSnacksSection) {
                                            "Ej. kiwi, espirulina, magnesio, granola..."
                                        } else {
                                            "Ej. camarón, quinoa, calabaza..."
                                        }
                                    )
                                },
                                label = { Text("Agregar otro ingrediente") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("custom_ingredient_input")
                            )
                            Button(
                                onClick = {
                                    if (customIngredient.isNotBlank()) {
                                        onAddCustomIngredient(customIngredient)
                                        customIngredient = ""
                                    }
                                },
                                enabled = customIngredient.isNotBlank(),
                                modifier = Modifier
                                    .height(54.dp)
                                    .testTag("add_custom_ingredient_button")
                            ) {
                                Icon(Icons.Outlined.Add, contentDescription = "Agregar")
                            }
                        }

                        // Active selected ingredients bar
                        AnimatedVisibility(visible = studio.selectedIngredients.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Seleccionados (${studio.selectedIngredients.size})",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    TextButton(
                                        onClick = onClearIngredients,
                                        modifier = Modifier.testTag("clear_ingredients_button")
                                    ) {
                                        Text("Limpiar")
                                    }
                                }
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    studio.selectedIngredients.forEach { ing ->
                                        InputChip(
                                            selected = true,
                                            onClick = { onToggleIngredient(ing) },
                                            label = { Text(ing) },
                                            trailingIcon = {
                                                Icon(
                                                    Icons.Outlined.Close,
                                                    contentDescription = "Quitar $ing",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Categorized ingredient chips for active section
                        RecipeCatalog.ingredientGroupsFor(studio.activeSection).forEach { group ->
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = group.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    group.items.forEach { item ->
                                        val selected = studio.selectedIngredients.any {
                                            it.equals(item, ignoreCase = true)
                                        }
                                        FilterChip(
                                            selected = selected,
                                            onClick = { onToggleIngredient(item) },
                                            leadingIcon = if (selected) {
                                                {
                                                    Icon(
                                                        Icons.Outlined.Check,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            } else null,
                                            label = { Text(item) },
                                            modifier = Modifier.testTag("ing_chip_${RecipeCatalog.normalize(item)}")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Difficulty filter + AI generation trigger
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = studio.difficultyFilter == null,
                            onClick = { onSetDifficulty(null) },
                            label = { Text("Todos los niveles") },
                            modifier = Modifier.testTag("diff_all")
                        )
                        RecipeDifficulty.entries.forEach { level ->
                            FilterChip(
                                selected = studio.difficultyFilter == level,
                                onClick = {
                                    onSetDifficulty(
                                        if (studio.difficultyFilter == level) null else level
                                    )
                                },
                                label = { Text(level.label) },
                                modifier = Modifier.testTag("diff_${level.id}")
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onGenerateAiRecipes,
                        enabled = !studio.isGeneratingAi,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("generate_ai_recipes_button")
                    ) {
                        if (studio.isGeneratingAi) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Creando recetas personalizadas con IA…")
                        } else {
                            Icon(
                                Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (isSnacksSection) {
                                    "Crear más batidos o snacks con IA"
                                } else {
                                    "Crear más opciones con IA (Básico / Intermedio / Avanzado)"
                                }
                            )
                        }
                    }

                    studio.aiError?.let { err ->
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = err,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }

            // AI generated options if present
            if (studio.aiRecipes.isNotEmpty()) {
                item {
                    Text(
                        text = "Opciones creadas por tu IA (${studio.aiRecipes.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                items(studio.aiRecipes, key = { it.id }) { recipe ->
                    RecipeOptionCard(
                        recipe = recipe,
                        matchedIngredients = studio.selectedIngredients,
                        isSaved = savedIds.contains(recipe.id),
                        onOpenManual = { onOpenRecipeManual(recipe) },
                        onToggleSave = {
                            if (savedIds.contains(recipe.id)) {
                                onDeleteSavedRecipe(recipe.id)
                            } else {
                                onSaveRecipeToFavorites(recipe)
                            }
                        }
                    )
                }
            }

            // Curated catalog matches
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when {
                            studio.selectedIngredients.isNotEmpty() ->
                                "Opciones recomendadas (${studio.catalogMatches.size})"
                            isSnacksSection ->
                                "Snacks y batidos (${studio.catalogMatches.size} opciones)"
                            else ->
                                "Recetario (${studio.catalogMatches.size} platillos)"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Toca para ver manual",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(studio.catalogMatches, key = { it.recipe.id }) { match ->
                RecipeOptionCard(
                    recipe = match.recipe,
                    matchedIngredients = match.matchedIngredients,
                    isSaved = savedIds.contains(match.recipe.id),
                    onOpenManual = { onOpenRecipeManual(match.recipe) },
                    onToggleSave = {
                        if (savedIds.contains(match.recipe.id)) {
                            onDeleteSavedRecipe(match.recipe.id)
                        } else {
                            onSaveRecipeToFavorites(match.recipe)
                        }
                    }
                )
            }

            // Saved Recipes Section
            if (studio.savedRecipes.isNotEmpty()) {
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "Mis recetas guardadas (${studio.savedRecipes.size})",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                items(studio.savedRecipes, key = { "saved_${it.id}" }) { saved ->
                    SavedRecipeRow(
                        recipe = saved,
                        onOpenManual = { onOpenRecipeManual(saved) },
                        onDelete = { onDeleteSavedRecipe(saved.id) }
                    )
                }
            }
        }
    }

    studio.activeRecipeManual?.let { recipe ->
        RecipeManualBottomSheet(
            recipe = recipe,
            isSaved = savedIds.contains(recipe.id),
            onDismiss = onCloseRecipeManual,
            onSaveFavorite = { onSaveRecipeToFavorites(recipe) },
            onLogAsMeal = { mealType, alsoSave ->
                onLogRecipeAsMeal(recipe, mealType, DateUtils.todayIso(), alsoSave)
                onCloseRecipeManual()
            }
        )
    }
}

@Composable
private fun difficultyBadgeColors(difficulty: RecipeDifficulty): Pair<Color, Color> {
    val macroColors = LocalMacroColors.current
    return when (difficulty) {
        RecipeDifficulty.BASIC -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        RecipeDifficulty.INTERMEDIATE -> macroColors.carbs.copy(alpha = 0.2f) to macroColors.carbs
        RecipeDifficulty.ADVANCED -> macroColors.protein.copy(alpha = 0.16f) to macroColors.protein
    }
}

@Composable
private fun RecipeOptionCard(
    recipe: RecipeOption,
    matchedIngredients: List<String>,
    isSaved: Boolean,
    onOpenManual: () -> Unit,
    onToggleSave: () -> Unit
) {
    val macroColors = LocalMacroColors.current
    val (badgeBg, badgeFg) = difficultyBadgeColors(recipe.difficulty)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onOpenManual)
            .testTag("recipe_card_${recipe.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(color = badgeBg, shape = RoundedCornerShape(8.dp)) {
                        Text(
                            text = recipe.difficulty.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = badgeFg,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${recipe.prepMinutes} min",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (recipe.isAiGenerated) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "IA",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
                IconButton(
                    onClick = onToggleSave,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Outlined.BookmarkAdded else Icons.Outlined.BookmarkBorder,
                        contentDescription = if (isSaved) "Quitar de guardadas" else "Guardar receta",
                        tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = recipe.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (matchedIngredients.isNotEmpty()) {
                Text(
                    text = "Con tus ingredientes: ${matchedIngredients.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${recipe.kcal} kcal · P ${recipe.protein}g · C ${recipe.carbs}g · G ${recipe.fat}g",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = macroColors.calories
                    )
                    Text(
                        text = "Ver manual",
                        style = MaterialTheme.typography.labelMedium,
                        color = macroColors.calories,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedRecipeRow(
    recipe: RecipeOption,
    onOpenManual: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onOpenManual)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recipe.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val sectionTag = if (recipe.section == RecipeSection.SNACKS_DRINKS) "Snacks/Bebidas · " else ""
                Text(
                    text = "$sectionTag${recipe.difficulty.label} · ${recipe.kcal} kcal · P${recipe.protein} C${recipe.carbs} G${recipe.fat}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    contentDescription = "Eliminar receta guardada",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RecipeSectionSegmentedBar(
    activeSection: RecipeSection,
    onSelectSection: (RecipeSection) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            RecipeSection.entries.forEach { section ->
                val selected = activeSection == section
                val bgColor by animateColorAsState(
                    targetValue = if (selected) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        Color.Transparent
                    },
                    label = "recipe_section_bg_${section.id}"
                )
                val fgColor by animateColorAsState(
                    targetValue = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    label = "recipe_section_fg_${section.id}"
                )
                val icon = if (section == RecipeSection.SNACKS_DRINKS) {
                    Icons.Outlined.LocalCafe
                } else {
                    Icons.Outlined.RestaurantMenu
                }

                Surface(
                    color = bgColor,
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = if (selected) 2.dp else 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectSection(section) }
                        .testTag("section_${section.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = fgColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = section.label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = fgColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun RecipeManualBottomSheet(
    recipe: RecipeOption,
    isSaved: Boolean,
    onDismiss: () -> Unit,
    onSaveFavorite: () -> Unit,
    onLogAsMeal: (MealType, Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedMealType by rememberSaveable(recipe.id) {
        mutableStateOf(
            if (recipe.section == RecipeSection.SNACKS_DRINKS) MealType.SNACK else MealType.LUNCH
        )
    }
    var alsoSaveToFavorites by rememberSaveable(recipe.id) { mutableStateOf(true) }
    val (badgeBg, badgeFg) = difficultyBadgeColors(recipe.difficulty)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(color = badgeBg, shape = RoundedCornerShape(8.dp)) {
                            Text(
                                text = recipe.difficulty.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = badgeFg,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            text = "${recipe.prepMinutes} min · ${recipe.kcal} kcal",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = recipe.title,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "Cerrar manual")
                }
            }

            // Macros summary strip
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    MacroPillColumn("Calorías", "${recipe.kcal} kcal")
                    MacroPillColumn("Proteína", "${recipe.protein} g")
                    MacroPillColumn("Carbs", "${recipe.carbs} g")
                    MacroPillColumn("Grasa", "${recipe.fat} g")
                }
            }

            // Exact ingredients
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Ingredientes (1 porción)",
                    style = MaterialTheme.typography.titleMedium
                )
                recipe.ingredients.forEach { ing ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 7.dp)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Text(
                            text = ing,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Step-by-step cooking manual
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Manual de preparación",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                recipe.steps.forEachIndexed { idx, step ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${idx + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = step,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (recipe.tips.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Outlined.TipsAndUpdates,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = recipe.tips,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            HorizontalDivider()

            // Save for a single meal
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Guardar como una comida de hoy",
                    style = MaterialTheme.typography.titleSmall
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    MealType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedMealType == type,
                            onClick = { selectedMealType = type },
                            label = { Text(type.label) }
                        )
                    }
                }

                if (!isSaved) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { alsoSaveToFavorites = !alsoSaveToFavorites }
                    ) {
                        Checkbox(
                            checked = alsoSaveToFavorites,
                            onCheckedChange = { alsoSaveToFavorites = it }
                        )
                        Text(
                            text = "Guardar también en Mis recetas guardadas",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!isSaved) {
                        OutlinedButton(
                            onClick = onSaveFavorite,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("save_recipe_favorite_button")
                        ) {
                            Text("Solo guardar", maxLines = 1)
                        }
                    }
                    Button(
                        onClick = { onLogAsMeal(selectedMealType, alsoSaveToFavorites) },
                        modifier = Modifier
                            .weight(1.4f)
                            .height(48.dp)
                            .testTag("log_recipe_as_meal_button")
                    ) {
                        Text("Guardar comida hoy", maxLines = 1)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun MacroPillColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

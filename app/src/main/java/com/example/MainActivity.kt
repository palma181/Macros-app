package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MacroViewModel
import com.example.ui.components.MealEditorBottomSheet
import com.example.ui.screens.AiScreen
import com.example.ui.screens.GoalsScreen
import com.example.ui.screens.MealsHistoryScreen
import com.example.ui.screens.ProgressScreen
import com.example.ui.screens.RecipesScreen
import com.example.ui.screens.TodayScreen
import com.example.ui.screens.WeightScreen
import com.example.ui.theme.MisMacrosTheme
import com.example.util.DateUtils
import kotlinx.coroutines.launch

private const val PAGE_TODAY = 0
private const val PAGE_MEALS = 1
private const val PAGE_RECIPES = 2
private const val PAGE_PROGRESS = 3
private const val PAGE_WEIGHT = 4
private const val PAGE_AI = 5
private const val PAGE_GOALS = 6

private data class NavDestination(
    val pageIndex: Int,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val Destinations = listOf(
    NavDestination(PAGE_TODAY, R.string.nav_today, Icons.Filled.Restaurant, Icons.Outlined.Restaurant, "nav_today"),
    NavDestination(PAGE_MEALS, R.string.nav_meals, Icons.Filled.History, Icons.Outlined.History, "nav_meals"),
    NavDestination(PAGE_RECIPES, R.string.nav_recipes, Icons.Filled.RestaurantMenu, Icons.Outlined.RestaurantMenu, "nav_recipes"),
    NavDestination(PAGE_PROGRESS, R.string.nav_progress, Icons.Filled.BarChart, Icons.Outlined.BarChart, "nav_progress"),
    NavDestination(PAGE_WEIGHT, R.string.nav_weight, Icons.Filled.MonitorWeight, Icons.Outlined.MonitorWeight, "nav_weight"),
    NavDestination(PAGE_AI, R.string.nav_ai, Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome, "nav_ai"),
    NavDestination(PAGE_GOALS, R.string.nav_goals, Icons.Filled.Tune, Icons.Outlined.Tune, "nav_goals")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MisMacrosTheme {
                MisMacrosApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisMacrosApp() {
    val context = LocalContext.current
    val viewModel: MacroViewModel = viewModel(factory = MacroViewModel.Factory(context))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val pagerState = rememberPagerState(initialPage = PAGE_TODAY, pageCount = { Destinations.size })
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    fun animateToPage(pageIndex: Int) {
        coroutineScope.launch {
            pagerState.animateScrollToPage(
                page = pageIndex,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    BackHandler(enabled = pagerState.currentPage != PAGE_TODAY && !uiState.editor.isOpen && uiState.recipeStudio.activeRecipeManual == null) {
        animateToPage(PAGE_TODAY)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = stringResource(Destinations[pagerState.currentPage].labelRes),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            floatingActionButton = {
                if (pagerState.currentPage == PAGE_TODAY) {
                    ExtendedFloatingActionButton(
                        onClick = { viewModel.openAddMealSheet(null) },
                        icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                        text = { Text("Comida") },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("fab_add_meal")
                    )
                }
            },
            bottomBar = {
                if (!useRail) {
                    ScrollablePillBottomBar(
                        currentPage = pagerState.currentPage,
                        onSelectPage = ::animateToPage
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        Destinations.forEach { dest ->
                            val selected = pagerState.currentPage == dest.pageIndex
                            val label = stringResource(dest.labelRes)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { animateToPage(dest.pageIndex) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label, maxLines = 1) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    beyondViewportPageCount = 1,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .testTag("main_horizontal_pager")
                ) { page ->
                    when (page) {
                        PAGE_TODAY -> TodayScreen(
                            uiState = uiState,
                            onShiftDate = viewModel::shiftSelectedDate,
                            onResetToToday = { viewModel.selectDate(DateUtils.todayIso()) },
                            onOpenAddMeal = viewModel::openAddMealSheet,
                            onEditMeal = viewModel::openEditMealSheet,
                            onDeleteMeal = viewModel::deleteMeal,
                            onNavigateToAi = { animateToPage(PAGE_AI) }
                        )

                        PAGE_MEALS -> MealsHistoryScreen(
                            uiState = uiState,
                            onEditMeal = viewModel::openEditMealSheet,
                            onDeleteMeal = viewModel::deleteMeal,
                            onRepeatMealToday = { meal ->
                                viewModel.repeatMealForToday(meal)
                            },
                            onOpenAddMeal = { viewModel.openAddMealSheet(null) }
                        )

                        PAGE_RECIPES -> RecipesScreen(
                            uiState = uiState,
                            onSelectSection = viewModel::setRecipeSection,
                            onToggleIngredient = viewModel::toggleIngredient,
                            onAddCustomIngredient = viewModel::addCustomIngredient,
                            onClearIngredients = viewModel::clearIngredients,
                            onSetDifficulty = viewModel::setDifficultyFilter,
                            onGenerateAiRecipes = viewModel::generateAiRecipes,
                            onOpenRecipeManual = viewModel::openRecipeManual,
                            onCloseRecipeManual = viewModel::closeRecipeManual,
                            onSaveRecipeToFavorites = viewModel::saveRecipeToFavorites,
                            onDeleteSavedRecipe = viewModel::deleteSavedRecipe,
                            onLogRecipeAsMeal = viewModel::logRecipeAsMeal
                        )

                        PAGE_PROGRESS -> ProgressScreen(uiState = uiState)

                        PAGE_WEIGHT -> WeightScreen(
                            uiState = uiState,
                            onSaveWeight = viewModel::saveWeight,
                            onDeleteWeight = viewModel::deleteWeight
                        )

                        PAGE_AI -> AiScreen(
                            uiState = uiState,
                            onSaveConfig = viewModel::saveAiConfig,
                            onTestConnection = viewModel::testAiConnection,
                            onAnalyzeWeek = viewModel::analyzeWeekWithCoach,
                            onAnalyzeMonth = viewModel::analyzeMonthWithCoach,
                            onAskCoach = viewModel::askCoach,
                            onCopiedMessage = viewModel::showSnackbar
                        )

                        PAGE_GOALS -> GoalsScreen(
                            uiState = uiState,
                            onSaveGoals = viewModel::saveGoals,
                            onExportBackup = { uri -> viewModel.exportBackupToUri(context, uri) },
                            onImportBackup = { uri -> viewModel.importBackupFromUri(context, uri) }
                        )
                    }
                }
            }
        }
    }

    MealEditorBottomSheet(
        state = uiState.editor,
        onDismiss = viewModel::closeMealSheet,
        onUpdateFields = viewModel::updateEditorFields,
        onEstimateText = viewModel::estimateMealFromText,
        onPhotoSelected = viewModel::analyzePhoto,
        onSave = viewModel::saveMealFromEditor
    )
}

@Composable
private fun ScrollablePillBottomBar(
    currentPage: Int,
    onSelectPage: (Int) -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(currentPage) {
        listState.animateScrollToItem(index = currentPage)
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            LazyRow(
                state = listState,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bottom_pill_bar")
            ) {
                itemsIndexed(Destinations, key = { _, d -> d.pageIndex }) { _, dest ->
                    val selected = currentPage == dest.pageIndex
                    val label = stringResource(dest.labelRes)
                    val bgColor by animateColorAsState(
                        targetValue = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        },
                        label = "tab_bg_${dest.pageIndex}"
                    )
                    val fgColor by animateColorAsState(
                        targetValue = if (selected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        label = "tab_fg_${dest.pageIndex}"
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(bgColor)
                            .clickable { onSelectPage(dest.pageIndex) }
                            .padding(horizontal = 14.dp)
                            .testTag(dest.testTag)
                    ) {
                        Icon(
                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                            contentDescription = label,
                            tint = fgColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = fgColor,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

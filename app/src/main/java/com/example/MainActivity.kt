package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.MacroViewModel
import com.example.ui.components.MealEditorBottomSheet
import com.example.ui.screens.AiScreen
import com.example.ui.screens.GoalsScreen
import com.example.ui.screens.ProgressScreen
import com.example.ui.screens.TodayScreen
import com.example.ui.screens.WeightScreen
import com.example.ui.theme.MisMacrosTheme
import com.example.util.DateUtils

const val ROUTE_TODAY = "today"
const val ROUTE_PROGRESS = "progress"
const val ROUTE_WEIGHT = "weight"
const val ROUTE_AI = "ai"
const val ROUTE_GOALS = "goals"

private data class NavDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val Destinations = listOf(
    NavDestination(ROUTE_TODAY, R.string.nav_today, Icons.Filled.Restaurant, Icons.Outlined.Restaurant, "nav_today"),
    NavDestination(ROUTE_PROGRESS, R.string.nav_progress, Icons.Filled.BarChart, Icons.Outlined.BarChart, "nav_progress"),
    NavDestination(ROUTE_WEIGHT, R.string.nav_weight, Icons.Filled.MonitorWeight, Icons.Outlined.MonitorWeight, "nav_weight"),
    NavDestination(ROUTE_AI, R.string.nav_ai, Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome, "nav_ai"),
    NavDestination(ROUTE_GOALS, R.string.nav_goals, Icons.Filled.Tune, Icons.Outlined.Tune, "nav_goals")
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

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_TODAY
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
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
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineSmall
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            floatingActionButton = {
                if (currentRoute == ROUTE_TODAY) {
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
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Destinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            val label = stringResource(dest.labelRes)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
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
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Destinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            val label = stringResource(dest.labelRes)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
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

                Box(modifier = Modifier.weight(1f)) {
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_TODAY
                    ) {
                        composable(ROUTE_TODAY) {
                            TodayScreen(
                                uiState = uiState,
                                onShiftDate = viewModel::shiftSelectedDate,
                                onResetToToday = { viewModel.selectDate(DateUtils.todayIso()) },
                                onOpenAddMeal = viewModel::openAddMealSheet,
                                onEditMeal = viewModel::openEditMealSheet,
                                onDeleteMeal = viewModel::deleteMeal,
                                onNavigateToAi = { navigateTo(ROUTE_AI) }
                            )
                        }
                        composable(ROUTE_PROGRESS) {
                            ProgressScreen(uiState = uiState)
                        }
                        composable(ROUTE_WEIGHT) {
                            WeightScreen(
                                uiState = uiState,
                                onSaveWeight = viewModel::saveWeight,
                                onDeleteWeight = viewModel::deleteWeight
                            )
                        }
                        composable(ROUTE_AI) {
                            AiScreen(
                                uiState = uiState,
                                onSaveConfig = viewModel::saveAiConfig,
                                onTestConnection = viewModel::testAiConnection,
                                onAnalyzeWeek = viewModel::analyzeWeekWithCoach,
                                onAnalyzeMonth = viewModel::analyzeMonthWithCoach,
                                onAskCoach = viewModel::askCoach,
                                onCopiedMessage = viewModel::showSnackbar
                            )
                        }
                        composable(ROUTE_GOALS) {
                            GoalsScreen(
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

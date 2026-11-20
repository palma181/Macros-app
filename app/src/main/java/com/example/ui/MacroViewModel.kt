package com.example.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.MealEntity
import com.example.data.local.WeightEntity
import com.example.data.model.AiConfig
import com.example.data.model.DayCalories
import com.example.data.model.MacroGoals
import com.example.data.model.MacroTotals
import com.example.data.model.MealType
import com.example.data.preferences.AppPreferencesRepository
import com.example.data.repository.MacroRepository
import com.example.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MealEditorState(
    val isOpen: Boolean = false,
    val editingId: Long? = null,
    val name: String = "",
    val mealType: MealType = MealType.LUNCH,
    val date: String = DateUtils.todayIso(),
    val kcalText: String = "",
    val proteinText: String = "",
    val carbsText: String = "",
    val fatText: String = "",
    val photoUri: Uri? = null,
    val isAnalyzing: Boolean = false,
    val aiStatus: String? = null,
    val aiError: String? = null
)

data class CoachState(
    val isLoading: Boolean = false,
    val response: String = "",
    val error: String? = null
)

data class ConnectionTestState(
    val isTesting: Boolean = false,
    val message: String? = null,
    val isSuccess: Boolean = false
)

data class PeriodSummary(
    val startDate: String = DateUtils.todayIso(),
    val endDate: String = DateUtils.todayIso(),
    val daysCount: Int = 7,
    val totals: MacroTotals = MacroTotals(),
    val activeDaysCount: Int = 0,
    val dailyBreakdown: List<DayCalories> = emptyList()
)

data class MacroUiState(
    val selectedDate: String = DateUtils.todayIso(),
    val goals: MacroGoals = MacroGoals(),
    val aiConfig: AiConfig = AiConfig(),
    val dayMeals: List<MealEntity> = emptyList(),
    val dayTotals: MacroTotals = MacroTotals(),
    val weekSummary: PeriodSummary = PeriodSummary(),
    val monthSummary: PeriodSummary = PeriodSummary(),
    val weights: List<WeightEntity> = emptyList(),
    val editor: MealEditorState = MealEditorState(),
    val coach: CoachState = CoachState(),
    val connectionTest: ConnectionTestState = ConnectionTestState(),
    val snackbarMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class MacroViewModel(
    private val repository: MacroRepository
) : ViewModel() {

    private val selectedDateFlow = MutableStateFlow(DateUtils.todayIso())
    private val editorFlow = MutableStateFlow(MealEditorState())
    private val coachFlow = MutableStateFlow(CoachState())
    private val connectionTestFlow = MutableStateFlow(ConnectionTestState())
    private val snackbarFlow = MutableStateFlow<String?>(null)

    private val dayMealsFlow = selectedDateFlow.flatMapLatest { date ->
        repository.observeMealsInRange(date, date)
    }

    private val weekMealsFlow = selectedDateFlow.flatMapLatest { date ->
        val start = DateUtils.mondayOfWeek(date)
        val end = DateUtils.addDays(start, 6)
        repository.observeMealsInRange(start, end)
    }

    private val monthMealsFlow = selectedDateFlow.flatMapLatest { date ->
        val start = DateUtils.firstDayOfMonth(date)
        val end = DateUtils.lastDayOfMonth(date)
        repository.observeMealsInRange(start, end)
    }

    private val coreDataFlow = combine(
        selectedDateFlow,
        repository.goalsFlow,
        repository.aiConfigFlow,
        dayMealsFlow,
        repository.allWeightsFlow
    ) { date, goals, aiConfig, dayMeals, weights ->
        CoreData(date, goals, aiConfig, dayMeals, weights)
    }

    private val periodDataFlow = combine(
        selectedDateFlow,
        weekMealsFlow,
        monthMealsFlow
    ) { date, weekMeals, monthMeals ->
        buildPeriods(date, weekMeals, monthMeals)
    }

    val uiState: StateFlow<MacroUiState> = combine(
        coreDataFlow,
        periodDataFlow,
        editorFlow,
        coachFlow,
        combine(connectionTestFlow, snackbarFlow) { a, b -> a to b }
    ) { core, periods, editor, coach, (connTest, snack) ->
        MacroUiState(
            selectedDate = core.selectedDate,
            goals = core.goals,
            aiConfig = core.aiConfig,
            dayMeals = core.dayMeals,
            dayTotals = sumMeals(core.dayMeals),
            weekSummary = periods.first,
            monthSummary = periods.second,
            weights = core.weights,
            editor = editor,
            coach = coach,
            connectionTest = connTest,
            snackbarMessage = snack
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MacroUiState()
    )

    private data class CoreData(
        val selectedDate: String,
        val goals: MacroGoals,
        val aiConfig: AiConfig,
        val dayMeals: List<MealEntity>,
        val weights: List<WeightEntity>
    )

    private fun sumMeals(meals: List<MealEntity>): MacroTotals =
        meals.fold(MacroTotals()) { acc, m ->
            MacroTotals(
                kcal = acc.kcal + m.kcal,
                protein = acc.protein + m.protein,
                carbs = acc.carbs + m.carbs,
                fat = acc.fat + m.fat
            )
        }

    private fun buildPeriods(
        anchorDate: String,
        weekMeals: List<MealEntity>,
        monthMeals: List<MealEntity>
    ): Pair<PeriodSummary, PeriodSummary> {
        val today = DateUtils.todayIso()
        val weekStart = DateUtils.mondayOfWeek(anchorDate)
        val weekEnd = DateUtils.addDays(weekStart, 6)
        val kcalByDay = weekMeals.groupBy { it.date }.mapValues { (_, list) -> list.sumOf { it.kcal } }
        val dailyBreakdown = (0..6).map { offset ->
            val d = DateUtils.addDays(weekStart, offset)
            DayCalories(
                date = d,
                dayInitial = DateUtils.weekDayInitial(offset),
                kcal = kcalByDay[d] ?: 0,
                isToday = d == today
            )
        }
        val weekSummary = PeriodSummary(
            startDate = weekStart,
            endDate = weekEnd,
            daysCount = 7,
            totals = sumMeals(weekMeals),
            activeDaysCount = weekMeals.map { it.date }.distinct().size,
            dailyBreakdown = dailyBreakdown
        )

        val monthStart = DateUtils.firstDayOfMonth(anchorDate)
        val monthDays = DateUtils.daysInMonth(anchorDate)
        val monthEnd = DateUtils.lastDayOfMonth(anchorDate)
        val monthSummary = PeriodSummary(
            startDate = monthStart,
            endDate = monthEnd,
            daysCount = monthDays,
            totals = sumMeals(monthMeals),
            activeDaysCount = monthMeals.map { it.date }.distinct().size
        )
        return weekSummary to monthSummary
    }

    fun shiftSelectedDate(deltaDays: Int) {
        selectedDateFlow.update { DateUtils.addDays(it, deltaDays) }
    }

    fun selectDate(isoDate: String) {
        selectedDateFlow.value = isoDate
    }

    fun openAddMealSheet(initialPhotoUri: Uri? = null) {
        editorFlow.value = MealEditorState(
            isOpen = true,
            date = selectedDateFlow.value,
            photoUri = initialPhotoUri
        )
        if (initialPhotoUri != null) {
            analyzePhoto(initialPhotoUri)
        }
    }

    fun openEditMealSheet(meal: MealEntity) {
        editorFlow.value = MealEditorState(
            isOpen = true,
            editingId = meal.id,
            name = meal.name,
            mealType = MealType.fromLabel(meal.mealType),
            date = meal.date,
            kcalText = meal.kcal.toString(),
            proteinText = meal.protein.toString(),
            carbsText = meal.carbs.toString(),
            fatText = meal.fat.toString()
        )
    }

    fun closeMealSheet() {
        editorFlow.update { it.copy(isOpen = false, isAnalyzing = false) }
    }

    fun updateEditorFields(
        name: String? = null,
        mealType: MealType? = null,
        date: String? = null,
        kcalText: String? = null,
        proteinText: String? = null,
        carbsText: String? = null,
        fatText: String? = null
    ) {
        editorFlow.update { current ->
            current.copy(
                name = name ?: current.name,
                mealType = mealType ?: current.mealType,
                date = date ?: current.date,
                kcalText = kcalText ?: current.kcalText,
                proteinText = proteinText ?: current.proteinText,
                carbsText = carbsText ?: current.carbsText,
                fatText = fatText ?: current.fatText
            )
        }
    }

    fun estimateMealFromText() {
        val description = editorFlow.value.name.trim()
        if (description.isBlank()) {
            editorFlow.update {
                it.copy(aiError = "Escribe primero qué comiste para estimar sus macros.", aiStatus = null)
            }
            return
        }
        viewModelScope.launch {
            editorFlow.update { it.copy(isAnalyzing = true, aiStatus = "Estimando macros con IA…", aiError = null) }
            repository.estimateFromText(description).fold(
                onSuccess = { est ->
                    editorFlow.update {
                        it.copy(
                            isAnalyzing = false,
                            kcalText = est.kcal.toString(),
                            proteinText = est.protein.toString(),
                            carbsText = est.carbs.toString(),
                            fatText = est.fat.toString(),
                            aiStatus = "Estimación lista. Revisa y ajusta si lo deseas.",
                            aiError = null
                        )
                    }
                },
                onFailure = { err ->
                    editorFlow.update {
                        it.copy(
                            isAnalyzing = false,
                            aiStatus = null,
                            aiError = err.message ?: "Error al estimar con IA"
                        )
                    }
                }
            )
        }
    }

    fun analyzePhoto(uri: Uri) {
        val hint = editorFlow.value.name.trim().ifBlank { null }
        viewModelScope.launch {
            editorFlow.update {
                it.copy(
                    photoUri = uri,
                    isAnalyzing = true,
                    aiStatus = "Analizando la foto con IA…",
                    aiError = null
                )
            }
            repository.estimateFromImageUri(uri, hint).fold(
                onSuccess = { est ->
                    editorFlow.update { current ->
                        current.copy(
                            isAnalyzing = false,
                            name = if (current.name.isBlank()) (est.name ?: "") else current.name,
                            kcalText = est.kcal.toString(),
                            proteinText = est.protein.toString(),
                            carbsText = est.carbs.toString(),
                            fatText = est.fat.toString(),
                            aiStatus = "Foto analizada. La imagen no se guarda en disco.",
                            aiError = null
                        )
                    }
                },
                onFailure = { err ->
                    editorFlow.update {
                        it.copy(
                            isAnalyzing = false,
                            aiStatus = null,
                            aiError = err.message ?: "No se pudo analizar la foto"
                        )
                    }
                }
            )
        }
    }

    fun saveMealFromEditor() {
        val draft = editorFlow.value
        val cleanName = draft.name.trim()
        if (cleanName.isBlank()) {
            editorFlow.update { it.copy(aiError = "Ingresa una descripción para la comida.") }
            return
        }
        val meal = MealEntity(
            id = draft.editingId ?: 0L,
            date = draft.date.ifBlank { DateUtils.todayIso() },
            mealType = draft.mealType.label,
            name = cleanName,
            kcal = draft.kcalText.toIntOrNull()?.coerceAtLeast(0) ?: 0,
            protein = draft.proteinText.toIntOrNull()?.coerceAtLeast(0) ?: 0,
            carbs = draft.carbsText.toIntOrNull()?.coerceAtLeast(0) ?: 0,
            fat = draft.fatText.toIntOrNull()?.coerceAtLeast(0) ?: 0
        )
        viewModelScope.launch {
            if (draft.editingId != null) {
                repository.updateMeal(meal)
                showSnackbar("Comida actualizada")
            } else {
                repository.insertMeal(meal)
                showSnackbar("Comida guardada")
            }
            editorFlow.value = MealEditorState(isOpen = false)
        }
    }

    fun deleteMeal(id: Long) {
        viewModelScope.launch {
            repository.deleteMeal(id)
            showSnackbar("Comida eliminada")
        }
    }

    fun saveWeight(date: String, kg: Double) {
        if (kg <= 0.0) return
        viewModelScope.launch {
            repository.saveWeight(date, kg)
            showSnackbar("Peso registrado: $kg kg")
        }
    }

    fun deleteWeight(date: String) {
        viewModelScope.launch {
            repository.deleteWeight(date)
            showSnackbar("Registro de peso eliminado")
        }
    }

    fun saveGoals(goals: MacroGoals) {
        viewModelScope.launch {
            repository.saveGoals(goals)
            showSnackbar("Metas diarias guardadas")
        }
    }

    fun saveAiConfig(config: AiConfig) {
        viewModelScope.launch {
            repository.saveAiConfig(config)
            connectionTestFlow.value = ConnectionTestState()
            showSnackbar("Configuración de IA guardada")
        }
    }

    fun testAiConnection(config: AiConfig) {
        viewModelScope.launch {
            repository.saveAiConfig(config)
            connectionTestFlow.value = ConnectionTestState(isTesting = true)
            repository.testAiConnection(config).fold(
                onSuccess = { msg ->
                    connectionTestFlow.value = ConnectionTestState(
                        isTesting = false,
                        message = msg,
                        isSuccess = true
                    )
                },
                onFailure = { err ->
                    connectionTestFlow.value = ConnectionTestState(
                        isTesting = false,
                        message = err.message ?: "Error de conexión",
                        isSuccess = false
                    )
                }
            )
        }
    }

    fun analyzeWeekWithCoach() {
        val week = uiState.value.weekSummary
        viewModelScope.launch {
            coachFlow.update { it.copy(isLoading = true, error = null) }
            repository.analyzePeriodWithCoach("semana", 7, week.startDate, week.endDate).fold(
                onSuccess = { reply -> coachFlow.value = CoachState(isLoading = false, response = reply) },
                onFailure = { err -> coachFlow.value = CoachState(isLoading = false, error = err.message) }
            )
        }
    }

    fun analyzeMonthWithCoach() {
        val month = uiState.value.monthSummary
        viewModelScope.launch {
            coachFlow.update { it.copy(isLoading = true, error = null) }
            repository.analyzePeriodWithCoach("mes", month.daysCount, month.startDate, month.endDate).fold(
                onSuccess = { reply -> coachFlow.value = CoachState(isLoading = false, response = reply) },
                onFailure = { err -> coachFlow.value = CoachState(isLoading = false, error = err.message) }
            )
        }
    }

    fun askCoach(question: String) {
        if (question.isBlank()) return
        viewModelScope.launch {
            coachFlow.update { it.copy(isLoading = true, error = null) }
            repository.askCoachQuestion(question, DateUtils.todayIso()).fold(
                onSuccess = { reply -> coachFlow.value = CoachState(isLoading = false, response = reply) },
                onFailure = { err -> coachFlow.value = CoachState(isLoading = false, error = err.message) }
            )
        }
    }

    fun exportBackupToUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val json = repository.exportBackupJson()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    } ?: error("No se pudo abrir el destino para guardar")
                }
            }.fold(
                onSuccess = { showSnackbar("Respaldo exportado correctamente") },
                onFailure = { showSnackbar("Error al exportar: ${it.message}") }
            )
        }
    }

    fun importBackupFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            val rawResult = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: error("No se pudo leer el archivo")
                }
            }
            rawResult.fold(
                onSuccess = { raw ->
                    repository.importBackupJson(raw).fold(
                        onSuccess = { (mealsCount, weightsCount) ->
                            showSnackbar("Importado: $mealsCount comidas y $weightsCount pesos")
                        },
                        onFailure = { showSnackbar("Archivo JSON no válido: ${it.message}") }
                    )
                },
                onFailure = { showSnackbar("No se pudo abrir el archivo: ${it.message}") }
            )
        }
    }

    fun clearSnackbar() {
        snackbarFlow.value = null
    }

    fun showSnackbar(message: String) {
        snackbarFlow.value = message
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = AppDatabase.getInstance(context)
            val prefs = AppPreferencesRepository(context)
            val repo = MacroRepository(context.applicationContext, db.macroDao(), prefs)
            return MacroViewModel(repo) as T
        }
    }
}

package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AiConfig
import com.example.data.model.AiPreset
import com.example.ui.MacroUiState
import com.example.ui.components.bringIntoViewOnFocus
import com.example.util.ExternalAiBridge

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiScreen(
    uiState: MacroUiState,
    onSaveConfig: (AiConfig) -> Unit,
    onTestConnection: (AiConfig) -> Unit,
    onAnalyzeWeek: () -> Unit,
    onAnalyzeMonth: () -> Unit,
    onAskCoach: (String) -> Unit,
    onCopiedMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val saved = uiState.aiConfig
    var selectedPreset by remember(saved) { mutableStateOf(saved.preset) }
    var baseUrl by remember(saved) { mutableStateOf(saved.baseUrl) }
    var apiKey by remember(saved) { mutableStateOf(saved.apiKey) }
    var model by remember(saved) { mutableStateOf(saved.model) }
    var showKey by rememberSaveable { mutableStateOf(false) }
    var question by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(saved) {
        selectedPreset = saved.preset
        baseUrl = saved.baseUrl
        apiKey = saved.apiKey
        model = saved.model
    }

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    fun currentDraftConfig() = AiConfig(
        preset = selectedPreset,
        baseUrl = baseUrl.trim(),
        apiKey = apiKey.trim(),
        model = model.trim()
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize()
                .testTag("ai_lazy_column"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Connection Card
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
                            text = stringResource(R.string.ai_connection_title),
                            style = MaterialTheme.typography.titleLarge
                        )

                        Text(
                            text = "Opciones gratuitas y compatibles con OpenAI",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AiPreset.entries.forEach { preset ->
                                FilterChip(
                                    selected = selectedPreset == preset,
                                    onClick = {
                                        selectedPreset = preset
                                        if (preset != AiPreset.CUSTOM) {
                                            baseUrl = preset.defaultUrl
                                            model = preset.defaultModel
                                        }
                                    },
                                    label = { Text(preset.displayName) },
                                    modifier = Modifier.testTag("ai_preset_${preset.id}")
                                )
                            }
                        }

                        if (selectedPreset.signupUrl.isNotBlank()) {
                            Surface(
                                color = if (selectedPreset.isFreeTier) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                },
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (selectedPreset.isFreeTier) {
                                            "Clave gratuita disponible"
                                        } else {
                                            "Consola de claves"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    TextButton(
                                        onClick = {
                                            runCatching {
                                                context.startActivity(
                                                    Intent(Intent.ACTION_VIEW, Uri.parse(selectedPreset.signupUrl))
                                                )
                                            }
                                        },
                                        modifier = Modifier.testTag("ai_get_key_button")
                                    ) {
                                        Text(
                                            text = if (selectedPreset.isFreeTier) "Obtener gratis" else "Abrir consola"
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            Icons.AutoMirrored.Outlined.OpenInNew,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = baseUrl,
                            onValueChange = { baseUrl = it },
                            label = { Text("URL base (API compatible con OpenAI)") },
                            placeholder = { Text("https://api.openai.com/v1") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .bringIntoViewOnFocus()
                                .testTag("ai_url_input")
                        )

                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it },
                            label = { Text("Clave de API") },
                            placeholder = { Text("sk-...") },
                            singleLine = true,
                            visualTransformation = if (showKey) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            trailingIcon = {
                                IconButton(onClick = { showKey = !showKey }) {
                                    Icon(
                                        imageVector = if (showKey) {
                                            Icons.Outlined.VisibilityOff
                                        } else {
                                            Icons.Outlined.Visibility
                                        },
                                        contentDescription = if (showKey) "Ocultar clave" else "Mostrar clave"
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .bringIntoViewOnFocus()
                                .testTag("ai_key_input")
                        )

                        OutlinedTextField(
                            value = model,
                            onValueChange = { model = it },
                            label = { Text("Modelo") },
                            supportingText = { Text(selectedPreset.supportsVisionHint) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .bringIntoViewOnFocus()
                                .testTag("ai_model_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onTestConnection(currentDraftConfig()) },
                                enabled = !uiState.connectionTest.isTesting,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("ai_test_button")
                            ) {
                                if (uiState.connectionTest.isTesting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Probando…")
                                } else {
                                    Icon(
                                        Icons.Outlined.WifiTethering,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Probar")
                                }
                            }
                            Button(
                                onClick = { onSaveConfig(currentDraftConfig()) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("ai_save_button")
                            ) {
                                Text("Guardar")
                            }
                        }

                        AnimatedVisibility(visible = uiState.connectionTest.message != null) {
                            val test = uiState.connectionTest
                            Surface(
                                color = if (test.isSuccess) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.errorContainer
                                },
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (test.isSuccess) {
                                            Icons.Outlined.CheckCircle
                                        } else {
                                            Icons.Outlined.ErrorOutline
                                        },
                                        contentDescription = null,
                                        tint = if (test.isSuccess) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onErrorContainer
                                        },
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = test.message.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (test.isSuccess) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onErrorContainer
                                        }
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Tu clave se guarda solo en este teléfono y no se incluye en respaldos ni en la nube.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // No-API-Key Card: Use user's ChatGPT / Claude app directly
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                    ),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Usar ChatGPT o Claude sin API Key",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Text(
                            text = "ChatGPT y Claude no permiten iniciar sesión desde apps externas sin pagar API. Para no gastar, usa cualquiera de los proveedores gratis de arriba (Gemini, Groq, GitHub, Mistral, Cerebras, SambaNova, HuggingFace u OpenRouter), o envía tu resumen directo a ChatGPT o Claude:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val week = uiState.weekSummary
                                    val weekMeals = uiState.allMeals.filter {
                                        it.date in week.startDate..week.endDate
                                    }
                                    val prompt = ExternalAiBridge.buildCoachPeriodPrompt(
                                        periodLabel = "semana",
                                        daysCount = 7,
                                        goals = uiState.goals,
                                        meals = weekMeals
                                    )
                                    ExternalAiBridge.sharePromptToExternalApp(
                                        context = context,
                                        prompt = prompt,
                                        chooserTitle = "Enviar a ChatGPT o Claude"
                                    )
                                    onCopiedMessage("Prompt copiado. Elige ChatGPT o Claude.")
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("external_ai_week_button")
                            ) {
                                Icon(
                                    Icons.Outlined.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Enviar semana", maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = {
                                    val prompt = ExternalAiBridge.buildCoachQuestionPrompt(
                                        question = question.ifBlank { "¿Qué me recomiendas cenar hoy para completar mis macros?" },
                                        goals = uiState.goals,
                                        todayTotals = uiState.dayTotals
                                    )
                                    ExternalAiBridge.sharePromptToExternalApp(
                                        context = context,
                                        prompt = prompt,
                                        chooserTitle = "Enviar pregunta a ChatGPT o Claude"
                                    )
                                    onCopiedMessage("Pregunta copiada. Elige ChatGPT o Claude.")
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("external_ai_question_button")
                            ) {
                                Icon(
                                    Icons.Outlined.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Enviar duda", maxLines = 1)
                            }
                        }
                    }
                }
            }

            // Coach IA Card
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.ai_coach_title),
                                style = MaterialTheme.typography.titleLarge
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onAnalyzeWeek,
                                enabled = !uiState.coach.isLoading,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("coach_week_button")
                            ) {
                                Text(stringResource(R.string.analyze_week), maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = onAnalyzeMonth,
                                enabled = !uiState.coach.isLoading,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("coach_month_button")
                            ) {
                                Text(stringResource(R.string.analyze_month), maxLines = 1)
                            }
                        }

                        OutlinedTextField(
                            value = question,
                            onValueChange = { question = it },
                            label = { Text("Pregúntale a tu Coach") },
                            placeholder = { Text("¿Qué ceno hoy para llegar a mi proteína?") },
                            minLines = 2,
                            maxLines = 4,
                            modifier = Modifier
                                .fillMaxWidth()
                                .bringIntoViewOnFocus()
                                .testTag("coach_question_input")
                        )

                        Button(
                            onClick = { onAskCoach(question) },
                            enabled = !uiState.coach.isLoading && question.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("coach_ask_button")
                        ) {
                            if (uiState.coach.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Pensando…")
                            } else {
                                Text(stringResource(R.string.ask_coach))
                            }
                        }

                        uiState.coach.error?.let { err ->
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

                        if (uiState.coach.response.isNotBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Respuesta",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        IconButton(
                                            onClick = {
                                                clipboardManager.setText(
                                                    AnnotatedString(uiState.coach.response)
                                                )
                                                onCopiedMessage("Respuesta copiada")
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                Icons.Outlined.ContentCopy,
                                                contentDescription = "Copiar respuesta",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = uiState.coach.response,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

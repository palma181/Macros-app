package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.local.WeightEntity
import com.example.data.model.DayCalories
import com.example.data.model.MacroGoals
import com.example.data.model.MacroTotals
import com.example.ui.theme.LocalMacroColors
import kotlin.math.max
import kotlin.math.min

@Composable
fun CalorieArcGauge(
    consumedKcal: Int,
    goalKcal: Int,
    modifier: Modifier = Modifier,
    size: Dp = 128.dp
) {
    val macroColors = LocalMacroColors.current
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val overColor = macroColors.protein
    val targetProgress = if (goalKcal > 0) (consumedKcal.toFloat() / goalKcal.toFloat()).coerceIn(0f, 1.25f) else 0f
    val animatedSweep by animateFloatAsState(
        targetValue = min(1f, targetProgress) * 260f,
        animationSpec = tween(durationMillis = 550),
        label = "calorie_arc"
    )
    val isOver = goalKcal > 0 && consumedKcal > goalKcal
    val activeColor = if (isOver) overColor else macroColors.calories
    val remaining = goalKcal - consumedKcal

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokeWidth = 12.dp.toPx()
            val inset = strokeWidth / 2f
            val arcSize = Size(this.size.width - strokeWidth, this.size.height - strokeWidth)
            drawArc(
                color = trackColor,
                startAngle = 140f,
                sweepAngle = 260f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            drawArc(
                color = activeColor,
                startAngle = 140f,
                sweepAngle = animatedSweep,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = consumedKcal.toString(),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "de $goalKcal kcal",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (remaining >= 0) "$remaining rest." else "+${-remaining} extra",
                style = MaterialTheme.typography.labelSmall,
                color = if (isOver) overColor else macroColors.calories,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun MacroProgressBarRow(
    label: String,
    current: Int,
    goal: Int,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val ratio = if (goal > 0) (current.toFloat() / goal.toFloat()).coerceIn(0f, 1f) else 0f
    val animatedRatio by animateFloatAsState(
        targetValue = ratio,
        animationSpec = tween(durationMillis = 450),
        label = "macro_bar_$label"
    )
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = "$current / $goal $unit",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(5.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedRatio)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
    }
}

@Composable
fun MacroBarsBlock(
    totals: MacroTotals,
    goals: MacroGoals,
    daysMultiplier: Int,
    includeCalories: Boolean = true,
    modifier: Modifier = Modifier
) {
    val macroColors = LocalMacroColors.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (includeCalories) {
            MacroProgressBarRow(
                label = "Calorías",
                current = totals.kcal,
                goal = goals.kcal * daysMultiplier,
                unit = "kcal",
                color = macroColors.calories
            )
        }
        MacroProgressBarRow(
            label = "Proteína",
            current = totals.protein,
            goal = goals.protein * daysMultiplier,
            unit = "g",
            color = macroColors.protein
        )
        MacroProgressBarRow(
            label = "Carbohidratos",
            current = totals.carbs,
            goal = goals.carbs * daysMultiplier,
            unit = "g",
            color = macroColors.carbs
        )
        MacroProgressBarRow(
            label = "Grasa",
            current = totals.fat,
            goal = goals.fat * daysMultiplier,
            unit = "g",
            color = macroColors.fat
        )
    }
}

@Composable
fun WeeklyCaloriesBarChart(
    days: List<DayCalories>,
    dailyGoalKcal: Int,
    modifier: Modifier = Modifier
) {
    val macroColors = LocalMacroColors.current
    val primaryColor = macroColors.calories
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val goalLineColor = MaterialTheme.colorScheme.outline

    val maxVal = max(
        dailyGoalKcal.coerceAtLeast(100),
        days.maxOfOrNull { it.kcal } ?: 0
    ).toFloat()

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp)
        ) {
            if (days.isEmpty()) return@Canvas
            val count = days.size
            val slotWidth = size.width / count
            val barWidth = (slotWidth * 0.52f).coerceAtMost(28.dp.toPx())
            val chartBottom = size.height - 6.dp.toPx()
            val usableHeight = chartBottom - 10.dp.toPx()

            // Daily goal dashed line
            if (dailyGoalKcal > 0) {
                val goalRatio = (dailyGoalKcal / maxVal).coerceIn(0f, 1f)
                val goalY = chartBottom - goalRatio * usableHeight
                drawLine(
                    color = goalLineColor,
                    start = Offset(0f, goalY),
                    end = Offset(size.width, goalY),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                )
            }

            days.forEachIndexed { index, item ->
                val centerX = slotWidth * index + slotWidth / 2f
                val left = centerX - barWidth / 2f

                // Track background
                drawRoundRect(
                    color = trackColor.copy(alpha = 0.55f),
                    topLeft = Offset(left, 10.dp.toPx()),
                    size = Size(barWidth, usableHeight),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )

                if (item.kcal > 0) {
                    val ratio = (item.kcal / maxVal).coerceIn(0.04f, 1f)
                    val barH = ratio * usableHeight
                    val top = chartBottom - barH
                    val barColor = when {
                        item.kcal > dailyGoalKcal && dailyGoalKcal > 0 -> macroColors.carbs
                        item.isToday -> primaryColor
                        else -> primaryColor.copy(alpha = 0.82f)
                    }
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(left, top),
                        size = Size(barWidth, barH),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            days.forEach { item ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = item.dayInitial,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (item.isToday) FontWeight.Bold else FontWeight.Medium,
                        color = if (item.isToday) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (item.kcal > 0) "${item.kcal}" else "—",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun WeightLineChart(
    weights: List<WeightEntity>,
    modifier: Modifier = Modifier
) {
    val lineColor = LocalMacroColors.current.calories
    val fillTop = lineColor.copy(alpha = 0.28f)
    val fillBottom = lineColor.copy(alpha = 0.0f)
    val surfaceColor = MaterialTheme.colorScheme.surface
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    if (weights.size < 2) {
        return
    }

    val minKg = weights.minOf { it.kg }
    val maxKg = weights.maxOf { it.kg }
    val range = (maxKg - minKg).let { if (it < 0.2) 1.0 else it }

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .padding(vertical = 6.dp)
        ) {
            val padX = 12.dp.toPx()
            val padY = 12.dp.toPx()
            val w = size.width - padX * 2
            val h = size.height - padY * 2

            // Subtle horizontal grid lines
            for (step in 0..2) {
                val y = padY + (h / 2f) * step
                drawLine(
                    color = gridColor,
                    start = Offset(padX, y),
                    end = Offset(size.width - padX, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            val points = weights.mapIndexed { idx, item ->
                val x = padX + (idx.toFloat() / (weights.size - 1).toFloat()) * w
                val normY = ((item.kg - minKg) / range).toFloat().coerceIn(0f, 1f)
                val y = padY + h - normY * h
                Offset(x, y)
            }

            val fillPath = Path().apply {
                moveTo(points.first().x, padY + h)
                points.forEach { pt -> lineTo(pt.x, pt.y) }
                lineTo(points.last().x, padY + h)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(fillTop, fillBottom),
                    startY = padY,
                    endY = padY + h
                )
            )

            val linePath = Path().apply {
                points.forEachIndexed { idx, pt ->
                    if (idx == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                }
            }
            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            points.forEach { pt ->
                drawCircle(color = lineColor, radius = 5.dp.toPx(), center = pt)
                drawCircle(color = surfaceColor, radius = 2.5.dp.toPx(), center = pt)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Mín: ${"%.1f".format(minKg)} kg",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Máx: ${"%.1f".format(maxKg)} kg",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

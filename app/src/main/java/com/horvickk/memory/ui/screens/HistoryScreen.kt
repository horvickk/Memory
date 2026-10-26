package com.horvickk.memory.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.horvickk.memory.data.ResultEntry
import com.horvickk.memory.data.ResultsStore
import com.horvickk.memory.game.Exercise
import com.horvickk.memory.ui.components.IconKind
import com.horvickk.memory.ui.components.InkButton
import com.horvickk.memory.ui.components.InkDialog
import com.horvickk.memory.ui.components.StartTopBar
import com.horvickk.memory.ui.components.Txt
import com.horvickk.memory.ui.components.inkBorder
import com.horvickk.memory.ui.theme.Dimens
import com.horvickk.memory.ui.theme.LocalAppColors
import com.horvickk.memory.ui.theme.Type
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val CHART_SIZE = 30
private const val CHART_MIN_SLOTS = 12

@Composable
fun HistoryScreen(exercise: Exercise, number: Int, results: ResultsStore, onBack: () -> Unit) {
    val colors = LocalAppColors.current
    val entries = results.of(exercise.id)
    val best = exercise.best(entries)
    var confirmClear by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy  HH:mm", Locale.getDefault()) }

    Column(Modifier.fillMaxSize()) {
        StartTopBar("ИСТОРИЯ", IconKind.BACK, onBack)
        LazyColumn(
            Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(horizontal = Dimens.screenPadding, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Txt("$number. ${exercise.title}", Type.cardTitle)
                Spacer(Modifier.height(4.dp))
                Txt(exercise.subtitle, Type.cardSubtitle, color = colors.inkSoft)
            }
            if (entries.isEmpty()) {
                item {
                    Txt(
                        "ЕЩЁ НЕТ РЕЗУЛЬТАТОВ.\nСЫГРАЙ, И ОНИ ПОЯВЯТСЯ ЗДЕСЬ",
                        Type.body,
                        Modifier.fillMaxWidth().inkBorder().padding(24.dp),
                        color = colors.inkSoft,
                        align = TextAlign.Center,
                    )
                }
            } else {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatBox("ЛУЧШИЙ", best?.let(exercise.formatScore) ?: "—", Modifier.weight(1f))
                        StatBox("ИГР", entries.size.toString(), Modifier.weight(1f))
                    }
                }
                item {
                    val recent = entries.takeLast(10)
                    val average = recent.map { it.score }.average()
                    StatBox(
                        "СРЕДНЕЕ ЗА ПОСЛЕДНИЕ ${recent.size}",
                        "%.1f".format(average).replace('.', ','),
                        Modifier.fillMaxWidth(),
                    )
                }
                if (entries.size >= 2) {
                    item { Chart(entries.takeLast(CHART_SIZE), exercise.lowerIsBetter) }
                }
                item { Txt("ВСЕ РЕЗУЛЬТАТЫ", Type.label, Modifier.padding(top = 8.dp), color = colors.inkSoft) }
                items(entries.asReversed()) { entry ->
                    ResultRow(entry, exercise, isBest = entry.score == best, dateFormat)
                }
                item {
                    Spacer(Modifier.height(8.dp))
                    InkButton("ОЧИСТИТЬ ИСТОРИЮ", Modifier.fillMaxWidth()) { confirmClear = true }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }

    if (confirmClear) {
        InkDialog(
            title = "ОЧИСТИТЬ ИСТОРИЮ?",
            text = "ВСЕ РЕЗУЛЬТАТЫ УПРАЖНЕНИЯ «${exercise.title}» БУДУТ УДАЛЕНЫ",
            confirm = "ОЧИСТИТЬ",
            dismiss = "ОТМЕНА",
            onConfirm = {
                results.clear(exercise.id)
                confirmClear = false
            },
            onDismiss = { confirmClear = false },
        )
    }
}

@Composable
private fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    Column(modifier.inkBorder().padding(horizontal = 14.dp, vertical = 12.dp)) {
        Txt(label, Type.label, color = colors.inkSoft, maxLines = 1)
        Spacer(Modifier.height(6.dp))
        Txt(value, Type.cardTitle, maxLines = 1)
    }
}

/** Столбики последних результатов: один ряд, один цвет, лучший — подписан. */
@Composable
private fun Chart(entries: List<ResultEntry>, lowerIsBetter: Boolean) {
    val colors = LocalAppColors.current
    val max = entries.maxOf { it.score }.coerceAtLeast(1)
    Column(Modifier.fillMaxWidth().inkBorder().padding(14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Txt("ПОСЛЕДНИЕ ${entries.size}", Type.label, Modifier.weight(1f), color = colors.inkSoft)
            Txt(if (lowerIsBetter) "НИЖЕ — ЛУЧШЕ" else "ВЫШЕ — ЛУЧШЕ", Type.label, color = colors.inkSoft)
        }
        Spacer(Modifier.height(12.dp))
        Canvas(Modifier.fillMaxWidth().height(120.dp)) {
            val gap = 2.dp.toPx()
            val slot = size.width / maxOf(entries.size, CHART_MIN_SLOTS)
            val barWidth = (slot - gap).coerceAtLeast(1f)
            val radius = 4.dp.toPx().coerceAtMost(barWidth / 2)
            val minBar = 2.dp.toPx()
            entries.forEachIndexed { i, entry ->
                val h = (size.height * entry.score / max).coerceAtLeast(minBar)
                val left = i * slot
                val top = size.height - h
                // Скругление только у верхнего (значимого) конца столбика.
                val path = Path().apply {
                    addRoundRect(
                        RoundRect(
                            left = left,
                            top = top,
                            right = left + barWidth,
                            bottom = size.height,
                            topLeftCornerRadius = CornerRadius(radius),
                            topRightCornerRadius = CornerRadius(radius),
                        )
                    )
                }
                drawPath(path, colors.ink)
            }
            drawRect(colors.inkFaint, Offset(0f, size.height - 1.dp.toPx()), Size(size.width, 1.dp.toPx()))
        }
    }
}

@Composable
private fun ResultRow(entry: ResultEntry, exercise: Exercise, isBest: Boolean, dateFormat: SimpleDateFormat) {
    val colors = LocalAppColors.current
    Row(
        Modifier.fillMaxWidth().inkBorder(Dimens.thinBorder).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Txt(dateFormat.format(Date(entry.time)), Type.body, Modifier.weight(1f), color = colors.inkSoft)
        if (isBest) {
            Txt("РЕКОРД", Type.label, Modifier.padding(end = 10.dp), color = colors.correct)
        }
        Txt(exercise.formatScore(entry.score), Type.button)
    }
}

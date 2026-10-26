package io.horvickk.memory.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.horvickk.memory.data.ResultsStore
import io.horvickk.memory.data.SettingsStore
import io.horvickk.memory.game.Exercise
import io.horvickk.memory.ui.components.CenterTopBar
import io.horvickk.memory.ui.components.IconKind
import io.horvickk.memory.ui.components.IconTap
import io.horvickk.memory.ui.components.LineIcon
import io.horvickk.memory.ui.components.Txt
import io.horvickk.memory.ui.components.inkBorder
import io.horvickk.memory.ui.components.tap
import io.horvickk.memory.ui.theme.Dimens
import io.horvickk.memory.ui.theme.LocalAppColors
import io.horvickk.memory.ui.theme.Type

@Composable
fun HomeScreen(
    exercises: List<Exercise>,
    results: ResultsStore,
    settings: SettingsStore,
    onOpen: (Exercise) -> Unit,
    onHelp: (Exercise) -> Unit,
    onHistory: (Exercise) -> Unit,
    onSettings: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        CenterTopBar(
            title = "УПРАЖНЕНИЯ",
            left = {
                IconTap(
                    if (settings.sound) IconKind.SOUND_ON else IconKind.SOUND_OFF,
                    if (settings.sound) "Выключить звук" else "Включить звук",
                ) { settings.updateSound(!settings.sound) }
            },
            right = { IconTap(IconKind.SETTINGS, "Настройки", size = 34, onClick = onSettings) },
        )
        LazyColumn(
            Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(horizontal = Dimens.screenPadding, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            itemsIndexed(exercises, key = { _, ex -> ex.id }) { index, exercise ->
                ExerciseCard(
                    number = index + 1,
                    exercise = exercise,
                    played = results.of(exercise.id).isNotEmpty(),
                    onOpen = { onOpen(exercise) },
                    onHelp = { onHelp(exercise) },
                    onHistory = { onHistory(exercise) },
                )
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun ExerciseCard(
    number: Int,
    exercise: Exercise,
    played: Boolean,
    onOpen: () -> Unit,
    onHelp: () -> Unit,
    onHistory: () -> Unit,
) {
    val colors = LocalAppColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .inkBorder()
            .tap(onClick = onOpen)
            .padding(start = 16.dp, end = 4.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Txt("$number.", Type.cardTitle, Modifier.align(Alignment.Top))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Txt(
                exercise.title,
                Type.cardTitle,
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 14.sp, maxFontSize = Type.cardTitle.fontSize, stepSize = 0.5.sp),
            )
            Spacer(Modifier.height(4.dp))
            Txt(exercise.subtitle, Type.cardSubtitle, color = colors.inkSoft)
        }
        Box(
            Modifier
                .size(48.dp)
                .semantics { contentDescription = if (played) "Пройдено. Правила" else "Правила" }
                .tap(onClick = onHelp),
            contentAlignment = Alignment.Center,
        ) {
            if (played) LineIcon(IconKind.CHECK, size = 30.dp)
            else Txt("?", Type.cardTitle.copy(fontSize = 30.sp))
        }
        IconTap(IconKind.HISTORY, "История", size = 30, onClick = onHistory)
    }
}

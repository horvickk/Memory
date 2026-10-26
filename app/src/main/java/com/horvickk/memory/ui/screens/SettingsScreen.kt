package com.horvickk.memory.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.horvickk.memory.BuildConfig
import com.horvickk.memory.data.ResultsStore
import com.horvickk.memory.data.SettingsStore
import com.horvickk.memory.data.ShowSpeed
import com.horvickk.memory.data.ThemeMode
import com.horvickk.memory.ui.components.IconKind
import com.horvickk.memory.ui.components.InkDialog
import com.horvickk.memory.ui.components.StartTopBar
import com.horvickk.memory.ui.components.Txt
import com.horvickk.memory.ui.components.inkBorder
import com.horvickk.memory.ui.components.tap
import com.horvickk.memory.ui.theme.Dimens
import com.horvickk.memory.ui.theme.LocalAppColors
import com.horvickk.memory.ui.theme.Type

@Composable
fun SettingsScreen(settings: SettingsStore, results: ResultsStore, onBack: () -> Unit) {
    val colors = LocalAppColors.current
    var confirmReset by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        StartTopBar("НАСТРОЙКИ", IconKind.BACK, onBack)
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.screenPadding, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SettingRow("ЗВУК", if (settings.sound) "ВКЛЮЧЁН" else "ВЫКЛЮЧЕН") {
                settings.updateSound(!settings.sound)
            }
            SettingRow("ВИБРАЦИЯ", if (settings.vibration) "ВКЛЮЧЕНА" else "ВЫКЛЮЧЕНА") {
                settings.updateVibration(!settings.vibration)
            }
            SettingRow("ТЕМА", settings.theme.title) {
                settings.updateTheme(ThemeMode.entries[(settings.theme.ordinal + 1) % ThemeMode.entries.size])
            }
            SettingRow("СКОРОСТЬ ПОКАЗА", settings.speed.title, "СКОЛЬКО ВРЕМЕНИ ДАЁТСЯ НА ЗАПОМИНАНИЕ") {
                settings.updateSpeed(ShowSpeed.entries[(settings.speed.ordinal + 1) % ShowSpeed.entries.size])
            }
            SettingRow("СБРОСИТЬ РЕЗУЛЬТАТЫ", "УДАЛИТЬ ИСТОРИЮ ВСЕХ УПРАЖНЕНИЙ") { confirmReset = true }
            Spacer(Modifier.height(12.dp))
            Txt(
                "MEMORY ${BuildConfig.VERSION_NAME}\nТРЕНИРОВКА ПАМЯТИ",
                Type.label,
                Modifier.fillMaxWidth(),
                color = colors.inkSoft,
                align = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
        }
    }

    if (confirmReset) {
        InkDialog(
            title = "СБРОСИТЬ ВСЁ?",
            text = "РЕЗУЛЬТАТЫ ВСЕХ УПРАЖНЕНИЙ БУДУТ УДАЛЕНЫ БЕЗ ВОЗМОЖНОСТИ ВОССТАНОВЛЕНИЯ",
            confirm = "СБРОСИТЬ",
            dismiss = "ОТМЕНА",
            onConfirm = {
                results.clearAll()
                confirmReset = false
            },
            onDismiss = { confirmReset = false },
        )
    }
}

@Composable
private fun SettingRow(title: String, value: String, note: String? = null, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .inkBorder()
            .tap(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Txt(title, Type.cardTitle)
            Spacer(Modifier.height(4.dp))
            Txt(value, Type.cardSubtitle, color = colors.inkSoft)
            if (note != null) {
                Spacer(Modifier.height(2.dp))
                Txt(note, Type.cardSubtitle, color = colors.inkSoft)
            }
        }
    }
}

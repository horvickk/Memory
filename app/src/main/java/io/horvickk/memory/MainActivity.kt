package io.horvickk.memory

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import io.horvickk.memory.audio.Feedback
import io.horvickk.memory.data.ThemeMode
import io.horvickk.memory.game.Exercise
import io.horvickk.memory.game.exercises.EXERCISES
import io.horvickk.memory.ui.components.InkDialog
import io.horvickk.memory.ui.screens.ExerciseScreen
import io.horvickk.memory.ui.screens.HistoryScreen
import io.horvickk.memory.ui.screens.HomeScreen
import io.horvickk.memory.ui.screens.SettingsScreen
import io.horvickk.memory.ui.theme.LocalAppColors
import io.horvickk.memory.ui.theme.MemoryTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as MemoryApp
        setContent {
            val dark = when (app.settings.theme) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            LaunchedEffect(dark) {
                val style = if (dark) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            MemoryTheme(dark) { MemoryNavHost(app) }
        }
    }
}

/** Маршруты хранятся строками, чтобы стек экранов переживал пересоздание активности. */
private object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    fun exercise(id: String) = "exercise/$id"
    fun history(id: String) = "history/$id"
}

@Composable
private fun MemoryNavHost(app: MemoryApp) {
    val colors = LocalAppColors.current
    val view = LocalView.current
    val feedback = remember(view) { Feedback(app.sounds, app.settings, view) }
    var stack by rememberSaveable { mutableStateOf(listOf(Routes.HOME)) }
    var help by remember { mutableStateOf<Exercise?>(null) }

    fun push(route: String) {
        stack = stack + route
    }

    fun pop() {
        if (stack.size > 1) stack = stack.dropLast(1)
    }

    fun exerciseOf(route: String): Exercise? = EXERCISES.firstOrNull { it.id == route.substringAfter('/') }

    BackHandler(enabled = stack.size > 1) { pop() }

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.paper)
            .safeDrawingPadding()
    ) {
        val route = stack.last()
        when {
            route == Routes.SETTINGS -> SettingsScreen(app.settings, app.results, onBack = ::pop)

            route.startsWith("exercise/") -> exerciseOf(route)?.let { exercise ->
                ExerciseScreen(exercise, app.results, app.settings, feedback, onClose = ::pop)
            }

            route.startsWith("history/") -> exerciseOf(route)?.let { exercise ->
                HistoryScreen(exercise, EXERCISES.indexOf(exercise) + 1, app.results, onBack = ::pop)
            }

            else -> HomeScreen(
                exercises = EXERCISES,
                results = app.results,
                settings = app.settings,
                onOpen = { push(Routes.exercise(it.id)) },
                onHelp = { help = it },
                onHistory = { push(Routes.history(it.id)) },
                onSettings = { push(Routes.SETTINGS) },
            )
        }
    }

    help?.let { exercise ->
        val best = exercise.best(app.results.of(exercise.id))
        val bestLine = best?.let { "\n\nЛУЧШИЙ РЕЗУЛЬТАТ: ${exercise.formatScore(it)}" }.orEmpty()
        InkDialog(
            title = exercise.title,
            text = exercise.rules + bestLine,
            confirm = "ИГРАТЬ",
            dismiss = "ЗАКРЫТЬ",
            onConfirm = {
                help = null
                push(Routes.exercise(exercise.id))
            },
            onDismiss = { help = null },
        )
    }
}

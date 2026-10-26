package com.horvickk.memory.game

import androidx.compose.runtime.Composable
import com.horvickk.memory.audio.Feedback
import com.horvickk.memory.data.ResultEntry

data class GameStatus(val text: String = "", val lives: Int? = null, val maxLives: Int = 3)

/** То, что экран упражнения даёт игре: обратная связь, скорость показа, статус и завершение. */
class GameContext(
    val feedback: Feedback,
    private val speedFactor: Float,
    private val onStatus: (GameStatus) -> Unit,
    private val onFinish: (Int) -> Unit,
) {
    fun status(text: String, lives: Int? = null, maxLives: Int = 3) = onStatus(GameStatus(text, lives, maxLives))

    /** Время показа задания с учётом настройки «Скорость показа». */
    fun showMs(base: Long): Long = (base * speedFactor).toLong()

    fun finish(score: Int) = onFinish(score)
}

class Exercise(
    val id: String,
    val title: String,
    val subtitle: String,
    val rules: String,
    val lowerIsBetter: Boolean = false,
    val formatScore: (Int) -> String,
    val play: @Composable (GameContext) -> Unit,
) {
    fun best(results: List<ResultEntry>): Int? =
        if (lowerIsBetter) results.minOfOrNull { it.score } else results.maxOfOrNull { it.score }

    fun isBetter(score: Int, than: Int): Boolean = if (lowerIsBetter) score < than else score > than
}

/** Русское склонение: plural(5, "цифра", "цифры", "цифр") -> "5 цифр". */
fun plural(n: Int, one: String, few: String, many: String): String {
    val mod10 = n % 10
    val mod100 = n % 100
    val word = when {
        mod10 == 1 && mod100 != 11 -> one
        mod10 in 2..4 && mod100 !in 12..14 -> few
        else -> many
    }
    return "$n $word"
}

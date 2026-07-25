package com.example.wasteland

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/**
 * Игровой тик, синхронизированный с VSync через withFrameNanos.
 *
 * Почему не delay(16) / delay(1000) в цикле:
 * - delay() в корутине не синхронизирован с отрисовкой экрана — тик может
 *   случиться в середине кадра, вызывая рассинхрон и дёрганую анимацию дохода.
 * - withFrameNanos ждёт следующий Choreographer-кадр (тот же механизм,
 *   что использует сама система для отрисовки View/Compose) — то есть
 *   обновление состояния происходит РОВНО в такт с VSync дисплея,
 *   без лишних промежуточных пересчётов и без рассинхрона.
 *
 * dt считается по разнице между реальными метками кадров (в наносекундах),
 * а не фиксированным шагом — так доход в секунду остаётся математически
 * точным даже если частота кадров устройства отличается (60/90/120 Гц)
 * или кадр был пропущен из-за нагрузки.
 */
@Composable
fun GameLoop(state: GameState) {
    LaunchedEffect(Unit) {
        var lastFrameNanos = -1L
        while (true) {
            val frameNanos = withFrameNanosCompat()
            if (lastFrameNanos >= 0) {
                val dtSeconds = (frameNanos - lastFrameNanos) / 1_000_000_000.0
                // Защита от аномально большого dt (например, приложение
                // было свёрнуто и возобновлено) — не начисляем доход "за офлайн"
                // одним огромным скачком, чтобы не выглядело как баг/дюп.
                val clampedDt = dtSeconds.coerceIn(0.0, 0.25)
                state.tickIncome(clampedDt)
                state.tickBoost(clampedDt)
            }
            lastFrameNanos = frameNanos
        }
    }
}

private suspend fun withFrameNanosCompat(): Long =
    androidx.compose.runtime.withFrameNanos { it }

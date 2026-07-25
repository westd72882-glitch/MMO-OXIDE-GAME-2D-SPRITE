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
fun GameLoop(state: GameState, gameSave: GameSave? = null) {
    // Режим низкой нагрузки (настройки -> Производительность): тикаем логику
    // не на каждый VSync-кадр, а с фиксированным шагом ~10 раз/сек. Экрану
    // это не мешает (сама отрисовка Compose всё равно идёт с системной
    // частотой), а вот количество recomposition, вызванных изменением
    // состояния (coins, buildingLevels и т.п.), падает в разы — на слабых
    // устройствах это и была основная причина низкого FPS.
    val lowPerf = state.settings.lowPerformanceMode

    LaunchedEffect(lowPerf) {
        if (lowPerf) {
            val stepMs = 100L
            val stepSeconds = stepMs / 1000.0
            while (true) {
                kotlinx.coroutines.delay(stepMs)
                state.tickIncome(stepSeconds)
                state.tickBoost(stepSeconds)
            }
        } else {
            var lastFrameNanos = -1L
            while (true) {
                val frameNanos = withFrameNanosCompat()
                if (lastFrameNanos >= 0) {
                    val dtSeconds = (frameNanos - lastFrameNanos) / 1_000_000_000.0
                    // Защита от аномально большого dt (например, приложение
                    // было свёрнуто и возобновлено) — не начисляем доход одним
                    // огромным скачком в этом цикле; оффлайн-доход считается
                    // отдельно и честно в GameSave.load при следующем запуске.
                    val clampedDt = dtSeconds.coerceIn(0.0, 0.25)
                    state.tickIncome(clampedDt)
                    state.tickBoost(clampedDt)
                }
                lastFrameNanos = frameNanos
            }
        }
    }

    // Автосохранение раз в 5 секунд, пока экран игры открыт — так прогресс
    // (монеты, здания, инвентарь) не теряется даже без явного закрытия игры.
    if (gameSave != null) {
        LaunchedEffect(Unit) {
            while (true) {
                kotlinx.coroutines.delay(5000)
                gameSave.save(state)
            }
        }
    }
}

private suspend fun withFrameNanosCompat(): Long =
    androidx.compose.runtime.withFrameNanos { it }

package com.example.wasteland

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

class MainActivity : ComponentActivity() {
    private var billingManagerRef: BillingManager? = null
    private var gameStateRef: GameState? = null
    private var gameSaveRef: GameSave? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Пишем ЛЮБОЙ краш в файл на диске ДО попытки что-либо отрисовать.
        // Так текст ошибки не потеряется, даже если UI не успеет обновиться
        // перед тем как система убьёт процесс. Дополнительно пытаемся
        // сохранить прогресс прямо в обработчике краша — чтобы аварийное
        // закрытие не стоило игроку прогресса.
        val crashFile = File(getExternalFilesDir(null), "crash_log.txt")
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                crashFile.writeText(sw.toString())
                gameStateRef?.let { state -> gameSaveRef?.save(state) }
            } catch (_: Throwable) {
                // если даже запись в файл не удалась, ничего не поделать
            }
            // передаём дальше системному обработчику, чтобы приложение
            // закрылось штатно (иначе может зависнуть)
            android.os.Process.killProcess(android.os.Process.myPid())
        }

        try {
            enableEdgeToEdge()
            val gameSave = GameSave(applicationContext)
            val gameState = GameState()
            gameSave.load(gameState) // восстанавливаем прогресс + оффлайн-доход сразу при старте
            gameStateRef = gameState
            gameSaveRef = gameSave

            val billingManager = BillingManager(applicationContext, gameState)
            billingManager.connect()
            billingManagerRef = billingManager

            setContent {
                WastelandTheme {
                    CompositionLocalProvider(LocalBillingManager provides billingManager) {
                        GameScreenWithState(gameState, gameSave = gameSave)
                    }
                }
            }
        } catch (t: Throwable) {
            val sw = StringWriter()
            t.printStackTrace(PrintWriter(sw))
            crashFile.writeText(sw.toString())
            setContent {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "КРАШ ПРИ ЗАПУСКЕ:\n\n$sw",
                        color = Color.Red,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }

    // Сохраняем прогресс при каждом сворачивании/переключении приложения —
    // это самый надёжный момент для сохранения на Android (гарантированно
    // вызывается системой, в отличие от onDestroy, который может не успеть).
    override fun onPause() {
        super.onPause()
        gameStateRef?.let { state -> gameSaveRef?.save(state) }
    }

    override fun onStop() {
        super.onStop()
        gameStateRef?.let { state -> gameSaveRef?.save(state) }
    }

    override fun onDestroy() {
        super.onDestroy()
        gameStateRef?.let { state -> gameSaveRef?.save(state) }
        billingManagerRef?.disconnect()
    }
}

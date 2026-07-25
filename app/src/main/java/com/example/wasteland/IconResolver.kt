package com.example.wasteland

import android.content.Context
import androidx.annotation.DrawableRes

/**
 * Ищет drawable по имени файла (например "stone" -> R.drawable.stone).
 * Файлы попадают в drawable автоматически из корня репозитория на этапе сборки
 * (см. .github/workflows/build.yml). Если ресурс не найден — используется заглушка
 * ic_placeholder, чтобы приложение не падало.
 */
object IconResolver {
    private val cache = HashMap<String, Int>()

    @DrawableRes
    fun resolve(context: Context, name: String): Int {
        cache[name]?.let { return it }
        val resId = context.resources.getIdentifier(name, "drawable", context.packageName)
        val finalId = if (resId != 0) resId else R.drawable.ic_placeholder
        cache[name] = finalId
        return finalId
    }
}

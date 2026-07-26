package com.example.wasteland

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * СОХРАНЕНИЕ ПРОГРЕССА.
 *
 * Всё состояние игрока (монеты, ресурсы, здания, апгрейды, инвентарь,
 * экипировка, HP/радиация, настройки) сериализуется в один JSON и
 * кладётся в SharedPreferences. Плюс отдельно храним timestamp последнего
 * сохранения — при следующем запуске считаем, сколько времени прошло,
 * и начисляем реальный оффлайн-доход (см. calculateOfflineEarnings),
 * а не просто теряем прогресс или обнуляем счётчик.
 *
 * Сохранение вызывается:
 *  - автоматически, раз в несколько секунд, пока открыт экран игры;
 *  - при onPause/onStop активности (см. MainActivity) — чтобы не терять
 *    прогресс при сворачивании приложения;
 *  - сразу после значимых действий (постройка, покупка) — на всякий случай.
 */
private const val PREFS_NAME = "oxide_stata_save"
private const val KEY_SAVE_JSON = "save_json"
private const val KEY_LAST_SEEN_MILLIS = "last_seen_millis"

private const val MAX_OFFLINE_SECONDS = 8 * 60 * 60.0 // максимум 8 часов оффлайн-дохода за раз — честно и предсказуемо

class GameSave(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(state: GameState) {
        val json = JSONObject().apply {
            put("coins", state.coins)
            put("playerHp", state.playerHp)
            put("radiation", state.radiation)
            put("equippedWeaponId", state.equippedWeaponId ?: JSONObject.NULL)
            put("equippedArmorId", state.equippedArmorId ?: JSONObject.NULL)

            val resourcesJson = JSONObject()
            ResourceType.entries.forEach { type ->
                resourcesJson.put(type.name, state.resources[type] ?: 0)
            }
            put("resources", resourcesJson)

            val buildingsJson = JSONObject()
            BUILDINGS.forEach { b -> buildingsJson.put(b.id, state.buildingLevel(b.id)) }
            put("buildings", buildingsJson)

            val upgradesJson = JSONArray()
            UPGRADE_ITEMS.forEach { u ->
                if (state.ownedUpgrades[u.id] == true) upgradesJson.put(u.id)
            }
            put("upgrades", upgradesJson)

            val inventoryJson = JSONArray()
            state.inventory.forEach { slot ->
                val slotJson = JSONObject()
                slotJson.put("itemId", slot.itemId ?: JSONObject.NULL)
                slotJson.put("displayName", slot.displayName)
                slotJson.put("iconRes", slot.iconRes)
                slotJson.put("count", slot.count)
                slotJson.put("isResource", slot.isResource)
                inventoryJson.put(slotJson)
            }
            put("inventory", inventoryJson)

            put("settings", JSONObject().apply {
                put("musicEnabled", state.settings.musicEnabled)
                put("soundEnabled", state.settings.soundEnabled)
                put("vibrationEnabled", state.settings.vibrationEnabled)
                put("lowPerformanceMode", state.settings.lowPerformanceMode)
                put("notificationsEnabled", state.settings.notificationsEnabled)
            })

            put("clickLevel", state.clickLevel)
            put("totalClicks", state.totalClicks)
            put("casinoWins", state.casinoWins)
            put("casinoLosses", state.casinoLosses)
        }

        prefs.edit()
            .putString(KEY_SAVE_JSON, json.toString())
            .putLong(KEY_LAST_SEEN_MILLIS, System.currentTimeMillis())
            .apply()
    }

    /**
     * Загружает сохранение (если есть) и возвращает результат с информацией
     * об оффлайн-заработке, чтобы UI мог показать "Пока вас не было: +N монет".
     */
    fun load(state: GameState): OfflineReport? {
        val raw = prefs.getString(KEY_SAVE_JSON, null) ?: return null
        val json = try {
            JSONObject(raw)
        } catch (_: Exception) {
            return null
        }

        state.setCoinsFromSave(json.optDouble("coins", STARTING_COINS.toDouble()))
        state.setPlayerHpFromSave(json.optInt("playerHp", 100))
        state.setRadiationFromSave(json.optInt("radiation", 0))
        state.setEquippedFromSave(
            weaponId = json.optString("equippedWeaponId", "").ifEmpty { null }.takeIf { json.opt("equippedWeaponId") != JSONObject.NULL },
            armorId = json.optString("equippedArmorId", "").ifEmpty { null }.takeIf { json.opt("equippedArmorId") != JSONObject.NULL }
        )

        json.optJSONObject("resources")?.let { resJson ->
            ResourceType.entries.forEach { type ->
                if (resJson.has(type.name)) {
                    state.setResourceFromSave(type, resJson.optInt(type.name, 0))
                }
            }
        }

        json.optJSONObject("buildings")?.let { bJson ->
            BUILDINGS.forEach { b ->
                if (bJson.has(b.id)) {
                    state.setBuildingLevelFromSave(b.id, bJson.optInt(b.id, 0))
                }
            }
        }

        json.optJSONArray("upgrades")?.let { arr ->
            for (i in 0 until arr.length()) {
                state.setUpgradeOwnedFromSave(arr.optString(i))
            }
        }

        json.optJSONArray("inventory")?.let { arr ->
            val slots = mutableListOf<InventorySlot>()
            for (i in 0 until arr.length()) {
                val s = arr.optJSONObject(i) ?: continue
                val itemId = if (s.isNull("itemId")) null else s.optString("itemId")
                slots.add(
                    InventorySlot(
                        itemId = itemId,
                        displayName = s.optString("displayName", ""),
                        iconRes = s.optString("iconRes", ""),
                        count = s.optInt("count", 0),
                        isResource = s.optBoolean("isResource", false)
                    )
                )
            }
            if (slots.isNotEmpty()) state.setInventoryFromSave(slots)
        }

        json.optJSONObject("settings")?.let { sJson ->
            state.setSettingsFromSave(
                GameSettings(
                    musicEnabled = sJson.optBoolean("musicEnabled", true),
                    soundEnabled = sJson.optBoolean("soundEnabled", true),
                    vibrationEnabled = sJson.optBoolean("vibrationEnabled", true),
                    lowPerformanceMode = sJson.optBoolean("lowPerformanceMode", false),
                    notificationsEnabled = sJson.optBoolean("notificationsEnabled", true)
                )
            )
        }

        state.setClickStateFromSave(
            level = json.optInt("clickLevel", 0),
            clicks = json.optInt("totalClicks", 0)
        )
        state.setCasinoStateFromSave(
            wins = json.optInt("casinoWins", 0),
            losses = json.optInt("casinoLosses", 0)
        )

        // --- Реальный оффлайн-доход ---
        val lastSeenMillis = prefs.getLong(KEY_LAST_SEEN_MILLIS, System.currentTimeMillis())
        val elapsedSeconds = ((System.currentTimeMillis() - lastSeenMillis) / 1000.0).coerceIn(0.0, MAX_OFFLINE_SECONDS)
        if (elapsedSeconds >= 1.0) {
            val perSecond = state.totalCoinsPerSecond()
            val earned = perSecond * elapsedSeconds
            if (earned > 0) {
                state.grantOfflineCoins(earned)
                return OfflineReport(elapsedSeconds, earned)
            }
        }
        return null
    }

    fun hasSave(): Boolean = prefs.contains(KEY_SAVE_JSON)

    fun wipe() {
        prefs.edit().clear().apply()
    }
}

data class OfflineReport(val elapsedSeconds: Double, val earnedCoins: Double)

/** Пользовательские настройки — сохраняются вместе с прогрессом. */
data class GameSettings(
    val musicEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    // Режим низкой нагрузки: реже тикает игровой цикл и синк UI —
    // полезно на слабых/старых устройствах для стабильного FPS.
    val lowPerformanceMode: Boolean = false,
    val notificationsEnabled: Boolean = true
)

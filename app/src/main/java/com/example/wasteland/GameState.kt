package com.example.wasteland

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class GameState {
    // Точное внутреннее значение монет — обновляется каждый кадр, НЕ читается напрямую в UI.
    private var exactCoins: Double = STARTING_COINS.toDouble()

    // UI-видимое значение — обновляется с ограниченной частотой (см. syncCoinsToUi),
    // чтобы recompose экрана происходил не 60 раз в секунду, а ~8 раз в секунду.
    // Этого достаточно, чтобы счётчик выглядел плавным, но не грузит рендер.
    var coins by mutableDoubleStateOf(STARTING_COINS.toDouble())
        private set

    private var uiSyncAccumulator = 0.0
    private val uiSyncIntervalSeconds = 0.12

    /** Начисляет монеты мгновенно (покупки, лут, донат) — сразу видно в UI. */
    private fun addCoinsImmediate(amount: Double) {
        exactCoins += amount
        coins = exactCoins
    }

    private fun spendCoins(amount: Double) {
        exactCoins -= amount
        coins = exactCoins
    }

    /** Начисление за оффлайн-время (см. GameSave.load). */
    fun grantOfflineCoins(amount: Double) {
        addCoinsImmediate(amount)
    }

    /**
     * Кэш дохода монет/сек — пересчитывается только при изменении зданий/апгрейдов,
     * а не каждый кадр. Раньше totalCoinsPerSecond() пересчитывался в composable
     * CoinBadge на каждой перерисовке, из-за чего доход в шапке иногда мелькал
     * неполным/устаревшим. Теперь это поле — единственный источник истины:
     * им же тикает начисление, им же отображается шапка.
     */
    var coinsPerSecond by mutableDoubleStateOf(0.0)
        private set

    private fun recalcIncome() {
        val fromBuildings = BUILDINGS.sumOf { b -> b.incomeAtLevel(buildingLevel(b.id)) }
        val fromUpgrades = UPGRADE_ITEMS
            .filter { ownedUpgrades[it.id] == true }
            .sumOf { it.coinsPerSecondBonus }
        coinsPerSecond = fromBuildings + fromUpgrades
    }

    // --- Настройки (звук/музыка/вибро/производительность) — сохраняются вместе с прогрессом ---
    var settings by mutableStateOf(GameSettings())
        private set

    fun updateSettings(newSettings: GameSettings) {
        settings = newSettings
    }

    fun setSettingsFromSave(saved: GameSettings) {
        settings = saved
    }

    // --- Восстановление состояния из сохранения (см. GameSave.load) ---
    fun setCoinsFromSave(value: Double) {
        exactCoins = value
        coins = value
    }

    fun setPlayerHpFromSave(value: Int) {
        playerHp = value.coerceIn(0, maxPlayerHp)
    }

    fun setRadiationFromSave(value: Int) {
        radiation = value.coerceIn(0, 100)
    }

    fun setEquippedFromSave(weaponId: String?, armorId: String?) {
        equippedWeaponId = weaponId
        equippedArmorId = armorId
    }

    fun setResourceFromSave(type: ResourceType, amount: Int) {
        resources[type] = amount
    }

    fun setBuildingLevelFromSave(id: String, level: Int) {
        buildingLevels[id] = level
        recalcIncome()
    }

    fun setUpgradeOwnedFromSave(id: String) {
        ownedUpgrades[id] = true
        recalcIncome()
    }

    fun setInventoryFromSave(slots: List<InventorySlot>) {
        inventory.clear()
        inventory.addAll(slots)
    }

    val resources = mutableStateMapOf<ResourceType, Int>().apply {
        putAll(startingResources())
    }

    // --- Здания (пассивный доход) ---
    val buildingLevels = mutableStateMapOf<String, Int>()

    fun buildingLevel(id: String): Int = buildingLevels[id] ?: 0

    fun buildingUpgradeCost(building: Building): Int =
        building.costForLevel(buildingLevel(building.id))

    fun upgradeBuilding(building: Building) {
        val level = buildingLevel(building.id)
        if (level >= building.maxLevel) {
            showToast("Максимальный уровень")
            return
        }
        val cost = building.costForLevel(level)
        if (coins < cost) {
            showToast("Недостаточно монет")
            return
        }
        spendCoins(cost.toDouble())
        buildingLevels[building.id] = level + 1
        recalcIncome()
        showToast("${building.displayName}: уровень ${level + 1}")
    }

    // --- Апгрейды (постоянные бонусы к доходу/бою) ---
    val ownedUpgrades = mutableStateMapOf<String, Boolean>()

    fun canAffordUpgrade(upgrade: Upgrade): Boolean =
        upgrade.cost.all { (type, need) -> (resources[type] ?: 0) >= need }

    fun buyUpgrade(upgrade: Upgrade) {
        if (ownedUpgrades[upgrade.id] == true) return
        if (!canAffordUpgrade(upgrade)) {
            showToast("Не хватает ресурсов")
            return
        }
        upgrade.cost.forEach { (type, need) ->
            resources[type] = (resources[type] ?: 0) - need
            removeFromInventory(type.name, need)
        }
        ownedUpgrades[upgrade.id] = true
        recalcIncome()
        addToInventory(upgrade.id, upgrade.displayName, upgrade.iconRes, 1, isResource = false)
        showToast("Собрано: ${upgrade.displayName}")
    }

    /** Суммарный доход монет/сек от всех зданий и апгрейдов (из кэша — см. recalcIncome). */
    fun totalCoinsPerSecond(): Double = coinsPerSecond

    fun totalCombatDamageBonus(): Int =
        UPGRADE_ITEMS.filter { ownedUpgrades[it.id] == true }.sumOf { it.combatDamageBonus }

    fun totalCombatDefenseBonus(): Int =
        UPGRADE_ITEMS.filter { ownedUpgrades[it.id] == true }.sumOf { it.combatDefenseBonus } + equippedArmorDefense()

    /**
     * Вызывается из игрового тика (см. GameLoop) каждый кадр с dt в секундах.
     * Копит точный доход внутри exactCoins каждый кадр, но "публикует" его
     * в UI-состояние coins не чаще uiSyncIntervalSeconds — это и есть
     * оптимизация против лишних recomposition 60 раз/сек.
     */
    fun tickIncome(dtSeconds: Double) {
        val income = effectiveCoinsPerSecond() * dtSeconds
        // Раньше был "if (income > 0)" — из-за double-округления при очень
        // маленьком доходе строка иногда пропускалась, и казалось, что доход
        // "не всегда есть". Начисляем всегда, даже дробные копейки накапливаются.
        exactCoins += income

        uiSyncAccumulator += dtSeconds
        if (uiSyncAccumulator >= uiSyncIntervalSeconds) {
            uiSyncAccumulator = 0.0
            coins = exactCoins
        }
    }

    /** Доход монет/сек с учётом активного буста — то самое значение, которое должно показываться в UI. */
    fun effectiveCoinsPerSecond(): Double = coinsPerSecond * incomeBoostMultiplier

    // --- Покупка готовых предметов за монеты ---
    fun buyResource(offer: ShopOffer) {
        if (coins < offer.price) {
            showToast("Недостаточно монет")
            return
        }
        spendCoins(offer.price.toDouble())
        resources[offer.resource] = (resources[offer.resource] ?: 0) + offer.amount
        addToInventory(offer.resource.name, offer.resource.displayName, offer.resource.iconRes, offer.amount, isResource = true)
        showToast("+${offer.amount} ${offer.resource.displayName}")
    }

    fun buyWeapon(item: WeaponItem) {
        if (coins < item.price) {
            showToast("Недостаточно монет")
            return
        }
        spendCoins(item.price.toDouble())
        addToInventory(item.id, item.displayName, item.iconRes, 1, isResource = false)
        showToast("Куплено: ${item.displayName}")
    }

    fun buyArmor(item: ArmorItem) {
        if (coins < item.price) {
            showToast("Недостаточно монет")
            return
        }
        spendCoins(item.price.toDouble())
        addToInventory(item.id, item.displayName, item.iconRes, 1, isResource = false)
        showToast("Куплено: ${item.displayName}")
    }

    fun buyConsumable(item: ConsumableItem) {
        if (coins < item.price) {
            showToast("Недостаточно монет")
            return
        }
        spendCoins(item.price.toDouble())
        addToInventory(item.id, item.displayName, item.iconRes, 1, isResource = false)
        showToast("Куплено: ${item.displayName}")
    }

    // --- Экипировка ---
    var equippedWeaponId by mutableStateOf<String?>(null)
        private set
    var equippedArmorId by mutableStateOf<String?>(null)
        private set

    fun equippedWeapon(): WeaponItem? = SHOP_WEAPONS.find { it.id == equippedWeaponId }
    fun equippedArmor(): ArmorItem? = SHOP_ARMOR.find { it.id == equippedArmorId }
    private fun equippedArmorDefense(): Int = equippedArmor()?.defense ?: 0

    /**
     * Использование предмета из инвентаря — единая точка входа для "функционала предмета":
     * оружие/броня экипируются, расходники применяются и расходуются.
     */
    fun useItem(slot: InventorySlot) {
        val itemId = slot.itemId ?: return
        SHOP_WEAPONS.find { it.id == itemId }?.let { weapon ->
            equippedWeaponId = if (equippedWeaponId == weapon.id) null else weapon.id
            showToast(if (equippedWeaponId != null) "Экипировано: ${weapon.displayName}" else "Снято")
            return
        }
        SHOP_ARMOR.find { it.id == itemId }?.let { armor ->
            equippedArmorId = if (equippedArmorId == armor.id) null else armor.id
            showToast(if (equippedArmorId != null) "Надето: ${armor.displayName}" else "Снято")
            return
        }
        SHOP_CONSUMABLES.find { it.id == itemId }?.let { consumable ->
            if (slot.count <= 0) return
            playerHp = (playerHp + consumable.healAmount).coerceIn(0, maxPlayerHp)
            radiation = (radiation + consumable.radAmount).coerceIn(0, 100)
            removeFromInventory(itemId, 1)
            showToast("Использовано: ${consumable.displayName}")
            return
        }
    }

    // --- Бой ---
    val maxPlayerHp = 100
    var playerHp by mutableIntStateOf(100)
        private set
    var radiation by mutableIntStateOf(0)
        private set

    var currentEnemy by mutableStateOf<Enemy?>(null)
        private set
    var enemyHp by mutableIntStateOf(0)
        private set
    var combatLog by mutableStateOf<String?>(null)
        private set

    fun startFight(enemy: Enemy) {
        currentEnemy = enemy
        enemyHp = enemy.maxHp
        combatLog = "Бой начался: ${enemy.displayName}"
    }

    fun attack() {
        val enemy = currentEnemy ?: return
        if (playerHp <= 0) {
            showToast("Вы без сознания — отступите и подлечитесь")
            return
        }
        val weapon = equippedWeapon()
        val damage = (weapon?.damage ?: 4) + totalCombatDamageBonus()
        enemyHp = (enemyHp - damage).coerceAtLeast(0)

        if (enemyHp <= 0) {
            addCoinsImmediate(enemy.coinReward.toDouble())
            enemy.lootResource?.let { res ->
                resources[res] = (resources[res] ?: 0) + enemy.lootAmount
                addToInventory(res.name, res.displayName, res.iconRes, enemy.lootAmount, isResource = true)
            }
            combatLog = "Победа! +${enemy.coinReward} монет"
            currentEnemy = null
            return
        }

        val incomingDamage = (enemy.damage - totalCombatDefenseBonus()).coerceAtLeast(1)
        playerHp = (playerHp - incomingDamage).coerceAtLeast(0)
        combatLog = "Вы: -$incomingDamage HP · Враг: -$damage HP"

        if (playerHp <= 0) {
            combatLog = "Вы потеряли сознание. Отступление."
            currentEnemy = null
        }
    }

    fun retreat() {
        currentEnemy = null
        combatLog = null
    }

    fun healPlayer(amount: Int) {
        playerHp = (playerHp + amount).coerceIn(0, maxPlayerHp)
    }

    // --- Инвентарь ---
    val inventory = mutableStateListOf<InventorySlot>().apply {
        addAll(emptyInventory(24))
        var idx = 0
        for ((type, amount) in startingResources()) {
            if (amount > 0 && idx < size) {
                this[idx] = InventorySlot(
                    itemId = type.name,
                    displayName = type.displayName,
                    iconRes = type.iconRes,
                    count = amount,
                    isResource = true
                )
                idx++
            }
        }
    }

    var toast by mutableStateOf<String?>(null)
        private set

    fun clearToast() { toast = null }
    private fun showToast(msg: String) { toast = msg }

    private fun addToInventory(itemId: String, displayName: String, iconRes: String, amount: Int, isResource: Boolean) {
        val existingIdx = inventory.indexOfFirst { it.itemId == itemId }
        if (existingIdx != -1) {
            val slot = inventory[existingIdx]
            inventory[existingIdx] = slot.copy(count = slot.count + amount)
            return
        }
        val emptyIdx = inventory.indexOfFirst { it.itemId == null }
        if (emptyIdx != -1) {
            inventory[emptyIdx] = InventorySlot(itemId, displayName, iconRes, amount, isResource)
        }
    }

    private fun removeFromInventory(itemId: String, amount: Int) {
        var remaining = amount
        for (i in inventory.indices) {
            if (remaining <= 0) break
            val slot = inventory[i]
            if (slot.itemId == itemId) {
                val take = minOf(slot.count, remaining)
                remaining -= take
                val newCount = slot.count - take
                inventory[i] = if (newCount <= 0) InventorySlot() else slot.copy(count = newCount)
            }
        }
    }

    fun moveInventoryItem(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        if (fromIndex !in inventory.indices || toIndex !in inventory.indices) return
        val from = inventory[fromIndex]
        val to = inventory[toIndex]
        if (from.itemId == null) return

        if (to.itemId == from.itemId && from.isResource) {
            inventory[toIndex] = to.copy(count = to.count + from.count)
            inventory[fromIndex] = InventorySlot()
        } else {
            inventory[fromIndex] = to
            inventory[toIndex] = from
        }
    }

    // --- Донат (см. BillingManager) ---
    /** Вызывается после успешной покупки доната — начисляет монеты сразу. */
    fun grantDonationCoins(amount: Int) {
        addCoinsImmediate(amount.toDouble())
        showToast("Спасибо за поддержку! +$amount монет")
    }

    /** Временный буст дохода x2 на заданное время (секунды), от доната. */
    var incomeBoostMultiplier by mutableDoubleStateOf(1.0)
        private set
    private var incomeBoostRemainingSec = 0.0

    fun activateIncomeBoost(durationSeconds: Double, multiplier: Double = 2.0) {
        incomeBoostMultiplier = multiplier
        incomeBoostRemainingSec = durationSeconds
        showToast("Буст дохода x${multiplier.toInt()} активирован!")
    }

    fun tickBoost(dtSeconds: Double) {
        if (incomeBoostRemainingSec > 0) {
            incomeBoostRemainingSec -= dtSeconds
            if (incomeBoostRemainingSec <= 0) {
                incomeBoostRemainingSec = 0.0
                incomeBoostMultiplier = 1.0
            }
        }
    }
}

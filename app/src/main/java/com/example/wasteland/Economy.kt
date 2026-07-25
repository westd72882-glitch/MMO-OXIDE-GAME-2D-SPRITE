package com.example.wasteland

/**
 * СМЫСЛ ИГРЫ / ПЕТЛЯ ПРОГРЕССИИ:
 *
 * 1. Добываешь ресурсы (сера/камень/металл) — либо вручную (тапом), либо
 *    пассивно через постройки (шахта, вышка).
 * 2. Крафтишь инструменты (кирка, топор) — они увеличивают пассивный доход
 *    ресурсов и множитель монет в секунду.
 * 3. Копишь монеты (от продажи ресурсов, от боя, от пассивного дохода) —
 *    покупаешь оружие, броню, транспорт.
 * 4. Идёшь в бой (вкладка "Бой") — используешь оружие и броню из инвентаря,
 *    получаешь монеты + шанс лута за победу.
 * 5. На монеты покупаешь апгрейды базы (Upgrade) — они увеличивают
 *    coinsPerSecond и защиту в бою.
 * 6. Круг повторяется на новом уровне сложности/дохода.
 *
 * Донат (Google Play Billing) ускоряет этот цикл (бустеры x2 дохода,
 * пакеты монет), но не открывает эксклюзивный контент — честная модель.
 */

/** Постройка, дающая пассивный доход монет/сек. Можно улучшать (уровни). */
data class Building(
    val id: String,
    val displayName: String,
    val iconRes: String,
    val description: String,
    val baseCost: Int,
    val costGrowth: Double,       // множитель цены за каждый следующий уровень
    val baseIncomePerSec: Double, // доход монет/сек на 1 уровень
    val maxLevel: Int = 50
) {
    fun costForLevel(currentLevel: Int): Int =
        (baseCost * Math.pow(costGrowth, currentLevel.toDouble())).toInt()

    fun incomeAtLevel(level: Int): Double = baseIncomePerSec * level
}

val BUILDINGS = listOf(
    Building(
        id = "mining_quarry",
        displayName = "Добывающая вышка",
        iconRes = "mining_quarry",
        description = "Автоматически добывает ресурсы и монеты со временем",
        baseCost = 500,
        costGrowth = 1.18,
        baseIncomePerSec = 0.8
    ),
    Building(
        id = "buggy_outpost",
        displayName = "Гараж багги",
        iconRes = "buggy",
        description = "Патрулирует пустошь, приносит трофеи",
        baseCost = 2200,
        costGrowth = 1.22,
        baseIncomePerSec = 3.2
    ),
    Building(
        id = "storage_cupboard",
        displayName = "Склад",
        iconRes = "cupboard",
        description = "Хранит и продаёт излишки ресурсов",
        baseCost = 350,
        costGrowth = 1.15,
        baseIncomePerSec = 0.4
    ),
)

/** Инструмент/апгрейд, который даёт постоянный пассивный бонус, а не постройку. */
data class Upgrade(
    val id: String,
    val displayName: String,
    val iconRes: String,
    val description: String,
    val cost: Map<ResourceType, Int>,
    val coinsPerSecondBonus: Double = 0.0,
    val combatDamageBonus: Int = 0,
    val combatDefenseBonus: Int = 0
)

val UPGRADE_ITEMS = listOf(
    Upgrade(
        id = "sharp_stone",
        displayName = "Заточенный камень",
        iconRes = "stone",
        description = "Простое режущее орудие. +0.1 монет/сек",
        cost = mapOf(ResourceType.STONE to 20),
        coinsPerSecondBonus = 0.1
    ),
    Upgrade(
        id = "pickaxe",
        displayName = "Кирка",
        iconRes = "pickaxe_0",
        description = "Ускоряет добычу камня и руды. +0.4 монет/сек",
        cost = mapOf(ResourceType.STONE to 30, ResourceType.METAL to 20),
        coinsPerSecondBonus = 0.4
    ),
    Upgrade(
        id = "axe",
        displayName = "Топор",
        iconRes = "axe_1",
        description = "Рубит и режет. +6 урона в бою",
        cost = mapOf(ResourceType.METAL to 25, ResourceType.STONE to 15),
        combatDamageBonus = 6
    ),
    Upgrade(
        id = "iron_ingot_upgrade",
        displayName = "Слиток железа",
        iconRes = "iron_ingot",
        description = "Переплавлен для брони. +5 защиты",
        cost = mapOf(ResourceType.METAL to 40),
        combatDefenseBonus = 5
    ),
    Upgrade(
        id = "metal_door",
        displayName = "Металлическая дверь",
        iconRes = "door_metal",
        description = "Укрепляет базу. +10 защиты, +0.2 монет/сек",
        cost = mapOf(ResourceType.METAL to 60, ResourceType.STONE to 40),
        combatDefenseBonus = 10,
        coinsPerSecondBonus = 0.2
    ),
)

/** Оружие и снаряжение, покупаемое за монеты — с явным боевым функционалом. */
data class WeaponItem(
    val id: String,
    val displayName: String,
    val iconRes: String,
    val description: String,
    val price: Int,
    val damage: Int,
    val fireRateMs: Long, // задержка между ударами/выстрелами
    val ammoIconRes: String? = null // если требует патроны из инвентаря
)

val SHOP_WEAPONS = listOf(
    WeaponItem(
        id = "iron_spear",
        displayName = "Железное копьё",
        iconRes = "iron_spear_0",
        description = "Ближний бой. Надёжно и без патронов",
        price = 220,
        damage = 14,
        fireRateMs = 700
    ),
    WeaponItem(
        id = "hunting_rifle",
        displayName = "Охотничья винтовка",
        iconRes = "hunting_rifle_0",
        description = "Точный выстрел на дистанции",
        price = 950,
        damage = 35,
        fireRateMs = 1400,
        ammoIconRes = "hunting_rifle_bullet"
    ),
    WeaponItem(
        id = "smg_9mm",
        displayName = "9мм ПП",
        iconRes = "res_9mm_smg",
        description = "Скорострельный пистолет-пулемёт",
        price = 1600,
        damage = 9,
        fireRateMs = 180
    ),
    WeaponItem(
        id = "assault_rifle",
        displayName = "Штурмовая винтовка",
        iconRes = "assault_rifle",
        description = "Баланс урона и скорострельности",
        price = 3200,
        damage = 22,
        fireRateMs = 260
    ),
    WeaponItem(
        id = "guntrap",
        displayName = "Ружейная ловушка",
        iconRes = "guntrap",
        description = "Ставится на базу, бьёт первого врага сама",
        price = 1100,
        damage = 40,
        fireRateMs = 2000
    ),
)

/** Защитное снаряжение — надевается перед боем, снижает получаемый урон. */
data class ArmorItem(
    val id: String,
    val displayName: String,
    val iconRes: String,
    val description: String,
    val price: Int,
    val defense: Int
)

val SHOP_ARMOR = listOf(
    ArmorItem(
        id = "hazmat_suit",
        displayName = "Защитный костюм",
        iconRes = "hazmat_0",
        description = "Защита от радиации и урона. +18 защиты",
        price = 1400,
        defense = 18
    ),
)

/** Расходники с явным эффектом при использовании из инвентаря. */
data class ConsumableItem(
    val id: String,
    val displayName: String,
    val iconRes: String,
    val description: String,
    val price: Int,
    val healAmount: Int = 0,
    val radAmount: Int = 0
)

val SHOP_CONSUMABLES = listOf(
    ConsumableItem(
        id = "antirad_pills",
        displayName = "Антирадиновые таблетки",
        iconRes = "antirad_pills",
        description = "Снижают радиацию на 25",
        price = 120,
        radAmount = -25
    ),
    ConsumableItem(
        id = "candy_cane",
        displayName = "Леденец-трость",
        iconRes = "candy_cane",
        description = "Восстанавливает 10 HP",
        price = 60,
        healAmount = 10
    ),
)

/** Существо/цель для боевой системы. */
data class Enemy(
    val id: String,
    val displayName: String,
    val iconRes: String,
    val maxHp: Int,
    val damage: Int,
    val coinReward: Int,
    val lootResource: ResourceType?,
    val lootAmount: Int
)

val ENEMIES = listOf(
    Enemy(
        id = "raider",
        displayName = "Мародёр",
        iconRes = "crazy_grin",
        maxHp = 60,
        damage = 6,
        coinReward = 45,
        lootResource = ResourceType.METAL,
        lootAmount = 8
    ),
    Enemy(
        id = "mutant",
        displayName = "Мутант",
        iconRes = "crazy_grin",
        maxHp = 140,
        damage = 14,
        coinReward = 120,
        lootResource = ResourceType.SULFUR,
        lootAmount = 10
    ),
    Enemy(
        id = "warlord",
        displayName = "Главарь клана",
        iconRes = "crazy_grin",
        maxHp = 320,
        damage = 24,
        coinReward = 400,
        lootResource = ResourceType.STONE,
        lootAmount = 25
    ),
)

const val COIN_ICON_RES_2 = "coins_2"
const val COIN_ICON_RES_3 = "coins_3"
const val COIN_ICON_RES_4 = "coins_4"

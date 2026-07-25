package com.example.wasteland

/**
 * ИМЕНА ИКОНОК СООТВЕТСТВУЮТ ИМЕНАМ ФАЙЛОВ .png В КОРНЕ РЕПОЗИТОРИЯ.
 * Workflow (.github/workflows/build.yml) на этапе сборки сам копирует все *.png
 * из корня репо в app/src/main/res/drawable/, приводя имя к нижнему регистру
 * и заменяя недопустимые символы на "_". То есть:
 *   Rocket.png      -> drawable/rocket
 *   Stone.png       -> drawable/stone
 *   Sulfur_Ore.png  -> drawable/sulfur_ore
 *   coin.png        -> drawable/coin
 *   Hunting_Rifle_Bullet.png -> drawable/hunting_rifle_bullet
 *
 * Чтобы добавить новый предмет: положите PNG с любым именем в корень репозитория,
 * закоммитьте — и укажите здесь iconRes тем же именем (в нижнем регистре,
 * пробелы/спецсимволы -> "_"). Ничего больше менять не нужно, workflow подхватит файл сам.
 * Если иконки нет в репо, Android просто не найдёт ресурс на этапе сборки — так что
 * добавляйте PNG для каждого iconRes, который тут используете.
 */

enum class ResourceType(val displayName: String, val iconRes: String) {
    SULFUR("Сера", "sulfur_ore"),
    STONE("Камень", "stone"),
    METAL("Металлолом", "metal_fragment"),
}

data class ShopOffer(
    val id: String,
    val resource: ResourceType,
    val amount: Int,
    val price: Int
)

/** Покупка ресурсов за монеты (вкладка "Магазин") */
val SHOP_RESOURCE_OFFERS = listOf(
    ShopOffer("buy_sulfur", ResourceType.SULFUR, 50, 40),
    ShopOffer("buy_stone", ResourceType.STONE, 100, 25),
    ShopOffer("buy_metal", ResourceType.METAL, 50, 45),
)

data class ShopItem(
    val id: String,
    val displayName: String,
    val iconRes: String,
    val description: String,
    val price: Int
)

/** Готовые предметы, которые покупаются напрямую за монеты (не крафтятся) */
val SHOP_ITEMS = listOf(
    ShopItem(
        id = "rocket",
        displayName = "Ракета РПГ",
        iconRes = "rocket",
        description = "Реактивный снаряд для гранатомёта",
        price = 180
    ),
    ShopItem(
        id = "hunting_rifle_bullet",
        displayName = "Патрон охотничьей винтовки",
        iconRes = "hunting_rifle_bullet",
        description = "Боеприпас для охотничьей винтовки",
        price = 15
    ),
    ShopItem(
        id = "smg_9mm",
        displayName = "9мм ПП",
        iconRes = "res_9mm_smg",
        description = "Скорострельный пистолет-пулемёт",
        price = 420
    ),
    ShopItem(
        id = "assault_rifle",
        displayName = "Штурмовая винтовка",
        iconRes = "assault_rifle",
        description = "Основное оружие для дальнего боя",
        price = 950
    ),
    ShopItem(
        id = "mining_quarry",
        displayName = "Добывающая вышка",
        iconRes = "mining_quarry",
        description = "Автоматически добывает ресурсы со временем",
        price = 1200
    ),
    ShopItem(
        id = "copter",
        displayName = "Мини-вертолёт",
        iconRes = "copter",
        description = "Лёгкая техника для разведки территории",
        price = 3500
    ),
    ShopItem(
        id = "candy_cane",
        displayName = "Леденец-трость",
        iconRes = "candy_cane",
        description = "Праздничная находка, можно использовать как дубинку",
        price = 60
    ),
)

data class ItemDef(
    val id: String,
    val displayName: String,
    val iconRes: String,
    val description: String,
    val cost: Map<ResourceType, Int>
)

/** Предметы, которые собираются из ресурсов (вкладка "Ресурсы") */
val CRAFT_ITEMS = listOf(
    ItemDef(
        id = "sharp_stone",
        displayName = "Заточенный камень",
        iconRes = "stone",
        description = "Простое режущее орудие",
        cost = mapOf(ResourceType.STONE to 20)
    ),
    ItemDef(
        id = "pickaxe",
        displayName = "Кирка",
        iconRes = "pickaxe_0",
        description = "Ускоряет добычу камня и руды",
        cost = mapOf(ResourceType.STONE to 30, ResourceType.METAL to 20)
    ),
)

const val COIN_ICON_RES = "coin"

fun startingResources(): MutableMap<ResourceType, Int> = mutableMapOf(
    ResourceType.SULFUR to 12,
    ResourceType.STONE to 30,
    ResourceType.METAL to 10,
)

const val STARTING_COINS = 250

data class InventorySlot(
    val itemId: String? = null,
    val displayName: String = "",
    val iconRes: String = "",
    val count: Int = 0,
    val isResource: Boolean = false
)

fun emptyInventory(size: Int = 24): List<InventorySlot> = List(size) { InventorySlot() }

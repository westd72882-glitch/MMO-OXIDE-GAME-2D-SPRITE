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

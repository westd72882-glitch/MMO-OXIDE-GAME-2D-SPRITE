package com.example.wasteland

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class GameState {
    var coins by mutableIntStateOf(STARTING_COINS)
        private set

    val resources = mutableStateMapOf<ResourceType, Int>().apply {
        putAll(startingResources())
    }

    val ownedCraftItems = mutableStateMapOf<String, Boolean>()

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

    fun buyResource(offer: ShopOffer) {
        if (coins < offer.price) {
            showToast("Недостаточно монет")
            return
        }
        coins -= offer.price
        resources[offer.resource] = (resources[offer.resource] ?: 0) + offer.amount
        addToInventory(offer.resource.name, offer.resource.displayName, offer.resource.iconRes, offer.amount, isResource = true)
        showToast("+${offer.amount} ${offer.resource.displayName}")
    }

    fun buyItem(item: ShopItem) {
        if (coins < item.price) {
            showToast("Недостаточно монет")
            return
        }
        coins -= item.price
        addToInventory(item.id, item.displayName, item.iconRes, 1, isResource = false)
        showToast("Куплено: ${item.displayName}")
    }

    fun canCraft(item: ItemDef): Boolean =
        item.cost.all { (type, need) -> (resources[type] ?: 0) >= need }

    fun craft(item: ItemDef) {
        if (ownedCraftItems[item.id] == true) return
        if (!canCraft(item)) {
            showToast("Не хватает ресурсов")
            return
        }
        item.cost.forEach { (type, need) ->
            resources[type] = (resources[type] ?: 0) - need
            removeFromInventory(type.name, need)
        }
        ownedCraftItems[item.id] = true
        addToInventory(item.id, item.displayName, item.iconRes, 1, isResource = false)
        showToast("Собрано: ${item.displayName}")
    }

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
}

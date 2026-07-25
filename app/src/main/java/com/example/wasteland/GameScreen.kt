package com.example.wasteland

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun GameScreen() {
    val state = remember { GameState() }
    GameScreenWithState(state)
}

@Composable
fun GameScreenWithState(state: GameState) {
    var tab by remember { mutableStateOf(0) } // 0 инвентарь, 1 крафт, 2 база, 3 магазин, 4 бой

    GameLoop(state)

    LaunchedEffect(state.toast) {
        if (state.toast != null) {
            delay(1800)
            state.clearToast()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text("WASTELAND", color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                    Text("SURVIVAL OUTPOST", color = TextMuted, fontSize = 10.sp, letterSpacing = 2.sp)
                }
                CoinBadge(coins = state.coins, coinsPerSecond = state.totalCoinsPerSecond() * state.incomeBoostMultiplier)
            }

            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) {
                    HpBar(current = state.playerHp, max = state.maxPlayerHp, label = "HP", color = WarnRed)
                }
                Box(modifier = Modifier.weight(1f)) {
                    HpBar(current = state.radiation, max = 100, label = "РАДИАЦИЯ", color = ResourceGreen)
                }
            }

            Spacer(Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(72.dp)
            ) {
                items(ResourceType.entries) { type ->
                    ResourceCard(type = type, amount = state.resources[type] ?: 0)
                }
            }

            Spacer(Modifier.height(16.dp))

            TabBar(selected = tab, onSelect = { tab = it })

            Spacer(Modifier.height(16.dp))

            Box(modifier = Modifier.weight(1f)) {
                when (tab) {
                    0 -> InventoryGrid(state)
                    1 -> Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        UpgradeList(state)
                    }
                    2 -> Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        BuildingList(state)
                    }
                    3 -> Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        ShopList(state)
                    }
                    4 -> CombatScreen(state)
                }
            }
        }

        state.toast?.let { msg ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ResourceGreen)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(text = msg, color = BgDark, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun UpgradeList(state: GameState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("КРАФТ УЛУЧШЕНИЙ", color = TextMuted, fontSize = 11.sp, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 2.dp))
        Text(
            "Каждый предмет даёт постоянный бонус к доходу или бою",
            color = TextSecondary,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        UPGRADE_ITEMS.forEach { item ->
            val done = state.ownedUpgrades[item.id] == true
            val affordable = state.canAffordUpgrade(item)

            ShopRow(
                iconRes = item.iconRes,
                title = item.displayName,
                subtitle = item.description,
                priceContent = {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        item.cost.forEach { (type, need) ->
                            val has = (state.resources[type] ?: 0) >= need
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                GameIcon(type.iconRes, 12.dp)
                                Text(need.toString(), color = if (has) TextSecondary else WarnRed, fontSize = 11.sp)
                            }
                        }
                    }
                },
                buttonLabel = if (done) "Готово" else "Собрать",
                buttonEnabled = !done && affordable,
                buttonActiveColor = if (done) BorderMuted else AccentRust,
                onClick = { state.buyUpgrade(item) },
                highlighted = done
            )
        }
    }
}

@Composable
private fun BuildingList(state: GameState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("ПОСТРОЙКИ БАЗЫ", color = TextMuted, fontSize = 11.sp, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 2.dp))
        Text(
            "Постройки приносят монеты автоматически, каждую секунду. Улучшайте их для роста дохода.",
            color = TextSecondary,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        BUILDINGS.forEach { building ->
            val level = state.buildingLevel(building.id)
            val cost = state.buildingUpgradeCost(building)
            val affordable = state.coins >= cost
            val maxed = level >= building.maxLevel

            ShopRow(
                iconRes = building.iconRes,
                title = "${building.displayName} · Ур. $level",
                subtitle = "${building.description} · доход: ${formatCoins(building.incomeAtLevel(level))}/сек",
                priceContent = {
                    if (!maxed) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            GameIcon(COIN_ICON_RES, 12.dp)
                            Text(formatCoins(cost.toDouble()), color = if (affordable) TextSecondary else WarnRed, fontSize = 11.sp)
                        }
                    } else {
                        Text("MAX", color = ResourceGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                },
                buttonLabel = if (maxed) "Макс" else if (level == 0) "Построить" else "Улучшить",
                buttonEnabled = !maxed && affordable,
                buttonActiveColor = AccentRust,
                onClick = { state.upgradeBuilding(building) },
                highlighted = false
            )
        }
    }
}

@Composable
private fun ShopList(state: GameState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("РЕСУРСЫ ЗА МОНЕТЫ", color = TextMuted, fontSize = 11.sp, letterSpacing = 1.sp)
        SHOP_RESOURCE_OFFERS.forEach { offer ->
            val affordable = state.coins >= offer.price
            ShopRow(
                iconRes = offer.resource.iconRes,
                title = "${offer.amount} × ${offer.resource.displayName}",
                subtitle = null,
                priceContent = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        GameIcon(COIN_ICON_RES, 12.dp)
                        Text(offer.price.toString(), color = TextSecondary, fontSize = 11.sp)
                    }
                },
                buttonLabel = if (affordable) "Купить" else "Мало монет",
                buttonEnabled = affordable,
                buttonActiveColor = AccentRust,
                onClick = { state.buyResource(offer) },
                highlighted = false
            )
        }

        Spacer(Modifier.height(6.dp))
        Text("ОРУЖИЕ", color = TextMuted, fontSize = 11.sp, letterSpacing = 1.sp)
        SHOP_WEAPONS.forEach { item ->
            val affordable = state.coins >= item.price
            ShopRow(
                iconRes = item.iconRes,
                title = item.displayName,
                subtitle = "${item.description} · урон ${item.damage}",
                priceContent = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        GameIcon(COIN_ICON_RES, 12.dp)
                        Text(item.price.toString(), color = TextSecondary, fontSize = 11.sp)
                    }
                },
                buttonLabel = if (affordable) "Купить" else "Мало монет",
                buttonEnabled = affordable,
                buttonActiveColor = AccentRust,
                onClick = { state.buyWeapon(item) },
                highlighted = false
            )
        }

        Spacer(Modifier.height(6.dp))
        Text("БРОНЯ", color = TextMuted, fontSize = 11.sp, letterSpacing = 1.sp)
        SHOP_ARMOR.forEach { item ->
            val affordable = state.coins >= item.price
            ShopRow(
                iconRes = item.iconRes,
                title = item.displayName,
                subtitle = "${item.description} · защита ${item.defense}",
                priceContent = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        GameIcon(COIN_ICON_RES, 12.dp)
                        Text(item.price.toString(), color = TextSecondary, fontSize = 11.sp)
                    }
                },
                buttonLabel = if (affordable) "Купить" else "Мало монет",
                buttonEnabled = affordable,
                buttonActiveColor = AccentRust,
                onClick = { state.buyArmor(item) },
                highlighted = false
            )
        }

        Spacer(Modifier.height(6.dp))
        Text("РАСХОДНИКИ", color = TextMuted, fontSize = 11.sp, letterSpacing = 1.sp)
        SHOP_CONSUMABLES.forEach { item ->
            val affordable = state.coins >= item.price
            ShopRow(
                iconRes = item.iconRes,
                title = item.displayName,
                subtitle = item.description,
                priceContent = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        GameIcon(COIN_ICON_RES, 12.dp)
                        Text(item.price.toString(), color = TextSecondary, fontSize = 11.sp)
                    }
                },
                buttonLabel = if (affordable) "Купить" else "Мало монет",
                buttonEnabled = affordable,
                buttonActiveColor = AccentRust,
                onClick = { state.buyConsumable(item) },
                highlighted = false
            )
        }

        Spacer(Modifier.height(6.dp))
        DonationSection(state)
    }
}

/** Универсальная строка магазина/крафта — вынесена, чтобы не дублировать разметку 4 раза. */
@Composable
private fun ShopRow(
    iconRes: String,
    title: String,
    subtitle: String?,
    priceContent: @Composable () -> Unit,
    buttonLabel: String,
    buttonEnabled: Boolean,
    buttonActiveColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    highlighted: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(PanelDark)
            .border(1.dp, if (highlighted) ResourceGreen.copy(alpha = 0.35f) else BorderMuted, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)).background(PanelDarker),
            contentAlignment = Alignment.Center
        ) {
            GameIcon(iconRes, 26.dp)
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            subtitle?.let { Text(it, color = TextSecondary, fontSize = 11.sp) }
            Spacer(Modifier.height(4.dp))
            priceContent()
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (buttonEnabled) buttonActiveColor else BorderMuted)
                .clickable(enabled = buttonEnabled, onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = buttonLabel,
                color = if (buttonEnabled) BgDark else TextMuted,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
    }
}

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
    var tab by remember { mutableStateOf(0) } // 0 инвентарь, 1 ресурсы(крафт), 2 магазин

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
                CoinBadge(coins = state.coins)
            }

            Spacer(Modifier.height(16.dp))

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
                        CraftList(state)
                    }
                    2 -> Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        ShopList(state)
                    }
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
private fun CraftList(state: GameState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("КРАФТ", color = TextMuted, fontSize = 11.sp, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 2.dp))
        CRAFT_ITEMS.forEach { item ->
            val done = state.ownedCraftItems[item.id] == true
            val affordable = state.canCraft(item)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(PanelDark)
                    .border(1.dp, if (done) ResourceGreen.copy(alpha = 0.35f) else BorderMuted, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Box(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)).background(PanelDarker),
                    contentAlignment = Alignment.Center
                ) {
                    GameIcon(item.iconRes, 26.dp)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(item.displayName, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(item.description, color = TextSecondary, fontSize = 11.sp)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        item.cost.forEach { (type, need) ->
                            val has = (state.resources[type] ?: 0) >= need
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                GameIcon(type.iconRes, 12.dp)
                                Text(need.toString(), color = if (has) TextSecondary else WarnRed, fontSize = 11.sp)
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (done) BorderMuted else if (affordable) AccentRust else BorderMuted)
                        .clickable(enabled = !done && affordable) { state.craft(item) }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = if (done) "Готово" else "Собрать",
                        color = if (done) ResourceGreen else if (affordable) BgDark else TextMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ShopList(state: GameState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("РЕСУРСЫ ЗА МОНЕТЫ", color = TextMuted, fontSize = 11.sp, letterSpacing = 1.sp)
        SHOP_RESOURCE_OFFERS.forEach { offer ->
            val affordable = state.coins >= offer.price
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(PanelDark)
                    .border(1.dp, BorderMuted, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Box(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)).background(PanelDarker),
                    contentAlignment = Alignment.Center
                ) {
                    GameIcon(offer.resource.iconRes, 26.dp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("${offer.amount} × ${offer.resource.displayName}", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        GameIcon(COIN_ICON_RES, 12.dp)
                        Text(offer.price.toString(), color = TextSecondary, fontSize = 11.sp)
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (affordable) AccentRust else BorderMuted)
                        .clickable(enabled = affordable) { state.buyResource(offer) }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = if (affordable) "Купить" else "Мало монет",
                        color = if (affordable) BgDark else TextMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Text("ПРЕДМЕТЫ ЗА МОНЕТЫ", color = TextMuted, fontSize = 11.sp, letterSpacing = 1.sp)
        SHOP_ITEMS.forEach { item ->
            val affordable = state.coins >= item.price
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(PanelDark)
                    .border(1.dp, BorderMuted, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Box(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)).background(PanelDarker),
                    contentAlignment = Alignment.Center
                ) {
                    GameIcon(item.iconRes, 26.dp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.displayName, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(item.description, color = TextSecondary, fontSize = 11.sp)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        GameIcon(COIN_ICON_RES, 12.dp)
                        Text(item.price.toString(), color = TextSecondary, fontSize = 11.sp)
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (affordable) AccentRust else BorderMuted)
                        .clickable(enabled = affordable) { state.buyItem(item) }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = if (affordable) "Купить" else "Мало монет",
                        color = if (affordable) BgDark else TextMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

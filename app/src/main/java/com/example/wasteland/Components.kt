package com.example.wasteland

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GameIcon(iconRes: String, size: Dp) {
    val context = LocalContext.current
    val resId = IconResolver.resolve(context, iconRes)
    Image(
        painter = painterResource(id = resId),
        contentDescription = null,
        modifier = Modifier.size(size)
    )
}

@Composable
fun CoinBadge(coins: Double, coinsPerSecond: Double = 0.0) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(PanelDarker)
            .border(1.dp, Color(0xFF4A3F2C), RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        GameIcon(COIN_ICON_RES, 26.dp)
        Column {
            Text(
                text = formatCoins(coins),
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            if (coinsPerSecond > 0) {
                Text(
                    text = "+${formatCoins(coinsPerSecond)}/сек",
                    color = ResourceGreen,
                    fontSize = 10.sp
                )
            }
        }
    }
}

/**
 * Форматирование монет. Раньше "else -> value.toInt()" обрезал дробную часть,
 * из-за чего доход вида "0.8/сек" отображался как "0/сек" и казалось, что
 * дохода нет вовсе. Теперь маленькие значения (< 1000) показываются с одним
 * знаком после запятой, если есть дробная часть — доход виден полностью.
 */
fun formatCoins(value: Double): String {
    return when {
        value >= 1_000_000 -> "%.2fM".format(value / 1_000_000)
        value >= 1_000 -> "%.1fK".format(value / 1_000)
        value > 0 && value < 1 -> "%.2f".format(value)
        value == value.toInt().toDouble() -> "%,d".format(value.toInt()).replace(",", " ")
        else -> "%.1f".format(value)
    }
}

@Composable
fun HpBar(current: Int, max: Int, label: String, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(label, color = TextSecondary, fontSize = 10.sp)
            Text("$current / $max", color = TextSecondary, fontSize = 10.sp)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(PanelDarker)
        ) {
            val fraction = if (max > 0) (current.toFloat() / max.toFloat()).coerceIn(0f, 1f) else 0f
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
    }
}

@Composable
fun ResourceCard(type: ResourceType, amount: Int) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(PanelDark)
            .border(1.dp, BorderMuted, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            GameIcon(type.iconRes, 30.dp)
            Text(
                text = amount.toString(),
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
        Text(text = type.displayName, color = TextSecondary, fontSize = 10.sp)
    }
}

/**
 * Секция доната в магазине. Показывает реальные цены из Google Play
 * (после того как товары загружены — см. BillingManager.queryProductDetails),
 * и запускает системное окно оплаты Google Play при нажатии.
 *
 * Пока приложение не опубликовано в Play Console с настроенными товарами,
 * цены будут отображаться как "—" и кнопка покажет предупреждение —
 * это ожидаемое поведение, см. комментарий в начале BillingManager.kt.
 */
@Composable
fun DonationSection(state: GameState) {
    val billing = LocalBillingManager.current
    val context = LocalContext.current
    val activity = context as? android.app.Activity

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("ПОДДЕРЖАТЬ ИГРУ", color = TextMuted, fontSize = 11.sp, letterSpacing = 1.sp)
        Text(
            "Донат ускоряет прогресс (монеты + временный буст дохода x2), но не даёт эксклюзивного контента.",
            color = TextSecondary,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        DONATION_PRODUCTS.forEach { product ->
            val price = billing?.prices?.get(product.productId)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(PanelDark)
                    .border(1.dp, Color(0xFF4A3F2C), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Box(
                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)).background(PanelDarker),
                    contentAlignment = Alignment.Center
                ) {
                    GameIcon(donationIconFor(product.productId), 34.dp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(donationLabelFor(product.productId), color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(
                        "+${product.coinReward} монет" + if (product.boostSeconds > 0) " · буст x2 на ${(product.boostSeconds / 60).toInt()} мин" else "",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(AccentRust)
                        .clickable(enabled = activity != null) {
                            if (billing != null && activity != null) {
                                billing.launchPurchase(activity, product.productId)
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = price ?: "…",
                        color = BgDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

private fun donationLabelFor(productId: String): String = when (productId) {
    "donation_small" -> "Мешочек монет"
    "donation_medium" -> "Сундучок"
    "donation_large" -> "Большой сундук"
    "donation_mega" -> "Мега-сундук"
    else -> "Донат"
}

private fun donationIconFor(productId: String): String = when (productId) {
    "donation_small" -> "coins_2"
    "donation_medium" -> "coins_3"
    "donation_large" -> "coins_4"
    "donation_mega" -> "coins_4"
    else -> COIN_ICON_RES
}

/**
 * Вкладки сгруппированы по смыслу и отображаются в ДВЕ строки — так все
 * 8 активностей помещаются без сжатия текста в нечитаемый размер, и при
 * этом порядок явно "рассортирован": прогресс/экономика сверху,
 * снаряжение/бой/настройки снизу.
 */
data class TabDef(val index: Int, val label: String)

val TOP_TABS = listOf(
    TabDef(0, "Инвентарь"),
    TabDef(1, "Крафт"),
    TabDef(2, "База"),
    TabDef(6, "Кликер"),
)
val BOTTOM_TABS = listOf(
    TabDef(3, "Магазин"),
    TabDef(7, "Казино"),
    TabDef(4, "Бой"),
    TabDef(5, "Настройки"),
)

@Composable
fun TabBar(selected: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, BorderMuted, RoundedCornerShape(8.dp))
        ) {
            TOP_TABS.forEach { tab ->
                TabItem(tab.label, selected == tab.index, Modifier.weight(1f)) { onSelect(tab.index) }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, BorderMuted, RoundedCornerShape(8.dp))
        ) {
            BOTTOM_TABS.forEach { tab ->
                TabItem(tab.label, selected == tab.index, Modifier.weight(1f)) { onSelect(tab.index) }
            }
        }
    }
}

@Composable
private fun TabItem(label: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(if (active) AccentRust else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 12.dp)
    ) {
        Text(
            text = label,
            color = if (active) BgDark else TextSecondary,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            maxLines = 1
        )
    }
}

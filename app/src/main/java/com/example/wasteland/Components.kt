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
fun CoinBadge(coins: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(PanelDarker)
            .border(1.dp, Color(0xFF4A3F2C), RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        GameIcon(COIN_ICON_RES, 20.dp)
        Text(
            text = "%,d".format(coins).replace(",", " "),
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
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
            GameIcon(type.iconRes, 22.dp)
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

@Composable
fun TabBar(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, BorderMuted, RoundedCornerShape(8.dp))
    ) {
        TabItem("Инвентарь", selected == 0, Modifier.weight(1f)) { onSelect(0) }
        TabItem("Ресурсы", selected == 1, Modifier.weight(1f)) { onSelect(1) }
        TabItem("Магазин", selected == 2, Modifier.weight(1f)) { onSelect(2) }
    }
}

@Composable
private fun TabItem(label: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(if (active) AccentRust else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = label,
            color = if (active) BgDark else TextSecondary,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}

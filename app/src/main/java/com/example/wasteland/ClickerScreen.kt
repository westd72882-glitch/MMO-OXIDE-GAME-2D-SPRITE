package com.example.wasteland

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * Кликер — отдельная активность: тап по большой монете даёт мгновенный
 * доход (не зависит от пассивного дохода зданий), прокачка увеличивает
 * доход за тап. Простой и понятный "idle+active" геймплей вдобавок
 * к пассивному доходу из вкладки "База".
 */
@Composable
fun ClickerScreen(state: GameState, gameSave: GameSave? = null) {
    val scope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("КЛИКЕР", color = TextMuted, fontSize = 11.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            "Тапайте по монете — каждый тап приносит доход, независимый от построек",
            color = TextSecondary,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            StatChip(label = "За тап", value = "+${formatCoins(state.coinsPerClick())}")
            StatChip(label = "Тапов всего", value = state.totalClicks.toString())
        }

        Spacer(Modifier.height(28.dp))

        Box(
            modifier = Modifier
                .size(180.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                }
                .clip(CircleShape)
                .background(PanelDark)
                .border(3.dp, AccentRust, CircleShape)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            state.clickCoins()
                            gameSave?.save(state)
                            scope.launch {
                                scale.snapTo(0.9f)
                                scale.animateTo(1f)
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            GameIcon(COIN_ICON_RES, 90.dp)
        }

        Spacer(Modifier.height(28.dp))

        val cost = state.clickUpgradeCost()
        val affordable = state.coins >= cost
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
            Column(modifier = Modifier.weight(1f)) {
                Text("Прокачать клик · Ур. ${state.clickLevel}", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("Следующий уровень: +${((state.clickLevel + 1) * 2 + 1)} монет за тап", color = TextSecondary, fontSize = 11.sp)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    GameIcon(COIN_ICON_RES, 12.dp)
                    Text(cost.toString(), color = if (affordable) TextSecondary else WarnRed, fontSize = 11.sp)
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (affordable) AccentRust else BorderMuted)
                    .clickable(enabled = affordable) {
                        state.upgradeClick()
                        gameSave?.save(state)
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Прокачать",
                    color = if (affordable) BgDark else TextMuted,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(PanelDark)
            .border(1.dp, BorderMuted, RoundedCornerShape(10.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(value, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(label, color = TextSecondary, fontSize = 10.sp)
    }
}

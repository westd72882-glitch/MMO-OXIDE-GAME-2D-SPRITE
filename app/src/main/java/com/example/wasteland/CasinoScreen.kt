package com.example.wasteland

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Казино — играется исключительно на внутриигровые монеты (не на реальные
 * деньги), чисто развлекательная активность: орёл/решка и слоты.
 * Ставка задаётся кнопками +/- фиксированными шагами, чтобы не открывать
 * системную клавиатуру ради быстрой игры.
 */
@Composable
fun CasinoScreen(state: GameState, gameSave: GameSave? = null) {
    var bet by remember { mutableStateOf(10) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text("КАЗИНО", color = TextMuted, fontSize = 11.sp, letterSpacing = 2.sp)
        Text(
            "Только внутриигровые монеты — азарт без риска для кошелька",
            color = TextSecondary,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(vertical = 8.dp)) {
            StatPill("Побед", state.casinoWins.toString(), ResourceGreen)
            StatPill("Поражений", state.casinoLosses.toString(), WarnRed)
        }

        // --- Ставка ---
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
            BetButton("-10") { bet = (bet - 10).coerceAtLeast(1) }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text("Ставка", color = TextSecondary, fontSize = 10.sp)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    GameIcon(COIN_ICON_RES, 16.dp)
                    Text(bet.toString(), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            BetButton("+10") { bet = (bet + 10).coerceAtMost(state.coins.toInt().coerceAtLeast(10)) }
        }

        Spacer(Modifier.height(16.dp))

        // --- Орёл / Решка ---
        Text("ОРЁЛ ИЛИ РЕШКА · x2", color = TextMuted, fontSize = 11.sp, letterSpacing = 1.sp)
        Text("50/50 — угадали монету, ставка удваивается", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(bottom = 6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            CasinoActionButton("Орёл", Modifier.weight(1f)) {
                state.playCoinFlip(bet, guessHeads = true)
                gameSave?.save(state)
            }
            CasinoActionButton("Решка", Modifier.weight(1f)) {
                state.playCoinFlip(bet, guessHeads = false)
                gameSave?.save(state)
            }
        }

        Spacer(Modifier.height(20.dp))

        // --- Слоты ---
        Text("СЛОТЫ", color = TextMuted, fontSize = 11.sp, letterSpacing = 1.sp)
        Text("3 одинаковых 7️⃣ — x20, 3 одинаковых — x8, любая пара — x1.5", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(bottom = 6.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(PanelDarker)
                .border(1.dp, BorderMuted, RoundedCornerShape(10.dp))
                .padding(16.dp)
        ) {
            state.lastSlotsReels.forEach { symbol ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(PanelDark)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(symbol, fontSize = 28.sp)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        CasinoActionButton("Крутить барабан", Modifier.fillMaxWidth()) {
            state.playSlots(bet)
            gameSave?.save(state)
        }

        state.lastCasinoResult?.let { result ->
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (result.won) ResourceGreen.copy(alpha = 0.15f) else WarnRed.copy(alpha = 0.15f))
                    .border(1.dp, if (result.won) ResourceGreen else WarnRed, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = result.message,
                    color = if (result.won) ResourceGreen else WarnRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StatPill(label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(PanelDark)
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text(label, color = TextSecondary, fontSize = 10.sp)
    }
}

@Composable
private fun BetButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(PanelDarker)
            .border(1.dp, BorderMuted, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(label, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun CasinoActionButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(AccentRust)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = BgDark, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

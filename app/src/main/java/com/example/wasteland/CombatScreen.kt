package com.example.wasteland

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Вкладка "Бой". Если бой не начат — список врагов для выбора.
 * Если бой идёт — экран боя с HP игрока/врага, кнопкой атаки
 * (использует экипированное оружие из GameState.equippedWeapon())
 * и кнопкой отступления.
 */
@Composable
fun CombatScreen(state: GameState) {
    val enemy = state.currentEnemy
    if (enemy == null) {
        EnemySelectScreen(state)
    } else {
        ActiveCombatScreen(state, enemy)
    }
}

@Composable
private fun EnemySelectScreen(state: GameState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("ВЫБЕРИТЕ ЦЕЛЬ", color = TextMuted, fontSize = 11.sp, letterSpacing = 1.sp)

        val weapon = state.equippedWeapon()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(PanelDark)
                .padding(10.dp)
        ) {
            if (weapon != null) {
                GameIcon(weapon.iconRes, 20.dp)
                Text("Экипировано: ${weapon.displayName} (урон ${weapon.damage})", color = TextSecondary, fontSize = 11.sp)
            } else {
                Text("Оружие не экипировано (кулак, урон 4). Выберите оружие в инвентаре.", color = WarnRed, fontSize = 11.sp)
            }
        }

        ENEMIES.forEach { enemy ->
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
                    GameIcon(enemy.iconRes, 26.dp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(enemy.displayName, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("HP ${enemy.maxHp} · урон ${enemy.damage} · награда ${enemy.coinReward} монет", color = TextSecondary, fontSize = 11.sp)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(AccentRust)
                        .clickable(enabled = state.playerHp > 0) { state.startFight(enemy) }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text("В бой", color = BgDark, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }

        if (state.playerHp <= 0) {
            Text(
                "Вы без сознания. Используйте расходники (леденец) в инвентаре, чтобы восстановить HP.",
                color = WarnRed,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun ActiveCombatScreen(state: GameState, enemy: Enemy) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(PanelDark)
                .border(1.dp, BorderMuted, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)).background(PanelDarker),
                        contentAlignment = Alignment.Center
                    ) {
                        GameIcon(enemy.iconRes, 34.dp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(enemy.displayName, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        HpBar(current = state.enemyHp, max = enemy.maxHp, label = "HP врага", color = WarnRed)
                    }
                }

                HpBar(current = state.playerHp, max = state.maxPlayerHp, label = "Ваше HP", color = ResourceGreen)

                state.combatLog?.let {
                    Text(it, color = TextSecondary, fontSize = 12.sp)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AccentRust)
                    .clickable { state.attack() }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("АТАКОВАТЬ", color = BgDark, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PanelDark)
                    .border(1.dp, BorderMuted, RoundedCornerShape(10.dp))
                    .clickable { state.retreat() }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("ОТСТУПИТЬ", color = TextSecondary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

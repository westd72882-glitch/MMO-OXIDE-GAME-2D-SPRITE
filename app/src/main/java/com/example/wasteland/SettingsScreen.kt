package com.example.wasteland

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Экран настроек. Все переключатели пишут сразу в state.settings и
 * немедленно сохраняются на диск (onSettingsChanged), чтобы настройки
 * не терялись при закрытии игры — как и весь остальной прогресс.
 */
@Composable
fun SettingsScreen(
    state: GameState,
    onSettingsChanged: (GameSettings) -> Unit,
    onResetProgress: () -> Unit
) {
    var showResetConfirm by remember { mutableStateOf(false) }
    val s = state.settings

    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("НАСТРОЙКИ", color = TextMuted, fontSize = 11.sp, letterSpacing = 1.sp)

        SettingToggle(
            title = "Музыка",
            subtitle = "Фоновая музыка в игре",
            checked = s.musicEnabled,
            onCheckedChange = { onSettingsChanged(s.copy(musicEnabled = it)) }
        )
        SettingToggle(
            title = "Звуки",
            subtitle = "Звуковые эффекты (бой, покупки)",
            checked = s.soundEnabled,
            onCheckedChange = { onSettingsChanged(s.copy(soundEnabled = it)) }
        )
        SettingToggle(
            title = "Вибрация",
            subtitle = "Отклик при нажатиях и в бою",
            checked = s.vibrationEnabled,
            onCheckedChange = { onSettingsChanged(s.copy(vibrationEnabled = it)) }
        )
        SettingToggle(
            title = "Уведомления",
            subtitle = "Напоминания о накопленном доходе",
            checked = s.notificationsEnabled,
            onCheckedChange = { onSettingsChanged(s.copy(notificationsEnabled = it)) }
        )

        Spacer(Modifier.height(4.dp))
        Text("ПРОИЗВОДИТЕЛЬНОСТЬ", color = TextMuted, fontSize = 11.sp, letterSpacing = 1.sp)
        SettingToggle(
            title = "Режим низкой нагрузки",
            subtitle = "Снижает частоту обновления логики — включите, если игра лагает или греется телефон",
            checked = s.lowPerformanceMode,
            onCheckedChange = { onSettingsChanged(s.copy(lowPerformanceMode = it)) }
        )

        Spacer(Modifier.height(4.dp))
        Text("ДАННЫЕ", color = TextMuted, fontSize = 11.sp, letterSpacing = 1.sp)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(PanelDark)
                .border(1.dp, WarnRed.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                .padding(14.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Сбросить прогресс", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("Удалит сохранение безвозвратно", color = TextSecondary, fontSize = 11.sp)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(WarnRed)
                    .clickable { showResetConfirm = true }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text("Сбросить", color = BgDark, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }

        if (showResetConfirm) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(PanelDarker)
                    .padding(12.dp)
            ) {
                Text("Точно сбросить весь прогресс?", color = TextPrimary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(WarnRed)
                        .clickable {
                            showResetConfirm = false
                            onResetProgress()
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("Да, сбросить", color = BgDark, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(BorderMuted)
                        .clickable { showResetConfirm = false }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("Отмена", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text("OXIDE STATA · v1.1", color = TextMuted, fontSize = 10.sp)
    }
}

@Composable
private fun SettingToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(PanelDark)
            .border(1.dp, BorderMuted, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(subtitle, color = TextSecondary, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = BgDark,
                checkedTrackColor = ResourceGreen,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = PanelDarker
            )
        )
    }
}

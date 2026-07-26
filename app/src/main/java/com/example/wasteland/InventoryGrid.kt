package com.example.wasteland

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch

/**
 * Инвентарь с drag-and-drop, оптимизированный так, чтобы во время
 * перетаскивания НЕ пересчитывался (recompose) весь список.
 *
 * ВАЖНО (исправление видимости при перетаскивании): раньше перетаскиваемая
 * иконка рисовалась ВНУТРИ Box самой ячейки, у которой стоял
 * Modifier.clip(RoundedCornerShape(...)). graphicsLayer{ translationX/Y }
 * двигает контент, но clip обрезает всё, что выходит за границы СВОЕЙ
 * ячейки — поэтому иконка "пропадала", как только заезжала на соседний
 * слот (её обрезало по границе исходной, а не целевой, ячейки).
 *
 * Исправление: содержимое перетаскиваемого слота больше не рисуется внутри
 * ячейки вообще (ячейка на время drag остаётся пустой), а вместо этого
 * рисуется в отдельном НЕобрезаемом оверлее поверх всей сетки —
 * Box(zIndex = высокий, без clip), позиционированном абсолютно по
 * фактическим координатам ячейки + offset пальца. Так иконка всегда
 * видна поверх любых других слотов, куда бы её ни тащили.
 */
@Composable
fun InventoryGrid(state: GameState) {
    val slotSize = 84.dp
    val gap = 10.dp
    val columns = 4

    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    val density = androidx.compose.ui.platform.LocalDensity.current

    var draggingIndex by remember { mutableStateOf(-1) }
    var hoverIndex by remember { mutableStateOf(-1) }
    var dragMoved by remember { mutableStateOf(false) }
    val dragOffset = remember {
        Animatable<Offset, androidx.compose.animation.core.AnimationVector2D>(
            Offset.Zero,
            Offset.VectorConverter
        )
    }
    val scope = rememberCoroutineScope()

    val slotSizePx = with(density) { slotSize.toPx() }
    val gapPx = with(density) { gap.toPx() }
    val cellStride = slotSizePx + gapPx

    fun indexAt(pos: Offset): Int {
        val col = (pos.x / cellStride).toInt().coerceIn(0, columns - 1)
        val row = (pos.y / cellStride).toInt().coerceAtLeast(0)
        val idx = row * columns + col
        return if (idx in state.inventory.indices) idx else -1
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Тап — использовать/экипировать · Перетащите — переставить",
            color = TextMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalArrangement = Arrangement.spacedBy(gap),
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { containerSize = it }
            ) {
                items(
                    count = state.inventory.size,
                    key = { index -> "slot_$index" }
                ) { index ->
                    val slot = state.inventory[index]
                    val isEquipped = slot.itemId != null &&
                        (slot.itemId == state.equippedWeaponId || slot.itemId == state.equippedArmorId)
                    InventorySlotCell(
                        slot = slot,
                        slotSize = slotSize,
                        isDragSource = draggingIndex == index,
                        isHoverTarget = hoverIndex == index && draggingIndex != -1 && draggingIndex != index,
                        isEquipped = isEquipped,
                        onDragStart = {
                            if (state.inventory[index].itemId != null) {
                                draggingIndex = index
                                hoverIndex = index
                                dragMoved = false
                                scope.launch { dragOffset.snapTo(Offset.Zero) }
                            }
                        },
                        onDrag = { delta ->
                            if (draggingIndex == index) {
                                if (delta.getDistance() > 2f) dragMoved = true
                                scope.launch { dragOffset.snapTo(dragOffset.value + delta) }
                                val basePos = cellCenter(index, columns, slotSizePx, gapPx)
                                hoverIndex = indexAt(basePos + dragOffset.value)
                            }
                        },
                        onDragEnd = {
                            if (draggingIndex == index) {
                                val target = hoverIndex
                                val moved = dragMoved
                                if (moved && target != -1 && target != index) {
                                    state.moveInventoryItem(index, target)
                                } else if (!moved) {
                                    state.useItem(state.inventory[index])
                                }
                                draggingIndex = -1
                                hoverIndex = -1
                                scope.launch { dragOffset.snapTo(Offset.Zero) }
                            }
                        }
                    )
                }
            }

            // Необрезаемый оверлей поверх всей сетки — здесь рисуется ТОЛЬКО
            // перетаскиваемая сейчас иконка, поэтому она никогда не прячется
            // за соседними ячейками.
            if (draggingIndex != -1 && draggingIndex in state.inventory.indices) {
                val draggedSlot = state.inventory[draggingIndex]
                val basePos = cellCenter(draggingIndex, columns, slotSizePx, gapPx)
                Box(
                    modifier = Modifier
                        .zIndex(10f)
                        .graphicsLayer {
                            val offset = dragOffset.value
                            translationX = basePos.x + offset.x - slotSizePx / 2f
                            translationY = basePos.y + offset.y - slotSizePx / 2f
                            scaleX = 1.15f
                            scaleY = 1.15f
                            shadowElevation = 16f
                        }
                        .size(slotSize),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        GameIcon(draggedSlot.iconRes, 44.dp)
                        if (draggedSlot.count > 1) {
                            Text(
                                text = draggedSlot.count.toString(),
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun cellCenter(index: Int, columns: Int, slotSizePx: Float, gapPx: Float): Offset {
    val stride = slotSizePx + gapPx
    val col = index % columns
    val row = index / columns
    return Offset(
        x = col * stride + slotSizePx / 2f,
        y = row * stride + slotSizePx / 2f
    )
}

/**
 * Отдельный composable для ячейки инвентаря. Пока предмет перетаскивается
 * (isDragSource), ячейка НЕ рисует свою иконку — она рисуется в оверлее
 * над всей сеткой (см. InventoryGrid), чтобы не обрезаться границами ячейки.
 */
@Composable
private fun InventorySlotCell(
    slot: InventorySlot,
    slotSize: Dp,
    isDragSource: Boolean,
    isHoverTarget: Boolean,
    isEquipped: Boolean,
    onDragStart: () -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(slotSize)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isHoverTarget) PanelDark.copy(alpha = 0.6f) else PanelDarker)
            .border(
                width = if (isHoverTarget || isEquipped) 2.dp else 1.dp,
                color = if (isHoverTarget) AccentRust else if (isEquipped) ResourceGreen else BorderMuted,
                shape = RoundedCornerShape(10.dp)
            )
            .then(
                if (slot.itemId != null) {
                    Modifier.pointerInput(slot.itemId) {
                        detectDragGestures(
                            onDragStart = { onDragStart() },
                            onDrag = { change, delta ->
                                change.consume()
                                onDrag(delta)
                            },
                            onDragEnd = { onDragEnd() },
                            onDragCancel = { onDragEnd() }
                        )
                    }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (slot.itemId != null && !isDragSource) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                GameIcon(slot.iconRes, 40.dp)
                if (slot.count > 1) {
                    Text(
                        text = slot.count.toString(),
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

package com.example.wasteland

import androidx.compose.animation.core.Animatable
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
 * Ключевая идея: во время drag двигается только графический слой
 * (Modifier.graphicsLayer), который читает Animatable.value напрямую —
 * это триггерит re-draw, а не recomposition всей сетки. Позиция
 * ячейки под пальцем вычисляется математически по сетке (индекс -> row/col),
 * а не через onGloballyPositioned на каждой ячейке (было главным источником
 * лагов: до 24 колбэков на каждый кадр перетаскивания).
 */
@Composable
fun InventoryGrid(state: GameState) {
    val slotSize = 76.dp
    val gap = 8.dp
    val columns = 4

    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    val density = androidx.compose.ui.platform.LocalDensity.current

    var draggingIndex by remember { mutableStateOf(-1) }
    var hoverIndex by remember { mutableStateOf(-1) }
    var dragMoved by remember { mutableStateOf(false) }
    val dragOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
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
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalArrangement = Arrangement.spacedBy(gap),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
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
                    dragOffsetProvider = { dragOffset.value },
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
                                // Перемещения почти не было — считаем это тапом:
                                // используем/экипируем предмет вместо переноса.
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
 * Отдельный composable для ячейки инвентаря. Вынесен отдельно, чтобы
 * Compose мог скипать recomposition ячеек, не участвующих в drag —
 * благодаря стабильным параметрам и лямбда-провайдеру dragOffsetProvider
 * (читается только внутри graphicsLayer, а не как обычный state здесь).
 */
@Composable
private fun InventorySlotCell(
    slot: InventorySlot,
    slotSize: Dp,
    isDragSource: Boolean,
    isHoverTarget: Boolean,
    isEquipped: Boolean,
    dragOffsetProvider: () -> Offset,
    onDragStart: () -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(slotSize)
            .zIndex(if (isDragSource) 1f else 0f)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isHoverTarget) PanelDark.copy(alpha = 0.6f) else PanelDarker)
            .border(
                width = if (isHoverTarget || isEquipped) 2.dp else 1.dp,
                color = if (isHoverTarget) AccentRust else if (isEquipped) ResourceGreen else BorderMuted,
                shape = RoundedCornerShape(8.dp)
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
        if (slot.itemId != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    // Читаем offset напрямую здесь — это единственное место,
                    // которое инвалидируется на каждый кадр драга (re-draw слоя),
                    // а НЕ recomposition всего дерева.
                    val offset = if (isDragSource) dragOffsetProvider() else Offset.Zero
                    translationX = offset.x
                    translationY = offset.y
                    scaleX = if (isDragSource) 1.12f else 1f
                    scaleY = if (isDragSource) 1.12f else 1f
                    alpha = if (isDragSource) 0.92f else 1f
                    shadowElevation = if (isDragSource) 12f else 0f
                }
            ) {
                GameIcon(slot.iconRes, 32.dp)
                if (slot.count > 1) {
                    Text(
                        text = slot.count.toString(),
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

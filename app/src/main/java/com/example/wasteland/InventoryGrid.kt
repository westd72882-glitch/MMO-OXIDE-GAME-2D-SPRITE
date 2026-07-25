package com.example.wasteland

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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

@Composable
fun InventoryGrid(state: GameState) {
    val slotSize = 76.dp
    val columns = 4

    val slotBounds = remember { mutableStateMapOf<Int, Rect>() }

    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var dragStartPos by remember { mutableStateOf(Offset.Zero) }
    var hoverIndex by remember { mutableStateOf<Int?>(null) }

    Column {
        Text(
            text = "Перетащите предмет, чтобы переставить",
            color = TextMuted,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(state.inventory.size) { index ->
                val slot = state.inventory[index]
                val isDragSource = draggingIndex == index
                val isHoverTarget = hoverIndex == index && draggingIndex != null && draggingIndex != index

                Box(
                    modifier = Modifier
                        .size(slotSize)
                        .onGloballyPositioned { coords ->
                            val pos = coords.positionInRoot()
                            slotBounds[index] = Rect(offset = pos, size = coords.size.toSize())
                        }
                        .zIndex(if (isDragSource) 1f else 0f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isHoverTarget) PanelDark.copy(alpha = 0.6f) else PanelDarker)
                        .border(
                            width = if (isHoverTarget) 2.dp else 1.dp,
                            color = if (isHoverTarget) AccentRust else BorderMuted,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .then(
                            if (slot.itemId != null) {
                                Modifier.pointerInput(index, state.inventory.size) {
                                    detectDragGestures(
                                        onDragStart = {
                                            draggingIndex = index
                                            dragStartPos = slotBounds[index]?.center ?: Offset.Zero
                                            dragOffset = Offset.Zero
                                        },
                                        onDrag = { change, delta ->
                                            change.consume()
                                            dragOffset += delta
                                            val currentPos = dragStartPos + dragOffset
                                            hoverIndex = slotBounds.entries.firstOrNull { (_, rect) ->
                                                rect.contains(currentPos)
                                            }?.key
                                        },
                                        onDragEnd = {
                                            val target = hoverIndex
                                            val source = draggingIndex
                                            if (target != null && source != null) {
                                                state.moveInventoryItem(source, target)
                                            }
                                            draggingIndex = null
                                            hoverIndex = null
                                            dragOffset = Offset.Zero
                                        },
                                        onDragCancel = {
                                            draggingIndex = null
                                            hoverIndex = null
                                            dragOffset = Offset.Zero
                                        }
                                    )
                                }
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (slot.itemId != null) {
                        val offsetToApply = if (isDragSource) dragOffset else Offset.Zero
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.graphicsLayer {
                                translationX = offsetToApply.x
                                translationY = offsetToApply.y
                                scaleX = if (isDragSource) 1.12f else 1f
                                scaleY = if (isDragSource) 1.12f else 1f
                                alpha = if (isDragSource) 0.9f else 1f
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
        }
    }
}

private fun IntSize.toSize() = Size(width.toFloat(), height.toFloat())

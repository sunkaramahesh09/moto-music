package com.motomusic.app.presentation.player

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Hosts the full player as a sheet over the app rather than as a screen of its own, so that
 * collapsing it — by the arrow, a swipe down, or the system back gesture — slides it away to
 * reveal the screen the user came from, which stays alive underneath the whole time.
 *
 * A downward swipe anywhere on the player follows the finger once its content is scrolled to
 * the top. Letting go past a quarter of the screen, or flicking, collapses it; anything less
 * springs back. On Android 14+ the predictive back gesture drives the same motion.
 *
 * [swipeEnabled] lets the content opt out, for example while the queue with its own drag
 * handles is showing.
 */
@Composable
fun PlayerOverlay(
    expanded: Boolean,
    onCollapse: () -> Unit,
    swipeEnabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val currentOnCollapse by rememberUpdatedState(onCollapse)
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val flingThreshold = with(density) { FLING_DISMISS_VELOCITY.toPx() }

    // How far the player has been pulled down, in pixels.
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var settleJob by remember { mutableStateOf<Job?>(null) }

    // A fresh expand always starts from the top, whatever the last collapse left behind.
    LaunchedEffect(expanded) {
        if (expanded) {
            settleJob?.cancel()
            dragOffset = 0f
        }
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val heightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)

        fun settle(velocity: Float) {
            settleJob?.cancel()
            val dismiss = dragOffset > heightPx * DISMISS_FRACTION || velocity > flingThreshold
            settleJob = scope.launch {
                if (dismiss) {
                    animate(dragOffset, heightPx, velocity, tween(DISMISS_MS)) { value, _ -> dragOffset = value }
                    currentOnCollapse()
                } else {
                    animate(
                        dragOffset,
                        0f,
                        velocity,
                        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
                    ) { value, _ -> dragOffset = value }
                }
            }
        }

        fun dragBy(delta: Float) {
            settleJob?.cancel()
            dragOffset = (dragOffset + delta).coerceIn(0f, heightPx)
        }

        val nestedScroll = remember(swipeEnabled, heightPx, flingThreshold) {
            object : NestedScrollConnection {
                // Finger moving back up while the player is pulled down: lift the player first.
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (!swipeEnabled || source != NestedScrollSource.UserInput) return Offset.Zero
                    if (available.y >= 0f || dragOffset <= 0f) return Offset.Zero
                    val consumed = available.y.coerceAtLeast(-dragOffset)
                    dragBy(consumed)
                    return Offset(0f, consumed)
                }

                // Content already at its top and the finger still pulling down: move the player.
                override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                    if (!swipeEnabled || source != NestedScrollSource.UserInput || available.y <= 0f) return Offset.Zero
                    dragBy(available.y)
                    return Offset(0f, available.y)
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    if (dragOffset <= 0f) return Velocity.Zero
                    settle(available.y)
                    return available
                }
            }
        }

        val progressOf = { (dragOffset / heightPx).coerceIn(0f, 1f) }

        AnimatedVisibility(
            visible = expanded,
            enter = EnterTransition.None,
            exit = ExitTransition.None,
        ) {
            // The scrim dims the screen underneath and fades as the player is pulled away.
            Box(
                Modifier
                    .fillMaxSize()
                    .animateEnterExit(enter = fadeIn(tween(ENTER_MS)), exit = fadeOut(tween(EXIT_MS)))
                    .graphicsLayer { alpha = 1f - progressOf() }
                    .background(Color.Black.copy(alpha = SCRIM_ALPHA)),
            )

            Box(
                Modifier
                    .fillMaxSize()
                    .animateEnterExit(
                        enter = slideInVertically(tween(ENTER_MS, easing = EmphasizedDecelerate)) { it } +
                            fadeIn(tween(ENTER_MS / 2)),
                        exit = slideOutVertically(tween(EXIT_MS, easing = EmphasizedAccelerate)) { it },
                    )
                    .graphicsLayer {
                        translationY = dragOffset
                        // A slight shrink tells the user the player is being put away, not scrolled.
                        val shrink = 1f - progressOf() * DRAG_SCALE_LOSS
                        scaleX = shrink
                        scaleY = shrink
                    }
                    .clip(RoundedCornerShape(topStart = cornerFor(dragOffset), topEnd = cornerFor(dragOffset)))
                    .background(MaterialTheme.colorScheme.surface)
                    .nestedScroll(nestedScroll)
                    .draggable(
                        state = rememberDraggableState { dragBy(it) },
                        orientation = Orientation.Vertical,
                        enabled = swipeEnabled,
                        onDragStopped = { velocity -> settle(velocity) },
                    ),
            ) {
                content()
            }
        }

        // Android hands Back to the most recently registered handler. Registering this one each
        // time the player opens puts it ahead of the navigation graph's own, which is underneath.
        if (expanded) PredictiveBackHandler(enabled = swipeEnabled) { progress ->
            settleJob?.cancel()
            val start = dragOffset
            try {
                progress.collect { event ->
                    dragOffset = start + (heightPx * PREDICTIVE_BACK_TRAVEL - start) * event.progress
                }
                settle(velocity = flingThreshold + 1f)
            } catch (e: CancellationException) {
                settle(velocity = 0f)
                throw e
            }
        }
    }
}

/** Corners round in as the player leaves full screen, the way a sheet looks. */
private fun cornerFor(offsetPx: Float) = if (offsetPx > 0f) 28.dp else 0.dp

private val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
private val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

private const val ENTER_MS = 380
private const val EXIT_MS = 250
private const val DISMISS_MS = 220
private const val SCRIM_ALPHA = 0.32f
private const val DISMISS_FRACTION = 0.25f
private const val DRAG_SCALE_LOSS = 0.08f

/** How far the predictive back gesture pulls the player before the user commits. */
private const val PREDICTIVE_BACK_TRAVEL = 0.2f

private val FLING_DISMISS_VELOCITY = 1_200.dp

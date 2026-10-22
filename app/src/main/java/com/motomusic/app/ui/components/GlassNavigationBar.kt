package com.motomusic.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/** One tab of [GlassNavigationBar]. */
data class GlassTab(val label: String, val icon: ImageVector)

/**
 * The bottom tab bar: a translucent strip with a hairline top edge, seated on the system
 * navigation bar. The active tab lights up in the accent with a small scale pulse; the rest
 * rest in muted grey. Content scrolls underneath it, which is what makes it read as glass.
 */
@Composable
fun GlassNavigationBar(
    tabs: List<GlassTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.navigationBars,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = GLASS_ALPHA)),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(hairlineColor()),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(windowInsets)
                .heightIn(min = 56.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { index, tab ->
                GlassTabItem(
                    tab = tab,
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun GlassTabItem(
    tab: GlassTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "tabColor",
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.94f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 700f),
        label = "tabScale",
    )
    Column(
        modifier = modifier
            .heightIn(min = 56.dp)
            .selectable(selected = selected, onClick = onClick, role = Role.Tab),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(25.dp).scale(scale),
        )
        Spacer(Modifier.height(2.dp))
        Text(text = tab.label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

/**
 * How opaque the floating chrome is. Without a real backdrop blur, any translucency lets list
 * text read through the bar and the mini player, so they stay solid.
 */
const val GLASS_ALPHA = 1f

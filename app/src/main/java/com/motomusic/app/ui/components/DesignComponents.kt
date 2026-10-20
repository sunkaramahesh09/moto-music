package com.motomusic.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.motomusic.app.ui.theme.MotoColors

/*
 * The building blocks of the Sonic Glass look: capsules for anything tappable that is not a
 * card, high-radius cards whose contents sit concentrically inside them, hairline edges instead
 * of shadows, and a small press-in scale on every card so touches feel physical.
 */

/** The colour reserved for favourites, whatever palette is active. */
val HeartColor: Color get() = MotoColors.Heart

/** Hairline edge for cards and floating chrome; stronger than a divider, softer than a border. */
@Composable
fun hairlineColor(): Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

/**
 * Scales content down slightly while pressed, then springs back. The scale is read inside the
 * layer block, so a press only redraws the card instead of recomposing it.
 */
fun Modifier.pressScale(interactionSource: MutableInteractionSource): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale = animateFloatAsState(
        targetValue = if (pressed) PRESSED_SCALE else 1f,
        animationSpec = if (pressed) tween(120) else spring(dampingRatio = 0.6f, stiffness = 600f),
        label = "pressScale",
    )
    Modifier.graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}

private const val PRESSED_SCALE = 0.96f

/** A card that presses in when touched. */
@Composable
fun PressableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    color: Color = MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .pressScale(interaction)
            .clip(shape)
            .background(color)
            .border(0.5.dp, hairlineColor(), shape)
            .clickable(interactionSource = interaction, indication = ripple(), onClick = onClick),
    ) {
        content()
    }
}

/**
 * The large title at the top of a top-level screen, with round action buttons on the right,
 * the way the design sets "Library", "Search" and "Browse".
 */
@Composable
fun LargeTitleHeader(
    title: String,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            actions()
        }
    }
}

/** A circular header action. [filled] gives it the tinted disc the design uses for the profile. */
@Composable
fun HeaderIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (filled) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/** A search field look-alike that opens the real search when tapped. */
@Composable
fun SearchPill(
    placeholder: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .heightIn(min = 46.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = placeholder,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A filter or category capsule. Selected ones fill with the accent. */
@Composable
fun CapsuleChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    selected: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    val container = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
    val content = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = container,
        contentColor = content,
        interactionSource = interaction,
        modifier = modifier.pressScale(interaction).heightIn(min = 36.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = if (icon != null) 12.dp else 16.dp, end = if (trailing != null) 6.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
            if (trailing != null) {
                Spacer(Modifier.width(2.dp))
                trailing()
            }
        }
    }
}

/**
 * A micro capsule for audio facts — "LOSSLESS", "FLAC". [onArtwork] gives it a dark glass
 * backing so it stays legible over any cover.
 */
@Composable
fun QualityBadge(
    text: String,
    modifier: Modifier = Modifier,
    onArtwork: Boolean = false,
    accent: Boolean = false,
) {
    val background = when {
        onArtwork -> Color.Black.copy(alpha = 0.62f)
        accent -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    }
    val color = when {
        onArtwork -> Color.White
        accent -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        color = color,
        maxLines = 1,
        modifier = modifier
            .clip(CircleShape)
            .background(background)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/** One tile of the 2×2 quick-access grid: a tinted glyph badge, a bold title and a count. */
@Composable
fun QuickAccessTile(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PressableCard(onClick = onClick, modifier = modifier.semantics(mergeDescendants = true) {}) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tint.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Accent tints for glyph badges, so the quick-access grid is not a wall of one blue. */
object TileTints {
    val Blue = Color(0xFF2997FF)
    val Rose = Color(0xFFFF2D55)
    val Teal = Color(0xFF30B0C7)
    val Purple = Color(0xFFAF52DE)
    val Orange = Color(0xFFFF9F0A)
    val Green = Color(0xFF34C759)
    val Indigo = Color(0xFF5E5CE6)
}

/** "NOW": marks the song that is playing, next to its title. */
@Composable
fun NowBadge(modifier: Modifier = Modifier) {
    QualityBadge(text = "Now", accent = true, modifier = modifier)
}

/** The favourite toggle: an outline heart that fills with rose when loved, with a little pop. */
@Composable
fun HeartButton(
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp,
    title: String? = null,
) {
    val pop by animateFloatAsState(
        targetValue = if (isFavorite) 1f else 0.9f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 500f),
        label = "heartPop",
    )
    IconButton(onClick = onClick, enabled = enabled, modifier = modifier.size(size)) {
        Icon(
            imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            contentDescription = when {
                title == null && isFavorite -> "Remove from favourites"
                title == null -> "Add to favourites"
                isFavorite -> "Remove $title from favourites"
                else -> "Add $title to favourites"
            },
            tint = if (isFavorite) HeartColor else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(iconSize).scale(if (isFavorite) pop else 1f),
        )
    }
}

/** A thin space between sections of a long scrolling screen. */
@Composable
fun SectionGap(height: Dp = 8.dp) {
    Spacer(Modifier.height(height))
}

/** Wraps a section in a single card with hairline edge, for grouped rows. */
@Composable
fun GroupedCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(0.5.dp, hairlineColor(), shape),
    ) {
        content()
    }
}

/** Accessibility helper for groups that should read as one item. */
fun Modifier.describedAs(description: String): Modifier = semantics { contentDescription = description }

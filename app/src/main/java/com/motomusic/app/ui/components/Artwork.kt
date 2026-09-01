package com.motomusic.app.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.motomusic.app.data.mediastore.ArtworkLoader

val LocalArtworkLoader = staticCompositionLocalOf<ArtworkLoader> {
    error("LocalArtworkLoader must be provided by MainActivity")
}

/**
 * Album artwork with a generated fallback.
 *
 * The bitmap is decoded at the size actually requested, so a 56dp list row never holds a
 * full-resolution cover, and already-cached artwork is shown on the first frame to keep
 * scrolling free of flicker.
 */
@Composable
fun ArtworkImage(
    albumId: Long,
    songUri: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 10.dp,
    contentDescription: String? = null,
) {
    val loader = LocalArtworkLoader.current
    val sizePx = with(LocalDensity.current) { size.roundToPx() }

    var bitmap by remember(albumId, songUri, sizePx) {
        mutableStateOf(loader.cached(albumId, songUri, sizePx))
    }
    LaunchedEffect(albumId, songUri, sizePx) {
        if (bitmap == null) bitmap = loader.load(albumId, songUri, sizePx)
    }

    ArtworkSurface(
        bitmap = bitmap,
        albumId = albumId,
        contentDescription = contentDescription,
        modifier = modifier.size(size).clip(RoundedCornerShape(cornerRadius)),
    )
}

/** Square artwork that fills the width it is given, used by the full player and detail headers. */
@Composable
fun LargeArtworkImage(
    albumId: Long,
    songUri: String?,
    sizeHint: Dp,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    contentDescription: String? = null,
) {
    val loader = LocalArtworkLoader.current
    val sizePx = with(LocalDensity.current) { sizeHint.roundToPx() }

    var bitmap by remember(albumId, songUri, sizePx) {
        mutableStateOf(loader.cached(albumId, songUri, sizePx))
    }
    LaunchedEffect(albumId, songUri, sizePx) {
        if (bitmap == null) bitmap = loader.load(albumId, songUri, sizePx)
    }

    ArtworkSurface(
        bitmap = bitmap,
        albumId = albumId,
        contentDescription = contentDescription,
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(cornerRadius)),
    )
}

@Composable
private fun ArtworkSurface(
    bitmap: Bitmap?,
    albumId: Long,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val placeholderColor = placeholderColorFor(albumId)
    // Artwork repeats what the adjacent text already says, so it is hidden from screen
    // readers unless the caller supplies a description of its own.
    val semantics = if (contentDescription == null) Modifier.clearAndSetSemantics {} else Modifier

    Box(modifier = modifier.background(placeholderColor).then(semantics)) {
        Crossfade(targetState = bitmap, animationSpec = tween(180), label = "artwork") { image ->
            if (image != null) {
                Image(
                    bitmap = image.asImageBitmap(),
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = contentDescription,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxSize(0.42f),
                    )
                }
            }
        }
    }
}

/**
 * Songs without a cover get a tinted tile instead of a grey box. The tint comes from the
 * active Material palette, so it follows dynamic colour and both themes.
 */
@Composable
private fun placeholderColorFor(albumId: Long): Color {
    val scheme = MaterialTheme.colorScheme
    val palette = remember(scheme) {
        listOf(
            scheme.primaryContainer,
            scheme.secondaryContainer,
            scheme.tertiaryContainer,
            scheme.surfaceVariant,
        )
    }
    val index = ((albumId % palette.size) + palette.size) % palette.size
    return palette[index.toInt()]
}

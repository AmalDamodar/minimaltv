package com.minimaltv.files.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.minimaltv.files.R

/**
 * Liquid-glass look (dark only: the TV home screen and this app share one calm look), as plain colours: translucent white panels with a bright top edge over a blurred photo.
 * No accent colour on purpose; everything is white, black or a shade of them.
 */
data class GlassColors(
    val dark: Boolean,
    val fillTop: Color,
    val fillBottom: Color,
    val strokeTop: Color,
    val strokeBottom: Color,
    val divider: Color,
    val pill: Color,
    val text: Color,
    val textDim: Color,
    val scrim: Color,
    val wallpaper: Int,
    // the selected row or button: one soft fill, nothing else
    val focusFill: Color,
)

val DarkGlass = GlassColors(
    dark = true,
    fillTop = Color(0x2EFFFFFF),
    fillBottom = Color(0x14FFFFFF),
    strokeTop = Color(0x66FFFFFF),
    strokeBottom = Color(0x14FFFFFF),
    divider = Color(0x1FFFFFFF),
    pill = Color(0x38FFFFFF),
    text = Color.White,
    textDim = Color(0xB3FFFFFF),
    scrim = Color(0x59000000),
    wallpaper = R.drawable.glass_bg_dark,
    focusFill = Color(0x2EFFFFFF),
)

val LocalGlass = staticCompositionLocalOf { DarkGlass }

val GlassPanelShape = RoundedCornerShape(28.dp)

/** A frosted panel: translucent gradient fill, thin bright edge. */
fun Modifier.glass(shape: Shape = GlassPanelShape): Modifier = composed {
    val g = LocalGlass.current
    this
        .clip(shape)
        .background(Brush.verticalGradient(listOf(g.fillTop, g.fillBottom)))
        .border(1.dp, Brush.verticalGradient(listOf(g.strokeTop, g.strokeBottom)), shape)
}

/** The blurred photo behind every screen, with a scrim so text stays readable. */
@Composable
fun GlassBackground(modifier: Modifier = Modifier.fillMaxSize(), content: @Composable () -> Unit) {
    val g = LocalGlass.current
    Box(modifier.background(if (g.dark) Color.Black else Color.White)) {
        Image(
            painterResource(g.wallpaper), contentDescription = null,
            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
        )
        Box(Modifier.fillMaxSize().background(g.scrim))
        content()
    }
}


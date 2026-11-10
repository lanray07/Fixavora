package com.fixavora.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.drawscope.translate

val Navy = Color(0xFF172A3A)
val Teal = Color(0xFF246B73)
val Ivory = Color(0xFFF8F7F3)
val Amber = Color(0xFFD9A45B)

@Composable
fun FixavoraTheme(dark: Boolean, contrast: Boolean, content: @Composable () -> Unit) {
    val colors = if (dark) darkColorScheme(primary = if(contrast) Color(0xFFBDF6FA) else Color(0xFF97D3D8), primaryContainer = Color(0xFF174F55), onPrimaryContainer = Ivory, secondary = Amber, secondaryContainer = Color(0xFF514126), onSecondaryContainer = Ivory, background = if(contrast) Color(0xFF080F14) else Color(0xFF111F2A), surface = if(contrast) Color(0xFF0F1C24) else Color(0xFF1C303C), onSurface = if(contrast) Color.White else Ivory, onBackground = Ivory, onSurfaceVariant = if(contrast) Color.White else Color(0xFFD7E3E6), surfaceVariant = Color(0xFF273E49), outline = if(contrast) Color(0xFFB9D0D7) else Color(0xFF91ADB5))
    else lightColorScheme(primary = if (contrast) Color(0xFF124C53) else Teal, primaryContainer = Color(0xFFDCE6E8), onPrimaryContainer = Navy, secondary = Color(0xFF76511F), secondaryContainer = Color(0xFFF2E5D0), onSecondaryContainer = Navy, background = if(contrast) Color.White else Ivory, surface = Color.White, onSurface = Navy, onBackground = Navy, onSurfaceVariant = if(contrast) Navy else Color(0xFF49454F), surfaceVariant = Color(0xFFE9EFED), outline = if(contrast) Navy else Color(0xFF66777B))
    MaterialTheme(colorScheme = colors, shapes = Shapes(small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp), medium = androidx.compose.foundation.shape.RoundedCornerShape(20.dp), large = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)), content = content)
}

/** Original architectural line illustration, deliberately free of external image dependencies. */
@Composable
fun HomeArt(modifier: Modifier = Modifier, color: Color = Color(0xFF95C5C5)) {
    Canvas(modifier.clipToBounds()) {
        val edge = minOf(size.width, size.height)
        translate((size.width - edge) / 2, (size.height - edge) / 2) {
        val w = edge; val h = edge
        drawCircle(color.copy(alpha = 0.12f), w * .44f, Offset(w * .52f, h * .51f))
        val home = Path().apply { moveTo(w * .19f, h * .46f); lineTo(w * .51f, h * .19f); lineTo(w * .85f, h * .46f); moveTo(w * .28f, h * .4f); lineTo(w * .28f, h * .8f); lineTo(w * .75f, h * .8f); lineTo(w * .75f, h * .4f) }
        drawPath(home, color, style = Stroke(4.dp.toPx()))
        drawRect(color, Offset(w * .38f, h * .49f), androidx.compose.ui.geometry.Size(w * .12f, h * .14f), style = Stroke(3.dp.toPx()))
        drawLine(color, Offset(w * .57f, h * .8f), Offset(w * .57f, h * .57f), 3.dp.toPx())
        drawLine(color, Offset(w * .57f, h * .57f), Offset(w * .66f, h * .57f), 3.dp.toPx())
        drawLine(color, Offset(w * .66f, h * .57f), Offset(w * .66f, h * .8f), 3.dp.toPx())
        drawCircle(Amber, w * .1f, Offset(w * .81f, h * .72f))
        val check = Path().apply { moveTo(w * .76f, h * .72f); lineTo(w * .8f, h * .76f); lineTo(w * .87f, h * .67f) }
        drawPath(check, Navy, style = Stroke(3.dp.toPx()))
        }
    }
}

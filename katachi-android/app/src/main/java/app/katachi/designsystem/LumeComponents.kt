package app.katachi.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** A bordered tile that sinks under the finger. Replaces the stock outlined card. */
@Composable
fun LumeCard(
    modifier: Modifier = Modifier,
    border: BorderStroke = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier
            .let { if (onClick != null) it.pressable(onClick = onClick) else it }
            .clip(shape)
            .border(border, shape),
    ) { content() }
}

/** A rounded progress bar that only ever moves forward smoothly. Replaces the stock linear indicator. */
@Composable
fun LumeProgressBar(progress: Float, modifier: Modifier = Modifier, height: Dp = 6.dp) {
    val shown by animateFloatAsState(progress.coerceIn(0f, 1f), LumeMotion.effects(), label = "bar")
    val track = MaterialTheme.colorScheme.surfaceVariant
    val fill = MaterialTheme.colorScheme.primary
    Canvas(modifier.fillMaxWidth().height(height)) {
        val r = CornerRadius(size.height / 2)
        drawRoundRect(track, Offset.Zero, size, r)
        if (shown > 0f) drawRoundRect(fill, Offset.Zero, Size(size.width * shown, size.height), r)
    }
}

/** A slowly turning arc while something loads. Replaces the stock circular indicator. */
@Composable
fun LumeSpinner(modifier: Modifier = Modifier, size: Dp = 36.dp) {
    val turn = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            turn.snapTo(0f)
            turn.animateTo(360f, tween(1400, easing = LinearEasing))
        }
    }
    val color: Color = MaterialTheme.colorScheme.primary
    Canvas(modifier.size(size)) {
        val w = 4.dp.toPx()
        drawArc(
            color, turn.value, 100f, false, Offset(w / 2, w / 2),
            Size(this.size.width - w, this.size.height - w), style = Stroke(w, cap = StrokeCap.Round),
        )
    }
}

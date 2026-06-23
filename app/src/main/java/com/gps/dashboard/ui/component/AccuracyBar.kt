package com.gps.dashboard.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.gps.dashboard.util.AccuracyLevel
import com.gps.dashboard.ui.theme.*

@Composable
fun AccuracyBar(
    level: AccuracyLevel,
    ratio: Float,
    modifier: Modifier = Modifier,
) {
    val color = when (level) {
        AccuracyLevel.GOOD -> StatusGood
        AccuracyLevel.MEDIUM -> StatusMedium
        AccuracyLevel.BAD -> StatusBad
    }

    val animatedRatio by animateFloatAsState(
        targetValue = ratio.coerceIn(0f, 1f),
        animationSpec = tween(300),
        label = "accuracy_ratio"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Border)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedRatio)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
    }
}

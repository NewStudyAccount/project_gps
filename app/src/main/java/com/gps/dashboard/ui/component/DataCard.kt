package com.gps.dashboard.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.gps.dashboard.ui.theme.*

@Composable
fun DataCard(
    label: String,
    value: String,
    unit: String? = null,
    modifier: Modifier = Modifier,
    extraContent: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Surface)
            .border(1.dp, Border, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(
            text = label,
            style = DataLabel.copy(fontSize = 11.sp),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row {
            Text(
                text = value,
                style = DataValue,
            )
            if (unit != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = unit,
                    style = DataUnit,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        extraContent?.invoke(this)
    }
}

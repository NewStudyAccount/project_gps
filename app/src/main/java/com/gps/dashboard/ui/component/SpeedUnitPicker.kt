package com.gps.dashboard.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gps.dashboard.data.model.SpeedUnit
import com.gps.dashboard.ui.theme.*

@Composable
fun SpeedUnitPicker(
    currentUnit: SpeedUnit,
    onUnitSelected: (SpeedUnit) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Text(
            text = currentUnit.label,
            style = DataUnit.copy(fontSize = 14.sp),
            modifier = Modifier.clickable { expanded = true }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(Surface)
                .clip(RoundedCornerShape(8.dp))
        ) {
            SpeedUnit.entries.forEach { unit ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = unit.label,
                            fontSize = 14.sp,
                            color = if (unit == currentUnit) Primary else TextPrimary,
                            fontFamily = MonoFontFamily,
                        )
                    },
                    onClick = {
                        onUnitSelected(unit)
                        expanded = false
                    },
                    modifier = if (unit == currentUnit) {
                        Modifier.background(Primary.copy(alpha = 0.15f))
                    } else {
                        Modifier
                    }
                )
            }
        }
    }
}

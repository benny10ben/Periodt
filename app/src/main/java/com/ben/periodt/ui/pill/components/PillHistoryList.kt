package com.ben.periodt.ui.pill.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ben.periodt.prediction.pretty
import com.ben.periodt.ui.theme.BricolageGrotesque
import com.ben.periodt.viewmodel.PeriodViewModel

private val SIZE_XXS = 11.sp
private val SIZE_SM  = 13.sp
private val SIZE_LG  = 15.sp

@Composable
fun PillHistoryItem(
    pack: PeriodViewModel.PillPack,
    cardBg: Color,
    textPrimary: Color,
    textSub: Color,
    pillBackground: Color,
    activeAccent: Color,
    normalAccent: Color,
    isSwiping: Boolean = false
) {
    Card(
        shape     = RoundedCornerShape(22.dp),
        colors    = CardDefaults.cardColors(containerColor = cardBg),
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier         = Modifier.size(42.dp).clip(CircleShape).background(pillBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = if (pack.endDate == null) Icons.Rounded.Medication else Icons.Rounded.History,
                    contentDescription = null,
                    tint               = if (pack.endDate == null) activeAccent else normalAccent,
                    modifier           = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = if (pack.endDate == null) "Active Pack" else "Completed Pack",
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = SIZE_LG,
                    color      = if (pack.endDate == null) activeAccent else textPrimary
                )
                Text(
                    text       = "${pack.startDate.pretty()} — ${pack.endDate?.pretty() ?: "Ongoing"}",
                    fontFamily = BricolageGrotesque,
                    fontSize   = SIZE_SM,
                    color      = textSub
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(pillBackground)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    "${pack.pillCount} pills",
                    fontFamily = BricolageGrotesque,
                    fontSize   = SIZE_XXS,
                    color      = textSub
                )
            }
        }
    }
}
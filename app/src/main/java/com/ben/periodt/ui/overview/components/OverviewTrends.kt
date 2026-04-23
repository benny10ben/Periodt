package com.ben.periodt.ui.overview.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ben.periodt.R
import com.ben.periodt.ui.theme.BricolageGrotesque
import com.ben.periodt.ui.theme.LocalAppIsDark

private val SIZE_MD  = 14.sp
private val SIZE_XL  = 20.sp

@Composable
fun RecentTrendsBanner(trends: Triple<String, String, Int>?, cycleCount: Int) {
    if (trends == null || cycleCount == 0) return

    val isDark      = LocalAppIsDark.current
    val textPrimary = if (isDark) Color.White else Color.Black
    val textSub     = if (isDark) Color.White else Color.Black

    val appBackgroundColor = if (isDark) Color.Black else Color.White
    val cardShape = RoundedCornerShape(26.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = appBackgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(0.5.dp, appBackgroundColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    clip = true
                    shape = cardShape
                }
        ) {
            Image(
                painter = painterResource(id = R.drawable.recent),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                if (isDark) Color.Black.copy(alpha = 0.90f) else Color.White.copy(alpha = 1f),
                                if (isDark) Color.Black.copy(alpha = 0.80f) else Color.White.copy(alpha = 0.6f),
                                if (isDark) Color.Black.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.0f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(0.85f)
            ) {
                Text(
                    text = "Recent Trends",
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = SIZE_XL,
                    color = textPrimary
                )

                Spacer(Modifier.height(12.dp))

                val annotatedSummary = buildAnnotatedString {
                    append("Over your last $cycleCount cycle${if (cycleCount > 1) "s" else ""}, your typical flow is ")
                    pushStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, color = textPrimary))
                    append("${trends.first.lowercase()} (${trends.second.lowercase()})")
                    pop()
                    append(", with an average pain level of ")
                    pushStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, color = textPrimary))
                    append("${trends.third}/10")
                    pop()
                    append(".")
                }

                Text(
                    text = annotatedSummary,
                    fontFamily = BricolageGrotesque,
                    fontSize = SIZE_MD,
                    color = textSub,
                    lineHeight = 20.sp
                )
            }
        }
    }
}
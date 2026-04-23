package com.ben.periodt.ui.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ben.periodt.ui.theme.BricolageGrotesque
import com.ben.periodt.ui.theme.LocalAppIsDark

private val SIZE_XS = 12.sp
private val SIZE_SM = 13.sp
private val SIZE_LG = 15.sp

@Composable
fun SettingsSection(title: String, surfaceColor: Color, content: @Composable ColumnScope.() -> Unit) {
    val isDark = LocalAppIsDark.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title.uppercase(),
            fontFamily = BricolageGrotesque,
            fontSize = SIZE_XS,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            color = textPrimaryWithAlpha(isDark),
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = surfaceColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) { content() }
        }
    }
}

@Composable
fun textPrimaryWithAlpha(isDark: Boolean) =
    if (isDark) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.4f)

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    tint: Color,
    showChevron: Boolean = true,
    onClick: () -> Unit
) {
    val isDark       = LocalAppIsDark.current
    val subTextColor = if (isDark) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.5f)

    Column(modifier = Modifier.clickable(onClick = onClick)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontFamily = BricolageGrotesque,
                    fontSize = SIZE_LG,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )
                if (subtitle != null) {
                    Text(
                        subtitle,
                        fontFamily = BricolageGrotesque,
                        fontSize = SIZE_SM,
                        color = subTextColor
                    )
                }
            }
            if (showChevron) {
                Icon(Icons.Rounded.ChevronRight, null, tint = subTextColor.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
            }
        }
    }
}
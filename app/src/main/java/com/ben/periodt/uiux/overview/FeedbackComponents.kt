package com.ben.periodt.uiux.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ben.periodt.ui.theme.BricolageGrotesque
import com.ben.periodt.ui.theme.LocalAppIsDark

@Composable
fun SuccessFeedbackDialog(
    title: String,
    message: String,
    buttonText: String = "Awesome",
    onDismiss: () -> Unit
) {
    val isDark = LocalAppIsDark.current

    // 1. UPDATED GRADIENT SURFACE
    val contentSurface = if (isDark) {
        Brush.linearGradient(
            0.0f to Color.Black,
            1.0f to Color(0xFF1B1B1B)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color(0xFFF8FAFC), Color(0xFFf2f0e3))
        )
    }

    // 2. UPDATED ACCENT COLOR
    val accentColor = if (isDark) Color(0xFFD89046) else Color(0xFF6d9567).copy(alpha = 0.6f)


    val surfaceFallback = if (isDark) Color(0xFF1B1B1B) else Color.White
    val textMain = if (isDark) Color.White else Color(0xFF1B1B1B)
    val textSub = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF64748B)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = surfaceFallback),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(modifier = Modifier.background(contentSurface)) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50).copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = title,
                        fontFamily = BricolageGrotesque,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = textMain
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = message,
                        fontFamily = BricolageGrotesque,
                        fontSize = 15.sp,
                        color = textSub,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor, // Use specified accentColor
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = buttonText,
                            fontFamily = BricolageGrotesque,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DestructiveConfirmationDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = LocalAppIsDark.current

    // 1. UPDATED GRADIENT SURFACE
    val contentSurface = if (isDark) {
        Brush.linearGradient(
            0.0f to Color.Black,
            1.0f to Color(0xFF1B1B1B)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color(0xFFF8FAFC), Color(0xFFf2f0e3))
        )
    }

    val surfaceFallback = if (isDark) Color(0xFF1B1B1B) else Color.White
    val textMain = if (isDark) Color.White else Color(0xFF1B1B1B)
    val textSub = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF64748B)
    val dangerColor = Color(0xFFEF5350)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = surfaceFallback),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(modifier = Modifier.background(contentSurface)) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(dangerColor.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteForever,
                            contentDescription = null,
                            tint = dangerColor,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = title,
                        fontFamily = BricolageGrotesque,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = textMain,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = message,
                        fontFamily = BricolageGrotesque,
                        fontSize = 15.sp,
                        color = textSub,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(50),
                            border = androidx.compose.foundation.BorderStroke(1.dp, textSub.copy(alpha = 0.3f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = textMain)
                        ) {
                            Text(
                                text = "Cancel",
                                fontFamily = BricolageGrotesque,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = onConfirm,
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = dangerColor,
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = "Delete",
                                fontFamily = BricolageGrotesque,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WhatsNewDialog(onDismiss: () -> Unit) {
    val isDark = LocalAppIsDark.current
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSub = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF64748B)

    ContentDialog(title = "What's New", onDismiss = onDismiss) {
        Column(
            modifier = Modifier.padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Version 1.1.9 (Bug fixes)",
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                fontSize = 18.sp
            )

            // --- Updated Widget ---
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "📱 New Home Widget",
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary,
                    fontSize = 15.sp
                )
                Text(
                    text = "Our home screen widget got a complete makeover! It now matches the app’s look perfectly with connected cycle strips.",
                    fontFamily = BricolageGrotesque,
                    color = textSub,
                    fontSize = 14.sp
                )
            }

            // --- Calendar Alignment Fix ---
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "🗓️ Calendar Alignment",
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary,
                    fontSize = 15.sp
                )
                Text(
                    text = "We fixed a bug where the days of the week didn't quite line up with the dates. Everything is now perfectly aligned for a clearer view of your cycle.",
                    fontFamily = BricolageGrotesque,
                    color = textSub,
                    fontSize = 14.sp
                )
            }
        }
    }
}
package com.ben.periodt.ui.profiles.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ben.periodt.R
import com.ben.periodt.ui.theme.BricolageGrotesque

private val SIZE_LG = 15.sp

val AvatarResMap = mapOf(
    "avatar_1" to R.drawable.avatar_1, "avatar_2" to R.drawable.avatar_2,
    "avatar_3" to R.drawable.avatar_3, "avatar_4" to R.drawable.avatar_4,
    "avatar_5" to R.drawable.avatar_5, "avatar_6" to R.drawable.avatar_6,
    "avatar_7" to R.drawable.avatar_7, "avatar_8" to R.drawable.avatar_8,
    "avatar_9" to R.drawable.avatar_9, "avatar_10" to R.drawable.avatar_10,
    "avatar_11" to R.drawable.avatar_11, "avatar_12" to R.drawable.avatar_12,
    "avatar_13" to R.drawable.avatar_13, "avatar_14" to R.drawable.avatar_14,
    "avatar_15" to R.drawable.avatar_15, "avatar_16" to R.drawable.avatar_16,
    "avatar_17" to R.drawable.avatar_17, "avatar_18" to R.drawable.avatar_18,
    "avatar_19" to R.drawable.avatar_19, "avatar_20" to R.drawable.avatar_20,
    "avatar_21" to R.drawable.avatar_21, "avatar_22" to R.drawable.avatar_22,
    "avatar_23" to R.drawable.avatar_23, "avatar_24" to R.drawable.avatar_24,
)

@Composable
fun AvatarDisplay(
    avatarString: String,
    name: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = SIZE_LG
) {
    if (avatarString.startsWith("#")) {
        val pColor = remember(avatarString) {
            try { Color(android.graphics.Color.parseColor(avatarString)) }
            catch (e: Exception) { Color(0xFFD89046) }
        }
        Box(
            modifier = modifier.clip(CircleShape).background(pColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.firstOrNull()?.uppercase() ?: "M",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontFamily = BricolageGrotesque,
                fontSize = fontSize
            )
        }
    } else {
        val resId = AvatarResMap[avatarString]
        if (resId != null) {
            AsyncImage(
                model = resId,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = modifier.clip(CircleShape)
            )
        } else {
            Box(
                modifier = modifier.clip(CircleShape).background(Color(0xFFD89046)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.firstOrNull()?.uppercase() ?: "M",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontFamily = BricolageGrotesque,
                    fontSize = fontSize
                )
            }
        }
    }
}
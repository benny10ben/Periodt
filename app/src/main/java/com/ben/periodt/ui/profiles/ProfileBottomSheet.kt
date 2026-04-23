package com.ben.periodt.ui.profiles

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ben.periodt.ui.profiles.components.ProfileRow
import com.ben.periodt.ui.profiles.components.SwipeToDeleteCard
import com.ben.periodt.ui.theme.BricolageGrotesque
import com.ben.periodt.viewmodel.PeriodViewModel

private val SIZE_SM = 13.sp
private val SIZE_LG = 15.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileBottomSheetContent(
    allProfiles: List<PeriodViewModel.Profile>,
    activeProfile: PeriodViewModel.Profile?,
    isDark: Boolean,
    onSwitch: (Int) -> Unit,
    onEdit: (PeriodViewModel.Profile) -> Unit,
    onDelete: (Int) -> Unit,
    onAddClick: () -> Unit,
    onSettingsClick: () -> Unit,
    listState: LazyListState = rememberLazyListState()
) {
    val textPrimary = if (isDark) Color.White else Color(0xFF1B1B1B)
    val textSub = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF1b1b1b).copy(alpha = 0.6f)
    val accentColor = if (isDark) Color(0xFFD89046) else Color(0xFFa5bda3)
    val rowBg = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 16.dp)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness    = Spring.StiffnessMediumLow
                )
            )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("Profiles", fontFamily = BricolageGrotesque, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = textPrimary)
                Box(Modifier.size(36.dp).clip(CircleShape).background(textSub.copy(alpha = 0.1f)).clickable(onClick = onSettingsClick), Alignment.Center) {
                    Icon(Icons.Rounded.Settings, null, tint = textPrimary, modifier = Modifier.size(20.dp))
                }
            }
            Text("Long press to edit • Swipe to delete", fontFamily = BricolageGrotesque, fontSize = SIZE_SM, color = textSub)
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 4.dp)
        ) {
            items(items = allProfiles, key = { it.id }) { profile ->
                val isSelected = profile.id == activeProfile?.id
                val highlightColor = if (profile.avatarColor.startsWith("#")) {
                    try { Color(android.graphics.Color.parseColor(profile.avatarColor)) } catch (e: Exception) { accentColor }
                } else accentColor
                val canDelete = allProfiles.size > 1 && !isSelected
                val itemModifier = Modifier.animateItem()

                if (canDelete) {
                    SwipeToDeleteCard(modifier = itemModifier, onDelete = { onDelete(profile.id) }) { isSwiping ->
                        ProfileRow(
                            modifier = Modifier,
                            profile = profile,
                            isSelected = isSelected,
                            highlightColor = highlightColor,
                            rowBg = if (isSelected) highlightColor.copy(alpha = 0.15f) else rowBg,
                            isDark = isDark,
                            textPrimary = textPrimary,
                            onSwitch = { if (!isSwiping) onSwitch(profile.id) },
                            onEdit = { if (!isSwiping) onEdit(profile) }
                        )
                    }
                } else {
                    ProfileRow(
                        modifier = itemModifier,
                        profile = profile,
                        isSelected = isSelected,
                        highlightColor = highlightColor,
                        rowBg = if (isSelected) highlightColor.copy(alpha = 0.15f) else rowBg,
                        isDark = isDark,
                        textPrimary = textPrimary,
                        onSwitch = onSwitch,
                        onEdit = onEdit
                    )
                }
            }
        }

        Box(modifier = Modifier.padding(horizontal = 24.dp).padding(top = 16.dp)) {
            Button(
                onClick = onAddClick,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                Icon(Icons.Rounded.Add, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add Profile", fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = SIZE_LG)
            }
        }
    }
}
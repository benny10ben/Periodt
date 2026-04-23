package com.ben.periodt.ui.profiles.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ben.periodt.ui.theme.BricolageGrotesque
import com.ben.periodt.ui.theme.LocalAppIsDark
import com.ben.periodt.viewmodel.PeriodViewModel
import kotlinx.coroutines.launch

private val SIZE_SM = 13.sp
private val SIZE_MD = 14.sp
private val SIZE_LG = 15.sp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProfileEditorDialog(
    existingProfile: PeriodViewModel.Profile?,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    val context = LocalContext.current
    var name by remember(existingProfile) { mutableStateOf(existingProfile?.name ?: "") }
    var selectedAvatar by remember(existingProfile) {
        mutableStateOf(existingProfile?.avatarColor ?: "avatar_1")
    }

    val avatars = remember { (1..24).map { "avatar_$it" } }
    val avatarPages = remember { avatars.chunked(8) }
    val avatarSetHeadings = remember { listOf("Girls", "Capybara", "Fruits", "Contribute") }

    val initialPage = remember(existingProfile, avatars) {
        val currentAvatar = existingProfile?.avatarColor ?: "avatar_1"
        val index = avatars.indexOf(currentAvatar)
        if (index != -1) index / 8 else 0
    }

    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { avatarSetHeadings.size })

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val thresholdPx = remember(configuration.screenHeightDp) { with(density) { (configuration.screenHeightDp.dp * 0.20f).toPx() } }
    var expandedOffset by remember { mutableFloatStateOf(0f) }

    class SheetStateHolder { var state: SheetState? = null }
    val sheetHolder = remember { SheetStateHolder() }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { targetValue ->
            if (targetValue == SheetValue.Hidden) {
                try {
                    val currentState = sheetHolder.state
                    if (currentState != null) {
                        val currentOffset = currentState.requireOffset()
                        val dragDistance = currentOffset - expandedOffset
                        dragDistance <= 10f || dragDistance >= thresholdPx
                    } else true
                } catch (e: Exception) { true }
            } else true
        }
    )
    sheetHolder.state = sheetState

    LaunchedEffect(sheetState.currentValue) {
        if (sheetState.currentValue == SheetValue.Expanded) {
            try { expandedOffset = sheetState.requireOffset() } catch (e: Exception) {}
        }
    }

    val containerColor = if (isDark) Color(0xFF1B1B1B) else Color.White
    val textPrimary = if (isDark) Color.White else Color(0xFF1B1B1B)
    val textSub = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF1b1b1b).copy(alpha = 0.6f)
    val accentColor = if (isDark) Color(0xFFD89046) else Color(0xFFa5bda3)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = containerColor,
        contentColor = textPrimary,
        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (existingProfile == null) "New Profile" else "Edit Profile",
                    fontFamily = BricolageGrotesque,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = textPrimary
                )
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(textSub.copy(alpha = 0.1f))
                        .clickable { coroutineScope.launch { sheetState.hide(); onDismiss() } },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, "Close", tint = textPrimary, modifier = Modifier.size(18.dp))
                }
            }

            Column(
                modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                ProfileNameField(name, { name = it }, isDark, accentColor, textPrimary, textSub)

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Choose Avatar", fontFamily = BricolageGrotesque, fontWeight = FontWeight.SemiBold, fontSize = SIZE_MD, color = textPrimary)
                        Text("•", color = textSub.copy(alpha = 0.4f))
                        AnimatedContent(
                            targetState = avatarSetHeadings.getOrElse(pagerState.currentPage) { "" },
                            transitionSpec = { (fadeIn() + slideInVertically { it / 2 }).togetherWith(fadeOut() + slideOutVertically { -it / 2 }) },
                            label = ""
                        ) { heading ->
                            Text(heading, fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = SIZE_SM, color = accentColor)
                        }
                    }

                    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth(), pageSpacing = 16.dp) { page ->
                        if (page < avatarPages.size) {
                            val rows = avatarPages[page].chunked(4)
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                rows.forEach { rowItems ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        rowItems.forEach { avatarName ->
                                            val isSelected = selectedAvatar == avatarName
                                            Box(
                                                modifier = Modifier.weight(1f).aspectRatio(1f).clip(CircleShape)
                                                    .background(if (isSelected) accentColor.copy(alpha = 0.2f) else Color.Transparent)
                                                    .then(if (isSelected) Modifier.border(2.5.dp, accentColor, CircleShape) else Modifier)
                                                    .clickable { selectedAvatar = avatarName }.padding(4.dp)
                                            ) {
                                                AvatarDisplay(avatarString = avatarName, name = "", modifier = Modifier.fillMaxSize())
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier.fillMaxWidth().aspectRatio(2.1f).clip(RoundedCornerShape(20.dp)).background(textSub.copy(alpha = 0.05f)).padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Would you like to contribute more diverse sets of avatars?",
                                        fontFamily = BricolageGrotesque,
                                        fontSize = SIZE_MD,
                                        color = textPrimary,
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        text = "contact me at",
                                        fontFamily = BricolageGrotesque,
                                        fontSize = SIZE_SM,
                                        color = textSub,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "developer.ben10@gmail.com",
                                        fontFamily = BricolageGrotesque,
                                        fontSize = SIZE_MD,
                                        color = accentColor,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable {
                                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                                data = Uri.parse("mailto:developer.ben10@gmail.com")
                                            }
                                            try { context.startActivity(intent) } catch (e: Exception) { }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Row(Modifier.fillMaxWidth(), Arrangement.Center, Alignment.CenterVertically) {
                        repeat(avatarSetHeadings.size) { iteration ->
                            val color = if (pagerState.currentPage == iteration) accentColor else textSub.copy(alpha = 0.2f)
                            Box(Modifier.padding(horizontal = 4.dp).size(if (pagerState.currentPage == iteration) 8.dp else 6.dp).clip(CircleShape).background(color))
                        }
                    }
                }
            }

            Button(
                onClick = { if (name.isNotBlank()) { coroutineScope.launch { sheetState.hide(); onSave(name, selectedAvatar) } } },
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp).height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                Text("Save Profile", fontFamily = BricolageGrotesque, fontSize = SIZE_LG, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegacyImportDialog(
    profiles: List<PeriodViewModel.Profile>,
    onImportToProfile: (Int?) -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = LocalAppIsDark.current
    val containerColor = if (isDark) Color(0xFF1B1B1B) else Color.White
    val textPrimary = if (isDark) Color.White else Color(0xFF1B1B1B)
    val textSub = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF1b1b1b).copy(alpha = 0.6f)
    val accentColor = if (isDark) Color(0xFFD89046) else Color(0xFFa5bda3)
    val rowBg = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f)

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val thresholdPx = remember(configuration.screenHeightDp) { with(density) { (configuration.screenHeightDp.dp * 0.20f).toPx() } }
    var expandedOffset by remember { mutableFloatStateOf(0f) }

    class SheetStateHolder { var state: SheetState? = null }
    val sheetHolder = remember { SheetStateHolder() }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { targetValue ->
            if (targetValue == SheetValue.Hidden) {
                try {
                    val currentState = sheetHolder.state
                    if (currentState != null) {
                        val currentOffset = currentState.requireOffset()
                        val dragDistance = currentOffset - expandedOffset
                        dragDistance <= 10f || dragDistance >= thresholdPx
                    } else true
                } catch (e: Exception) { true }
            } else true
        }
    )
    sheetHolder.state = sheetState

    LaunchedEffect(sheetState.currentValue) {
        if (sheetState.currentValue == SheetValue.Expanded) {
            try { expandedOffset = sheetState.requireOffset() } catch (e: Exception) {}
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = containerColor,
        contentColor = textPrimary,
        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp)
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness    = Spring.StiffnessMediumLow
                    )
                )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Legacy Backup", fontFamily = BricolageGrotesque, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = textPrimary)
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(textSub.copy(alpha = 0.1f))
                        .clickable { coroutineScope.launch { sheetState.hide(); onDismiss() } },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, null, tint = textPrimary, modifier = Modifier.size(18.dp))
                }
            }

            Column(
                modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text("We found an older backup format... Which profile should we merge this data into?", fontFamily = BricolageGrotesque, fontSize = SIZE_MD, color = textSub, lineHeight = 22.sp)

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    profiles.forEach { p ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(rowBg)
                                .clickable { coroutineScope.launch { sheetState.hide(); onImportToProfile(p.id) } }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AvatarDisplay(p.avatarColor, p.name, Modifier.size(40.dp), SIZE_MD)
                            Spacer(Modifier.width(16.dp))
                            Text("Merge into ${p.name}", color = textPrimary, fontFamily = BricolageGrotesque, fontWeight = FontWeight.SemiBold, fontSize = SIZE_LG)
                        }
                    }
                }
            }

            Button(
                onClick = { coroutineScope.launch { sheetState.hide(); onImportToProfile(null) } },
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp).height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                Icon(Icons.Rounded.Add, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Create New Profile", fontFamily = BricolageGrotesque, fontSize = SIZE_LG, fontWeight = FontWeight.Bold)
            }
        }
    }
}
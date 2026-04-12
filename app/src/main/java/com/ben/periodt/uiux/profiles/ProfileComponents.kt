package com.ben.periodt.uiux.profiles

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ben.periodt.ui.theme.BricolageGrotesque
import com.ben.periodt.viewmodel.PeriodViewModel
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

private val SIZE_SM = 13.sp
private val SIZE_MD = 14.sp
private val SIZE_LG = 15.sp

private val AvatarResMap = mapOf(
    "avatar_1" to com.ben.periodt.R.drawable.avatar_1,
    "avatar_2" to com.ben.periodt.R.drawable.avatar_2,
    "avatar_3" to com.ben.periodt.R.drawable.avatar_3,
    "avatar_4" to com.ben.periodt.R.drawable.avatar_4,
    "avatar_5" to com.ben.periodt.R.drawable.avatar_5,
    "avatar_6" to com.ben.periodt.R.drawable.avatar_6,
    "avatar_7" to com.ben.periodt.R.drawable.avatar_7,
    "avatar_8" to com.ben.periodt.R.drawable.avatar_8,
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
        val resId = AvatarResMap[avatarString] ?: 0
        if (resId != 0) {
            Image(
                painter = painterResource(id = resId),
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

@Composable
fun SwipeToDeleteCard(
    modifier: Modifier = Modifier,
    onDelete: () -> Unit,
    content: @Composable (Boolean) -> Unit
) {
    val density  = LocalDensity.current
    val scope    = rememberCoroutineScope()
    val offsetX  = remember { Animatable(0f) }
    val revealPx = with(density) { 80.dp.toPx() }
    val deletePx = with(density) { 150.dp.toPx() }  // lower = snappier feel
    var widthPx  by remember { mutableStateOf(0f) }

    val itemShape = RoundedCornerShape(20.dp)

    // Three distinct specs for three distinct feelings
    val revealSpring   = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
    val snapBackSpring = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy,     stiffness = Spring.StiffnessMediumLow)
    val deleteExit     = tween<Float>(durationMillis = 180, easing = FastOutLinearInEasing)

    val isRevealed by remember { derivedStateOf { offsetX.value.absoluteValue > revealPx * 0.55f } }
    val isSwiping  by remember { derivedStateOf { offsetX.value.absoluteValue > 4f } }

    // Smooth proportional alpha — no more hard jump
    val bgAlpha by remember { derivedStateOf {
        (offsetX.value.absoluteValue / revealPx).coerceIn(0f, 0.85f)
    }}

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { widthPx = it.width.toFloat() }
    ) {
        // Background layer
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(itemShape)
                .background(Color.Red.copy(alpha = bgAlpha))
                .padding(horizontal = 24.dp),
            contentAlignment = if (offsetX.value >= 0f) Alignment.CenterStart else Alignment.CenterEnd
        ) {
            AnimatedVisibility(
                visible = isRevealed,
                enter   = fadeIn(tween(120)) + scaleIn(tween(120, easing = FastOutSlowInEasing)),
                exit    = fadeOut(tween(80))  + scaleOut(tween(80))
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            scope.launch {
                                val target = if (offsetX.value >= 0f) widthPx else -widthPx
                                offsetX.animateTo(target, deleteExit)
                                onDelete()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Draggable card layer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(itemShape)
                .offset { IntOffset(offsetX.value.toInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                val off = offsetX.value
                                when {
                                    off <= -deletePx || off >= deletePx -> {
                                        // Fast, no-bounce exit → then delete
                                        val target = if (off < 0f) -widthPx else widthPx
                                        offsetX.animateTo(target, deleteExit)
                                        onDelete()
                                    }
                                    off <= -(revealPx * 0.5f) -> offsetX.animateTo(-revealPx, revealSpring)
                                    off >=  (revealPx * 0.5f) -> offsetX.animateTo( revealPx, revealSpring)
                                    else                       -> offsetX.animateTo(0f, snapBackSpring)
                                }
                            }
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            offsetX.snapTo((offsetX.value + dragAmount).coerceIn(-widthPx, widthPx))
                        }
                    }
                }
        ) {
            content(isSwiping)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
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
    val textSub     = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF1b1b1b).copy(alpha = 0.6f)
    val accentColor = if (isDark) Color(0xFFD89046) else Color(0xFF6d9567).copy(alpha = 0.6f)
    val rowBg          = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(vertical = 8.dp)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness    = Spring.StiffnessMediumLow
                )
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Header (Padding added back locally) ---
        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Profiles", fontFamily = BricolageGrotesque, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = textPrimary)
                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(textSub.copy(alpha = 0.1f)).clickable(onClick = onSettingsClick), contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Rounded.Settings, contentDescription = "Settings", tint = textPrimary, modifier = Modifier.size(20.dp))
                }
            }
            Text(text = "Long press to edit • Swipe to delete", fontFamily = BricolageGrotesque, fontSize = SIZE_SM, color = textSub)
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
                        ProfileRow(profile = profile, isSelected = isSelected, highlightColor = highlightColor, rowBg = if (isSelected) highlightColor.copy(alpha = 0.15f) else rowBg, isDark = isDark, textPrimary = textPrimary, onSwitch = { if (!isSwiping) onSwitch(it) }, onEdit = { if (!isSwiping) onEdit(it) })
                    }
                } else {
                    ProfileRow(modifier = itemModifier, profile = profile, isSelected = isSelected, highlightColor = highlightColor, rowBg = if (isSelected) highlightColor.copy(alpha = 0.15f) else rowBg, isDark = isDark, textPrimary = textPrimary, onSwitch = onSwitch, onEdit = onEdit)
                }
            }
        }

        // --- Button (Padding added back locally) ---
        Box(modifier = Modifier.padding(horizontal = 24.dp)) {
            Button(
                onClick = onAddClick,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Icon(Icons.Rounded.Add, null, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp))
                Text(text = "Add Profile", fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = SIZE_LG)
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ProfileRow(
    modifier: Modifier = Modifier,
    profile: PeriodViewModel.Profile,
    isSelected: Boolean,
    highlightColor: Color,
    rowBg: Color,
    isDark: Boolean,
    textPrimary: Color,
    onSwitch: (Int) -> Unit,
    onEdit: (PeriodViewModel.Profile) -> Unit
) {
    val sheetBg = if (isDark) Color(0xFF1B1B1B) else Color.White
    val targetBg = if (isSelected) androidx.compose.ui.graphics.lerp(sheetBg, highlightColor, 0.15f) else rowBg
    val animatedBg by animateColorAsState(targetValue = targetBg, animationSpec = tween(300), label = "rowBgAnim")

    Row(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(animatedBg)
            .combinedClickable(onClick = { onSwitch(profile.id) }, onLongClick = { onEdit(profile) })
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarDisplay(avatarString = profile.avatarColor, name = profile.name, modifier = Modifier.size(48.dp), fontSize = SIZE_LG)
        Spacer(Modifier.width(16.dp))
        Text(text = profile.name, modifier = Modifier.weight(1f), color = textPrimary, fontFamily = BricolageGrotesque, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, fontSize = SIZE_LG, letterSpacing = (-0.3).sp)
        AnimatedVisibility(visible = isSelected, enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) + fadeIn(), exit = scaleOut(tween(150)) + fadeOut()) {
            Icon(imageVector = Icons.Rounded.CheckCircle, contentDescription = null, tint = highlightColor, modifier = Modifier.size(22.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditorDialog(
    existingProfile: PeriodViewModel.Profile?,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name           by remember(existingProfile) { mutableStateOf(existingProfile?.name ?: "") }
    var selectedAvatar by remember(existingProfile) { mutableStateOf(existingProfile?.avatarColor ?: "avatar_1") }
    val avatars = remember { listOf("avatar_1", "avatar_2", "avatar_3", "avatar_4", "avatar_5", "avatar_6", "avatar_7", "avatar_8") }
    val avatarRows = remember(avatars) { avatars.chunked(4) }
    val containerColor = if (isDark) Color(0xFF1B1B1B) else Color.White
    val textPrimary    = if (isDark) Color.White else Color(0xFF1B1B1B)
    val textSub        = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF64748B)
    val accentColor    = if (isDark) Color(0xFFD89046) else Color(0xFF6d9567).copy(alpha = 0.6f)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = containerColor, contentColor = textPrimary, modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).navigationBarsPadding().padding(bottom = 16.dp).verticalScroll(rememberScrollState())                .animateContentSize(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness    = Spring.StiffnessMediumLow
            )
        ), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = if (existingProfile == null) "New Profile" else "Edit Profile", fontFamily = BricolageGrotesque, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = textPrimary)
                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(textSub.copy(alpha = 0.1f)).clickable(onClick = onDismiss), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Close, "Close", tint = textPrimary, modifier = Modifier.size(18.dp))
                }
            }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Profile Name", fontFamily = BricolageGrotesque, fontSize = SIZE_SM) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(
                    fontFamily = BricolageGrotesque,
                    fontSize = SIZE_LG,
                    color = textPrimary
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor     = textPrimary,
                    unfocusedTextColor   = textPrimary,
                    focusedLabelColor    = accentColor,
                    unfocusedLabelColor  = textSub,
                    focusedBorderColor   = accentColor,
                    unfocusedBorderColor = textSub.copy(alpha = 0.5f),
                    cursorColor          = accentColor,
                    selectionColors = androidx.compose.foundation.text.selection.TextSelectionColors(
                        handleColor = accentColor,
                        backgroundColor = accentColor.copy(alpha = 0.3f)
                    )
                )
            )
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(text = "Choose Avatar", fontFamily = BricolageGrotesque, fontWeight = FontWeight.SemiBold, fontSize = SIZE_MD, color = textPrimary)
                avatarRows.forEach { rowItems ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        rowItems.forEach { avatarName ->
                            val isSelected = selectedAvatar == avatarName
                            val resId = AvatarResMap[avatarName] ?: 0
                            Box(modifier = Modifier.weight(1f).aspectRatio(1f).clip(CircleShape).background(if (isSelected) accentColor.copy(alpha = 0.2f) else Color.Transparent).clickable { selectedAvatar = avatarName }.padding(4.dp), contentAlignment = Alignment.Center) {
                                if (resId != 0) Image(painter = painterResource(id = resId), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape).then(if (isSelected) Modifier.border(2.5.dp, accentColor, CircleShape) else Modifier))
                            }
                        }
                    }
                }
            }
            Button(onClick = { if (name.isNotBlank()) onSave(name, selectedAvatar) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(56.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White), elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)) {
                Text(text = "Save Profile", fontFamily = BricolageGrotesque, fontSize = SIZE_LG, fontWeight = FontWeight.Bold)
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
    val isDark         = com.ben.periodt.ui.theme.LocalAppIsDark.current
    val containerColor = if (isDark) Color(0xFF1B1B1B) else Color.White
    val textPrimary    = if (isDark) Color.White else Color(0xFF1B1B1B)
    val textSub        = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF64748B)
    val accentColor    = if (isDark) Color(0xFFD89046) else Color(0xFF6d9567).copy(alpha = 0.6f)
    val rowBg          = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f)

    // ✨ FIXED: Integrated Scroll guard into the SheetState logic
    val scrollState = rememberScrollState()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { newValue ->
            // Prevent closing via swipe-down if the user hasn't scrolled all the way up
            if (newValue == SheetValue.Hidden) scrollState.value == 0 else true
        }
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = containerColor,
        contentColor     = textPrimary,
        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
                .verticalScroll(scrollState)
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness    = Spring.StiffnessMediumLow
                    )
                ),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Legacy Backup", fontFamily = BricolageGrotesque, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = textPrimary)
                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(textSub.copy(alpha = 0.1f)).clickable(onClick = onDismiss), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Close, "Close", tint = textPrimary, modifier = Modifier.size(18.dp))
                }
            }

            Text(text = "We found an older backup format that doesn't contain profile information. Which profile should we merge this data into?", fontFamily = BricolageGrotesque, fontSize = SIZE_MD, color = textSub, lineHeight = 22.sp, modifier = Modifier.padding(horizontal = 24.dp))

            Column(modifier = Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                profiles.forEach { p ->
                    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(rowBg).clickable { onImportToProfile(p.id) }.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        AvatarDisplay(avatarString = p.avatarColor, name = p.name, modifier = Modifier.size(40.dp), fontSize = SIZE_MD)
                        Spacer(Modifier.width(16.dp))
                        Text(text = "Merge into ${p.name}", color = textPrimary, fontFamily = BricolageGrotesque, fontWeight = FontWeight.SemiBold, fontSize = SIZE_LG)
                    }
                }
            }

            Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                Button(onClick = { onImportToProfile(null) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(56.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White), elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp))
                    Text(text = "Create New Profile", fontFamily = BricolageGrotesque, fontSize = SIZE_LG, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
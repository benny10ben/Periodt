package com.ben.periodt.ui.profiles

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import coil.compose.AsyncImage
import com.ben.periodt.R
import com.ben.periodt.ui.theme.BricolageGrotesque
import com.ben.periodt.ui.theme.LocalAppIsDark
import com.ben.periodt.viewmodel.PeriodViewModel
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

private val SIZE_SM = 13.sp
private val SIZE_MD = 14.sp
private val SIZE_LG = 15.sp

private val AvatarResMap = mapOf(
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

@Composable
fun ProfileNameField(
    value: String,
    onValueChange: (String) -> Unit,
    isDark: Boolean,
    accentColor: Color,
    textPrimary: Color,
    textSub: Color
) {
    val isFocused = remember { mutableStateOf(false) }
    val fieldBg = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)

    val borderColor by animateColorAsState(
        targetValue = if (isFocused.value) accentColor.copy(alpha = 0.8f) else Color.Transparent,
        animationSpec = tween(200),
        label = "fieldBorder"
    )

    val labelFloat by animateFloatAsState(
        targetValue = if (isFocused.value || value.isNotEmpty()) 1f else 0f,
        animationSpec = tween(180),
        label = "labelFloat"
    )

    val labelSize = lerp(SIZE_LG.value, SIZE_SM.value, labelFloat)
    val labelColor by animateColorAsState(
        targetValue = if (isFocused.value) accentColor else textSub,
        animationSpec = tween(180),
        label = "labelColor"
    )

    CompositionLocalProvider(
        LocalTextSelectionColors provides TextSelectionColors(
            handleColor = accentColor,
            backgroundColor = accentColor.copy(alpha = 0.25f)
        )
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                fontFamily = BricolageGrotesque,
                fontSize = SIZE_LG,
                color = textPrimary
            ),
            cursorBrush = SolidColor(accentColor),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isFocused.value = it.isFocused },
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(fieldBg)
                        .border(1.5.dp, borderColor, RoundedCornerShape(18.dp))
                        .animateContentSize(animationSpec = tween(180))
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = "Profile Name",
                        fontFamily = BricolageGrotesque,
                        fontSize = labelSize.sp,
                        color = labelColor,
                        modifier = Modifier.graphicsLayer {
                            translationY = lerp(0f, -12f, labelFloat)
                        }
                    )
                    Box(modifier = Modifier.padding(top = if (labelFloat > 0.5f) 24.dp else 0.dp)) {
                        innerTextField()
                    }
                }
            }
        )
    }
}


@Composable
fun SwipeToDeleteCard(
    modifier: Modifier = Modifier,
    onDelete: () -> Unit,
    content: @Composable (Boolean) -> Unit
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val revealPx = with(density) { 80.dp.toPx() }
    val deletePx = with(density) { 150.dp.toPx() }
    var widthPx by remember { mutableStateOf(0f) }

    val itemShape = RoundedCornerShape(20.dp)

    val revealSpring = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
    val snapBackSpring = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
    val deleteExit = tween<Float>(durationMillis = 180, easing = FastOutLinearInEasing)

    val isRevealed by remember { derivedStateOf { offsetX.value.absoluteValue > revealPx * 0.55f } }
    val isSwiping by remember { derivedStateOf { offsetX.value.absoluteValue > 4f } }

    val bgAlpha by remember { derivedStateOf { (offsetX.value.absoluteValue / revealPx).coerceIn(0f, 0.85f) } }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { widthPx = it.width.toFloat() }
    ) {
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
                enter = fadeIn(tween(120)) + scaleIn(tween(120, easing = FastOutSlowInEasing)),
                exit = fadeOut(tween(80)) + scaleOut(tween(80))
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            scope.launch {
                                val target = if (offsetX.value >= 0f) widthPx else -widthPx
                                offsetX.animateTo(target, deleteExit)
                                onDelete()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }
        }

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
                                        val target = if (off < 0f) -widthPx else widthPx
                                        offsetX.animateTo(target, deleteExit)
                                        onDelete()
                                    }
                                    off <= -(revealPx * 0.5f) -> offsetX.animateTo(-revealPx, revealSpring)
                                    off >= (revealPx * 0.5f) -> offsetX.animateTo(revealPx, revealSpring)
                                    else -> offsetX.animateTo(0f, snapBackSpring)
                                }
                            }
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        scope.launch { offsetX.snapTo((offsetX.value + dragAmount).coerceIn(-widthPx, widthPx)) }
                    }
                }
        ) {
            content(isSwiping)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
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
    val targetBg = if (isSelected) lerp(sheetBg, highlightColor, 0.15f) else rowBg
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
    val accentColor = if (isDark) Color(0xFFD89046) else Color(0xFF6d9567).copy(alpha = 0.6f)

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
    val accentColor = if (isDark) Color(0xFFD89046) else Color(0xFF6d9567).copy(alpha = 0.6f)
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
    val accentColor = if (isDark) Color(0xFFD89046) else Color(0xFF6d9567).copy(alpha = 0.6f)
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
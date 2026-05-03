package com.ben.periodt.ui.settings.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ben.periodt.ui.theme.BricolageGrotesque
import com.ben.periodt.ui.theme.LocalAppIsDark
import kotlinx.coroutines.launch
import com.ben.periodt.viewmodel.AuthState
import com.ben.periodt.viewmodel.AuthViewModel

private val SIZE_MD = 14.sp
private val SIZE_LG = 15.sp

enum class ThemeMode { SYSTEM, LIGHT, DARK }
val THEME_MODE_KEY = stringPreferencesKey("theme_mode")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentDialog(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val isDark = LocalAppIsDark.current
    val containerColor = if (isDark) Color(0xFF1B1B1B) else Color.White
    val textPrimary    = if (isDark) Color.White else Color(0xFF1B1B1B)
    val textSub        = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF1b1b1b).copy(alpha = 0.6f)

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val thresholdPx = remember(configuration.screenHeightDp) {
        with(density) { (configuration.screenHeightDp.dp * 0.20f).toPx() }
    }

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
                    } else {
                        true
                    }
                } catch (e: Exception) {
                    true
                }
            } else {
                true
            }
        }
    )

    sheetHolder.state = sheetState

    LaunchedEffect(sheetState.currentValue) {
        if (sheetState.currentValue == SheetValue.Expanded) {
            try {
                expandedOffset = sheetState.requireOffset()
            } catch (e: Exception) {}
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = containerColor,
        sheetState = sheetState,
        scrimColor = Color.Black.copy(alpha = 0.32f),
        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
        dragHandle = { BottomSheetDefaults.DragHandle(color = textSub.copy(alpha = 0.2f)) }
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    fontFamily = BricolageGrotesque,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(textSub.copy(alpha = 0.1f))
                        .clickable {
                            coroutineScope.launch {
                                sheetState.hide()
                                onDismiss()
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Close, null, tint = textPrimary, modifier = Modifier.size(18.dp))
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                content()
            }
        }
    }
}

@Composable
fun FaqItem(question: String, answer: String, primary: Color, sub: Color) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(question, fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, color = primary, fontSize = SIZE_LG)
        Spacer(Modifier.height(4.dp))
        Text(answer, fontFamily = BricolageGrotesque, color = sub, fontSize = SIZE_MD, lineHeight = 20.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceDialog(current: ThemeMode, onSelect: (ThemeMode) -> Unit, onDismiss: () -> Unit) {
    val isDark         = LocalAppIsDark.current
    val containerColor = if (isDark) Color(0xFF1B1B1B) else Color.White
    val textPrimary    = if (isDark) Color.White else Color(0xFF1B1B1B)
    val textSub        = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF1b1b1b).copy(alpha = 0.6f)
    val accentColor    = if (isDark) Color(0xFFD89046) else Color(0xFFa5bda3)
    val rowBg          = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f)

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val thresholdPx = remember(configuration.screenHeightDp) {
        with(density) { (configuration.screenHeightDp.dp * 0.20f).toPx() }
    }

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
                    } else {
                        true
                    }
                } catch (e: Exception) {
                    true
                }
            } else {
                true
            }
        }
    )

    sheetHolder.state = sheetState

    LaunchedEffect(sheetState.currentValue) {
        if (sheetState.currentValue == SheetValue.Expanded) {
            try {
                expandedOffset = sheetState.requireOffset()
            } catch (e: Exception) {}
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = containerColor,
        sheetState = sheetState,
        scrimColor = Color.Black.copy(alpha = 0.32f),
        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
        dragHandle = { BottomSheetDefaults.DragHandle(color = textSub.copy(alpha = 0.2f)) }
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Appearance",
                    fontFamily = BricolageGrotesque,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(textSub.copy(alpha = 0.1f))
                        .clickable {
                            coroutineScope.launch {
                                sheetState.hide()
                                onDismiss()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, null, tint = textPrimary, modifier = Modifier.size(18.dp))
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ThemeOptionRow("System Default", current == ThemeMode.SYSTEM, textPrimary, rowBg, accentColor) {
                    coroutineScope.launch {
                        sheetState.hide()
                        onSelect(ThemeMode.SYSTEM)
                    }
                }
                ThemeOptionRow("Light Mode",     current == ThemeMode.LIGHT,   textPrimary, rowBg, accentColor) {
                    coroutineScope.launch {
                        sheetState.hide()
                        onSelect(ThemeMode.LIGHT)
                    }
                }
                ThemeOptionRow("Dark Mode",      current == ThemeMode.DARK,    textPrimary, rowBg, accentColor) {
                    coroutineScope.launch {
                        sheetState.hide()
                        onSelect(ThemeMode.DARK)
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeOptionRow(
    label: String,
    isSelected: Boolean,
    textPrimary: Color,
    rowBg: Color,
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (isSelected) accentColor.copy(alpha = 0.15f) else rowBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontFamily = BricolageGrotesque,
            color = if (isSelected) accentColor else textPrimary,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = SIZE_LG
        )
        Icon(
            imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isSelected) accentColor else textPrimary.copy(alpha = 0.2f),
            modifier = Modifier.size(22.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthBottomSheet(
    viewModel: AuthViewModel,
    initialIsLogin: Boolean,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    val authState by viewModel.authState.collectAsState()
    var isLoginMode by remember { mutableStateOf(initialIsLogin) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            onSuccess()
        }
    }

    val isDark = LocalAppIsDark.current
    val containerColor = if (isDark) Color(0xFF1B1B1B) else Color.White
    val textPrimary    = if (isDark) Color.White else Color(0xFF1B1B1B)
    val textSub        = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF1b1b1b).copy(alpha = 0.6f)
    val accentColor    = if (isDark) Color(0xFFD89046) else Color(0xFFa5bda3)

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = containerColor,
        sheetState = sheetState,
        scrimColor = Color.Black.copy(alpha = 0.32f),
        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
        dragHandle = { BottomSheetDefaults.DragHandle(color = textSub.copy(alpha = 0.2f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isLoginMode) "Welcome Back" else "Create Account",
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "End-to-End Encrypted Sync",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = BricolageGrotesque,
                color = textSub
            )

            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username", fontFamily = BricolageGrotesque) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = textPrimary,
                    unfocusedBorderColor = textSub.copy(alpha = 0.5f),
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary
                )
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password", fontFamily = BricolageGrotesque) },
                singleLine = true,
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    val image = if (isPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(imageVector = image, contentDescription = "Toggle password visibility", tint = textSub)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = textPrimary,
                    unfocusedBorderColor = textSub.copy(alpha = 0.5f),
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary
                )
            )

            Spacer(Modifier.height(24.dp))

            if (authState is AuthState.Loading) {
                CircularProgressIndicator(color = accentColor)
            } else {
                Button(
                    onClick = {
                        if (isLoginMode) viewModel.login(username, password)
                        else viewModel.register(username, password)
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = if (isLoginMode) "Sign In" else "Sign Up",
                        fontFamily = BricolageGrotesque,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            if (authState is AuthState.Error) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = (authState as AuthState.Error).message,
                    color = Color(0xFFEF5350),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = BricolageGrotesque)
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = if (isLoginMode) "Don't have an account? Sign up" else "Already have an account? Sign in",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = BricolageGrotesque,
                color = textPrimary,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { isLoginMode = !isLoginMode }
                    .padding(8.dp)
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDialog(
    currentUser: String?,
    onLoginClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onChangeUsernameClick: () -> Unit,
    onChangePasswordClick: () -> Unit,
    onDeleteAccountClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = LocalAppIsDark.current
    val containerColor = if (isDark) Color(0xFF1B1B1B) else Color.White
    val textPrimary    = if (isDark) Color.White else Color(0xFF1B1B1B)
    val textSub        = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF1b1b1b).copy(alpha = 0.6f)
    val accentColor    = if (isDark) Color(0xFFD89046) else Color(0xFFa5bda3)
    val dangerColor    = Color(0xFFEF5350)
    val rowBg          = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f)

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = containerColor,
        sheetState = sheetState,
        scrimColor = Color.Black.copy(alpha = 0.32f),
        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
        dragHandle = { BottomSheetDefaults.DragHandle(color = textSub.copy(alpha = 0.2f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (currentUser != null) {
                // --- LOGGED IN VIEW ---
                Box(
                    modifier = Modifier.size(64.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.AccountCircle, null, tint = accentColor, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = currentUser,
                    fontFamily = BricolageGrotesque,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                Text(
                    text = "End-to-End Encrypted Cloud",
                    fontFamily = BricolageGrotesque,
                    color = textSub,
                    fontSize = 14.sp
                )

                Spacer(Modifier.height(32.dp))

                // NEW: Feature Buttons!
                Column(
                    modifier = Modifier.clip(RoundedCornerShape(24.dp)).background(rowBg)
                ) {
                    SettingsItem(Icons.Rounded.Badge, "Change Username", null, textPrimary) {
                        coroutineScope.launch { sheetState.hide(); onChangeUsernameClick() }
                    }
                    SettingsItem(Icons.Rounded.Password, "Change Password", null, textPrimary) {
                        coroutineScope.launch { sheetState.hide(); onChangePasswordClick() }
                    }
                    SettingsItem(Icons.Rounded.DeleteForever, "Delete Account", null, dangerColor) {
                        coroutineScope.launch { sheetState.hide(); onDeleteAccountClick() }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        coroutineScope.launch {
                            sheetState.hide()
                            onLogoutClick()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = dangerColor.copy(alpha = 0.1f), contentColor = dangerColor),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Icon(Icons.Rounded.Logout, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Log Out", fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

            } else {
                // --- LOGGED OUT VIEW ---
                Box(
                    modifier = Modifier.size(64.dp).clip(CircleShape).background(textSub.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.CloudOff, null, tint = textSub, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Not Signed In",
                    fontFamily = BricolageGrotesque,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                Text(
                    text = "Your data is only stored on this device.",
                    fontFamily = BricolageGrotesque,
                    color = textSub,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(32.dp))

                Button(
                    onClick = {
                        coroutineScope.launch {
                            sheetState.hide()
                            onLoginClick()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White)
                ) {
                    Text("Sign In / Register", fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordSheet(
    viewModel: AuthViewModel,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    val changeState by viewModel.passwordChangeState.collectAsState()

    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var isOldVisible by remember { mutableStateOf(false) }
    var isNewVisible by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(changeState) {
        if (changeState is AuthState.Success) {
            viewModel.resetPasswordChangeState()
            onSuccess()
        }
    }

    val isDark = LocalAppIsDark.current
    val containerColor = if (isDark) Color(0xFF1B1B1B) else Color.White
    val textPrimary    = if (isDark) Color.White else Color(0xFF1B1B1B)
    val textSub        = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF1b1b1b).copy(alpha = 0.6f)
    val accentColor    = if (isDark) Color(0xFFD89046) else Color(0xFFa5bda3)

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.resetPasswordChangeState()
            onDismiss()
        },
        containerColor = containerColor,
        sheetState = sheetState,
        scrimColor = Color.Black.copy(alpha = 0.32f),
        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
        dragHandle = { BottomSheetDefaults.DragHandle(color = textSub.copy(alpha = 0.2f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Change Password",
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Your password protects your encryption keys. Please don't forget it!",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = BricolageGrotesque,
                color = textSub,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            // Old Password
            OutlinedTextField(
                value = oldPassword,
                onValueChange = { oldPassword = it; localError = null },
                label = { Text("Current Password", fontFamily = BricolageGrotesque) },
                singleLine = true,
                visualTransformation = if (isOldVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { isOldVisible = !isOldVisible }) {
                        Icon(if (isOldVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, null, tint = textSub)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = textPrimary, unfocusedBorderColor = textSub.copy(alpha = 0.5f))
            )

            Spacer(Modifier.height(16.dp))

            // New Password
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it; localError = null },
                label = { Text("New Password", fontFamily = BricolageGrotesque) },
                singleLine = true,
                visualTransformation = if (isNewVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { isNewVisible = !isNewVisible }) {
                        Icon(if (isNewVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, null, tint = textSub)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = textPrimary, unfocusedBorderColor = textSub.copy(alpha = 0.5f))
            )

            Spacer(Modifier.height(12.dp))

            // Confirm Password
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; localError = null },
                label = { Text("Confirm New Password", fontFamily = BricolageGrotesque) },
                singleLine = true,
                visualTransformation = if (isNewVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = textPrimary, unfocusedBorderColor = textSub.copy(alpha = 0.5f))
            )

            Spacer(Modifier.height(24.dp))

            if (changeState is AuthState.Loading) {
                CircularProgressIndicator(color = accentColor)
            } else {
                Button(
                    onClick = {
                        if (newPassword.length < 8) {
                            localError = "Password must be at least 8 characters."
                        } else if (newPassword != confirmPassword) {
                            localError = "New passwords do not match."
                        } else {
                            viewModel.changePassword(oldPassword, newPassword)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White)
                ) {
                    Text("Update Password", fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            val displayError = localError ?: (changeState as? AuthState.Error)?.message
            if (displayError != null) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = displayError,
                    color = Color(0xFFEF5350),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = BricolageGrotesque)
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeUsernameSheet(
    viewModel: AuthViewModel,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    val changeState by viewModel.usernameChangeState.collectAsState()
    var newUsername by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(changeState) {
        if (changeState is AuthState.Success) {
            viewModel.resetUsernameChangeState()
            onSuccess()
        }
    }

    val isDark = LocalAppIsDark.current
    val containerColor = if (isDark) Color(0xFF1B1B1B) else Color.White
    val textPrimary    = if (isDark) Color.White else Color(0xFF1B1B1B)
    val textSub        = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF1b1b1b).copy(alpha = 0.6f)
    val accentColor    = if (isDark) Color(0xFFD89046) else Color(0xFFa5bda3)

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.resetUsernameChangeState()
            onDismiss()
        },
        containerColor = containerColor,
        sheetState = sheetState,
        scrimColor = Color.Black.copy(alpha = 0.32f),
        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
        dragHandle = { BottomSheetDefaults.DragHandle(color = textSub.copy(alpha = 0.2f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Change Username",
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Choose a unique username to identify your encrypted cloud account.",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = BricolageGrotesque,
                color = textSub,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = newUsername,
                onValueChange = { newUsername = it; localError = null },
                label = { Text("New Username", fontFamily = BricolageGrotesque) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = textPrimary, unfocusedBorderColor = textSub.copy(alpha = 0.5f))
            )

            Spacer(Modifier.height(24.dp))

            if (changeState is AuthState.Loading) {
                CircularProgressIndicator(color = accentColor)
            } else {
                Button(
                    onClick = {
                        if (newUsername.isBlank()) {
                            localError = "Username cannot be empty."
                        } else {
                            viewModel.changeUsername(newUsername)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White)
                ) {
                    Text("Update Username", fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            val displayError = localError ?: (changeState as? AuthState.Error)?.message
            if (displayError != null) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = displayError,
                    color = Color(0xFFEF5350),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = BricolageGrotesque)
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
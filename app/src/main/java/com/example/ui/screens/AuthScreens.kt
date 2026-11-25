package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.AutoAwesomeMotion
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.SupabaseConnectionBadge
import com.example.ui.components.VibeAvatar
import com.example.ui.components.parseHexColor
import com.example.ui.theme.AvatarColors
import com.example.ui.theme.VibeCyan
import com.example.ui.theme.VibeEmerald
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.example.util.GoogleAuthManager
import com.example.util.GoogleSignInResult
import android.accounts.AccountManager
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PersonAdd

private data class GoogleAccountItem(
    val name: String,
    val email: String,
    val initial: String,
    val avatarColor: Color
)

private enum class AuthStep {
    WELCOME,
    SIGN_IN,
    CREATE_ACCOUNT,
    OTP_VERIFY,
    FORGOT_PASSWORD,
    RESET_PASSWORD_VERIFY
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AuthFlowScreen(
    isLoading: Boolean,
    onSignInPassword: (String, String) -> Unit,
    onSignUpEmail: (String, String, String, String, String, String, () -> Unit) -> Unit,
    onRequestOtp: (String, String, String, () -> Unit) -> Unit,
    onVerifyOtp: (String, String, String, String, String) -> Unit,
    onSignInGoogle: (String, String, String, String?) -> Unit = { _, _, _, _ -> },
    onRequestPasswordReset: (String, () -> Unit) -> Unit = { _, _ -> },
    onVerifyPasswordReset: (String, String, String) -> Unit = { _, _, _ -> },
    onShowMessage: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showGoogleChooserSheet by rememberSaveable { mutableStateOf(false) }

    val accountChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val selectedEmail = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            if (!selectedEmail.isNullOrBlank()) {
                val cleanEmail = selectedEmail.trim().lowercase()
                val derivedName = cleanEmail.substringBefore("@")
                    .replace(".", " ")
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                onSignInGoogle("", cleanEmail, derivedName, null)
            }
        }
    }

    val handleGoogleClick = {
        GoogleAuthManager.launchGoogleSignIn(
            context = context,
            scope = coroutineScope,
            onFallbackToChooser = {
                try {
                    accountChooserLauncher.launch(
                        GoogleAuthManager.createAccountChooserIntent(context)
                    )
                } catch (_: Exception) {
                    showGoogleChooserSheet = true
                }
            }
        ) { result ->
            when (result) {
                is GoogleSignInResult.Success -> {
                    onSignInGoogle(
                        result.idToken,
                        result.email,
                        result.displayName,
                        result.avatarUrl
                    )
                }
                is GoogleSignInResult.Error -> {
                    onShowMessage(result.message)
                }
                GoogleSignInResult.Cancelled -> {
                    // User dismissed the Google sign-in prompt
                }
            }
        }
    }

    var step by rememberSaveable { mutableStateOf(AuthStep.WELCOME) }
    var pendingOrigin by rememberSaveable { mutableStateOf(AuthStep.SIGN_IN) }

    var fullName by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var phoneNumber by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var bio by rememberSaveable { mutableStateOf("Hey there! I'm vibing on VibeChat ✨") }
    var selectedAvatarColor by rememberSaveable { mutableStateOf(AvatarColors.first()) }
    var otpCode by rememberSaveable { mutableStateOf("") }

    var resetPassword by rememberSaveable { mutableStateOf("") }
    var resetConfirmPassword by rememberSaveable { mutableStateOf("") }
    var showResetPassword by rememberSaveable { mutableStateOf(false) }
    var showResetConfirmPassword by rememberSaveable { mutableStateOf(false) }

    var showSignInPassword by rememberSaveable { mutableStateOf(false) }
    var showRegisterPassword by rememberSaveable { mutableStateOf(false) }
    var showConfirmPassword by rememberSaveable { mutableStateOf(false) }

    val emailFocusRequester = remember { FocusRequester() }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        focusedBorderColor = VibeEmerald,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
        focusedLabelColor = VibeEmerald,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedLeadingIconColor = VibeEmerald,
        unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        cursorColor = VibeEmerald
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        Color(0xFF070D19),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedContent(
            targetState = step,
            label = "auth_original_ui_transition"
        ) { currentStep ->
            when (currentStep) {
                AuthStep.WELCOME -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 520.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SupabaseConnectionBadge(onClick = {})

                            Spacer(modifier = Modifier.height(18.dp))

                            // Original Hero Card with Generated Artwork & App Icon
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(230.dp)
                                    .border(
                                        width = 1.dp,
                                        brush = Brush.linearGradient(
                                            listOf(
                                                VibeEmerald.copy(alpha = 0.55f),
                                                VibeCyan.copy(alpha = 0.35f)
                                            )
                                        ),
                                        shape = RoundedCornerShape(28.dp)
                                    ),
                                shape = RoundedCornerShape(28.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Image(
                                        painter = painterResource(id = R.drawable.img_welcome_hero),
                                        contentDescription = "VibeChat Welcome Banner",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color.Transparent,
                                                        Color(0xCC0B1120)
                                                    )
                                                )
                                            )
                                    )
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(18.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.img_app_logo),
                                            contentDescription = "VibeChat Logo",
                                            modifier = Modifier
                                                .size(60.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .border(
                                                    1.5.dp,
                                                    VibeEmerald.copy(alpha = 0.8f),
                                                    RoundedCornerShape(16.dp)
                                                ),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column {
                                            Text(
                                                text = "VibeChat",
                                                style = MaterialTheme.typography.headlineMedium,
                                                color = Color.White,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                            Text(
                                                text = "Chat • Connect • Vibe",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFFD1FAE5),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = "Connect with your circle in real time",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Rich direct & group chats, interactive voice notes, and 24-hour status stories with 6-digit session verification.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                WelcomeFeaturePill(icon = Icons.Default.Mic, label = "Voice Notes")
                                WelcomeFeaturePill(icon = Icons.Default.AutoAwesomeMotion, label = "24h Stories")
                                WelcomeFeaturePill(icon = Icons.Default.Groups, label = "Group Squads")
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { step = AuthStep.CREATE_ACCOUNT },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .testTag("welcome_get_started_button"),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = VibeEmerald,
                                    contentColor = Color(0xFF042F2E)
                                )
                            ) {
                                Text(
                                    text = "Get Started — Create Account",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedButton(
                                onClick = { step = AuthStep.SIGN_IN },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .testTag("welcome_sign_in_button"),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, VibeEmerald.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "I Already Have an Account — Sign In",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            GoogleSignInButton(
                                onClick = handleGoogleClick,
                                enabled = !isLoading,
                                text = "Continue with Google"
                            )
                        }
                    }
                }

                AuthStep.SIGN_IN, AuthStep.CREATE_ACCOUNT -> {
                    val isSignIn = currentStep == AuthStep.SIGN_IN
                    BackHandler {
                        step = if (isSignIn) AuthStep.WELCOME else AuthStep.SIGN_IN
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 520.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 22.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Original Top Bar with Back Button, Brand Header & Cloud Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = {
                                    step = if (isSignIn) AuthStep.WELCOME else AuthStep.SIGN_IN
                                },
                                modifier = Modifier.testTag("register_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }

                            SupabaseConnectionBadge(onClick = {})
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Brand Header with App Logo
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_app_logo),
                                contentDescription = "VibeChat Logo",
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(1.dp, VibeEmerald.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isSignIn) "Welcome Back" else "Join VibeChat",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Chat • Connect • Vibe",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Original Segmented Mode Switcher (Sign In | Create Account)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp)
                            ) {
                                val tabs = listOf(
                                    AuthStep.SIGN_IN to "Sign In",
                                    AuthStep.CREATE_ACCOUNT to "Create Account"
                                )
                                tabs.forEach { (tabStep, label) ->
                                    val selected = currentStep == tabStep
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (selected) VibeEmerald else Color.Transparent
                                            )
                                            .clickable { step = tabStep }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (selected) Color(0xFF042F2E) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        if (isSignIn) {
                            // SIGN IN CARD (Original UI Structure + Present Architecture)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(24.dp)
                                    ),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 24.dp)
                                ) {
                                    Text(
                                        text = "Sign In",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = "Enter your credentials. A 6-digit verification code will be sent to start your session.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 18.sp
                                    )

                                    Spacer(modifier = Modifier.height(18.dp))

                                    OutlinedTextField(
                                        value = email,
                                        onValueChange = { email = it },
                                        label = { Text("Email Address or Username") },
                                        placeholder = { Text("you@vibechat.io or @username") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = "Email Address or Username"
                                            )
                                        },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = textFieldColors,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .focusRequester(emailFocusRequester)
                                            .testTag("login_email_input")
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = "Change Email / Username?",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = VibeEmerald,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier
                                                .align(Alignment.CenterEnd)
                                                .clickable {
                                                    email = ""
                                                    try {
                                                        emailFocusRequester.requestFocus()
                                                    } catch (_: Exception) {
                                                    }
                                                }
                                                .padding(vertical = 4.dp)
                                                .testTag("change_email_link")
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = password,
                                        onValueChange = { password = it },
                                        label = { Text("Password") },
                                        placeholder = { Text("Enter your password") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = "Password"
                                            )
                                        },
                                        trailingIcon = {
                                            IconButton(onClick = { showSignInPassword = !showSignInPassword }) {
                                                Icon(
                                                    imageVector = if (showSignInPassword) {
                                                        Icons.Default.VisibilityOff
                                                    } else {
                                                        Icons.Default.Visibility
                                                    },
                                                    contentDescription = "Toggle password visibility"
                                                )
                                            }
                                        },
                                        singleLine = true,
                                        visualTransformation = if (showSignInPassword) {
                                            VisualTransformation.None
                                        } else {
                                            PasswordVisualTransformation()
                                        },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = textFieldColors,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("login_password_input")
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = "Forgot Password?",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = VibeEmerald,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier
                                                .align(Alignment.CenterEnd)
                                                .clickable {
                                                    step = AuthStep.FORGOT_PASSWORD
                                                }
                                                .padding(vertical = 4.dp)
                                                .testTag("forgot_password_link")
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(18.dp))

                                    Button(
                                        onClick = {
                                            if (email.isBlank()) {
                                                onShowMessage("Please enter your Email Address or Username")
                                            } else {
                                                pendingOrigin = AuthStep.SIGN_IN
                                                otpCode = ""
                                                onRequestOtp(email, fullName, username) {
                                                    step = AuthStep.OTP_VERIFY
                                                }
                                            }
                                        },
                                        enabled = !isLoading,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(54.dp)
                                            .testTag("login_submit_button"),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = VibeEmerald,
                                            contentColor = Color(0xFF042F2E)
                                        )
                                    ) {
                                        if (isLoading) {
                                             CircularProgressIndicator(
                                                 modifier = Modifier.size(22.dp),
                                                 strokeWidth = 2.dp,
                                                 color = Color(0xFF042F2E)
                                             )
                                        } else {
                                             Text(
                                                 text = "Sign In",
                                                 style = MaterialTheme.typography.titleMedium,
                                                 fontWeight = FontWeight.Bold
                                             )
                                        }
                                    }

                                    if (password.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "Or sign in directly with password",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier
                                                .align(Alignment.CenterHorizontally)
                                                .clickable {
                                                    onSignInPassword(email, password)
                                                }
                                                .padding(vertical = 4.dp)
                                                .testTag("direct_password_signin_button")
                                        )
                                    }

                                    OrDivider(text = "or continue with")

                                    GoogleSignInButton(
                                        onClick = handleGoogleClick,
                                        enabled = !isLoading,
                                        text = "Sign in with Google"
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(22.dp))

                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Don't have an account?  ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Create Account",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = VibeEmerald,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clickable { step = AuthStep.CREATE_ACCOUNT }
                                        .padding(vertical = 4.dp)
                                        .testTag("go_to_create_account_button")
                                )
                            }
                        } else {
                            // CREATE ACCOUNT CARD (Original UI Structure with Avatar + Username + Present Architecture)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(24.dp)
                                    ),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Registration",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.align(Alignment.Start)
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "Create your profile and @username. A 6-digit verification code will be sent to confirm your email.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 18.sp,
                                        modifier = Modifier.align(Alignment.Start)
                                    )

                                    Spacer(modifier = Modifier.height(18.dp))

                                    // Live Avatar Preview & Color Picker from the Original UI
                                    val previewHandle = username.trim().removePrefix("@").ifBlank {
                                        fullName.trim().lowercase().replace("\\s+".toRegex(), "_").ifBlank { "username" }
                                    }
                                    VibeAvatar(
                                        name = fullName.ifBlank { "Vibe User" },
                                        avatarColorHex = selectedAvatarColor,
                                        size = 72.dp,
                                        isOnline = true,
                                        showOnlineIndicator = true
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = "@$previewHandle",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = VibeEmerald,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AvatarColors.take(6).forEach { hex ->
                                            val isSelected = selectedAvatarColor == hex
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(parseHexColor(hex))
                                                    .border(
                                                        width = if (isSelected) 2.5.dp else 0.dp,
                                                        color = if (isSelected) Color.White else Color.Transparent,
                                                        shape = CircleShape
                                                    )
                                                    .clickable { selectedAvatarColor = hex },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Selected avatar color",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(18.dp))

                                    // 1. Full Name
                                    OutlinedTextField(
                                        value = fullName,
                                        onValueChange = {
                                            val previousAuto = fullName.trim().lowercase().replace("\\s+".toRegex(), "_")
                                            fullName = it
                                            if (username.isBlank() || username == previousAuto) {
                                                username = it.trim().lowercase().replace("\\s+".toRegex(), "_")
                                            }
                                        },
                                        label = { Text("Full Name") },
                                        placeholder = { Text("Alex Rivera") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = "Full Name"
                                            )
                                        },
                                        singleLine = true,
                                        shape = RoundedCornerShape(16.dp),
                                        colors = textFieldColors,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("register_name_input")
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // 2. Username (@handle) — from the Original UI
                                    OutlinedTextField(
                                        value = username,
                                        onValueChange = { username = it.removePrefix("@").lowercase().replace(" ", "_") },
                                        label = { Text("Username (@handle)") },
                                        placeholder = { Text("alex_vibe") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.AlternateEmail,
                                                contentDescription = "Username"
                                            )
                                        },
                                        singleLine = true,
                                        shape = RoundedCornerShape(16.dp),
                                        colors = textFieldColors,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("register_username_input")
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))



                                    // 4. Email Address
                                    OutlinedTextField(
                                        value = email,
                                        onValueChange = { email = it },
                                        label = { Text("Email Address") },
                                        placeholder = { Text("you@vibechat.io") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Email,
                                                contentDescription = "Email Address"
                                            )
                                        },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = textFieldColors,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("register_email_input")
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // 5. Password (min 6 characters)
                                    OutlinedTextField(
                                        value = password,
                                        onValueChange = { password = it },
                                        label = { Text("Password (min 6 characters)") },
                                        placeholder = { Text("Min 6 characters") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = "Password"
                                            )
                                        },
                                        trailingIcon = {
                                            IconButton(onClick = { showRegisterPassword = !showRegisterPassword }) {
                                                Icon(
                                                    imageVector = if (showRegisterPassword) {
                                                        Icons.Default.VisibilityOff
                                                    } else {
                                                        Icons.Default.Visibility
                                                    },
                                                    contentDescription = "Toggle password visibility"
                                                )
                                            }
                                        },
                                        singleLine = true,
                                        visualTransformation = if (showRegisterPassword) {
                                            VisualTransformation.None
                                        } else {
                                            PasswordVisualTransformation()
                                        },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = textFieldColors,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("register_password_input")
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // 6. Confirm Password — from Present Architecture
                                    OutlinedTextField(
                                        value = confirmPassword,
                                        onValueChange = { confirmPassword = it },
                                        label = { Text("Confirm Password") },
                                        placeholder = { Text("Re-enter password") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = "Confirm Password"
                                            )
                                        },
                                        trailingIcon = {
                                            IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                                                Icon(
                                                    imageVector = if (showConfirmPassword) {
                                                        Icons.Default.VisibilityOff
                                                    } else {
                                                        Icons.Default.Visibility
                                                    },
                                                    contentDescription = "Toggle confirm password visibility"
                                                )
                                            }
                                        },
                                        singleLine = true,
                                        visualTransformation = if (showConfirmPassword) {
                                            VisualTransformation.None
                                        } else {
                                            PasswordVisualTransformation()
                                        },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = textFieldColors,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("register_confirm_password_input")
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // 7. Short Bio / Status — from Original UI
                                    OutlinedTextField(
                                        value = bio,
                                        onValueChange = { bio = it },
                                        label = { Text("Bio / Vibe Status") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = "Bio"
                                            )
                                        },
                                        singleLine = true,
                                        shape = RoundedCornerShape(16.dp),
                                        colors = textFieldColors,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("register_bio_input")
                                    )

                                    Spacer(modifier = Modifier.height(20.dp))

                                    Button(
                                        onClick = {
                                            val resolvedUsername = username.trim().removePrefix("@")
                                                .ifBlank { fullName.trim().lowercase().replace("\\s+".toRegex(), "_") }
                                            when {
                                                fullName.isBlank() -> onShowMessage("Please enter your Full Name")
                                                resolvedUsername.isBlank() -> onShowMessage("Please choose a @username")
                                                email.isBlank() || !email.contains("@") -> onShowMessage("Please enter a valid Email Address")
                                                password.length < 6 -> onShowMessage("Password must be at least 6 characters")
                                                confirmPassword.isNotEmpty() && password != confirmPassword ->
                                                    onShowMessage("Passwords do not match")
                                                else -> {
                                                    username = resolvedUsername
                                                    pendingOrigin = AuthStep.CREATE_ACCOUNT
                                                    otpCode = ""
                                                    val resolvedBio = bio
                                                    onSignUpEmail(
                                                        email,
                                                        password,
                                                        fullName,
                                                        resolvedUsername,
                                                        resolvedBio,
                                                        selectedAvatarColor
                                                    ) {
                                                        step = AuthStep.OTP_VERIFY
                                                    }
                                                }
                                            }
                                        },
                                        enabled = !isLoading,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(54.dp)
                                            .testTag("register_submit_button"),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = VibeEmerald,
                                            contentColor = Color(0xFF042F2E)
                                        )
                                    ) {
                                        if (isLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(22.dp),
                                                strokeWidth = 2.dp,
                                                color = Color(0xFF042F2E)
                                            )
                                        } else {
                                            Text(
                                                text = "Create Account",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    OrDivider(text = "or register with")

                                    GoogleSignInButton(
                                        onClick = handleGoogleClick,
                                        enabled = !isLoading,
                                        text = "Sign up with Google"
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(22.dp))

                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Already have an account?  ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Sign In",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = VibeEmerald,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clickable { step = AuthStep.SIGN_IN }
                                        .padding(vertical = 4.dp)
                                        .testTag("go_to_sign_in_button")
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }

                AuthStep.OTP_VERIFY -> {
                    BackHandler { step = pendingOrigin }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 520.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 22.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(onClick = { step = pendingOrigin }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }

                            Text(
                                text = "6-Digit Verification",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.width(48.dp))
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(24.dp)
                                ),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (pendingOrigin == AuthStep.CREATE_ACCOUNT) {
                                        "Confirm Your Email"
                                    } else {
                                        "Enter Verification Code"
                                    },
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = if (pendingOrigin == AuthStep.CREATE_ACCOUNT) {
                                        "We sent a 6-digit confirmation code to $email. Please enter it to verify your account."
                                    } else {
                                        "We sent a 6-digit sign in code to $email. Please enter it to start your session."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(18.dp))

                                // Visual 6-Digit Preview Boxes from Original UI
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    for (i in 0 until 6) {
                                        val char = otpCode.getOrNull(i)?.toString() ?: ""
                                        val isActive = otpCode.length == i
                                        Box(
                                            modifier = Modifier
                                                .size(width = 42.dp, height = 50.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                                )
                                                .border(
                                                    width = if (isActive || char.isNotEmpty()) 1.5.dp else 1.dp,
                                                    color = if (isActive || char.isNotEmpty()) {
                                                        VibeEmerald
                                                    } else {
                                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                                    },
                                                    shape = RoundedCornerShape(12.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = char,
                                                style = MaterialTheme.typography.titleLarge,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                OutlinedTextField(
                                    value = otpCode,
                                    onValueChange = { if (it.length <= 6) otpCode = it.filter(Char::isDigit) },
                                    label = { Text("Enter 6-Digit Code") },
                                    placeholder = { Text("Enter 6-digit code") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Verification Code"
                                        )
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = textFieldColors,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("otp_code_input")
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Change Email / Username?",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = VibeEmerald,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier
                                            .clickable { step = pendingOrigin }
                                            .padding(vertical = 4.dp)
                                    )

                                    Text(
                                        text = "Resend Code",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = VibeEmerald,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier
                                            .clickable {
                                                if (pendingOrigin == AuthStep.CREATE_ACCOUNT) {
                                                    val resolvedHandle = username.trim().removePrefix("@")
                                                        .ifBlank { fullName.trim().lowercase().replace("\\s+".toRegex(), "_") }
                                                        .ifBlank { email.substringBefore("@") }
                                                    val resolvedBio = bio
                                                    onSignUpEmail(email, password, fullName, resolvedHandle, resolvedBio, selectedAvatarColor) {}
                                                } else {
                                                    onRequestOtp(email, fullName, username) {}
                                                }
                                            }
                                            .padding(vertical = 4.dp)
                                            .testTag("resend_otp_button")
                                    )
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Button(
                                    onClick = {
                                        val resolvedHandle = username.trim().removePrefix("@")
                                            .ifBlank {
                                                fullName.trim().lowercase().replace("\\s+".toRegex(), "_")
                                            }
                                            .ifBlank { email.substringBefore("@") }
                                        if (otpCode.length < 6) {
                                            onShowMessage("Please enter the complete 6-digit verification code sent to your email")
                                        } else {
                                            onVerifyOtp(
                                                email,
                                                otpCode,
                                                fullName,
                                                resolvedHandle,
                                                selectedAvatarColor
                                            )
                                        }
                                    },
                                    enabled = !isLoading,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                        .testTag("otp_verify_button"),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = VibeEmerald,
                                        contentColor = Color(0xFF042F2E)
                                    )
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(22.dp),
                                            strokeWidth = 2.dp,
                                            color = Color(0xFF042F2E)
                                        )
                                    } else {
                                        Text(
                                            text = "Verify & Continue",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                AuthStep.FORGOT_PASSWORD -> {
                    BackHandler { step = AuthStep.SIGN_IN }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 520.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 22.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = { step = AuthStep.SIGN_IN },
                                modifier = Modifier.testTag("forgot_password_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to Sign In",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }

                            SupabaseConnectionBadge(onClick = {})
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(listOf(VibeEmerald, VibeCyan))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = Color(0xFF042F2E),
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Reset Password",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Enter your registered email address. A secure recovery code will be sent to your inbox.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(24.dp)
                                ),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 24.dp)
                            ) {
                                OutlinedTextField(
                                    value = email,
                                    onValueChange = { email = it },
                                    label = { Text("Email Address or @username") },
                                    placeholder = { Text("you@example.com") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Email,
                                            contentDescription = "Email"
                                        )
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = textFieldColors,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("reset_email_input")
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                Button(
                                    onClick = {
                                        if (email.isBlank()) {
                                            onShowMessage("Please enter your registered Email Address or Username")
                                        } else {
                                            onRequestPasswordReset(email) {
                                                otpCode = ""
                                                resetPassword = ""
                                                resetConfirmPassword = ""
                                                step = AuthStep.RESET_PASSWORD_VERIFY
                                            }
                                        }
                                    },
                                    enabled = !isLoading,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                        .testTag("send_reset_code_button"),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = VibeEmerald,
                                        contentColor = Color(0xFF042F2E)
                                    )
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(22.dp),
                                            strokeWidth = 2.dp,
                                            color = Color(0xFF042F2E)
                                        )
                                    } else {
                                        Text(
                                            text = "Send Reset Code",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        TextButton(onClick = { step = AuthStep.SIGN_IN }) {
                            Text(
                                text = "Back to Sign In",
                                style = MaterialTheme.typography.labelLarge,
                                color = VibeEmerald,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                AuthStep.RESET_PASSWORD_VERIFY -> {
                    BackHandler { step = AuthStep.FORGOT_PASSWORD }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 520.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 22.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = { step = AuthStep.FORGOT_PASSWORD },
                                modifier = Modifier.testTag("reset_verify_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }

                            Text(
                                text = "New Password",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.width(48.dp))
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(24.dp)
                                ),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Enter Reset Code",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "Enter the 6-digit password reset verification code sent to $email, then choose your new password.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(18.dp))

                                // Visual 6-Digit Preview Boxes
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    for (i in 0 until 6) {
                                        val char = otpCode.getOrNull(i)?.toString() ?: ""
                                        val isActive = otpCode.length == i
                                        Box(
                                            modifier = Modifier
                                                .size(width = 42.dp, height = 50.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                                )
                                                .border(
                                                    width = if (isActive || char.isNotEmpty()) 1.5.dp else 1.dp,
                                                    color = if (isActive || char.isNotEmpty()) {
                                                        VibeEmerald
                                                    } else {
                                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                                    },
                                                    shape = RoundedCornerShape(12.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = char,
                                                style = MaterialTheme.typography.titleLarge,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                OutlinedTextField(
                                    value = otpCode,
                                    onValueChange = { if (it.length <= 6) otpCode = it.filter(Char::isDigit) },
                                    label = { Text("6-Digit Recovery Code") },
                                    placeholder = { Text("Enter 6-digit code") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Code"
                                        )
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = textFieldColors,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("reset_otp_input")
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                OutlinedTextField(
                                    value = resetPassword,
                                    onValueChange = { resetPassword = it },
                                    label = { Text("New Password") },
                                    placeholder = { Text("At least 6 characters") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "New Password"
                                        )
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { showResetPassword = !showResetPassword }) {
                                            Icon(
                                                imageVector = if (showResetPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = if (showResetPassword) "Hide password" else "Show password"
                                            )
                                        }
                                    },
                                    visualTransformation = if (showResetPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = textFieldColors,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("reset_new_password_input")
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                OutlinedTextField(
                                    value = resetConfirmPassword,
                                    onValueChange = { resetConfirmPassword = it },
                                    label = { Text("Confirm New Password") },
                                    placeholder = { Text("Re-enter new password") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Confirm New Password"
                                        )
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { showResetConfirmPassword = !showResetConfirmPassword }) {
                                            Icon(
                                                imageVector = if (showResetConfirmPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = if (showResetConfirmPassword) "Hide password" else "Show password"
                                            )
                                        }
                                    },
                                    visualTransformation = if (showResetConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = textFieldColors,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("reset_confirm_password_input")
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Change Email?",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = VibeEmerald,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier
                                            .clickable { step = AuthStep.FORGOT_PASSWORD }
                                            .padding(vertical = 4.dp)
                                    )

                                    Text(
                                        text = "Resend Code",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = VibeEmerald,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier
                                            .clickable {
                                                onRequestPasswordReset(email) {}
                                            }
                                            .padding(vertical = 4.dp)
                                            .testTag("resend_reset_otp_button")
                                    )
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Button(
                                    onClick = {
                                        if (otpCode.length < 6) {
                                            onShowMessage("Please enter the complete 6-digit recovery code from your email")
                                        } else if (resetPassword.length < 6) {
                                            onShowMessage("New password must be at least 6 characters")
                                        } else if (resetPassword != resetConfirmPassword) {
                                            onShowMessage("Passwords do not match")
                                        } else {
                                            onVerifyPasswordReset(email, otpCode, resetPassword)
                                        }
                                    },
                                    enabled = !isLoading,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                        .testTag("set_new_password_button"),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = VibeEmerald,
                                        contentColor = Color(0xFF042F2E)
                                    )
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(22.dp),
                                            strokeWidth = 2.dp,
                                            color = Color(0xFF042F2E)
                                        )
                                    } else {
                                        Text(
                                            text = "Set New Password & Sign In",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
        }
    }

    if (showGoogleChooserSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var customAccountEmail by remember { mutableStateOf("") }
        var showCustomInput by remember { mutableStateOf(false) }

        // Dynamically read all email addresses registered & visible on this device (never hardcoded)
        val allDeviceAccounts = remember(context) {
            val palette = listOf(
                Color(0xFF1E88E5),
                Color(0xFF00897B),
                Color(0xFF455A64),
                Color(0xFF6366F1),
                Color(0xFFEC4899)
            )
            GoogleAuthManager.getVisibleDeviceEmailAccounts(context).mapIndexed { idx, accEmail ->
                val derivedName = accEmail.substringBefore("@")
                    .replace(".", " ")
                    .replace("_", " ")
                    .split(" ")
                    .filter { it.isNotBlank() }
                    .joinToString(" ") { part ->
                        part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                    }
                    .ifBlank { accEmail }
                GoogleAccountItem(
                    name = derivedName,
                    email = accEmail,
                    initial = derivedName.firstOrNull()?.uppercase() ?: "G",
                    avatarColor = palette[idx % palette.size]
                )
            }
        }

        ModalBottomSheet(
            onDismissRequest = { showGoogleChooserSheet = false },
            sheetState = sheetState,
            containerColor = Color(0xFF131314),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = null
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.Start
                ) {
                    // Top Google Identity Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_google_logo),
                                contentDescription = "Google Logo",
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "Sign in with Google",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFE3E3E3),
                                fontSize = 14.sp
                            )
                        }

                        IconButton(
                            onClick = { showGoogleChooserSheet = false },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF9AA0A6),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Official VibeChat App Logo
                    Image(
                        painter = painterResource(id = R.drawable.img_app_logo),
                        contentDescription = "VibeChat Logo",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Headline and Subtitle
                    Text(
                        text = "Choose an account",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Normal,
                        fontSize = 30.sp,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "to continue to ",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFFC4C7C5),
                            fontSize = 16.sp
                        )
                        Text(
                            text = "VibeChat",
                            style = MaterialTheme.typography.bodyLarge,
                            color = VibeEmerald,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Divider before accounts
                    HorizontalDivider(
                        color = Color(0xFF3C4043).copy(alpha = 0.7f),
                        thickness = 0.8.dp
                    )

                    // Dynamically discovered device email accounts
                    allDeviceAccounts.forEach { account ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showGoogleChooserSheet = false
                                    onSignInGoogle(
                                        "",
                                        account.email,
                                        account.name,
                                        null
                                    )
                                }
                                .padding(vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(account.avatarColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = account.initial,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = account.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFE8EAED),
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = account.email,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF9AA0A6),
                                    fontSize = 14.sp
                                )
                            }
                        }

                        HorizontalDivider(
                            color = Color(0xFF3C4043).copy(alpha = 0.5f),
                            thickness = 0.6.dp
                        )
                    }

                    // Browse system device email accounts
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showGoogleChooserSheet = false
                                try {
                                    accountChooserLauncher.launch(
                                        GoogleAuthManager.createAccountChooserIntent(context)
                                    )
                                } catch (_: Exception) {
                                    onShowMessage("No system account picker available. Enter your email below.")
                                }
                            }
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF282A2D)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "Choose from device emails",
                                tint = VibeEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Text(
                            text = "Choose from device accounts",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFE8EAED),
                            fontSize = 15.sp
                        )
                    }

                    HorizontalDivider(
                        color = Color(0xFF3C4043).copy(alpha = 0.5f),
                        thickness = 0.6.dp
                    )

                    // "Use another account" row
                    if (!showCustomInput) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showCustomInput = true }
                                .padding(vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF282A2D)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = "Use another account",
                                    tint = Color(0xFFC4C7C5),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Text(
                                text = "Use another account",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFE8EAED),
                                fontSize = 15.sp
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = customAccountEmail,
                                onValueChange = { customAccountEmail = it },
                                placeholder = { Text("Enter email address", color = Color(0xFF8E918F)) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color(0xFFE8EAED),
                                    focusedContainerColor = Color(0xFF1E2024),
                                    unfocusedContainerColor = Color(0xFF1E2024),
                                    focusedBorderColor = Color(0xFFA8C7FA),
                                    unfocusedBorderColor = Color(0xFF3C4043)
                                )
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { showCustomInput = false }) {
                                    Text("Cancel", color = Color(0xFF9AA0A6))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (customAccountEmail.isNotBlank()) {
                                            showGoogleChooserSheet = false
                                            val clean = customAccountEmail.trim().lowercase()
                                            val name = clean.substringBefore("@")
                                                .replace(".", " ")
                                                .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                                            onSignInGoogle("", clean, name, null)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA8C7FA)),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Text("Next", color = Color(0xFF040E1B), fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Bottom Google consent & disclaimer
                    Text(
                        text = "To continue, Google will share your name, email address, language preference, and profile picture with VibeChat. Before using this app, you can review VibeChat's Privacy Policy and Terms of Service.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8E918F),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
}
}





@Composable
private fun WelcomeFeaturePill(
    icon: ImageVector,
    label: String
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = VibeEmerald,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun OrDivider(
    text: String = "or continue with"
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
        )
        Text(
            text = "  $text  ",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
        )
    }
}

@Composable
private fun GoogleSignInButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    text: String = "Sign in with Google"
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("google_signin_button"),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_google_logo),
                contentDescription = "Google Logo",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

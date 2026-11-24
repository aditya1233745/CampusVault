package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AuthManager
import com.example.model.CampusConstants
import com.example.ui.components.EyeIcon
import com.example.ui.components.LockIcon
import com.example.ui.components.MailIcon
import com.example.ui.components.PhoneIcon
import com.example.ui.components.PillInputField
import com.example.ui.components.ProfileIcon
import com.example.ui.components.SpringActionButton
import com.example.ui.components.VerifiedCheckIcon
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.BrandHeroGradient
import com.example.ui.theme.DisplayFamily
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.GreenActionGradient
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.SunsetOrange
import com.example.ui.theme.SurfaceSubtle
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class AuthScreenStep { WELCOME, LOGIN, SIGNUP }

@Composable
fun AuthFlowScreen(
    authManager: AuthManager,
    onAuthSuccess: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf(AuthScreenStep.WELCOME) }
    var isLoading by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    AnimatedContent(
        targetState = step,
        transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(180)) },
        label = "authStepTransition"
    ) { currentStep ->
        when (currentStep) {
            AuthScreenStep.WELCOME -> WelcomeHeroScreen(
                onGetStarted = {
                    feedbackMessage = null
                    step = AuthScreenStep.LOGIN
                }
            )
            AuthScreenStep.LOGIN -> {
                BackHandler { step = AuthScreenStep.WELCOME }
                LoginSheetScreen(
                    isLoading = isLoading,
                    message = feedbackMessage,
                    onSwitchToSignUp = {
                        feedbackMessage = null
                        step = AuthScreenStep.SIGNUP
                    },
                    onForgotPassword = { email ->
                        if (!isLoading) {
                            scope.launch {
                                isLoading = true
                                feedbackMessage = null
                                val result = authManager.sendPasswordReset(email)
                                isLoading = false
                                feedbackMessage = result.fold(
                                    onSuccess = { "Check your email to reset your password." },
                                    onFailure = { it.message ?: "Failed to send reset email." }
                                )
                            }
                        }
                    },
                    onLogin = { email, password ->
                        if (!isLoading) {
                            scope.launch {
                                isLoading = true
                                feedbackMessage = null
                                val result = authManager.signInWithEmail(email, password)
                                isLoading = false
                                result.onSuccess { onAuthSuccess() }
                                    .onFailure { feedbackMessage = it.message ?: "Login failed." }
                            }
                        }
                    }
                )
            }
            AuthScreenStep.SIGNUP -> {
                BackHandler { step = AuthScreenStep.LOGIN }
                SignUpSheetScreen(
                    isLoading = isLoading,
                    message = feedbackMessage,
                    onSwitchToLogin = {
                        feedbackMessage = null
                        step = AuthScreenStep.LOGIN
                    },
                    onSignUp = { fullName, email, password, branch, gradYear, phone ->
                        if (!isLoading) {
                            scope.launch {
                                isLoading = true
                                feedbackMessage = null
                                val result = authManager.signUpWithEmail(
                                    fullName = fullName,
                                    email = email,
                                    password = password,
                                    branch = branch,
                                    gradYear = gradYear,
                                    phoneNumber = phone
                                )
                                isLoading = false
                                result.onSuccess { onAuthSuccess() }
                                    .onFailure { feedbackMessage = it.message ?: "Sign up failed." }
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun WelcomeHeroScreen(onGetStarted: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandHeroGradient)
            .statusBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            HeroBackdrop(modifier = Modifier.fillMaxSize())
            AnimatedWordmark()
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = PureWhite,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 28.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SpringActionButton(
                    text = "Get Started",
                    gradient = GreenActionGradient,
                    onClick = onGetStarted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp)
                        .testTag("get_started_button")
                )
            }
        }
    }
}

@Composable
fun LoginSheetScreen(
    isLoading: Boolean,
    message: String?,
    onSwitchToSignUp: () -> Unit,
    onForgotPassword: (String) -> Unit,
    onLogin: (String, String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(true) }
    var localError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandHeroGradient)
            .statusBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
        ) {
            AuthHeaderPattern(modifier = Modifier.fillMaxSize())
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 24.dp, vertical = 22.dp)
            ) {
                Text(
                    text = "CampusVault",
                    fontFamily = DisplayFamily,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.8).sp,
                    color = PureWhite
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(SunsetOrange)
                )
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = PureWhite,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Login", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Don't have an account? ", fontSize = 13.sp, color = TextSecondary)
                    Text(
                        "Sign Up",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = SunsetOrange,
                        modifier = Modifier
                            .testTag("switch_to_signup")
                            .clickable(enabled = !isLoading, onClick = onSwitchToSignUp)
                            .padding(vertical = 4.dp)
                    )
                }

                Spacer(Modifier.height(22.dp))

                PillInputField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = "College Email",
                    keyboardType = KeyboardType.Email,
                    leadingIcon = { MailIcon(color = TextSecondary, size = 18.dp) },
                    modifier = Modifier.testTag("login_email_input")
                )

                Spacer(Modifier.height(14.dp))

                PillInputField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = "Password",
                    isPassword = !passwordVisible,
                    keyboardType = KeyboardType.Password,
                    leadingIcon = { LockIcon(color = TextSecondary, size = 18.dp) },
                    trailingIcon = {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { passwordVisible = !passwordVisible },
                            contentAlignment = Alignment.Center
                        ) {
                            EyeIcon(
                                color = if (passwordVisible) EmeraldGreen else TextSecondary,
                                size = 18.dp
                            )
                        }
                    },
                    modifier = Modifier.testTag("login_password_input")
                )

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { rememberMe = !rememberMe }
                            .padding(vertical = 4.dp, horizontal = 2.dp)
                    ) {
                        VerifiedCheckIcon(
                            color = if (rememberMe) EmeraldGreen else TextSecondary,
                            size = 18.dp
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            "Remember Me",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        "Forgot Password?",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = SunsetOrange,
                        modifier = Modifier
                            .testTag("forgot_password_button")
                            .clickable(enabled = !isLoading) {
                                localError = null
                                onForgotPassword(email)
                            }
                            .padding(vertical = 4.dp)
                    )
                }

                AuthMessageText(localError ?: message)

                Spacer(Modifier.height(22.dp))

                SpringActionButton(
                    text = if (isLoading) "Please wait..." else "Login",
                    gradient = GreenActionGradient,
                    enabled = !isLoading,
                    onClick = {
                        when {
                            !email.contains("@") -> localError = "Enter a valid college email"
                            password.length < 6 -> localError = "Password must be at least 6 characters"
                            else -> {
                                localError = null
                                onLogin(email.trim(), password)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp)
                        .testTag("login_submit_button")
                )
            }
        }
    }
}

@Composable
fun SignUpSheetScreen(
    isLoading: Boolean,
    message: String?,
    onSwitchToLogin: () -> Unit,
    onSignUp: (String, String, String, String, String, String) -> Unit
) {
    var localError by remember { mutableStateOf<String?>(null) }
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var selectedBranch by remember { mutableStateOf("CSE") }
    var selectedYear by remember { mutableStateOf("2026") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandHeroGradient)
            .statusBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            AuthHeaderPattern(modifier = Modifier.fillMaxSize())
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 24.dp, vertical = 22.dp)
            ) {
                Text(
                    text = "CampusVault",
                    fontFamily = DisplayFamily,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.8).sp,
                    color = PureWhite
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(SunsetOrange)
                )
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = PureWhite,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Sign Up", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Already have an account? ", fontSize = 13.sp, color = TextSecondary)
                    Text(
                        "Login",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = SunsetOrange,
                        modifier = Modifier
                            .testTag("switch_to_login")
                            .clickable(enabled = !isLoading, onClick = onSwitchToLogin)
                            .padding(vertical = 4.dp)
                    )
                }

                Spacer(Modifier.height(20.dp))

                PillInputField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    placeholder = "Full Name",
                    leadingIcon = { ProfileIcon(color = TextSecondary, size = 18.dp) },
                    modifier = Modifier.testTag("signup_name_input")
                )
                Spacer(Modifier.height(12.dp))
                PillInputField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = "College Email",
                    keyboardType = KeyboardType.Email,
                    leadingIcon = { MailIcon(color = TextSecondary, size = 18.dp) },
                    modifier = Modifier.testTag("signup_email_input")
                )
                Spacer(Modifier.height(12.dp))
                PillInputField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = "Password",
                    isPassword = !passwordVisible,
                    keyboardType = KeyboardType.Password,
                    leadingIcon = { LockIcon(color = TextSecondary, size = 18.dp) },
                    trailingIcon = {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { passwordVisible = !passwordVisible },
                            contentAlignment = Alignment.Center
                        ) {
                            EyeIcon(
                                color = if (passwordVisible) EmeraldGreen else TextSecondary,
                                size = 18.dp
                            )
                        }
                    },
                    modifier = Modifier.testTag("signup_password_input")
                )
                Spacer(Modifier.height(12.dp))
                PillInputField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it.filter { ch -> ch.isDigit() || ch == '+' || ch == ' ' || ch == '-' }.take(20) },
                    placeholder = "Phone Number (shared only on pickup requests)",
                    keyboardType = KeyboardType.Phone,
                    leadingIcon = { PhoneIcon(color = TextSecondary, size = 18.dp) },
                    modifier = Modifier.testTag("signup_phone_input")
                )

                Spacer(Modifier.height(16.dp))
                Text(
                    "Branch",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(CampusConstants.BRANCHES) { branch ->
                        val active = branch == selectedBranch
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (active) EmeraldGreen else SurfaceSubtle)
                                .clickable { selectedBranch = branch }
                                .padding(horizontal = 16.dp, vertical = 9.dp)
                        ) {
                            Text(
                                branch,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (active) PureWhite else TextPrimary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text(
                    "Graduation Year",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CampusConstants.GRAD_YEARS.forEach { year ->
                        val active = year == selectedYear
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(50))
                                .background(if (active) EmeraldGreen else SurfaceSubtle)
                                .clickable { selectedYear = year }
                                .padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                year,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (active) PureWhite else TextPrimary
                            )
                        }
                    }
                }

                AuthMessageText(localError ?: message)

                Spacer(Modifier.height(22.dp))
                SpringActionButton(
                    text = if (isLoading) "Please wait..." else "Create Account",
                    gradient = GreenActionGradient,
                    enabled = !isLoading,
                    onClick = {
                        when {
                            fullName.isBlank() -> localError = "Enter your full name"
                            !email.contains("@") -> localError = "Enter a valid college email"
                            password.length < 6 -> localError = "Password must be at least 6 characters"
                            else -> {
                                localError = null
                                onSignUp(
                                    fullName.trim(),
                                    email.trim(),
                                    password,
                                    selectedBranch,
                                    selectedYear,
                                    phoneNumber.trim()
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp)
                        .testTag("signup_submit_button")
                )
            }
        }
    }
}

@Composable
fun CampusProfileSetupScreen(
    initialName: String,
    collegeEmail: String,
    initialBranch: String = "CSE",
    initialYear: String = "2026",
    initialPhone: String = "",
    isEditing: Boolean = false,
    isBusy: Boolean = false,
    errorMessage: String? = null,
    onBack: (() -> Unit)? = null,
    onSaveProfile: (fullName: String, branch: String, gradYear: String, phoneNumber: String) -> Unit
) {
    if (onBack != null) {
        BackHandler(onBack = onBack)
    }
    var fullName by remember(initialName) { mutableStateOf(initialName.ifBlank { "Member" }) }
    var phoneNumber by remember(initialPhone) { mutableStateOf(initialPhone) }
    var selectedBranch by remember(initialBranch) { mutableStateOf(initialBranch) }
    var selectedYear by remember(initialYear) { mutableStateOf(initialYear) }
    var localError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandHeroGradient)
            .statusBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            AuthHeaderPattern(modifier = Modifier.fillMaxSize())
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 24.dp, vertical = 22.dp)
            ) {
                Text(
                    text = "CampusVault",
                    fontFamily = DisplayFamily,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.8).sp,
                    color = PureWhite
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(SunsetOrange)
                )
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = PureWhite,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    if (isEditing) "Update Campus Profile" else "Complete Your Profile",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(Modifier.height(4.dp))
                Text(collegeEmail, fontSize = 13.sp, color = TextSecondary)
                Spacer(Modifier.height(20.dp))

                PillInputField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    placeholder = "Full Name",
                    leadingIcon = { ProfileIcon(color = TextSecondary, size = 18.dp) },
                    modifier = Modifier.testTag("profile_name_input")
                )
                Spacer(Modifier.height(12.dp))
                PillInputField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it.filter { ch -> ch.isDigit() || ch == '+' || ch == ' ' || ch == '-' }.take(20) },
                    placeholder = "Phone Number (auto-filled when requesting)",
                    keyboardType = KeyboardType.Phone,
                    leadingIcon = { PhoneIcon(color = TextSecondary, size = 18.dp) },
                    modifier = Modifier.testTag("profile_phone_input")
                )

                Spacer(Modifier.height(18.dp))
                Text(
                    "Branch",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(CampusConstants.BRANCHES) { branch ->
                        val active = branch == selectedBranch
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (active) EmeraldGreen else SurfaceSubtle)
                                .clickable { selectedBranch = branch }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                branch,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (active) PureWhite else TextPrimary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    "Graduation Year",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CampusConstants.GRAD_YEARS.forEach { year ->
                        val active = year == selectedYear
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(50))
                                .background(if (active) EmeraldGreen else SurfaceSubtle)
                                .clickable { selectedYear = year }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                year,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (active) PureWhite else TextPrimary
                            )
                        }
                    }
                }

                AuthMessageText(localError ?: errorMessage)

                Spacer(Modifier.height(24.dp))
                SpringActionButton(
                    text = if (isBusy) "Saving..." else if (isEditing) "Save Changes" else "Enter CampusVault",
                    gradient = GreenActionGradient,
                    enabled = !isBusy,
                    onClick = {
                        if (fullName.isBlank()) {
                            localError = "Enter your full name"
                        } else {
                            localError = null
                            onSaveProfile(fullName.trim(), selectedBranch, selectedYear, phoneNumber.trim())
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_profile_button")
                )

                if (onBack != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Cancel",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onBack)
                            .padding(horizontal = 18.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthMessageText(text: String?) {
    if (text.isNullOrBlank()) return
    val info = text.startsWith("Check your email")
    Spacer(Modifier.height(12.dp))
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (info) EmeraldGreen else AlertCrimson,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun HeroBackdrop(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "backdrop")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse),
        label = "drift"
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawCircle(PureWhite.copy(alpha = 0.07f), radius = w * 0.55f, center = Offset(w * (0.08f + 0.06f * drift), h * 0.12f))
        drawCircle(PureWhite.copy(alpha = 0.06f), radius = w * 0.42f, center = Offset(w * (0.98f - 0.08f * drift), h * 0.62f))
        drawCircle(SunsetOrange.copy(alpha = 0.28f), radius = w * 0.08f, center = Offset(w * 0.84f, h * (0.2f + 0.05f * drift)))
        drawCircle(SolarAmber.copy(alpha = 0.32f), radius = w * 0.035f, center = Offset(w * 0.14f, h * (0.7f - 0.05f * drift)))
    }
}

@Composable
private fun AuthHeaderPattern(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawCircle(PureWhite.copy(alpha = 0.08f), radius = w * 0.42f, center = Offset(w * 0.92f, h * 0.14f))
        drawCircle(PureWhite.copy(alpha = 0.06f), radius = w * 0.30f, center = Offset(w * 0.62f, h * 0.85f))
        drawCircle(SunsetOrange.copy(alpha = 0.32f), radius = w * 0.06f, center = Offset(w * 0.78f, h * 0.46f))
        drawCircle(SolarAmber.copy(alpha = 0.36f), radius = w * 0.025f, center = Offset(w * 0.24f, h * 0.22f))
    }
}

@Composable
private fun AnimatedWordmark(modifier: Modifier = Modifier) {
    val word = "CampusVault"
    val inspecting = LocalInspectionMode.current
    val letters = remember { word.map { Animatable(if (inspecting) 1f else 0f) } }
    val underline = remember { Animatable(if (inspecting) 1f else 0f) }

    LaunchedEffect(Unit) {
        letters.forEachIndexed { index, anim ->
            launch {
                delay(180L + index * 55L)
                anim.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 220f))
            }
        }
        delay(180L + word.length * 55L + 260L)
        underline.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
    }

    val shimmer = rememberInfiniteTransition(label = "wordShimmer")
    val sweep by shimmer.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            tween(2200, delayMillis = 1600, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "sweep"
    )

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    val band = size.width * 0.22f
                    val x = size.width * sweep
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(Color.Transparent, SolarAmber, SunsetOrange, Color.Transparent),
                            start = Offset(x - band, 0f),
                            end = Offset(x + band, size.height * 0.3f)
                        ),
                        blendMode = BlendMode.SrcAtop
                    )
                },
            verticalAlignment = Alignment.Bottom
        ) {
            word.forEachIndexed { index, ch ->
                Text(
                    text = ch.toString(),
                    fontFamily = DisplayFamily,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1).sp,
                    color = PureWhite,
                    modifier = Modifier.graphicsLayer {
                        val p = letters[index].value
                        alpha = p.coerceIn(0f, 1f)
                        translationY = (1f - p) * 90f
                        val sc = 0.5f + 0.5f * p
                        scaleX = sc
                        scaleY = sc
                    }
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .width(84.dp * underline.value.coerceIn(0f, 1f))
                .height(5.dp)
                .clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(listOf(SunsetOrange, SolarAmber)))
        )
    }
}

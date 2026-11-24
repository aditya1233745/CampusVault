package com.example.ui.components

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BorderLight
import com.example.ui.theme.DisplayFamily
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SkyBlue
import com.example.ui.theme.SunsetOrange
import com.example.ui.theme.SurfaceSubtle
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletPop

fun resolveDisplayName(context: Context, uri: Uri): String {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
        if (c.moveToFirst()) return c.getString(0) ?: "attachment"
    }
    return "attachment"
}

@Composable
fun PillInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                placeholder,
                fontSize = 14.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        singleLine = singleLine,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(if (singleLine) 50 else 24),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = SurfaceSubtle,
            unfocusedContainerColor = SurfaceSubtle,
            focusedBorderColor = EmeraldGreen,
            unfocusedBorderColor = Color.Transparent,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            cursorColor = EmeraldGreen
        ),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun SpringActionButton(
    text: String,
    gradient: Brush,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val haptics = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 450f),
        label = "btnScale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .height(54.dp)
            .clip(RoundedCornerShape(50))
            .background(if (enabled) gradient else Brush.horizontalGradient(listOf(TextSecondary.copy(alpha = 0.4f), TextSecondary.copy(alpha = 0.4f))))
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                role = Role.Button
            ) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontFamily = DisplayFamily,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            color = PureWhite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun AnimatedFloatingBottomBar(
    selectedTab: Int,
    pendingRequestCount: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .shadow(14.dp, RoundedCornerShape(50), ambientColor = Color.Black.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(50),
        color = PureWhite,
        border = BorderStroke(1.dp, BorderLight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FloatingNavPill(
                label = "Home",
                selected = selectedTab == 0,
                activeColor = EmeraldGreen,
                testTag = "nav_home",
                onClick = { onTabSelected(0) },
                icon = { color -> HomeIcon(color = color, size = 18.dp) }
            )
            FloatingNavPill(
                label = "Requests",
                selected = selectedTab == 1,
                activeColor = SkyBlue,
                badgeCount = pendingRequestCount,
                testTag = "nav_requests",
                onClick = { onTabSelected(1) },
                icon = { color -> ChatIcon(color = color, size = 18.dp) }
            )
            FloatingNavPill(
                label = "Post",
                selected = selectedTab == 2,
                activeColor = SunsetOrange,
                testTag = "nav_post",
                onClick = { onTabSelected(2) },
                icon = { color -> PlusIcon(color = color, size = 18.dp) }
            )
            FloatingNavPill(
                label = "Profile",
                selected = selectedTab == 3,
                activeColor = SkyBlue,
                testTag = "nav_profile",
                onClick = { onTabSelected(3) },
                icon = { color -> ProfileIcon(color = color, size = 18.dp) }
            )
        }
    }
}

@Composable
private fun FloatingNavPill(
    label: String,
    selected: Boolean,
    activeColor: Color,
    testTag: String,
    badgeCount: Int = 0,
    onClick: () -> Unit,
    icon: @Composable (Color) -> Unit
) {
    val bg by animateColorAsState(
        if (selected) activeColor.copy(alpha = 0.14f) else Color.Transparent,
        tween(200),
        label = "navBg"
    )
    val contentColor by animateColorAsState(
        if (selected) activeColor else TextSecondary,
        tween(200),
        label = "navFg"
    )
    Row(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag(testTag)
            .semantics { contentDescription = label }
            .clip(RoundedCornerShape(50))
            .background(bg)
            .animateContentSize(spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 380f))
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = if (selected) 15.dp else 13.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box {
            icon(contentColor)
            if (badgeCount > 0 && !selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(SunsetOrange)
                )
            }
        }
        AnimatedVisibility(visible = selected) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(6.dp))
                Text(
                    label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = contentColor,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

fun categoryAccent(category: String): Color = when (category) {
    "Notes" -> SkyBlue
    "Books" -> EmeraldGreen
    "Laptops & Tech" -> VioletPop
    "Calculators & Gear" -> SunsetOrange
    "Hostel" -> SunsetOrange
    "Uniforms" -> VioletPop
    else -> EmeraldGreen
}

@Composable
fun CategoryArtIcon(category: String, tint: Color, size: Dp) {
    when (category) {
        "Notes", "Books" -> BookArt(tint, size)
        "Laptops & Tech", "Calculators & Gear" -> BoltArt(tint, size)
        "Hostel" -> HomeIcon(tint, size)
        else -> BoxArt(tint, size)
    }
}

@Composable
fun BookArt(color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val s = Stroke(width = w * 0.1f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawRoundRect(color, Offset(w * 0.18f, h * 0.14f), Size(w * 0.64f, h * 0.72f), CornerRadius(w * 0.08f), style = s)
        drawLine(color, Offset(w * 0.34f, h * 0.14f), Offset(w * 0.34f, h * 0.86f), strokeWidth = w * 0.09f)
    }
}

@Composable
fun BoltArt(color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val path = Path().apply {
            moveTo(w * 0.56f, h * 0.12f)
            lineTo(w * 0.24f, h * 0.56f)
            lineTo(w * 0.50f, h * 0.56f)
            lineTo(w * 0.44f, h * 0.88f)
            lineTo(w * 0.76f, h * 0.44f)
            lineTo(w * 0.50f, h * 0.44f)
            close()
        }
        drawPath(path, color)
    }
}

@Composable
fun BoxArt(color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val s = Stroke(width = w * 0.1f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawRoundRect(color, Offset(w * 0.16f, h * 0.22f), Size(w * 0.68f, h * 0.58f), CornerRadius(w * 0.1f), style = s)
        drawLine(color, Offset(w * 0.16f, h * 0.42f), Offset(w * 0.84f, h * 0.42f), strokeWidth = w * 0.09f)
    }
}

@Composable
fun HomeIcon(color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val s = Stroke(width = w * 0.11f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val roof = Path().apply {
            moveTo(w * 0.16f, h * 0.48f)
            lineTo(w * 0.5f, h * 0.18f)
            lineTo(w * 0.84f, h * 0.48f)
        }
        drawPath(roof, color, style = s)
        drawRect(color, Offset(w * 0.26f, h * 0.48f), Size(w * 0.48f, h * 0.34f), style = s)
    }
}

@Composable
fun ChatIcon(color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val s = Stroke(width = w * 0.11f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val bubble = Path().apply {
            moveTo(w * 0.2f, h * 0.22f)
            lineTo(w * 0.8f, h * 0.22f)
            lineTo(w * 0.8f, h * 0.68f)
            lineTo(w * 0.42f, h * 0.68f)
            lineTo(w * 0.22f, h * 0.84f)
            lineTo(w * 0.22f, h * 0.68f)
            lineTo(w * 0.2f, h * 0.68f)
            close()
        }
        drawPath(bubble, color, style = s)
    }
}

@Composable
fun PlusIcon(color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.13f
        drawLine(color, Offset(w * 0.5f, h * 0.2f), Offset(w * 0.5f, h * 0.8f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.2f, h * 0.5f), Offset(w * 0.8f, h * 0.5f), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

@Composable
fun ProfileIcon(color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val s = Stroke(width = w * 0.11f, cap = StrokeCap.Round)
        drawCircle(color, radius = w * 0.18f, center = Offset(w * 0.5f, h * 0.34f), style = s)
        val arc = Path().apply {
            moveTo(w * 0.2f, h * 0.84f)
            quadraticTo(w * 0.5f, h * 0.58f, w * 0.8f, h * 0.84f)
        }
        drawPath(arc, color, style = s)
    }
}

@Composable
fun SearchIcon(color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val s = Stroke(width = w * 0.11f, cap = StrokeCap.Round)
        drawCircle(color, radius = w * 0.28f, center = Offset(w * 0.44f, h * 0.44f), style = s)
        drawLine(color, Offset(w * 0.65f, h * 0.65f), Offset(w * 0.84f, h * 0.84f), strokeWidth = w * 0.12f, cap = StrokeCap.Round)
    }
}

@Composable
fun BookmarkIcon(filled: Boolean, color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val path = Path().apply {
            moveTo(w * 0.24f, h * 0.16f)
            lineTo(w * 0.76f, h * 0.16f)
            lineTo(w * 0.76f, h * 0.84f)
            lineTo(w * 0.5f, h * 0.64f)
            lineTo(w * 0.24f, h * 0.84f)
            close()
        }
        if (filled) drawPath(path, color)
        else drawPath(path, color, style = Stroke(width = w * 0.11f, join = StrokeJoin.Round))
    }
}

@Composable
fun BackIcon(color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val s = Stroke(width = w * 0.12f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val chevron = Path().apply {
            moveTo(w * 0.62f, h * 0.24f)
            lineTo(w * 0.34f, h * 0.5f)
            lineTo(w * 0.62f, h * 0.76f)
        }
        drawPath(chevron, color, style = s)
    }
}

@Composable
fun UploadIcon(color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val s = Stroke(width = w * 0.11f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawLine(color, Offset(w * 0.5f, h * 0.78f), Offset(w * 0.5f, h * 0.24f), strokeWidth = w * 0.11f, cap = StrokeCap.Round)
        val arrow = Path().apply {
            moveTo(w * 0.28f, h * 0.44f)
            lineTo(w * 0.5f, h * 0.22f)
            lineTo(w * 0.72f, h * 0.44f)
        }
        drawPath(arrow, color, style = s)
    }
}

@Composable
fun ShieldIcon(color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val s = Stroke(width = w * 0.11f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.14f)
            lineTo(w * 0.82f, h * 0.28f)
            lineTo(w * 0.82f, h * 0.52f)
            quadraticTo(w * 0.82f, h * 0.76f, w * 0.5f, h * 0.88f)
            quadraticTo(w * 0.18f, h * 0.76f, w * 0.18f, h * 0.52f)
            lineTo(w * 0.18f, h * 0.28f)
            close()
        }
        drawPath(path, color, style = s)
    }
}

@Composable
fun VerifiedCheckIcon(color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val s = Stroke(width = w * 0.11f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawCircle(color, radius = w * 0.4f, center = Offset(w * 0.5f, h * 0.5f), style = s)
        val tick = Path().apply {
            moveTo(w * 0.32f, h * 0.51f)
            lineTo(w * 0.45f, h * 0.64f)
            lineTo(w * 0.69f, h * 0.38f)
        }
        drawPath(tick, color, style = s)
    }
}

@Composable
fun MailIcon(color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val s = Stroke(width = w * 0.1f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawRoundRect(color, Offset(w * 0.14f, h * 0.24f), Size(w * 0.72f, h * 0.52f), CornerRadius(w * 0.08f), style = s)
        val flap = Path().apply {
            moveTo(w * 0.16f, h * 0.28f)
            lineTo(w * 0.5f, h * 0.54f)
            lineTo(w * 0.84f, h * 0.28f)
        }
        drawPath(flap, color, style = s)
    }
}

@Composable
fun LockIcon(color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val s = Stroke(width = w * 0.1f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawRoundRect(color, Offset(w * 0.22f, h * 0.46f), Size(w * 0.56f, h * 0.38f), CornerRadius(w * 0.08f), style = s)
        val shackle = Path().apply {
            moveTo(w * 0.32f, h * 0.46f)
            lineTo(w * 0.32f, h * 0.32f)
            quadraticTo(w * 0.5f, h * 0.12f, w * 0.68f, h * 0.32f)
            lineTo(w * 0.68f, h * 0.46f)
        }
        drawPath(shackle, color, style = s)
    }
}

@Composable
fun EyeIcon(color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val s = Stroke(width = w * 0.1f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val lid = Path().apply {
            moveTo(w * 0.12f, h * 0.5f)
            quadraticTo(w * 0.5f, h * 0.18f, w * 0.88f, h * 0.5f)
            quadraticTo(w * 0.5f, h * 0.82f, w * 0.12f, h * 0.5f)
        }
        drawPath(lid, color, style = s)
        drawCircle(color, radius = w * 0.12f, center = Offset(w * 0.5f, h * 0.5f))
    }
}

@Composable
fun CameraIcon(color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val s = Stroke(width = w * 0.1f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawRoundRect(color, Offset(w * 0.14f, h * 0.28f), Size(w * 0.72f, h * 0.52f), CornerRadius(w * 0.12f), style = s)
        drawLine(color, Offset(w * 0.34f, h * 0.28f), Offset(w * 0.4f, h * 0.18f), strokeWidth = w * 0.1f, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.4f, h * 0.18f), Offset(w * 0.6f, h * 0.18f), strokeWidth = w * 0.1f, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.6f, h * 0.18f), Offset(w * 0.66f, h * 0.28f), strokeWidth = w * 0.1f, cap = StrokeCap.Round)
        drawCircle(color, radius = w * 0.15f, center = Offset(w * 0.5f, h * 0.54f), style = s)
    }
}

@Composable
fun StarIcon(filled: Boolean, color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val star = Path().apply {
            moveTo(w * 0.5f, h * 0.12f)
            lineTo(w * 0.61f, h * 0.37f)
            lineTo(w * 0.88f, h * 0.40f)
            lineTo(w * 0.68f, h * 0.59f)
            lineTo(w * 0.73f, h * 0.86f)
            lineTo(w * 0.5f, h * 0.72f)
            lineTo(w * 0.27f, h * 0.86f)
            lineTo(w * 0.32f, h * 0.59f)
            lineTo(w * 0.12f, h * 0.40f)
            lineTo(w * 0.39f, h * 0.37f)
            close()
        }
        if (filled) drawPath(star, color)
        else drawPath(star, color, style = Stroke(width = w * 0.09f, join = StrokeJoin.Round))
    }
}

@Composable
fun SparkleIcon(color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val main = Path().apply {
            moveTo(w * 0.46f, h * 0.12f)
            quadraticTo(w * 0.50f, h * 0.44f, w * 0.82f, h * 0.48f)
            quadraticTo(w * 0.50f, h * 0.52f, w * 0.46f, h * 0.84f)
            quadraticTo(w * 0.42f, h * 0.52f, w * 0.10f, h * 0.48f)
            quadraticTo(w * 0.42f, h * 0.44f, w * 0.46f, h * 0.12f)
            close()
        }
        drawPath(main, color)
        drawCircle(color, radius = w * 0.07f, center = Offset(w * 0.78f, h * 0.22f))
    }
}

@Composable
fun EditIcon(color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val s = Stroke(width = w * 0.11f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val pencil = Path().apply {
            moveTo(w * 0.22f, h * 0.78f)
            lineTo(w * 0.26f, h * 0.58f)
            lineTo(w * 0.68f, h * 0.16f)
            lineTo(w * 0.84f, h * 0.32f)
            lineTo(w * 0.42f, h * 0.74f)
            close()
        }
        drawPath(pencil, color, style = s)
    }
}

@Composable
fun SendIcon(color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val plane = Path().apply {
            moveTo(w * 0.16f, h * 0.20f)
            lineTo(w * 0.88f, h * 0.50f)
            lineTo(w * 0.16f, h * 0.80f)
            lineTo(w * 0.30f, h * 0.52f)
            lineTo(w * 0.62f, h * 0.50f)
            lineTo(w * 0.30f, h * 0.48f)
            close()
        }
        drawPath(plane, color)
    }
}

@Composable
fun CloseIcon(color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.13f
        drawLine(color, Offset(w * 0.26f, h * 0.26f), Offset(w * 0.74f, h * 0.74f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.74f, h * 0.26f), Offset(w * 0.26f, h * 0.74f), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

@Composable
fun PhoneIcon(color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val s = Stroke(width = w * 0.11f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val handset = Path().apply {
            moveTo(w * 0.26f, h * 0.18f)
            lineTo(w * 0.40f, h * 0.18f)
            lineTo(w * 0.46f, h * 0.36f)
            lineTo(w * 0.36f, h * 0.44f)
            quadraticTo(w * 0.42f, h * 0.58f, w * 0.56f, h * 0.64f)
            lineTo(w * 0.64f, h * 0.54f)
            lineTo(w * 0.82f, h * 0.60f)
            lineTo(w * 0.82f, h * 0.74f)
            quadraticTo(w * 0.80f, h * 0.86f, w * 0.64f, h * 0.84f)
            quadraticTo(w * 0.28f, h * 0.78f, w * 0.16f, h * 0.36f)
            quadraticTo(w * 0.14f, h * 0.20f, w * 0.26f, h * 0.18f)
            close()
        }
        drawPath(handset, color, style = s)
    }
}



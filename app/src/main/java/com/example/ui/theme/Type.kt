package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val DisplayFamily = FontFamily.SansSerif
val BodyFamily = FontFamily.SansSerif

val CampusVaultTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Black,
        fontSize = 26.sp,
        letterSpacing = (-0.6).sp,
        color = TextPrimary
    ),
    headlineSmall = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 20.sp,
        letterSpacing = (-0.4).sp,
        color = TextPrimary
    ),
    titleMedium = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        letterSpacing = (-0.2).sp,
        color = TextPrimary
    ),
    bodyMedium = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = TextSecondary
    ),
    labelSmall = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        letterSpacing = 0.2.sp
    )
).let { t ->
    t.copy(
        bodyLarge = t.bodyLarge.copy(fontFamily = BodyFamily),
        bodySmall = t.bodySmall.copy(fontFamily = BodyFamily),
        labelLarge = t.labelLarge.copy(fontFamily = BodyFamily),
        labelMedium = t.labelMedium.copy(fontFamily = BodyFamily),
        titleLarge = t.titleLarge.copy(fontFamily = DisplayFamily),
        titleSmall = t.titleSmall.copy(fontFamily = BodyFamily),
        headlineLarge = t.headlineLarge.copy(fontFamily = DisplayFamily),
        headlineMedium = t.headlineMedium.copy(fontFamily = DisplayFamily),
        displayLarge = t.displayLarge.copy(fontFamily = DisplayFamily),
        displayMedium = t.displayMedium.copy(fontFamily = DisplayFamily)
    )
}

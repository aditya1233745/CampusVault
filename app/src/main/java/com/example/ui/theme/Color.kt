package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val PureWhite = Color(0xFFFFFFFF)
val CanvasOffWhite = Color(0xFFF7F8FA)
val SurfaceSubtle = Color(0xFFF3F4F6)
val BorderLight = Color(0xFFE5E7EB)
val EmeraldGreen = Color(0xFF16A34A)
val EmeraldLight = Color(0xFFDCFCE7)
val SunsetOrange = Color(0xFFF97316)
val OrangeLight = Color(0xFFFFEDD5)
val SolarAmber = Color(0xFFFACC15)
val SkyBlue = Color(0xFF0EA5E9)
val SkyLight = Color(0xFFE0F2FE)
val VioletPop = Color(0xFF7C3AED)
val VioletLight = Color(0xFFEDE9FE)
val AlertCrimson = Color(0xFFDC2626)
val TextPrimary = Color(0xFF111827)
val TextSecondary = Color(0xFF6B7280)
val DarkNavy = Color(0xFF0F172A)

val GreenActionGradient = Brush.horizontalGradient(
    listOf(EmeraldGreen, Color(0xFF15803D))
)

val OrangeAccentGradient = Brush.horizontalGradient(
    listOf(SunsetOrange, Color(0xFFEA580C))
)

val VioletGradient = Brush.horizontalGradient(
    listOf(VioletPop, Color(0xFF6D28D9))
)

val BrandHeroGradient = Brush.verticalGradient(
    listOf(Color(0xFF0B4F27), Color(0xFF15803D), Color(0xFF22A456))
)

val ProfileGradient = Brush.linearGradient(
    listOf(VioletPop, SkyBlue)
)

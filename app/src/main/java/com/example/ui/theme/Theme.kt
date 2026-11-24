package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle

@Composable
fun CampusVaultTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = EmeraldGreen,
            secondary = SunsetOrange,
            tertiary = SkyBlue,
            background = CanvasOffWhite,
            surface = PureWhite,
            onPrimary = PureWhite,
            onSecondary = PureWhite,
            onBackground = TextPrimary,
            onSurface = TextPrimary
        ),
        typography = CampusVaultTypography,
        content = {
            ProvideTextStyle(TextStyle(fontFamily = BodyFamily)) {
                content()
            }
        }
    )
}

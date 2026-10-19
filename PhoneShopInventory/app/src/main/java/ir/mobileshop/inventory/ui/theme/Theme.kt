package ir.mobileshop.inventory.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary          = Accent500,
    onPrimary        = Neutral0,
    primaryContainer = Color(0xFFB2EDE3),
    onPrimaryContainer = Accent600,
    secondary        = Neutral700,
    onSecondary      = Neutral0,
    secondaryContainer = Neutral100,
    onSecondaryContainer = Neutral900,
    background       = Neutral50,
    onBackground     = Neutral900,
    surface          = Neutral0,
    onSurface        = Neutral900,
    surfaceVariant   = Neutral100,
    onSurfaceVariant = Neutral700,
    outline          = Neutral300,
    error            = Danger,
    onError          = Neutral0,
)

private val DarkColors = darkColorScheme(
    primary          = AccentOnDark,
    onPrimary        = Neutral900,
    primaryContainer = Accent600,
    onPrimaryContainer = Color(0xFFB2EDE3),
    secondary        = Neutral200,
    onSecondary      = Neutral900,
    secondaryContainer = Neutral700,
    onSecondaryContainer = Neutral100,
    background       = Color(0xFF141412),
    onBackground     = Neutral100,
    surface          = Color(0xFF1E1D1A),
    onSurface        = Neutral100,
    surfaceVariant   = Neutral700,
    onSurfaceVariant = Neutral200,
    outline          = Neutral500,
    error            = Color(0xFFFF6B6B),
    onError          = Neutral900,
)

@Composable
fun PhoneShopTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, typography = Typography, content = content)
}

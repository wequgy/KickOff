package com.example.kickoffwidget.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily as ComposeFontFamily
import androidx.compose.ui.text.font.FontWeight as ComposeFontWeight
import androidx.glance.text.FontFamily as GlanceFontFamily
import com.example.kickoffwidget.R

object WidgetTheme {
    val BricolageGrotesque = GlanceFontFamily.Monospace
    val HankenGrotesk = GlanceFontFamily.Monospace
    val JetBrainsMono = GlanceFontFamily.Monospace
}

object AppTheme {
    val BricolageGrotesque = ComposeFontFamily(
        Font(R.font.bricolage_grotesque_extrabold, ComposeFontWeight.W800)
    )
    val HankenGrotesk = ComposeFontFamily(
        Font(R.font.hanken_grotesk, ComposeFontWeight.W500),
        Font(R.font.hanken_grotesk, ComposeFontWeight.W700)
    )
    val JetBrainsMono = ComposeFontFamily(
        Font(R.font.jetbrains_mono_semibold, ComposeFontWeight.W600)
    )
}

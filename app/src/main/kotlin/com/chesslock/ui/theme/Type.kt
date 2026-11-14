package com.chesslock.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Typography tuned for one big number (the remaining time) and compact labels. The system font is
 * used on purpose: no font asset has to be bundled and every language renders correctly.
 */
internal val ChessLockTypography = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.copy(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = 56.sp,
            lineHeight = 60.sp,
            letterSpacing = (-1.5).sp,
        ),
        displayMedium = base.displayMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 44.sp,
            lineHeight = 48.sp,
            letterSpacing = (-1).sp,
        ),
        displaySmall = base.displaySmall.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 36.sp,
            lineHeight = 40.sp,
            letterSpacing = (-0.5).sp,
        ),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.Medium),
        labelMedium = base.labelMedium.copy(letterSpacing = 0.4.sp),
    )
}

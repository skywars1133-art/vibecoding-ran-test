package com.chesslock.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.chesslock.domain.budget.BudgetUrgency

/** The three colours one urgency level needs: card background, text and accent (ring, chip). */
data class UrgencyPalette(
    val container: Color,
    val content: Color,
    val accent: Color,
)

/**
 * Maps a [BudgetUrgency] to theme colours. Amber is used while time is running low and the error
 * palette once it is critical or gone, so the Home card and the block screen tell the same story.
 */
@Composable
@ReadOnlyComposable
fun urgencyPalette(urgency: BudgetUrgency): UrgencyPalette {
    val scheme = MaterialTheme.colorScheme
    return when (urgency) {
        BudgetUrgency.COMFORTABLE -> UrgencyPalette(
            container = scheme.primaryContainer,
            content = scheme.onPrimaryContainer,
            accent = scheme.primary,
        )

        BudgetUrgency.WARNING -> UrgencyPalette(
            container = scheme.tertiaryContainer,
            content = scheme.onTertiaryContainer,
            accent = scheme.tertiary,
        )

        BudgetUrgency.CRITICAL,
        BudgetUrgency.EMPTY,
        -> UrgencyPalette(
            container = scheme.errorContainer,
            content = scheme.onErrorContainer,
            accent = scheme.error,
        )
    }
}

package com.chesslock.data

import com.chesslock.domain.entitlements.Entitlements
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for premium features (SPEC 3b / PHASES Phase 4).
 *
 * No purchase is possible yet, so this always starts on [Entitlements.FREE]. When Play Billing is
 * added it only has to call [update] after a purchase or restore; every consumer already observes
 * [state], so no feature code changes then.
 */
@Singleton
class EntitlementsRepository @Inject constructor() {
    private val mutableState = MutableStateFlow(Entitlements.FREE)
    val state: StateFlow<Entitlements> = mutableState.asStateFlow()

    /** Publishes a new snapshot (future Billing hook; safe to call with [Entitlements.FREE]). */
    fun update(entitlements: Entitlements) {
        mutableState.value = entitlements
    }
}

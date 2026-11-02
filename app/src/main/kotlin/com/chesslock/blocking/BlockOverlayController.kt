package com.chesslock.blocking

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.chesslock.MainActivity
import com.chesslock.ui.blocking.BlockScreen
import com.chesslock.ui.theme.ChessLockTheme
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Adds and removes the full-screen Compose block screen on the overlay window (SPEC 1). */
@Singleton
class BlockOverlayController @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private var overlay: ComposeView? = null
    private var owner: OverlayOwner? = null
    @Volatile private var showing = false

    fun canShow(): Boolean = BlockingPermissions.canDrawOverlays(context)

    fun show() {
        if (showing) return
        showing = true
        mainHandler.post {
            if (!showing || overlay != null) return@post
            if (!canShow()) {
                showing = false
                return@post
            }

            val lifecycleOwner = OverlayOwner().also { it.create() }
            val view = ComposeView(context).apply {
                setViewTreeLifecycleOwner(lifecycleOwner)
                setViewTreeViewModelStoreOwner(lifecycleOwner)
                setViewTreeSavedStateRegistryOwner(lifecycleOwner)
                setContent {
                    ChessLockTheme {
                        BlockScreen(
                            onSolvePuzzle = {
                                openApp()
                                hide()
                            },
                            onGoHome = {
                                goHome()
                                hide()
                            },
                        )
                    }
                }
            }
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                title = "ChessLock block screen"
            }

            val added = runCatching { windowManager.addView(view, params) }.isSuccess
            if (added) {
                overlay = view
                owner = lifecycleOwner
            } else {
                lifecycleOwner.destroy()
                showing = false
            }
        }
    }

    fun hide() {
        if (!showing && overlay == null) return
        showing = false
        mainHandler.post {
            val view = overlay ?: return@post
            overlay = null
            runCatching { windowManager.removeView(view) }
            owner?.destroy()
            owner = null
        }
    }

    private fun openApp() {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        runCatching { context.startActivity(intent) }
    }

    private fun goHome() {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    /** Minimal owners so Compose can run inside a service-owned [ComposeView]. */
    private class OverlayOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)
        private val savedStateController by lazy { SavedStateRegistryController.create(this) }

        override val lifecycle: Lifecycle get() = lifecycleRegistry
        override val viewModelStore: ViewModelStore = ViewModelStore()
        override val savedStateRegistry: SavedStateRegistry
            get() = savedStateController.savedStateRegistry

        fun create() {
            savedStateController.performRestore(null)
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        }

        fun destroy() {
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
            viewModelStore.clear()
        }
    }
}

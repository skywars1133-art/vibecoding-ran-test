package com.chesslock.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.chesslock.R
import com.chesslock.ui.puzzle.PuzzleMoveStatus

/** The four puzzle feedback moments that have a bundled sound. */
enum class SoundCue {
    MOVE,
    WRONG,
    SOLVED,
    HINT,
}

/** Pure mapping from puzzle state to a sound cue, so the wiring can be unit-tested. */
object SoundCues {
    fun forMove(status: PuzzleMoveStatus): SoundCue? = when (status) {
        PuzzleMoveStatus.WRONG -> SoundCue.WRONG
        PuzzleMoveStatus.CORRECT -> SoundCue.MOVE
        PuzzleMoveStatus.SOLVED -> SoundCue.SOLVED
        PuzzleMoveStatus.NONE -> null
    }
}

/**
 * Low-latency player for the bundled CC0 feedback clips. Sonification usage keeps these short ticks
 * under the device's notification/DND policy instead of treating them as music.
 */
class GameSounds(context: Context) {
    private val pool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val sampleIds: Map<SoundCue, Int> = mapOf(
        SoundCue.MOVE to pool.load(context.applicationContext, R.raw.sfx_move, 1),
        SoundCue.WRONG to pool.load(context.applicationContext, R.raw.sfx_wrong, 1),
        SoundCue.SOLVED to pool.load(context.applicationContext, R.raw.sfx_solved, 1),
        SoundCue.HINT to pool.load(context.applicationContext, R.raw.sfx_hint, 1),
    )

    fun play(cue: SoundCue) {
        val id = sampleIds[cue] ?: return
        // A load is asynchronous; SoundPool ignores play() until the sample is ready.
        pool.play(id, VOLUME, VOLUME, 1, 0, 1f)
    }

    fun release() {
        pool.release()
    }

    private companion object {
        const val VOLUME = 0.85f
    }
}

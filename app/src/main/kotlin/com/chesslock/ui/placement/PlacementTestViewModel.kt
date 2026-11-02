package com.chesslock.ui.placement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chesslock.chess.ChessPiece
import com.chesslock.chess.ChessPosition
import com.chesslock.chess.ChesslibMateChecker
import com.chesslock.data.PlayerStore
import com.chesslock.data.PuzzleRepository
import com.chesslock.domain.config.AppConfig
import com.chesslock.domain.elo.PlacementTest
import com.chesslock.domain.puzzle.MoveResult
import com.chesslock.domain.puzzle.PuzzleProgress
import com.chesslock.domain.puzzle.PuzzleSession
import com.chesslock.domain.puzzle.WrongMoveOutcome
import com.chesslock.ui.puzzle.PuzzleMoveStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import javax.inject.Inject

data class PlacementUiState(
    val isLoading: Boolean = true,
    val loadError: Boolean = false,
    val puzzleId: String = "",
    val puzzleRating: Int = 0,
    val playerIsWhite: Boolean = true,
    val pieces: List<ChessPiece> = emptyList(),
    val legalMoves: List<String> = emptyList(),
    val lastMove: String? = null,
    val livesLeft: Int = 0,
    val answered: Int = 0,
    val total: Int = 0,
    val moveStatus: PuzzleMoveStatus = PuzzleMoveStatus.NONE,
    val puzzleSolved: Boolean = false,
    val puzzleFailed: Boolean = false,
    val testComplete: Boolean = false,
    val showResult: Boolean = false,
    val endedEarly: Boolean = false,
    val finalElo: Int = 0,
    val saving: Boolean = false,
    val saveError: Boolean = false,
)

/**
 * Onboarding placement test (SPEC 4). Plays [AppConfig.placementPuzzleCount] adaptive puzzles from the
 * built-in stock. Rewards and ELO changes are deliberately not recorded, puzzles are not marked solved,
 * and each puzzle is used at most once per run.
 */
@HiltViewModel
class PlacementTestViewModel @Inject constructor(
    private val repository: PuzzleRepository,
    private val playerStore: PlayerStore,
    private val config: AppConfig,
) : ViewModel() {
    private val test = PlacementTest(config)
    private val usedPuzzleIds = mutableSetOf<String>()

    private val mutableState = MutableStateFlow(
        PlacementUiState(total = config.placementPuzzleCount),
    )
    val state: StateFlow<PlacementUiState> = mutableState.asStateFlow()

    private var position: ChessPosition? = null
    private var progress: PuzzleProgress? = null
    private var session: PuzzleSession? = null
    private var moveInProgress = false

    init {
        loadNextPuzzle()
    }

    fun loadNextPuzzle() {
        if (test.isComplete) {
            showResult(endedEarly = false)
            return
        }
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(isLoading = true, loadError = false)
            try {
                val target = test.targetRating
                    .roundToInt()
                    .coerceIn(config.eloFloor, config.placementMaxRating)
                val puzzle = repository.nextPuzzle(target, usedPuzzleIds)
                if (puzzle == null) {
                    showResult(endedEarly = true)
                    return@launch
                }
                usedPuzzleIds += puzzle.id
                val nextPosition = ChessPosition(puzzle.fen)
                val nextProgress = PuzzleProgress(puzzle, ChesslibMateChecker(puzzle.fen))
                val nextSession = PuzzleSession(puzzle.rating, config)
                val openingMove = nextProgress.start()
                check(nextPosition.play(openingMove)) { "Puzzle ${puzzle.id} has an illegal opening move" }

                position = nextPosition
                progress = nextProgress
                session = nextSession
                mutableState.value = PlacementUiState(
                    isLoading = false,
                    puzzleId = puzzle.id,
                    puzzleRating = puzzle.rating,
                    playerIsWhite = puzzle.playerIsWhite,
                    pieces = nextPosition.pieces,
                    legalMoves = nextPosition.legalMoves,
                    lastMove = openingMove,
                    livesLeft = nextSession.livesLeft,
                    answered = test.answered,
                    total = config.placementPuzzleCount,
                    finalElo = test.estimatedElo(),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutableState.value = mutableState.value.copy(isLoading = false, loadError = true)
            }
        }
    }

    fun onMove(uci: String) {
        val currentPosition = position ?: return
        val currentProgress = progress ?: return
        val currentSession = session ?: return
        val current = mutableState.value
        if (moveInProgress || current.isLoading || current.showResult ||
            current.puzzleSolved || current.puzzleFailed
        ) {
            return
        }
        if (uci !in currentPosition.legalMoves) return

        moveInProgress = true
        viewModelScope.launch {
            try {
                when (val result = currentProgress.submit(uci)) {
                    MoveResult.Wrong -> {
                        val outcome = currentSession.wrongMove()
                        if (outcome is WrongMoveOutcome.LifeLost && outcome.livesLeft == 0) {
                            test.record(solved = false)
                            mutableState.value = mutableState.value.copy(
                                livesLeft = currentSession.livesLeft,
                                moveStatus = PuzzleMoveStatus.NONE,
                                puzzleFailed = true,
                                answered = test.answered,
                                testComplete = test.isComplete,
                                finalElo = test.estimatedElo(),
                            )
                        } else {
                            mutableState.value = mutableState.value.copy(
                                livesLeft = currentSession.livesLeft,
                                moveStatus = PuzzleMoveStatus.WRONG,
                            )
                        }
                    }

                    is MoveResult.Correct -> {
                        check(currentPosition.play(uci)) {
                            "Accepted move is not legal on the displayed board"
                        }
                        result.reply?.let { reply ->
                            check(currentPosition.play(reply)) {
                                "Puzzle reply is not legal on the displayed board"
                            }
                        }
                        currentSession.correctMove()
                        if (result.solved) {
                            test.record(solved = true)
                            mutableState.value = mutableState.value.copy(
                                pieces = currentPosition.pieces,
                                legalMoves = emptyList(),
                                lastMove = currentProgress.movesPlayed.lastOrNull(),
                                livesLeft = currentSession.livesLeft,
                                moveStatus = PuzzleMoveStatus.SOLVED,
                                puzzleSolved = true,
                                answered = test.answered,
                                testComplete = test.isComplete,
                                finalElo = test.estimatedElo(),
                            )
                        } else {
                            mutableState.value = mutableState.value.copy(
                                pieces = currentPosition.pieces,
                                legalMoves = currentPosition.legalMoves,
                                lastMove = currentProgress.movesPlayed.lastOrNull(),
                                moveStatus = PuzzleMoveStatus.CORRECT,
                            )
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutableState.value = mutableState.value.copy(loadError = true)
            } finally {
                moveInProgress = false
            }
        }
    }

    /** Called by the Continue / See result button after the current puzzle is decided. */
    fun advance() {
        if (mutableState.value.showResult) return
        if (test.isComplete) {
            showResult(endedEarly = false)
        } else {
            loadNextPuzzle()
        }
    }

    fun retry() = loadNextPuzzle()

    /** Stores the estimated starting ELO; the main screen then switches to normal play. */
    fun confirmResult() {
        val current = mutableState.value
        if (current.saving) return
        mutableState.value = current.copy(saving = true, saveError = false)
        viewModelScope.launch {
            try {
                playerStore.completePlacement(current.finalElo)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutableState.value = mutableState.value.copy(saving = false, saveError = true)
            }
        }
    }

    private fun showResult(endedEarly: Boolean) {
        position = null
        progress = null
        session = null
        mutableState.value = mutableState.value.copy(
            isLoading = false,
            loadError = false,
            legalMoves = emptyList(),
            showResult = true,
            endedEarly = endedEarly,
            testComplete = test.isComplete || endedEarly,
            finalElo = test.estimatedElo(),
        )
    }
}

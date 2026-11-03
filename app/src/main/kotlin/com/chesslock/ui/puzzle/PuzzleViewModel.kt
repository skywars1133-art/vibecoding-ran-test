package com.chesslock.ui.puzzle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chesslock.chess.ChessPiece
import com.chesslock.chess.ChessPosition
import com.chesslock.chess.ChesslibMateChecker
import com.chesslock.data.PlayerStore
import com.chesslock.data.PuzzleRepository
import com.chesslock.data.BudgetStore
import com.chesslock.data.SettingsStore
import com.chesslock.data.StatisticsStore
import com.chesslock.domain.config.AppConfig
import com.chesslock.domain.elo.EloResult
import com.chesslock.domain.puzzle.HintAvailability
import com.chesslock.domain.puzzle.MoveResult
import com.chesslock.domain.puzzle.PuzzleProgress
import com.chesslock.domain.puzzle.PuzzleSession
import com.chesslock.domain.puzzle.SkipKind
import com.chesslock.domain.puzzle.SkipPolicy
import com.chesslock.domain.puzzle.WrongMoveOutcome
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PuzzleMoveStatus {
    NONE,
    WRONG,
    CORRECT,
    SOLVED,
}

data class PuzzleUiState(
    val isLoading: Boolean = true,
    val loadError: Boolean = false,
    val stockEmpty: Boolean = false,
    val puzzleId: String = "",
    val puzzleRating: Int = 0,
    val playerElo: Int = 0,
    val eloDelta: Int? = null,
    val playerIsWhite: Boolean = true,
    val pieces: List<ChessPiece> = emptyList(),
    val legalMoves: List<String> = emptyList(),
    val lastMove: String? = null,
    val livesLeft: Int = 0,
    val rewardSeconds: Long = 0,
    val elapsedSeconds: Long = 0,
    val freeSkipsUsedToday: Int = 0,
    val canSkip: Boolean = false,
    val moveStatus: PuzzleMoveStatus = PuzzleMoveStatus.NONE,
    val solved: Boolean = false,
    val hintsUsedToday: Int = 0,
    val hintLevel: Int = 0,
    val hintSourceSquare: String? = null,
    val hintTargetSquare: String? = null,
    val hintAvailability: HintAvailability = HintAvailability.AVAILABLE,
)

@HiltViewModel
class PuzzleViewModel @Inject constructor(
    private val repository: PuzzleRepository,
    private val playerStore: PlayerStore,
    private val budgetStore: BudgetStore,
    private val settingsStore: SettingsStore,
    private val statisticsStore: StatisticsStore,
    private val config: AppConfig,
) : ViewModel() {
    private val mutableState = MutableStateFlow(PuzzleUiState(playerElo = config.eloStart))
    val state: StateFlow<PuzzleUiState> = mutableState.asStateFlow()

    private var position: ChessPosition? = null
    private var progress: PuzzleProgress? = null
    private var session: PuzzleSession? = null
    private var moveInProgress = false
    private var timerJob: Job? = null

    init {
        loadPuzzle()
    }

    fun loadPuzzle() {
        if (mutableState.value.isLoading && mutableState.value.puzzleId.isNotEmpty()) return
        viewModelScope.launch {
            timerJob?.cancel()
            mutableState.value = PuzzleUiState(isLoading = true, playerElo = playerStore.currentState().elo)
            try {
                val player = playerStore.currentState()
                val puzzle = repository.nextPuzzle(player.elo)
                if (puzzle == null) {
                    mutableState.value = PuzzleUiState(stockEmpty = true, isLoading = false, playerElo = player.elo)
                    return@launch
                }
                val nextPosition = ChessPosition(puzzle.fen)
                val nextProgress = PuzzleProgress(puzzle, ChesslibMateChecker(puzzle.fen))
                val nextSession = PuzzleSession(puzzle.rating, config)
                val openingMove = nextProgress.start()
                check(nextPosition.play(openingMove)) { "Puzzle ${puzzle.id} has an illegal opening move" }

                position = nextPosition
                progress = nextProgress
                session = nextSession
                mutableState.value = PuzzleUiState(
                    isLoading = false,
                    puzzleId = puzzle.id,
                    puzzleRating = puzzle.rating,
                    playerElo = player.elo,
                    playerIsWhite = puzzle.playerIsWhite,
                    pieces = nextPosition.pieces,
                    legalMoves = nextPosition.legalMoves,
                    lastMove = openingMove,
                    livesLeft = nextSession.livesLeft,
                    rewardSeconds = nextSession.currentRewardSeconds(),
                    freeSkipsUsedToday = player.freeSkipsUsedToday,
                    canSkip = SkipPolicy.availableSkip(player.freeSkipsUsedToday, 0, config) != null,
                    hintsUsedToday = player.hintsUsedToday,
                    hintAvailability = nextSession.hintAvailability(player.hintsUsedToday),
                )
                startTimer(puzzle.id)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutableState.value = PuzzleUiState(
                    isLoading = false,
                    loadError = true,
                    playerElo = playerStore.currentState().elo,
                )
            }
        }
    }

    private fun startTimer(puzzleId: String) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(config.puzzleTimerTickSeconds * 1000)
                val current = mutableState.value
                if (current.puzzleId != puzzleId || current.solved || current.loadError) break
                val elapsed = current.elapsedSeconds + config.puzzleTimerTickSeconds
                mutableState.value = current.copy(
                    elapsedSeconds = elapsed,
                    canSkip = SkipPolicy.availableSkip(current.freeSkipsUsedToday, elapsed, config) != null,
                )
            }
        }
    }

    fun skipPuzzle() {
        val currentSession = session ?: return
        val currentPuzzle = progress?.puzzle
        val current = mutableState.value
        if (moveInProgress || current.isLoading || current.solved) return
        val skipKind = SkipPolicy.availableSkip(
            current.freeSkipsUsedToday,
            current.elapsedSeconds,
            config,
        ) ?: return

        moveInProgress = true
        viewModelScope.launch {
            try {
                // A puzzle whose LOSS was already recorded keeps that attempt; do not log a skip
                // on top of it (it would show up twice in the history).
                if (currentPuzzle != null && currentSession.eloResult != EloResult.LOSS) {
                    runCatching {
                        statisticsStore.recordSkip(currentPuzzle, current.playerElo)
                    }
                }
                currentSession.skip()
                if (skipKind == SkipKind.FREE) playerStore.consumeFreeSkip()
                loadPuzzle()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutableState.value = mutableState.value.copy(loadError = true)
            } finally {
                moveInProgress = false
            }
        }
    }

    /**
     * Grants a hint ONLY after the rewarded ad reported that the reward was earned (SPEC 3b). The
     * caller must not grant anything when the ad fails, is skipped or the player closes it early.
     */
    fun onAdRewardEarned() {
        val currentSession = session ?: return
        val currentProgress = progress ?: return
        val current = mutableState.value
        if (current.isLoading || current.solved) return
        if (!currentSession.canRequestHint(current.hintsUsedToday)) return

        viewModelScope.launch {
            try {
                val hintsUsedToday = playerStore.consumeHint()
                val level = currentSession.grantHint()
                val highlight = HintHighlight.forMove(currentProgress.expectedMove, level)
                mutableState.value = mutableState.value.copy(
                    hintsUsedToday = hintsUsedToday,
                    hintLevel = level,
                    hintSourceSquare = highlight?.sourceSquare,
                    hintTargetSquare = highlight?.targetSquare,
                    hintAvailability = currentSession.hintAvailability(hintsUsedToday),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // The ad was already watched; keep the puzzle usable even if persistence fails.
            }
        }
    }

    fun onMove(uci: String) {
        val currentPosition = position ?: return
        val currentProgress = progress ?: return
        val currentSession = session ?: return
        if (moveInProgress || mutableState.value.isLoading || mutableState.value.solved) return
        if (uci !in currentPosition.legalMoves) return

        moveInProgress = true
        viewModelScope.launch {
            try {
                when (val result = currentProgress.submit(uci)) {
                MoveResult.Wrong -> {
                    val outcome = currentSession.wrongMove()
                    var elo = mutableState.value.playerElo
                    var eloDelta = mutableState.value.eloDelta
                    if (outcome is WrongMoveOutcome.LifeLost && outcome.eloLossApplied) {
                        val update = playerStore.recordResult(currentSession.puzzleRating, EloResult.LOSS)
                        elo = update.newElo
                        eloDelta = update.delta
                        // The third lost life is when ELO changes, so the attempt is logged here.
                        runCatching {
                            statisticsStore.recordLoss(
                                puzzle = currentProgress.puzzle,
                                hintsUsed = currentSession.hintsUsed,
                                eloBefore = update.oldElo,
                                eloAfter = update.newElo,
                            )
                        }
                    }
                    mutableState.value = mutableState.value.copy(
                        livesLeft = currentSession.livesLeft,
                        rewardSeconds = currentSession.currentRewardSeconds(),
                        playerElo = elo,
                        eloDelta = eloDelta,
                        moveStatus = PuzzleMoveStatus.WRONG,
                    )
                }

                is MoveResult.Correct -> {
                    check(currentPosition.play(uci)) { "Accepted move is not legal on the displayed board" }
                    result.reply?.let { reply ->
                        check(currentPosition.play(reply)) { "Puzzle reply is not legal on the displayed board" }
                    }
                    currentSession.correctMove()
                    var elo = mutableState.value.playerElo
                    var eloDelta = mutableState.value.eloDelta
                    var reward = currentSession.currentRewardSeconds()
                    if (result.solved) {
                        val outcome = currentSession.solved()
                        // When the LOSS was already applied, the attempt row is completed instead
                        // of adding a second entry with a second ELO change.
                        var statsEloBefore = elo
                        var statsEloAfter = elo
                        if (!outcome.eloAlreadyApplied) {
                            val update = playerStore.recordResult(currentSession.puzzleRating, outcome.eloResult)
                            elo = update.newElo
                            eloDelta = update.delta
                            statsEloBefore = update.oldElo
                            statsEloAfter = update.newElo
                        }
                        reward = outcome.rewardSeconds
                        repository.markSolved(currentProgress.puzzle.id)
                        if (outcome.rewardSeconds > 0L) {
                            val settings = settingsStore.settings.first()
                            budgetStore.addEarned(
                                seconds = outcome.rewardSeconds,
                                limitSeconds = settings.dailyLimitSeconds,
                                capSeconds = settings.dailyCapSeconds,
                            )
                        }
                        runCatching {
                            if (outcome.eloAlreadyApplied) {
                                val completed = statisticsStore.completeLoss(
                                    puzzleId = currentProgress.puzzle.id,
                                    rewardSeconds = outcome.rewardSeconds,
                                    hintsUsed = currentSession.hintsUsed,
                                )
                                if (!completed) {
                                    statisticsStore.recordSolved(
                                        puzzle = currentProgress.puzzle,
                                        hintsUsed = currentSession.hintsUsed,
                                        eloBefore = statsEloBefore,
                                        eloAfter = statsEloAfter,
                                        rewardSeconds = outcome.rewardSeconds,
                                    )
                                }
                            } else {
                                statisticsStore.recordSolved(
                                    puzzle = currentProgress.puzzle,
                                    hintsUsed = currentSession.hintsUsed,
                                    eloBefore = statsEloBefore,
                                    eloAfter = statsEloAfter,
                                    rewardSeconds = outcome.rewardSeconds,
                                )
                            }
                        }
                    }
                    mutableState.value = mutableState.value.copy(
                        pieces = currentPosition.pieces,
                        legalMoves = if (result.solved) emptyList() else currentPosition.legalMoves,
                        lastMove = currentProgress.movesPlayed.lastOrNull(),
                        livesLeft = currentSession.livesLeft,
                        rewardSeconds = reward,
                        playerElo = elo,
                        eloDelta = eloDelta,
                        moveStatus = if (result.solved) PuzzleMoveStatus.SOLVED else PuzzleMoveStatus.CORRECT,
                        solved = result.solved,
                        // The hint level resets after every correct move (SPEC 3b).
                        hintLevel = currentSession.currentHintLevel,
                        hintSourceSquare = null,
                        hintTargetSquare = null,
                        hintAvailability = currentSession.hintAvailability(
                            mutableState.value.hintsUsedToday,
                        ),
                    )
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
}

# ChessLock

Android app: block distracting apps on a daily time budget and earn extra time by solving chess puzzles. Puzzles change your ELO; hints cost a rewarded ad.

- Product spec: [SPEC.md](SPEC.md)
- Delivery plan: [PHASES.md](PHASES.md)
- Instructions for Claude Code: [CLAUDE.md](CLAUDE.md)

## Getting started
Open this folder in Claude Code and say: "Read CLAUDE.md, SPEC.md and PHASES.md, then send me your plan."

If a session runs out of credit, `CHECKPOINT.md` says where to resume.

## Build and test
Requirements: JDK 17+, Android Studio (or the Android SDK).

```
./gradlew :domain:test          # unit tests for all rules (ELO, rewards, lives, hints, skips, budget)
./gradlew :app:testDebugUnitTest # chess rules and mating alternatives
./gradlew :app:assembleDebug    # Android app (needs the Android SDK; set sdk.dir in local.properties)
```

No network or Android SDK (for example a locked-down sandbox)? The pure-Kotlin domain tests still run with the
Kotlin compiler and JUnit bundled in the Gradle distribution:

```
scripts/test-domain.sh
```

### Built-in puzzle set
The committed `app/src/main/assets/builtin_puzzles.csv` contains 50 real Lichess puzzles (CC0). To regenerate it on a machine with internet:

```
curl -O https://database.lichess.org/lichess_db_puzzle.csv.zst
zstdcat lichess_db_puzzle.csv.zst | python3 scripts/build-builtin-puzzles.py > app/src/main/assets/builtin_puzzles.csv
```

It picks 5 puzzles per 200-point rating band from 600 to 2600 (most reliable, most popular first) and prints a
warning if a band has fewer. Only the best candidates per band are retained while streaming, so memory use stays bounded.
Commit the resulting file after validating it.

## Architecture
- `domain/`: pure Kotlin/JVM, no Android imports. `config/AppConfig` (the ONE place for numeric constants), `elo`,
  `reward`, `puzzle` (`Puzzle` + Lichess CSV parser, `PuzzleProgress` solution tracking, `PuzzleSession` lives/hints,
  `SkipPolicy`), `budget` (daily time budget, midnight reset).
- `app/`: Android app (Compose, Material 3, Hilt). `di/ConfigModule` provides `AppConfig`; `data/` contains the Room
  puzzle stock and DataStore player progress; `chess/` adapts chesslib for legal UCI moves and mating alternatives;
  `ui/puzzle/` contains the tap-to-move board, promotion picker, timer, skip controls and puzzle ViewModel.

## Decisions
- **Domain module is plain Kotlin/JVM.** Rules are testable without an emulator and stay independent of Android.
- **Reward order:** gross (base + difficulty bonus) -> hint multiplier (x0.5 if any hint used) -> minus 20 s per wrong
  move after all lives are lost -> never below 0. The flat penalty is not halved.
- **ELO K-factor counts every puzzle that has an ELO result**, including hint wins (delta 0) and losses.
  K = 40 while fewer than 20 such puzzles have been played, else 20. Rounded to nearest integer, floor 400.
- **ELO loss is applied when the third life is lost**; solving afterwards pays the reduced reward but does not
  touch ELO again (`SolveOutcome.eloAlreadyApplied`). Skipping after the loss keeps that loss.
- **Skip kinds:** the 20-minute skip is preferred over the free skip when both are possible, so the free skip is kept.
- **Midnight reset:** budget rolls over only when the local date moves *forward*. Moving the device clock back does
  not refill the budget. Earned time is capped by the optional daily cap, and lowering the cap never removes
  time already earned.
- **Chess rules sit behind a seam.** `PuzzleProgress` only compares UCI strings; whether an alternative move delivers
  mate comes from an injected `MateChecker`, implemented by `ChesslibMateChecker` in the app module. `ChessPosition`
  exposes only legal UCI moves to the board. Lichess accepts any mating move, so the first mating alternative ends the puzzle.
- **Puzzle validation:** a puzzle needs an even number of moves, at least 2 (opponent's automatic first move, then
  player/opponent pairs ending on the player's move). Malformed CSV rows are skipped and reported, never fatal.
- **No invented puzzle data.** The built-in set is generated from the real dump by `scripts/build-builtin-puzzles.py`.
- **Placement test is a staircase:** the target puzzle rating starts at the default ELO, goes up after a solve and
  down after a failure, with steps 300, 225, 169, ... shrinking to a 75 minimum. The estimate is the target the next
  puzzle would have had (clamped to 400..2800). No hints or skips during placement; a skip counts as a failure.
- **Next-puzzle choice:** desired rating = ELO + Gaussian offset (sigma 100), then a random pick among the 3 closest
  unsolved puzzles. Returns "no puzzle" only when the whole stock is solved.
- **Starting ELO before the placement test:** 1000 (`AppConfig.eloStart`).
- **Min SDK 26** (java.time without desugaring, notification channels).
- **Dependency versions** in `gradle/libs.versions.toml` were picked as a compatible set (AGP 8.7.3, Kotlin 2.0.21,
  KSP 2.0.21-1.0.28, Hilt 2.52, Compose BOM 2024.10.01) but could not be resolved in the authoring sandbox. Bump them
  if Gradle complains.
- **Status of the Android module:** the skeleton (Hilt app, theme, sk/en strings, config injection) is written but has
  not been compiled yet; only `:domain` has been built and tested so far.

## Licenses
| Item | License | Use |
| --- | --- | --- |
| Lichess puzzle database | CC0 | Puzzle data (Phase 1 built-in set, Phase 2 packs) |
| AndroidX, Jetpack Compose, Material 3 | Apache-2.0 | UI |
| Hilt / Dagger | Apache-2.0 | DI |
| Kotlin, KSP | Apache-2.0 | Language / build |
| JUnit 4 | EPL-1.0 | Tests only (not shipped) |
| chesslib | Apache-2.0 | Legal moves, FEN and checkmate detection |
| System Unicode chess glyphs | No separate asset | Board pieces rendered as text; no bundled piece graphics |

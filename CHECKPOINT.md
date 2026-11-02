# CHECKPOINT

Resume file. Update after every meaningful step and before any likely credit cutoff. A new session reads this first, then CLAUDE.md, SPEC.md, PHASES.md and the README "Decisions".

## Status
- Date: 2026-11-02
- Current phase: Phase 1 (MVP), in progress
- Plan: **approved by the user** (2026-11-02)
- Last completed: Phase 1 tasks 1, 2 and 3 (skeleton, domain rules, puzzle model/parser/progress). Task 3 is partial: built-in puzzle FILE not generated yet (needs network)

## Environment notes (important for the next session)
- The authoring sandbox cannot reach Maven Central, Google Maven or the Gradle plugin portal, and has no Android SDK.
  So `:app` cannot be compiled there. Only `:domain` is verified, via `scripts/test-domain.sh`
  (uses the Kotlin compiler and JUnit bundled in Gradle's lib folder). Last run: **79 tests, all passing**.
- Pushing needs the Claude GitHub App installed on the repo owner (already done; push works).
- No pip/npm/Maven/Lichess access in the sandbox: cannot fetch real puzzles or install chesslib/python-chess. Do NOT invent puzzle data.
- Gradle wrapper was generated offline (8.14.3).
- Dependency versions in `gradle/libs.versions.toml` are unresolved/unverified.

## Plan (approved)

### Stack
- Kotlin + Jetpack Compose + Material 3, MVVM / clean architecture.
- Room (puzzles, attempts, usage), DataStore (settings), WorkManager (daily pack, midnight reset), Hilt (DI).
- Chess rules: `bhlangonijr/chesslib` (Apache-2.0). Board is a custom Compose component.
- AdMob rewarded ads + UMP consent. Test IDs in debug only.
- Backend (Phase 3): Firebase Auth + Firestore, append-only attempt log for conflicts.
- Puzzle pipeline (Phase 2): GitHub Action processes the Lichess CC0 dump into rating-bucket files hosted statically.

### Main risks
- App blocking: UsageStats polling vs OEM battery kills; foreground service, boot receiver, WorkManager watchdog, "Protection is not active" status. Avoid AccessibilityService.
- Lichess pipeline: dump too large for in-app, so pre-processed buckets hosted statically.
- Ads: hint granted only in `onUserEarnedReward`, never a free fallback; offline disables Hint.
- Licenses: record every dependency, dataset and piece set in README.

### Phase 1 task order
1. [x] Gradle project, Hilt, `AppConfig`, string resources sk/en, theme (written, `:app` not yet compiled)
2. [x] Domain logic + unit tests: ELO, reward, lives/penalties, hints, skip rules, budget + midnight reset (56 tests pass)
3. [~] Puzzles: DONE `Puzzle` + `LichessPuzzleCsv` parser, `PuzzleProgress` (auto opponent moves, alt-mate via `MateChecker` seam), tests, `scripts/build-builtin-puzzles.py`.
   TODO (needs network on a dev machine): generate `app/src/main/assets/builtin_puzzles.csv` (see README), add chesslib to `:app`, implement `MateChecker` + legal-move validation + Room loader
4. [ ] Board component + puzzle screen
5. [ ] Placement test (adaptive, offline)
6. [ ] Blocking service, categories, block screen, boot receiver
7. [ ] Onboarding + permissions + protection status
8. [ ] Hints with AdMob test ads
9. [ ] README wrap-up for Phase 1, then STOP and wait for feedback

## Done log
- 2026-11-02: cloned repo, read all docs, wrote checkpoint, plan approved, pushed checkpoint.
- 2026-11-02: added `:domain` (AppConfig, EloCalculator, RewardCalculator, PuzzleSession, SkipPolicy, TimeBudget) with 56 passing tests; Gradle project, wrapper, Android skeleton, README build/decisions/licenses, `scripts/test-domain.sh`.

- 2026-11-02: added Puzzle model, Lichess CSV parser, PuzzleProgress, 23 more tests (79 total), built-in set generator script (tested on synthetic data only).

## Next step
Blocked on network for: generating the built-in puzzle file, adding chesslib, compiling `:app`. Still doable offline in `:domain`: task 5 placement-test logic (adaptive difficulty, ELO from results) and adaptive puzzle selection with unit tests. Do that next, then UI/blocking code in `:app` (written but unverifiable here).

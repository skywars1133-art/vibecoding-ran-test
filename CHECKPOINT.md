# CHECKPOINT

Resume file. Update after every meaningful step and before any likely credit cutoff. A new session reads this first, then CLAUDE.md, SPEC.md, PHASES.md and the README "Decisions".

## Status
- Date: 2026-11-02
- Current phase: Phase 1 (MVP), in progress
- Plan: **approved by the user** (2026-11-02)
- Last completed: Phase 1 tasks 3 and 4 (real built-in puzzle stock, app chess/storage integration, board and puzzle screen).

## Verification
- Ran `:domain:test :app:testDebugUnitTest :app:assembleDebug` with JDK 17.0.12, Gradle 8.14.3 and Android SDK 35.
- **98 domain tests + 8 app tests passed; 0 failures, errors or skips.**
- `:app:assembleDebug` produced a signed debug APK containing `assets/builtin_puzzles.csv`.
- Room exported `app/schemas/com.chesslock.data.ChessLockDatabase/1.json` without a schema-location warning.
- Not yet exercised on an emulator or physical device.

## Environment notes
- Dependencies resolve from Google Maven, Maven Central and JitPack (chesslib).
- The committed built-in set contains 50 real Lichess CC0 puzzles, five in each rating band from 600 through 2600. Every FEN and complete UCI sequence was validated with `python-chess`.
- The generator streams the large Lichess dump while retaining only the best candidates per rating band.
- Pushing needs the Claude GitHub App installed on the repo owner (already done; push works).
- Do NOT invent puzzle data.

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
1. [x] Gradle project, Hilt, `AppConfig`, string resources sk/en, theme.
2. [x] Domain logic + unit tests: ELO, reward, lives/penalties, hints, skip rules, budget + midnight reset.
3. [x] Puzzles: `Puzzle` + Lichess CSV parser, `PuzzleProgress`, chesslib legal moves and mating alternatives, Room stock and solved tracking, DataStore player progress, and the generated 50-puzzle built-in asset.
4. [x] Tap-to-move Compose board with legal targets, last-move highlighting, orientation and promotion picker; puzzle screen with lives, ELO, timer, rewards and both skip paths.
5. [~] Placement test: DONE logic `PlacementTest` (staircase) + `PuzzleSelector` with tests. TODO: UI flow using the built-in set.
6. [ ] Blocking service, categories, block screen, boot receiver
7. [ ] Onboarding + permissions + protection status
8. [ ] Hints with AdMob test ads
9. [ ] README wrap-up for Phase 1, then STOP and wait for feedback

## Done log
- 2026-11-02: cloned repo, read all docs, wrote checkpoint, plan approved, pushed checkpoint.
- 2026-11-02: added `:domain` (AppConfig, EloCalculator, RewardCalculator, PuzzleSession, SkipPolicy, TimeBudget) with 56 passing tests; Gradle project, wrapper, Android skeleton, README build/decisions/licenses, `scripts/test-domain.sh`.
- 2026-11-02: added Puzzle model, Lichess CSV parser, PuzzleProgress, 23 more tests (79 total), built-in set generator script (tested on synthetic data only).
- 2026-11-02: added PlacementTest (staircase) and PuzzleSelector, 19 more tests (98 total), new AppConfig constants.
- 2026-11-02: generated and validated the 50-puzzle Lichess asset; added bounded-memory generation, Room/DataStore, chesslib adapters, the Compose board and puzzle flow, sk/en resources, and 8 passing app tests. Full debug build verified.

## Next step
Implement the placement-test UI using the built-in set (task 5), then continue with blocking, onboarding/permissions and AdMob hints in the approved order. Puzzle rewards are currently calculated and displayed but are not yet credited to the blocking budget; connect them when the budget/blocking app layer is implemented. No Hint button is exposed before rewarded ads are implemented.

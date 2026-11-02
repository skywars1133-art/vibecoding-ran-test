# CHECKPOINT

Resume file. Update after every meaningful step and before any likely credit cutoff. A new session reads this first, then CLAUDE.md, SPEC.md, PHASES.md and the README "Decisions".

## Status
- Date: 2026-11-02
- Current phase: Phase 1 (MVP), in progress
- Plan: **approved by the user** (2026-11-02)
- Last completed: Phase 1 tasks 1 and 2 (skeleton + domain logic with tests)

## Environment notes (important for the next session)
- The authoring sandbox cannot reach Maven Central, Google Maven or the Gradle plugin portal, and has no Android SDK.
  So `:app` cannot be compiled there. Only `:domain` is verified, via `scripts/test-domain.sh`
  (uses the Kotlin compiler and JUnit bundled in Gradle's lib folder). Last run: **56 tests, all passing**.
- Pushing needs the Claude GitHub App installed on the repo owner (already done; push works).
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
3. [ ] Built-in set of ~50 puzzles + loader, move validation (alt mate accepted). Needs chesslib (cannot be downloaded in the sandbox; a puzzle-pack data file and parser can still be written and tested)
4. [ ] Board component + puzzle screen
5. [ ] Placement test (adaptive, offline)
6. [ ] Blocking service, categories, block screen, boot receiver
7. [ ] Onboarding + permissions + protection status
8. [ ] Hints with AdMob test ads
9. [ ] README wrap-up for Phase 1, then STOP and wait for feedback

## Done log
- 2026-11-02: cloned repo, read all docs, wrote checkpoint, plan approved, pushed checkpoint.
- 2026-11-02: added `:domain` (AppConfig, EloCalculator, RewardCalculator, PuzzleSession, SkipPolicy, TimeBudget) with 56 passing tests; Gradle project, wrapper, Android skeleton, README build/decisions/licenses, `scripts/test-domain.sh`.

## Next step
Phase 1 task 3: puzzle model + CSV parser for the Lichess format in `:domain` (pure Kotlin, testable offline), then a built-in puzzle set. Moves/legal-move checking needs chesslib, which can only be added and verified where Maven is reachable.

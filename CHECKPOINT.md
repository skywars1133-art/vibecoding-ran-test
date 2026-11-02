# CHECKPOINT

Resume file. Update after every meaningful step and before any likely credit cutoff. A new session reads this first, then CLAUDE.md, SPEC.md, PHASES.md and the README "Decisions".

## Status
- Date: 2026-11-02
- Current phase: Phase 1 (MVP), in progress
- Plan: **approved by the user** (2026-11-02)
- Last completed: Phase 1 task 9 — README wrap-up (build/test and run-on-device instructions, architecture, decisions and the license table including AdMob).

## Verification
- Ran `:domain:test :app:testDebugUnitTest :app:assembleDebug` with JDK 21.0.11, Gradle 8.14.3 and Android SDK 35.
- **110 domain tests + 26 app tests passed (136 total); 0 failures, errors or skips.**
- `:app:assembleDebug` produced a signed debug APK.
- Hint/ad flow verified on the Samsung tablet `R9TX902F92W` (SM-X210, Android 16, Slovak locale):
  - tapping Hint showed the confirmation dialog; confirming opened `com.google.android.gms.ads.AdActivity` with the official AdMob *test* ad;
  - the reward was granted only after the ad reported `Odmena je udelená`; `player.preferences_pb` then contained `hint_date=2026-11-02` and `hints_used=2`;
  - the time reward dropped from 3:00 to 1:30 after the hint, the level-1 text rendered, and a screenshot pixel scan found exactly one blue hint square (`c4`) — a hint never costs a life;
  - the daily counter persists across a relaunch.
- On the test device (Android 15, `CIYDDIMV5D5XNFUS`):
  - usage access and overlay appops granted; the foreground service runs with notification id 1001;
  - opening Facebook with a normal budget charged usage (10 s recorded in `budget.preferences_pb`) and showed no block screen;
  - seeding `used_seconds = 200000` produced the full-screen `TYPE_APPLICATION_OVERLAY` window titled "ChessLock block screen"; a `uiautomator` dump confirmed the text "Time's up", "You have used today's time for blocked apps.", "Solve a puzzle to earn more time.", the "Solve a puzzle" button and the "Go home" button;
  - switching to the launcher removed the overlay, and restoring the budget let the blocked app open normally.
- **Pending device check:** the phone rebooted into its secure lock screen and then dropped off USB, so the boot-receiver restart still needs a manual unlock/reconnect and one more reboot. Force-stopped apps intentionally do not receive boot broadcasts; the app must be launched normally before a reboot test.
- The overlay is created on the main thread (a `LifecycleRegistry` crash from the first version was fixed).
- UI input injection is blocked for adb on this device, so the buttons ("Solve a puzzle", "Go home") and the permission dialogs must be exercised manually.

## Environment notes
- Dependencies resolve from Google Maven, Maven Central and JitPack (chesslib).
- The committed built-in set contains 50 real Lichess CC0 puzzles, five in each rating band from 600 through 2600. Every FEN and complete UCI sequence was validated with `python-chess`.
- The generator streams the large Lichess dump while retaining only the best candidates per rating band.
- Pushing needs the Claude GitHub App installed on the repo owner (already done; push works).
- Do NOT invent puzzle data.
- `adb shell input` (taps/keyevents) is rejected with `INJECT_EVENTS`; use `am start`, `monkey`, `uiautomator dump`, `dumpsys` and logcat for verification.
- `pm grant POST_NOTIFICATIONS` is rejected on this device; grant notification permission manually or through the app's onboarding/settings links.
- A secure lock is active after reboot. `wm dismiss-keyguard` cannot bypass it; the device must be unlocked manually.

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
5. [x] Placement test: `PlacementTest` staircase + adaptive `PuzzleSelector` with tests, plus the onboarding UI flow using the built-in stock.
6. [x] Blocking service, categories, block screen, boot receiver (device reboot re-check pending after unlock).
7. [x] Onboarding + permissions + protection status.
8. [x] Hints with AdMob test ads
9. [x] README wrap-up for Phase 1, then STOP and wait for feedback

## Done log
- 2026-11-02: cloned repo, read all docs, wrote checkpoint, plan approved, pushed checkpoint.
- 2026-11-02: added `:domain` (AppConfig, EloCalculator, RewardCalculator, PuzzleSession, SkipPolicy, TimeBudget) with 56 passing tests; Gradle project, wrapper, Android skeleton, README build/decisions/licenses, `scripts/test-domain.sh`.
- 2026-11-02: added Puzzle model, Lichess CSV parser, PuzzleProgress, 23 more tests (79 total), built-in set generator script (tested on synthetic data only).
- 2026-11-02: added PlacementTest (staircase) and PuzzleSelector, 19 more tests (98 total), new AppConfig constants.
- 2026-11-02: generated and validated the 50-puzzle Lichess asset; added bounded-memory generation, Room/DataStore, chesslib adapters, the Compose board and puzzle flow, sk/en resources, and 8 passing app tests. Full debug build verified.
- 2026-11-02: added the settings screen (reachable from the puzzle top bar) with persisted piece styles: Classic, Outline and Letters, each previewed and applied live to the board. 4 new app tests (110 total); debug APK built and installed on the test device.
- 2026-11-02: bundled Cburnett chess artwork as VectorDrawables (BSD-3-Clause, documented in `THIRD_PARTY_NOTICES.md`) and added it as a fourth selectable style; 3 new app tests (113 total). Verified on the device.
- 2026-11-02: added the placement-test onboarding flow: shared board component, adaptive 10-puzzle run from the built-in stock with no rewards/ELO writes, 3 lives per puzzle, early finish on an empty stock, result screen, and `placement_completed` routing. Build and all 113 tests pass; device routing verified.
- 2026-11-02: added the blocking layer: `BlockCategory`/`PlatformCategory`/`AppCategoryClassifier`/`BlockPolicy` with precedence tests, daily `BudgetStore`, `ForegroundAppDetector`, `AppInfoProvider`, foreground `BlockingService`, main-thread Compose `BlockOverlayController` and `BlockScreen`, 5/1-minute warnings, `BootReceiver`, settings controls for limit/cap/categories/permissions, and puzzle rewards credited to the budget. 14 new tests (127 total at that point); device overlay and budget behavior verified.
- 2026-11-02: added the permissions onboarding (usage access, overlay, notifications) and a protection-status banner on the puzzle screen and in settings; the blocking service now exposes its running state and pauses counting / hides the overlay while the screen is off. The Compose-only additions keep the suite at 129 tests (109 domain + 20 app); full build verified. Device boot re-check pending unlock.
- 2026-11-02: added rewarded-ad hints (AdMob 23.6.0, official test IDs): `RewardedAdManager` preloads/shows with reward granted only in `onUserEarnedReward`; `HintAvailability` + `PuzzleSession.hintAvailability` domain rules; `PlayerStore` daily hint counter; `HintHighlight` board highlighting (level 1 piece, level 2 target); ViewModel/UI dialog, offline and daily-limit notes; en/sk strings. 7 new tests (136 total). Device-verified full ad + reward + hint flow on the SM-X210 tablet.
- 2026-11-02: Phase 1 task 9 README wrap-up: run-on-device steps, updated architecture/decisions/status, and the license table incl. AdMob; committed and pushed `9437825`. **Phase 1 (MVP) is complete — stopping for user feedback.**

## Next step
1. **Phase 1 is complete.** Wait for user feedback before starting Phase 2 (puzzle supply).
2. Optional pending device check: reconnect/unlock the old phone `CIYDDIMV5D5XNFUS`, launch the app once (clears Android's stopped flag), reboot, unlock, and confirm `BlockingService` starts automatically and the notification says protection is active. Also tap through the block screen buttons and permission dialogs there manually.

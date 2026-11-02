# CHECKPOINT

Resume file. Update after every meaningful step and before any likely credit cutoff. A new session reads this first, then CLAUDE.md, SPEC.md, PHASES.md and the README "Decisions".

## Status
- Date: 2026-11-02
- Current phase: Phase 2 (puzzle supply) — complete and device-verified; awaiting user feedback
- Plan: **approved by the user** (2026-11-02)
- Last completed: Phase 2 — daily 150-puzzle packs from the Lichess dump via GitHub Actions, WorkManager sync
  (24 h + launch + "Download now"), Wi-Fi-only default, Room accumulation with solved-never-repeat, and the Settings
  stock/download UI. Live release published and all flows verified on the tablet.
- Before Phase 2: Phase 1 was complete (MVP, language selection, Cburnett default, four licensed SVG piece sets).

## Verification
- Phase 2: ran `:domain:test :app:testDebugUnitTest :app:assembleDebug` with JDK 21.0.11, Gradle 8.14.3 and Android
  SDK 35. **116 domain tests + 31 app tests passed (147 total); 0 failures, errors or skips.** Debug APK built.
- Phase 2: validated `scripts/build-puzzle-packs.py` on synthetic data with a portable Python 3.12.7: 13 files x 150
  rows, unique IDs, ratings inside every band window, sorted rows, and a different daily salt produces a different
  selection. EN/SK resources were checked programmatically: 132 keys in both files, no placeholder mismatches and no
  `R.string.*` references missing from the default resources.
- Phase 2 live pipeline: pushed the workflow/script, Actions run `41524678991` succeeded on the first try and published
  13 assets to https://github.com/skywars1133-art/vibecoding-ran-test/releases/tag/puzzle-packs. A validator downloaded
  every asset (including one through a cache-busting `?v=` URL) and checked: exact Lichess header, 150 rows per file,
  unique IDs within each file, ratings inside `center-300..center+300`, rating deviation <= 100, popularity >= 90,
  plays >= 2000, 2-8 UCI plies and a 6-field FEN. Regenerated packs span each band, e.g. the 2200 pack now covers
  1900..2498 instead of stopping at 2217.
- Phase 2 on-device verification (Samsung tablet `R9TX902F92W`, SM-X210, Android 16):
  - installed the new debug APK; the launch sync downloaded band 1000 for `ELO 1088` and logged
    `Puzzle pack 1000: 150 puzzles, 149 new`;
  - Room grew 50 -> 199 rows; solved stayed 7, unsolved 192; the one ID that overlaps the built-in set (`ahNzi`) was
    ignored, proving de-duplication; sync metadata stored `last_band_center=1000`, `last_pack_size=150`,
    `last_added_count=149`, a fresh `last_success_epoch_ms`, and cleared the old 404 error;
  - WorkManager: periodic `12ad0e3d-…` ENQUEUED with a 86,400,000 ms interval and the one-time launch worker succeeded;
  - Settings shows the stock (192), last download, band, Wi-Fi-only switch (on by default) and **Download now**;
  - toggling Wi-Fi off wrote `wifi_only_downloads=False` and changed the JobScheduler network capability from
    `NOT_METERED&INTERNET...` to `INTERNET...`; toggling it back restored both;
  - **Download now** inside 24 h logged `Puzzle pack 1000: 150 puzzles, 0 new`, showed the "requested" note and
    refreshed the last-download timestamp;
  - solved-never-repeat: marked the overlapping `ahNzi` row solved in the DB, forced another download (0 new), and
    confirmed `ahNzi` stayed solved and the total stayed 199;
  - stock-exhausted screen: with every puzzle marked solved, the app showed "No unsolved puzzles are left in the offline
    stock...", plus working **Open settings** and **Retry** buttons; the real 7-solved state was then restored;
  - Slovak UI: "Sťahovanie úloh", "Nevyriešené úlohy v lokálnej zásobe: 192", "Posledné stiahnutie…", "Pásmo úloh:
    1000", "Sťahovať iba cez Wi-Fi" and "Stiahnuť teraz" all render, and tapping **Stiahnuť teraz** shows
    "Sťahovanie je naplánované...". The app was switched back to English afterwards.
- Phase 1 verification history:
  - **110 domain tests + 31 app tests passed (141 total); 0 failures, errors or skips.**
  - `:app:assembleDebug` produced a signed debug APK.
- Language/piece verification on the Samsung tablet `R9TX902F92W` (SM-X210, Android 16, Slovak locale):
  - switching System -> Slovak -> English updated every visible string immediately, without an activity restart or
    crash, and `settings.preferences_pb` stored `app_language=slovak` / `english`;
  - every artwork style rendered in the Settings preview rows and on the puzzle board; logcat had no
    `Could not render SVG` warnings;
  - Cburnett, Chessnut, Fantasy, Celtic and Spatial all produced comparable non-background pixel counts, and Spatial
    showed pieces on exactly the same squares as Cburnett for the same puzzle;
  - Spatial initially rendered only outlines/blank because its SVGs use root `width`/`height` in points and inherited
    gradients. After removing the root sizing and inlining the gradient stops, every Spatial piece rasterized normally.
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
- On the old phone `CIYDDIMV5D5XNFUS`, `adb shell input` (taps/keyevents) is rejected with `INJECT_EVENTS`; use
  `am start`, `monkey`, `uiautomator dump`, `dumpsys` and logcat there. On the tablet `R9TX902F92W`, `adb shell input
  tap/swipe` works and was used for the Phase 2 UI checks.
- `pm grant POST_NOTIFICATIONS` is rejected on this device; grant notification permission manually or through the app's onboarding/settings links.
- A secure lock is active after reboot. `wm dismiss-keyguard` cannot bypass it; the device must be unlocked manually.
- This machine has no system Python/Node/`gh`. A portable interpreter for validating scripts is at
  `C:\Users\detid\AppData\Local\Temp\opencode\python-embed\python.exe`; the GitHub Action itself uses the runner's
  `python3` and `gh`.

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
- 2026-11-02: added the in-app language option (`AppLanguage` System/English/Slovak, DataStore persistence, localized
  `ContextWrapper` + `LocalConfiguration` provider so `hiltViewModel()` still finds the Activity), made Cburnett the
  default piece style, and added four commercially usable SVG sets from the Lichess repository: Chessnut
  (Apache-2.0), Fantasy/Celtic/Spatial (MIT). SVGs render at runtime with AndroidSVG 1.4 and a bitmap `LruCache`;
  failures fall back to Classic glyphs. Spatial assets were normalised for AndroidSVG (root size/style removed,
  gradient stops inlined). 5 new tests (141 total); device-verified all styles and both languages.
- 2026-11-02: **Phase 2 implemented.** Added `PuzzlePackPlanner` (nearest 200-point band, ±300 range, deterministic
  filename/URL, due check) + 6 tests; `puzzlePack*`/Wi-Fi constants in `AppConfig`; HTTP CSV client, sync orchestrator,
  DataStore sync metadata, Hilt `PuzzlePackWorker`, and `PuzzlePackScheduler` (24 h periodic + launch/forced one-time,
  `UNMETERED` by default and `CONNECTED` when mobile data is allowed); Room `unsolvedCount`, `prepareStock` and
  `addPuzzles` with `INSERT IGNORE`; Settings stock/last-sync/error/Wi-Fi/Download-now UI; clearer empty-stock screen
  with Settings + Retry; English/Slovak strings; generator script and daily GitHub Action publishing to the fixed
  `puzzle-packs` release; README/notices/checkpoint updates. Build + 147 tests pass; generator validated on synthetic
  data. Committed as `36808a0`, `a395d06` and `17807d1`.
- 2026-11-02: **Phase 2 verified live.** Actions run `41524678991` published 13 validated packs; installed the APK on
  the tablet and verified the band download, Room accumulation/dedupe, sync metadata, WorkManager scheduling, the
  Wi-Fi toggle's effect on the network constraint, Download now within 24 h, solved-flag preservation, the empty-stock
  screen, and the full Slovak section. Details in Verification.

## Next step
1. **Phase 2 is complete and verified. Stop for user feedback before starting Phase 3.**
2. Optional pending Phase 1 device check: reconnect/unlock the old phone `CIYDDIMV5D5XNFUS`, launch the app once,
   reboot, unlock, and confirm `BlockingService` starts automatically. Also exercise the block-screen buttons and
   permission dialogs manually (adb input injection is blocked on that device).

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

### Run on a device

1. Install the debug build (Android 8.0 / API 26 or newer):

   ```
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

   Or open the project in Android Studio and press Run.
2. Finish the placement test (or skip through it as described in `CHECKPOINT.md`) to reach normal puzzle play.
3. Open **Settings** to choose the app language (System default / English / Slovak), the daily limit, blocked
   categories and piece style.
4. The **Puzzle downloads** section shows the offline stock, the last download and the Wi-Fi-only switch. Tap
   **Download now** to fetch the current pack immediately; otherwise WorkManager refreshes it about once a day.
5. Grant **Usage access**, **Display over other apps** and (optionally) **Notifications** from the protection banner or
   Settings; the banner turns to "Protection is active" once the blocking service is running.
6. Solve a puzzle to earn time, then open a blocked app after the budget runs out to see the block screen.
7. Tap **Stats** in the puzzle top bar for local-only statistics: ELO over time, overall and per-theme success rate,
   day streak, puzzles solved, time earned, and blocked-app time today/this week. Nothing is uploaded.
8. Hints use AdMob's official **test** ad IDs in this phase, so no real ad is shown or billed. Hints need a network and
   are disabled with an explanatory note while offline.

### Shareable test APK

`dist/ChessLock-0.1.0-test.apk` is a release build signed with the local Android debug key. It installs on API 26+
devices with `adb install -r dist/ChessLock-0.1.0-test.apk` (or by tapping the file on the device after allowing
installation from unknown sources). Because it uses the same key as the debug build, it upgrades an existing test
install without deleting app data.

Rebuild it with:

```
./gradlew :app:assembleRelease
$ANDROID_HOME/build-tools/<version>/zipalign -f -p 4 \
  app/build/outputs/apk/release/app-release-unsigned.apk \
  app/build/outputs/apk/release/app-release-aligned.apk
$ANDROID_HOME/build-tools/<version>/apksigner sign \
  --ks ~/.android/debug.keystore --ks-key-alias androiddebugkey \
  --ks-pass pass:android --key-pass pass:android \
  --out dist/ChessLock-0.1.0-test.apk \
  app/build/outputs/apk/release/app-release-aligned.apk
```

This is a **test key only** (anyone with the standard debug keystore could sign an update). Production signing material
belongs to Phase 4; never commit a keystore. `dist/` is gitignored.

### Built-in puzzle set
The committed `app/src/main/assets/builtin_puzzles.csv` contains 50 real Lichess puzzles (CC0). To regenerate it on a machine with internet:

```
curl -O https://database.lichess.org/lichess_db_puzzle.csv.zst
zstdcat lichess_db_puzzle.csv.zst | python3 scripts/build-builtin-puzzles.py > app/src/main/assets/builtin_puzzles.csv
```

It picks 5 puzzles per 200-point rating band from 600 to 2600 (most reliable, most popular first) and prints a
warning if a band has fewer. Only the best candidates per band are retained while streaming, so memory use stays bounded.
Commit the resulting file after validating it.

### Daily puzzle packs (Phase 2)
The full Lichess dump is far too large to download in the app. The GitHub Action in
`.github/workflows/puzzle-packs.yml` processes it once a day and publishes small rating-band CSVs as assets of the
fixed **`puzzle-packs`** release
(https://github.com/skywars1133-art/vibecoding-ran-test/releases/tag/puzzle-packs):

- band centers are every 200 points from 400 to 2800 (13 files: `puzzles-400.csv` ... `puzzles-2800.csv`);
- each file contains 150 puzzles from `center - 300` to `center + 300`, so it has easier and harder material around
  the player;
- candidates are filtered for rating reliability (deviation), popularity and play count, then kept per 50-point rating
  slice so a pack spans the whole band instead of drifting toward its easiest puzzles; a daily salt rotates the
  selection, so a new download normally brings new material even when the dump itself has not changed.

The app picks the band nearest the current ELO, downloads its CSV with `HttpURLConnection`, and inserts it into Room.
It asks for a pack via WorkManager every 24 hours, once per app launch (only if the last success is old enough), and
from the **Download now** button. The default is Wi-Fi-only (`NetworkType.UNMETERED`); turning the switch off relaxes
the constraint to any connected network. Failed downloads retry with exponential backoff and the error is shown in
Settings. Insertions use `IGNORE`, so solved puzzles never become unsolved and a repeated pack creates no duplicates.

To run the pipeline locally (needs Python 3.9+ and `zstd`):

```
curl -O https://database.lichess.org/lichess_db_puzzle.csv.zst
zstdcat lichess_db_puzzle.csv.zst | python3 scripts/build-puzzle-packs.py --output-dir puzzle-packs
```

Add `--require-full` to fail instead of warning when a band cannot be filled (the workflow always does, so an
incomplete pack is never published).

To publish immediately, open **Actions > Build puzzle packs > Run workflow**. The scheduled run is at 02:17 UTC.
The app's base URL and the pipeline's band defaults live in `AppConfig.puzzlePack*` and in
`scripts/build-puzzle-packs.py`; change them together. The workflow needs Actions enabled and the default
`GITHUB_TOKEN` write permission (declared in the workflow). If the release does not exist yet, the app records a
failed sync and retries; it keeps working from the built-in set plus any stock already downloaded.

## Architecture
- `domain/`: pure Kotlin/JVM, no Android imports. `config/AppConfig` (the ONE place for numeric constants), `elo`,
  `reward`, `puzzle` (`Puzzle` + Lichess CSV parser, `PuzzleProgress` solution tracking, `PuzzleSession` lives/hints,
  `SkipPolicy`, `PuzzlePackPlanner` band/URL/due rules), `budget` (daily time budget, midnight reset), `stats`
  (`Statistics` aggregation for success rate, streaks, themes, ELO history and earned time), and `blocking`
  (`BlockCategory`, the curated `AppCategoryClassifier`, `BlockRules`/`BlockPolicy`).
- `app/`: Android app (Compose, Material 3, Hilt). `di/ConfigModule` provides `AppConfig`; `data/` contains the Room
  puzzle stock and the local `attempts`/`usage_days` statistics tables, DataStore player progress, the shared
  `BudgetStore`, the settings store, the puzzle-sync metadata store, `PuzzlePackClient` (HTTP CSV download),
  `PuzzlePackSync` (ELO -> band -> Room), `PuzzlePackScheduler` and `PuzzlePackWorker` (WorkManager); `chess/` adapts
  chesslib for legal UCI moves and mating alternatives; `ui/puzzle/` contains the tap-to-move board, piece rendering,
  promotion picker, timer, skip controls and puzzle ViewModel; `ui/stats/` contains the local statistics screen,
  ELO chart and its ViewModel; `ui/settings/` contains the settings screen and its ViewModel; `blocking/` contains the
  foreground service, usage detector, overlay controller, notifications, boot receiver and permission helpers; `ads/`
  contains the rewarded-ad manager and connectivity helper; `ui/blocking/` contains the block screen and
  protection-status banner; `ui/onboarding/` contains the permissions onboarding.

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
  puzzle would have had (clamped to 400..2800). No hints or skips are offered during placement.
- **Placement test flow:** onboarding shows it until `placement_completed` is stored. It uses the built-in stock only,
  keeps each puzzle unique within one run (repository `excludeIds`), grants no rewards, records no ELO changes and does
  not mark puzzles solved. Losing all three lives records a failure immediately. If the stock runs out early the test
  ends with the current estimate. Confirming the result stores the ELO and opens normal puzzle play.
- **Next-puzzle choice:** desired rating = ELO + Gaussian offset (sigma 100), then a random pick among the 3 closest
  unsolved puzzles. Returns "no puzzle" only when the whole stock is solved.
- **Daily packs are pre-processed outside the app.** A GitHub Action streams the CC0 Lichess dump into 13 rating-band
  CSVs and publishes them as assets of the fixed `puzzle-packs` GitHub Release (no backend, no Pages setup). The app
  maps ELO to the nearest 200-point center and downloads only that file, so the phone never sees the multi-gigabyte
  dump. Packs cover center +/-300 and hold 150 puzzles. The pipeline keeps up to 3000 candidates per band, stratified
  into 50-point rating slices (rating deviation, popularity, plays, reasonable length rank the candidates within each
  slice) and rotates the 150 by a daily salt; the app adds the UTC day as a query parameter so a replaced release asset
  is not served from a cache.
- **Pack sync is WorkManager + DataStore metadata.** The periodic request runs every 24 h; the app also queues a
  one-time check on launch (skipped when the last success is fresh) and a forced sync from **Download now**. The
  default constraint is `UNMETERED` (Wi-Fi-only), relaxed to `CONNECTED` when the user allows mobile data. Failures
  retry with exponential backoff and are surfaced in Settings; the next daily run (or the next launch) gets another
  chance. A pack download is idempotent: `INSERT IGNORE` keeps the original row, so solved IDs never return.
- **Built-in stock remains the offline baseline.** The 50-puzzle asset supports first launch and the placement test
  without a network, while daily packs accumulate in the same Room stock. The puzzle screen's empty-stock message
  explains that no unsolved puzzles are left and offers a shortcut to Settings.
- **Starting ELO before the placement test:** 1000 (`AppConfig.eloStart`).
- **Piece styles.** Settings (opened from the puzzle top bar) offers Classic (solid Unicode glyphs), Outline (hollow
  Unicode glyphs with a contrast stroke), Letters (K/Q/R/B/N/P), Cburnett (bundled VectorDrawable artwork by Colin M.L.
  Burnett, BSD-3-Clause) and four licensed SVG sets rendered at runtime with AndroidSVG: Chessnut (Alexis Luengas,
  Apache-2.0), Fantasy, Celtic and Spatial (Maurizio Monge, MIT). Cburnett is the default. The choice is stored in the
  `settings` DataStore and applied to the board immediately; switching does not reset the current puzzle. Tornelo's
  named sets (Chigorin, Blackburne, etc.) are proprietary and are not bundled. See `THIRD_PARTY_NOTICES.md`.
- **Language is in-app.** Settings offers System default, English and Slovak. The choice is stored in the `settings`
  DataStore and applied through a localized `ContextWrapper` + `LocalConfiguration` override, so every Compose string
  switches immediately without restarting the activity. Notification and foreground-service text still follows the
  system locale because they are built outside the composition.
- **Blocking is non-strict and permission-based.** A foreground service polls `UsageStatsManager` events (no
  `AccessibilityService`) and charges the one shared daily budget only while a blocked package is in the foreground.
  `TYPE_APPLICATION_OVERLAY` (granted through `SYSTEM_ALERT_WINDOW`) shows a full-screen Compose block screen when the
  budget is empty. The service polls every 2 s, caps catch-up after a freeze at 10 s, flushes pending usage every 15 s,
  and stops counting / hides the overlay while the screen is off.
- **Blocking precedence:** ChessLock, the launcher, phone/emergency, Settings, SystemUI, the installer and the
  permission controller are never blocked. An explicit "never block" override wins over an explicit "always block"
  override, which wins over the selected category. Categories default to Social, Games and Video/streaming; the
  daily limit defaults to 30 min and the optional total cap is off.
- **Category source:** `ApplicationInfo.category` plus a curated map of popular packages, because many apps leave the
  platform category unset and Android has no communication/shopping category values. `QUERY_ALL_PACKAGES` is declared
  for reliable metadata reads and the future individual-apps picker; the Play policy declaration is release-prep work.
- **Protection status is visible.** First run explains usage access, overlay and notification permissions and lets the
  user continue without them; afterwards the puzzle screen and Settings show "Protection is not active" (or "App
  blocking is off") with a shortcut to fix it. The status also reflects whether the blocking service is alive.
- **Boot and process death:** `BootReceiver` starts the service after `BOOT_COMPLETED` and `MY_PACKAGE_REPLACED` when
  blocking is enabled. Android deliberately skips boot broadcasts for apps that were force-stopped until the user
  launches them again.
- **Puzzle rewards feed the budget:** a solved puzzle's reward is written to `BudgetStore` immediately, so the block
  screen disappears on the next poll without restarting the service.
- **Hints are rewarded ads only.** Tapping Hint first shows a dialog that explains the cost (half the time reward and
  no ELO gain on a win). The hint is granted only from AdMob's `onUserEarnedReward` callback; an ad that fails, is
  skipped or is closed early grants nothing. Level 1 highlights the piece to move, level 2 also highlights the target;
  the counter resets after each correct move. A hint never costs a life. The daily hint limit is unlimited by default
  (`AppConfig.dailyHintLimit`); offline devices show an explanatory note and disable the button. The next ad is
  preloaded after the previous one closes. Phase 1 uses AdMob's official test app and rewarded-unit IDs; production IDs
  and UMP consent are Phase 4 work.
- **Statistics are local-only for now.** Accounts, cloud sync and the leaderboard are deferred at the user's request;
  the statistics screen reads Room and DataStore on the device and uploads nothing. Room v2 adds append-only
  `attempts` and per-day `usage_days`; `MIGRATION_1_2` only creates the new tables/indices, so existing puzzles and
  solved flags survive the upgrade.
- **One row per attempt.** A LOSS is written when the third life is lost (the moment ELO changes). If the player keeps
  going and solves that puzzle, the same row is completed with its reward and hint count, and the ELO result stays the
  loss that was already applied. Skips are logged but excluded from the success rate; a skip after an already-recorded
  loss is not logged again. "Puzzles solved" is the cumulative count of unique solved puzzles, so solves from before
  statistics existed are included.
- **Stats aggregation lives in the domain module.** `Statistics.summarize` takes attempts, the current ELO and `today`,
  then produces the success rate, day streaks (today may still be empty), top themes, the latest
  `statsEloHistoryLimit` rated attempts, and total earned time. Generic Lichess tags (`short`, `long`, `master`,
  `blitz`, ...) are filtered from the theme list via `AppConfig.statsIgnoredThemes`.
- **Usage history is best-effort.** `BudgetStore` mirrors its usage/earned writes into `usage_days`, but a statistics
  failure is logged and never changes the budget result. The screen uses the larger of today's history row and the live
  budget, so usage recorded before the stats table existed is not shown as zero. All date handling uses the local date.
- **Min SDK 26** (java.time without desugaring, notification channels).
- **Dependency versions** in `gradle/libs.versions.toml` are a resolved compatible set (AGP 8.7.3, Kotlin 2.0.21,
  KSP 2.0.21-1.0.28, Hilt 2.52, Compose BOM 2024.10.01, AdMob 23.6.0, AndroidSVG 1.4, WorkManager 2.9.1,
  androidx.hilt 1.2.0). Bump them if Gradle complains.
- **Piece artwork** lives in `app/src/main/assets/pieces/<style>/` as 12 SVGs per set; Cburnett stays as VectorDrawable
  resources. AndroidSVG rasterizes the SVG sets to a small `LruCache` of bitmaps; a failed render falls back to the
  Classic glyph so the board never becomes unplayable.
- **Status of the Android module:** Phase 1 is complete (puzzle stock, board/puzzle flow, placement test, blocking
  service and overlay, permissions onboarding and protection status, rewarded-ad hints, language and piece styles).
  Phase 2 is complete and device-verified (daily rating-band pack generation in GitHub Actions, WorkManager downloads
  with a Wi-Fi-only default, Room accumulation, solved-never-repeat, and the Settings stock/download UI). Phase 3 is
  implemented as **local-only statistics** (ELO history, success rate overall/per theme, streaks, solved count, earned
  time, blocked-app usage) with accounts/cloud/leaderboard deferred. The app compiles and all unit tests pass; the
  statistics migration and attempt recording were verified on the tablet. Details are in `CHECKPOINT.md`.

## Licenses
| Item | License | Use |
| --- | --- | --- |
| Lichess puzzle database | CC0 | Puzzle data (Phase 1 built-in set, Phase 2 packs) |
| AndroidX, Jetpack Compose, Material 3 | Apache-2.0 | UI |
| Hilt / Dagger | Apache-2.0 | DI |
| AndroidX WorkManager | Apache-2.0 | Daily puzzle-pack scheduling and background download |
| AndroidX Hilt Work | Apache-2.0 | Injects dependencies into `PuzzlePackWorker` |
| Kotlin, KSP | Apache-2.0 | Language / build |
| JUnit 4 | EPL-1.0 | Tests only (not shipped) |
| chesslib | Apache-2.0 | Legal moves, FEN and checkmate detection |
| Google Mobile Ads SDK (AdMob) | Google Mobile Ads SDK / Google APIs terms (proprietary) | Rewarded hint ads; official test IDs in Phase 1; production IDs and UMP consent in Phase 4 |
| AndroidSVG | Apache-2.0 | Rasterizes the bundled SVG piece sets |
| Cburnett chess pieces | BSD-3-Clause (also GFDL/GPL) | Default board artwork (`res/drawable/cburnett_*.xml`); see `THIRD_PARTY_NOTICES.md` |
| Chessnut chess pieces | Apache-2.0 | Bundled artwork (`assets/pieces/chessnut/`) |
| Fantasy, Celtic, Spatial chess pieces | MIT | Bundled artwork (`assets/pieces/{fantasy,celtic,spatial}/`) |
| System Unicode chess glyphs | No separate asset | Classic/Outline/Letters styles rendered as text |

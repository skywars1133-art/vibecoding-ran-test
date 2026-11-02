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
  `SkipPolicy`), `budget` (daily time budget, midnight reset), and `blocking` (`BlockCategory`, the curated
  `AppCategoryClassifier`, `BlockRules`/`BlockPolicy`).
- `app/`: Android app (Compose, Material 3, Hilt). `di/ConfigModule` provides `AppConfig`; `data/` contains the Room
  puzzle stock, DataStore player progress, the shared `BudgetStore` and the settings store; `chess/` adapts chesslib
  for legal UCI moves and mating alternatives; `ui/puzzle/` contains the tap-to-move board, piece rendering,
  promotion picker, timer, skip controls and puzzle ViewModel; `ui/settings/` contains the settings screen and its
  ViewModel; `blocking/` contains the foreground service, usage detector, overlay controller, notifications, boot
  receiver and permission helpers; `ads/` contains the rewarded-ad manager and connectivity helper; `ui/blocking/`
  contains the block screen and protection-status banner; `ui/onboarding/` contains the permissions onboarding.

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
- **Starting ELO before the placement test:** 1000 (`AppConfig.eloStart`).
- **Piece styles.** Settings (opened from the puzzle top bar) offers Classic (solid Unicode glyphs), Outline (hollow
  Unicode glyphs with a contrast stroke), Letters (K/Q/R/B/N/P) and Cburnett (bundled SVG artwork by Colin M.L.
  Burnett, BSD-3-Clause — see `THIRD_PARTY_NOTICES.md`). The choice is stored in the `settings` DataStore and applied
  to the board immediately; switching does not reset the current puzzle. Tornelo's named sets (Chigorin, Blackburne,
  etc.) are proprietary and are not bundled.
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
- **Min SDK 26** (java.time without desugaring, notification channels).
- **Dependency versions** in `gradle/libs.versions.toml` are a resolved compatible set (AGP 8.7.3, Kotlin 2.0.21,
  KSP 2.0.21-1.0.28, Hilt 2.52, Compose BOM 2024.10.01, AdMob 23.6.0). Bump them if Gradle complains.
- **Status of the Android module:** Phase 1 tasks 1–8 are implemented (puzzle stock, board/puzzle flow, placement test,
  blocking service and overlay, permissions onboarding and protection status, rewarded-ad hints). The app compiles,
  its unit tests pass, and blocking plus the hint/ad flow have been verified on physical Android devices. A final
  boot-receiver re-check on the old test phone still awaits a manual unlock after it dropped off USB. Remaining Phase 1
  work is the wrap-up, tracked in `CHECKPOINT.md`.

## Licenses
| Item | License | Use |
| --- | --- | --- |
| Lichess puzzle database | CC0 | Puzzle data (Phase 1 built-in set, Phase 2 packs) |
| AndroidX, Jetpack Compose, Material 3 | Apache-2.0 | UI |
| Hilt / Dagger | Apache-2.0 | DI |
| Kotlin, KSP | Apache-2.0 | Language / build |
| JUnit 4 | EPL-1.0 | Tests only (not shipped) |
| chesslib | Apache-2.0 | Legal moves, FEN and checkmate detection |
| Google Mobile Ads SDK (AdMob) | Google Mobile Ads SDK / Google APIs terms (proprietary) | Rewarded hint ads; official test IDs in Phase 1; production IDs and UMP consent in Phase 4 |
| Cburnett chess pieces | BSD-3-Clause (also GFDL/GPL) | Bundled board artwork (`res/drawable/cburnett_*.xml`); see `THIRD_PARTY_NOTICES.md` |
| System Unicode chess glyphs | No separate asset | Classic/Outline/Letters styles rendered as text |

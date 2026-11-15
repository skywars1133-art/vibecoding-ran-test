# CHECKPOINT

Resume file. Update after every meaningful step and before any likely credit cutoff. A new session reads this first, then CLAUDE.md, SPEC.md, PHASES.md and the README "Decisions".

## Status
- Date: 2026-11-15
- Current phase: Phase 4 (hardening and release prep) - widget bundle implemented and device-verified; awaiting user
  feedback
- Plan: **approved by the user** (2026-11-02)
- Last completed: visual-polish bundle - fixed ChessLock brand palette (no Material You dynamic colours), typography
  and shapes, an animated Home budget ring with an urgency chip driven by pure `BudgetUrgency` (comfortable / warning /
  critical / empty), a richer puzzle header (info chips, hearts for the lives, framed board, animated solved card), a
  gradient ELO chart and a redesigned block screen. The overlay now also honours the saved theme and in-app language.
  206 tests pass; debug and signed release APKs rebuilt, and the release was installed over the debug build with all
  app data intact. Follow-up (same day): the Home ring was reported as "not working" on the tablet; it rendered
  correctly at partial budgets, so the 100%-full state was the problem. `BudgetRing` now sweeps from 0 to the target on
  every change and draws twelve gauge ticks over the stroke, so a full ring still reads as a dial. Release rebuilt
  (SHA-256 `2A5F28568FF90528C512727C2DC2228A843523584B08D62C9F84F9FFC7865A46`). Then a launcher-identity bundle added
  a branded adaptive icon (Cburnett knight on a deep-green gradient, with an Android 13 monochrome variant) and a
  matching Android 12+ system splash plus a pre-12 launch window; release re-signed
  (SHA-256 `1E80E0D163BB6205306013B4FB77FCA0D39D60A2781088F40173D424F1438B23`). Then a shortcut bundle added
  three static launcher shortcuts (Solve a puzzle, View statistics, Open settings) routed through the pure
  `ShortcutDestination` enum; release re-signed
  (SHA-256 `6E02BCB03261364CE9D98A57ECDE0D33B3D893ACBADAE566B372BD333CDEA763`). Then a home-screen widget bundle added
  a 3x2 **Time budget** RemoteViews card (knight, remaining clock, urgency caption, progress bar, ELO) that refreshes on
  budget/ELO/limit/language writes plus the platform 30-minute tick, follows the in-app language and opens Home on tap;
  release re-signed (SHA-256 `904D6F36347DF7730C3CCCAEE5B338A469A91E655940884E21E4AEDA7E6D2095`, 10 847 720 bytes).
  Follow-up at the user's request: the widget now refreshes **every minute** through a chained, minute-aligned
  one-shot `AlarmManager.RTC` alarm instead of relying on the 30-minute platform tick; release re-signed (SHA-256
  `11C68AC662FE26714D8D8A15AC758E863248F7CC138A7937F970D01C82636D04`, 10 847 720 bytes). 219 tests pass.
- Before that: Phase 4 protection and settings bundle - adaptive foreground polling (`PollingPolicy`: 2 s while <=5 min
  remain, relaxed 5 s otherwise), battery-optimization exemption guidance in onboarding and Settings (recommended, not
  required for "active"), a persistent 5/1-minute **Time warnings** toggle, a **Haptic feedback** toggle wired to puzzle
  moves, and a **Privacy and ads** explainer screen. 198 tests pass.
- Before that: Home screen as the app root — big remaining-time card (limit/earned/used breakdown, progress bar),
  ELO and earned-today metrics, protection banner, and **Solve a puzzle** / **Stats** / **Settings** actions. The
  pre-existing blocked-app overcount was also fixed: the service compared `SystemClock.elapsedRealtime()` **milliseconds**
  against a **seconds** catch-up cap, so every 2 s poll charged the full 10 s cap (~4× inflation). Counting now goes
  through a unit-tested `UsageMeter` that converts milliseconds, carries sub-second remainders and advances the tick
  before any I/O.
- Before that: local settings bundle — in-app Light/Dark/System theme and a searchable per-app blocking picker
  (Use categories / Always block / Never block) built on the existing overrides. 172 tests pass.
- Before that: Phase 3 local statistics — ELO history chart, overall/per-theme success rate, day streak, puzzles
  solved, total earned time, and blocked-app usage today/this week. Room v2 (`attempts`, `usage_days`) with a tested
  migration, attempt recording for solve/loss/skip, EN/SK UI, and 19 new tests.
- Deferred: accounts, cloud sync and leaderboard, at the user's request.
- Before Phase 3: Phase 2 complete (daily puzzle packs, WorkManager sync, solved-never-repeat) and Phase 1 complete
  (MVP, language selection, Cburnett default, four licensed SVG piece sets).

## Verification
- Visual-polish bundle (2026-11-14): ran `:domain:test :app:testDebugUnitTest :app:assembleDebug` and
  `:app:assembleRelease` with JDK 21.0.11. **152 domain tests + 54 app tests passed (206 total); 0 failures, errors or
  skips.** `BudgetUrgencyTest` (7) covers the warning/critical boundaries, empty and negative budgets, unsorted and
  missing threshold lists, and non-positive thresholds; `HomeStateTest` gained an urgency case fed by `config.warnAtSeconds`.
- Release APK rebuilt and signed at `dist/ChessLock-0.1.0-test.apk` (v2/v3 verified, SHA-256
  `2CC533CBE0F84AE0DFBA6F57607E4718EBCEC4CDEDFF305BA46DA4D0E52DC10D`, 10 818 040 bytes) and installed over the debug
  build. Data survived: ELO **1612**, budget `35:00` remaining (`30:00` limit + `5:00` earned) and `1453` unsolved
  puzzles at the time. Reopening the app after midnight rolled the day's budget over as designed; continued play had
  raised ELO to 1660 by the final install. The installed package is the release variant (`run-as` reports "package not debuggable").
- Visual checks on `R9TX902F92W` (2026-11-14):
  - **Home hero ring** renders at 148 dp with the countdown centred. Light theme: mint card gradient (sampled
    `#A2EECE` under the ring, fading to `#97E3C3`), ring `#116B4F`, countdown `#002117`. Dark
    theme: `#00513B` base, mid-gradient `#115D46`, ring/text `#8AD6B7` / `#A6F2D2`. Metric tiles use
    `surfaceContainerLow` with tinted icon badges.
  - **Urgency states** were forced by temporarily rewriting `budget.preferences_pb` (valid DataStore proto; backup
    restored immediately, and an empty budget writes no stats rows): `30:00` remaining showed **Plenty of time** (green),
    `4:00` showed **Running low** with an amber gradient (observed `#724205 -> #865313`) and `#FFB95C` accent, `0:30`
    showed **Almost out** in the error palette (`#980D13 -> #AB3730`, `#FFB4AB` accent), and `0:00` showed **No time
    left** with an empty ring and the error palette.
  - **Puzzle screen** shows the ELO and rating chips, the side chip, the solving timer, `Lives: 3` next to three filled
    hearts and the live `Time reward` chip; the board sits in a rounded frame and Hint/Skip are visible while unsolved.
  - **Statistics** ELO chart draws the gradient area fill under the line (dark theme: `#273A32` just below the line
    fading to the card background `#18231E`).
  - **Block screen** in English: light theme `#F5FBF7` background, error badge `#FFDAD6`/`#410002`, primary button
    `#116B4F`; dark theme `#0F1512` background, badge `#93000A`/`#FFDAD6`, primary `#8AD6B7`.
  - The overlay previously always used the system locale and theme. It now renders in the in-app language and saved
    theme, verified in English while the device locale is Slovak, and in both Light and Dark.
- Ring legibility follow-up (2026-11-14): `:domain:test :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease`
  with JDK 21.0.11 kept all **206 tests green**. `BudgetRing` now uses `Animatable(0f)` + `LaunchedEffect` to sweep
  0 -> target over 900 ms with `FastOutSlowInEasing`, and draws 12 radial gauge ticks (`contentColor` at 22% alpha,
  2 dp, rounded caps) over the stroke band. Pixel checks on `R9TX902F92W`:
  - 25% budget: arc covers 0-90 degrees in `#116B4F`, remaining track `#82C7AA`. Ticks darken the arc to `#0D5B43` at
    30/90 degrees and the track to `#75B49A`/`#69A58D` at 180/270 degrees.
  - 100% budget: the whole ring is `#116B4F` with the same tick marks (`#0D5B43` at 0/30/90/270 degrees), so the full
    state no longer looks like a plain static circle.
  - Confirmed identical output on the signed release build, then restored the budget to `30:00` remaining (`100%`) and
    reinstalled the release over debug with ELO and stock intact.
- Release APK re-signed at `dist/ChessLock-0.1.0-test.apk` (v2/v3 verified, 10 818 040 bytes, SHA-256
  `2A5F28568FF90528C512727C2DC2228A843523584B08D62C9F84F9FFC7865A46`) and installed over the debug build; the
  installed package is the release variant (no debuggable flag).
- Launcher-identity bundle (2026-11-14): `:domain:test :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease`
  with JDK 21.0.11 kept **206 tests green** (resource-only changes, no new logic). Added
  `drawable/ic_launcher_background|foreground`, `mipmap-anydpi-v26|v33/ic_launcher(_round)`, `values/colors.xml`,
  `drawable/splash_legacy.xml`, a `values-v31/themes.xml` splash theme and `android:icon`/`roundIcon` in the manifest.
  Device checks on `R9TX902F92W`:
  - **Launcher icon** in the app drawer: a 78x78 px green tile (`#116B4F`-family gradient) centred in the 158 px cell
    with the white knight inside. The app-info header shows the same green mark at 39x39 px.
  - **Cold-start splash** (debug and release): the first captured frame is a full brand-green `#116B4F` field with a
    centred white knight measuring 224x226 px (debug) / 220x218 px (release) at the 1200x1920 screen centre, followed
    by Home. `am start` immediately followed by `screencap` reliably caught it.
  - **Themed icon**: the v33 variants expose the same foreground as `monochrome`; the device's themed-icon setting was
    left unchanged, so only the standard icon was visually checked.
- Release APK re-signed at `dist/ChessLock-0.1.0-test.apk` (v2/v3 verified, 10 822 584 bytes, SHA-256
  `1E80E0D163BB6205306013B4FB77FCA0D39D60A2781088F40173D424F1438B23`) and installed over the debug build. Data
  survived (ELO and puzzle stock unchanged; budget showed `35:00` remaining), and the installed package has no
  debuggable flag.
- Shortcut bundle (2026-11-14): `:domain:test :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease` with
  JDK 21.0.11 gave **152 domain + 58 app tests (210 total); 0 failures, errors or skips**. New
  `ShortcutDestinationTest` (4) checks every action maps back to its destination, unknown and `null` actions are
  ignored, and the actions stay unique.
  - **Registration:** `cmd shortcut get-shortcuts com.chesslock` lists all three (`puzzle`, `statistics`, `settings`)
    targeting `com.chesslock/.MainActivity`, ranks 0-2, with the three branded icons. The first attempt printed only
    `Success` because the `android.app.shortcuts` meta-data had been placed under `<application>`; Android only reads
    it from the MAIN/LAUNCHER activity. It now sits inside the `MainActivity` element and the static shortcuts load.
  - **Real long-press** (debug and release) on ChessLock in the app drawer shows ChessLock + **Puzzle / Štatistiky /
    Nastavenia** above the Select / Add to home / Uninstall row, with three green circular shortcut icons (white glyph
    on the brand-green `#116B4F` family). Tapping **Puzzle** opened the puzzle screen (ELO 1667, rating 1727), and
    tapping **Štatistiky** opened Statistics (ELO 1667, 22 rated puzzles in the chart). Direct
    `am start -a com.chesslock.shortcut.PUZZLE|STATISTICS|SETTINGS` calls route to the matching screen after a
    force-stop, covering the cold-start `onCreate` path; the warm path is covered by `onNewIntent`.
  - Shortcut labels follow the **system** locale (Slovak on this tablet) even when the app is set to English, which is
    expected for static shortcuts; the destinations are language-independent.
  - The `uiautomator dump` command fails with `ERROR: could not get idle state` while the popup is still animating;
    `uiautomator dump --compressed` after ~8 s captures the popup reliably.
- Release APK re-signed with the corrected manifest at `dist/ChessLock-0.1.0-test.apk` (v2/v3 verified, 10 826 904
  bytes, SHA-256 `6E02BCB03261364CE9D98A57ECDE0D33B3D893ACBADAE566B372BD333CDEA763`), installed over the debug build.
  Data survived (ELO 1667, puzzle stock and statistics unchanged) and the package has no debuggable flag. An
  intermediate build with the misplaced meta-data (`7CCE735E...`) was never distributed.
- The local date rolled over to 2026-11-14 after verification, so the next launch resets `earned_seconds`/`used_seconds`
  for the new day (the midnight reset is by design; ELO and puzzle stock are unaffected).
- Widget bundle (2026-11-15): `:domain:test :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease` with
  JDK 21.0.11 gave **152 domain + 64 app tests (216 total); 0 failures, errors or skips**. New `WidgetSnapshotTest`
  (6) covers a full budget, earned/used arithmetic, the daily cap, over-use clamping to zero, a zero total and the
  urgency thresholds.
  - **Picker:** searching "chess" lists **ChessLock -> Časový rozpočet, 3x2** with a live preview (knight, `--:--`,
    "Dnes zostáva") and the Slovak description. Tapping the preview shows the **Pridať** button; adding placed the card
    on home page 2 without restarting the launcher.
  - **Rendered output:** `uiautomator` reads `ChessLock / ELO 1667 / 35:00 / Dnes zostáva` inside the 436x300 px card.
    Pixel sampling confirmed the deep board green (`17,108,79`) and a full-width white progress fill at y≈308 for a
    100% budget.
  - **Refresh paths:** raising the daily limit 30 -> 45 min changed the card from `35:00` to `50:00` without reopening
    it (then restored 30 min). Browsing Chrome (a **Use categories** app) for 10 s changed `35:00` -> `34:50`, proving
    `BudgetStore.addUsage` requests a render. ELO writes go through the same `requestUpdate` path.
  - **Tap to open:** the card opens Home (`MainActivity` focused). The first tap after dropping the widget only leaves
    the launcher's edit mode, which is platform behaviour.
  - **Language:** switching the app to English changed the caption to "Time left today" immediately; switching back to
    Slovak restored "Dnes zostáva". This needed a new non-Compose `withAppLanguage` helper plus a `setLanguage` update
    hook, because widget text is built outside the Compose locale wrapper.
  - **Midnight rollover:** the next morning Home and the widget both showed limit `30:00`, earned `0:00`, used `0:10`
    -> `29:50`, i.e. the new day reset earned/used and the widget stayed in sync.
  - Release APK re-signed at `dist/ChessLock-0.1.0-test.apk` (v2/v3 verified, 10 847 720 bytes, SHA-256
    `904D6F36347DF7730C3CCCAEE5B338A469A91E655940884E21E4AEDA7E6D2095`), installed over the debug build. Data and the
    placed widget survived (ELO 1667, limit 30:00); `run-as` reports "package not debuggable".
- Per-minute widget bundle (2026-11-15): `:domain:test :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease`
  with JDK 21.0.11 gave **152 domain + 67 app tests (219 total); 0 failures, errors or skips**. New
  `nextWidgetTickAt` (3) covers minute alignment, a tick landing exactly on the boundary, and a non-positive interval.
  - **Cadence:** `setInexactRepeating` fired but got a batching window of up to 45 s; a chained one-shot
    `set(RTC, nextMinute, ...)` measured a ~21 s window, so the ticker uses the one-shot chain, re-arming itself
    before each render. `dumpsys alarm` showed `*alarm*:com.chesslock.widget.action.TICK`, `repeatInterval=0`, firing
    at `10:02:16` and re-arming for `10:03:00`; the release build fired at `10:09:00` and re-armed for `10:10:00`.
  - **Recovery:** `am force-stop` cleared the scheduled alarm (no pending batch). Installing over the app
    (`ACTION_MY_PACKAGE_REPLACED`) then re-armed it at the next minute boundary (`10:12:00`) through
    `BootReceiver` -> `BudgetWidgetUpdater.refreshIfPlaced`, which also repaints the card; `MainActivity` runs the
    same call on launch for force-stopped installs. `onDisabled` cancels the chain when the last widget is removed.
  - **Language anomaly resolved:** Home and the widget were showing English mid-session. `settings.preferences_pb`
    held `app_language = english` and had a write timestamp after the previous release install, so it was an in-app
    selection rather than an install reset. Selecting **Slovenčina** again stored `slovak`, and Home, Settings and the
    widget switched immediately. Reinforcing it over a debug build and then the signed release left the 10:04 file
    timestamp untouched, i.e. installs do not rewrite the language. No code path writes a language without
    `SettingsStore.setLanguage`.
  - Release APK re-signed at `dist/ChessLock-0.1.0-test.apk` (v2/v3 verified, 10 847 720 bytes, SHA-256
    `11C68AC662FE26714D8D8A15AC758E863248F7CC138A7937F970D01C82636D04`), installed over the debug build and
    non-debuggable. Data survived (ELO 1667, limit `30:00`, used `0:10` -> `29:50`), the widget stayed on home page 2
    with the Slovak caption `Dnes zostáva`, and the minute alarm was scheduled after the install.
- Phase 4 bundle (2026-11-13): ran `:domain:test :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease` with
  JDK 21.0.11. **145 domain tests + 53 app tests passed (198 total); 0 failures, errors or skips.** The new
  `PollingPolicyTest` (5) covers the relaxed interval above the threshold, the switch at the threshold, empty/negative
  budgets, relaxed-never-below-active, and interval clamping. `AppSettingsTest` now also asserts the warning and haptics
  defaults.
- Release APK rebuilt and signed at `dist/ChessLock-0.1.0-test.apk` (v2/v3 verified, SHA-256
  `0891F3F80BC54F08C859056A7046BC3769C0FD4D5A2C61E5226CCC56B19BD592`, 10 801 656 bytes) and installed over the debug
  build. Data survived and is identical to before the upgrade: ELO **1604**, budget `30:00` limit / `0:00` used,
  `1454` unsolved puzzles, band 1600, last download 13 Nov 2026 11:33. The installed package is the release variant
  (`run-as` reports "package not debuggable").
- Phase 4 device checks on `R9TX902F92W` (2026-11-13):
  - **Settings sections** render in both builds: Feedback -> **Haptic feedback**, **System permissions** -> **Time
    warnings** (plus Usage access / Overlay / Notifications, all Granted) and **Battery optimization exemption**
    (Granted), then **Privacy and ads**.
  - **Toggles persist.** Haptic feedback and Time warnings were switched off: `settings.preferences_pb` gained
    `haptics_enabled = 0` and `warning_notifications = 0` and the switches read unchecked; switching both back on
    rewrote `1`/`1`. (An earlier attempt missed because the screen had scrolled back to the top; the switches are at
    `x≈1137, y≈736` and `y≈1388` at the "Puzzle downloads" scroll position.)
  - **Battery card** opens the system battery-usage list (`ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`), where
    ChessLock was absent because it was not exempt. `adb shell cmd deviceidle whitelist +com.chesslock` flipped the card
    to **Granted**; the shell exemption was left in place so protection stays reliable on this test device (remove with
    `adb shell cmd deviceidle whitelist -com.chesslock`).
  - **Privacy and ads** opens from Settings and shows all four sections (data stays on device, app-usage data, ads and
    consent, local-only footer); system Back returns to Settings.
  - **Adaptive polling** verified end-to-end with the new one-line `Poll interval is now Xs` log (logged only when the
    interval changes). At `30:00` remaining the service reported `5s`. The budget file was then temporarily rewritten
    to `used_seconds = 3000` (valid DataStore preferences proto, backup restored immediately afterwards) to reach an
    empty budget: the service reported `2s` and the block screen appeared over the browser within ~4 s. Restoring the
    original file brought Home back to `30:00` / `0:00` used and the log back to `5s`; no stats rows were written
    because an empty budget charges nothing.
- Home bundle + usage fix (2026-11-04): ran `:domain:test :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease`
  with JDK 21.0.11. **140 domain tests + 53 app tests passed (193 total); 0 failures, errors or skips.** The 12
  `UsageMeterTest` cases cover persisted-reset, a 2 000 ms tick charging 2 s (the regression), remainder carry, the
  catch-up cap, remaining-budget clamp, flush/restore and the midnight reset. `HomeStateTest` (5) and
  `TimeFormatTest` (4) cover the home mapping and clock formatting.
- Overcount fix on the tablet `R9TX902F92W` (debug build): after reverting to a clean `0:00`, 10 s of Chrome added
  **8 s** (previously 40 s) and 20 s added **18 s**; the ~2 s gap is the two tick boundaries. On the signed release
  build, 10 s of Brawl Stars added **8 s**. Chrome was then returned from **Always block** to **Use categories**
  (confirmed: no Chrome entry under `always_blocked_packages`/`never_blocked_packages`).
- Release APK rebuilt and signed at `dist/ChessLock-0.1.0-test.apk` (v2/v3 verified, SHA-256
  `69EA1D84EA89A96A6C958701B85D8130FB932EBAC2F75E8B91DB88BD71B4851A`), installed over the debug build. Data survived:
  ELO 1600, stats `85% (6 of 7)`, and the puzzle band. Today's budget was reset to `0:00` after the timing tests; the
  release-build Brawl Stars check left `0:08` used, which is harmless test residue.
- Home flow on the tablet (2026-11-03, debug build): Home showed limit `30:00`, earned `14:00`, used `0:00`,
  remaining `44:00`, ELO 1559; **Solve a puzzle** opened the puzzle with a **Back** action and system Back returned
  Home; Stats and Settings opened from Home.
- Local settings bundle (2026-11-03): ran `:domain:test :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease`
  with JDK 21.0.11. **128 domain tests + 44 app tests passed (172 total); 0 failures, errors or skips.** The new
  `AppPickerTest` covers blank/trimmed/case-insensitive label and package search; `AppSettingsTest` covers the theme
  default and storage-key round-trip.
- Theme on the tablet `R9TX902F92W`: Settings shows System default / Light / Dark. A screenshot pixel check measured
  the light background at `(250,248,255)` and dark at `(18,19,24)`; the choice persisted as `theme_mode=light|dark`
  in `settings.preferences_pb`, survived reinstall, and was left on System default (the tablet's system theme is light).
- Per-app picker on the tablet: **Choose individual apps** lists launchable apps (critical packages excluded), search
  for `chr` reduced the list to Chrome, and the row label changed after each selection. Selecting **Always block**
  wrote `always_blocked_packages=com.android.chrome`; **Never block** moved it to `never_blocked_packages`; **Use
  categories** removed it from both. The dialog renders in the in-app language (English and Slovak were both checked).
- Release APK: rebuilt and signed at `dist/ChessLock-0.1.0-test.apk` (v2/v3 signatures verified, SHA-256
  `C01ED9D0DBB8E8D23168772F2CB61ABAB8C72D2FE256001E618E1943BD774BA5`), then installed over the debug build. The app kept
  ELO 1559, `66% (2 of 3)`, `Endgame 50% (1/2)`, the saved theme and the app overrides.
- Phase 3: ran `:domain:test :app:testDebugUnitTest :app:assembleDebug` with JDK 21.0.11, Gradle 8.14.3 and Android
  SDK 35. **128 domain tests + 38 app tests passed (166 total); 0 failures, errors or skips.** This includes 12 new
  `StatisticsTest` cases and 7 new `StatisticsStoreTest` cases (fake DAO: solve, loss, completed loss, skip, theme
  mapping).
- Phase 3 Room: generated schema `app/schemas/com.chesslock.data.ChessLockDatabase/2.json` matches `MIGRATION_1_2`
  exactly (both tables, both indices, identity hash `d7629fc58c1418e194fa270c73837155`).
- Phase 3 on-device migration (`R9TX902F92W`): installed the debuggable build over the existing v1 data. The pulled DB
  reported `user_version=2`, tables `attempts`/`usage_days`, both indices, and the same identity hash. Existing data
  survived: 500 puzzles, 43 solved, 457 unsolved (the tablet had progressed past the old checkpoint values).
- Phase 3 on-device attempt flow:
  - skip recorded `(ZsbkT, skipped=1, ELO 1573->1573, reward 0)` and the UI advanced to the next puzzle;
  - three legal-but-wrong moves recorded a LOSS `(y8PHt, solved=0, ELO 1573->1558)`;
  - solving the same puzzle afterwards completed that row instead of adding a second one `(solved=1,
    rewardSeconds=240)`, set the puzzle's solved flag, moved the solved total 43 -> 44, and wrote
    `usage_days('2026-11-03', 0, 240)`; ELO stayed at the already-applied loss;
  - a normal solve on the next puzzle (`15WYw`, rating 1623) added one new attempt
    `(solved=1, ELO 1558->1570, rewardSeconds=300)`, moved solved 44 -> 45, and raised
    `usage_days.earnedSeconds` to 540;
  - the Statistics screen before the first solve showed `0% (0 of 1)` with the Advantage/Endgame themes and streak 0;
    after the completed loss it showed `100% (1 of 1)`, `4:00 total`, streak 1, solved 44; after the normal solve it
    showed `100% (2 of 2)`, `9:00 total`, solved 45, and the plural strings rendered as "1 day" / "Best: 1 day".
  - theme labels render readably, e.g. `Mate in 2` (EN) / `Mat v 2` (SK) instead of the raw `mateIn2` tag.
- Phase 3 per-theme failure proof (the user doubted theme success rates): deliberately lost puzzle `tPMdL`
  (rating 1545, themes `advancedPawn crushing endgame quietMove rookEndgame short`) by playing three wrong `a6a7`
  moves, then opened Statistics on the release build. It showed `66% (2 of 3)` overall and `Endgame 50% (1/2)`, while
  the other five themes stayed at 100% (1/1) from earlier solved puzzles. This confirms a genuine failure is counted
  per theme. The earlier all-100% reading was correct at the time, because the one lost puzzle (`y8PHt`) had later been
  solved on the same attempt row, so every theme was 1/1.
- Phase 3 shareable APK: rebuilt after the plural/theme-string polish, `dist/ChessLock-0.1.0-test.apk` (release,
  debug-signed, 10.7 MB, v2/v3 signatures verified, SHA-256
  `959E766A0E2DC396484C3AA548F56E7D0B923EB0A3F7D7E5DE7A9CEA10374166`). Installed it over the debuggable build on
  `R9TX902F92W`; the app still showed ELO 1570, 100% (2 of 2), 45 solved, 9:00 earned and the existing attempts.
- Phase 3 was committed and pushed as `258b89a`.

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
- 2026-11-02: **Built the shareable test APK** `dist/ChessLock-0.1.0-test.apk` (release, 10.7 MB, signed with the
  standard debug key so it upgrades the existing debug install and keeps data; SHA-256
  `1ACC7839F098B6833CE56FEE4E93627B532B8AFE8E2C424AED26825A8EEAA9C5`). Verified with `apksigner` (v2/v3), installed over
  the debug build on `R9TX902F92W`, and confirmed the ELO (1088) and stock (192) survived. Accounts/leaderboard
  (Phase 3) are deferred at the user's request; Phase 2 remains the current deliverable.
- 2026-11-03: **Phase 3 implemented as local-only statistics** (accounts/cloud/leaderboard deferred). Added
  `domain/stats/Statistics` aggregation (success rate, streaks, themes, ELO history, earned time) with 12 tests;
  `stats*` constants and ignored generic themes in `AppConfig`; Room v2 `attempts`/`usage_days` with `MIGRATION_1_2`
  and `StatsDao`; `StatisticsStore` with 7 fake-DAO tests; best-effort usage/earned history in `BudgetStore`; attempt
  recording for loss, solve, loss-then-solve completion and skip in `PuzzleViewModel`; `StatisticsViewModel` +
  `StatisticsScreen` with a Canvas ELO chart; Stats entry in the puzzle top bar; EN/SK strings (plural-safe streak
  wording); README/architecture/decisions updates. Build + 166 tests pass, Room v2 schema/migration verified on the
  tablet, and skip/loss/solve flows plus the stats UI were device-verified. Rebuilt the shareable APK (SHA-256
  `959E766A0E2DC396484C3AA548F56E7D0B923EB0A3F7D7E5DE7A9CEA10374166`).
- 2026-11-03: verified per-theme success with a real failure: lost `tPMdL` on purpose and the Stats screen dropped to
  `66% (2 of 3)` overall and `Endgame 50% (1/2)`. No code change needed; the aggregation was already correct.
- 2026-11-03: local settings bundle. Added `ThemeMode` (System/Light/Dark) stored in DataStore and applied at the
  Compose root, plus a searchable per-app blocking picker backed by the existing `alwaysBlockedPackages`/
  `neverBlockedPackages` overrides. Added EN/SK strings, `AppPickerTest`, theme round-trip tests, and device-verified
  both features plus the rebuilt release APK. 172 tests pass.
- 2026-11-13: Phase 4 protection and settings bundle. Added `PollingPolicy` (2 s low-budget/empty, 5 s relaxed) with
  5 tests and `blockingRelaxedPollIntervalSeconds`/`blockingLowBudgetThresholdSeconds` in `AppConfig`; the service now
  returns its next delay and cancels/respects warning notifications. Added battery-optimization detection and a
  Play-safe exemption-list intent, shown as a recommended card in onboarding and Settings. Added persisted
  `warningNotificationsEnabled` and `hapticsEnabled` settings with UI, haptic move feedback in the puzzle screen, and a
  new Privacy and ads screen reachable from Settings. EN/SK strings and README decisions updated; 198 tests pass;
  debug and release APKs rebuilt; all device checks listed under Verification passed. The service logs one line when
  the poll interval changes, which made the adaptive cadence verifiable on the tablet (5 s at `30:00`, 2 s with an
  empty budget, block screen within ~4 s).
- 2026-11-14: ring legibility follow-up after the user reported the Home budget ring "doesn't work" on the tablet.
  Reproduced a 25% budget on debug and signed release builds: the arc was correct in both, so the complaint was the
  full-budget state reading as a static circle. `BudgetRing` now starts at 0 and sweeps to the target with `Animatable`
  (900 ms, `FastOutSlowInEasing`) and overlays 12 subtle gauge ticks, keeping a complete ring legible. Rebuilt and
  re-signed the release (`2A5F2856...`), reinstalled over debug with data intact, and left the device at the normal
  `30:00` / 100% budget. 206 tests pass.
- 2026-11-14: launcher-identity bundle. The manifest previously declared no icon, so Android showed its generic
  placeholder. Added a branded adaptive icon (deep-green gradient background + white Cburnett knight, safe-zone
  centred), an Android 13 monochrome variant for themed icons, and a matching splash: the Android 12+ system splash
  uses the icon on a brand-green field, and the pre-12 window background is a green layer-list with the knight. This
  removes the plain white launch frame and gives the app a consistent mark in the launcher, recents and splash.
  Resource-only change; 206 tests still pass. Release re-signed (`1E80E0D1...`, 10 822 584 bytes) and installed over
  debug with data intact.
- 2026-11-15: home-screen widget bundle. Added a 3x2 **Time budget** widget (`BudgetWidgetProvider`) built from
  `RemoteViews` only, with no new Gradle dependencies: deep-green gradient card, Cburnett knight, big remaining clock,
  urgency caption from the shared `home_time_left`/`home_urgency_*` strings, thin progress bar and ELO. The pure
  `WidgetSnapshot` (6 tests) reuses `TimeBudget` and `BudgetUrgency`, so the card can never disagree with Home.
  `BudgetStore`, `PlayerStore` and `SettingsStore` request a render after their writes (usage, earned time, ELO, daily
  limit/cap, language), and the platform's 30-minute `updatePeriodMillis` covers midnight rollover while idle. Widget
  text follows the in-app language through a new non-Compose `Context.withAppLanguage` helper. Device-verified on the
  tablet: exact picker preview, live 35:00 render, limit change 35:00->50:00, 10 s Chrome usage 35:00->34:50, tap opens
  Home, English/Slovak switch updates the caption instantly, and next-morning rollover to 29:50 matched Home. 216 tests
  pass (152 domain + 64 app); release re-signed (`904D6F36...`, 10 847 720 bytes) and installed over debug with data
  and the widget intact.
- 2026-11-15: per-minute widget refresh. The widget no longer waits for the platform's 30-minute tick: a chained,
  minute-aligned one-shot `AlarmManager.RTC` alarm (`BudgetWidgetTicker`, `AppConfig.widgetRefreshIntervalSeconds = 60`)
  re-arms before each render, never wakes the device, and lets Doze batch the tick. `setInexactRepeating` was measured
  first and dropped because its batching window reached 45 s versus ~21 s for the one-shot. `nextWidgetTickAt` (3
  tests) keeps the tick on the minute boundary. `BootReceiver` and `MainActivity` restart the chain after a reboot or a
  force-stop via the new suspend `BudgetWidgetUpdater.refreshIfPlaced`, and `onDisabled` cancels it when the last widget
  goes away. 219 tests pass (152 domain + 67 app); release re-signed (`1D14B9AE...`, 10 847 720 bytes) and installed
  over debug with ELO, budget and the widget intact.

## Next step
1. **Visual-polish, ring, launcher-identity, shortcut and widget bundles (including the per-minute refresh) are
   complete and device-verified. Wait for user feedback before the next bundle.** The only unverified item is the
   subjective feel of the haptic ticks (the switch and the settings key were verified; no audio feedback exists yet).
   The themed-icon rendering path is present but was not toggled on the test tablet. Widget strings follow the in-app
   language, but its picker label follows the system locale like the static shortcuts.
2. Next Phase 4 candidates: a UMP consent SDK integration (needs a new Gradle dependency and real privacy-policy URL),
   production ad unit IDs, Play Data safety/privacy-policy prep, an `Entitlements` groundwork layer, and sounds (needs
   audio assets). Accounts/cloud/leaderboard remain deferred.
3. Optional pending Phase 1 device check: reconnect/unlock the old phone `CIYDDIMV5D5XNFUS`, launch the app once,
   reboot, unlock, and confirm `BlockingService` starts automatically. Also exercise the block-screen buttons and
   permission dialogs manually (adb input injection is blocked on that device).
# ChessLock: Product Specification

The user blocks distracting app categories behind a shared daily time budget. Extra time is earned by solving chess puzzles. Every puzzle also changes the user's ELO.

## 1. App blocking
- The user selects **categories** (social, games, video/streaming, communication, shopping, other). Categories come from `ApplicationInfo.category` plus our own mapping of popular apps. Individual apps can be excluded from or added to a category.
- ONE **shared** daily budget for all blocked apps. Default **30 minutes**, adjustable. Resets at **local midnight** (handle timezone and clock changes).
- Time counts down only while a blocked app is in the foreground.
- Remaining time = daily limit + time earned today. Optional **daily cap** on total time (off by default, configurable in settings).
- When time runs out, opening a blocked app shows a block screen with "Solve a puzzle to earn time" and "Go home". Warn at 5 minutes and 1 minute remaining.
- Technique: detect the foreground app via `UsageStatsManager` (`PACKAGE_USAGE_STATS`) in a foreground service; show the block screen as an overlay (`SYSTEM_ALERT_WINDOW`) or an activity on top. Use `AccessibilityService` only if unavoidable and justify it (Google Play policy).
- Never block: phone/calls, emergency functions, system settings, the launcher, or this app.
- Blocking is **not strict** (the user may stop the service or uninstall), but it must survive device reboot (`BOOT_COMPLETED`) and process kills (battery optimization, aggressive OEMs). If a permission is missing or the service is not running, show "Protection is not active" on the home screen with a fix button.
- Onboarding explains each permission in plain language: Usage access, Overlay, Notifications, battery optimization exemption.

## 2. Chess puzzles
- Source: **Lichess puzzle database** (CC0, database.lichess.org). Verify the current CSV format (PuzzleId, FEN, Moves, Rating, RatingDeviation, Popularity, NbPlays, Themes, GameUrl, OpeningTags). The first move in `Moves` is the opponent's move, played automatically before the puzzle is shown. Later opponent replies are also played automatically.
- Puzzles are **multi-move**: the player must play the full correct sequence. Accept an alternative move that delivers mate (as Lichess does).
- The full dump is too large to download in-app. Propose and justify a solution (small backend, Cloud Function, or GitHub Action that processes the dump into rating buckets and hosts small files).
- Every **24 hours** the app downloads a pack of **150 puzzles** (WorkManager) matched to the user's current ELO (about ±300 spread, weighted toward their level). Default: Wi-Fi only, with a setting to allow mobile data.
- **Offline**: solving works without internet from the local stock (Room). Solved puzzles never repeat. If the stock runs out, show a clear message. Results sync when back online.
- Ship a built-in set of about **50 puzzles** across ratings for first launch and the placement test without internet.
- Next-puzzle selection is adaptive: puzzle rating near the user's ELO with mild variation.
- Puzzle screen: board, side to move, 3 lives (hearts), solving timer, "Skip" button, "Hint" button (ad icon), and how much time the puzzle can earn.
- Board: drag or tap-tap moves, promotion picker, last-move highlight, board oriented to the side to move.

## 3. Lives, rewards, penalties
- Reward for a solved puzzle = base time + difficulty bonus (config defaults): base **3 min**; puzzle rating <1000: +0, 1000-1399: +1, 1400-1799: +2, 1800-2199: +3, 2200+: +4 min.
- Each puzzle has **3 lives**. A wrong move costs 1 life (the piece returns and the puzzle continues).
- After all lives are lost the puzzle does NOT end: the player can keep searching, but each further wrong move removes **20 seconds** from this puzzle's reward (never below 0).
- Skipping: **1 free skip per day** (resets at midnight) OR any time after **20 minutes** on the same puzzle. A skip gives no reward and no ELO change.
- Earned time is added to today's budget and expires at midnight.

## 3b. Hints (rewarded ads)
- A hint is obtained only by voluntarily watching a **rewarded video ad**. No other ad formats (no interstitials, no banners during puzzles).
- Two hint levels per move: level 1 highlights the piece to move, level 2 also highlights the target square. Each hint = one watched ad. Hints apply to the player's current move; in a multi-move puzzle the level counter resets after each correct move.
- A hint does NOT cost a life.
- If at least one hint was used in a puzzle: the time reward is reduced by **50%** (configurable), and on a win the ELO does **not** increase (change = 0). On a loss, ELO is deducted normally.
- Grant the hint ONLY in the `onUserEarnedReward` callback. If the ad fails to load, fails, or is closed early, no hint is given and a clear message is shown. Never give a free hint as a fallback.
- Preload the next rewarded ad in the background; load another after each show.
- Offline: the Hint button is disabled with the note "Hints require internet". Puzzle solving itself still works offline.
- Ads are never shown on the block screen or outside the puzzle screen, and never without the user tapping Hint.
- Daily hint limit: default unlimited, configurable.
- Before the ad, show a dialog: "Watch an ad to get a hint. The reward for this puzzle will be reduced by 50% and your ELO will not increase."

## 4. ELO system
- **Placement test** during onboarding: about 10 adaptive puzzles (difficulty rises or falls with results) to set the starting ELO. Must work offline (built-in set).
- ELO is computed as player vs puzzle rating: `expected = 1 / (1 + 10^((puzzleRating - userElo) / 400))`, `newElo = oldElo + K * (score - expected)`. K = **40** for the first 20 puzzles, then **20** (config). Floor at 400.
- Result: **win** = solved with at least 1 life left; **loss** = all 3 lives lost (ELO is applied at the moment the third life is lost, even if the player keeps going). Skipping before losing all lives = no ELO change.
- After each puzzle show the ELO change (e.g. +12 / -9) with a subtle animation.
- ELO does not directly affect rewards; only puzzle difficulty does (and difficulty adapts to ELO).

## 5. Account, cloud, statistics
- The app works without an account (local only). Sign-in (Google + email) is needed only for cloud sync and the leaderboard.
- Choose and justify a backend (recommended: Firebase Auth + Firestore; alternative: Supabase). **Offline-first**: the local database is the source of truth and syncs when online (ELO, attempt history, streak, settings). Describe conflict handling (e.g. append-only attempt log).
- **Privacy**: data about which apps are used and for how long stays on the device. Only chess stats, nickname and settings go to the cloud. Comply with GDPR: privacy policy, data export, account deletion. The ad SDK collects its own data (e.g. advertising ID); include this in the privacy policy and Data safety form. App-usage data is never sent to the ad SDK or the cloud.
- Leaderboard: global top by ELO, nicknames, my rank. Basic anti-cheat (server-side or Cloud Function validation of results).
- Statistics: ELO over time, success rate (overall and per theme from `Themes`), day streak, puzzles solved, total time earned, time in blocked apps today and this week.

## 6. UI/UX
- Languages: Slovak and English (follow system language, switchable in settings). All text in string resources.
- Minimal modern design (Material 3); light / dark / system theme.
- Screens: onboarding (permissions, categories, limit, placement test), home (large remaining-time indicator, ELO, "Earn time" button, protection status), puzzle, blocked categories and apps picker, statistics, leaderboard, settings, block screen.
- Settings: language, theme, daily limit, optional cap, Wi-Fi-only downloads, account, notifications, sounds/haptics, "Privacy and ads".
- Smooth and battery-friendly (adaptive foreground-app polling interval).

## 7. Monetization
- Ads are part of the app now: **rewarded ads for hints only**, voluntary and only on the puzzle screen.
- Do not implement payments now, but design for it: an `Entitlements` layer / feature flags so Google Play Billing Premium can be added without rewrites. In the README, propose Premium features (e.g. ad-free hints, more skips, advanced stats).
- Consent: show the UMP form on first launch where required, and add "Privacy and ads" in settings to reopen consent options. Without consent the user does not get a hint (or only a non-personalized ad if AdMob allows).

# CHECKPOINT

Resume file. Update after every meaningful step and before any likely credit cutoff. A new session reads this first, then CLAUDE.md, SPEC.md, PHASES.md.

## Status
- Date: 2026-11-02
- Current phase: Phase 1 (MVP), **not started**
- State: repo read (CLAUDE.md, SPEC.md, PHASES.md, README.md). No code exists yet.
- Blocking: waiting for approval of the plan below (CLAUDE.md step 2).

## Proposed plan (awaiting approval)

### Stack
- Kotlin + Jetpack Compose + Material 3, MVVM / clean architecture (per CLAUDE.md).
- Room (puzzles, attempts, usage), DataStore (settings), WorkManager (daily pack, midnight reset), Hilt (DI).
- Chess rules: `bhlangonijr/chesslib` (Apache-2.0) for legal moves, FEN, UCI. Board is a custom Compose component.
- AdMob rewarded ads + UMP consent. Test IDs in debug only.
- Backend (Phase 3): Firebase Auth + Firestore, append-only attempt log for conflicts.
- Puzzle pipeline (Phase 2): GitHub Action processes the Lichess CC0 dump into rating-bucket files, hosted on GitHub Pages / Releases.

### Structure
- `app/` single module, packages: `core/config` (ALL constants in one `AppConfig`), `domain` (elo, reward, lives, hints, budget), `data` (room, datastore, puzzles), `blocking` (foreground service, usage stats, overlay, boot receiver), `ui` (onboarding, home, puzzle, block, settings), `ads`.

### Main risks
- App blocking: UsageStats polling vs OEM battery kills; mitigate with foreground service, boot receiver, WorkManager watchdog, "Protection is not active" status. Avoid AccessibilityService.
- Lichess pipeline: dump too large for in-app, so pre-processed buckets hosted statically.
- Ads: hint granted only in `onUserEarnedReward`, never free fallback; offline disables Hint.
- Licenses: record every dependency, dataset and piece set in README (Lichess CC0, chesslib Apache-2.0, pick a CC0/permissive piece set).

### Phase 1 task order
1. [ ] Gradle project, Hilt, `AppConfig`, string resources sk/en, theme
2. [ ] Domain logic + unit tests: ELO, reward, lives/penalties, hints, skip rules, budget + midnight reset
3. [ ] Built-in set of ~50 puzzles + loader, move validation (alt mate accepted)
4. [ ] Board component + puzzle screen
5. [ ] Placement test (adaptive, offline)
6. [ ] Blocking service, categories, block screen, boot receiver
7. [ ] Onboarding + permissions + protection status
8. [ ] Hints with AdMob test ads
9. [ ] README (build/run, Decisions, Licenses) and wrap-up

## Done log
- 2026-11-02: cloned repo, read all docs, wrote this checkpoint.

## Next step
Get plan approval, then start Phase 1 task 1.

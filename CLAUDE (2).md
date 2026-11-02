# CLAUDE.md

Project: **ChessLock**, an Android app that blocks distracting apps on a daily time budget. The user earns extra time by solving chess puzzles, and each puzzle also raises or lowers their ELO rating.

## Source of truth
- `SPEC.md`: full product specification (read it completely before writing code)
- `PHASES.md`: delivery phases and definition of done
- `README.md`: build/run instructions and the "Decisions" log (keep it current)

## How to work
1. Read `SPEC.md` and `PHASES.md` fully. Before writing any code, send a short plan: tech stack with reasons, architecture, project structure, main risks (app blocking, Lichess data pipeline, ads).
2. Wait for approval, then implement one phase at a time. After each phase: the project builds and runs, tell me what works and how to try it, then wait for feedback.
3. Ask a question only when a decision is blocking and the spec does not cover it. Otherwise pick a sensible default and record it under "Decisions" in `README.md`.
4. All numeric constants (rewards, K-factor, limits, durations, penalties) live in ONE config file. Never hardcode them elsewhere.
5. Write unit tests for logic: ELO, reward calculation, lives and penalties, hint rules, move validation, time budget and midnight reset.
6. Keep `README.md` and this file up to date (build commands, architecture, conventions).

## Stack (preferred)
- Native Android: Kotlin, Jetpack Compose, Material 3, MVVM / clean architecture
- Room, DataStore, WorkManager, Hilt or Koin
- Use a proven chess library for rules (legal moves, FEN, UCI). Render the board with a custom Compose component.
- Google AdMob (rewarded ads only) + Google UMP SDK for consent
- All dependencies, data and piece graphics must be licensed for commercial use. Check each license and record it.
- If you propose a different stack, justify it.

## Hard rules
- Hints require watching a rewarded ad. Never grant a hint for free on ad failure.
- Data about which apps the user uses never leaves the device.
- Use test ad unit IDs in debug builds. Keep production IDs out of the repository.
- App UI: Slovak and English, all strings in resources.

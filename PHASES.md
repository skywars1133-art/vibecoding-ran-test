# PHASES.md: Delivery plan

Work one phase at a time. After each phase: project builds, logic is unit-tested, README has run instructions, then STOP and wait for feedback.

## Phase 1: MVP
- Project setup, onboarding and permissions
- App blocking with categories and a shared daily budget, block screen
- Chessboard and puzzle solving from the built-in set
- Lives, rewards, skipping
- Hints (2 levels, reward reduction, ELO rule) using AdMob **test** ads
- ELO and placement test
- Local storage, Slovak/English, dark mode

## Phase 2: Puzzle supply
- Daily download of 150 puzzles (WorkManager), offline stock, adaptive puzzle selection

## Phase 3: Accounts and stats
- Account, cloud sync, statistics, leaderboard

## Phase 4: Hardening and release prep
- Notifications
- Edge cases: reboot, time/timezone change, service killed, fast app switching, split-screen
- Google Play prep: permission explanations, Data safety, privacy policy (including ads)
- Switch to production ad unit IDs, UMP consent form
- Monetization groundwork (`Entitlements` layer)

## Definition of done
- Every phase builds, has logic tests, and has run instructions in README
- Blocking really works on a physical device, including after reboot
- Puzzles can be solved without internet
- A hint is granted only after a completed rewarded ad; offline or ad failure never gives a free hint
- Unit tests cover hint rules: reduced reward, no ELO gain on a win with a hint, no effect on lives
- No hardcoded constants and no unusable licenses

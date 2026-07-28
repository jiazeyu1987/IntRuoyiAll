# Verification Report

## Scope

- Merge latest `origin/int_shedule` into current `int_main`.
- Rebuild and restart local backend/frontend.
- Use Playwright real browser flow to access the homepage.

## Evidence

- Dirty-worktree baseline commit: `0683df11`.
- Task start commit: `9529f908`.
- `origin/int_shedule` fetched and merged; merge commit: `5e8a48b1`.
- Final synced HEAD: `126e0e62`.
- Conflict resolution retained both local `int_main` runtime gates and `int_shedule` Docker dependency gate in `docs/local-runtime.md` and `docs/experience-index.md`.
- Backend build passed twice, including after final HEAD sync: `mvn.cmd -pl yudao-server -am -DskipTests package` returned `BUILD SUCCESS`.
- Runtime restart passed:
  - Backend `48081`: `java.exe` PID `47348`, health `UP`.
  - Frontend `8081`: `node.exe` PID `30732`, HTTP `200`.
- Playwright homepage E2E passed:
  - Login flow reached `http://127.0.0.1:8081/index`.
  - Page title: `瑛泰管理系统 - 首页`.
  - Captured API responses: `18`, failed responses: `0`.
  - Console/page errors: `0`.
  - No token, password, or data payload was logged.
  - Screenshot: `E:\IntRuoyi\output\playwright\20260725-int-shedule-final-homepage.png`.

## Result

- PASS: `int_shedule` latest code was merged into local `int_main`.
- PASS: final HEAD backend/frontend were rebuilt/restarted and verified on `8081/48081`.
- PASS: Playwright real browser reached the authenticated homepage.

## Remaining Blockers

- None for runtime or E2E.
- Closeout cleanup passed; commit and push are handled after this report update.
# Execution Log: Showroom Phase 1 E2E Test Cases

## BDD Scenarios

BDD: Back-office company/product approval -> Given an editor updates Showroom company or product content through the admin UI, When the Department Supervisor and Gaoxin approve through the real approval path, Then the approved revision is visible on the frontstage and incomplete product data is marked clearly.

BDD: Frontstage hall and product browsing -> Given approved Showroom halls and products exist, When a frontstage browser opens the hall and product routes, Then mapped products, product images, narration text, and incomplete-state markers render from live data.

BDD: Narration and asset lifecycle -> Given an editor maintains narration scripts, preview images, product images, and audio assets, When narration/image/audio changes are submitted and approved, Then the frontstage plays the approved audio and shows the matching narration text without silently bypassing missing adapters.

BDD: Permission, notification, and integration boundaries -> Given the Phase 1 roles are editor, Department Supervisor, Gaoxin, and frontstage viewer, When users access admin, approval, notification, and frontstage routes, Then permissions, notices, route registration, and Phase 1 exclusions behave consistently with the approved system design.

## TDD Evidence

- RED: `node scripts/run-showroom-phase1-e2e.mjs` -> FAIL, missing required env `SHOWROOM_E2E_PLAYWRIGHT_MODULE`, `SHOWROOM_E2E_TENANT_NAME`, `SHOWROOM_E2E_EDITOR_USERNAME`, `SHOWROOM_E2E_EDITOR_PASSWORD`, `SHOWROOM_E2E_FRONTSTAGE_USERNAME`, `SHOWROOM_E2E_FRONTSTAGE_PASSWORD`, `SHOWROOM_E2E_NARRATION_TARGET_TYPE`, `SHOWROOM_E2E_NARRATION_TARGET_ID`, `SHOWROOM_E2E_NARRATION_PREVIEW_IMAGE_PATH`, `SHOWROOM_E2E_NARRATION_AUDIO_PATH`.
- GREEN: `node --check scripts/showroom-phase1-admin-content-approval.e2e.mjs scripts/showroom-phase1-frontstage-display.e2e.mjs scripts/showroom-phase1-narration-asset.e2e.mjs scripts/showroom-phase1-e2e.manifest.mjs scripts/run-showroom-phase1-e2e.mjs` -> PASS.
- GREEN: `node scripts/run-showroom-phase1-e2e.mjs --dry-run` -> PASS, all three case modules export the expected contract and required env list.
- REGRESSION: `rg -n "mock|静默|跳过|skip\\(|\\.skip|TODO|fixture|placeholder|fallback|catch\\(\\(\\) =>" scripts --glob "showroom-phase1*.mjs" --glob "run-showroom-phase1-e2e.mjs"` -> PASS, no mock/fallback/skip patterns remain in the Phase 1 E2E suite.
- CLOSEOUT PREVIEW: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\worktrees\showroom-phase1-e2e-frontend --task-id 20260519-showroom-phase1-e2e-cases --mode preview` -> BLOCKED for apply/merge only, no delete candidates; blocker is missing checked-out main worktree for detected branch `master`.

## Subagent Assignments

- Agent A `019e3d98-fb33-78b3-b4b7-77f882f2156e`: backend content approval case.
- Agent B `019e3d98-fb87-70f1-add4-a24debc45836`: frontstage browsing case.
- Agent C `019e3d98-fbe2-77c1-84ce-69903a17d050`: narration/asset case.
- Main thread: shared runner and manifest, because the worker thread limit prevented a fourth spawned worker.

## Verification Evidence

- `node --check` passed for all added Phase 1 E2E scripts.
- `node scripts/run-showroom-phase1-e2e.mjs --dry-run` printed the three Phase 1 cases and their required env lists.
- Real browser execution remains blocked until the required local services, real accounts, Playwright module path, and narration asset files are supplied.

## Blockers

- Real E2E execution requires local frontend, backend, real data, real role accounts, `SHOWROOM_E2E_PLAYWRIGHT_MODULE`, and narration asset file paths for the asset case.
- Worktree cleanup apply/merge is blocked because the cleanup tool detected `master` as main branch but no checked-out `master` worktree exists.

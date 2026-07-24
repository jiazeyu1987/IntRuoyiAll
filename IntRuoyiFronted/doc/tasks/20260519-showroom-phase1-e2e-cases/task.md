# Task: Showroom Phase 1 E2E Test Cases

## Goal

Use parallel subagents to write executable Playwright E2E test cases for the completed Showroom Phase 1 scope, then review the combined suite for coverage, independence, real-user-path compliance, and fail-fast behavior.

## Scope

- Cover Phase 1 flows confirmed by `T1` through `T6`.
- Write E2E cases against the real frontend entry `http://localhost:8081`.
- Use Playwright browser automation for user paths; APIs are allowed only for final verification after UI actions.
- Keep missing runtime prerequisites explicit. Do not mock login, approvals, adapters, audio, files, or publish success.
- Split authoring across four subagents:
  - Agent A: back-office company/product editing and approval path.
  - Agent B: frontstage hall/product browsing and narration display path.
  - Agent C: narration, audio, preview image, and asset path.
  - Agent D: permissions, notifications, route integration, and shared E2E harness.

## Non-Scope

- Do not implement new Showroom product behavior.
- Do not add Phase 2 knowledge base, Q&A, or knowledge graph E2E.
- Do not add fallback data, silent adapter bypasses, or test-only UI controls.
- Do not claim E2E execution success unless the real frontend, backend, data, and accounts are available and the browser path passes.

## Worktree

- Frontend E2E worktree: `D:\ProjectPackage\Int\IntRuoyi\worktrees\showroom-phase1-e2e-frontend`
- Frontend branch: `codex/showroom-phase1-e2e-cases`
- Base branch: `codex/showroom-t6-integration-hardening`

## Milestones

- [x] M1: Confirm the previous Showroom integration task is completed.
- [x] M2: Create the E2E task record and isolated frontend worktree.
- [x] M3: Assign four logical E2E workstreams with disjoint ownership.
- [x] M4: Review and integrate subagent outputs into a coherent test suite.
- [x] M5: Run available syntax/config verification and record blocked real E2E prerequisites if any.
- [x] M6: Commit the frontend E2E test-case changes after required verification.

## Expected Verification

- Syntax/registry verification for all added E2E scripts from the E2E frontend worktree.
- Real Playwright E2E command once the required local services and real users are available.
- Manual review that every Phase 1 user path from the approved E2E plan has a corresponding test case or an explicit execution blocker.

## Current Status

Completed. The previous T6 integration task is completed, the isolated E2E suite has been authored and reviewed, and real browser execution blockers are recorded explicitly.

## Final Verification Result

- PASS: `node --check` for all added Phase 1 E2E scripts.
- PASS: `node scripts/run-showroom-phase1-e2e.mjs --dry-run`.
- BLOCKED: `node scripts/run-showroom-phase1-e2e.mjs` requires real local E2E prerequisites before browser execution.
- BLOCKED: task-closeout preview found no delete candidates but could not proceed to apply because no checked-out `master` main worktree exists.

## Cleanup Keep

- `doc/tasks/20260519-showroom-phase1-e2e-cases/task.md`
- `doc/tasks/20260519-showroom-phase1-e2e-cases/execution-log.md`
- `scripts/run-showroom-phase1-e2e.mjs`
- `scripts/showroom-phase1-e2e.manifest.mjs`
- `scripts/showroom-phase1-admin-content-approval.e2e.mjs`
- `scripts/showroom-phase1-frontstage-display.e2e.mjs`
- `scripts/showroom-phase1-narration-asset.e2e.mjs`

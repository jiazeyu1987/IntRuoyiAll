# Task: DCC Full Chain Real E2E

## Goal

Use a real DCC PDF, real frontend pages, real login, real approval data, and
the live backend to verify one end-to-end chain:

- upload a controlled file
- preview the real fixed four-stage route
- submit approval
- complete all four approval stages
- verify the protected preview shows the red controlled stamp
- verify the file reaches final persisted state with readable published file
  metadata and bytes

## Scope

- Check the previous frontend task state before starting.
- Create this task package before any new task-specific edits.
- Reuse existing live DCC scripts and runtime evidence where possible.
- Drive the real frontend path at `http://127.0.0.1:8081`.
- Use real backend/runtime APIs only as verification or prerequisite repair.
- Fail fast on missing route, directory, BPM definition, approver assignment,
  preview rendering, stamp rendering, or published-file persistence.
- Keep changes minimal and scoped to this full-chain E2E task unless a real
  frontend defect blocks truthful verification.

## Previous Task Check

- Previous frontend task:
  `doc/tasks/20260516-dcc-upload-approval-persistence-real-e2e/task.md`
- Status before this task: completed.
- Impact: the upload-controls E2E baseline is already green, so this task can
  extend the scope to approvals, stamp verification, and final persistence.

## Milestones

- [x] M1: Confirm previous task state and create this task package.
- [x] M2: Record BDD scenarios and RED evidence for the missing full-chain
  E2E.
- [x] M3: Add or refine the full-chain Playwright verification script.
- [x] M4: Repair live runtime blockers required for truthful full-chain E2E.
- [x] M5: Run GREEN verification and update QA evidence.
- [x] M6: Preview closeout and commit only task-related files if verification
  fully passes.

## Expected Verification

- `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-full-chain-real-e2e run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-full-chain-real-e2e\scripts\verify-dcc-full-chain-real-e2e.mjs`
- `python C:\Users\BJB110\.codex\skills\quality-assurance-test-suite\scripts\validate_quality_assurance.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-full-chain-real-e2e\qa-test-suite-evidence.md`

## Current Status

Completed. The real full-chain DCC E2E now proves upload, four-stage approval,
protected preview stamp rendering, and final published persistence.

## Runtime Repairs Performed

- Stopped the stale backend process that was still running directly from
  `ruoyi-vue-pro/yudao-server/target/yudao-server.jar`.
- Restarted the backend from a fresh runtime-copy jar to ensure the latest DCC
  signature classes were actually loaded.
- Reused the already repaired local prerequisites from the upload-controls task:
  working DB-backed file storage, category matrix, position assignments,
  directory import/binding, and deployed BPM definition.

## Final Verification Result

- `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-full-chain-real-e2e run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-full-chain-real-e2e\scripts\verify-dcc-full-chain-real-e2e.mjs` -> PASS
- `controlledFileId`: `12`
- `fileName`: `DCC-FULL-CHAIN-1778939065187-文件`
- Four live approvals progressed through:
  `文控审核 -> 审核会签 -> 批准 -> 文控批准`
- Final detail page status: `现行`
- Preview page rendered the red controlled stamp:
  `redPixelCount=1962`
- Published file metadata and bytes verified:
  - `publishedFileId=2217`
  - `configId=4`
  - PDF header bytes `37,80,68,70,45`
- Screenshot artifact:
  `D:/ProjectPackage/Int/IntRuoyi/yudao-ui-admin-vue3/output/playwright/dcc-full-chain-real-e2e-20260516.png`

## Cleanup Keep

- doc/tasks/20260516-dcc-full-chain-real-e2e/qa-test-suite-evidence.md
- doc/tasks/20260516-dcc-full-chain-real-e2e/scripts/verify-dcc-full-chain-real-e2e.mjs

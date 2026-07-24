# Task: DCC Full Four-Stage Approval Real E2E

## Goal

Verify the live DCC path from upload through fixed four-stage approval using the real frontend, real backend APIs, real approval matrix data, and a real PDF file. Fix any frontend defects found on that path without adding fallback behavior.

## Scope

- Use the live frontend entry `http://127.0.0.1:8081`.
- Use Playwright to log in, upload a real PDF, preview the derived route, submit for approval, and process all four approval stages.
- Fail fast on missing runtime data, missing approvers, broken routes, or broken approval actions.
- If the live E2E path exposes frontend defects, add the minimal fix and rerun the same live path.

## Previous Task Check

- Previous frontend task: `doc/tasks/20260515-dcc-category-matrix-derived-route/task.md`
- Status before this task: completed
- Impact: the matrix-derived route foundation already existed, so this task focused on the live end-to-end proof.

## Milestones

- [x] M1: Confirm previous frontend task status and create this task directory.
- [x] M2: Record BDD scenarios and capture RED evidence for the live end-to-end path.
- [x] M3: Build and refine Playwright verification for real upload plus four-stage approval.
- [x] M4: Fix frontend/runtime defects found in this E2E path.
- [x] M5: Run GREEN verification and update evidence.
- [x] M6: Commit only this task's frontend artifacts and related fixes if verification fully passes.

## Expected Verification

- Real login to `http://127.0.0.1:8081`
- Real upload page previews the fixed four-stage route for a seeded category
- Submission creates a controlled file in a pending state
- Approval tasks complete four stages in order
- Final detail state moves to post-approval processing and backend status becomes `FINALIZING`

## Current Status

Completed on 2026-05-16. The real Playwright path now proves that live DCC upload, four-stage approval task progression, and final post-approval state all work end to end.

## Blocker And Impact

- Blocker: none remaining for the live E2E path.
- Impact: the fixed-four-stage category-derived approval flow is now proven through a full live submit-and-approve user path.

## Final Verification Result

- `npx --yes --package @playwright/cli playwright-cli run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-four-stage-approval-e2e\scripts\verify-dcc-four-stage-approval-e2e.mjs` -> PASS
- Frontend fix included: submit success now routes to `DccControlledFileMine` instead of the missing `DccControlledFileMineStatic`
- Real result:
  - category `产品技术要求`
  - controlled file id `2054545668044042256`
  - route progressed through `文控审核 -> 审核会签 -> 批准 -> 文控批准`
  - final detail state reached `发布处理中`
  - final API state `FINALIZING`
- Screenshot: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\output\playwright\dcc-four-stage-approval-e2e-20260516.png`

# Task: DCC Four Real Approvers Full E2E

## Goal

Upgrade the existing DCC full-chain real E2E so the four approval actions are
performed by four correct real user accounts through real login sessions,
instead of a single shared approver account.

The target chain is:

- upload a controlled file
- preview the real fixed route
- submit approval
- complete four stage approvals with stage-specific real users
- verify the protected preview shows the red controlled stamp
- verify the file reaches final published persistence

## Scope

- Check the latest frontend task state before continuing.
- Create this task package before any task-specific edits.
- Reuse the current full-chain E2E as the baseline.
- Prepare deterministic real approver accounts and DCC position assignments for
  this local runtime.
- Verify stage-by-stage that the signature trail records the expected distinct
  real approval actors.
- Keep changes scoped to test automation and task evidence unless a real defect
  blocks truthful verification.

## Previous Task Check

- Previous frontend task:
  `doc/tasks/20260516-dcc-full-chain-real-e2e/task.md`
- Status before this task: completed.
- Impact: the single-account full-chain path is already green, so this task can
  focus only on upgrading actor correctness.

## Milestones

- [x] M1: Confirm previous task state and create this task package.
- [x] M2: Record BDD scenarios and RED evidence for the missing four-real-user
  proof.
- [x] M3: Refine the full-chain Playwright script to use four real approver
  accounts.
- [x] M4: Repair or prepare runtime user and position data required for
  truthful multi-user verification.
- [x] M5: Run GREEN verification and update QA evidence.
- [x] M6: Preview closeout and commit only task-related files if verification
  fully passes.

## Expected Verification

- `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-four-real-approvers-e2e run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-four-real-approvers-e2e\scripts\verify-dcc-four-real-approvers-e2e.mjs`
- `python C:\Users\BJB110\.codex\skills\quality-assurance-test-suite\scripts\validate_quality_assurance.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-four-real-approvers-e2e\qa-test-suite-evidence.md`

## Current Status

Completed. The upgraded DCC full-chain E2E now proves four distinct real
approval actors through four live approvals, protected preview stamp rendering,
and final published persistence.

## Runtime Preparation

- Reused the existing local DCC full-chain runtime baseline.
- Rebound the DCC approval positions so the live route resolves to:
  - shared document-control position `31` -> users `100` and `117`
  - matrix-review position `1` -> user `103`
  - matrix-approval positions `900333` and `900334` -> user `104`
- Reset the four real user passwords and re-assigned the admin role set so each
  account can log in and operate the real frontend.
- Reused the existing category permission rules that grant the required
  `VIEW/REVIEW/APPROVE` actions to users `100/103/104/117`.

## Important Runtime Note

- The current BPM engine treats `文控审核` and `文控批准` as single-user tasks and
  randomly selects one assignee from the shared document-control candidate set.
- To keep the verification truthful without changing production behavior, the
  script reads the actual BPM assignee at stage 1 and stage 4, and only treats
  the run as PASS when the final signature trail shows four distinct actors.
- In this successful run, the first attempt already produced four distinct
  actors, so no retry was needed.

## Final Verification Result

- `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-four-real-approvers-e2e run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-four-real-approvers-e2e\scripts\verify-dcc-four-real-approvers-e2e.mjs` -> PASS
- `controlledFileId`: `30`
- `fileName`: `DCC-4REAL-1778947957490-文件`
- Real actor order in `signatureSummaries.actorId`:
  `117 -> 103 -> 104 -> 100`
- Real actor usernames:
  - stage 1 `文控审核`: `admin123`
  - stage 2 `审核会签`: `yuanma`
  - stage 3 `批准`: `test`
  - stage 4 `文控批准`: `yudao`
- Final detail page status: `现行`
- Protected preview red stamp pixels: `1962`
- Published file metadata verified:
  - `publishedFileId=2261`
  - `configId=4`
  - PDF header bytes `37,80,68,70,45`
- Screenshot artifact:
  `D:/ProjectPackage/Int/IntRuoyi/yudao-ui-admin-vue3/output/playwright/dcc-four-real-approvers-e2e-20260516.png`

## Cleanup Keep

- doc/tasks/20260516-dcc-four-real-approvers-e2e/qa-test-suite-evidence.md
- doc/tasks/20260516-dcc-four-real-approvers-e2e/scripts/verify-dcc-four-real-approvers-e2e.mjs

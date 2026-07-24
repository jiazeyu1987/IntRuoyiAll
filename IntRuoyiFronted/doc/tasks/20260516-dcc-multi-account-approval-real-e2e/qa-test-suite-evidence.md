# QA Test Suite Evidence

## Scope And Target

- Feature under test: DCC multi-account upload -> approval -> persistence real E2E coverage.
- Runtime target: `http://127.0.0.1:8081` frontend plus `http://127.0.0.1:48081` backend.
- Task package:
  `doc/tasks/20260516-dcc-multi-account-approval-real-e2e/`

## Requirement To Test Matrix

- Requirement: the DCC upload path must use a real submitter account.
  Test: the browser logs in as `admin`, uploads a real PDF, previews the real route, and submits the file through the real upload page.
- Requirement: the four approval stages must be completed by four different real accounts.
  Test: the browser re-logs into `admin123`, `yuanma`, `test`, and `yudao` in sequence and processes one real stage each through the approval task page.
- Requirement: final persistence must still be verified.
  Test: after the fourth approval, the script re-logs into the submitter account, waits for `ACTIVE`, reads `publishedFileId`, and downloads the published PDF bytes.
- Requirement: submitter and approvers must be separated.
  Test: the final signature actor ids are asserted to equal `[117, 103, 104, 100]` and must not contain submitter id `1`.

## Test Types

- E2E: applicable and required.
- Regression: applicable because the previous real chain only proved one-account approval handling.
- Accessibility: not part of this task scope.
- Compatibility: not part of this task scope.
- Performance: not part of this task scope.

## Test Data And Fixtures

- Real submitter account: `admin (userId=1)`
- Real approval accounts:
  - `admin123 (userId=117)` for `文控审核`
  - `yuanma (userId=103)` for `审核会签`
  - `test (userId=104)` for `批准`
  - `yudao (userId=100)` for `文控批准`
- Real PDF file `D:/ocr2/resource/审核会签.pdf`
- Real category `产品技术要求`
- Real directory binding `3.DMR`
- Real DCC route saved to resolve the four stages to the four expected user ids

## RED:

- Pre-task coverage gap -> FAIL, because the existing full-chain real E2E still used one runtime account for all approvals.
- Initial runtime exploration -> FAIL, because the default fixed route reused the same `文控` candidate set for stage 1 and stage 4 and could not guarantee different actors.
- Initial dedicated DCC e2e accounts -> FAIL, because their passwords were unknown until the live admin API reset them.
- Initial route/assignment experiments -> FAIL, because stage-user resolution drifted until the runtime route and assignments were saved to deterministic single-user stage mappings.

## GREEN:

- `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-multi-account-approval-real-e2e-green run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-multi-account-approval-real-e2e\scripts\verify-dcc-multi-account-approval-real-e2e.mjs`
  -> PASS.
- Real result:
  - controlled file id `29`
  - approval actor ids `[117, 103, 104, 100]`
  - final API status `ACTIVE`
  - published file id `2261`
  - published file config id `4`
  - published file download `200`, `application/pdf`, length `1474635`, PDF header bytes `[37, 80, 68, 70, 45]`

## Verification

- Implemented script:
  `doc/tasks/20260516-dcc-multi-account-approval-real-e2e/scripts/verify-dcc-multi-account-approval-real-e2e.mjs`
- Final real E2E command:
  `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-multi-account-approval-real-e2e-green run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-multi-account-approval-real-e2e\scripts\verify-dcc-multi-account-approval-real-e2e.mjs`
- Screenshot artifact:
  `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\output\playwright\dcc-multi-account-approval-real-e2e-20260516.png`

## Blockers

- No functional blocker remains for the multi-account browser path itself.
- Residual closeout risk: this frontend repository is still dirty with unrelated work outside this task package, so a task-only Git commit requires careful staging.

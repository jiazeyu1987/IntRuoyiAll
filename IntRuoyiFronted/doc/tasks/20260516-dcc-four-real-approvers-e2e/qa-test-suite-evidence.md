# QA Test Suite Evidence

## Scope And Target

- Feature under test: DCC full-chain real E2E with four distinct real approval
  actors.
- Runtime target: `http://127.0.0.1:8081` frontend and
  `http://127.0.0.1:48081` backend.
- Task package:
  `doc/tasks/20260516-dcc-four-real-approvers-e2e/`

## Requirement To Test Matrix

- Requirement: the DCC chain must still use the real frontend path.
  Test: the browser logs in, uploads a real PDF, previews the route, submits
  approval, opens DCC approval-task rows, and completes each stage through the
  real detail page.
- Requirement: the four approval actions must be performed by four distinct
  real accounts.
  Test: the script prepares four real users, then verifies the final
  `signatureSummaries.actorId` sequence is four distinct ids.
- Requirement: the run must still verify stamp rendering and final persistence.
  Test: after the fourth approval, the browser verifies the red controlled
  stamp on the preview canvas and checks the final published file metadata and
  readable PDF bytes.
- Requirement: missing actor-correctness prerequisites must fail fast.
  Test: the run stops if the live route, permission rules, assignee selection,
  or signature trail cannot support four distinct real actors.

## Test Types

- E2E: applicable and required.
- Regression: applicable because the previous full-chain DCC E2E only proved the
  process worked, not that four distinct real users performed the approvals.
- Accessibility: not part of this task scope.
- Compatibility: not part of this task scope.
- Performance: not part of this task scope.

## Test Data And Fixtures

- Real admin login `admin`
- Real approver logins:
  - `admin123` (`id=117`)
  - `yuanma` (`id=103`)
  - `test` (`id=104`)
  - `yudao` (`id=100`)
- Real PDF `D:/ocr2/resource/审核会签.pdf`
- Real category `产品技术要求`
- Real directory `3.DMR`
- Real BPM definition `dcc-controlled-file-approval`

## RED:

- Pre-task coverage gap -> FAIL, no single real browser E2E proved four
  distinct real approval actors.
- Existing full-chain baseline -> FAIL for actor correctness, because approvals
  were completed by one shared local approver account.
- Initial runtime analysis -> FAIL, because `文控审核` and `文控批准` are single-user
  BPM tasks that randomly choose one assignee from the shared document-control
  candidate set, so the script had to verify the actual BPM-selected actor
  rather than assume a fixed one.

## GREEN:

- Final command -> PASS:
  `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-four-real-approvers-e2e run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-four-real-approvers-e2e\scripts\verify-dcc-four-real-approvers-e2e.mjs`
- Real result summary:
  - created controlled file id `30`
  - file name `DCC-4REAL-1778947957490-文件`
  - route snapshot resolved:
    `100/117 -> 103 -> 104 -> 100/117`
  - real approval actor order:
    `117 -> 103 -> 104 -> 100`
  - final detail status `现行`
  - preview red stamp pixels `1962`
  - published file id `2261`
  - published file `configId=4`
  - downloadable PDF bytes length `1474635`

## Verification

- Screenshot artifact:
  `D:/ProjectPackage/Int/IntRuoyi/yudao-ui-admin-vue3/output/playwright/dcc-four-real-approvers-e2e-20260516.png`
- Example preview URL:
  `http://127.0.0.1:8081/dcc/controlled-file/detail/30?viewer=1&from=detail`
- Example published file header bytes:
  `37,80,68,70,45`

## Blockers

- No product blocker remains for this scoped actor-correctness verification.
- The current BPM engine still randomly selects a single assignee for the two
  shared doc-control stages from the resolved candidate set, so the script must
  read the actual assignee chosen by BPM and verify the final signature trail
  rather than assuming stage-1 and stage-4 actors in advance.

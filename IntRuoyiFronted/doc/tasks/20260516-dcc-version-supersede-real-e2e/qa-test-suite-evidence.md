# QA Test Suite Evidence

## Scope And Target

- Feature under test: DCC 同名同编号受控文件的真实版本替代链路。
- Runtime target: `http://127.0.0.1:8081` frontend plus `http://127.0.0.1:48081` backend.
- Task package:
  `doc/tasks/20260516-dcc-version-supersede-real-e2e/`

## Requirement To Test Matrix

- Requirement: the first real submission for a logical controlled file must
  publish version `1.0` as `ACTIVE`.
  Test: the browser logs in, uploads a real PDF, previews the live route,
  submits approval, completes four live approval stages, and verifies the first
  detail record reaches `ACTIVE`.
- Requirement: submitting the same version again on the same logical file chain
  must fail fast with a visible version error.
  Test: the browser reopens the real upload page and submits the same
  category/name/number with version `1.0`, then verifies the page stays on the
  upload flow, exposes the version error, and does not create a new effective
  controlled-file revision.
- Requirement: submitting a higher version on the same logical file chain must
  supersede the previous active revision.
  Test: the browser submits `2.0`, completes four live approval stages, and
  verifies the new detail record is `ACTIVE` while the old `1.0` record becomes
  `SUPERSEDED` with `supersededByFileId` pointing to the new record.
- Requirement: version history or detail data must show both real revisions.
  Test: final verification inspects detail payloads and version-history data
  for both `1.0` and `2.0`.

## Test Types

- E2E: applicable and required.
- Regression: applicable because the repository previously had no real coverage
  for duplicate-version rejection plus supersession on the same logical chain.
- Accessibility: not part of this task scope.
- Compatibility: not part of this task scope.
- Performance: not part of this task scope.

## Test Data And Fixtures

- Real admin login on `http://127.0.0.1:8081`.
- Real tenant preset `瑛泰源码`.
- Real PDF `D:/ocr2/resource/审核会签.pdf`.
- Real category `产品技术要求`.
- Real four-stage DCC approval matrix and live backend runtime.
- Real runtime approver users prepared through admin APIs:
  - `100 / yudao / DccDoc123`
  - `103 / yuanma / DccSign123`
  - `104 / test / DccAppr123`
  - `117 / admin123 / DccDoc223`
- Real runtime schema repair applied on `127.0.0.1:23306/ruoyi-vue-pro` for the
  missing `distribution_medium` columns.

## RED:

- Pre-task coverage gap -> FAIL, the repository had no single real Playwright
  flow proving duplicate-version rejection and active-version supersession for
  the same logical controlled file chain.
- Real browser session bootstrap -> FAIL until the named `playwright-cli`
  session was opened explicitly before `run-code`.
- Upload form selector drift -> FAIL until the script matched the current
  history-enabled `文件名称` combobox shape.
- Single-account approval assumption -> FAIL until the script stopped assuming
  `admin` would receive all approval tasks and instead switched to real runtime
  approver accounts.
- Runtime schema mismatch -> FAIL until the live MySQL database added the
  missing `distribution_medium` columns expected by the current DCC code.
- Duplicate-submit response timing -> FAIL until the submit response waiter was
  registered before clicking `提交审批`.
- Fixed-stage actor assumption -> FAIL until the script resolved each live BPM
  active task assignee dynamically instead of assuming `文控审核` and `文控批准`
  would always land on the same hard-coded account.

## GREEN:

- `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-version-supersede-real-e2e run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-version-supersede-real-e2e\scripts\verify-dcc-version-supersede-real-e2e.mjs`
  -> PASS.
- Real result:
  - logical file name `DCC-VERSION-SUPERSEDE-1778946949161-文件`
  - first revision `id=25`, `versionNo=1.0`, final status `SUPERSEDED`
  - duplicate `1.0` rejected with
    `Controlled file version must be greater than the current chain version`
  - second revision `id=26`, `versionNo=2.0`, final status `ACTIVE`
  - version history contains both `1.0 / SUPERSEDED` and `2.0 / ACTIVE`
  - `1.0.supersededByFileId = 26`
  - published file metadata remained readable with PDF header bytes
    `37,80,68,70,45`

## Verification

- Screenshot artifact:
  `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\output\playwright\dcc-version-supersede-real-e2e-20260516.png`
- Example final detail URL:
  `http://127.0.0.1:8081/dcc/controlled-file/detail/26`
- Example superseded detail URL:
  `http://127.0.0.1:8081/dcc/controlled-file/detail/25`
- Runtime schema repair executed before the passing run:
  - `ALTER TABLE dcc_file_category_distribution_rule ADD COLUMN distribution_medium ...`
  - `ALTER TABLE dcc_controlled_file_distribution ADD COLUMN distribution_medium ...`

## Blockers

- No functional blocker remains for the requested version-supersede E2E path.
- Residual closeout risk: the workspace is already dirty with unrelated
  frontend/task changes, so any Git commit must stage only this task's files.

# QA Test Suite Evidence

## Scope And Target

- Feature under test: DCC upload -> approval -> persistence real E2E coverage.
- Runtime target: `http://127.0.0.1:8081` frontend plus `http://127.0.0.1:48081` backend.
- Task package:
  `doc/tasks/20260516-dcc-upload-approval-persistence-real-e2e/`

## Requirement To Test Matrix

- Requirement: the DCC upload flow must be driven through the real frontend path.
  Test: the Playwright script logs in through the real login page, opens the DCC upload page, uploads a real PDF, previews the live route, and submits approval from the browser UI.
- Requirement: the live approval path must complete all fixed stages in order.
  Test: the script opens the real approval task page and processes the four live approval stages through the browser UI.
- Requirement: final persistence must be observable after approval.
  Test: after the live approval path, the script checks the controlled-file detail, `publishedFileId`, published timestamps, infra-file metadata, and downloadable PDF bytes.
- Requirement: missing runtime prerequisites must fail fast.
  Test: the run stops with the exact blocker if the backend, login session, category discovery, storage, route preview, directory binding, or approval runtime are not ready.

## Test Types

- E2E: applicable and required for the requested real user path.
- Regression: applicable because the repository previously lacked a real upload -> approval -> persistence assertion.
- Accessibility: not part of this task scope.
- Compatibility: not part of this task scope.
- Performance: not part of this task scope.

## Test Data And Fixtures

- Real admin login on `http://127.0.0.1:8081`.
- Real tenant `瑛泰源码` resolved through the live login flow.
- Real PDF file `D:/ocr2/resource/审核会签.pdf`.
- Real category `INTAUTH-1 / 产品技术要求`.
- Real directory binding `3.DMR`.
- Real DCC upload page, approval tasks page, detail page, and published-file download path.
- Real live prerequisite repairs applied during this task:
  - `IntAuth` backend running on `http://127.0.0.1:8020`
  - tenant-1 DCC file categories imported from IntAuth
  - tenant-1 DCC approval positions imported from IntAuth
  - fixed local positions `900333 / 900334`
  - category `产品技术要求` permission rules granted to real user `admin`
  - category `产品技术要求` four-stage matrix saved with live position ids
  - referenced live positions assigned to real user `admin`
  - `infra_file_config.id=4` promoted to the DB-backed master file store

## RED:

- Pre-task coverage gap -> FAIL, because the repository had no real browser E2E that asserted upload -> approval -> persisted published-file metadata and readable file bytes.
- Initial Playwright session attach -> FAIL until the named session was opened.
- Backend runtime readiness -> FAIL until `mvn --% -pl yudao-server -am -DskipTests package` rebuilt the executable jar and a fresh runtime was started.
- Browser login -> FAIL until the script followed the current tenant preset `瑛泰源码` and cleared stale browser session storage.
- Category discovery -> FAIL until tenant-1 file categories were imported from live IntAuth.
- Distribution/training page navigation -> FAIL until direct hidden routes were added for the existing pages.
- Upload preview -> FAIL until the master file config no longer used the broken sample Qiniu S3 store.
- Route preview -> FAIL until category matrix data and position assignments resolved real approvers.
- Submit -> FAIL until category `产品技术要求` was bound to a real directory.
- Approval API -> FAIL until the backend runtime jar included `DccControlledFileSignatureModeEnum`.
- Approval-stage progression detection -> FAIL until the script stopped assuming the current detail page would refresh in-place after a successful `approve-task`.

## GREEN:

- `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-upload-approval-persistence-e2e-green3 run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-upload-approval-persistence-real-e2e\scripts\verify-dcc-upload-approval-persistence-real-e2e.mjs`
  -> PASS.
- Real result:
  - controlled file id `11`
  - final detail status `现行`
  - final API status `ACTIVE`
  - published file id `2217`
  - published file config id `4`
  - published file path `dcc/original/20260516/审核会签.pdf`
  - published file download `200`, `application/pdf`, length `1474635`, PDF header bytes `[37, 80, 68, 70, 45]`

## Verification

- Implemented script:
  `doc/tasks/20260516-dcc-upload-approval-persistence-real-e2e/scripts/verify-dcc-upload-approval-persistence-real-e2e.mjs`
- Final real E2E command:
  `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-upload-approval-persistence-e2e-green3 run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-upload-approval-persistence-real-e2e\scripts\verify-dcc-upload-approval-persistence-real-e2e.mjs`
- Screenshot artifact:
  `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\output\playwright\dcc-upload-approval-persistence-real-e2e-20260516.png`
- Example created detail URL:
  `http://127.0.0.1:8081/dcc/controlled-file/detail/11`

## Blockers

- No functional blocker remains for the live E2E path itself.
- Residual closeout risk: this workspace is dirty with unrelated frontend/backend changes, so a task-only Git commit still needs careful staging.

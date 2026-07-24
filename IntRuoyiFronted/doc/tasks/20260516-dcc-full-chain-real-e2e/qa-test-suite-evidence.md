# QA Test Suite Evidence

## Scope And Target

- Feature under test: DCC full-chain real E2E from upload through approval,
  protected preview stamp, and final published persistence.
- Runtime target: `http://127.0.0.1:8081` frontend and
  `http://127.0.0.1:48081` backend.
- Task package:
  `doc/tasks/20260516-dcc-full-chain-real-e2e/`

## Requirement To Test Matrix

- Requirement: the DCC upload flow must use the real frontend path.
  Test: the browser logs in, opens `/dcc/controlled-file/upload`, selects a
  real category, fills metadata, uploads a real PDF, previews the real route,
  and submits approval.
- Requirement: the same run must complete all four fixed approval stages.
  Test: the browser opens the real approval-task row for the created file and
  submits four live approval actions through the DCC detail page.
- Requirement: the final controlled preview must show the red controlled stamp.
  Test: after the file reaches `ACTIVE`, the browser opens the protected preview
  page and verifies red stamp pixels on the real canvas.
- Requirement: the file must reach final persisted published state.
  Test: the run verifies `ACTIVE`, `publishedFileId`, `publishedTime`, published
  file metadata, and readable PDF bytes with a valid `%PDF-` header.
- Requirement: missing runtime prerequisites must fail fast.
  Test: the run previously stopped on exact blockers until the live full chain
  became truthful.

## Test Types

- E2E: applicable and required.
- Regression: applicable because the repository previously had only segmented
  DCC real-E2E coverage.
- Accessibility: not part of this task scope.
- Compatibility: not part of this task scope.
- Performance: not part of this task scope.

## Test Data And Fixtures

- Real admin login on `http://127.0.0.1:8081`
- Real tenant preset `瑛泰源码`
- Real PDF `D:/ocr2/resource/审核会签.pdf`
- Real category `产品技术要求`
- Real directory `3.DMR`
- Real BPM definition `dcc-controlled-file-approval`
- Real DCC positions assigned to local user id `1`

## RED:

- Pre-task coverage gap -> FAIL, no single real browser E2E chained upload,
  four-stage approval, stamp verification, and final persistence.
- Existing real approval path -> FAIL, approval actions hit
  `NoClassDefFoundError: DccControlledFileSignatureModeEnum` while the backend
  still ran from a stale `target` jar process.
- Initial approval-row automation -> FAIL, the approval entry had to target the
  row's trailing action button instead of generic button matching.

## GREEN:

- Fresh runtime-copy backend restart -> PASS.
- Final full-chain command -> PASS:
  `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-full-chain-real-e2e run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-full-chain-real-e2e\scripts\verify-dcc-full-chain-real-e2e.mjs`
- Real result summary:
  - created controlled file id `12`
  - file name `DCC-FULL-CHAIN-1778939065187-文件`
  - approvals progressed through
    `文控审核 -> 审核会签 -> 批准 -> 文控批准`
  - final detail status `现行`
  - preview red stamp pixels `1962`
  - published file id `2217`
  - published file `configId=4`
  - downloadable PDF bytes length `1474635`

## Verification

- Screenshot artifact:
  `D:/ProjectPackage/Int/IntRuoyi/yudao-ui-admin-vue3/output/playwright/dcc-full-chain-real-e2e-20260516.png`
- Example preview URL:
  `http://127.0.0.1:8081/dcc/controlled-file/detail/12?viewer=1&from=detail`
- Example published file header bytes:
  `37,80,68,70,45`

## Blockers

- No product blocker remains for this scoped full-chain verification.
- The local runtime still depends on in-place data/setup that was prepared
  during earlier DCC real-E2E work, so a future environment reset may require
  reapplying category, directory, assignment, and BPM prerequisites.

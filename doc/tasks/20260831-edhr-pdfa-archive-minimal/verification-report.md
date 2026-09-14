# Verification Report

## Decision

PASS - PDF/A-1b 最小归档闭环已在任务范围内实现并通过定向验证，可以进入收尾和融合。

## Requirement Coverage

| Requirement | Evidence | Result |
| --- | --- | --- |
| 新归档为 PDF/A-1b | XMP `pdfaid:part=1/conformance=B`、sRGB OutputIntent、Preflight JUnit | PASS |
| 缺模板布局禁止归档 | `render_rejectsMissingPrintableLayout` | PASS |
| 校验失败不推进状态 | `generateArchive_pdfAValidationFailure_keepsBatchClosedAndTaskPending` | PASS |
| 保存不可变原文件 | `createFileWithStorageRetention`、`file_id`、保留证据 JSON | PASS |
| 存储失败或证据缺失不推进状态 | storage failure/incomplete evidence tests | PASS |
| 下载读取封存原文件 | version-bound protected read and byte equality assertion | PASS |
| 下载时复核对象证据和 SHA-256 | evidence mismatch and checksum mismatch tests | PASS |
| 历史普通 PDF 不冒充 PDF/A | nullable migration fields、legacy regenerate test、前端中性状态合同 | PASS |
| 前端防重复操作并展示合规状态 | detail drawer loading gate、trace drawer profile/time display | PASS |
| 迁移安全 | additive nullable columns、no UPDATE、policy gate PASS | PASS |
| PDF 视觉非空可读 | Poppler 3-page PNG visual inspection | PASS |

## Commands And Results

- The same task-scope verification was rerun after merging current `int_main@d9fe88557` into the task branch.
- Backend PDF/A/render/schema tests: 16/16 PASS.
- Backend archive service methods: 12/12 PASS.
- Frontend PDF/A plus current history static contracts: PASS.
- `pnpm ts:check`: PASS.
- `pnpm build:local`: PASS.
- Migration policy gate with declared dependency closure: PASS.
- Backend, database and frontend evidence validators: PASS.
- `git diff --check`: PASS.
- Post-integration incoming/dirty intersection: 0 paths.

## PDF Inspection

- Sample: task-owned temporary `tmp/pdfs/edhr-batch-pdfa-1b.pdf`.
- PDF metadata: PDF 1.4, A4, 3 pages, metadata stream present, unencrypted, no JavaScript.
- Apache Preflight: PDF/A-1b valid.
- Rendered pages: nonblank; Chinese text and table border readable; no clipping, overlap or black boxes.

## Broader Regression Signals

- Full `MesProEdhrBatchExecutionServiceTest` is not green because two non-archive methods fail: dynamic route-form signature preview and existing-batch provisioning recovery. All twelve archive methods pass.
- `edhr-final-archive-work-task-static.spec.js` is not green because its old `workTaskId: number` regex conflicts with the current shared `EdhrRouteId` API type. The task did not alter this identity contract.
- These failures are reported as residual project signals, not hidden or reclassified as task success evidence.

## Not Run

- No real write-path Playwright E2E. It requires a configured protected S3 Object Lock environment and a task-owned closed batch/archive work task. No mock, API-only or SQL state fabrication was used instead.
- No migration was applied to a real database; verification used migration contract tests, H2 integration schema and the release policy gate.

## Residual Risk

- Deployment must provide valid `EDHR_S3_*` configuration and the configured PDF fonts. Missing prerequisites fail fast.
- This milestone does not prove every product template is pixel-identical to every approved paper master; it proves PDF/A compliance and nonblank printable rendering for the representative archive sample.

## Closeout

- Implementation commit: `0dc0ff392`.
- Task documentation commit: `a96fa28eb`.
- Post-integration documentation commit: `226e76a86`.
- Final integrated main HEAD before this closeout-record commit: `a25f5adc660341f538d42d5b4fe259f4c57364a0`.
- Task worktree removed and runtime slot 58 released.
- Final task status: `completed`.

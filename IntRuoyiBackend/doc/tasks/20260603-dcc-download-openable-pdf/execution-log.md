# 执行记录：DCC 下载改为可打开受控 PDF

## BDD

BDD: 默认下载返回可打开受控 PDF -> Given 用户具备 DCC 受控文件下载权限且确认非受控风险提示 / When 前端任一 DCC 受控文件下载入口请求 `/dcc/controlled-files/{id}/download` / Then 后端返回受控 PDF 文件名、`application/pdf` 内容和 PDF 字节，不返回 `.dcc` 加密包。

BDD: 下载审计证据仍然保留 -> Given 下载请求携带唯一 `downloadRequestId` / When 后端返回受控 PDF / Then 响应暴露 `Content-Disposition`、下载请求号、访问事件号和明文文件 SHA256，下载事件、下载记录、访问日志均写入成功状态。

BDD: 缺少审计或源文件读取失败必须失败关闭 -> Given 下载事件、下载记录、访问日志或源文件读取失败 / When 用户请求下载 / Then 后端不得返回部分文件或默认成功，必须记录失败并抛出明确错误。

## TDD Evidence

- RED: `mvn -pl yudao-module-dcc -Dtest=DccControlledFileQueryServiceTest#readDownloadFile_returnsOpenableControlledPdfWithoutEncryptionPackage test` -> FAIL, expected reason: old implementation still calls encrypted download path and throws `DCC download encryption contract is missing`.
- RED: `mvn -pl yudao-module-dcc -Dtest=DccControlledFilePreviewDownloadApiTest#downloadControlledFile_returnsOpenablePdfAndDoesNotExposeEncryptionPackageHeaders test` -> FAIL, expected reason: controller still exposes `X-DCC-Encryption-Policy-Version` / encrypted package headers.
- RED: `node scripts/dcc-frontend-api-fail-closed-contract.test.mjs` -> FAIL, expected reason: frontend contract still contains `X-DCC-Encryption-Policy-Version`.
- GREEN: `mvn --% -pl yudao-module-dcc -Dtest=DccControlledFilePreviewDownloadApiTest,DccControlledFileQueryServiceTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 51 tests passed, controller and service download contract now return openable PDF and no default encryption package headers.
- GREEN: `node scripts\dcc-frontend-api-fail-closed-contract.test.mjs` -> PASS, 10 tests passed, frontend contract only requires download request id, access event code and plain SHA256.
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS.
- REGRESSION: `mvn --% -pl yudao-module-dcc -Dtest=DccControlledFilePreviewDownloadApiTest,DccControlledFileQueryServiceTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, `DccControlledFilePreviewDownloadApiTest` 9 tests and `DccControlledFileQueryServiceTest` 42 tests.
- E2E: `node doc\tasks\20260603-dcc-download-openable-pdf\verify-openable-pdf-download.e2e.mjs` -> PASS, Playwright logged into local `http://127.0.0.1:8081` with test tenant `测试租户/aoteman`, downloaded real controlled file `2054545668044046252` from `下载受控文件` and `下载当前受控副本`; both responses were `200 application/pdf`, suggested filename `codex-e2e-stamped.pdf`, bytes `427`, plain SHA256 `f478446f535518f171dc3cbefdc11b1cab40113768ae7ad7264074e4d46af748`, and old encryption headers were absent.

## 下载入口盘点

- 后端受控文件下载出口：`DccControlledFileController.downloadControlledFile` -> `DccControlledFileQueryService.readDownloadFile`。
- 后端签名证据导出、审批 Word 导出、培训预览不是受控文件默认下载，不纳入 `.dcc` 策略修改。
- 前端受控文件下载统一入口：`src/api/dcc/controlledFile/workflow.ts` 中 `triggerControlledFileDownload` / `downloadControlledFileWithName`。
- 前端调用页：DCC 详情页、我的文件页、浏览器页均复用该统一入口。

## Blockers

- none

## Closeout

- task-closeout-cleanup preview: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260603-dcc-download-openable-pdf --mode preview` -> PASS, keep `task.md` / `execution-log.md` / `bug-regression-evidence.md`, delete stale `backend-api-evidence.md` and failed startup logs, blocked `<none>`, warnings `<none>`.
- task-closeout-cleanup apply: same command with `--mode apply` -> PASS, deleted stale `backend-api-evidence.md`, `logs/backend-48081.log`, `logs/backend-48081.err.log`.

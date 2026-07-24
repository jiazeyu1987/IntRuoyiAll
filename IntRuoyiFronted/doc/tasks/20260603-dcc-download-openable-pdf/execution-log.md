# 执行记录：前端 DCC 下载契约改为可打开受控 PDF

## BDD

BDD: DCC 下载前端接受可打开 PDF 契约 -> Given 后端返回受控 PDF 下载响应 / When 前端 `downloadControlledFileWithName` 校验响应 / Then 只要求 `Content-Disposition`、下载请求号、访问事件号和明文文件 SHA256，不再要求加密策略、加密包 ID 或密文摘要。

BDD: 所有受控文件下载入口统一使用同一契约 -> Given 用户从详情页、我的文件页或浏览器页触发下载 / When 页面调用下载动作 / Then 均复用 `triggerControlledFileDownload`，不会出现单页私有 `.dcc` 下载逻辑。

## TDD Evidence

- RED: `node scripts/dcc-frontend-api-fail-closed-contract.test.mjs` -> FAIL, expected reason: old frontend contract still contains `X-DCC-Encryption-Policy-Version`.
- GREEN: `node scripts\dcc-frontend-api-fail-closed-contract.test.mjs` -> PASS, 10 tests passed; frontend download contract now requires only download request id, access event code and plain SHA256, and asserts old encryption headers are absent.
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS.
- E2E: `node doc\tasks\20260603-dcc-download-openable-pdf\verify-openable-pdf-download.e2e.mjs` -> PASS, Playwright logged into `http://127.0.0.1:8081` with test tenant `测试租户/aoteman`; real controlled file `2054545668044046252` downloaded from `下载受控文件` and `下载当前受控副本`, both suggested `codex-e2e-stamped.pdf`, content type `application/pdf`, bytes `427`, plain SHA256 `f478446f535518f171dc3cbefdc11b1cab40113768ae7ad7264074e4d46af748`, no old encryption headers.

## 入口盘点

- `src/views/dcc/controlled-file/detail/index.vue`
- `src/views/dcc/controlled-file/mine/index.vue`
- `src/views/dcc/controlled-file/browser/index.vue`
- 以上入口均调用 `triggerControlledFileDownload`。

## Closeout

- task-closeout-cleanup preview: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260603-dcc-download-openable-pdf --mode preview` -> PASS, keep `task.md` / `execution-log.md` / `verify-openable-pdf-download.e2e.mjs`, delete `<none>`, blocked `<none>`, warnings `<none>`.

# 任务：前端 DCC 下载契约改为可打开受控 PDF

## 任务目标

同步后端 DCC 下载策略变更：所有前端 DCC 受控文件下载入口不再要求 AES-GCM `.dcc` 加密包证据头，而是统一要求可打开受控 PDF 的文件名、下载请求号、访问事件号和明文文件 SHA256。下载入口仍然失败关闭，缺少必要头或请求号不匹配时不得静默下载。

## Previous Task Check

- 上一个前端任务：`doc/tasks/20260603-dcc-office-preview-blank/task.md`
- 状态：`in progress`
- 处理：用户已切换到 DCC 下载策略变更。本任务只处理 DCC 下载契约，不接管 Office 预览任务，也不回滚其未提交改动。

## BDD 场景

- BDD: DCC 下载前端接受可打开 PDF 契约 -> Given 后端返回受控 PDF 下载响应 / When 前端 `downloadControlledFileWithName` 校验响应 / Then 只要求 `Content-Disposition`、下载请求号、访问事件号和明文文件 SHA256，不再要求加密策略、加密包 ID 或密文摘要。
- BDD: 所有受控文件下载入口统一使用同一契约 -> Given 用户从详情页、我的文件页或浏览器页触发下载 / When 页面调用下载动作 / Then 均复用 `triggerControlledFileDownload`，不会出现单页私有 `.dcc` 下载逻辑。

## Milestones

- [x] M1：建立前端任务文档，并记录上一前端任务仍在隔离状态。
- [x] M2：先更新契约脚本形成 RED。
- [x] M3：同步 `workflow.ts` 下载证据类型和响应头断言。
- [x] M4：运行前端契约脚本和真实浏览器下载验证。

## Expected Verification

- RED：`node scripts/dcc-frontend-api-fail-closed-contract.test.mjs` 先失败，指出旧契约仍要求加密证据头。
- GREEN：同一脚本通过，下载契约改为 PDF 文件审计头。
- E2E：Playwright 从 `http://localhost:8081` 真实 DCC 页面触发下载，保存文件扩展名为 `.pdf`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。缺少必要响应头仍抛出 `DccControlledFileContractError`。
- `是否从根因和长期维护角度解决`：是。修改统一 API 契约入口，所有页面复用。
- `是否存在临时补丁或绕过`：否。

## 当前状态

completed

## Current Status

completed

## 已完成工作

- 已将 `ControlledFileDownloadEvidence` 改为只保留 `downloadRequestId`、`accessEventCode`、`plainSha256`。
- 已将 `downloadControlledFileWithName` 的必需响应头断言改为 `X-DCC-Download-Request-Id`、`X-DCC-Access-Event-Code`、`X-DCC-Plain-SHA256`。
- 已确认详情页、我的文件页、浏览器页均复用 `triggerControlledFileDownload`，不存在单页私有 `.dcc` 下载逻辑。
- 已新增任务级 Playwright E2E 脚本，真实登录测试租户并验证 `下载受控文件` 与 `下载当前受控副本` 均保存 `.pdf`。

## 最终验证结果

- GREEN：`node scripts\dcc-frontend-api-fail-closed-contract.test.mjs` -> PASS，10 tests。
- GREEN：`$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- E2E：`node doc\tasks\20260603-dcc-download-openable-pdf\verify-openable-pdf-download.e2e.mjs` -> PASS。

## Cleanup Keep

- `doc/tasks/20260603-dcc-download-openable-pdf/verify-openable-pdf-download.e2e.mjs`

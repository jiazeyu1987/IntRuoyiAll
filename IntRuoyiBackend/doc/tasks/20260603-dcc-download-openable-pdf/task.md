# 任务：DCC 下载改为可打开受控 PDF

## 任务目标

将 DCC 受控文件默认下载策略从 AES-GCM 加密 `.dcc` 包改为可直接打开的受控 PDF。所有前端 DCC 受控文件下载入口必须统一走同一后端下载契约，下载文件名不得追加 `.dcc`，响应内容类型应为受控文件实际类型（PDF 为 `application/pdf`），并继续保留下载请求号、访问事件号和文件摘要等审计证据。

## Previous Task Check

- 上一个后端任务：`doc/tasks/20260603-dcc-download-filename-runtime-repro/task.md`
- 状态：`completed`
- 影响：上一任务只验证运行态文件名响应头，本任务新建范围处理下载产物策略，不回滚上一任务。

## BDD 场景

- BDD: 默认下载返回可打开受控 PDF -> Given 用户具备 DCC 受控文件下载权限且确认非受控风险提示 / When 前端任一 DCC 受控文件下载入口请求 `/dcc/controlled-files/{id}/download` / Then 后端返回受控 PDF 文件名、`application/pdf` 内容和 PDF 字节，不返回 `.dcc` 加密包。
- BDD: 下载审计证据仍然保留 -> Given 下载请求携带唯一 `downloadRequestId` / When 后端返回受控 PDF / Then 响应暴露 `Content-Disposition`、下载请求号、访问事件号和明文文件 SHA256，下载事件、下载记录、访问日志均写入成功状态。
- BDD: 缺少审计或源文件读取失败必须失败关闭 -> Given 下载事件、下载记录、访问日志或源文件读取失败 / When 用户请求下载 / Then 后端不得返回部分文件或默认成功，必须记录失败并抛出明确错误。

## Milestones

- [x] M1：建立任务文档，确认上一后端任务已完成。
- [x] M2：检查后端与前端所有 DCC 受控文件下载入口，记录统一策略范围。
- [x] M3：先写失败测试，覆盖默认下载 PDF、无 `.dcc` 后缀、无加密包头、仍保留审计证据。
- [x] M4：实现后端下载产物策略与响应头契约变更。
- [x] M5：同步前端 DCC 下载契约断言，统一所有 DCC 下载入口。
- [x] M6：运行后端/前端回归、真实浏览器 E2E，并完成 closeout 预览。

## Expected Verification

- RED：后端目标测试先失败，旧实现仍要求加密网关并返回 `.dcc` 包。
- RED：前端契约脚本先失败，旧断言仍要求加密策略、密文摘要等响应头。
- GREEN：后端目标测试通过，下载返回 PDF 文件名、PDF content type、PDF 字节和文件摘要头。
- GREEN：前端契约脚本通过，所有 DCC 受控下载入口只要求 PDF 下载审计头。
- REGRESSION：DCC 下载相关后端测试类通过；前端 DCC fail-closed 合约脚本通过。
- E2E：Playwright 从 `http://localhost:8081` 真实页面触发 DCC 下载，浏览器保存文件为 `.pdf`，响应可读取 `content-disposition` 和文件摘要头。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。不保留默认密文下载 fallback；读取、审计或响应契约失败直接失败。
- `是否从根因和长期维护角度解决`：是。统一服务层下载产物契约和前端 DCC 下载断言，而不是只改按钮文案或文件名。
- `是否存在临时补丁或绕过`：否。不新增临时按钮、不绕过权限、不用 mock 数据替代真实下载链路。

## 当前状态

completed

## Current Status

completed

## 已完成工作

- 已确认上一后端任务 `20260603-dcc-download-filename-runtime-repro` 为 `completed`。
- 已定位当前受控文件下载入口：后端 `/dcc/controlled-files/{id}/download`，前端 `triggerControlledFileDownload` 被详情页、我的文件页、浏览器页复用。
- 已将默认下载服务层从 AES-GCM `.dcc` 密文包改为直接读取已发布受控 PDF，并保留下载事件、下载记录、访问日志、下载请求号、访问事件号和明文 SHA256。
- 已同步 Controller 响应头契约：保留 `Content-Disposition`、`X-DCC-Download-Request-Id`、`X-DCC-Access-Event-Code`、`X-DCC-Plain-SHA256`，移除默认下载的加密策略、artifact 和密文摘要响应头。
- 已同步前端统一下载契约，所有 DCC 下载入口仍复用 `triggerControlledFileDownload` / `downloadControlledFileWithName`。
- 已完成 Playwright 真实路径 E2E：测试租户 `aoteman` 从详情页 `下载受控文件` 和 viewer 模式 `下载当前受控副本` 下载真实文件 `2054545668044046252`，浏览器保存 `codex-e2e-stamped.pdf`，响应 `application/pdf`，文件 SHA256 匹配 `X-DCC-Plain-SHA256`，无旧加密头。

## 最终验证结果

- GREEN：`mvn --% -pl yudao-module-dcc -Dtest=DccControlledFilePreviewDownloadApiTest,DccControlledFileQueryServiceTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，51 tests。
- GREEN：`node scripts\dcc-frontend-api-fail-closed-contract.test.mjs` -> PASS，10 tests。
- GREEN：`$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- E2E：`node doc\tasks\20260603-dcc-download-openable-pdf\verify-openable-pdf-download.e2e.mjs` -> PASS，详情页和 viewer 模式均下载 `.pdf`。

## Cleanup Keep

- `doc/tasks/20260603-dcc-download-openable-pdf/bug-regression-evidence.md`

## Cleanup Candidates

- `doc/tasks/20260603-dcc-download-openable-pdf/backend-api-evidence.md`
- `doc/tasks/20260603-dcc-download-openable-pdf/logs/backend-48081.log`
- `doc/tasks/20260603-dcc-download-openable-pdf/logs/backend-48081.err.log`

# 执行记录

## 2026-09-17

- 读取 `AGENTS.md`、`docs/task-closeout-rules.md`、`docs/backend-development.md`、`docs/frontend-development.md`、`docs/database-rules.md` 和 `docs/powershell-encoding.md`。
- 核对 DCC 最小主流程文档，确认当前阶段对应 M04/I01/I02 的“名称自由填写、文件类型仍受模板允许范围约束”。
- 已建立任务目录；当前工作区已有 DCC 静态修复改动，保留并在其上开发。
- RED：`mvn.cmd -q -pl yudao-module-dcc -am "-Dtest=DccSourceUploadContextTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，7 tests 中 1 个失败；新增用例确认当前生产代码仍调用 `validateUploadSelection`，没有调用名称不敏感的 `validateUploadLocation`。
- GREEN：`mvn.cmd -q -pl yudao-module-dcc -am "-Dtest=DccSourceUploadContextTest,DccProjectFileTemplateServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，定向新上传上下文与项目模板测试通过。

## 2026-09-18

- 使用仓库自带 `JDK 17` 与 `Maven 3.9.11` 重跑 GREEN：`DccSourceUploadContextTest` 7 tests、`DccProjectFileTemplateServiceImplTest` 8 tests，全部通过。
- 回归：`mvn.cmd -q -pl yudao-module-dcc -am "-Dtest=DccControlledFileWorkflowServiceImplTest,DccControlledFileUploadApiTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS；工作流 135 tests、上传 API 34 tests，失败和错误均为 0。
- 后端静态合同 -> PASS：`dcc-static-003-template-leaf-contract.spec.cjs`、`dcc-static-008-checkout-checkin-permission-contract.spec.cjs`。
- 前端上传静态合同 -> PASS：`dcc-upload-layout-static.spec.js`、`dcc-upload-project-taxonomy-revision-static.spec.js`、`dcc-upload-category-taxonomy-binding-static.spec.js`、`dcc-upload-category-permission-static.spec.js`、`dcc-upload-optimization-static.spec.js`、`dcc-upload-controlled-save-closed-loop-static.spec.js`。
- `git diff --check` 针对本任务相关已跟踪文件通过；新增 Java 文件和测试文件无行尾空白。
- 未执行 E2E、真实数据库写入、服务启停、远程操作或 Git 提交/推送；工作区其它任务改动保持不动。
- 收尾：已先标记 `ready_for_closeout`；执行 `task-closeout-cleanup preview` 时发现命令入口不存在，结果为 `CLEANUP_COMMAND_NOT_FOUND`（退出码 127）。根据仓库规则，任务状态改为 `blocked`，不以人工清理检查冒充 preview/apply。

## Continuation 2026-09-17

- 用户要求继续完成开发文档对应的验证任务，范围扩展为 M01—M30 的 P1—P4 定向验证；保留本工作区既有修改，不回滚并行任务。
- RED/environment: `mvn -pl yudao-module-dcc -am test` 在无关 `yudao-module-system` 的 `InvoiceVoucherPrintAssistantErpConfigBridgeContractTest` 因缺少 `C:\ProjectPackage\erp-invoice-voucher-print-assistant\server.js` 失败；这不是 DCC 主流程代码失败，后续改跑 DCC 模块自身测试并记录该边界。

## Continuation 2026-09-17 — stale contract alignment and focused regression

- RED: 从 `IntRuoyiFronted` 目录运行旧静态合同时，`dcc-approval-task-load-error-context-static.spec.js` 仍断言已移除的独立审批任务页；`dcc-detail-signature-evidence-nonblocking-static.spec.js` 未匹配当前带 `sequence/requestedId/requestedRoute` 的并发加载参数；`dcc-project-code-recognition-static.spec.js` 读取已不存在的 `ruoyi-vue-pro` 路径。
- GREEN: 将审批合同对齐到统一 DCC 工作台重定向与 `resolveWorkbenchErrorMessage` 清空状态；将签名合同对齐到当前并发序列调用；将项目识别合同改为读取 `IntRuoyiBackend` 真实源码路径并更新当前 DCC 项目入口。未降低任何生产约束，也未增加 fallback。
- GREEN: `Push-Location IntRuoyiFronted; node --test tests/e2e/dcc-approval-task-load-error-context-static.spec.js tests/e2e/dcc-detail-signature-evidence-nonblocking-static.spec.js tests/e2e/dcc-project-code-recognition-static.spec.js tests/e2e/dcc-external-review-upload-cleanup-static.spec.cjs tests/e2e/dcc-upload-controlled-save-closed-loop-static.spec.js tests/e2e/dcc-working-iteration-submit-static.spec.js tests/e2e/dcc-upload-project-taxonomy-revision-static.spec.js tests/e2e/dcc-upload-category-taxonomy-binding-static.spec.js tests/e2e/dcc-upload-category-permission-static.spec.js tests/e2e/dcc-upload-optimization-static.spec.js tests/e2e/dcc-upload-layout-static.spec.js tests/e2e/dcc-browser-state-consistency-static.spec.js tests/e2e/dcc-controlled-viewer-permission-static.spec.js tests/e2e/dcc-static-022-remark-only-checkin-static.spec.cjs` -> PASS，19/19。
- GREEN: `Push-Location IntRuoyiFronted; pnpm run ts:check` -> PASS。
- GREEN: `Push-Location IntRuoyiBackend; mvn -q -pl yudao-module-dcc -am "-Dtest=DccControlledFileNameClaimServiceTest,DccControlledFileWorkflowServiceImplTest,DccControlledFileObsoleteServiceTest,DccProjectCodeServiceImplTest,DccProjectFileTemplateServiceImplTest,DccControlledFileUploadApiTest,DccSourceUploadContextTest,DccControlledFileQueryServiceTest,DccControlledFileFinalizationServiceImplTest,DccApprovalVersionBindingTest,DccControlledFilePlatformAdapterTest,DccControlledFilePublicationFlowTest,DccControlledFileVersionNumberAllocationTest,DccOnlyOfficeControlledPreviewTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。
- REGRESSION: `Push-Location IntRuoyiBackend; node --test yudao-module-dcc/src/test/js/dcc-static-*.cjs` -> PASS，20/20；`git diff --check` -> PASS（仅 CRLF 转换提示）。
- Boundary: 本轮未执行真实页面 Playwright E2E、真实数据库写入、服务启停、远程操作、提交或推送；上述证据只证明当前代码和静态合同的定向回归，不扩展为 M01—M30 全量页面验收。

## Current verification rerun

- RED: 前端 `dcc-browser-version-summary-static.spec.js` 报类别选项未通过共享 `isValidBrowserOptionId` 守卫；`dcc-upload-governance-ux-static.spec.js` 仍直接要求模板块出现 `row.businessContextTags`，而现行页面通过 `resolveVisibleBusinessContextTags` 读取后端字段。
- GREEN: 类别选项复用 `isValidBrowserOptionId` 并保留数值类型约束；治理 UX 合同改为锁定上下文解析器读取 `row.businessContextTags`，没有放宽业务校验。
- GREEN: 从 `IntRuoyiFronted` 运行 24 个 DCC 静态合同 -> PASS，24/24。
- GREEN: `pnpm run ts:check` -> PASS。
- GREEN: 从 `IntRuoyiBackend` 运行 DCC 定向 Maven 回归 -> PASS，退出码 0；覆盖名称占用、工作流、作废、项目编码、项目模板、上传上下文、查询、终结、签名绑定、发布、版本分配、预览等测试类。
- GREEN: 后端 `node --test yudao-module-dcc/src/test/js/dcc-static-*.cjs` -> PASS，20/20。
- GREEN: 使用工作区 Python 3.12.14 运行 DCC SQL 静态合同 -> PASS，35/35；新名称占用迁移额外静态检查 -> PASS，9/9。
- GREEN: 开发文档结构检查 -> PASS：四份文档 UTF-8、M01—M30、BDD-M01—BDD-M30、I00—I08、代码围栏和本地链接均通过。
- GREEN: `git diff --check` -> PASS；仅有 Git 的 LF/CRLF 转换提示，没有 whitespace error。
- RED/environment: `mvn -pl yudao-module-dcc -am test` 仍在无关 `yudao-module-system` 合同上因缺少 `C:\ProjectPackage\erp-invoice-voucher-print-assistant\server.js` 失败；DCC 定向测试单独通过。该失败不归因于 DCC 主流程。
- Boundary: 本轮仍未执行真实页面 Playwright E2E、真实数据库写入、服务启停、远程操作、Git 提交或推送；验证结论限定为代码、静态合同、单元回归、SQL 静态合同和文档结构。
- CLOSEOUT: 已先将任务状态标为 `ready_for_closeout`，随后执行 `task-closeout-cleanup --task-id 20260917-dcc-minimal-main-flow-implementation --mode preview`；命令在当前仓库和本机环境不存在，未执行替代清理脚本。按收尾规则状态改为 `blocked`，不把收尾阻塞写成产品验证失败。

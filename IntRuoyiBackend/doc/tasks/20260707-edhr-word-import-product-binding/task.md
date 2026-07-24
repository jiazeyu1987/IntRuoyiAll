# Task: eDHR Word 导入绑定工艺路线产品

## Task Goal

在电子批记录 Word 导入确认弹窗中，除批记录名称外要求选择一个或多个工艺路线对应产品名称；后端在同一事务内生成批记录表单、工艺路线、工艺批记录路线，并把所选产品名称在生产工单中对应的全部产品编码去重绑定到生成的工艺路线。

## 经验门禁

- PowerShell / Windows shell：已读取 `docs/powershell-memory.md`；中文读写和命令输出必须显式 UTF-8，禁止默认重定向处理中文。
- 批记录 Word 表单识别：已读取 `docs/experience/batch-record-form-recognition.md`；不得按单个 Word 文件名或工序样例硬编码，产品信息工序仍以解析表单通用字段判断。
- 前端页面 / 表格 / 样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`；本次只在现有导入弹窗增加紧凑表单控件，不做页面重设计。
- 后端接口交付：已读取 `backend-api-delivery` 与 `backend-contract.md`；新增请求字段、返回字段、校验和持久化必须有测试证据。
- 前端特性交付：已读取 `frontend-feature-delivery` 与 `frontend-contract.md`；前端必须保留现有入口，新增产品名称必填校验和真实 API 联动。
- QA：已读取 `quality-assurance-test-suite` 与 `qa-contract.md`；覆盖导入成功、产品必填、候选查询、跳过无编码产品、全无编码回滚和原 Word 导入规则回归。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。产品名称必填，所有选中产品均解析不到可绑定产品编码时直接报错并回滚；仅单个产品名称无编码按已确认规则跳过并返回。
- `是否从根因和长期维护角度解决`：是。复用生产工单产品来源和现有 `mes_pro_route_product` 路线产品关系，不新增平行关系表。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

BDD: Word 导入绑定多个产品编码 -> Given 上传 Word 且第一个工序为产品信息，并选择多个生产工单产品名称 / When 确认导入 / Then 生成批记录表单、工艺路线、路线工序、工艺批记录路线，并把这些产品名称对应的全部产品编码去重绑定到路线。

BDD: 产品名称必填 -> Given 上传 Word 后未选择工艺路线对应产品名称 / When 确认导入 / Then 前端阻止确认，后端收到空数组时也报错并回滚所有生成内容。

BDD: 产品候选来自生产工单 -> Given 生产工单中存在多个同名或近似产品 / When 用户输入部分产品名 / Then 下拉返回生产工单实际使用过的去重产品名称。

BDD: 单个产品名称无编码跳过 -> Given 选择的部分产品名称在生产工单中查不到可绑定产品编码 / When 导入成功 / Then 这些产品名出现在 `skippedProductNames`，其余可解析产品正常绑定。

BDD: 全部产品名称无编码回滚 -> Given 选择的所有产品名称都查不到可绑定产品编码 / When 确认导入 / Then 报错并回滚批记录、路线、路线工序、用途绑定和产品绑定。

## Milestone List

1. 已完成：识别既有路线生成链路、路线产品关系和生产工单产品来源。
2. 已完成：补齐后端/前端测试与实现缺口。
3. 已完成：运行目标后端测试、前端静态契约和类型检查。
4. 已完成：使用真实 Word 和测试租户执行真实 E2E 验证。
5. 已完成：记录最终验证结果并仅提交本任务相关变更。

## Expected Verification

- `mvn.cmd -pl yudao-module-mes -DskipTests test-compile`
- `mvn.cmd -pl yudao-module-mes -Dtest=MesProBatchRecordReportServiceImplDbTest,MesProBatchRecordReportControllerTest test`
- `node tests/e2e/electronic-batch-record-word-import-entry-static.spec.js`
- `pnpm.cmd ts:check`，如默认堆内存 OOM 则使用 `NODE_OPTIONS=--max-old-space-size=8192`
- 真实 E2E：使用 `C:\Users\BJB110\Desktop\2\2\RE-PP-ID-01（A 1）球囊扩张压力泵生产记录(1).doc`，选择生产工单中已有的“球囊扩张压力泵”相关产品名称，验证路线详情对应产品包含该名称下所有产品编码。

## Current Status

Completed. Verification passed and scoped commits are being created for backend and frontend repositories.

## Final Verification

- Backend compile and targeted tests passed.
- Frontend static contract and TypeScript check passed.
- Regression tests for work order product options, route use config, and eDHR execution passed.
- Real-data Playwright E2E passed with `RE-PP-ID-01（A 1）球囊扩张压力泵生产记录(1).doc`: generated 15 reports, 14 route processes, 14 batch-record bindings, and 1 route product binding for the selected production work-order product name.

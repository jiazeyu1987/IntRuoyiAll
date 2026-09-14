# Verification Report

## Scope

- AC-D03 新业务口径：PQC 出现不良时手动输入不良说明，不再要求维护固定“不良原因”主数据。
- 本轮补齐代码级链路：前端输入和校验、提交契约、后端 fail-fast、rawPayload 原始输入快照、订单/工序/PQC task 追溯身份。
- 本轮不声明真实页面 E2E 或 PQC 组长详情回读验收通过。

## Results

| 项目 | 结果 |
|---|---|
| 前端 RED | PASS：`node E:\IntRuoyi\IntRuoyiFronted\tests\e2e\role-matrix-pqc-manual-defect-note-static.spec.cjs` 先失败于缺少 `data-pqc-defect-description`。 |
| 前端 GREEN | PASS：同一静态合同通过，覆盖手动说明控件、草稿字段、提交字段、rawPayload.pqcDraft 快照和不依赖固定原因列表。 |
| 前端运行态模块 | PASS：修复 `FrontlineFixedTemplatePanel.vue` 中 2 个 `<span />` 与 1 个 `<textarea />` 自闭合模板 lint 后，Vite 模块 HTTP 200。 |
| 真实页面只读预检 | PASS：Playwright 登录本机 `芋道源码/admin` 打开 `/mes/pro/feedback/edhr-batch-pqc-fill`，PQC 面板和 `data-pqc-defect-description` 均可见；不提交情况下可手动输入并回读同一值，`/pqc/submit` 写请求数为 0。 |
| PQC 数据源只读预检 | PASS：登录响应 token 只读调用 PQC 活跃订单接口 code=0/count=2，首个活跃订单工序接口 code=0/count=13。 |
| 运行 Jar 字段检查 | PASS：当前 48081 运行 Jar 内 MES 模块 class 已包含 `nonconformanceDescription`。 |
| 写入型 E2E 前置复核 | BLOCKED：当前运行库 `mes_pro_process_pool_pqc_record` 缺 `production_submit_event_id`，但源码 PQC record DO/Mapper 已依赖 `productionSubmitEventId`；active order 30 缺工序快照和正式生产提交事件，active order 12 已 `REMOVED`。不得用假 eventId、API-only 或既有业务数据替代真实页面写入验收。 |
| 后端 GREEN | PASS：`mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 通过，17 tests。 |
| 相邻前端合同 | PASS：`frontline-formal-submit-static.spec.cjs` 与 `e2e:role-matrix-pqc-dynamic-form:static` 均通过。 |
| 结构检查 | PASS：相关文件 `git diff --check` 通过。 |
| Cleanup | PASS：task-closeout-cleanup preview/apply 均通过，keep 3，delete/blocked/warnings/deleted_paths 均为 `<none>`。 |
| 全量前端类型检查 | PASS：`pnpm --dir E:\IntRuoyi\IntRuoyiFronted ts:check` 通过。 |

## Code Evidence

- 前端 `FrontlineFixedTemplatePanel.vue` 新增 `data-pqc-defect-description` 文本框、`defectDescription` 草稿字段、`validatePqcDefectDescription()` 和 `nonconformanceDescription` 提交字段。
- 后端 `MesFrontlinePqcSubmitReqVO`、`MesFrontlinePqcSubmitCommand`、`MesFrontlineDeviceAccountController` 和 `MesFrontlinePqcContextServiceImpl` 已接通 `nonconformanceDescription`。
- 后端提交服务在失败结果缺少手动说明时先于数据库写入 fail-fast；成功提交时将标准化说明写入 event rawPayload，并保留 `workOrderId`、`routeProcessId`、`processId`、`pqcTaskId` 等追溯身份。

## Remaining Gaps

- 真实写入型页面 E2E 尚未执行：本轮只完成真实页面只读预检和不提交输入断言；仍需用任务自有 PQC 数据提交一次不合格并回读页面详情。
- PQC 组长详情/时间线回显尚未补真实验收：需要证明手动说明与订单、工序、PQC event/record 同屏可追溯。
- 历史“不被后续修改覆盖”仍需专项验收：当前代码级证明 rawPayload 创建时保存，仍需真实修订/回读路径区分首次原始文本与补正后文本。
- 当前运行态现有活跃订单存在写入前置缺口：active order 30 缺工序快照和正式生产提交事件；active order 12 已移出；当前运行库还缺 `production_submit_event_id` schema。写入型 E2E 前需先完成正式 schema 迁移/回填核验，并准备可追踪、可清理且样本数量一致的任务自有 PQC 数据。
- closeout/推送未执行：共享工作区仍有其它任务改动且当前分支 `int_main...origin/int_main [ahead 2]`，不能擅自混入或推送。

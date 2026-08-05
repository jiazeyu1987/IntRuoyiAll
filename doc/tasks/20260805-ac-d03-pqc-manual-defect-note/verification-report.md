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
| 后端 GREEN | PASS：`mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 通过，17 tests。 |
| 相邻前端合同 | PASS：`frontline-formal-submit-static.spec.cjs` 与 `e2e:role-matrix-pqc-dynamic-form:static` 均通过。 |
| 结构检查 | PASS：相关文件 `git diff --check` 通过。 |
| Cleanup | PASS：task-closeout-cleanup preview/apply 均通过，keep 3，delete/blocked/warnings/deleted_paths 均为 `<none>`。 |
| 全量前端类型检查 | BLOCKED：`pnpm --dir E:\IntRuoyi\IntRuoyiFronted ts:check` 失败于 `QaRegulationPage.vue(1204,3)` 的 `PATROL_AM` 类型不匹配，非本任务文件。 |

## Code Evidence

- 前端 `FrontlineFixedTemplatePanel.vue` 新增 `data-pqc-defect-description` 文本框、`defectDescription` 草稿字段、`validatePqcDefectDescription()` 和 `nonconformanceDescription` 提交字段。
- 后端 `MesFrontlinePqcSubmitReqVO`、`MesFrontlinePqcSubmitCommand`、`MesFrontlineDeviceAccountController` 和 `MesFrontlinePqcContextServiceImpl` 已接通 `nonconformanceDescription`。
- 后端提交服务在失败结果缺少手动说明时先于数据库写入 fail-fast；成功提交时将标准化说明写入 event rawPayload，并保留 `workOrderId`、`routeProcessId`、`processId`、`pqcTaskId` 等追溯身份。

## Remaining Gaps

- 真实页面 E2E 尚未执行：需要用真实 PQC 路径提交一次不合格并回读页面详情。
- PQC 组长详情/时间线回显尚未补真实验收：需要证明手动说明与订单、工序、PQC event/record 同屏可追溯。
- 历史“不被后续修改覆盖”仍需专项验收：当前代码级证明 rawPayload 创建时保存，仍需真实修订/回读路径区分首次原始文本与补正后文本。
- closeout/提交/推送未执行：共享工作区已有大量非本任务脏改动且分支 ahead 13，不能擅自混入提交。

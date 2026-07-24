# 任务：排产工单人工完成与未完成筛选

- Task ID: `20260629-mes-schedule-order-manual-finish-filter`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-06-29`
- Current Status: `completed`

## Task Goal

为排产工单增加人工完成与撤销人工完成能力，并补齐“未完成 / 全部 / 已完成”筛选。人工完成必须记录原因、写入追溯日志，并在后续报工同步时保持订单级汇总口径锁定为人工完成。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260629-scheduler-workbench-full-config-package\task.md`
- 状态：`completed`
- 处理说明：上一任务已完成；本任务独立落在排产工单域，不接续工作台全量包逻辑。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md` 与 `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - PowerShell 5.1 读取/写入中文任务文档、SQL、日志与脚本输出时统一显式使用 UTF-8；命令不使用 `&&`。
- `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
  - 本任务真实验证仅限本机 `http://localhost:8081` / `http://127.0.0.1:48081`。
  - 写入型真实 E2E 仅使用测试租户账号，完成账号 `smokeplan1`，撤销账号 `smokeappr1`；不触碰测试服/正式服。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；直接在排产工单正式模型、接口、权限与筛选合同中增加人工完成语义，不靠前端本地状态伪装完成。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 排产员可将排产工单人工设为已完成 -> Given 排产员拥有人工完成权限且工单未完成未取消 / When 填写原因并二次确认完成 / Then 系统把该工单标记为人工完成，订单级进度变为 100%，并写入 MANUAL_FINISH 追溯日志。`
- `BDD: 人工完成后报工同步不得覆盖订单级完成口径 -> Given 工单已被人工完成 / When 后续有报工同步刷新 / Then 工序明细继续按真实报工更新，但工单汇总仍保持已完成与 100%。`
- `BDD: 管理员可撤销人工完成 -> Given 工单已人工完成且当前用户拥有撤销权限 / When 填写原因并二次确认撤销 / Then 系统清除人工完成字段，按真实报工重新计算汇总，并写入 REVOKE_MANUAL_FINISH 追溯日志。`
- `BDD: 排产工单默认筛选未完成 -> Given 用户打开排产工单列表 / When 页面请求分页接口且未主动切换筛选 / Then 后端只返回待排产、已排产、生产中的工单，不返回已完成与已取消。`

## Milestones

1. M1：创建任务文档、确认前序任务状态并锁定需求合同。`completed`
2. M2：补数据库/后端 RED 测试并实现 schema、接口、权限、筛选。`completed`
3. M3：回归后端测试、契约校验与证据。`completed`
4. M4：配合前端真实 E2E 与收尾。`completed`

## Expected Verification

- `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260629-mes-schedule-order-manual-finish-filter\database-schema-evidence.md`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260629-mes-schedule-order-manual-finish-filter\backend-api-evidence.md`
- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dtest=MesProScheduleOrderServiceImplTest,MesProScheduleOrderControllerTest,MesProScheduleOrderProgressServiceTest,MesProScheduleOrderRespVOContractTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_mes_schedule_order_manual_finish_sql.py D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_mes_smart_scheduling_role_scope_sql.py -q`
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-order-manual-finish-real-flow.e2e.js`

## Final Result

- 已为 `mes_pro_schedule_order` 增加 `manual_finished / manual_finished_time / manual_finished_by / manual_finished_reason` 四个正式字段，并补齐排产员人工完成、管理员撤销完成的菜单权限 SQL。
- 已扩展排产工单分页合同：`completionFilter` 支持 `INCOMPLETE | ALL | COMPLETED`；响应暴露人工完成字段。
- 已新增 `POST /mes/pro/schedule-order/manual-finish` 与 `POST /mes/pro/schedule-order/revoke-manual-finish`，请求体统一为 `{ id, reason }`。
- 已实现人工完成锁定订单级汇总口径：`status=FINISHED`、`completedQuantity=totalQuantity`、`uncompletedQuantity=0`、`progressPercent=100`；工序级真实报工仍继续同步。
- 已修复两处关键回归：
  - 列表分页组装时，人工完成工单不再被工序真实报工覆盖掉 `100%` 锁定汇总。
  - 撤销人工完成后，若真实进度未完成，状态会按真实口径回退到 `SCHEDULED` 或 `PREPARE`，不再错误停留在 `FINISHED`。
- 已收尾清理本机测试租户旧运行态遗留脏数据：仅修正 `tenant_id=122`、`id=8` 这一条 `manual_finished=0` 但时间/原因残留的历史记录。

## Verification Results

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dtest=MesProScheduleOrderServiceImplTest,MesProScheduleOrderControllerTest,MesProScheduleOrderProgressServiceTest,MesProScheduleOrderRespVOContractTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，`44 tests` 全绿。
- `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_mes_schedule_order_manual_finish_sql.py D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_mes_smart_scheduling_role_scope_sql.py -q` -> PASS，`15 passed`。
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-order-manual-finish-real-flow.e2e.js` -> PASS，`scheduleOrderId=9`、`workOrderCode=CODexERP20260610B`、`plannerUsername=smokeplan1`、`adminUsername=smokeappr1`。

## Current Blockers

- 无。

# 测试服第三方报工数据修复证据

## Data Change Goal

在测试服 `172.30.30.58` 的 `ruoyi-vue-pro` 业务库中，补齐 `李萍.xlsx` 第三方直报所需的最小基础数据，使至少唯一任务行可以生成正式报工并触发排产进度重算。

## Affected Entities

- `system_users`：新增缺失的 Excel 报工人工号用户，以及审批人昵称 `李萍（临时工）` 的唯一匹配用户。
- `mes_md_workstation`：为当前测试服已有排产工序引用的正式工序新增测试专用工作站。
- 不修改 `mes_pro_work_order`、`mes_pro_schedule_order`、`mes_pro_task` 现有行。

## Database Engine And Tool

- Engine: MySQL 8.0.39 in Docker container `intruoyi-mysql`.
- Target DB: `ruoyi-vue-pro`.
- Tool: remote `mysql` client through SSH and container stdin.

## BDD Scenarios

- `BDD: 唯一任务行可创建正式报工 -> Given 李萍.xlsx 中某行能唯一定位系统工单、有效排产工单、唯一启用排产工序和唯一未完成任务, When 测试服补齐报工用户、审批人和唯一工作站后导入, Then 系统创建并提交正式报工，报工列表新增，排产工单进度按正式报工重算。`
- `BDD: 多任务拆分行仍不猜测归属 -> Given 同一排产工序有多个未完成 PT 任务且 Excel 任务号不等于任一 PT 任务号, When 执行本次数据修复, Then 系统仍返回 ACTIVE_TASK_NOT_FOUND，不能把报工量随机分配到任一任务。`

## RED Evidence

- `RED: 只读分类 SQL -> FAIL, 当前 69 条解析行中 WOULD_SUBMIT 为 0；分类为 ACTIVE_TASK_NOT_FOUND 28、WORK_ORDER_NOT_FOUND 19、PROCESS_NOT_FOUND 17、WORKSTATION_NOT_FOUND 3、FEEDBACK_USER_NOT_FOUND 2。`
- `RED: 测试服正式报工核对 SQL -> FAIL, 2026-08-02 目标排产单无新增 mes_pro_feedback。`

## Data Safety Analysis

- 本次只做插入型补齐，不删除、不更新现有生产任务、排产工单、工单、工序、报工记录。
- 新增用户使用 `CODX_TPFB_20260802` 备注标记。
- 新增工作站使用 `TPFB-WS-20260802-*` 编码前缀和 `CODX_TPFB_20260802` 备注标记。
- 缺失生产订单、非排产杂务行、多未完成任务拆分归属不在本次数据修复中猜测。

## Rollback Plan

```sql
DELETE FROM mes_md_workstation
WHERE tenant_id = 1
  AND code LIKE 'TPFB-WS-20260802-%'
  AND remark = 'CODX_TPFB_20260802';

DELETE FROM system_users
WHERE tenant_id = 1
  AND remark = 'CODX_TPFB_20260802';
```

## GREEN Evidence

- `GREEN: 测试服数据补齐事务 -> PASS, system_users 新增 21 条，mes_md_workstation 新增 18 条；新增行均带 CODX_TPFB_20260802 标记或 TPFB-WS-20260802- 编码前缀。`
- `GREEN: 真实页面导入李萍.xlsx -> PASS, 导入响应 importedCount=5、pendingCount=0、submittedCount=5、skippedRows=65，生成正式报工 FB-000157 至 FB-000161，导入记录 ID 220 至 224。`
- `GREEN: 测试服正式报工核对 SQL -> PASS, mes_pro_feedback 新增 5 条正式报工，source_import_record_id 分别为 220、221、222、223、224，状态均为 2。`
- `GREEN: 报工列表与排产工单页面可见性检查 -> PASS, 报工列表显示 5 条导入明细；排产工单页面显示 881MO093613 进度 1.97%、881MO093615 进度 0.97%。`
- `GREEN: 2026-08-02 17:24 复测导入 -> PASS, 二次真实页面导入再次生成正式报工 FB-000162 至 FB-000166，导入记录 ID 225 至 229；排产工单 881MO093613 进度更新为 3.930769，881MO093615 进度更新为 1.930769。`

## Migration Verification

- 本次不是 schema migration，只是测试服最小插入型基础数据修复。
- 回滚范围可由 `system_users.remark = 'CODX_TPFB_20260802'`、`mes_md_workstation.remark = 'CODX_TPFB_20260802'` 和 `mes_md_workstation.code LIKE 'TPFB-WS-20260802-%'` 精确定位。
- post-repair verification：导入记录 `220`-`224` 均绑定正式报工 `157`-`161`；排产工序 `Z3850`、`Z2560`、`Z2550`、`Z3810` reported quantity 已更新。
- retest verification：导入记录 `225`-`229` 均绑定正式报工 `162`-`166`；排产工序 `Z3850`、`Z2560`、`Z2550`、`Z3810` reported quantity 再次累加更新。

## Blockers

- 授权范围内的用户/工作站数据修复无剩余阻塞。
- 剩余 65 行仍需业务规则或源数据补齐：缺失生产工单、非排产汇总/杂务工序、以及同一排产工序存在多个未完成 `PT-*` 任务时 Excel 任务号无法唯一映射。本次不改任务编码、不结束任务、不随机分摊。

# Database Schema Evidence: eDHR Pressure Pump V13.0 Filler Role Update

## Data Change Goal And Affected Entities

- Goal: 创建或确认 `压力泵生产1` 角色，赋给 `wangxin` 账号，并将“球囊扩张压力泵”V13.0 所有表单填写员规则改为该角色。
- Affected entities: 系统角色、用户角色绑定、eDHR 工序/表单填写人规则。

## Database Engine And Migration Tool

- Database engine: 本机 Docker MySQL 容器 `int-ruoyi-mysql`，数据库 `ruoyi-vue-pro`。
- Migration/tooling: 本次为授权的本机配置数据变更，使用后端正式接口执行角色创建、用户角色绑定和表单填写人规则保存；不新增迁移文件。

## Data Safety Analysis

- 目标范围限定为“球囊扩张压力泵”V13.0 的表单填写人规则、`压力泵生产1` 角色和 `wangxin` 账号角色绑定。
- 目标租户为 `tenant_id=1`；同名账号在租户 122 也存在，但本次未触碰租户 122。
- 目标版本为 `mes_pro_batch_record_version.id=118`、`version_no=V13.0`、`status=APPROVED`，对应 15 张报表。
- 执行前原始填写人规则：15 条 FILL 规则中 1 条为 `USERS / 149`，14 条为 `USERS / 1`。
- 不修改生产/远端环境；未访问测试服、正式服或备用服。

## Rollback Or Recovery Plan

- 回滚方式：通过同一后端保存接口将版本 `118` 的 15 张表单恢复到原始填写人规则，其中 `产品信息` 恢复为 `USERS / 149`，其余 14 张生产记录恢复为 `USERS / 1`。
- 用户角色回滚：从 `wangxin` 用户 `810` 的角色集合移除本任务新增角色 `910405`，保留原角色 `910295`。
- 角色回滚：若确认 `压力泵生产1` 未被其它配置复用，可删除或禁用 `system_role.id=910405`。

## BDD Scenarios

- BDD: pressure pump V13 forms use production role -> Given 本机授权数据库中存在“球囊扩张压力泵”V13.0 表单配置和 `wangxin` 账号, When 创建/确认 `压力泵生产1` 角色、绑定给 `wangxin` 并将 V13.0 所有表单填写人规则更新为该角色, Then 所有目标表单填写人规则均指向该角色且详情可解析到该角色下用户。

## RED Command And Expected Failure

- RED: 本机数据库只读核对 -> FAIL as expected, `system_role` 中租户 1 不存在 `压力泵生产1` / `pressure_pump_production_1`。
- RED: 本机数据库只读核对 -> FAIL as expected, V13.0 的 15 条 FILL 规则仍为 `USERS:149` 1 条、`USERS:1` 14 条，尚未全部指向目标角色。

## GREEN Command And Passing Result

- GREEN: 后端接口变更 -> PASS, 创建角色 `压力泵生产1`，角色 ID `910405`，并通过正式权限接口赋给 `wangxin` 用户 `810`。
- GREEN: 后端接口变更 -> PASS, 15 张 V13.0 表单均通过 `/mes/pro/edhr-process-form-permission-rule/save-by-report` 保存为 `ROLE / 910405`。

## Migration Verification

- Schema verification: 已核对 `system_role`、`system_user_role`、`system_users`、`system_role_category`、`mes_pro_batch_record_version`、`mes_pro_batch_record_report`、`mes_pro_edhr_process_form_permission_rule`、`mes_pro_edhr_work_task`、`system_entitlement_claim`、`system_entitlement_grant`。
- Role verification: `system_role.id=910405`，`name=压力泵生产1`，`code=pressure_pump_production_1`，`category_id=5`，`status=0`，`tenant_id=1`。
- User-role verification: `system_user_role` 中 `user_id=810` 同时拥有原角色 `910295` 和新增角色 `910405`。
- Rule verification: `mes_pro_edhr_process_form_permission_rule` 中 `batch_record_version_id=118` 的 15 条 FILL 规则全部为 `candidate_source_type=ROLE`、`candidate_source_ids=910405`。
- Runtime verification: `responsibility_source_key LIKE 'FORM|%|118'` 的 20 条活动 FILL 工作任务均解析到 `candidate_user_snapshot=810`、`assignee_user_id=810`。
- Detail API verification: `GET /admin-api/mes/pro/edhr-batch-execution/get?id=900000000823` 返回的 V13.0 表单任务 `fillableUsers` 显示 `userId=810`、`displayName=王歆`。

## Blockers

- Implementation blocker: none.
- Closeout blocker: 主工作区存在本任务开始前的并行脏改动和未跟踪文件，按项目提交门禁不能在未基线提交全部脏改动的情况下只提交本任务文件；本次未执行提交/推送。

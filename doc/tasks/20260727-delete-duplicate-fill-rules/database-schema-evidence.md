# Database Schema Evidence

## Data Change Goal

- 修复租户 1、批记录版本 130、报表 `1d05410f1d3140c5b8aa6786887ae69c` 的填写人规则多结果异常。
- 保留一条物理记录并规范为正式表单级规则，删除其余 86 条 E2E 辅助规则。

## Affected Entities

- `mes_pro_edhr_process_form_permission_rule`
- 只影响 `tenant_id=1`、`route_process_id=0`、目标报表、`batch_record_version_id=130`、`rule_type=FILL`、启用且未删除的记录。

## Database And Tooling

- Engine: MySQL 8 / InnoDB。
- Runtime: local Docker container `int-ruoyi-mysql`。
- Change mechanism: guarded SQL transaction; no migration framework and no production schema change.

## Schema Evidence

- 主键：`id`。
- 版本字段：`batch_record_version_id`。
- 唯一键：`tenant_id, route_process_id, batch_record_report_id, batch_record_version_id, rule_type, scope_key, signature_cell_key, deleted`。
- 当前目标范围预检计数：87。

## Data Safety Analysis

- SQL 固定租户、报表、版本、路由层级、规则类型、启用状态和删除标记。
- 事务首先锁定并断言目标行数为 87、保留载体恰好 1 条。
- 更新影响行数必须为 1，删除影响行数必须为 86，提交前剩余计数必须为 1。
- 任一断言失败时 `ROLLBACK` 并抛出错误。

## Retained Rule

- Source evidence: V14.0 source version is V13.0; V13.0 and the other 14 forms in V14.0 use role `910405`.
- Final scope: `ALL`.
- Final candidate source: `ROLE / 910405` (`压力泵生产1`).
- Resolved enabled users: `王歆` and `任丹`.
- Guarded repair retained physical row `3217`; a later write-type E2E restored the same formal rule semantics as physical row `3391`.
- Current stable version scope remains `batch_record_version_id=130`.

## Recovery Plan

- Before-image is exported with `mysqldump --no-create-info` into the task directory.
- Restore by deleting the same exact target scope and importing the snapshot.
- Never restore into another tenant, report, or version.

## Rollback

- Transaction assertion failure executes `ROLLBACK` and raises an error.
- Post-commit recovery uses the 87-row before-image only within the exact tenant, report, version, route-process, rule-type, enabled, and deleted scope.

## BDD

- `BDD: 重复填写人规则清理 -> Given 当前目标版本存在 87 条启用规则 When 执行受控修复 Then 仅保留 1 条正式角色规则、删除 86 条并恢复查询。`

## RED

- `RED: exact-scope count and get-by-report log -> FAIL, count=87 and selectOne throws TooManyResultsException.`

## GREEN

- `GREEN: guarded repair transaction -> PASS, updated=1, deleted=86, remaining=1`.
- `doc/tasks/20260727-delete-duplicate-fill-rules/execute-repair.ps1` passed:
  retained `1`, updated `1`, deleted `86`, remaining `1`.
- Snapshot contains `87` inserts with SHA-256
  `FCB40150DCA3216DA66746213689EDEDD08799B2F51F4A378AD560E3E035AA60`.

## Migration Verification

- No schema migration is introduced.
- Post-change database scope contains exactly one enabled `FILL` rule:
  `ALL / ROLE / 910405 / version 130`.
- No `CODX_VFC_ASSIST_*` enabled rules remain in the latest stable database check.
- Temporary repair procedure count is `0`.
- Login-state `get-by-report` returned `code=0`, `fillRuleStatus=CONFIGURED`,
  `ROLE / 910405`, and candidates 王歆、任丹.
- Real page read-only verification displayed `已配置 王歆、任丹` and produced no MES write request.

## Blockers

- None. A write-type E2E can temporarily recreate the 87 cell-scoped rules; future verification must wait for its restore phase before judging the final state.

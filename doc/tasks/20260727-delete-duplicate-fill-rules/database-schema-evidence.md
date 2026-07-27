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

## Recovery Plan

- Before-image is exported with `mysqldump --no-create-info` into the task directory.
- Restore by deleting the same exact target scope and importing the snapshot.
- Never restore into another tenant, report, or version.

## BDD

- `BDD: 重复填写人规则清理 -> Given 当前目标版本存在 87 条启用规则 When 执行受控修复 Then 仅保留 1 条正式角色规则、删除 86 条并恢复查询。`

## RED

- `RED: exact-scope count and get-by-report log -> FAIL, count=87 and selectOne throws TooManyResultsException.`

## GREEN

- Pending guarded transaction and post-commit verification.

## Migration Verification

- No schema migration is introduced.
- Pending post-change row count, retained rule content, unrelated-scope checksum, and API/page verification.

## Blockers

- Fail if a conflicting E2E process writes the same report again.
- Fail if the exact target count differs from 87 or the retained carrier is not unique.

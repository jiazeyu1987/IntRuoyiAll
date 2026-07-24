# 执行记录：定位 DCC 其他类别目录绑定缺失

## BDD

BDD: 其他类别必须有目录绑定 -> Given 用户在 NAS 转移弹窗中选择目录 `1. QMS documents` 且模板类别为 `其他(906104)` / When 提交转移任务 / Then 后端必须确认该类别存在有效目录绑定且覆盖所选目录，否则明确返回类别未绑定目录错误。

BDD: 数据修复不得跨租户或误绑目录 -> Given 类别、目录和绑定数据属于具体租户 / When 修复 `906104` 的目录绑定 / Then 只能在同一租户、唯一确认的目标目录上新增或恢复绑定；如果租户或目标目录不唯一，必须失败并报告。

## Evidence

- BUG REPORT: 前端“转移到 DCC”弹窗提交时显示 `selected category is not bound to a directory: 906104`。
- ENVIRONMENT: local `int_main` backend, `http://127.0.0.1:48081`, workspace `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`。
- READ-ONLY SQL: `906104` -> `其他`, `active=1`, `tenant_id=1`, `deleted=0`。
- READ-ONLY SQL: `dcc_category_directory_binding` for `category_id=906104` -> `0` active rows。
- READ-ONLY SQL: tenant `1` active DCC directories -> `0`; historical `1. QMS documents` rows are all `deleted=1`。
- DATA FIX SCRIPT: `doc/tasks/20260603-dcc-other-category-directory-binding/apply-local-other-qms-binding.sql` with fail-fast checks for category identity, active directory ambiguity, and conflicting active bindings.

## RED

- RED: 本机只读 SQL -> FAIL, expected reason: `906104` exists as active `其他`, but no active directory binding exists and no active DCC target root exists for `1. QMS documents`。

## GREEN

- GREEN: `docker exec int-ruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 -D ruoyi-vue-pro -e "source /tmp/apply-local-other-qms-binding.sql"` -> PASS, returned `category_id=906104`, `directory_id=906306`, `directory_name=1. QMS documents`。
- GREEN: post-fix read-only SQL -> PASS, `dcc_file_directory.id=906306` is active and not deleted; `dcc_category_directory_binding.id=906254` binds `category_id=906104` to `directory_id=906306` with `active=1`, `deleted=0`。
- GREEN: idempotency rerun -> PASS, returned the same `directory_id=906306` and did not create duplicate active rows。

## Root Cause

- 本机 DCC 目录树已被清空，`906104` 虽然作为 `其他` 文件类别存在且治理数据已补齐，但未创建对应 DCC 根目录绑定。
- NAS 转移入口在最新后端中会先校验所选类别目录绑定；因此这次报错是最新校验生效后暴露出的本机配置数据缺口，不是后端未重启。

## Risk And Regression Scope

- 受影响范围：DCC NAS 转移任务提交、后台执行和类别目录绑定数据。
- 本次数据修复只影响本机 tenant `1` 的 `dcc_file_directory` 与 `dcc_category_directory_binding`；不修改服务器、MinIO、NAS、展厅受保护配置或其他租户。

## Notes

- First SQL invocation through PowerShell stdin failed before execution because the mysql client received a BOM character; read-back confirmed no data changes.
- Second invocation failed on stored procedure variable/table collation mismatch before data insertion; read-back confirmed no data changes. The script now pins connection and variables to `utf8mb4_unicode_ci`.

## Closeout

- DATABASE EVIDENCE VALIDATION: `python -X utf8 C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc\tasks\20260603-dcc-other-category-directory-binding\database-schema-evidence.md` -> PASS。
- BUG EVIDENCE VALIDATION: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260603-dcc-other-category-directory-binding\bug-regression-evidence.md` -> PASS。
- CLOSEOUT PREVIEW: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260603-dcc-other-category-directory-binding --mode preview` -> PASS，keep `task.md` / `execution-log.md` / `apply-local-other-qms-binding.sql` / `database-schema-evidence.md` / `bug-regression-evidence.md`，delete `<none>`，blocked `<none>`，warnings `<none>`。

## Blockers

- 待确认。

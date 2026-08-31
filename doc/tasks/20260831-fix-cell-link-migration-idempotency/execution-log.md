# 20260831 修正批记录单元格链接迁移幂等性执行日志

## Log

- BDD: existing cell-link source columns -> Given 三个 source 列已存在但 migration ledger 未 APPLIED，When required SQL 重放历史 migration，Then 每列检查后 no-op，不报 duplicate column，并允许后续宽度 migration 执行。
- INFO: base -> branch=`codex/20260831-fix-cell-link-migration-idempotency`，HEAD=`c3a134a797c3aab24feba3aafec32815f8345cf1`，独立 worktree clean。
- INFO: real-schema -> 测试服 source_type=32、source_field_code=64、source_field_name=100；后续 `20260830_mes_batch_record_cell_link_structured_source_widths` 正式升级后两列到 1024/255。
- INFO: scope -> 只增加表/逐列 information_schema 守卫，保留原列 DDL，不执行数据库写入。

RED: `python -X utf8 -m pytest script\tests\test_mes_batch_record_cell_link_work_order_source_sql.py -q --basetemp <state>\pytest-cell-link-idempotency-red` -> FAIL，`1 failed, 1 passed`；预期原因：当前 SQL 无 procedure/information_schema 逐列守卫。

GREEN: 同一目标 pytest -> PASS，`2 passed`。
- GREEN: release-regression -> 目标 + SQL idempotency + metadata/policy/preflight -> `46 passed`。
- GREEN: maintenance-migration-policy -> status=passed、migrationCount=551。
- INFO: target-plan-command-parse -> 首次临时 manifest 复验命令因 `foreach($t in$targets)` 缺空格在本地 ParserError，未执行任何文件/数据库动作；修正语法后重跑。
- GREEN: target-bound-code-only-plan -> status=passed、items=552、blocked=0、apply=11、目标 action=APPLY/reapply current checksum。
- GREEN: evidence-validators -> bug-regression/database-schema evidence validator PASS。
- GREEN: branch-runtime-port-guard -> PASS，slot 59，8314/48314；本任务未启动服务。
- INFO: integration-preflight -> app `int_main` 仍为 base `c3a134a7`，目标 SQL/test/task paths 无 dirty overlap，可在实现提交后 ff-only。
- GREEN: implementation-commit -> PASS，commit=`273b5dad6`，5 files，仅目标 SQL、回归测试和任务记录。
- GREEN: int-main-fast-forward -> PASS，app `int_main` 从 `c3a134a7` ff-only 到 `273b5dad6`；目标路径无 dirty overlap。
- GREEN: int-main-regression -> PASS，46 passed in 11.08s、实际维护 gate 551 项、branch runtime guard PASS、commit ancestor=true。
- GREEN: closeout -> PASS，fix worktree Git registration=0、physical path=false、branch 已融合；slot 59 active=false/deletedAt 已记录；最终 migration APPLIED、publish SUCCESS。

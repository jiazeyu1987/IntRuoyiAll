# 任务：生产工单 ERP 快照字段发布 SQL 幂等修复

- Task ID: `20260701-mes-work-order-erp-snapshot-fields-release-idempotency`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-07-01`
- Current Status: `completed`
- User Request: `我希望可以从根本解决这个问题,不是临时方案,比如换了产线,换了日期,换了产品就又报错了`

## Task Goal

修复 `sql/mysql/20260630_mes_pro_work_order_erp_snapshot_fields.sql` 在测试服真实发布时因目标库已存在 ERP 快照字段而报 `Duplicate column name 'workshop_name'` 的问题，使该 required SQL 对“全新库缺列”和“旧库已部分/全部有列”两类环境都可重复执行，不再依赖手工改库或一次性绕过。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260630-schedule-calendar-capacity-horizon-renewal\task.md`
- 状态：`completed`
- 处理说明：AUTO-DAY 班次产能缺失的后端根因修复已完成并提交；当前待处理的是测试服发布链路中独立暴露出的 required SQL 幂等性缺口。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 本轮命中 PowerShell 与发布链路经验，先修复 required SQL 契约，再回到真实发布链路。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - 中文文档与 SQL 文件读写必须显式 UTF-8；PowerShell 5.1 下不使用 `&&`。
- `D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-build-preflight-lessons.md`
  - required SQL 在测试服失败时，优先做只读根因定位并回到 SQL 契约修复，不手工改测试库绕过。
- `D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-agent-checklist.md`
  - 发布失败优先按 migration / required SQL 契约层修复；修复后必须用新 `releaseTag` 重新跑 `build-release -> publish-test`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。required SQL 必须对真实环境可重入，不允许用脚本跳过、捕获后忽略、或手工改库充当正式解法。
- `是否从根因和长期维护角度解决`：是。修复目标是把 ERP 快照字段迁移改成正式幂等契约，并补上自动化测试门禁。
- `是否存在临时补丁或绕过`：否。不采用只在测试服手工补 migration 状态、删除列或改 SQL 顺序的临时方案。

## BDD 场景

- `BDD: 已存在 ERP 快照字段时 required SQL 仍可重复执行 -> Given mes_pro_work_order 已存在 workshop_name 等全部或部分 ERP 快照字段 / When 运行 20260630_mes_pro_work_order_erp_snapshot_fields.sql / Then SQL 只补缺口且不再报 Duplicate column。`
- `BDD: 全新库缺少 ERP 快照字段时 required SQL 仍完整补齐 -> Given mes_pro_work_order 尚未包含 ERP 快照字段 / When 运行 20260630_mes_pro_work_order_erp_snapshot_fields.sql / Then 9 个 ERP 快照字段以既定类型、注释与列顺序被补齐。`
- `BDD: 迁移只补列不破坏本地扩展 -> Given 本地 mes_pro_work_order 已含现有业务字段 / When ERP 快照字段迁移执行或重跑 / Then 不执行 drop/delete/change/rename，也不覆盖其他本地扩展字段。`

## Milestones

1. M1：建立任务文档并确认真实发布阻断根因。`completed`
2. M2：先写 RED 回归测试，证明当前 SQL 不满足重复发布契约。`completed`
3. M3：实现幂等 SQL 正式修复并补证据。`completed`
4. M4：运行 GREEN / migration gate / TDD 门禁并回填结果。`completed`
5. M5：提交最小修复并交回维护仓重跑 committed-only 测试服发布。`completed`

## Expected Verification

- RED：
  - `python -X utf8 -m pytest script/tests/test_mes_work_order_erp_snapshot_fields_sql.py -q`
- GREEN / REGRESSION：
  - `python -X utf8 -m pytest script/tests/test_mes_work_order_erp_snapshot_fields_sql.py script/tests/test_release_migration_metadata_sql_20260630.py -q`
  - `python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql`
  - `python -X utf8 tool/verify_tdd_compliance.py --task-dir doc/tasks/20260701-mes-work-order-erp-snapshot-fields-release-idempotency --paths sql/mysql/20260630_mes_pro_work_order_erp_snapshot_fields.sql script/tests/test_mes_work_order_erp_snapshot_fields_sql.py`

## Current Blockers

- 无代码侧阻断。后续只需在维护仓以新 `releaseTag` 重新执行 committed-only `build-release -> publish-test`。

## Final Verification Result

- `python -X utf8 -c "import subprocess; repo=r'D:\\ProjectPackage\\Int\\IntRuoyi\\ruoyi-vue-pro'; sql=subprocess.run(['git','show','HEAD:sql/mysql/20260630_mes_pro_work_order_erp_snapshot_fields.sql'], cwd=repo, check=True, capture_output=True, text=True, encoding='utf-8').stdout; required=['FROM information_schema.COLUMNS','PREPARE mes_pro_work_order_erp_snapshot_workshop_name_stmt','COLUMN_NAME = \\'planned_end_time\\'']; missing=[item for item in required if item not in sql]; assert not missing, 'missing guard fragments: ' + ', '.join(missing)"` -> FAIL（RED，证明旧版 SQL 不满足重复发布契约）
- `python -X utf8 -m pytest script/tests/test_mes_work_order_erp_snapshot_fields_sql.py script/tests/test_release_migration_metadata_sql_20260630.py -q` -> PASS
- `python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql` -> PASS
- `python -X utf8 tool/verify_tdd_compliance.py --task-dir doc/tasks/20260701-mes-work-order-erp-snapshot-fields-release-idempotency --paths sql/mysql/20260630_mes_pro_work_order_erp_snapshot_fields.sql script/tests/test_mes_work_order_erp_snapshot_fields_sql.py` -> PASS
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260701-mes-work-order-erp-snapshot-fields-release-idempotency\bug-regression-evidence.md` -> PASS
- `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260701-mes-work-order-erp-snapshot-fields-release-idempotency\database-schema-evidence.md` -> PASS

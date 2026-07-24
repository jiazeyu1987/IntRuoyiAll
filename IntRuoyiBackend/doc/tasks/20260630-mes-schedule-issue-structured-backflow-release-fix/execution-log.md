# 20260630 排产问题结构化回流 SQL 发布幂等性修复执行日志

BDD: 排产问题结构化回流 SQL 可重复执行 -> Given mes_pro_schedule_issue 可能已提前拥有 status/source 字段与索引 When 发布 required SQL 重新执行 20260624_mes_schedule_issue_structured_backflow.sql Then SQL 不因重复列或重复索引失败。

BDD: 发布前能提前发现 SQL 不可重入风险 -> Given required SQL 会被纳入真实发布包 When 运行发布前 SQL 幂等性门禁测试 Then 不允许保留裸 ALTER ADD COLUMN 或裸 ADD KEY 造成重复执行失败。

GREEN: experience-preflight -> PASS，已读取 `D:\ProjectPackage\Int\IntRuoyiMaintance\docs\experience-index.md`、`D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-build-preflight-lessons.md`、`D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-agent-checklist.md`。
INFO: publish-test-failure-evidence -> 维护仓真实测试服发布 `op-2026-06-30T113310680094600Z-844a0e48-aaec-4638-b63f-514985aad00e` 在执行 required SQL `20260624_mes_schedule_issue_structured_backflow.sql` 时失败；日志证据为 `ERROR 1060 (42S21) at line 4: Duplicate column name 'status'`，且发布锁已 `LOCK_RELEASED`。
INFO: root-cause-readonly-check -> 只读检查仓库 SQL 后确认 `20260624_mes_schedule_issue_structured_backflow.sql` 仍使用裸 `ALTER TABLE ... ADD COLUMN/ADD KEY`；同域后续 SQL `20260626_mes_schedule_issue_lifecycle.sql` 已针对同批字段补充 `information_schema.columns` 存在性检查，说明当前失败属于 required SQL 幂等性契约缺失，而非环境异常。
RED: sql-idempotency-contract -> FAIL，发布失败现场已证明旧版 `20260624_mes_schedule_issue_structured_backflow.sql` 在真实库已存在 `status` 列时仍执行裸 `ADD COLUMN status`，触发 `ERROR 1060 (42S21): Duplicate column name 'status'`。
GREEN: sql-idempotency-tests -> PASS，`python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_mes_schedule_issue_structured_backflow_sql.py D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_release_sql_idempotency_contract.py -q` 通过，`13 passed in 0.24s`。
GREEN: migration-policy-gate -> PASS，`python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\release\run-release-migration-policy-gate.py --sql-root D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\mysql` 通过；修复后 `20260624_mes_schedule_issue_structured_backflow` 迁移元数据与依赖契约仍有效。
GREEN: backend-main-branch-commit -> PASS，后端主分支已提交 `782dc5a886a07f214bb3fe89541ca73a47c03fbd`，提交信息 `任务: 修复排产问题结构化回流发布幂等性`；当前可返回维护仓重新执行新的主分支 `build-release -> publish-test`。

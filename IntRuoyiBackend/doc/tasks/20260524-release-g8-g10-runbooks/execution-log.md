# 20260524 release g8 g10 runbooks execution log

## BDD

- BDD: rollback-app runbook executable -> Given 发布负责人批准应用回滚 / When 备份恢复执行人按 runbook 操作 / Then 能明确前置条件、命令、禁止范围、报告字段和验收证据。
- BDD: restore-data runbook executable -> Given 数据责任人批准恢复 / When 执行者按 runbook 操作 / Then 能先保护现场、选择恢复点、生成 preRestoreSnapshotId、恢复数据并记录业务影响。
- BDD: alert routing runbook executable -> Given 运维 owner 准备启用 webhook / When 按 runbook 配置和验收 / Then 能明确配置字段、路由矩阵、发送证据、失败升级和 BLOCKED 条件。

## TDD Evidence

- RED: `python -X utf8 -m pytest ruoyi-vue-pro\script\tests\test_release_go_no_go_contract_docs.py -q` -> FAIL, `docs/recovery/rollback-app.md`、`docs/recovery/restore-data.md`、`docs/operations/backup-ops-alert-routing.md` 不存在。
- GREEN: `python -X utf8 -m pytest ruoyi-vue-pro\script\tests\test_release_go_no_go_contract_docs.py -q` -> PASS, 6 passed。
- GREEN: `python -X utf8 -m pytest ruoyi-vue-pro\script\tests\test_release_go_no_go_contract_docs.py ruoyi-vue-pro\script\tests\test_backup_ops_notification_flow_tooling.py ruoyi-vue-pro\script\tests\test_backup_ops_linux_runtime_rollback_tooling.py -q` -> PASS, 11 passed。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root D:\ProjectPackage\Int\IntRuoyi` -> PASS。
- GREEN: UTF-8 read check for runbooks, release doc, task docs and test -> PASS, 9 files.

## 过程记录

- 任务开始：2026-05-24。
- 本任务只写文档和文档契约测试，不触发真实生产动作。
- 已新增根文档：
  - `docs/recovery/rollback-app.md`
  - `docs/recovery/restore-data.md`
  - `docs/operations/backup-ops-alert-routing.md`
- 已更新 `docs/releases/20260524-int-ruoyi-ops-go-no-go.md`，链接到三份详细 runbook。
- 发布结论未改为 GO；缺真实 owner、webhook target 和发送证据时 G8/G9/G10 仍保持 `BLOCKED`。

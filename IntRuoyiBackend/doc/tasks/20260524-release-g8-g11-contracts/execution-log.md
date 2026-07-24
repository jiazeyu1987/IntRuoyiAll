# 20260524 release g8 g11 contracts execution log

## BDD

- BDD: rollback-app 确认接口 fail-closed -> Given 发布负责人尚未确认触发条件和目标 tag / When reviewer 查看 Go/No-Go / Then G8 必须保持 `BLOCKED`，并列出要填的确认字段。
- BDD: restore-data 确认接口 fail-closed -> Given 数据责任人尚未确认恢复触发条件、目标 backupId 和影响范围 / When reviewer 查看 Go/No-Go / Then G9 必须保持 `BLOCKED`，并要求 `preRestoreSnapshotId`。
- BDD: alert route fail-closed -> Given `notify.enabled=false`、`channel=pending` 或缺 target/owner/发送证据 / When reviewer 查看 Go/No-Go / Then G10 必须保持 `BLOCKED`。
- BDD: owner matrix fail-closed -> Given 发布负责人、备份恢复执行人、数据责任人、验收负责人或 reviewer 任一缺失 / When reviewer 查看 Go/No-Go / Then G11 必须保持 `BLOCKED`。

## TDD Evidence

- RED: `python -X utf8 -m pytest ruoyi-vue-pro\script\tests\test_release_go_no_go_contract_docs.py -q` -> FAIL, Go/No-Go 文档缺少 `## G8-G11 Confirmation Interfaces` 以及 G8/G9/G10/G11 具体确认字段。
- GREEN: `python -X utf8 -m pytest ruoyi-vue-pro\script\tests\test_release_go_no_go_contract_docs.py -q` -> PASS, 3 passed。
- GREEN: `python -X utf8 -m pytest ruoyi-vue-pro\script\tests\test_release_go_no_go_contract_docs.py ruoyi-vue-pro\script\tests\test_backup_ops_notification_flow_tooling.py ruoyi-vue-pro\script\tests\test_backup_ops_linux_runtime_rollback_tooling.py -q` -> PASS, 8 passed。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root D:\ProjectPackage\Int\IntRuoyi` -> PASS。
- GREEN: UTF-8 read check for release doc, task docs and new test -> PASS, 6 files.

## 过程记录

- 任务开始：2026-05-24。
- 不执行生产回滚、数据恢复、真实通知或正式发布。
- 子 agent G8/G9 只读结论：
  - `rollback-app` 支持 `-SelectedImageTag`，只更新 `IMAGE_TAG`、重启 backend/frontend 并检查健康，不恢复 MySQL、MinIO 或 Redis。
  - `restore-data` 支持 `-SelectedBackupId`，会先发送 started 通知、生成 `preRestoreSnapshotId`，再恢复 MySQL 与对象文件并做健康/样例验证。
  - 真实触发条件、目标选择规则、业务影响范围和审批人必须由 owner 确认，Codex 不能代填。
- 子 agent G10/G11 只读结论：
  - 当前 `notify.enabled=false`、`notify.channel=pending`，缺真实 `notify.webhook.url`、alert target、owner 和发送证据。
  - 代码可见 disabled/pending/unsupported/failed/sent 状态，但没有真实 webhook 接通证据，G10/G11 仍必须 `BLOCKED`。
- 本次文档结论：
  - `docs/releases/20260524-int-ruoyi-ops-go-no-go.md` 已新增 `G8-G11 Confirmation Interfaces`，明确每个 gate 的确认字段、当前 BLOCKED 值和 GO 条件。
  - 文档没有把缺失 owner、webhook 或发送证据改写为 GO；正式发布结论仍保持 `BLOCKED`。

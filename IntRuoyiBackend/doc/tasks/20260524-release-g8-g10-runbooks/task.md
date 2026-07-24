# 20260524 release g8 g10 runbooks

## 任务目标

- 在服务仓库中为 G8/G9/G10 详细运行手册增加可测试文档契约。
- 运行手册位于仓库根 `docs/`，服务仓库提交测试与任务证据；根目录不是 git 仓库。

## 里程碑

1. 创建服务仓库任务文档与 BDD/TDD 证据框架。
2. RED：新增 runbook 文档契约测试。
3. GREEN：补齐详细 runbook 并更新发布门禁链接。
4. 执行回归、UTF-8 检查和 task-closeout 预览。
5. 提交服务仓库测试与任务证据。

## 预期验证

- `python -X utf8 -m pytest ruoyi-vue-pro\script\tests\test_release_go_no_go_contract_docs.py -q`
- `python -X utf8 -m pytest ruoyi-vue-pro\script\tests\test_release_go_no_go_contract_docs.py ruoyi-vue-pro\script\tests\test_backup_ops_notification_flow_tooling.py ruoyi-vue-pro\script\tests\test_backup_ops_linux_runtime_rollback_tooling.py -q`

## 当前状态

- 状态：completed
- 当前阶段：G8/G9/G10 详细 runbook 和文档契约测试已完成；真实 owner/webhook/发送证据仍待外部提供。
- 阻塞边界：真实 owner、webhook target、发送证据、正式发布确认仍需外部提供。

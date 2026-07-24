# 20260524 release readiness worktree migration

## 任务目标

- 记录 backend worktree 从 `20260524-doc-readiness-worktree-check` 迁移到 `20260524-release-readiness-gates-dev`。
- 保留旧 backend worktree 中必要的发布门禁文档契约提交。
- 本任务只做 worktree 迁移记录，不执行正式发布、不触发回滚/恢复、不发送真实通知。

## 里程碑

1. 从旧 backend worktree HEAD 创建新 backend worktree。
2. 验证新 backend worktree 包含必要提交。
3. 在新 backend worktree 中运行文档契约与发布脚本回归测试。
4. 删除旧 backend/frontend worktree。
5. 提交迁移任务记录。

## 预期验证

- `git log -5 --oneline` 包含 `cd3ed90909`、`952d233c24`、`d412e04d1e`。
- `python -X utf8 -m pytest script\tests\test_release_go_no_go_contract_docs.py script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` 通过。
- 旧 worktree 不再出现在 `git worktree list` 中。

## 当前状态

- 状态：completed
- 当前阶段：新 backend worktree 已创建并验证通过；旧 worktree 已删除。

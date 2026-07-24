# 20260524 release readiness worktree migration execution log

## BDD

- BDD: migrate necessary worktree content -> Given 旧 worktree 命名为文档检查用途且 backend 分支已有必要提交 / When 新建明确开发任务 worktree / Then 新 worktree 必须包含必要提交、文档入口和验证能力，旧 worktree 必须被删除。

## TDD Evidence

- GREEN: `git worktree add -b task/20260524-release-readiness-gates-dev ...\ruoyi-vue-pro d412e04d1e...` -> PASS。
- GREEN: `git log -5 --oneline` -> PASS，包含 `d412e04d1e`、`952d233c24`、`cd3ed90909`。
- GREEN: `python -X utf8 -m pytest script\tests\test_release_go_no_go_contract_docs.py script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS, 28 passed。
- GREEN: `python -X utf8 ...validate_acceptance_plan.py --root D:\ProjectPackage\Int\IntRuoyi\worktrees\20260524-release-readiness-gates-dev` -> PASS。
- GREEN: `git worktree remove` old backend/frontend worktrees -> PASS。
- GREEN: `Test-Path D:\ProjectPackage\Int\IntRuoyi\worktrees\20260524-doc-readiness-worktree-check` -> `False`。

## 过程记录

- 新 backend worktree：`D:\ProjectPackage\Int\IntRuoyi\worktrees\20260524-release-readiness-gates-dev\ruoyi-vue-pro`。
- 新 branch：`task/20260524-release-readiness-gates-dev`。
- 旧 worktree 已删除。

# 20260524 release readiness gates dev

## 任务目标

- 在 backend worktree `task/20260524-release-readiness-gates-dev` 中开发发布门禁验证/确认工具。
- 覆盖 G6/G7、G8/G9、G10/G11 的 fail-closed 验证能力。
- 不执行正式发布、不触发回滚/恢复、不发送真实通知。

## 里程碑

1. Worker A：G6/G7 Playwright/front-end-path validation tooling.
2. Worker B：G8/G9 confirmation validation tooling.
3. Worker C：G10/G11 confirmation validation tooling.
4. 主 agent review and regression.

## 预期验证

- `python -X utf8 -m pytest script\tests\test_release_readiness_g6_g7_tooling.py -q`
- `python -X utf8 -m pytest script\tests\test_release_readiness_g8_g9_contracts.py -q`
- `python -X utf8 -m pytest script\tests\test_release_readiness_g10_g11_contracts.py -q`
- `python -X utf8 -m pytest script\tests\test_release_go_no_go_contract_docs.py script\tests\test_publish_int_ruoyi_to_test_tooling.py -q`

## 当前状态

- 状态：completed
- 当前阶段：3 个子 agent 的发布门禁验证/确认工具已完成，主 reviewer 已复核通过。
- 结论：工具开发 PASS；正式发布 Go/No-Go 仍为 `BLOCKED`。

## Cleanup Keep

- `doc/tasks/20260524-release-readiness-gates-dev/verification-report.md`
- `doc/tasks/20260524-release-readiness-gates-dev/worker-a-g6-g7-evidence.md`
- `doc/tasks/20260524-release-readiness-gates-dev/worker-b-g8-g9-evidence.md`
- `doc/tasks/20260524-release-readiness-gates-dev/worker-c-g10-g11-evidence.md`

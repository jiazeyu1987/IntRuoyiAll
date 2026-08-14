# AGENTS 默认无需 Git 提交

## Task Goal

修改工作区根目录 `AGENTS.md`，取消每次任务必须创建基线提交、实现提交、收尾提交并推送到 `origin` 的要求；默认不执行 Git 提交或推送，只有用户明确要求时才执行对应 Git 操作。

## Milestones

- [x] M1：定位 `AGENTS.md` 中强制提交与推送规则。
- [x] M2：将 Git 策略改为默认无需提交或推送。
- [x] M3：完成结构检查并记录结果。

## Expected Verification

- `rg` 检查 `AGENTS.md` 不再要求每个任务必须提交或推送。
- `rg` 检查仅在用户明确要求时执行 Git 操作。
- `git diff --check -- AGENTS.md doc/tasks/20260807-agents-no-required-git` 通过。

## Current Status

completed

Git 默认策略已完成修改并通过结构验证；cleanup preview/apply 均通过，无文件需要删除，未执行 Git 提交或推送。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；直接修改任务级 Git 治理规则。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- 已读取 `docs/experience-index.md`。
- 匹配经验集中在 `docs/powershell-memory.md` 的共享分支、脏工作区和提交安全门禁；这些门禁继续作为用户明确要求 Git 操作时的安全规则，不再构成默认任务完成条件。
- 已按 `project-experience-consolidation` 评估：本次变更本身已写入权威 `AGENTS.md`，没有额外的故障经验或排查经验需要重复写入长期经验文档。

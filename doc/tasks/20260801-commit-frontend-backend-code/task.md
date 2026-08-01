# 20260801-commit-frontend-backend-code

## Task Goal

提交并推送当前 `int_main` 工作区中已存在的前后端代码、测试和相关任务/规则文档改动。

## Milestones

- [x] 预检 Git 分支、远端、脏工作区和提交范围。
- [x] 将本次开始前已存在的脏改动保存为独立基线提交。
- [x] 记录本次提交任务的验证、清理和收尾证据。
- [x] 推送 `int_main` 到 `origin` 并确认本地不再领先远端。

## Expected Verification

- `git status --short --branch`
- `git diff --cached --name-status`
- `git push origin int_main`
- 推送后 `git status --short --branch` 不显示 ahead。

## Current Status

completed

## Experience Gate

- 已读取 `docs/experience-index.md`。
- 适用门禁：`docs/powershell-memory.md#任务提交推送前置门禁`、`docs/powershell-memory.md#脏工作区基线门禁`、`docs/powershell-memory.md#提交后残余改动复扫门禁`、`docs/task-closeout-rules.md#提交规则`、`docs/task-closeout-rules.md#收尾规则`。
- 适用推送门禁：`D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-build-preflight-lessons.md#2026-07-24-GitHub-推送前历史大文件门禁`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，按项目提交/推送门禁拆分基线与本次收尾记录。
- `是否存在临时补丁或绕过`：否。

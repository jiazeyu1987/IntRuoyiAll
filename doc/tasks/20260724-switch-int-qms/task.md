# 创建并切换 int_qms 分支

## Task Goal

- 从当前 `int_main` HEAD 创建本地分支 `int_qms`，并切换当前工作区到该分支。

## Milestones

- [x] 读取 Git / PowerShell / 任务规则。
- [x] 创建任务目录。
- [x] 检查当前分支、目标分支是否存在、脏工作区状态。
- [x] 创建并切换到 `int_qms`。
- [x] 验证当前分支为 `int_qms`。

## Expected Verification

- `git branch --show-current` 输出 `int_qms`。
- `git status --short --branch` 显示当前分支为 `int_qms`。

## Current Status

ready_for_closeout

## Verification Evidence

- 切换前当前分支：`int_main`。
- 切换前当前 HEAD：`db2f3ca2`。
- 本地分支 `int_qms` 未发现。
- 远端分支 `origin/int_qms` 未发现。
- 工作区存在既有未提交改动；本任务不提交、不回滚、不丢弃这些改动。
- 已执行 `git switch -c int_qms`。
- 切换后当前分支：`int_qms`。
- 切换后 `git status --short --branch`：`## int_qms`。

## Blockers

- 用户目标已完成。
- Git closeout blocked: 当前分支 `int_qms` 尚无远端跟踪分支，且工作区存在未提交改动；本任务不自动提交、推送或处理非本任务改动。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，本任务仅执行明确的 Git 分支创建与切换。
- `是否存在临时补丁或绕过`：否。

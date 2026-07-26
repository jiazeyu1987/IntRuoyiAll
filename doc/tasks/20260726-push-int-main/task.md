# 推送 int_main 代码

## Task Goal

将当前 `int_main` 分支上需要推送的本地提交安全推送到 `origin/int_main`，并按项目规则记录工作区状态、推送前置检查和最终验证结果。

## Milestones

- [x] 读取并记录 Git 推送与任务收尾规则
- [x] 检查当前分支、远端、脏工作区和待推送提交
- [x] 按规则处理既有脏工作区基线
- [x] 推送 `int_main` 到 `origin` 并验证不再 ahead
- [x] 完成任务记录与最终状态

## Expected Verification

- `git status --short --branch`
- `git branch --show-current`
- `git remote -v`
- 推送前待推送对象大小检查
- `git push origin int_main`
- 推送后 `git status --short --branch`

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；按项目 Git 推送门禁处理脏工作区、远端与 ahead 状态。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- PowerShell / Git 共同前置经验：推送前必须检查分支、remote、工作区脏状态、staged 文件清单；禁止 force push、历史重写、destructive reset、丢弃脏改动或跳过 push。
- GitHub 推送大文件门禁：推送前检查待推送历史中是否存在超过 GitHub 100 MB 限制的 blob；如发现超限对象或推送被 GH001 拒绝，必须阻塞。

## Cleanup Keep

- doc/tasks/20260726-push-int-main/verification-report.md

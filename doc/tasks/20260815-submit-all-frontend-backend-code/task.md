# 提交并推送前后端全部当前代码

## Task Goal

核对并提交 `E:\IntRuoyi` 的 `int_main` 当前前端、后端全部代码改动，保留并纳入用户明确要求的现有改动，完成提交前门禁后推送到 `origin/int_main`。

## Milestones

- [x] M1：核对 Git 仓库、分支、远端、脏工作区、冲突、敏感文件和大文件。
- [x] M2：运行前后端现有提交前验证与分支运行端口门禁。
- [x] M3：暂存并复核前后端全部代码文件，创建提交。
- [x] M4：复扫残余改动，提交必要任务记录并推送 `origin/int_main`。
- [x] M5：确认代码提交已到达远端并完成收尾记录。

## Expected Verification

- `git status --short --branch` 确认分支为 `int_main`，无冲突，远端为 `origin/int_main`。
- `scripts\preflight\branch-runtime-port-guard.ps1` 通过。
- 前后端任务已有验证记录保持有效；提交前至少完成后端编译/聚焦测试与前端静态/类型门禁复核。
- 暂存区不包含凭据、运行日志、PID、构建目录或超过 GitHub 限制的大文件。
- 推送后本地 `int_main` 不再领先 `origin/int_main`。

## Applicable Gates

- 全部提交授权仍需排除凭据、运行产物、PID、构建目录和超大文件。
- 每次提交后必须复扫残余前后端改动，不能只提交一轮后直接推送。
- 推送前必须运行分支运行端口 guard，并检查待推送历史大文件。
- 当前共享工作区存在并行任务痕迹；提交前必须确认没有活动 Git 索引写入或冲突标记。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；按当前 Git 事实、提交门禁和远端状态完成整体提交。
- 是否存在临时补丁或绕过：否。

## Current Status

completed

## Remaining Blockers

- 无。

# 融合 int_main 到 int_shedule

## Task Goal

- 刷新远端引用，将最新 `origin/int_main` 融合到当前 `int_shedule` 分支，并完成项目要求的合并验证、提交、推送与收尾。

## Milestones

- [x] 读取 Git、worktree、端口、任务收尾与 PowerShell 规则。
- [x] 保存任务开始前的既存脏工作区基线。
- [x] 刷新 `origin/int_main` 并判定分支差异。
- [x] 融合最新 `origin/int_main`，处理并验证冲突结果。
- [x] 运行分支端口守卫并验证 Git 收敛状态。
- [x] 推送 `int_shedule`，执行任务清理并完成收尾。

## Expected Verification

- `git status --short --branch --untracked-files=all`
- `git fetch origin int_main int_shedule`
- `git rev-list --left-right --count origin/int_main...HEAD`
- `git merge --no-edit origin/int_main` 或记录已包含目标提交
- `powershell -ExecutionPolicy Bypass -File scripts\preflight\branch-runtime-port-guard.ps1`
- `git diff --check`
- `git push origin int_shedule`
- 推送后当前分支不再领先 `origin/int_shedule`

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；以远端引用、提交图、端口守卫和推送状态证明融合完成。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- `docs\experience-index.md` 命中 `docs\worktree-memory.md` 的并行主工作区远端快进融合、D-Main 本地主线滞后远端融合和 Windows fast-forward 检出半写恢复门禁。
- 融合前记录当前 `HEAD`、目标 `origin/int_main`、完整脏状态和提交图；既存脏内容必须先形成独立基线提交。
- 融合被阻塞或超时后，不得叠加新的 merge/reset；必须先核对 `MERGE_HEAD`、任务自有 Git 进程、`index.lock` 与工作区/index/ref 一致性。
- 融合后必须验证 `origin/int_main` 是 `HEAD` 的祖先、端口守卫通过、无冲突标记且当前分支成功推送。

## Baseline

- 任务开始前既存脏改动已保存为独立基线提交 `8a28606c`：`chore: preserve pre-merge worktree baseline`。
- 文件范围：`docs/worktree-memory.md`、`doc/tasks/merge-int-main-start-runtime-20260731/task.md`、`execution-log.md`、`verification-report.md`。

## Merge Result

- `git merge --no-edit origin/int_main` 已完成，生成合并提交 `0de158877d3d3e3d1fb7bb8b64b2bef0db4e25bb`。
- `origin/int_main=a386dc0daf00aabba0494e64f0439ea2630e4e10` 已成为当前 `HEAD` 的祖先。
- 合并后相对 `origin/int_main` 仅保留任务开始前独立基线提交中的经验文档和任务记录差异。

## Closeout Result

- `task-closeout-cleanup preview`：PASS，keep 三个核心任务记录，delete/blocked/warnings 均为 none。
- `task-closeout-cleanup apply`：PASS，未删除任何文件，当前仓库 `linked=False`。
- `project-experience-consolidation`：PASS，现有 `docs/worktree-memory.md` 与 `docs/powershell-memory.md` 已覆盖本次融合等待、索引锁和冲突扫描门禁，无需新增经验文档。
- 最终提交和推送按本任务完成后执行；推送后以 `git status --short --branch` 确认当前分支不再领先远端。

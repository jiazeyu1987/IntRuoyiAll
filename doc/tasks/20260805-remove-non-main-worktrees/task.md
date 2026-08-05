# 删除非 int_main Worktree

## Task Goal

删除当前 `E:\IntRuoyi` 仓库中除主工作区 `E:/IntRuoyi` / `int_main` 之外的所有 Git worktree，并保留可审计的操作与验证记录。

## Milestones

- [x] 读取 `docs/worktree-restrictions.md`、`docs/task-closeout-rules.md`、`docs/powershell-memory.md` 与 `docs/powershell-encoding.md`。
- [x] 保存开始前既有脏工作区基线提交，避免混入本任务记录。
- [x] 记录删除前 worktree 清单，确认目标仅限 `D:\IntRuoyiWorktree\` 下的非 `int_main` worktree。
- [ ] 逐个删除非 `int_main` worktree。
- [ ] 复核最终 worktree 清单只保留 `E:/IntRuoyi`。
- [ ] 记录最终验证、提交并推送任务记录。

## Expected Verification

- `git worktree list --porcelain` 删除前显示存在非 `int_main` worktree。
- 删除命令仅针对 `D:\IntRuoyiWorktree\` 下路径，且不删除 `E:/IntRuoyi`。
- 删除后 `git worktree list --porcelain` 只保留 `E:/IntRuoyi` / `refs/heads/int_main`。
- `git status --short --branch`、任务记录提交和 `git push origin int_main` 完成；若 push 失败，记录 blocker。

## Current Status

in_progress

已完成规则读取与既有脏改动基线提交；准备记录删除前 worktree 清单并执行逐个删除。

## Design Constraints Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，按 Git worktree 元数据删除非主工作区，并保留审计记录。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- Worktree 删除门禁：删除前必须确认目标绝对路径位于 `D:\IntRuoyiWorktree\` 下、Git 注册状态来自 `git worktree list --porcelain`、逐个记录 dirty 状态；dirty worktree 只能在用户明确授权删除目标目录时使用 `--force`。
- 残留目录门禁：若 `git worktree remove` 后出现 `Directory not empty`，不得扩大删除范围；必须确认 Git 注册已移除、残留目录无 `.git`、无归属不明进程且路径仍在 `D:\IntRuoyiWorktree\` 下，才能处理当前目标残留。
- 物理根复核门禁：不能把 Git 注册删除等同于物理目录清理完成；删除后必须复核 `git worktree list --porcelain` 与每个目标 `Test-Path`。

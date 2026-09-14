# 删除已融合 Worktree 跟进任务

## 任务目标

确认 `D:\IntRuoyiWorktree\edhr-latest-published-form` 的分支提交已完整融合进 `int_main`，在工作区干净、无进程和端口占用的前提下，通过 Git 正常移除该 worktree，并同步关闭端口登记项。

## 里程碑

- [x] M1：读取 worktree、PowerShell、任务收尾和项目经验门禁。
- [ ] M2：验证目标路径、Git 注册、分支融合状态、工作区状态、进程与端口占用；当前被未提交任务文档阻塞。
- [ ] M3：正常移除目标 worktree、prune Git 登记并关闭端口登记项。
- [ ] M4：验证 Git 登记、物理目录、端口与登记项均已清理。
- [ ] M5：完成 cleanup preview/apply、任务记录提交和 `int_main` 推送。

## 预期验证

- `git merge-base --is-ancestor codex/edhr-latest-published-form int_main` 返回退出码 `0`。
- `git rev-list --count int_main..codex/edhr-latest-published-form` 返回 `0`。
- 目标 worktree 的 `git status --short` 为空。
- 无进程命令行引用目标路径，端口 `8088` 和 `48088` 均未监听。
- `git worktree list --porcelain` 不再包含目标路径。
- `Test-Path D:\IntRuoyiWorktree\edhr-latest-published-form` 返回 `False`。
- `D:\IntRuoyiWorktree\.ports\worktree-ports.json` 中目标登记项为 `active=false`。
- `git status --short --branch` 最终不再显示 `int_main` 领先 `origin/int_main`。

## 经验门禁

### 已融合 Worktree 删除门禁

- Trigger: 删除 `D:\IntRuoyiWorktree\` 下已融合进 `int_main` 的 linked worktree。
- Preflight check: 核对绝对路径位于允许根目录内，确认 Git 注册、worktree clean、分支是 `int_main` 的 ancestor、无未合入提交、无进程和端口占用。
- Blocker: 路径越界、未合入提交、未提交改动、目录被进程占用、端口仍监听或登记项无法安全关闭时立即停止。
- Verification: 删除后复查 `git worktree list --porcelain`、`Test-Path`、端口监听和端口登记项。
- Forbidden action: 不用 `Remove-Item -Recurse` 替代正常 `git worktree remove`，不使用 `--force` 丢弃未授权改动，不删除本地分支引用。
- Evidence: `docs/worktree-memory.md#worktree-删除门禁`。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；通过 Git worktree 正常移除并同步登记状态，不直接删除目录。
- 是否存在临时补丁或绕过：否。

## Cleanup Keep

- doc/tasks/20260726-delete-merged-worktree-followup/task.md
- doc/tasks/20260726-delete-merged-worktree-followup/execution-log.md
- doc/tasks/20260726-delete-merged-worktree-followup/verification-report.md

## Current Status

blocked

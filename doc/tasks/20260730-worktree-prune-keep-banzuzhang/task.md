# 20260730 Worktree Prune Keep Banzuzhang

## Task Goal

删除当前 Git 登记的 IntRuoyi worktree，保留 `int_main` 和 `20260730-banzuzhang`。

## Milestones

- [x] 读取 worktree、runtime、PowerShell/Git、task closeout 和经验门禁。
- [x] 盘点 Git 登记 worktree、物理目录、运行进程、dirty 状态和分支合入状态。
- [x] 删除满足门禁的目标 worktree，并保留被门禁阻塞的目标。
- [x] 验证 Git 登记列表、目标目录存在性和端口登记状态。

## Expected Verification

- `git -C E:\IntRuoyi worktree list --porcelain`
- 对删除目标执行 `Test-Path`
- 检查 `D:\IntRuoyiWorktree\.ports\worktree-ports.json` 中删除目标已标记为非活动

## Current Status

completed

## Deletion Authorization

- 2026-07-30 用户明确授权删除剩余 5 个受 dirty / 未合入提交门禁阻塞的 worktree，并丢弃其中未提交或未合入内容。

## Final Verification

- `git -C E:\IntRuoyi worktree list --porcelain` 仅保留 `E:\IntRuoyi` 和 `D:\IntRuoyiWorktree\20260730-banzuzhang`。
- 5 个追加授权删除目标均 `Test-Path = False`。
- `D:\IntRuoyiWorktree\.ports\worktree-ports.json` 当前仅 `20260730-banzuzhang` 为 `active = true`。
- 本次任务临时空目录 `D:\IntRuoyiWorktree\.runtime\empty-delete-source-20260730-worktree-prune` 已删除。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，优先使用 `git worktree remove` 并同步端口登记状态。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- `GREEN: experience-preflight -> PASS`：已读取 `docs\experience-index.md` 中 worktree 清理相关索引，并读取 `docs\worktree-memory.md#Worktree 删除门禁`。

# Delete Merged Worktrees

## Task Goal

Delete the following user-specified IntRuoyi worktrees:

- `D:\IntRuoyiWorktree\batch-route-snapshot-e2e-20260724`
- `D:\IntRuoyiWorktree\jiluben_20260722_clean`
- `D:\IntRuoyiWorktree\system-backup-plan`

## Milestones

- [x] Read required worktree and task closeout rules.
- [x] Confirm target worktree paths and merge status.
- [x] Create task documentation before destructive cleanup proceeds.
- [x] Create missing `docs\worktree-memory.md` with user authorization.
- [x] Delete the three worktrees with Git worktree commands.
- [x] Verify the three worktrees are no longer registered or present on disk.
- [x] Record final verification and close task.

## Expected Verification

- `git worktree list --porcelain` no longer lists the three target paths.
- `Test-Path` returns false for all three target directories.
- Port/worktree cleanup evidence is recorded in `execution-log.md`.

## Current Status

completed

The three user-specified worktrees have been removed from Git registration and their physical directories no longer exist. Port registry entries have been marked inactive. Cleanup preview/apply passed with no extra deletions.

## 经验门禁

### Worktree removal cleanup memory

- Trigger: User requests deletion or cleanup of IntRuoyi worktrees under `D:\IntRuoyiWorktree\`.
- Preflight check: Read `docs\worktree-restrictions.md`, `docs\task-closeout-rules.md`, `docs\experience-index.md`, and the matched experience document routed by the index.
- Blocker: Target path outside `D:\IntRuoyiWorktree\`, unmerged commits, or dirty worktree without explicit discard authorization.
- Verification: `docs\worktree-memory.md` exists and records the deletion gate; deletion verification must confirm no target remains registered or present on disk.
- Forbidden action: Do not silently proceed with destructive worktree removal when the matched high-risk experience gate is unavailable.
- Evidence: `docs\experience-index.md` route for `worktree remove failed`, `residual worktree directory`, `Invalid argument`, and `Directory not empty`.

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；已按用户授权补齐长期 worktree 删除门禁，并继续执行当前删除任务。
- `是否存在临时补丁或绕过`：否。

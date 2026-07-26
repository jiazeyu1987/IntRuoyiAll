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
- [ ] Delete the three worktrees with Git worktree commands.
- [ ] Verify the three worktrees are no longer registered or present on disk.
- [ ] Record final verification and close task.

## Expected Verification

- `git worktree list --porcelain` no longer lists the three target paths.
- `Test-Path` returns false for all three target directories.
- Port/worktree cleanup evidence is recorded in `execution-log.md`.

## Current Status

blocked

Deletion is blocked because `docs\experience-index.md` routes worktree deletion/removal cleanup risk to `E:\IntRuoyi\docs\worktree-memory.md`, but that required matching experience document is missing. Under the project high-risk cleanup gate, this must be recorded before proceeding, and the destructive deletion should not continue without explicit user authorization.

## 经验门禁

### Worktree removal cleanup memory

- Trigger: User requests deletion or cleanup of IntRuoyi worktrees under `D:\IntRuoyiWorktree\`.
- Preflight check: Read `docs\worktree-restrictions.md`, `docs\task-closeout-rules.md`, `docs\experience-index.md`, and the matched experience document routed by the index.
- Blocker: `E:\IntRuoyi\docs\worktree-memory.md` is missing, so the matched worktree cleanup memory cannot be checked.
- Verification: Record `BLOCKER: experience-preflight -> missing E:\IntRuoyi\docs\worktree-memory.md` before any destructive deletion.
- Forbidden action: Do not silently proceed with destructive worktree removal when the matched high-risk experience gate is unavailable.
- Evidence: `docs\experience-index.md` route for `worktree remove failed`, `residual worktree directory`, `Invalid argument`, and `Directory not empty`.

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：否；当前阻塞于缺失的 worktree cleanup 经验门禁，需用户明确授权继续或补齐经验文档。
- `是否存在临时补丁或绕过`：否。

# fancha0925 Worktree Closeout

## Task Goal

Remove the already-integrated `fancha0925` worktree, preserve ignored task records, and release its runtime slot only after the checkout is removed.

## Milestones

- M1 completed: verify the branch is integrated into `int_main`, inspect worktree status, runtime reservation, ignored task artifacts, and processes.
- M2 completed: preserve ignored task records and archive/remove the managed worktree.
- M3 completed: release the runtime slot and verify Git registration and port reservation state.

## Expected Verification

- `fancha0925` is an ancestor of `int_main`.
- No tracked or non-ignored untracked changes are lost; ignored task records are preserved and hash-verified before archive.
- The exact managed worktree is archived/removed; no unrelated worktree, branch, or process is modified.
- Slot 37 (`int_main`, 8212/48212) is marked inactive only after removal.

## Current Status

completed：用户授权后，仅提交并推送本任务收尾记录；worktree 归档、目录移除、槽位释放及最终清理验证均通过。

## 设计约束检查

- Keep all existing `int_main` edits and the other active `gxp-integration` worktree untouched.
- Preserve the local recoverable archive and the original feature branch; do not delete the branch.
- Commit and push only this task's closeout records; preserve all unrelated and parallel edits in `int_main`.
- Use the Codex managed worktree archive operation, not shell recursive deletion.
- Do not stop processes unless their ownership is proven to be this worktree; none is currently listening on its registered ports.
- Keep the remaining parent directory because it contains preserved logs and a source-file copy; removing it is outside the exact Git checkout boundary.

## Cleanup Keep

- doc/tasks/20260928-fancha0925-worktree-closeout/archived-task-records/

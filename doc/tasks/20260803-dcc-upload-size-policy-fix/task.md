# DCC Upload Size Policy Fix

## Task Goal

Fix the DCC upload failure that reports `DCC upload size policy is missing or invalid` when a user uploads a file, without adding fallback or hiding policy errors.

## Milestones

- [x] M1: Reproduce the upload-size-policy failure with a targeted regression check.
- [x] M2: Identify and fix the root cause in the formal upload-size-policy path.
- [x] M3: Run targeted regression verification for the DCC upload policy path.
- [ ] M4: Record verification evidence and complete closeout. Cleanup preview is blocked by the dirty main worktree.

## Expected Verification

- Backend regression test proving an upload can resolve an approved effective size policy.
- Targeted Maven test for DCC upload-size-policy behavior.
- Bug regression evidence validator for this task.

## Current Status

ready_for_closeout

## Design Constraint Check

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，目标是在正式上传大小策略配置链路中修复缺失/无效策略问题。
- 是否存在临时补丁或绕过：否。

## Applicable Gates

- 使用 `bug-regression-fix-loop`：必须先复现/RED，再做最小正式修复并 GREEN。
- `docs/task-closeout-rules.md`：任务文档、BDD/TDD、验证报告和收尾状态必须完整。
- `docs/powershell-encoding.md` / `docs/powershell-memory.md`：PowerShell 不使用 `&&`，中文文档 UTF-8，Maven `-D` 参数整体加引号。
- `docs/worktree-restrictions.md` / `docs/branch-runtime-ports.md`：当前修复在 `D:\IntRuoyiWorktree\dcc-upload-size-policy-fix` 隔离 worktree 中进行；已登记 `int_main` profile slot 14，端口 `8095/48095`，未启动服务。

## Baseline Notes

- Existing dirty worktree baseline commits created before this task's implementation: `2ddc9b122`, `14da650fd`, `8300af6d6`.
- Isolated branch/worktree: `codex/dcc-upload-size-policy-fix` at `D:\IntRuoyiWorktree\dcc-upload-size-policy-fix`.
- Implementation commit: `627951dc7 fix: seed DCC upload size policies`.
- Closeout record commit: `87d49f09a docs: record DCC upload size policy closeout blocker`.
- Branch push: `codex/dcc-upload-size-policy-fix` pushed to `origin` after one-off clearing of the stale scoped GitHub proxy config for the push command.

## Closeout Blockers

- Cleanup preview kept `task.md`, `execution-log.md`, and `verification-report.md`; task-owned temporary artifacts were removed after their summaries were copied into retained records.
- `task-closeout-cleanup --mode apply` cannot run yet because `E:\IntRuoyi` main worktree is dirty and the linked worktree cannot be fast-forward merged into `int_main`.
- Latest cleanup preview remains blocked with no delete candidates: current branch cannot be ff-only merged into `int_main`, and main worktree `E:\IntRuoyi` is dirty.

# Worktree Memory

## Worktree 删除门禁

- Trigger: 删除、清理、合并后移除、修复残留目录、处理 `git worktree remove` 失败、`Directory not empty`、`Invalid argument`、或断链 worktree。
- Preflight check: 先读取 `docs\worktree-restrictions.md`，确认目标绝对路径位于 `D:\IntRuoyiWorktree\` 下；用 `git worktree list --porcelain` 确认 Git 注册状态；用 `git status --short` 记录每个目标 worktree 的未提交变更；用 `git merge-base --is-ancestor <branch> int_main` 或等效命令确认分支提交是否已合入目标基线。
- Blocker: 目标不在 `D:\IntRuoyiWorktree\` 下、目标不是用户明确指定的当前任务对象、分支仍有未合入提交、存在未提交变更但用户未明确授权丢弃、目录被运行进程占用、或端口登记表需要释放但无法验证目录已删除。
- Verification: 删除后必须重新运行 `git worktree list --porcelain`，并对每个目标执行 `Test-Path`；若存在端口登记项，只有在目录已删除且任务记录完成后才允许将槽位标记为可复用；验证结果写入当前 `doc\tasks\<task-id>\execution-log.md`。
- Forbidden action: 禁止用 `Remove-Item -Recurse` 替代正常 `git worktree remove` 作为首选路径；禁止删除未指定 worktree；禁止因为 `Directory not empty` 就扩大清理范围；禁止静默丢弃未提交变更；禁止删除或释放其他任务的端口登记项。
- Evidence: 2026-07-26 删除已合入 worktree 前补齐长期经验门禁，要求先确认合入状态、未提交变更授权、路径边界和删除后注册状态。

## 删除操作顺序

1. 阶段 1：目标确认
   必查项：用户指定路径、绝对路径、Git worktree 注册、当前分支、HEAD、是否在 `D:\IntRuoyiWorktree\` 下。
   推荐命令：`git worktree list --porcelain`、`Resolve-Path`、`git -C <path> status --short`。
   Fail Fast：路径越界、路径不存在但仍有 Git 注册残留、或目标不是当前任务指定对象。
   必须记录：路径、分支、HEAD、dirty 文件数量。

2. 阶段 2：合入与脏变更检查
   必查项：目标分支是否已合入 `int_main`，是否存在未提交变更，用户是否授权丢弃。
   推荐命令：`git merge-base --is-ancestor <branch> int_main`、`git rev-list --count int_main..<branch>`、`git -C <path> status --short`。
   Fail Fast：未合入提交数量大于 0，或 dirty worktree 未获得明确删除授权。
   必须记录：是否已合入、未合入提交数、dirty 文件数量和授权依据。

3. 阶段 3：删除与残留处理
   必查项：优先使用 `git worktree remove <path>`；dirty worktree 仅在已获授权时使用 `--force`。
   推荐命令：`git worktree remove --force <path>`、`git worktree prune`。
   Fail Fast：删除失败且原因不是当前目标自身残留；不要扩大到父目录或其他 worktree。
   必须记录：删除命令、退出码、失败文本或成功结果。

4. 阶段 4：收尾验证
   必查项：Git 注册列表、物理目录、端口登记项、任务日志。
   推荐命令：`git worktree list --porcelain`、`Test-Path <path>`、读取 `D:\IntRuoyiWorktree\.ports\worktree-ports.json`。
   Fail Fast：任一目标仍注册、物理目录仍存在、或端口登记状态无法解释。
   必须记录：最终 worktree 列表、目录存在性、端口登记表处理结果。

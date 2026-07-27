# 20260727 Merge D Worktrees

## Task Goal

将 `D:\IntRuoyiWorktree\` 下当前登记在 `E:\IntRuoyi` Git 仓库中且已 clean、可验证合入的附加 worktree 安全融合进 `int_main`，确认合并结果和必要验证后删除这些已处理 worktree；dirty 或验证阻塞的 worktree 保留不动。

## Milestones

- [x] 创建任务记录并读取 worktree/Git/收尾/经验门禁。
- [x] 盘点主工作区和全部 `D:\IntRuoyiWorktree\` worktree 的分支、remote、脏状态、是否已合入 `int_main`。
- [x] 对 clean 且可验证合入的 worktree 按可验证顺序融合到 `int_main`，dirty 或阻塞 worktree 保留不动。
- [x] 运行合并后保护门禁与可行验证，确认 `int_main` 状态。
- [x] 删除已成功融合的附加 worktree，并记录最终 worktree 列表。
- [x] 完成收尾清理、经验沉淀、提交与推送。

## Expected Verification

- `git status --short --branch`
- `git worktree list --porcelain`
- `git merge-base --is-ancestor <worktree-head> int_main`
- `scripts\preflight\branch-runtime-port-guard.ps1`
- 必要时按合并影响运行聚焦构建/测试。
- 删除后确认 `D:\IntRuoyiWorktree\` 下目标 worktree 不再登记或存在。

## Current Status

completed：已处理本轮 clean 且验证可合入的 worktree。`codex/edhr-latest-published-form` 已合入 `int_main`，`codex/20260727_pici` 已确认早已是 `int_main` 祖先；两者对应 worktree 已删除并释放端口登记。`codex/codex-test-process-route` 因 add/add merge conflict 保留，`codex/202607727_yingshe` 因 dirty 且自身任务 blocked 保留，`codex/20260727-todo-task-hidden-status` 为本轮中新出现的其他任务 worktree，非本轮处理对象。最终验证与 cleanup apply 已通过；提交和推送证据记录在 `execution-log.md`。

## Design Constraint Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，按 Git 合并关系与项目 worktree 规则处理，不用拷贝覆盖或手工绕过。
- `是否存在临时补丁或绕过`：否。

## Experience Gates

- `docs\worktree-memory.md#多 Worktree 批量融合门禁`：
  - 先冻结 `int_main` dirty 基线。
  - 逐个 worktree 记录 branch、HEAD、`git status --short` 和目标验证命令。
  - dirty 内容必须在原分支形成独立可追溯提交。
  - 按依赖和冲突风险顺序逐分支 merge，每次冲突修复后运行该分支目标测试。
  - 不能证明 `git merge-base --is-ancestor <branch> int_main` 时不得删除 worktree。
- `docs\worktree-memory.md#Worktree 删除门禁`：
  - 删除前确认目标在 `D:\IntRuoyiWorktree\` 下、Git 注册状态、clean 状态和合入状态。
  - 首选 `git worktree remove <path>`，删除后重新确认注册列表和物理目录。

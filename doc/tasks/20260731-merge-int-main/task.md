# 融合 int_main 分支

## Task Goal

- 按项目 Git / worktree / closeout 规则确认当前工作区状态，并完成用户要求的 `int_main` 分支融合。

## Milestones

- [x] 读取合并、Git 编排、端口合约、任务收尾和编码相关规则。
- [x] 建立任务记录并读取经验索引门禁。
- [x] 确认当前分支、远端和工作区状态。
- [x] 获取远端状态并执行或判定 `int_main` 合并。
- [x] 运行合并后必要验证并记录结果。

## Expected Verification

- `git status --short --branch`
- `git branch --show-current`
- `git remote -v`
- `git fetch origin`
- `git merge origin/int_main` 或明确记录无需合并的原因
- `scripts\preflight\branch-runtime-port-guard.ps1`

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，按项目合并门禁确认当前分支与远端状态。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- `docs\experience-index.md` 命中 `docs\worktree-memory.md#D-Main 本地主线滞后远端融合门禁`、`#并行主工作区远端快进融合门禁`、`#多 Worktree 批量融合门禁`。
- 本次当前目录为 `D:\ProjectPackage\IntRuoyi\IntRuoyiAll`，当前分支为 `int_main`，适用 D-Main 本地主线与远端同步门禁。
- 合并前必须记录 `git status --short --branch`、`git rev-list --left-right --count HEAD...origin/int_main`、本地/远端差异；合并后必须运行 `scripts\preflight\branch-runtime-port-guard.ps1`。

## Experience Consolidation

- 已复核现有 `docs\worktree-memory.md` 中 D-Main 本地主线滞后远端融合门禁，本次执行未发现新的可复用经验，不新增长期经验文档。

# repair-jiluben-worktree-20260724

## Task Goal

重新创建干净的 `jiluben_20260722` 相关 worktree，并把断链快照中需要保留的改动比对、迁移到新 worktree。

## Milestones

- [x] 确认源仓库、断链快照和目标 worktree 根目录状态
- [x] 创建干净 mono-repo worktree
- [x] 比对断链快照与干净 worktree 的差异
- [x] 迁移需要保留的改动并验证 Git 状态
- [x] 完成结构化验证和任务记录

## Expected Verification

- `git status --short --branch` 能在新 backend/frontend worktree 正常运行
- 新 worktree 的 `.git` 指向存在的 Git 元数据
- 迁移后差异可用 `git diff --stat` 和 `git status` 追踪
- 不修改断链快照目录中的原始文件

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否
- `是否从根因和长期维护角度解决`：是，重建有效 Git worktree 并迁移可追踪改动
- `是否存在临时补丁或绕过`：否

## Notes

- 断链目录：`D:\ProjectPackage\Int\IntRuoyiWorktrees\jiluben_20260722`
- 目标 worktree 根目录必须位于：`D:\IntRuoyiWorktree\`
- 新 worktree：`D:\IntRuoyiWorktree\jiluben_20260722_clean`
- 新分支：`repair/jiluben-20260722-clean`
- `docs\experience-index.md` 缺失；本任务未执行删除、发布、远程服务或生产数据操作，按恢复性 worktree 迁移继续并记录风险。
- 非主分支 worktree 已补登端口槽位：slot `1`，前端端口 `8082`，后端端口 `48082`；服务未启动。
- Cleanup apply 已删除任务内临时清单，保留 `task.md`、`execution-log.md` 和 `verification-report.md`。
- 经验沉淀已合并到 `docs\worktree-restrictions.md` 的“断链快照恢复规则”。

## Cleanup Candidates

- `doc/tasks/repair-jiluben-worktree-20260724/diff-candidates.json`
- `doc/tasks/repair-jiluben-worktree-20260724/migration-report.json`

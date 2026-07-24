# Task: 限制 IntRuoyi worktree 创建目录

## Task Goal

按用户要求更新当前项目 `AGENTS.md`，明确所有 IntRuoyi 相关 worktree 只能创建在 `D:\IntRuoyiWorktree\` 目录下，禁止在其他位置创建或复用 worktree。

## Milestones

- [x] 创建任务目录并记录需求
- [x] 更新 `AGENTS.md` 的 worktree 目录约束
- [x] 验证规则文本、编码和路径约束
- [x] 收尾并记录最终验证结果

## Expected Verification

- `AGENTS.md` 包含 `D:\IntRuoyiWorktree\`。
- `AGENTS.md` 明确限制只能在该目录下创建 worktree。
- `AGENTS.md` 可按 UTF-8 正常读取。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，将 worktree 位置作为项目级硬性规则记录。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- `docs/experience-index.md` 当前不存在；本任务仅更新项目规则文档，不执行真实 E2E、服务器、数据库、发布、备份、恢复、worktree 创建/合并/清理等高风险动作。

## Current Status

completed

## Final Verification Result

PASS。`AGENTS.md` 已限制所有 IntRuoyi worktree 只能创建在 `D:\IntRuoyiWorktree\` 下，并已确认该目录存在。UTF-8 读取、关键规则检查、task-closeout-cleanup preview/apply 均通过。

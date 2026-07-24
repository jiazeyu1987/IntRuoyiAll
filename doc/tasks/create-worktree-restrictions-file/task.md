# Task: 创建 worktree 限制文件

## Task Goal

按用户要求整理 IntRuoyi worktree 相关限制为独立规则文件，并更新 `AGENTS.md`，要求每次创建 worktree 前必须先读取该限制文件。

## Milestones

- [x] 创建任务目录并记录需求
- [x] 创建 `docs/worktree-restrictions.md`
- [x] 更新 `AGENTS.md` 强制预读限制文件
- [x] 验证规则文件、引用和编码
- [x] 收尾并记录最终验证结果

## Expected Verification

- `docs/worktree-restrictions.md` 存在并包含 worktree 根目录、主分支、端口槽位和冲突处理规则。
- `AGENTS.md` 明确创建 worktree 前必须先读取 `docs/worktree-restrictions.md`。
- 两个文档均可按 UTF-8 正常读取。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，将 worktree 创建、端口分配和冲突处理固化为项目级规则文件。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- `docs/experience-index.md` 当前不存在；本任务只创建项目文档和更新规则引用，不创建 worktree、不启动/停止端口、不操作服务器、不执行 E2E、不触碰数据库。
- 用户已明确要求创建 worktree 限制文件，因此允许新建长期 worktree 规则文档。

## Current Status

completed

## Final Verification Result

PASS。已创建 `docs/worktree-restrictions.md`，并已在 `AGENTS.md` 中规定创建、启动、停止、重启、合并或清理任何 IntRuoyi worktree 前必须先读取该限制文件。UTF-8 校验、关键规则校验和 task-closeout-cleanup preview/apply 均通过。实现提交为 `eb8f78bc`。

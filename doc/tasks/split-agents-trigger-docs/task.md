# Task: 拆分 AGENTS 触发式专项规则

## Task Goal

将 `AGENTS.md` 中适合“使用时再读取”的规则整理为专项文档，并在 `AGENTS.md` 中建立触发式必读索引，降低总纲冗余并减少端口、E2E、数据库、编码、发布和收尾操作误用风险。

## Milestones

- [x] 创建任务目录并记录现状
- [x] 新增专项规则文件
- [x] 更新 `AGENTS.md` 触发式必读索引
- [x] 验证专项文件、引用和编码
- [x] 收尾并记录最终验证结果

## Expected Verification

- `docs/local-runtime.md`、`docs/e2e-rules.md`、`docs/database-rules.md`、`docs/powershell-encoding.md`、`docs/task-closeout-rules.md`、`docs/release-backup-restore.md` 存在并可按 UTF-8 读取。
- `AGENTS.md` 明确列出触发式专项文件和读取条件。
- 不修改已有未跟踪的 `docs/server-access.md`、`docs/login-access.md` 内容。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，将高频高风险规则拆成触发式必读文档。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- `docs/experience-index.md` 当前不存在；本任务只新增和引用项目规则文档，不启动服务、不创建 worktree、不操作服务器、不执行 E2E、不触碰数据库。
- 已读取现有 `docs/server-access.md` 与 `docs/login-access.md`，当前不修改这两个未跟踪文件。

## Current Status

completed

## Final Verification Result

PASS。已新增 6 个触发式专项规则文件，并在 `AGENTS.md` 中建立 `Trigger-Read Rule Files` 索引。UTF-8 读取、引用完整性、cleanup preview/apply 均通过。实现提交为 `cec604a5`。

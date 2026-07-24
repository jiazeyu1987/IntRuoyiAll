# Task: 拆分后端和前端触发式规则

## Task Goal

继续整理 `AGENTS.md`：将后端开发和前端开发规则拆为按需读取的专项文件，并在总纲中明确对应触发场景。

## Milestones

- [x] 识别现有触发式规则和无关并发改动
- [x] 新增后端和前端专项规则文件
- [x] 更新 `AGENTS.md` 触发式必读索引
- [x] 验证文件、引用和编码
- [x] 收尾并记录最终验证结果

## Expected Verification

- `docs/backend-development.md` 和 `docs/frontend-development.md` 存在并可按 UTF-8 读取。
- `AGENTS.md` 明确后端或前端改动前必须读取对应专项文件。
- 本任务不暂存或提交无关并发任务的改动。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，将后端和前端实施规则归入可触发读取的专项文件。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- `docs/experience-index.md` 已读取：本任务命中“经验沉淀 / 新增经验 / 更新经验”路由；新增规则应优先写入已有主题文档，避免重复入口。
- 本任务只整理规则文档和引用，不创建 worktree、不启动/停止端口、不操作服务器、不执行 E2E、不触碰数据库，因此不触发高风险 `experience-preflight`。
- 当前 `docs/experience-index.md` 是其他未跟踪任务文件；本任务不修改其索引内容。

## Current Status

completed

## Final Verification Result

PASS。已新增 `docs/backend-development.md` 与 `docs/frontend-development.md`，并在 `AGENTS.md` 中要求对应改动前先读取专项文件。UTF-8、引用、`git diff --check` 和 task-closeout-cleanup preview/apply 均通过。实现提交为 `457ec633`。

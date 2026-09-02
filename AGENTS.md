# IntRuoyi Agent Instructions

适用于 `E:\IntRuoyi`，最近层级 `AGENTS.md` 优先。后端为 Java 17/Spring Boot/Maven，前端为 Vue 3/Vite/TypeScript；主分支 `int_main`。

- 修改、测试、运行、Git、数据库、E2E、发布、服务器或 worktree 操作前，先读 `docs` 中对应规则及 `task-closeout-rules.md`；文件缺失即阻塞。
- 默认禁止 fallback、降级、吞异常、模拟成功和兼容补丁；缺少依赖、数据、权限或服务时准确报错，不得猜测或绕过。
- 改文件前建立 `doc/tasks/<task-id>/`，记录目标、里程碑、验证、状态和设计约束。功能、修复、重构须先写 Given/When/Then，再以 RED/GREEN 完成严格 TDD；文档变更做结构验证。完成时先标记 `ready_for_closeout`，清理验证后再标记 `completed`。
- E2E 仅在用户当轮明确要求时执行，必须用 Playwright 走真实页面、真实测试账号和任务自有数据；E2E PASS 的前提是：被验收业务动作全部由 Playwright 在真实前端页面上完成，API/DB 仅允许只读核验，不允许承担任何被验收动作；不得用 API 或 mock 替代。
- 未经当轮明确授权，不得启用子 Agent、执行 Git 提交/推送、操作远程服务器、发布、数据库写入，或停止/重启 `int_main` 后端服务；附加 worktree 内可以重启当前任务自有后端，但必须遵守 `docs/worktree-restrictions.md` 的槽位、端口和归属检查，不得影响 `int_main` 或其他任务。额外 worktree 仅可位于 `D:\IntRuoyiWorktree\`，先预约槽位，禁止占用 `48081`。
- “工序开始”“批记录表单”“表单槽位”是三条独立链路；批记录只取逐工序正式绑定，表单槽位只取 `formBindings`，不得互相补齐或推断，验证也须分别覆盖。
- PowerShell 禁用 `&&`，中文统一 UTF-8；只处理当前任务资产，不动并行或无关改动。回复简洁、面向业务；发现需求有误须先核对并指出。

## Branch Runtime Port Matrix

- Branch runtime port matrix: `docs\branch-runtime-ports.md`.
- `int_main_d=8101/48101`.
- Additional worktree slot in `1..100`; reserve it with `reserve-worktree-slot.ps1` before starting services.

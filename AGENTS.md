# IntRuoyi Agent Instructions

适用于当前 IntRuoyi Git 仓库根目录，不绑定电脑盘符或绝对路径；最近层级 `AGENTS.md` 优先。后端为 Java 17/Spring Boot/Maven，前端为 Vue 3/Vite/TypeScript；仓库集成主分支为 `int_main`，本机文控开发分支为 `int_qms`。

## 本机分工与默认提交目标

- 用户使用两台电脑分工：另一台电脑在 `int_main` 进行主程序开发；本机在 `int_qms` 进行文控（DCC）模块开发、问题修复、验证和代码提交。
- 本机各 Codex 线程默认以 `int_qms` 为文控任务的开发、验证和提交分支。本机用户说“提交代码”或“提交主干代码”且未另行指定分支时，目标是当前文控分支 `int_qms`，不要因仓库集成主分支为 `int_main` 就改变提交目标。
- 执行任务和 Git 操作前，以 `git branch --show-current` 核对实际分支；目录名即使含 `int_main` 也不能作为分支依据。实际分支与本机约定不一致时，先核对原因，不自动切换。
- 只有用户明确要求时，才将文控改动合入 `int_main` 或切换到 `int_main`；默认提交到 `int_qms`，获准推送时推送到 `origin/int_qms`。
- 本节仅明确工作分工与目标分支，不构成常驻 Git 提交/推送、E2E、数据库写入、服务重启或发布授权；仍遵守下列当轮授权规则。

## 通用执行规则

- 修改、测试、运行、Git、数据库、E2E、发布、服务器或 worktree 操作前，先读 `docs` 中对应规则及 `task-closeout-rules.md`；文件缺失即阻塞。
- 默认禁止 fallback、降级、吞异常、模拟成功和兼容补丁；缺少依赖、数据、权限或服务时准确报错，不得猜测或绕过。
- 改文件前建立 `doc/tasks/<task-id>/`，记录目标、里程碑、验证、状态和设计约束。功能、修复、重构须先写 Given/When/Then，再以 RED/GREEN 完成严格 TDD；文档变更做结构验证。完成时先标记 `ready_for_closeout`，清理验证后再标记 `completed`。
- E2E 仅在用户当轮明确要求时执行，必须用 Playwright 走真实页面、真实测试账号和任务自有数据；E2E PASS 的前提是：被验收业务动作全部由 Playwright 在真实前端页面上完成，API/DB 仅允许只读核验，不允许承担任何被验收动作；不得用 API 或 mock 替代。
- 未经当轮明确授权，不得启用子 Agent、执行 Git 提交/推送、操作远程服务器、发布、数据库写入，或停止/重启 `int_main` 后端服务；附加 worktree 内可以重启当前任务自有后端，但必须遵守 `docs/worktree-restrictions.md` 的槽位、端口和归属检查，不得影响 `int_main` 或其他任务。worktree 根目录可按电脑环境配置，先预约槽位，禁止占用 `48081`。
- “工序开始”“批记录表单”“表单槽位”是三条独立链路；批记录只取逐工序正式绑定，表单槽位只取 `formBindings`，不得互相补齐或推断，验证也须分别覆盖。
- PowerShell 禁用 `&&`，中文统一 UTF-8；只处理当前任务资产，不动并行或无关改动。回复简洁、面向业务；发现需求有误须先核对并指出。

## Branch Runtime Port Matrix

- Branch runtime port matrix: `docs\branch-runtime-ports.md`.
- `int_main_d=8101/48101`.
- Additional worktree slot in `1..100`; reserve it with `reserve-worktree-slot.ps1` before starting services.

# E2E测试环境(禁止删除)
E2E验证租户 芋道源码
用户名 admin
密码 admin123
只能通过前端操作，不能直接调用接口,比如不能使用 fetch/apiGet 等方式

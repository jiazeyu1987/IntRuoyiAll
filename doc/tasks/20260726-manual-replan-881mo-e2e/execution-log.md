# Execution Log

## User Intent

- 执行企业级 Playwright 真实浏览器 E2E：排产工单手动重排 `881MO093613/881MO093615`，租户 `1`，入口 `http://127.0.0.1:8081`。

## Rule Preflight

- 已读取 `docs/task-closeout-rules.md`。
- 已读取 `docs/e2e-rules.md`。
- 已读取 `docs/login-access.md`。
- 已读取 `docs/local-runtime.md`。
- 已读取 `docs/worktree-restrictions.md`。
- 已读取 `docs/powershell-encoding.md`。
- 已读取 `docs/experience-index.md`。
- GREEN: experience-preflight -> PASS, 命中真实 E2E 与 Element Plus 表格选择门禁。

## BDD

- BDD: 手动重排仅应用目标工单 -> Given 租户 1 的排产工单页存在来源生产工单号 `881MO093613` 和 `881MO093615`; When 在真实页面筛选、勾选两个目标工单并执行手动重排确认应用; Then 页面提示应用重排成功，apply 请求业务码为 0，只有两个目标工单显示橙色已排产样式，最近成功排产记录为 `REPLAN_APPLY`，生产排产甘特图仅包含两个目标工单。

## Command Evidence

- Pending.

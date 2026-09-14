# 生产组长负责路线展示数据源修复

## 任务目标

- 修复生产组长工作台顶部“负责工艺路线”错误复用工序配置维护范围的问题。
- 新增正式只读职责查询契约，按当前账号在 active `routeStartProductionLeaders` 中的直接账号或角色命中返回路线。
- admin 的全路线工序维护能力继续保留，但顶部职责标签只显示正式配置的路线。

## 里程碑

- [x] M1：读取前后端、E2E、任务收尾规则和相关技能契约。
- [x] M2：确认 Controller/Service/API/页面边界，记录 BDD 并取得前后端 RED。
- [x] M3：实现后端职责查询与前端独立状态源，取得定向 GREEN。
- [x] M4：完成相邻回归、类型检查、证据校验和真实 Playwright 验证。
- [x] M5：完成经验沉淀和 task-closeout-cleanup 收尾。

## 预期验证

- 后端定向测试：有维护权限的 admin 职责查询仍只读取 USER/USERS/ROLE 正式快照；维护列表继续返回全部 active 路线。
- 前端静态合同：顶部标签不再读取 `processConfigRows`，独立调用正式职责接口；加载、空态、失败态明确。
- `pnpm ts:check`。
- 真实 Playwright：`芋道源码/admin` 顶部仅显示两条压力泵路线，工序配置维护列表仍可覆盖其它 active 路线，页面无错误。

## 经验门禁

- 已读取 `docs/experience-index.md`。
- 适用 `docs/backend-development.md#生产组长工序配置维护权限不得被工序开始快照误拦`：维护权限与正式职责必须分开；职责按 active `routeStartProductionLeaders` 的 USER/USERS/ROLE 命中计算。
- 适用前端静态契约隔离门禁：先新增任务专用最小合同取得 RED，再实现最小正式链路。
- 适用 `docs/frontend-development.md#前端多布局模式真实页面门禁`：真实路由使用平铺模式时，必须验证平铺分支的可见职责栏，不能只验证内部页签分支。

## Current Status

completed

独立正式职责查询链路已实现；后端定向 JUnit、前端静态合同、相邻工作台合同、`pnpm ts:check` 和只读 Playwright 均通过。经验门禁已归并到项目长期规则，task-closeout-cleanup preview/apply 已完成且只保留 `task.md`、`execution-log.md`、`verification-report.md` 三份核心记录。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；职责接口失败时显示正式错误并清空，不回退工序维护列表。
- `是否从根因和长期维护角度解决`：是；将职责读取与维护授权拆成独立后端契约和前端状态源。
- `是否存在临时补丁或绕过`：否。

## Cleanup Keep

- doc/tasks/20260807-team-leader-responsible-route-source-fix/task.md
- doc/tasks/20260807-team-leader-responsible-route-source-fix/execution-log.md
- doc/tasks/20260807-team-leader-responsible-route-source-fix/verification-report.md

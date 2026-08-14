# admin 仅负责两条压力泵工艺路线

## 任务目标

在本机 `int_main` 的 tenant `1` 中，仅为 `芋道源码/admin` 保留以下两条当前 active 工艺路线的“工序开始生产组长”配置：

- `922119 / 球囊扩张压力泵`
- `980091 / 按压式球囊扩充压力泵`

从其它当前 active 工艺路线的 `routeStartProductionLeaders` 正式快照中移除 admin，不修改其它生产组长、角色权限、表单槽位或批记录表单配置。

## 里程碑

- [x] M1：读取任务、数据库、登录、E2E、编码和既有经验门禁。
- [x] M2：只读核对 tenant、admin、全部当前 active 路线及 admin 现有配置范围，并完成 RED。
- [x] M3：备份精确目标 active version，事务化收敛 admin 配置范围。
- [x] M4：完成数据库、登录态 API 和真实页面只读验证。
- [x] M5：完成经验沉淀、cleanup preview/apply 和任务收尾。

## 预期验证

- RED：写入前断言 admin 仅命中两条目标 active 路线；若其它 active 路线仍命中 admin，则按预期失败并列出精确路线/version。
- GREEN：写入后 tenant `1` 当前 active 路线中，admin 的 `routeStartProductionLeaders` 命中集合严格等于 `922119, 980091`。
- 保持性：非目标路线中的其它生产组长配置逐项保留；非 active version、其它租户、角色权限、`formBindings` 和批记录表单不变。
- 登录态 API：逐条读取当前 active 路线的 `/mes/pro/route/flow-config/route-start-production-leaders`，仅两条目标路线返回 admin 配置，其余路线返回空配置。
- Playwright：通过本机真实工艺路线页面只读确认两条目标路线显示 admin、至少一条非目标路线不显示生产组长配置，且验证期间无 MES 写请求。

## 经验门禁

- 已读取 `docs/experience-index.md`。
- 适用 `docs/backend-development.md#生产组长工序配置维护权限不得被工序开始快照误拦`：写入和复验均按当前 active version 解析。admin 拥有维护权限时，`process-config/list` 会按正式维护权限展示全部 active 路线，不能用该维护列表证明生产组长职责范围；本任务改用路线配置读取接口逐条证明正式快照。
- 适用 `docs/database-rules.md#数据修复与写入型-E2E-恢复并发门禁`：写入前复核同范围并发写入，事务断言精确影响范围，写入后重新读取当前 active 状态。

## Current Status

completed：数据库、正式配置 API、真实 Playwright 页面、截图检查、经验沉淀、database evidence validator、最终数据库 GREEN 和 task-closeout-cleanup preview/apply 全部通过。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；目标路线、tenant、admin 或正式 active 快照不唯一时直接阻塞。
- `是否从根因和长期维护角度解决`：是；直接收敛生产组长职责的正式 active `routeStartProductionLeaders` 数据源。
- `是否存在临时补丁或绕过`：否；不通过角色权限、前端隐藏或默认成功掩盖配置范围。

## Cleanup Keep

- doc/tasks/20260807-admin-pressure-pump-only-route-start-leader/task.md
- doc/tasks/20260807-admin-pressure-pump-only-route-start-leader/execution-log.md
- doc/tasks/20260807-admin-pressure-pump-only-route-start-leader/verification-report.md
- doc/tasks/20260807-admin-pressure-pump-only-route-start-leader/db-backup/
- doc/tasks/20260807-admin-pressure-pump-only-route-start-leader/db-repair/

## Cleanup Candidates

- output/playwright/20260807-admin-pressure-pump-only-route-start-leader/

# 生产组长工序配置仅展示负责路线工序

## Task Goal

将生产组长工作台“工序配置”中的工序列表限制为当前生产组长正式负责的工艺路线下的工序；其它工艺路线的工序不得显示，也不得通过 admin 维护权限、空列表成功或其它来源兜底扩展范围。

## Milestones

- [x] M1: 定位现有后端工序配置列表的数据源、权限边界和前端使用链路。
- [x] M2: 先补充失败回归测试，证明 admin 当前会看到非正式负责路线工序。
- [x] M3: 后端按正式负责路线过滤工序配置列表，并保留无负责路线时的显式空范围行为。
- [x] M4: 更新前端静态合同与真实 E2E 断言，确保工序配置来源与顶部负责路线一致。
- [x] M5: 运行定向后端、前端静态、类型检查和真实页面验证。
- [x] M6: 更新验证报告、经验记录和收尾状态。

## Expected Verification

- 后端定向测试：`mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderProcessConfigServiceTest,MesRouteStartProductionLeaderAuthorizationServiceTest,MesProcessPoolTeamLeaderControllerTest" test`
- 前端静态合同：`node tests/e2e/team-leader-responsible-routes-static.spec.cjs`
- 前端相邻合同：`node tests/e2e/team-leader-workbench-static.spec.cjs`
- 前端相邻合同：`node tests/e2e/team-leader-process-config-unified-static.spec.cjs`
- 前端相邻合同：`node tests/e2e/team-leader-process-config-filter-query-static.spec.cjs`
- 前端类型检查：`pnpm ts:check`
- 真实页面验证：`node tests/e2e/team-leader-responsible-routes-real.e2e.js`

## Current Status

completed

## BDD Scenarios

- BDD: 工序配置仅展示正式负责路线工序 -> Given admin 当前正式负责的 active 工艺路线只有“球囊扩张压力泵”和“按压式球囊扩充压力泵”; When 打开生产组长工作台的“工序配置”; Then 列表中的工序只能来自这两条正式负责路线; And 其它 active 工艺路线工序不得显示。
- BDD: 禁止维护权限扩大工序配置范围 -> Given 当前用户拥有生产组长维护权限但未被配置为某条路线的正式生产组长; When 请求 `process-config/list`; Then 后端不得因维护权限返回该路线工序; And 不得用维护权限、admin 身份或空值 fallback 替代正式负责路线来源。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，目标是统一后端正式负责路线过滤链路。
- 是否存在临时补丁或绕过：否。

## Experience Gate Summary

- 适用门禁：`docs/backend-development.md#生产组长工序配置维护权限不得被工序开始快照误拦` 曾要求拥有维护权限的 admin 可维护全部 active 路线；本次用户明确更新产品口径为“工序配置里的工序应该来自于这个生产组长负责的工艺路线里的工序，其他的工序不应该显示”，因此本任务需同步修正该旧经验，禁止继续用维护权限扩大工序配置列表。
- 适用门禁：`docs/frontend-development.md#前端多布局模式真实页面门禁`，真实 E2E 需确认当前页面实际布局与工序配置入口，不得只跑静态合同。
- 适用门禁：`docs/powershell-memory.md#powershell-maven--d-参数引号门禁` 与 `#maven-单模块陈旧依赖门禁`，Maven 目标测试必须在 PowerShell 中整体引用 `-D...` 并保留 `-am`。

## Closeout Result

- 2026-08-07: `task-closeout-cleanup` preview/apply 均通过；无 blocked、无 warnings。
- 已清理本任务浏览器截图、临时 `result.json` 和热补丁临时目录；保留 `task.md`、`execution-log.md`、`verification-report.md`。
- 未执行 Git commit/push：当前项目规则要求用户明确请求后才执行 Git 操作。

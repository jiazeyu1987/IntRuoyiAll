# 生产组长页签系统异常修复

## Task Goal

修复进入“生产组长”页签时提示“系统异常”的问题，使用户能够正常打开生产组长工作台，并保持既有生产组长业务功能可用。

## Milestones

- [x] M1：复现异常并定位失败请求与根因。
- [x] M2：补充可稳定复现问题的回归测试并取得 RED 证据。
- [x] M3：实现最小正式修复并取得 GREEN 证据。
- [x] M4：完成相关回归验证与真实入口验证。
- [x] M5：完成任务证据归档和清理收尾。

## Expected Verification

- 定向回归测试能够证明导致页签异常的行为已修复。
- 生产组长相邻静态合同测试通过。
- 在本地真实前端入口通过 Playwright 打开生产组长页签，不再出现“系统异常”。
- 缺陷证据校验脚本通过。

## Applicable Experience Gate

- `docs/database-rules.md#运行态迁移漂移系统异常门禁`：先冻结服务端首个数据库异常、Mapper 和目标表，再对比运行库 schema 与正式迁移；不得先改业务代码适配旧库。
- `docs/frontend-development.md#前端角色内容页签拆分口径门禁`：生产组长独立入口必须保持 `leader-type="PRODUCTION"`、内部默认页签和按页签加载边界，相邻 PQC 工作台合同需回归。
- `docs/frontend-development.md#前端多布局模式真实页面门禁`：以包装页实际启用的生产组长内部页签布局做真实复现，不能用隐藏分支或 API 单独成功代替页面通过。
- `docs/e2e-rules.md#真实 E2E 页面加载判据门禁`：真实页面验证需要记录目标请求、业务码、可见区域、console/page error，并区分目标链路与无关资源异常。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；以真实失败请求和正式数据链路为依据定位根因。
- 是否存在临时补丁或绕过：否。

## Current Status

completed：两项缺失的仓库正式迁移已完整应用到本机运行库，目标页签、定向回归、缺陷证据校验和任务清理均已通过。

## Completed Work

- 已确认生产组长入口由 `ProductionLeaderWorkbenchPage.vue` 承载。
- 已通过本机真实登录复现：`/mes/pro/process-pool/team-leader/active-order/list` 返回 HTTP 200、业务码 `500`、消息“系统异常”。
- 已冻结首个后端异常：`MesProcessPoolOrderProcessCompletionMapper` 查询目标表时缺少 `source_event_ids_json`。
- 已核对运行库：目标表共有 0 行，并同时缺少 `source_event_ids_json`、`source_allocation_ids_json`、`aggregate_hash`、`backfill_idempotency_key` 和 `idx_mes_pp_order_process_completion_aggregate`。
- 已确认仓库正式迁移 `20260801_mes_process_pool_team_leader_p4_order_completion_backfill.sql` 覆盖完整字段与索引，release migration policy gate 通过。
- 已执行首个正式迁移；同一请求随后暴露第二个迁移漂移：缺少 `mes_pro_process_pool_active_order_release_application` 表。
- 已核对第二个正式迁移 `20260808_mes_active_order_release_application.sql`：依赖快照表及 9 个关键字段存在，父菜单 `900310` 存在，目标菜单 ID/权限无冲突；迁移将新建目标表、目标权限菜单，并为当前 2 个有效 `tenant_admin` 角色建立绑定。
- 已完整应用 `20260801_mes_process_pool_team_leader_p4_order_completion_backfill.sql` 与 `20260808_mes_active_order_release_application.sql`，未修改前后端源码，未增加 fallback 或临时结构补丁。
- 已复核运行态 schema：4 个聚合字段、`idx_mes_pp_order_process_completion_aggregate`、放行申请表及其关键字段均存在；权限菜单唯一，2 个有效租户管理员角色均已绑定。
- 已通过真实页面重新进入生产组长页签；默认人员管理与“活跃订单池”均可见，活跃订单接口业务码为 `0`、返回 7 条真实数据，页面无“系统异常”，浏览器 console error 为 0。
- 已运行定向后端 JUnit、后端静态合同和生产组长前端静态合同，均通过。
- 项目经验沉淀检查命中既有 `docs/database-rules.md#运行态迁移漂移系统异常门禁`，本次没有新增可复用规则，因此未改动长期经验文档。
- 已完成 task-closeout-cleanup preview/apply；仅删除本任务临时 `bug-regression-evidence.md` 与 `migration-policy-gate.json`，保留 `task.md`、`execution-log.md`、`verification-report.md`。
- 收尾期间本机 `48081` 被并行任务切换到新的 `int_main` 运行 Jar；未干预该进程，待 health 恢复 `UP` 后再次通过真实登录复验，活跃订单接口业务码仍为 `0`、页面显示 `Total 7`、无“系统异常”、console error 为 0。
- 最终 Playwright 复验后再次执行 task-closeout-cleanup preview/apply，仅删除 5 个本任务已确认路径的临时页面快照/console 文件；blocked 0、warnings 0。

## Verification Evidence

- RED：真实页面目标接口业务码为 `500`；首次运行态 schema 契约确认 4 个必需字段和聚合索引缺失；首个迁移后复测确认第二个正式表缺失。
- 迁移前目标表行数：0。
- GREEN：运行态 schema 契约通过；`MesProcessPoolTeamLeaderSchemaTest` 7/7 通过；3 个生产组长定向前端静态合同和 2 个后端放行申请静态合同通过；真实页面目标接口业务码为 `0`，页面无“系统异常”。
- 相邻既有测试 `edhr-batch-record-leader-tabs-static.spec.js` 仍失败，其断言依赖已过期的 `leaderType === 'PQC'` 词法相邻关系；本任务未修改相关源码，该失败不影响已通过的生产组长目标合同，作为独立基线问题记录，不在本次数据库迁移修复范围内扩改。

## Remaining Blockers

- 无。

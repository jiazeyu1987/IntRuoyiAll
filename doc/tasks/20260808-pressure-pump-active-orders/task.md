# 20260808 Pressure Pump Active Orders

## Task Goal

为“生产组长 > 活跃订单池”补齐按压式扩张压力泵和球囊扩张压力泵产品加入活跃订单的正式链路，使新增多个相关产品订单时能在活跃订单池中显示并保留正式路线、版本、ERP 数量和加入时间。

## Milestones

- [x] 定位活跃订单池前端入口、后端候选/新增/列表接口和测试覆盖。
- [x] 补充 BDD 场景和 RED 测试，复现目标产品活跃订单缺失。
- [x] 按正式路线绑定和发布态版本实现最小修复，不引入 fallback。
- [x] 运行目标 GREEN、相邻回归和静态/编译验证。
- [x] 完成验证报告与收尾记录。

## Expected Verification

- 后端定向测试覆盖按压式扩张压力泵、球囊扩张压力泵相关生产订单可加入并列入活跃订单。
- 前端静态合同或目标检查覆盖活跃订单池仍调用正式候选、新增、列表接口，不使用本地 mock。
- 若需要真实页面验证，则按项目 E2E 规则使用真实前端路径验证新增后列表展示。
- `git diff --check` 通过。

## Current Status

completed - 已通过真实前端路径新增 2 条球囊扩张压力泵活跃订单；按压式球囊扩充压力泵保留 5 条既有 ACTIVE 活跃订单。DB 核验、经验沉淀、cleanup preview/apply 均已完成；未执行 Git stage/commit/push。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；必须沿用生产工单产品正式路线绑定、ACTIVE 发布版本和统一活跃订单服务。
- `是否存在临时补丁或绕过`：否。

## Cleanup Candidates

- doc/tasks/20260808-pressure-pump-active-orders/add-pressure-pump-active-orders.e2e.cjs
- doc/tasks/20260808-pressure-pump-active-orders/artifacts

## Closeout Evidence

- CLEANUP PREVIEW: `task_closeout.py --task-id 20260808-pressure-pump-active-orders --mode preview` -> PASS，blocked/warnings 均为 `<none>`。
- CLEANUP APPLY: `task_closeout.py --task-id 20260808-pressure-pump-active-orders --mode apply` -> PASS，仅删除一次性 Playwright 脚本和 artifacts，保留 `task.md`、`execution-log.md`、`verification-report.md`。
- EXPERIENCE: 已更新 `docs/backend-development.md#零排产活跃订单必须使用发布态正式路线` 与 `docs/experience-index.md`。

## Applicable Experience Gates

### 零排产活跃订单必须使用发布态正式路线

- Trigger: 生产组长活跃订单候选/新增、已确认生产工单没有有效排产工单、`MesTeamLeaderActiveOrderServiceImpl`、`mes_pro_route_product`、`mes_pro_route_version.route_snapshot_json`。
- Preflight check: 先按生产工单产品读取唯一未删除的正式路线绑定，再读取唯一 `active=1 AND lifecycle_status=ACTIVE` 版本；候选资格和新增写入必须复用同一个路线来源解析契约。
- Blocker: 产品无绑定/多绑定、ACTIVE 版本缺失/不唯一、快照节点与 SCHEDULE 配置集合不一致、工序重复、没有启用工序、数量系数非正数、ERP 数量非正数或正式 PQC 规程缺失时必须 fail fast。
- Verification: 后端测试覆盖零排产成功、缺绑定、缺 ACTIVE 版本、快照不完整、单排产继续使用排产路线和多排产冲突。
- Forbidden action: 禁止默认路线、任取第一条绑定/版本、读取草稿当前配置、默认数量系数、空工序成功、前端文案放宽或 API-only 成功。

### 生产组长工序配置必须按正式负责路线限定

- Trigger: 生产组长工作台、工序配置、活跃订单池、正式负责工艺路线、`routeStartProductionLeaders`。
- Preflight check: 区分维护入口权限和正式负责路线范围；活跃订单相关候选不得通过维护权限扩大路线范围。
- Blocker: 用户拥有维护权限但不在正式负责路线内仍可维护或加入非负责路线订单时必须停止。
- Verification: 后端回归需证明正式负责路线限制仍生效。
- Forbidden action: 禁止用 admin 身份、维护权限、空列表成功或前端过滤替代正式后端授权。

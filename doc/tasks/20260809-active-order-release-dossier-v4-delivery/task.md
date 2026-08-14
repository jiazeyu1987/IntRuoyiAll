# 活跃订单放行资料 V4 监督交付

## Task Goal

以 V4 最终开发方案和已通过的 M0 契约为唯一实现基线，组织 A1-A6 六个子 Agent 完成前端、申请编排、三类正式资料 writer、fixture 与真实 E2E；主 Agent 逐项审查、组织独立测试和缺陷返修，最终完成集成与集成测试。

## Milestones

- [x] 复核 V4、M0、验收标准、BDD/TDD/E2E 和当前代码基线。
- [x] A2 完成“当前 apply 未调用 A3/A4/A5”严格 RED。
- [x] A3 完成批记录 writer 并独立验证。
- [x] A4 完成过程检验单 writer 并独立验证。
- [x] A5 完成损耗单 writer 与完成性检查并独立验证。
- [x] A2 完成编排、双 100%、canonical hash、事务和待办集成。
- [x] A1 完成前端契约硬化。
- [ ] A6 完成 fixture manifest、真实页面 E2E 和只读核验。
- [ ] 主 Agent 完成全量集成、独立测试、缺陷闭环和最终审计。

## Expected Verification

- 每个 AC-01 至 AC-15 均有执行证据和独立测试证据。
- 后端聚焦 JUnit、静态合同、schema、compile 通过。
- 前端静态合同和 `pnpm ts:check` 通过。
- 真实 Playwright 路径证明生产/PQC 历史可见、自然双 100%、三类正式资料、签名、唯一负责人待办和最终放行/驳回。
- 同来源重复申请不重复创建 batch、execution、审计、transaction 或 work task。
- 任一正式来源、映射、签名、QA 版本、负责人或真实页面入口缺失时 fail fast，不得记录假 PASS。

## Current Status

blocked

A2-A5 后端实现及主审修正完成，最新稳定窗口串行集成回归 55/55 PASS；A1 前端入口硬化、前端回归及 `pnpm ts:check` PASS。用户已纠正 P7/A6 目标为 `球囊扩张压力泵`，导管路线 `900025` 与产品 `902231/902252/902262/907242` 已标记为当前目标外的 stale 证据。当前压力泵目标为 `922119 / RT000028 / V27 / routeVersionId=627 / ACTIVE`；A6 只读复核和独立测试均确认 blocker 证据可放行，但 P7 仍为 `BLOCKED / NOT COMPLETE`：启用产品 3 个未冻结唯一，当前工序 ID 与 V27 snapshot 工序 ID 口径未冻结，三类传统报表完整组合为 0/14，QA 仅 1/14 PUBLISHED，三类 source mapping 为 0，release owner 仍需真实 UI 登录/签名证明，preflight 仍缺 27 项显式环境变量。A6 本轮业务写、SQL 写、manifest 和残留数据均为 0。用户已授权将 `dev-plan.md` 改名并把七个既有节点转换为监督器里程碑结构；初始化已通过，P1-P6 完成状态及执行/独立测试证据已迁移，当前阶段为 P7/A6。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。正式来源或环境前置缺失时阻塞，不使用 mock、SQL 直改、API-only、默认 MAIN 或 formBindings 替代。
- `是否从根因和长期维护角度解决`：是。复用当前 eDHR、正式批记录、PQC 汇集、字段审计、放行事务和 RELEASE_APPROVE，不新增平行流程。
- `是否存在临时补丁或绕过`：否。

## BDD Scenarios

- BDD: 真实历史形成双 100 后申请 -> Given 生产/PQC 正式历史、签名、三类绑定和负责人完整; When 生产组长真实页面申请; Then 三类资料生成、完成性/precheck 通过并创建唯一负责人待办。
- BDD: 正式来源缺失时阻塞 -> Given 缺任一批记录绑定、PQC CONFIRMED 汇集、损耗映射、签名或负责人; When 申请; Then 返回定位 blocker，生成事务无部分资料且无待办。
- BDD: 重复申请幂等 -> Given 同一来源快照已申请; When 重复提交; Then 返回既有申请且不重复生成任何正式对象。
- BDD: 负责人处理 -> Given 申请处于 PENDING_RELEASE_APPROVAL; When RELEASE_APPROVE 负责人从真实页面批准或驳回; Then eDHR 放行事务进入 RELEASED 或 REJECTED 并保留事件审计。

## Applicable Experience Gates

- `docs/backend-development.md#活跃订单申请放行资料必须只使用正式来源`
- `docs/backend-development.md#edhr-放行负责人来源门禁`
- `docs/backend-development.md#mes-pqc-项目级检验快照门禁`
- `docs/frontend-development.md#前端写入成功与列表刷新失败分层门禁`
- `docs/e2e-rules.md` 的真实页面、任务自有数据和禁止 API-only 门禁
- `docs/powershell-memory.md#Maven-目标目录文件系统异常门禁` 与 `#Maven-javac/Lombok-class-写入长时间运行门禁`

## Cleanup Keep

- doc/tasks/20260809-active-order-release-dossier-v4-delivery/task.md
- doc/tasks/20260809-active-order-release-dossier-v4-delivery/request-analysis.md
- doc/tasks/20260809-active-order-release-dossier-v4-delivery/prd.md
- doc/tasks/20260809-active-order-release-dossier-v4-delivery/dev-plan.md
- doc/tasks/20260809-active-order-release-dossier-v4-delivery/test-plan.md
- doc/tasks/20260809-active-order-release-dossier-v4-delivery/task-state.json
- doc/tasks/20260809-active-order-release-dossier-v4-delivery/execution-log.md
- doc/tasks/20260809-active-order-release-dossier-v4-delivery/test-report.md
- doc/tasks/20260809-active-order-release-dossier-v4-delivery/verification-report.md

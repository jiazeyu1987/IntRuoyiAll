# 无排产工单加入活跃订单

## 任务目标

允许已确认生产工单在不存在有效排产工单时，通过产品正式工艺路线绑定和该路线当前 ACTIVE 版本加入生产组长活跃订单；ERP 计划开工时间为空不再阻塞零排产候选或新增，零排产 PQC 任务使用活跃订单实际加入日期；保留存在一条有效排产工单时的既有解析行为，并对缺失正式路线、版本、工序快照、ERP 数量或 PQC 正式规程的情况 fail fast。

## 用户意图

- 用户明确要求取消“必须存在排产工单”限制。
- 用户明确要求取消“ERP 计划开工时间缺失”限制，并确认 PQC 业务日期不得作为加入限制。
- 无排产工单不是降级路径，而是正式支持的业务模式。
- 零排产 PQC 任务以活跃订单实际加入日期作为正式记录日期；禁止通过默认路线、默认数量系数、空工序或 mock 成功掩盖其它正式来源缺失。

## 适用经验门禁

- `docs/experience-index.md` 已存在并命中 `docs/backend-development.md#MES-工艺路线产品绑定状态门禁`。
- 路线来源使用 `mes_pro_route_product` 的正式产品绑定，禁止使用 `MdItemApi.routeId`、产品名称或前端状态推断。
- 路线版本必须是该正式绑定路线唯一的 `active=1` 且 `lifecycle_status=ACTIVE` 版本；工序和数量系数必须来自该版本发布快照，不读取草稿或猜测当前配置。

## 里程碑

- [x] M1：核对现有排产解析、生产工单字段、产品路线绑定、ACTIVE 版本快照和 PQC 任务依赖。
- [x] M2：记录 BDD 并新增无排产场景 RED 测试。
- [x] M3：实现无排产正式路线解析、候选资格和新增快照/PQC 链路。
- [x] M4：完成目标测试、相邻回归、后端证据校验和真实用户路径 E2E。
- [x] M5：完成上一版经验沉淀和 cleanup preview/apply；Git 推送不属于当前任务完成门禁。
- [x] M6：移除零排产 ERP 计划开工时间门禁，完成 BDD/TDD、聚焦回归、经验修订和收尾。

## 预期验证

- `MesTeamLeaderActiveOrderServiceTest`：无排产成功、正式来源缺失失败、单排产兼容、多排产仍阻塞。
- `MesTeamLeaderActiveOrderServiceTest`：无排产且 ERP 计划开工时间为空时，候选可加入且 PQC 任务记录日期等于活跃订单加入日期。
- `MesProcessPoolTeamLeaderControllerTest`：候选与新增接口合同保持一致。
- MES 模块定向 Maven 测试及必要的相邻回归通过。
- `backend-api-delivery` evidence validator 通过。
- 按真实前端候选搜索与加入路径执行 Playwright E2E；缺少登录、租户、运行态或任务自有数据时必须记录精确 blocker。
- `git diff --check` 通过；本次未获用户授权，不执行 Git 提交或推送。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。用户明确将零排产 PQC 记录日期定义为非加入门禁，系统使用活跃订单实际加入日期；其它正式来源缺失仍直接失败。
- `是否从根因和长期维护角度解决`：是。候选资格与新增链路共享同一正式路线解析契约，避免前后端或查询/写入规则分裂。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed - ERP 计划开工时间与 PQC 业务日期已不再阻塞零排产候选/新增；M6 实现、27 项相关回归、evidence 校验、经验修订和 cleanup 均已完成。

## Cleanup Keep

- doc/tasks/20260807-active-order-without-schedule-order/task.md
- doc/tasks/20260807-active-order-without-schedule-order/execution-log.md
- doc/tasks/20260807-active-order-without-schedule-order/verification-report.md

## Cleanup Candidates

- IntRuoyiFronted/test-results/production-leader-active-order-focused
- output/runtime/int_main-slot8-active-order
- doc/tasks/20260807-active-order-without-schedule-order/erp-planned-start-gate-static.spec.cjs
- doc/tasks/20260807-active-order-without-schedule-order/backend-api-evidence.md
- doc/tasks/20260807-active-order-without-schedule-order/test-classpath.txt
- doc/tasks/20260807-active-order-without-schedule-order/focused-classes
- doc/tasks/20260807-active-order-without-schedule-order/focused-v2
- doc/tasks/20260807-active-order-without-schedule-order/focused-test-classes
- doc/tasks/20260807-active-order-without-schedule-order/focused-controller-classes
- doc/tasks/20260807-active-order-without-schedule-order/javac-main.args
- doc/tasks/20260807-active-order-without-schedule-order/javac-test.args
- doc/tasks/20260807-active-order-without-schedule-order/javac-candidate.args
- doc/tasks/20260807-active-order-without-schedule-order/javac-main-v2.args
- doc/tasks/20260807-active-order-without-schedule-order/javac-support-v2.args
- doc/tasks/20260807-active-order-without-schedule-order/javac-mappers-v2.args
- doc/tasks/20260807-active-order-without-schedule-order/javac-regression-test.args
- doc/tasks/20260807-active-order-without-schedule-order/javac-controller-test.args
- doc/tasks/20260807-active-order-without-schedule-order/junit.args
- doc/tasks/20260807-active-order-without-schedule-order/junit-regression.args

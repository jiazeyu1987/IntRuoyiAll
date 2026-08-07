# 无排产工单加入活跃订单

## 任务目标

允许已确认生产工单在不存在有效排产工单时，通过产品正式工艺路线绑定和该路线当前 ACTIVE 版本加入生产组长活跃订单；保留存在一条有效排产工单时的既有解析行为，并对缺失正式路线、版本、工序快照、ERP 数量、ERP 计划开工时间或 PQC 正式规程的情况 fail fast。

## 用户意图

- 用户明确要求取消“必须存在排产工单”限制。
- 无排产工单不是降级路径，而是正式支持的业务模式。
- 禁止通过默认路线、默认日期、默认数量系数、空工序或 mock 成功掩盖正式来源缺失。

## 适用经验门禁

- `docs/experience-index.md` 已存在并命中 `docs/backend-development.md#MES-工艺路线产品绑定状态门禁`。
- 路线来源使用 `mes_pro_route_product` 的正式产品绑定，禁止使用 `MdItemApi.routeId`、产品名称或前端状态推断。
- 路线版本必须是该正式绑定路线唯一的 `active=1` 且 `lifecycle_status=ACTIVE` 版本；工序和数量系数必须来自该版本发布快照，不读取草稿或猜测当前配置。

## 里程碑

- [x] M1：核对现有排产解析、生产工单字段、产品路线绑定、ACTIVE 版本快照和 PQC 任务依赖。
- [x] M2：记录 BDD 并新增无排产场景 RED 测试。
- [ ] M3：实现无排产正式路线解析、候选资格和新增快照/PQC 链路。
- [ ] M4：完成目标测试、相邻回归、后端证据校验和真实用户路径 E2E。
- [ ] M5：经验沉淀、cleanup preview/apply、提交并推送。

## 预期验证

- `MesTeamLeaderActiveOrderServiceTest`：无排产成功、正式来源缺失失败、单排产兼容、多排产仍阻塞。
- `MesProcessPoolTeamLeaderControllerTest`：候选与新增接口合同保持一致。
- MES 模块定向 Maven 测试及必要的相邻回归通过。
- `backend-api-delivery` evidence validator 通过。
- 按真实前端候选搜索与加入路径执行 Playwright E2E；缺少登录、租户、运行态或任务自有数据时必须记录精确 blocker。
- `git diff --check`、分支运行端口 guard、Git 提交和 `git push origin int_main` 通过。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。无排产是用户明确批准的正式模式；缺失正式来源直接失败。
- `是否从根因和长期维护角度解决`：是。候选资格与新增链路共享同一正式路线解析契约，避免前后端或查询/写入规则分裂。
- `是否存在临时补丁或绕过`：否。

## Current Status

in_progress - BDD 与 RED 已完成，正在实现无排产正式路线解析。

## Cleanup Keep

- doc/tasks/20260807-active-order-without-schedule-order/task.md
- doc/tasks/20260807-active-order-without-schedule-order/execution-log.md
- doc/tasks/20260807-active-order-without-schedule-order/verification-report.md

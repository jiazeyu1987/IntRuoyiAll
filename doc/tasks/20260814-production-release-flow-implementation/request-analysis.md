# 生产放行闭环实施请求分析

## User Goal

完成生产组长双 100% 完工到 PQC 放行、批次执行与三类正式映射、三类负责人上传四份资料、管理者代表放行和可追溯入列的全链路开发与独立验证。详细业务目标见第 2 节。

## Current System

当前系统已具备部分批次执行、映射、特殊节点附件和最终放行能力，但组长申请时会越级执行下游动作，状态、角色、并发和接口合同均不满足目标。详细代码证据和差距见第 3、4 节。

## Constraints

必须遵守统一接口合同、同步事务、无 fallback、租户角色授权、ID 字符串、严格幂等与乐观锁，以及工序开始、逐工序批记录表单、`formBindings` 表单槽位三条独立链路。详细约束见第 5 至 10 节。

## Unknowns

产品时序和接口已冻结；尚未知的是目标测试租户、账号、三类负责人、正式表单来源、旧数据预检、文件存储、签核和本地运行环境的实际可用性。详细清单见第 12 节。

## Risks

最高风险是旧数据误认领、角色或正式来源缺失、第四份报告与管理者待办非原子、冻结路线漂移、通用接口绕过和大整数精度丢失。详细分级见第 11 节。

## Validation Surface

验证覆盖后端状态机、权限、事务、幂等、并发、唯一性、三类正式来源和报告快照；前端页面与接口合同；以及六类账号的真实 Playwright 路径。详细范围见第 13 节。

## Blocking Prerequisites

目标租户角色和成员、三类报告负责人、三类正式表单来源、旧申请/批次预检、文件存储和签核、数据库/Redis/前后端入口任一缺失时，对应集成或 E2E 必须阻塞，禁止 mock、SQL 补状态或 API-only 降级。详细条件见第 12、15 节。

## 1. 任务定位

- 总任务：`20260814-production-release-flow-implementation`
- 业务代码工作树：`D:\IntRuoyiWorktree\pqc-production-release-flow`
- 本文目的：把已冻结的 SP-0～SP-4 业务目标与当前真实代码逐项对齐，明确实施范围、现状差距、接口边界、迁移阻塞、验证面和失败条件。
- 结论性质：本文是实施输入，不代表当前系统已经具备目标能力。

## 2. 用户意图与冻结业务目标

用户要求形成一条不可跳步、可审计、按角色授权的生产放行闭环：

1. 生产组长只能在生产进度和检测进度均为 100% 时点击完工。
2. 完工成功只创建生产放行申请和 PQC 待办，不得提前创建批次执行、四份资料待办、管理者代表待办或放行事务。
3. PQC 放行权限来自角色 `MES_PQC_RELEASE_OWNER`；目标环境首版将用户 `zhulijiang` 配置为该角色成员，但业务代码不得按用户名判断权限。
4. PQC 审批通过后，系统针对该放行申请创建唯一批次执行，并把冻结生产数据映射到正式生产批记录、过程检验表单、损耗单。
5. 同一事务中创建四份资料待办：来料检报告、灭菌报告、成品检报告、成品检记录。四份资料由三类负责人完成，其中成品检负责人负责后两份。
6. 四份资料全部完成前不得创建管理者代表待办；第四份完成时，系统在同一事务中创建放行事务和管理者代表待办。
7. 管理者代表权限来自角色 `MES_MANAGEMENT_REPRESENTATIVE`；目标环境首版将用户 `xujianhai` 配置为该角色成员，但业务代码不得按用户名判断权限。
8. 管理者代表放行后，申请进入 `RELEASED`；只有 `RELEASED` 数据可进入可追溯列表。
9. 全流程禁止 fallback、模拟成功、默认成功、静默降级、猜测式数据关联或用非正式表单来源代替正式来源。

## 3. 已审阅输入

### 3.1 已评审业务与接口文档

- `doc/tasks/20260814-active-order-release-flow-docs/` 下的总 PRD、开发文档、子项目计划和统一接口合同。
- `doc/tasks/20260814-sp0-release-roles-baseline/`：角色、权限、候选人解析和数据基线。
- `doc/tasks/20260814-sp1-team-leader-pqc-release-task/`：生产组长完工至 PQC 待办。
- `doc/tasks/20260814-sp2-pqc-release-batch-execution/`：PQC 决策、批次执行和三类正式映射。
- `doc/tasks/20260814-sp3-report-upload-gate/`：三类负责人、四份资料和第四份完成门禁。
- `doc/tasks/20260814-sp4-manager-release-traceability/`：管理者代表放行和可追溯列表。
- 统一接口合同：`doc/tasks/20260814-active-order-release-flow-docs/interface-contract.md`。

### 3.2 已核对真实代码

下列代码证据来自工作树 `D:\IntRuoyiWorktree\pqc-production-release-flow`：

- 组长放行编排：`MesTeamLeaderActiveOrderReleaseGenerationService.java`
- 申请持久化和返回模型：`MesTeamLeaderActiveOrderReleaseApplicationPersistenceService.java`、`MesTeamLeaderActiveOrderReleaseApplicationResult.java`、`MesTeamLeaderActiveOrderReleaseApplyRespVO.java`
- 组长接口：`MesProcessPoolTeamLeaderController.java`
- 当前申请表：`20260808_mes_active_order_release_application.sql`
- 批次执行和特殊节点：`MesProEdhrBatchExecutionServiceImpl.java`
- 工作任务：`MesProEdhrWorkTaskServiceImpl.java`、`MesProEdhrWorkTaskDO.java`、`MesProEdhrWorkTaskRespVO.java`、`MesProEdhrWorkTaskPageReqVO.java`
- 三类映射：`MesTeamLeaderActiveOrderReleaseBatchRecordWriterImpl.java`、`MesTeamLeaderActiveOrderReleaseProcessInspectionWriterImpl.java`、`MesTeamLeaderActiveOrderReleaseLossReportWriterImpl.java`
- 放行事务：`MesProEdhrReleaseServiceImpl.java`
- 工艺路线特殊节点负责人：`MesProRouteFlowConfigServiceImpl.java`
- 前端组长工作台：`TeamLeaderWorkbenchPage.vue`、`teamLeader.ts`
- 前端可追溯/放行查询：`FormTraceReleaseTab.vue`、`ReleasePage.vue`

## 4. 当前实现结论

当前系统不是目标流程。它只有部分可复用能力，主时序、状态模型、角色模型和并发合同均不满足冻结目标。

| 领域 | 当前真实行为 | 与目标的差距 | 关键代码证据 |
| --- | --- | --- | --- |
| 组长门禁 | 前后端已校验生产和检测进度均为 100% | 该门禁可复用，但完工后的动作错误 | `TeamLeaderWorkbenchPage.vue` 的双进度判断；`MesTeamLeaderActiveOrderReleaseGenerationService.generate` 的完成度校验 |
| 组长完工后时序 | 当前服务立即规划并写入三类表单，调用 `openOrCreate` 创建批次执行，再调用 `submitForApproval` 创建放行事务和最终审批任务 | 目标只允许创建申请和 PQC 待办，当前严重越级 | `MesTeamLeaderActiveOrderReleaseGenerationService.java` 中 `openOrCreate`、三个 writer 调用和 `submitForApproval` |
| 申请状态 | 当前仅有 `BLOCKED`、`PENDING_RELEASE_APPROVAL` | 缺少 PQC、四报告和管理者代表阶段状态；旧状态不可直接解释为新流程 | 申请 SQL、DO、持久化服务常量 |
| 申请数据 | 当前保存 `batch_execution_id`、`release_transaction_id`、`release_approval_work_task_id`，没有 PQC 任务、PQC 决策、报告快照、聚合版本 | SP-1 所需字段和唯一约束不存在 | `20260808_mes_active_order_release_application.sql`、申请 DO |
| PQC 角色/任务 | 仓库中没有 `MES_PQC_RELEASE_OWNER`、`PQC_PRODUCTION_RELEASE` 和目标 PQC 权限 | 需要新增角色基线、候选人解析、任务类型和接口 | 全仓检索无目标角色码、任务类型和权限码 |
| 批次执行唯一关联 | 当前 `active_context_key` 按工单、路线、批号生成，并选择当前活动路线版本 | 目标必须按 `PQC_RELEASE:{applicationId}` 唯一，并使用申请冻结的路线版本；不得漂移到新版本 | `MesProEdhrBatchExecutionServiceImpl.openOrCreate` |
| 批记录映射 | 已按工序正式批记录绑定检查并写入 | 可复用，但必须受 PQC 审批事务和冻结来源约束 | `MesTeamLeaderActiveOrderReleaseBatchRecordWriterImpl` |
| 过程检验/损耗映射 | 当前 writer 允许动态表单槽位模板路径 | 目标要求正式传统报表及有效 `batchRecordReportId`；`formBindings` 不得替代正式来源 | ProcessInspection/Loss writer 中动态 binding 分支 |
| 四份资料节点 | 系统会向通用批次注入四类特殊节点，并存在负责人配置 | 节点当前不由 PQC 审批一次性创建，且按顺序推进，不是审批后四个并行待办 | `ensureReportNodes`、`createInitialFillTask`、`createNextFillAfterSpecialNodeResolved` |
| 四份资料门禁 | 当前四类特殊节点属于可跳过节点；上传/完成请求没有 `expectedVersion` 和完整幂等合同 | 目标四份均必填、不可跳过；命令必须版本校验和幂等 | `SKIPPABLE_SPECIAL_NODE_TYPES`、complete/upload VO |
| 管理者代表 | 当前最终审批人来自工艺路线 `RELEASE_APPROVE` 分配规则 | 目标必须由 `MES_MANAGEMENT_REPRESENTATIVE` 角色解析候选人 | `createReleaseApprovalTaskAfterSubmit` 和组长 generation service 的负责人解析 |
| 放行前核验 | 当前放行检查项已包含四类资料 | 可复用检查框架，但目标必须强制四份必填、复核申请快照并与最终审批原子提交 | `MesProEdhrReleaseServiceImpl.buildCheckItems`、`approve` |
| 放行命令 | 当前 approve 有幂等键，但无最终工作任务 ID 和 `expectedVersion` | 不能满足任务归属、并发和统一接口合同 | 当前 release approve 请求 VO 和实现 |
| 可追溯列表 | 一个前端入口只固定 `completedTraceOnly=true`，未同时固定 `releaseStatus=RELEASED`；通用列表可选状态 | 目标入口必须同时固定两个条件，后端也必须强制验证 | `FormTraceReleaseTab.vue`、`ReleasePage.vue` |
| ID 精度 | 新链路现有 Java/TS DTO 仍有 `Long`/`number` | 必须按字符串传输，Java Long 字段逐字段序列化 | 申请响应 VO、`teamLeader.ts` |
| 错误合同 | 通用错误响应不能稳定携带阶段、状态和阻塞项 | 需要 MES 范围的结构化业务异常与异常处理，不能丢失阻塞数据 | 统一接口合同与现有 `CommonResult` 行为对照 |

## 5. 目标边界与状态模型

### 5.1 唯一公开总体状态

放行申请的 `applicationStatus` 是跨 SP-1～SP-4 唯一公开总体状态，持久状态只允许：

| 状态 | 含义 | 可接受的下一步 |
| --- | --- | --- |
| `PQC_RELEASE_PENDING` | PQC 待审批 | PQC 通过或拒绝 |
| `PQC_RELEASE_REJECTED` | PQC 已拒绝，首版终态 | 无；重新申请不在首版 |
| `REPORT_UPLOAD_PENDING` | 批次执行已创建，四份资料待完成 | 四份完成后进入管理者代表待办 |
| `MANAGER_RELEASE_PENDING` | 管理者代表待放行 | 管理者代表通过 |
| `RELEASED` | 已完成最终放行 | 进入可追溯列表 |

`PQC_RELEASE_APPROVED` 和 `REPORT_UPLOAD_COMPLETED` 只作为审计事件，不作为可停留的持久状态。

### 5.2 事务边界

1. **SP-1 事务**：创建申请、解析 PQC 候选人、创建 PQC 待办、写审计事件。任一步失败全部回滚。
2. **SP-2 事务**：锁定申请、校验 PQC 任务/角色/版本/幂等键、创建唯一批次执行、完成三类正式映射、创建精确四个资料待办、更新申请为 `REPORT_UPLOAD_PENDING`、完成 PQC 任务、写审计。任一步失败全部回滚。
3. **SP-3 单份完成事务**：校验资料任务归属和版本，保存附件与业务字段，完成任务并推进申请。前三份完成不得创建最终待办。
4. **SP-3 第四份完成事务**：除完成资料任务外，还必须创建放行事务和管理者代表待办，申请变为 `MANAGER_RELEASE_PENDING`；任一步失败全部回滚。
5. **SP-4 事务**：锁定放行事务和申请，重算四份资料快照并比较，完成管理者代表任务，写入签核审计，更新放行事务和申请为 `RELEASED`。任一步失败全部回滚。

所有内部交接使用同一 Spring Boot 单体和数据库事务完成；消息、事件总线或轮询不能作为首版业务触发条件。

## 6. 角色、权限与候选人约束

### 6.1 PQC

- 角色码：`MES_PQC_RELEASE_OWNER`
- 首版目标环境成员：`zhulijiang`
- 业务授权依据：当前租户内启用用户是否属于该角色，而不是用户名是否等于 `zhulijiang`。
- 最小权限固定为：`mes:pro-edhr-work-task:query`、`mes:pro-production-release:query`、`mes:pro-production-release:pqc-approve`、`mes:pro-production-release:pqc-reject`。

### 6.2 管理者代表

- 角色码：`MES_MANAGEMENT_REPRESENTATIVE`
- 首版目标环境成员：`xujianhai`
- 业务授权依据：当前租户内启用用户是否属于该角色，而不是用户名是否等于 `xujianhai`。
- 最小权限固定为：`mes:pro-edhr-work-task:query`、`mes:pro-edhr-release:query`、`mes:pro-edhr-release:approve`。
- 不授予宽泛的批记录审批权限；PQC 也不继承管理者代表权限。

### 6.3 负责人缺失

角色不存在、角色无启用成员、成员不属于当前租户或权限未绑定时，相关创建事务必须失败并回滚。禁止把待办指给管理员、组长、固定用户或任意候选人作为降级。

## 7. 接口与上下游衔接分析

统一接口合同已经足以支持分阶段并行开发，但实现必须共用同一状态字典、任务类型、错误类型、ID 字符串合同和迁移脚本。

| 阶段 | 对外接口/内部端口 | 上游输入 | 必须产生的下游输出 |
| --- | --- | --- | --- |
| SP-0 | 无 HTTP；按租户+角色码解析候选人 | tenantId、roleCode | 严格唯一或明确候选人集合；缺失即结构化失败 |
| SP-1 | 复用组长完工 POST；新增申请回执 GET | POST：activeOrderId、idempotencyKey、applyRemark；GET：activeOrderId | applicationId、PQC taskId、`PQC_RELEASE_PENDING`；不得返回伪造的批次/放行 ID |
| SP-2 | `/production-release/pqc/approve`、`reject`、`get` | applicationId、PQC taskId、expectedVersion、idempotencyKey、决定数据 | batchExecutionId、三类映射结果、精确四个资料任务、`REPORT_UPLOAD_PENDING`，或拒绝终态 |
| SP-3 | 扩展候选任务查询、上传准备、资料完成 | taskId、expectedVersion、idempotencyKey、附件/灭菌批号 | 单份完成状态；第四份完成后 releaseTransactionId、manager taskId、`MANAGER_RELEASE_PENDING` |
| SP-4 | 扩展 `/edhr-release/approve` 与详情查询；固定可追溯查询 | releaseTransactionId、manager taskId、expectedVersion、idempotencyKey、签核证据 | 放行事务和申请均为 `RELEASED`；可追溯查询只返回已放行数据 |

Java 中所有 Long 类型业务 ID 必须逐字段使用 `ToStringSerializer`；JSON 和 TypeScript 一律使用字符串。所有命令型接口必须接收 1～128 位 ASCII `idempotencyKey`；SP-1 apply 按冻结合同不接收 `expectedVersion`，SP-2～SP-4 命令还必须接收并校验正确聚合的 `expectedVersion`。

## 8. 数据、迁移与唯一性约束

### 8.1 必需迁移

- SP-1 统一拥有申请表和工作任务表共享迁移，其他阶段不得重复创建同一 DDL。
- 申请表补齐 PQC 工作任务、决定人/时间/原因、报告快照哈希、乐观锁版本以及需要的唯一约束。
- 工作任务表允许 `batch_execution_id` 仅在 PQC 任务场景为空；其他任务仍必须关联批次执行。
- PQC 工作任务使用 `business_scope_type=RELEASE_APPLICATION`、`business_scope_id=applicationId`；生成 scope ID 并建立防重唯一索引。
- 批次执行使用 `active_context_key=PQC_RELEASE:{applicationId}` 形成申请级唯一关联。
- 菜单、角色、权限和用户绑定必须按业务码/权限码解析，禁止依赖固定菜单主键或硬编码用户名。

### 8.2 旧数据硬阻塞

在部署新链路前必须执行旧数据预检：

- 发现旧申请状态 `BLOCKED` 或 `PENDING_RELEASE_APPROVAL`；或
- 发现已提前创建的批次执行，但无法用正式、唯一、可审计的 PQC 审批记录证明它属于同一放行申请；

则必须按对象类型返回 `LEGACY_RELEASE_APPLICATION_MIGRATION_REQUIRED` 或 `LEGACY_BATCH_EXECUTION_MIGRATION_REQUIRED` 并阻塞新流程。禁止自动猜测、按工单/批号近似匹配、直接复用、删除旧批次或把旧最终审批任务当作新 PQC 审批结果。

## 9. 正式表单来源约束

三条链路必须独立验证，不能互相补齐：

1. **生产批记录**：只允许使用每个工序在工序设置中的正式批记录表单绑定。
2. **过程检验表单**：只允许使用正式过程检验传统报表绑定，且必须存在有效 `batchRecordReportId`。
3. **损耗单**：只允许使用正式损耗传统报表绑定，且必须存在有效 `batchRecordReportId`；零损耗也必须生成正式零值单据。

`formBindings`、动态表单槽位、默认 `MAIN` 槽位、工序开始上传人、旧字段猜测或模板 25/28 动态路径都不得作为这三类正式映射的 fallback。缺少任一正式来源时，PQC 通过事务必须失败且不产生部分批次执行或待办。

## 10. 明确非目标

- PQC 拒绝后的重新申请、撤回、改派、加签。
- 管理者代表拒绝。
- 已完成资料的撤回、删除、重传、覆盖、版本化。
- 将四份资料扩展为可配置数量，或新增第四类负责人。
- 用消息队列、定时轮询或跨服务最终一致性取代首版同步事务。
- 迁移工具自动处置旧申请和旧批次执行。
- 把目标链路的限制无差别施加到系统内所有非本业务批次和通用放行流程。

## 11. 风险与硬阻塞

| 优先级 | 风险/阻塞 | 影响 | 处理要求 |
| --- | --- | --- | --- |
| P0 | 旧申请或旧批次执行不能证明与同一 PQC 审批正式关联 | 可能把未经过 PQC 的旧数据误放行 | 上线前预检；按对象命中 `LEGACY_RELEASE_APPLICATION_MIGRATION_REQUIRED` 或 `LEGACY_BATCH_EXECUTION_MIGRATION_REQUIRED`，人工处置后才能继续 |
| P0 | 目标租户角色、用户、权限或角色成员关系缺失 | 无法创建合法待办 | MIG-RF-0 必须先完成并以角色解析验证；不得 fallback |
| P0 | 正式过程检验/损耗报表绑定缺失，只有动态表单槽位 | 无法形成合规批记录 | PQC 通过失败并回滚；先补齐正式配置 |
| P0 | 第四份资料完成和管理者待办创建不在同一事务 | 可能出现资料齐全却无人审批 | 必须由内部端口同步原子执行 |
| P1 | 当前 `openOrCreate` 选择活动路线版本而非冻结版本 | 配置变更后映射内容漂移 | 新建/扩展正式入口，强制使用申请冻结 routeVersionId |
| P1 | 特殊报告节点全局注入、顺序推进且可跳过 | 目标任务数量、并发性和必填性错误 | 目标链路按精确四节点初始化并禁止 skip；不得依赖当前顺序推进 |
| P1 | 通用 release reject/withdraw/delete 接口可触达目标对象 | 产生首版未定义状态 | 对目标申请来源返回 `UNSUPPORTED_RELEASE_ACTION`，保持其他业务兼容边界明确 |
| P1 | ID 仍按数字传输 | 大整数在浏览器丢精度 | 所有目标 DTO 改为字符串合同并做静态/接口测试 |
| P1 | 通用错误响应丢失阻塞数据 | 前端无法给出可纠正原因 | 使用 MES 专用业务异常和异常处理，返回 stage/status/blockers |
| P2 | 可追溯只靠前端筛选 | 其他调用者可看到未放行数据 | 后端查询固定校验 `completedTraceOnly=true` 与 `releaseStatus=RELEASED` |

## 12. 尚需环境确认的前置条件

以下不是允许实现自行猜测的产品设计问题，而是开发验证前必须取得的环境证据：

- 目标测试租户的 tenantId，以及 `zhulijiang`、`xujianhai` 是否存在、启用并归属该租户。
- 三类现有负责人配置是否都能解析到启用用户，成品检负责人是否可同时接收两份任务。
- 真实测试批次是否具备冻结工艺路线版本、逐工序正式批记录、正式过程检验报表、正式损耗报表和有效报表 ID。
- 当前数据库是否已执行旧申请表迁移，以及旧申请/旧批次执行预检结果。
- 文件上传存储、签核证据和登录凭据是否可用于真实 Playwright E2E。
- 工作树对应前后端运行端口、数据库和 Redis 是否满足项目运行规则。

任一必需前置缺失时，相关测试或上线判定必须失败并报告确切缺口，不得以 mock、直接 SQL 补写、API-only 路径或默认成功代替。

## 13. 验证面

### 13.1 后端测试

- 状态机：所有合法转换、非法跳转、PQC 拒绝终态、目标对象不支持的动作。
- 权限：角色正向、非角色反向、跨租户反向、硬编码用户名扫描。
- 事务：SP-1、SP-2、第四份完成、SP-4 每个中途失败点全部回滚。
- 幂等与并发：同键重放、异键重复、陈旧版本、双击、两个审批人竞争、第四份并发完成。
- 数据唯一性：申请唯一 PQC 任务、申请唯一批次执行、任务 scope 唯一、只创建精确四个资料任务和一个管理者任务。
- 正式来源：三条表单链路分别正向；动态槽位不能证明正式来源；零损耗正式单据。
- 报告快照：四份附件完整性、哈希冻结、最终放行前重算一致性。
- 结构化错误和 ID 精度合同。
- 可追溯后端固定过滤。

### 13.2 前端测试

- 组长双 100% 门禁、点击幂等、回执只展示申请/PQC 状态。
- PQC 候选任务、详情、通过和拒绝页面/交互。
- 三类负责人只看到自己的任务；成品检负责人看到两份；四份均不可跳过。
- 管理者代表仅在四份完成后看到待办；最终通过后状态刷新。
- 成功提交与刷新失败分层展示，不能把已成功提交误报为失败。
- 所有 ID 为字符串，状态和阻塞项文案与统一合同一致。

### 13.3 真实 E2E

必须通过前端按不同真实账号完成：生产组长完工 → zhulijiang 的 PQC 通过 → 三类负责人上传四份资料 → xujianhai 最终放行 → 可追溯列表出现。另需覆盖 PQC 拒绝、缺角色、缺正式来源、未齐四份、无权限、并发冲突和旧数据迁移阻塞的反向路径。

## 14. 并行开发与合并顺序

可以并行开发，但不能无序合并：

1. 先冻结共享状态、错误类型、任务类型、DTO 和 MIG-RF-0/MIG-RF-1 所有权。
2. SP-0 候选人解析与 SP-1 申请/PQC 待办先落地，形成 SP-2 的稳定输入。
3. SP-2 的批次创建和三类正式 writer 可内部并行，但必须由同一审批事务集成。
4. SP-3 可在统一任务 DTO 和内部端口合同冻结后并行开发；第四份完成必须调用正式的管理者阶段初始化端口。
5. SP-4 可基于冻结接口并行开发，但在 SP-3 第四份事务通过集成测试前不能宣称可联调完成。
6. 最后统一执行状态机、事务、权限、迁移和真实 E2E 验证。

## 15. 实施就绪判断

需求和接口边界已经明确，具备分阶段开发条件；当前代码尚未达到业务目标。实施能否成功取决于三个不可绕过的门禁：

1. 目标租户的角色、权限、成员和三类资料负责人配置真实可用。
2. 测试批次存在三类独立正式表单来源，不能只存在动态表单槽位。
3. 旧申请和旧批次执行预检通过，或所有命中记录已被正式迁移处置。

在这三个门禁通过前，可以完成代码和自动化测试，但不能给出业务全链路上线通过结论。

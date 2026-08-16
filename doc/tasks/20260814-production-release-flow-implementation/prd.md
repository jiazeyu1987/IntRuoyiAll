# 生产放行闭环实施 PRD

## 1. Goal

把生产组长完工、PQC 生产放行、批次执行创建、四份资料上传、管理者代表放行和可追溯展示实现为一条按角色授权、不可跳步、事务一致、可审计的正式业务链路。

成功标准是：生产和检测进度均为 100% 的批次，由生产组长发起后，只有经过 PQC 审批、正式批记录映射、四份资料齐全和管理者代表最终放行，才会进入可追溯列表；任何缺权限、缺负责人、缺正式来源、版本冲突、旧数据关系不明或中途写入失败都必须明确失败且不产生部分成功。

## 2. Purpose and Scope

### 2.1 Purpose

当前系统在生产组长发起时已经提前创建批次执行、映射资料并进入最终放行审批，无法表达独立 PQC 审批、四份报告门禁和管理者代表角色放行。本需求重建放行主时序，并在不混淆三类表单来源的前提下复用现有批次执行、工作任务、附件和放行基础能力。

### 2.2 Scope

本版本包括 SP-0～SP-4：

- SP-0：创建并配置 PQC 负责人、管理者代表两个角色及其最小权限；提供按租户和角色码解析候选人的内部能力。
- SP-1：生产组长双 100% 完工门禁；创建生产放行申请和 PQC 待办。
- SP-2：PQC 查询、通过、拒绝；通过后创建申请唯一批次执行，完成三类正式数据映射并创建四份资料待办。
- SP-3：三类负责人上传并完成四份资料；第四份完成时创建放行事务和管理者代表待办。
- SP-4：管理者代表最终放行；只有 `RELEASED` 进入可追溯列表。
- 全阶段：状态、版本、幂等、结构化错误、审计、租户隔离、ID 字符串和旧数据迁移阻塞。

## 3. Evidence Reviewed

### 3.1 Product evidence

- `doc/tasks/20260814-active-order-release-flow-docs/` 中已评审的 PRD、开发文档、子项目拆分和统一接口合同。
- `doc/tasks/20260814-sp0-release-roles-baseline/` 至 `doc/tasks/20260814-sp4-manager-release-traceability/` 中五个阶段的冻结任务文档。
- 项目 `AGENTS.md` 中“工序开始、批记录表单、表单槽位”三条独立链路契约和严格禁止 fallback 要求。

### 3.2 Current-system evidence

- 当前组长生成服务会在申请阶段调用批次 `openOrCreate`、三个映射 writer 和 release `submitForApproval`，证明主时序与目标不符。
- 当前申请表和模型只有 `BLOCKED/PENDING_RELEASE_APPROVAL`，没有 PQC 任务、PQC 决策、报告快照和聚合版本。
- 当前仓库不存在目标角色码、PQC 任务类型及目标权限码。
- 当前批次执行按工单/路线/批号防重并选择活动路线版本，不能证明和同一 PQC 申请唯一关联。
- 当前过程检验和损耗 writer 允许动态表单槽位路径，不符合正式来源要求。
- 当前四类特殊节点为通用注入、顺序推进且可跳过，不符合 PQC 后精确四个并行必填待办。
- 当前最终审批人来自工艺路线 `RELEASE_APPROVE` 分配规则，不是管理者代表角色。
- 当前放行检查框架已识别四类资料，可作为最终快照复核的基础。
- 当前可追溯入口没有在所有层同时固定 `completedTraceOnly=true` 和 `releaseStatus=RELEASED`。

## 4. Product Summary

本产品能力以“生产放行申请”为跨阶段主聚合，以申请状态作为唯一公开总体状态。PQC 审批是批次执行创建的唯一业务入口；四份资料全部完成是管理者代表待办创建的唯一业务入口；管理者代表通过是进入可追溯列表的唯一业务入口。

该链路从业务上是五个可独立开发和验证的子项目，但在数据上共享同一个 applicationId、同一个状态机、同一组版本/幂等约束和明确的内部事务端口。

## 5. Target Users

| 用户/系统主体 | 职责 | 授权来源 |
| --- | --- | --- |
| 生产组长 | 在生产和检测均完成后发起放行申请 | 现有生产组长业务权限和活动工单归属 |
| PQC 负责人 | 审核生产数据，执行 PQC 通过或拒绝 | 角色 `MES_PQC_RELEASE_OWNER`；首版目标环境成员为 `zhulijiang` |
| 来料检负责人 | 上传并完成来料检报告 | 现有工艺路线对应负责人配置 |
| 灭菌负责人 | 上传并完成灭菌报告及必要灭菌批号 | 现有工艺路线对应负责人配置 |
| 成品检负责人 | 上传并分别完成成品检报告、成品检记录 | 现有工艺路线对应负责人配置 |
| 管理者代表 | 在四份资料齐全后执行最终批记录放行 | 角色 `MES_MANAGEMENT_REPRESENTATIVE`；首版目标环境成员为 `xujianhai` |
| 后端系统 | 同步创建任务、映射正式数据、执行门禁、维护状态、审计和事务 | 同一应用与数据库内的受控服务调用 |

角色成员用户名是目标环境初始化和验收数据，不是代码授权条件。生产代码必须按当前租户、角色码、启用状态和权限解析用户。

## 6. First Version Scope

### 6.1 Included

- 双进度 100% 的生产组长完工门禁。
- 申请级防重、命令幂等和乐观锁。
- PQC 角色待办、通过和终态拒绝。
- PQC 通过后申请唯一批次执行和冻结工艺路线版本。
- 正式生产批记录、正式过程检验表单、正式损耗单映射。
- 三类负责人承担四份不可跳过资料任务。
- 第四份完成与最终放行任务创建的原子交接。
- 管理者代表角色放行和报告快照复核。
- 只展示 `RELEASED` 的可追溯列表。
- 结构化阻塞、全链路审计、跨租户隔离和大整数 ID 安全。
- 旧流程数据预检和显式迁移阻塞。

### 6.2 Non-Goals

- PQC 拒绝后的重新申请、恢复、撤回或改派。
- 管理者代表拒绝、退回或会签。
- 已完成资料的删除、撤回、覆盖、重新上传或版本管理。
- 将报告数量做成可配置；首版固定为四份。
- 新增“成品检记录负责人”；成品检负责人承担成品检报告和成品检记录。
- 用消息队列、定时任务、轮询或最终一致性代替同步事务。
- 自动猜测、复用或删除旧批次执行以完成旧数据迁移。
- 用动态表单槽位替代正式生产批记录、过程检验表单或损耗单。
- 将本业务首版限制无差别应用到无关的通用批次/放行流程。

## 7. User or System Scenarios

### SC-01 生产组长成功发起

Given 生产组长对目标活动工单有操作权，且生产进度和检测进度均为 100%，PQC 角色存在有效候选人<br>
When 生产组长点击完工并提交 activeOrderId、有效幂等键和申请备注<br>
Then 系统只创建一个生产放行申请和一个 PQC 待办，申请状态为 `PQC_RELEASE_PENDING`，不创建任何下游业务对象。

### SC-02 进度未完成

Given 生产进度或检测进度任一未达到 100%<br>
When 生产组长尝试完工<br>
Then 前端阻止提交；即使绕过前端，后端也返回进度阻塞且不写入申请或任务。

### SC-03 PQC 通过

Given 申请为 `PQC_RELEASE_PENDING`，当前用户是该 PQC 待办候选人，正式表单来源和三类负责人均完整<br>
When PQC 提交通过<br>
Then 系统在一个事务中完成申请唯一批次执行、三类正式映射、精确四个资料待办、PQC 任务完成和申请状态推进。

### SC-04 PQC 拒绝

Given 申请为 `PQC_RELEASE_PENDING` 且当前用户有权处理<br>
When PQC 提交非空拒绝原因<br>
Then 申请进入 `PQC_RELEASE_REJECTED` 终态，不创建批次执行、资料任务、放行事务或管理者任务。

### SC-05 三类负责人完成四份资料

Given 申请为 `REPORT_UPLOAD_PENDING` 且四份资料任务已按负责人创建<br>
When 来料检、灭菌、成品检负责人分别通过自己的前端待办上传并完成资料<br>
Then 每份资料分别完成；前三份不产生管理者任务，第四份完成时原子创建唯一放行事务和管理者代表待办。

### SC-06 管理者代表最终放行

Given 申请为 `MANAGER_RELEASE_PENDING`，当前用户属于管理者代表角色，四份资料仍完整且快照未变化<br>
When 管理者代表提交最终放行<br>
Then 放行事务、工作任务和申请原子更新为已放行，审计完整，该批次出现在可追溯列表。

### SC-07 旧流程记录阻塞

Given 存在旧申请状态或提前创建的批次执行，且无法证明其和同一 PQC 审批正式唯一关联<br>
When 新流程预检、PQC 查询或 PQC 审批尝试处理该记录<br>
Then 系统按对象返回 `LEGACY_RELEASE_APPLICATION_MIGRATION_REQUIRED` 或 `LEGACY_BATCH_EXECUTION_MIGRATION_REQUIRED`，不得自动匹配、复用、删除或继续放行。

### SC-08 并发和重复提交

Given 两个请求针对同一申请、任务或放行事务并发提交<br>
When 请求幂等键相同、SP-1 同一权威业务身份换用不同请求键，或 `expectedVersion` 已过期<br>
Then 同键同载荷返回首次结果；SP-1 按后端权威 `businessIdempotencyKey` 返回同一申请；同键不同载荷、SP-2 至 SP-4 异键重复动作或旧版本返回明确冲突，且只产生一套业务对象。

## 8. Functional Requirements

### FR-01 统一申请聚合

系统必须以 `releaseApplicationId` 贯穿 PQC 任务、批次执行、四份资料任务、放行事务、管理者任务和审计事件。`applicationStatus` 是跨阶段唯一公开总体状态，不允许各阶段自行解释另一套总体状态。

### FR-02 生产组长完工门禁

前端和后端都必须校验生产进度、检测进度均为 100%，并校验当前用户的活动工单归属和操作权限。任何校验失败不得产生写入。

### FR-03 SP-1 原子创建

完工成功必须在同一事务中完成：

- 创建或幂等返回唯一放行申请；
- 按当前租户和角色码 `MES_PQC_RELEASE_OWNER` 解析有效候选人；
- 创建唯一 `PQC_PRODUCTION_RELEASE` 工作任务，业务范围为 `RELEASE_APPLICATION/applicationId`；
- 记录 `PQC_RELEASE_REQUESTED` 审计事件；
- 申请进入 `PQC_RELEASE_PENDING`。

不得在 SP-1 创建批次执行、三类映射、四份资料任务、放行事务或管理者代表任务。

### FR-04 PQC 查询与授权

PQC 用户只能查询和处理自己在当前租户内有候选资格的 PQC 任务。角色权限固定为 `mes:pro-edhr-work-task:query`、`mes:pro-production-release:query`、`mes:pro-production-release:pqc-approve`、`mes:pro-production-release:pqc-reject`。授权依据是角色和任务候选关系，不得硬编码 `zhulijiang` 或把管理员身份作为替代。

### FR-05 PQC 拒绝

PQC 拒绝必须要求非空原因，原子完成工作任务、决定字段、审计和申请状态 `PQC_RELEASE_REJECTED`。该状态首版不可重开，任何继续操作返回 `UNSUPPORTED_RELEASE_ACTION` 或当前阶段对应的不可处理错误。

### FR-06 PQC 通过前预检

通过前必须在不产生业务写入的前提下验证：

- 申请、PQC 任务、候选人、状态、版本和幂等键有效；
- 冻结工艺路线版本存在且与申请一致；
- 每道工序的正式生产批记录绑定完整；
- 正式过程检验传统报表和有效 `batchRecordReportId` 完整；
- 正式损耗传统报表和有效 `batchRecordReportId` 完整；
- 三类资料负责人均可解析；
- 不存在未处置的旧流程关联歧义。

任一失败必须返回结构化阻塞并保持数据库无部分写入。

### FR-07 PQC 通过原子执行

PQC 通过必须在同一事务中：

1. 锁定并校验申请和 PQC 工作任务；
2. 用 `active_context_key=PQC_RELEASE:{applicationId}` 创建申请唯一批次执行；
3. 使用申请冻结的工艺路线版本，不得重新选择当前活动版本；
4. 写入正式生产批记录、正式过程检验表单和正式损耗单；
5. 创建精确四个 `FILL` 资料任务；
6. 完成 PQC 任务并写入决定人、时间和审计；
7. 申请进入 `REPORT_UPLOAD_PENDING`。

任何一步失败必须回滚全部结果。

### FR-08 三类正式映射

- 批记录必须逐工序读取工序设置中的正式批记录表单绑定。
- 过程检验必须读取正式过程检验传统报表绑定。
- 损耗单必须读取正式损耗传统报表绑定，零损耗也要生成带正式来源的零值单据。
- `formBindings`、表单槽位、工序开始负责人、默认 `MAIN`、模板 25/28 或旧字段推断不得补齐任何正式来源。

### FR-09 精确四份资料任务

PQC 通过后只创建且必须创建以下四个业务节点：

| 节点类型 | 资料 | 负责人来源 |
| --- | --- | --- |
| `INCOMING_INSPECTION_REPORT` | 来料检报告 | 来料检负责人配置 |
| `STERILIZATION_REPORT` | 灭菌报告 | 灭菌负责人配置 |
| `FINISHED_PRODUCT_INSPECTION_REPORT` | 成品检报告 | 成品检负责人配置 |
| `FINISHED_PRODUCT_INSPECTION_RECORD` | 成品检记录 | 成品检负责人配置 |

四个任务必须都关联同一 applicationId 和 batchExecutionId，均不可跳过。成品检负责人应看到两个独立任务，而不是一份合并资料。

### FR-10 资料候选任务查询

候选任务查询必须支持 `nodeTypes` 和 `batchExecutionId` 过滤，并返回 `nodeType`、`nodeName`、字符串 ID 和当前 `version`。用户只能看到自己可处理的任务，跨租户、跨负责人查询不得返回数据。

### FR-11 上传准备与完成

- 上传准备必须校验 taskId、expectedVersion、idempotencyKey 和候选人资格。
- 完成命令必须保存附件元数据、文件哈希及节点专有字段；灭菌报告必须保存并校验灭菌批号。
- 资料完成后不可通过目标业务入口执行跳过、删除待上传、撤回或覆盖；这些动作返回 `UNSUPPORTED_RELEASE_ACTION`。
- 附件缺失、哈希不一致、节点类型不匹配或任务已锁定必须明确失败。

### FR-12 四份资料门禁

完成任一资料后，系统必须在数据库锁和唯一约束保护下按四个目标 nodeType 计算完成数。前三份完成时申请保持 `REPORT_UPLOAD_PENDING`，不得创建放行事务或管理者待办。

### FR-13 第四份完成原子交接

第四份资料完成时，系统必须在同一事务中：

- 完成该资料任务；
- 校验四份资料都存在有效附件和哈希；
- 生成并保存报告快照哈希；
- 创建唯一放行事务；
- 按 `MES_MANAGEMENT_REPRESENTATIVE` 解析候选人；
- 创建唯一管理者代表待办；
- 写 `REPORT_UPLOAD_COMPLETED` 审计事件；
- 申请进入 `MANAGER_RELEASE_PENDING`。

负责人缺失、放行事务创建失败或管理者任务创建失败时，第四份资料完成也必须回滚。

### FR-14 管理者代表查询与授权

管理者代表只能查询和处理自己在当前租户内有候选资格的最终待办。角色权限固定为 `mes:pro-edhr-work-task:query`、`mes:pro-edhr-release:query`、`mes:pro-edhr-release:approve`。授权依据是角色 `MES_MANAGEMENT_REPRESENTATIVE` 和任务候选关系，不得硬编码 `xujianhai`，也不得要求或授予宽泛的 `mes:pro-batch-record-execution:approve`。

### FR-15 最终放行复核

最终通过前必须锁定申请和放行事务，验证任务归属、`expectedVersion`、幂等键、`MANAGER_RELEASE_PENDING` 状态，并重新计算四份资料快照。快照与冻结值不一致、任一附件缺失或状态不符时必须阻塞。

### FR-16 最终放行原子执行

管理者代表通过必须在同一事务中：

- 完成最终工作任务；
- 写入签核证据、意见、操作者和时间；
- 放行事务进入 `RELEASED`；
- 申请进入 `RELEASED`；
- 写最终放行审计事件。

首版不支持管理者代表拒绝、退回或撤回；对目标申请来源的这些动作返回 `UNSUPPORTED_RELEASE_ACTION`。

### FR-17 可追溯列表

目标可追溯入口前后端都必须固定使用 `completedTraceOnly=true` 且 `releaseStatus=RELEASED`。后端不得仅信任前端参数。未放行、PQC 拒绝、资料待上传或管理者待办中的记录均不得出现在可追溯列表。

### FR-18 幂等与乐观锁

- 所有命令必须接收 1～128 位 ASCII `idempotencyKey`。
- SP-1 apply 的冻结请求仅包含 activeOrderId、idempotencyKey、applyRemark，不接收 `expectedVersion`，由活动工单/申请业务唯一约束和幂等记录防重。
- SP-2～SP-3 使用申请聚合版本；SP-4 使用放行事务版本，并同时校验申请状态。
- 同键同载荷重放返回第一次成功结果，不重复写入。
- 同键不同载荷、异键重复业务动作、陈旧版本或并发状态变化返回明确冲突。
- 数据库唯一约束是最后防线，不能只依赖应用层先查后写。

### FR-19 结构化失败

业务失败仍使用统一 `CommonResult` 非零 code，但 data 必须稳定提供当前 `stage`、`status` 和 `blockers`。至少覆盖：进度不足、组长无权、角色/候选人缺失、任务不可处理、版本/幂等冲突、正式来源缺失、旧数据迁移阻塞、负责人/附件/节点/锁定/快照错误、最终放行不可处理和不支持动作。

禁止吞异常、只返回通用“操作失败”、伪造成功 ID 或在失败后继续下游动作。

### FR-20 ID 精度

所有目标链路中的业务 ID 在 JSON 和 TypeScript 中必须是字符串。Java Long 字段必须逐字段配置 `ToStringSerializer`，不能依赖全局碰巧生效的转换。

### FR-21 审计

每个业务转换必须记录租户、applicationId、关联 taskId/batchExecutionId/releaseTransactionId、前后状态、操作者、角色、时间、幂等键摘要和决定/阻塞结果。审计事件不能代替事务状态，也不能在事务回滚后留下成功记录。

### FR-22 旧流程迁移门禁

上线和处理旧记录前必须预检旧状态 `BLOCKED/PENDING_RELEASE_APPROVAL` 及提前创建批次执行的记录。不能用正式 PQC 审批记录证明同一申请唯一关联时，申请类问题必须返回 `LEGACY_RELEASE_APPLICATION_MIGRATION_REQUIRED`，批次执行关联问题必须返回 `LEGACY_BATCH_EXECUTION_MIGRATION_REQUIRED` 并阻塞；不得猜测、自动复用、删除或默认视为已通过 PQC。

## 9. Business Rules

| Rule ID | 规则 |
| --- | --- |
| BR-01 | 生产和检测必须都精确达到完成标准，不能只看前端显示值。 |
| BR-02 | SP-1 只创建申请和 PQC 待办。 |
| BR-03 | PQC 通过是创建目标批次执行的唯一业务入口。 |
| BR-04 | 一个申请最多一个 PQC 待办、一个批次执行、一个放行事务和一个管理者代表待办。 |
| BR-05 | PQC 拒绝是首版终态，不创建任何下游对象。 |
| BR-06 | 三类表单来源独立，动态表单槽位不能补齐正式表单。 |
| BR-07 | 资料固定四份、负责人固定三类，成品检负责人承担两份。 |
| BR-08 | 四份资料均不可跳过；前三份完成不能提前创建最终待办。 |
| BR-09 | 第四份完成和管理者代表阶段初始化必须原子提交。 |
| BR-10 | 管理者代表通过是进入可追溯列表的唯一入口。 |
| BR-11 | 角色成员按租户和角色码解析；用户名只用于目标环境验收。 |
| BR-12 | 旧数据关系无法正式证明时必须阻塞，不允许近似匹配。 |
| BR-13 | 任何结构化前置校验失败都必须在写入前失败；事务中失败则全部回滚。 |

## 10. States and Transitions

| 当前持久状态 | 触发者/动作 | 审计事件 | 下一持久状态 | 原子输出 |
| --- | --- | --- | --- | --- |
| 无申请 | 生产组长完工 | `PQC_RELEASE_REQUESTED` | `PQC_RELEASE_PENDING` | 申请 + PQC 待办 |
| `PQC_RELEASE_PENDING` | PQC 拒绝 | `PQC_RELEASE_REJECTED` | `PQC_RELEASE_REJECTED` | PQC 决定与任务完成 |
| `PQC_RELEASE_PENDING` | PQC 通过 | `PQC_RELEASE_APPROVED` | `REPORT_UPLOAD_PENDING` | 唯一批次 + 三类映射 + 四资料任务 |
| `REPORT_UPLOAD_PENDING` | 前三份中的任一份完成 | 对应资料完成事件 | `REPORT_UPLOAD_PENDING` | 当前资料完成 |
| `REPORT_UPLOAD_PENDING` | 第四份完成 | `REPORT_UPLOAD_COMPLETED` | `MANAGER_RELEASE_PENDING` | 快照 + 放行事务 + 管理者待办 |
| `MANAGER_RELEASE_PENDING` | 管理者代表通过 | `MANAGER_RELEASED` | `RELEASED` | 最终任务、事务、申请和签核审计完成 |

`PQC_RELEASE_APPROVED` 和 `REPORT_UPLOAD_COMPLETED` 是同一事务内的瞬时审计事件，不能暴露成长期停留状态。任何未在表中定义的目标链路状态转换均拒绝。

## 11. Interface Contract

接口字段、错误结构和内部端口以 `doc/tasks/20260814-active-order-release-flow-docs/interface-contract.md` 为唯一详细合同。本 PRD 冻结以下上下游语义：

| 阶段 | 接口/端口 | 核心输入 | 核心输出 |
| --- | --- | --- | --- |
| SP-0 | 租户角色候选人解析内部服务 | tenantId、roleCode | 启用候选人；缺失明确失败 |
| SP-1 | 组长完工 POST、申请回执 GET | POST：activeOrderId、idempotencyKey、applyRemark；GET：activeOrderId | applicationId、PQC taskId、`PQC_RELEASE_PENDING` |
| SP-2 | `/mes/pro/production-release/pqc/approve`、`reject`、`get` | applicationId、PQC taskId、expectedVersion、idempotencyKey | 拒绝终态，或 batchExecutionId、四任务、`REPORT_UPLOAD_PENDING` |
| SP-2→SP-3 | `initializeRequiredReportStage(...)` | applicationId、batchExecutionId、四负责人解析结果 | 精确四个 FILL 任务 |
| SP-3 | 候选任务查询、上传准备、资料完成 | taskId、expectedVersion、idempotencyKey、附件/灭菌批号 | 任务完成状态；第四份时返回最终阶段关联 ID |
| SP-3→SP-4 | `initializeManagerReleaseStage(...)` | applicationId、batchExecutionId、报告快照 | releaseTransactionId、manager taskId |
| SP-4 | `/mes/pro/edhr-release/approve`、`get` | releaseTransactionId、manager taskId、expectedVersion、idempotencyKey、签核证据 | `RELEASED` |
| Trace | 放行分页查询 | 固定 completedTraceOnly、releaseStatus | 仅已放行记录 |

组长完工响应不得继续以非空下游 ID 假装批次/最终放行已创建。所有目标接口的 ID 都是字符串。

## 12. Non-Functional Requirements

### NFR-01 一致性

跨阶段初始化必须在同一应用、同一数据库事务内同步完成。不得以消息、轮询或补偿脚本作为首版成功条件。

### NFR-02 并发安全

应用层行锁/乐观锁、幂等记录和数据库唯一约束共同保证在双击、重试、多个候选人竞争和第四份并发完成时只生成一套对象。

### NFR-03 安全与最小权限

所有读取和命令均校验租户、角色、权限、任务候选关系和对象归属。PQC 与管理者代表权限隔离，不授予与任务无关的宽泛权限。

### NFR-04 可审计

每次状态变化、审批、阻塞和文件完成均能按 applicationId 重建完整时间线，且审计和业务数据事务一致。

### NFR-05 可诊断

前端和日志能区分业务阻塞、权限失败、版本冲突、已成功但刷新失败和系统异常。日志不得输出密码、令牌、私钥或文件存储凭据。

### NFR-06 数据精度

浏览器、接口和日志关联中的所有 Long ID 不得因 JavaScript 数字精度发生变化。

### NFR-07 性能

候选任务、申请详情和可追溯分页必须使用可索引的租户、状态、任务类型、业务 scope、batchExecutionId 和 releaseStatus 条件；禁止依赖全表扫描完成常用列表查询。

### NFR-08 可维护性

共享状态、节点类型、阻塞类型和接口 DTO 必须只有一个正式定义来源。SP-0～SP-4 不得各自复制同义常量或重复建表迁移。

### NFR-09 无降级

缺角色、缺来源、缺附件、缺运行环境或旧数据不明时必须 fail fast。禁止 mock、fallback、默认管理员、默认负责人和默认成功。

## 13. Dependencies and Constraints

### 13.1 Product and data dependencies

- 目标测试租户存在启用用户 `zhulijiang`、`xujianhai`，并分别绑定目标角色；代码仍只按角色授权。
- 三类资料负责人配置均能解析到当前租户启用用户。
- 真实测试批次具有冻结 routeVersionId、逐工序正式批记录、正式过程检验报表、正式损耗报表及有效报表 ID。
- 文件存储和签核证据能力真实可用。

### 13.2 Migration dependencies

- MIG-RF-0：角色、权限、用户角色绑定按业务码幂等迁移。
- MIG-RF-1：由 SP-1 独占申请表和工作任务表共享 DDL；其他阶段只消费合同。
- `active_context_key=PQC_RELEASE:{applicationId}` 具备租户内唯一约束。
- 上线前旧数据预检无未处置的 `LEGACY_RELEASE_APPLICATION_MIGRATION_REQUIRED` 或 `LEGACY_BATCH_EXECUTION_MIGRATION_REQUIRED`。

### 13.3 Engineering constraints

- Java 17/Spring Boot、Vue 3/TypeScript 和现有模块边界不变。
- 保持“工序开始、批记录表单、表单槽位”三条配置链路独立。
- 不以固定菜单 ID、固定用户 ID 或用户名写权限逻辑。
- 目标流程扩展通用任务/放行能力时，必须用明确业务来源限制新规则的影响范围，不能破坏无关流程。
- BDD + 严格 TDD；真实 E2E 必须通过 Playwright 和真实前端路径完成。

## 14. Edge Cases

| Edge ID | 场景 | 期望行为 |
| --- | --- | --- |
| EDGE-01 | 一个进度为 99.99% 或后端完成度未落库 | 阻止完工，不写入 |
| EDGE-02 | 双击完工或网络重试 | 同键返回同一申请/PQC 任务；不同键不重复创建 |
| EDGE-03 | PQC 角色存在但无启用成员 | SP-1 回滚并返回角色候选人阻塞 |
| EDGE-04 | `zhulijiang` 存在但未绑定角色 | 不因用户名获得权限 |
| EDGE-05 | 两个 PQC 候选人同时审批 | 只有一个决定成功；另一个得到版本/状态冲突 |
| EDGE-06 | PQC 拒绝后再次通过 | 返回不支持或不可处理，不创建下游 |
| EDGE-07 | 申请后工艺路线切换活动版本 | PQC 仍使用申请冻结版本；冻结版本不存在则阻塞 |
| EDGE-08 | 只有动态表单槽位，没有正式过程检验/损耗报表 | PQC 通过失败并全部回滚 |
| EDGE-09 | 实际损耗为零 | 创建正式零值损耗单，不省略、不走动态路径 |
| EDGE-10 | 成品检负责人同时收到两份任务 | 两份独立完成；不能一份附件自动完成两个节点 |
| EDGE-11 | 尝试跳过四类任一节点 | 返回 `UNSUPPORTED_RELEASE_ACTION` 或报告节点不可跳过阻塞 |
| EDGE-12 | 第四份资料完成时管理者角色无候选人 | 第四份完成及阶段推进全部回滚 |
| EDGE-13 | 第三、第四份并发完成 | 锁和唯一约束保证只创建一个放行事务和一个管理者任务 |
| EDGE-14 | 资料完成后附件被篡改或缺失 | 最终放行快照复核失败，不进入 `RELEASED` |
| EDGE-15 | `xujianhai` 未绑定管理者角色 | 不因用户名获得权限 |
| EDGE-16 | 管理者代表通过成功但页面刷新失败 | 明确显示提交成功、刷新失败；不得重报为提交失败 |
| EDGE-17 | 未放行记录手工传入 trace 查询 | 后端仍不返回该记录 |
| EDGE-18 | 旧批次与申请仅能按批号近似匹配 | 返回迁移阻塞，不复用、不删除 |
| EDGE-19 | 18 位以上 ID | JSON 和前端保持原字符串完全一致 |
| EDGE-20 | 同幂等键携带不同载荷 | 返回幂等冲突，不复用旧成功结果 |

## 15. Acceptance Criteria

### AC-01 双进度门禁

给定生产或检测进度任一未达到 100%，从前端点击和直接调用后端两条路径均不能创建申请/任务；响应明确指出未完成项目。两项都为 100% 时才允许进入 AC-02。

### AC-02 组长归属门禁

给定不属于目标活动工单的用户，即使两项进度为 100%，完工也返回无权阻塞，数据库无新增申请和工作任务。

### AC-03 PQC 角色基线

在目标租户完成 MIG-RF-0 后，`MES_PQC_RELEASE_OWNER` 角色存在、权限集合与合同完全一致，`zhulijiang` 是启用成员；移除其角色后立即失去 PQC 候选和审批能力，证明代码未硬编码用户名。

### AC-04 SP-1 只产生两个对象

生产组长首次成功完工后，只新增一个申请和一个 `PQC_PRODUCTION_RELEASE` 待办，状态为 `PQC_RELEASE_PENDING`；批次执行、三类映射、四份资料任务、放行事务和管理者任务增量均为零，响应不返回伪造的下游 ID。

### AC-05 SP-1 原子性和幂等

模拟 PQC 候选人解析失败或 PQC 任务写入失败时，申请与审计也回滚；同幂等键同载荷重放返回相同 applicationId/taskId；同一权威业务身份即使换用不同请求键也返回同一申请；同一请求键对应不同权威快照时返回 `IDEMPOTENCY_PAYLOAD_CONFLICT`。

### AC-06 申请回执

通过 `GET /mes/pro/process-pool/team-leader/active-order/release/get?activeOrderId={id}` 按 activeOrderId 查询，合法组长和 PQC 候选人可取得字符串 applicationId、当前状态、版本和 PQC taskId；无权、跨租户或不存在数据得到结构化失败。

### AC-07 PQC 正向与反向权限

`zhulijiang` 仅在属于当前租户 `MES_PQC_RELEASE_OWNER` 且是任务候选人时可查询和处理；普通组长、管理者代表、其他租户同名用户以及仅有管理员身份的用户均不能处理。

### AC-08 PQC 拒绝终态

PQC 用非空原因拒绝后，申请和任务原子进入拒绝完成状态并记录审计；批次执行、映射、资料任务、放行事务和管理者任务均不存在；再次通过、重新申请或撤回返回不支持/不可处理。

### AC-09 PQC 通过原子输出

满足全部前置后，PQC 通过一次事务内产生：一个申请唯一批次执行、完整三类正式映射、精确四个资料 FILL 任务、完成的 PQC 任务和 `REPORT_UPLOAD_PENDING`；在任一 writer 或任务创建点注入失败时，上述增量全部为零且申请仍可重试。

### AC-10 批次申请唯一关联

同一 applicationId 的并发/重复通过只存在一个 `active_context_key=PQC_RELEASE:{applicationId}` 的批次执行；不同申请不得因工单/批号相同而错误共享。系统不得在 PQC 时重新选择当前活动路线版本。

### AC-11 旧数据迁移阻塞

准备旧 `BLOCKED/PENDING_RELEASE_APPROVAL` 申请或提前创建但无正式 PQC 关联的批次执行，预检和业务接口分别返回 `LEGACY_RELEASE_APPLICATION_MIGRATION_REQUIRED` 或 `LEGACY_BATCH_EXECUTION_MIGRATION_REQUIRED`；数据库没有自动关联、复用、删除或新阶段推进。

### AC-12 三类正式来源独立

分别移除逐工序批记录、正式过程检验报表、正式损耗报表，PQC 通过都返回对应来源阻塞并回滚。仅补充 `formBindings`、默认 `MAIN`、工序开始上传人或模板 25/28 不能使测试通过。

### AC-13 零损耗单

真实损耗为零时仍创建带正式损耗报表 ID、正确批次/工序关联和零值明细的损耗单；不能省略记录或落入动态表单路径。

### AC-14 四份任务和三类负责人

PQC 通过后按 nodeType 查询恰好四个任务：来料检负责人一个、灭菌负责人一个、成品检负责人两个。四个任务共享 applicationId/batchExecutionId，但 taskId 各不相同，均返回字符串 ID 和版本。

### AC-15 四份均不可跳过

对四类任一任务调用 skip、删除待上传、撤回或完成后覆盖接口，目标链路均返回明确不支持且任务/申请状态不变；未上传有效附件不能完成。

### AC-16 资料上传权限、版本和幂等

每类负责人只能操作自己的任务；跨负责人、跨租户、错误 nodeType、旧 expectedVersion 均失败。同键同载荷重放不重复附件/审计，同键异载荷或异键重复完成返回冲突。

### AC-17 前三份门禁

任意顺序完成 1～3 份后，申请仍为 `REPORT_UPLOAD_PENDING`，放行事务和管理者任务数量均为零；候选查询只把已完成任务从待办中移除。

### AC-18 第四份原子交接

第四份完成时同一事务保存有效附件/哈希、冻结四报告快照、创建一个放行事务和一个管理者代表任务并推进为 `MANAGER_RELEASE_PENDING`。模拟角色解析、事务创建或任务创建失败时，第四份任务也保持未完成且无任何部分对象。

### AC-19 第四份并发唯一性

两个请求并发触发四份齐全判断时，仅一个请求成功初始化最终阶段；数据库只存在一个放行事务和一个管理者任务，另一个返回幂等结果或明确版本/状态冲突。

### AC-20 管理者代表角色基线

在目标租户完成 MIG-RF-0 后，`MES_MANAGEMENT_REPRESENTATIVE` 角色存在、权限集合与合同完全一致，`xujianhai` 是启用成员且没有被额外授予宽泛批记录审批权限；移除角色后立即失去最终待办和放行能力。

### AC-21 管理者代表授权

只有当前租户内的管理者代表角色成员且为最终任务候选人能查询和通过；PQC、组长、报告负责人、其他租户同名用户和仅管理员身份用户均不能处理。

### AC-22 首版不支持最终拒绝

对来源为本生产放行申请的管理者任务调用拒绝、退回或撤回，返回 `UNSUPPORTED_RELEASE_ACTION`，申请保持 `MANAGER_RELEASE_PENDING`；无虚假审计或下游状态变化。

### AC-23 最终快照复核

在最终放行前删除、替换或篡改任一报告附件/哈希，approve 返回报告快照阻塞，放行事务、工作任务和申请均不变；四份快照一致时才允许 AC-24。

### AC-24 最终放行原子性和幂等

管理者代表通过后，最终任务完成、放行事务和申请均为 `RELEASED`，签核证据和审计完整；任一写入失败全部回滚。同键重放返回首次结果，旧版本或竞争请求返回明确冲突。

### AC-25 只允许 RELEASED 可追溯

可追溯页面和后端请求同时固定 `completedTraceOnly=true`、`releaseStatus=RELEASED`。五种申请状态各准备一条数据时只返回 `RELEASED`；即使调用者修改/省略状态参数，也不能从目标入口取得未放行记录。

### AC-26 放行后即时可见

AC-24 成功提交后，在同一真实用户流程刷新可追溯页面可看到该批次，且关联的申请、批次执行、四份资料和签核信息可追溯。

### AC-27 提交成功与刷新失败分层

模拟最终提交成功而后续详情刷新失败，页面明确保留“提交已成功”的结果并单独提示刷新失败，不得把成功审批重新显示为提交失败或诱导用户重复审批。

### AC-28 字符串 ID 合同

使用超过 JavaScript 安全整数范围的 applicationId、taskId、batchExecutionId 和 releaseTransactionId 运行接口和前端测试，网络 JSON、TypeScript 状态、路由参数和后续请求值逐字符一致，均无 `number` 类型转换。

### AC-29 结构化错误合同

对进度、权限、角色、正式来源、旧数据、报告附件、版本、幂等、快照和不支持动作各触发一次失败，响应均为非零 code，且 data 含稳定 `stage/status/blockers`；前端能显示可操作原因，日志不存在默认成功或吞异常。

### AC-30 租户隔离

两个租户准备相同用户名、角色码、工单号和批号时，候选解析、任务查询、审批、附件、放行和可追溯均只影响当前租户；跨租户 ID 请求返回无权或不存在且不泄露详情。

### AC-31 审计完整性

成功全流程可按 applicationId 重建发起、PQC 决定、四份资料完成、最终放行时间线；PQC 拒绝和各类阻塞有对应失败/决定证据；事务回滚场景不存在伪成功审计。

### AC-32 真实端到端主链路

使用真实前端、真实测试租户和真实账号完成：生产组长双 100% 完工 → `zhulijiang` 以 PQC 角色通过 → 来料检/灭菌/成品检负责人上传四份真实测试附件 → `xujianhai` 以管理者代表角色通过 → 可追溯列表出现。全过程不使用 mock、直接 SQL 推状态、API-only 替代或默认管理员。

### AC-33 三条配置链路回归

分别为工序开始上传人、逐工序正式批记录和表单槽位 `formBindings` 建立独立数据，验证本需求只从逐工序正式批记录及两个正式传统报表来源映射；修改表单槽位不改变正式批记录名称、链接、配置状态或映射结果。

### AC-34 数据库约束回归

直接构造并发/重复写入时，数据库唯一约束阻止重复 PQC scope、申请批次关联、放行事务和管理者任务；约束异常被转换为稳定业务冲突，不以 500、吞异常或重复成功返回。

## 16. Open Questions

业务时序、角色码、四份资料、接口边界和 V1 不支持动作已经冻结，没有待实现团队自行决定的产品问题。下列是验证前必须确认的环境事实：

- 目标测试 tenantId 及两个首版用户的存在、启用和租户归属。
- 三类资料负责人实际配置及候选用户。
- 可用于 E2E 的真实活动工单、冻结路线版本和三类正式表单绑定。
- 旧申请/旧批次预检清单及人工处置状态。
- 文件存储、签核证据、数据库、Redis、前后端运行入口和测试凭据。

上述任一缺失都不能由开发人员猜测或以 fallback 补齐。

## 17. Product Blockers

以下条件任一成立，不能宣称业务目标完成或允许上线：

1. MIG-RF-0 未完成，或 `zhulijiang`/`xujianhai` 未按角色正确绑定。
2. MIG-RF-1 未完成或唯一约束未实际生效。
3. 存在未处置的 `LEGACY_RELEASE_APPLICATION_MIGRATION_REQUIRED` 或 `LEGACY_BATCH_EXECUTION_MIGRATION_REQUIRED` 记录。
4. 测试批次缺少逐工序正式批记录、正式过程检验报表或正式损耗报表，只有动态表单槽位。
5. 四份资料中的任一份仍可跳过，或第四份与管理者阶段初始化不能原子回滚。
6. 管理者代表放行前未重算报告快照，或可追溯后端不能强制只返回 `RELEASED`。
7. 目标接口仍有 ID 数字精度风险、结构化阻塞丢失、用户名硬编码、mock、默认管理员或其他降级路径。
8. 未通过角色正反向、事务故障注入、并发幂等、迁移阻塞、三配置链路和真实 Playwright E2E 验收。

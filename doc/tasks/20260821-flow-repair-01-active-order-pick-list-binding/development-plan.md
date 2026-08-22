# Development Plan：正式领料单绑定与稳定追溯

## 1. 目标态

目标业务链路如下：

```text
生产组长选择生产工单
  -> 查询同租户正式领料单候选
  -> 显式选择一个领料单
  -> 后端锁定并校验工单、领料单头、领料单明细、审核状态、物料关系
  -> 同事务创建活跃订单 + 领料单头快照 + 明细快照 + 审计
  -> 一线生产/PQC/组长复核只产生事实
  -> 活跃订单完成节点消费绑定快照回填正式批记录、过程检验单和有损耗时的损耗单
  -> 三类回填成功后创建或复用批次执行，并写入批次执行-领料单稳定关系
  -> 批次执行只消费冻结关系和快照，上传四份材料
  -> 四份材料齐套后才允许放行
  -> 追溯按关系表返回活跃订单、工单、领料单、明细、一线生产、一线PQC和损耗来源
```

加入动作不再只形成 `workOrderId`。`workOrderId` 是工单事实，`pickListBindingId` 是领料单绑定事实，两者不能互相推断。领料单绑定成功前不得创建“已绑定”的活跃订单；绑定成功后不得静默替换领料单。

## 2. 当前代码事实

| 位置 | 当前事实 | 影响 |
| --- | --- | --- |
| `MesTeamLeaderActiveOrderAddReqVO` / `MesTeamLeaderActiveOrderAddReqBO` | 请求已包含 `pickListId`、候选快照 hash 和 `idempotencyKey`；Long ID 通过 HTTP 字符串传输 | 加入请求能够表达用户选择和幂等载荷；定向服务测试覆盖成功、冲突和重复路径 |
| `MesTeamLeaderActiveOrderServiceImpl` | 候选按领料单 `productionOrderNo` 与正式工单号精确匹配；提交时重读审核状态、头和全部明细，创建绑定头/明细快照并记录 hash | 已实现工单匹配、审核门禁、稳定分录校验、来源漂移阻断和审计 |
| `MesProcessPoolActiveOrderPickListBindingDO` / `...BindingItemDO` | 正式保存绑定头、全部明细、来源标识、来源版本、`sourceSnapshotHash`、绑定状态、请求 hash 和幂等键 | SQL `20260822_mes_active_order_pick_list_binding.sql` 建立头/明细表及租户内唯一约束 |
| `MesProcessPoolActiveOrderPickListBindingMapper` / item mapper | 绑定头按活跃订单、幂等键读取，明细按绑定 ID 读取并保留全量分录 | 下游不再依赖可变 ERP 查询结果作为历史来源 |
| `TeamLeaderWorkbenchPage.vue` / `teamLeader.ts` | 新增弹窗加载领料单候选，必须选择可绑定项后提交 `pickListId`、候选 hash 和幂等键 | 前端静态合同 `teamLeaderPickListBinding.static.spec.cjs` PASS |
| `MesProductionPickListSourceServiceImpl` | 活跃订单路径要求 `pickListBindingId`，读取绑定头和全量明细快照，校验 hash/状态/正式分录顺序；来源缺失或漂移直接阻断 | 定向 source service 测试 PASS；不按工单临时换单 |
| `MesCompletionBackfillReceipt` / 批次与放行命令 | 命令对象携带 `pickListBindingId`、`pickListId`、`sourceSnapshotHash`、`bindingVersion`、`batchPickListRelationId` | 流程修复6/7可消费稳定字段；批次关系最终落库和最终放行仍由相邻任务负责 |
| 当前验证基线 | `int_main` 已包含流程1实现及测试整合提交 | MES compile、100 个定向 JUnit、前端静态合同、schema 合同、diff-check、runtime guard 均 PASS |
| `MesProEdhrBatchExecutionDO` / `MesTeamLeaderActiveOrderReleaseBatchRecordWriterImpl` | 流程1已将绑定 ID/hash 传入完成、批次和来源解析命令；批次关系和最终材料/放行状态仍由流程6/7/8/10落库 | 流程1不再按工单临时换单，但不宣称相邻流程已完成 |

## 3. 根因

1. **请求模型缺失绑定身份**：加入接口以工单为唯一业务输入，前端没有候选查询和选择确认。
2. **持久化边界错误**：活跃订单、批次执行和领料单之间没有正式关系表或快照，只有运行时反查。
3. **来源解析时机错误**：`resolveValue` 在放行阶段按工单号重新查 ERP，无法证明加入时用户确认的单据，也无法发现来源漂移后应阻断而不是换单。
4. **重复/恢复逻辑未纳入绑定**：当前按工单历史 `REUSE/RECOVER`，同一工单换领料单时可能错误复用旧业务记录。
5. **共享合同未冻结**：批次执行、资料上传和追溯消费者没有统一的 `pickListBindingId`、快照哈希、状态所有者和结构化 blocker。

## 4. 修改边界

### 4.1 本任务未来允许修改的模块

- MES 班组长活跃订单 Controller/VO/Service/Mapper/DO 和对应测试。
- MES 领料单正式来源端口：新增按绑定 ID 解析、按工单列候选、快照哈希计算的只读能力。
- MES 批次执行与追溯的关系端口和 schema（由唯一迁移 owner 实现）。
- 前端班组长活跃订单新增弹窗、API 类型和静态合同测试。

### 4.2 明确禁止的修改

- 不在 `formBindings`、表单槽位、工序开始或默认 `MAIN` 中增加领料单字段。
- 不在一线生产签名、PQC 签名或组长复核节点回填批记录、检验单、损耗单或创建批次执行。
- 不在放行阶段继续使用“按工单号查最新/唯一领料单”作为主来源。
- 不通过直接 SQL 给现有活跃订单填 `pickListId`，不删除或重写历史报工/快照冒充迁移。
- 不在本任务中实现流程修复6、7、9的业务代码。

## 5. 领域数据与状态设计

### 5.1 活跃订单领料单绑定聚合

建议新增表 `mes_pro_process_pool_active_order_pick_list_binding`，由“活跃订单绑定 owner”单独负责，不把状态塞进 `mes_pro_process_pool_active_order`：

| 字段 | 类型/含义 | 约束与来源 |
| --- | --- | --- |
| `id` | Long | JSON 以字符串传输；主键 |
| `tenant_id` | Long | 租户隔离；不可由前端传入 |
| `active_order_id` | Long | 正式活跃订单；唯一当前绑定 |
| `work_order_id` | Long | 加入请求工单；必须与活跃订单一致 |
| `route_id`、`route_version_id` | Long | 加入时冻结的路线身份 |
| `pick_list_id` | Long | ERP 正式领料单头 ID |
| `source_fid`、`source_bill_no` | String | ERP 稳定来源标识和展示编号快照 |
| `source_document_status` | String | 绑定时必须为已审核 `C`；状态 owner 是 ERP 同步 |
| `source_modify_time` | DateTime | 绑定时 ERP 来源版本证据 |
| `source_snapshot_hash` | String | 头 + 明细 + 工单匹配摘要的 SHA-256 |
| `binding_status` | Enum | `BOUND`、`FROZEN`、`INVALIDATED`；状态 owner 是 MES 绑定服务 |
| `bound_by`、`bound_at` | Long/DateTime | 生产组长及绑定时间 |
| `invalidated_at`、`invalidation_code` | DateTime/String | 来源漂移、移除或迁移 blocker 的审计原因 |
| `version` | Integer | 乐观锁；写命令必须携带 `expectedVersion`（若目标已存在） |
| `idempotency_key`、`request_payload_hash` | String | 租户内幂等和同键载荷冲突检测 |

唯一性：

- 同租户一个活跃订单最多一个 `BOUND/FROZEN` 绑定；使用条件生成列或等价条件唯一索引，不能用覆盖 `INVALIDATED` 历史的宽泛唯一索引。
- 同租户 `idempotency_key` 唯一；相同 key + 相同载荷返回原回执，相同 key + 不同载荷返回结构化冲突。
- `pick_list_id` 可被多个活跃订单引用，但每个绑定必须有独立的快照和来源审计，不共享可变状态。

### 5.2 领料单明细快照

建议新增表 `mes_pro_process_pool_active_order_pick_list_binding_item`：

- `binding_id`、`pick_list_item_id`、`source_entry_id`、`source_line_key` 必填。
- 保存物料编码、名称、规格、单位、批次号、申请/实际领料数量、生产订单号/行号以及原始修改时间快照。
- `(tenant_id, binding_id, pick_list_item_id)` 和 `(tenant_id, binding_id, source_entry_id)` 唯一；缺少稳定分录号或行键直接阻断。
- 同物料多明细：绑定快照必须保存全部明细和全部稳定 ID。canonical 只用于单值字段解析，不得替代明细集合；下游批次和追溯必须引用全部明细快照。
- 正式匹配规则已冻结：领料单生产工单号（当前代码字段 productionOrderNo）必须与所选生产工单的正式工单号精确一致。候选查询先按该字段过滤，提交绑定时后端必须重新读取并校验；不再使用路线 BOM 或 ERP 物料目录做二次筛选。匹配通过后冻结领料单头和全部明细，明细不得因物料目录比较被裁剪。

### 5.3 批次执行稳定关系

建议新增表 `mes_pro_edhr_batch_execution_pick_list_binding`，作为批次执行的不可变关系：

- `batch_execution_id`、`active_order_id`、`binding_id`、`work_order_id`、`pick_list_id`、`source_snapshot_hash`、`linked_stage`、`linked_at`、`relation_status`。
- 关系写入必须与批次执行创建或复用、以及完成节点三类回填处于同一事务；下游初始化失败整体回滚。
- `(tenant_id, batch_execution_id, binding_id)` 唯一；同一批次执行不能绑定两个不同有效领料单。
- 关系表快照不可因 ERP 后续同步而更新；来源漂移只改变绑定状态为 `INVALIDATED` 并阻塞继续放行，保留原快照和审计。
- 合法独立入口可没有 activeOrderId，但必须有等价正式领料/物料来源凭证、稳定关系、完整快照、sourceSnapshotHash、来源版本、幂等键和追溯根；活跃订单链路必须消费 pickListBindingId，任何入口不得按 workOrderId 临时反查。

### 5.4 状态所有者和状态机

| 状态/字段 | 唯一 owner | 允许的变化 |
| --- | --- | --- |
| ERP 领料单 `documentStatus`、`sourceModifyTime` | ERP 同步链路 | ERP 正式同步更新；MES 只读核验 |
| 绑定 `bindingStatus` | MES 活跃订单绑定服务 | `BOUND -> FROZEN`；来源漂移/移除/数据修复只能 `-> INVALIDATED` |
| 活跃订单 `activeStatus/businessStatus` | 活跃订单服务 | 与绑定状态独立；移除不删除绑定历史 |
| 批次关系 `relationStatus` | eDHR 批次执行服务 | `LINKED -> INVALIDATED`；不得回写成另一张领料单 |
| 审计事件 | 审计服务/事务调用方 | 追加，不作为业务状态读取来源 |

## 6. 接口设计

### 6.1 候选查询

`GET /mes/pro/process-pool/team-leader/active-order/pick-list-options?workOrderId=<string>`

响应 `PickListOptionRespVO[]`：

```text
{
  id: string,
  sourceFid: string,
  sourceBillNo: string,
  documentStatus: "C" | other,
  sourceModifyTime: string,
  productionOrderNo: string,
  matchedMaterialCount: number,
  matchedItemIds: string[],
  selectable: boolean,
  blockerCode?: string,
  blockerMessage?: string,
  candidateSnapshotHash: string
}
```

候选接口只返回当前租户、当前生产组长负责范围内、能解析到正式工单关系的记录；它是展示和预检，不是授权。前端必须显示领料单号、审核状态、来源时间和物料摘要；没有可选候选时提交按钮保持禁用并显示后端 blocker。

### 6.2 加入请求和回执

`POST /mes/pro/process-pool/team-leader/active-order/add`

请求字段：

```text
{
  workOrderId: string,
  pickListId: string,
  pickListCandidateSnapshotHash: string,
  idempotencyKey: string,
  expectedActiveOrderVersion?: number
}
```

后端必须重新读取正式数据，不信任候选快照中的状态、物料或名称。成功回执：

```text
{
  activeOrderId: string,
  workOrderId: string,
  pickListBindingId: string,
  pickListId: string,
  bindingStatus: "BOUND" | "FROZEN",
  sourceSnapshotHash: string,
  action: "ADD" | "REUSE" | "RECOVER",
  bindingVersion: number
}
```

同一工单已有活跃订单时，只有请求领料单与现有有效绑定的 `pickListId + sourceSnapshotHash` 完全一致才允许 `REUSE`；不同领料单必须返回 `ACTIVE_ORDER_PICK_LIST_CONFLICT`，不得覆盖绑定。历史移除订单只能在历史绑定仍存在、来源未漂移且请求领料单一致时 `RECOVER`；否则要求正式重建或删除后重新新增，不能猜测。

### 6.3 结构化 blocker

至少冻结以下 `blockerCode`，不能只返回中文 `msg`：

`PICK_LIST_REQUIRED`、`PICK_LIST_NOT_FOUND`、`PICK_LIST_NOT_APPROVED`、`PICK_LIST_WORK_ORDER_MISMATCH`、`PICK_LIST_TENANT_MISMATCH`、`PICK_LIST_DETAIL_ID_MISSING`、`PICK_LIST_DETAIL_DUPLICATE_SOURCE_ENTRY`、`PICK_LIST_SOURCE_STALE`、`ACTIVE_ORDER_PICK_LIST_CONFLICT`、`ACTIVE_ORDER_PICK_LIST_BINDING_MISSING`、`IDEMPOTENCY_CONFLICT`、`BATCH_PICK_LIST_RELATION_MISSING`、`BATCH_PICK_LIST_RELATION_CONFLICT`。

响应至少包含 `blockerCode`、`objectType`、`objectId`、`workOrderId`、`pickListId`（若有）、`sourceSnapshotHash`（若有）、`retryable`、`ownerStage`，以便前端、批次执行和追溯统一处理。

## 7. 多途径创建批次执行的前置条件

| 入口 | 前置条件与关系 | owner |
| --- | --- | --- |
| 活跃订单完成 | activeOrderId、workOrderId、pickListBindingId、pickListId、sourceSnapshotHash、bindingVersion、完成回填 receipt；成功写 batchPickListRelationId | 流程修复6 |
| 合法独立创建 | 可无 activeOrderId，但必须有 entryType、entryBusinessId、等价正式领料/物料来源凭证、头/全部明细 ID、完整快照、sourceSnapshotHash、来源版本、稳定关系、幂等键和请求 hash | 流程修复9分类，流程修复6落库 |
| 批次放行 | 活跃绑定或独立等价来源关系、四材料 manifest、放行申请和版本 | 流程修复8门禁，流程修复10最终状态 |

独立入口不因缺少 activeOrderId 自动禁止，但缺少等价正式来源、稳定关系、hash、幂等或追溯根必须阻断；任何入口不得按 workOrderId 临时反查。独立入口的等价正式来源类型仍由流程修复9冻结，不得用 workOrderId 临时反查。

## 8. 跨线程接口与邻接契约

### FR1-PICK-6：回填后创建/复用批次（流程修复6）

输出给 6：activeOrderId、workOrderId、routeId、routeVersionId、pickListBindingId、pickListId、sourceSnapshotHash、bindingVersion、pickListDetailIds（全部明细，Long 以字符串传输）、completionBackfillReceiptId、completionAggregateHash、idempotencyKey。6 在三类回填成功后同事务创建/复用批次，并输出 batchExecutionId、batchPickListRelationId、relationStatus、绑定版本/hash 和完整明细快照引用；不得按工单反查或建批后补关系。

### FR1-PICK-7：批次执行完整映射和放行后追溯（流程修复7）

输出给 7：batchExecutionId、batchPickListRelationId、activeOrderId（活跃链路必填，独立链路可空）、workOrderId、pickListBindingId（活跃链路必填）、pickListId、绑定头快照、全部明细快照/明细 ID、sourceSnapshotHash、bindingVersion、来源版本、完成回填和关系状态。7 输出不可变追溯根，反查头、全部明细、快照 hash、生产/PQC、损耗、回填、材料 manifest 和放行事件；不得只保留 canonical 行。

### FR1-PICK-8：四份材料上传门禁（流程修复8）邻接

8 只负责来料检报告、灭菌报告、成品检报告、成品检记录的版本/hash、上传责任和齐套门禁，消费 6/7 的批次关系，不替代领料绑定。

### FR1-PICK-9：多入口前置条件合同（流程修复9）

9 为所有创建/放行入口冻结分类：活跃订单入口强制 pickListBindingId；合法独立入口可无 activeOrderId，但必须有入口自有等价正式来源类型、头/全部明细、完整快照、稳定关系、sourceSnapshotHash、来源版本、幂等键、请求 hash、权限和追溯根。所有入口禁止按 workOrderId 临时反查；来源策略未冻结时返回 blocker。

### FR1-PICK-10：最终放行状态与追溯（流程修复10）邻接

10 负责唯一最终 RELEASED 状态、放行事务和放行后追溯根，消费 7 的完整关系映射及 8 的四材料 manifest。10 尚未完成设计，未冻结前置和状态 owner 是实现 blocker。

### FR1-PICK-11：BDD/TDD/迁移总门禁（流程修复11）邻接

11 汇总 1-10 的 BDD、严格 RED/GREEN/REGRESSION、迁移、历史阻断和回滚证据；本任务字段、独立入口分类、全量明细和生产工单号精确匹配规则必须纳入其矩阵。本任务不宣称生产 RED/GREEN/REGRESSION 已运行。
## 9. 迁移与回滚边界

- 迁移前只读盘点所有 `ACTIVE` 活跃订单、对应工单、正式已审核领料单候选、明细稳定 ID、已有放行/批次执行关系和重复情况。
- 旧活跃订单没有唯一正式领料单时不得自动填充；必须返回迁移 blocker，由批准的“正式重建”或“删除后重新新增活跃订单”流程处理，禁止直接 SQL 更新活动订单或运行快照。
- 已有批次执行但没有有效完成回填和绑定证据时，不认领、不复用、不自动删除；按现有历史批次执行门禁冻结并单独制定迁移方案。
- 回滚只允许撤回尚未被业务使用的新增 schema/代码；对已产生的绑定和快照不得物理删除，使用 `INVALIDATED` 加审计保留证据。恢复旧代码也不得恢复“按工单反查成功即放行”的行为。
- 迁移唯一 owner、字段类型、条件唯一索引、执行顺序、回滚脚本和历史预检必须由数据库线程确认后才能开发。

## 10. 主要失败 blocker

1. 当前加入 API、前端 DTO、活跃订单表和批次执行表均缺少正式领料单关系。
2. 当前放行 writer 仍按工单号解析领料单，尚无“按绑定快照读取”的正式端口。
3. 当前历史 `REUSE/RECOVER` 逻辑不比较领料单绑定，改造前无法保证幂等和恢复安全。
4. 现有 HTTP Long ID 类型为 number，需冻结字符串合同并完成前后端适配。
5. 没有确认测试租户、账号、已审核领料单、明细和可清理数据，无法在本轮执行真实 E2E。

## 11. 实现顺序与门禁

1. 先由本任务冻结 API/状态/表字段和 blocker 编码，并由流程修复6、7、9书面确认。
2. 数据库线程先完成 schema 预检和迁移设计；未通过不得改 Java/TypeScript。
3. 按严格 TDD 先补后端绑定服务和 mapper 合同测试，再实现接口；前端只在后端合同冻结后增加候选选择和提交。
4. 完成节点、批次执行和资料上传消费者分别只实现自己的字段消费，不共享临时反查 helper。
5. 最后用真实页面路径验证加入、刷新、完成、批次关系、四份材料、放行和追溯；任何缺失前置都记录 blocker，不以 mock/API-only 代替。

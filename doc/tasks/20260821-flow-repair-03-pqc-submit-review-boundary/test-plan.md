# Test Plan

## 1. Test Objective

后续实施必须证明：一线 PQC 和 PQC 组长只生产确认后的结构化来源；正式过程检验单只由活跃订单完成节点生成；所有身份、逐件值、设备快照、版本、签名、幂等和追溯在提交到放行之间不漂移。

本文件仍是合同测试计划，不是完整链路已执行证据。流程 3 task-owned 定向测试的实际结果记录在 `execution-log.md` 和 `verification-report.md`；流程 4/6/7/8/9/10/11、数据库验证和真实 E2E 仍为 `planned / NOT RUN`。

## 2. Acceptance Matrix

| ID | Behavior | Required Evidence |
| --- | --- | --- |
| PQC-03-01 | 正式任务完整时可提交全部检验方法和计划数量的逐件结果 | 服务测试 + 请求合同 + DB 断言 |
| PQC-03-02 | 无设备项目允许无设备；要求设备的项目冻结提交时设备快照 | 服务测试 + 前端合同 |
| PQC-03-03 | 提交仅生成 `SUBMITTED` 来源，不生成正式过程检验单 | 事务测试 + 负向 DB 断言 |
| PQC-03-04 | 组长在正式人员范围内读取同一 payload 确认 | 权限/服务测试 |
| PQC-03-05 | 确认原子生成唯一 aggregate version，任务为来源 `CONFIRMED` | 聚合服务事务测试 |
| PQC-03-06 | 退回不生成汇集，新提交创建新 revision 且旧版不可消费 | 状态机测试 |
| PQC-03-07 | 终态后重复同命令幂等，相反决策和同键不同内容冲突 | 幂等与状态冲突测试 |
| PQC-03-08 | 并发复核只有一个 CAS 胜者，无孤立复核/汇集明细 | 并发集成测试 |
| PQC-03-09 | 缺逐件、版本、签名、设备快照或跨租户时 fail-fast | 参数化负向测试 |
| PQC-03-10 | 流程 4 双 100% 完成节点只消费唯一确认 aggregate version 并回填正式过程检验单 | 流程 4 集成合同 |
| PQC-03-11 | 三类回填成功后流程 6 才创建/复用批次执行，流程 7 再映射批次执行过程检验记录 | 流程 6/7 集成测试 |
| PQC-03-12 | 流程 8 校验四份材料，流程 10 完成最终放行和管理者代表签名，流程 11 验证追溯 | 流程 8/10/11 集成 + 真实 E2E |
| PQC-03-13 | 活跃订单 receipt 与独立场景 receipt 是互斥合法建批分支，均缺失、同时出现或伪造身份均阻断 | 流程 6/9 合同测试 |
| PQC-03-14 | 流程 7 Origin/TraceLink 或适用 PQC 映射未完成/hash 不一致时，流程 8/10 均阻断 | 流程 7/8/10 负向集成测试 |
| PQC-03-15 | 批次详情、PQC/生产申请、管理者代表批准、独立批次入口只适配统一 gate/finalization | 多入口真实 E2E |

## 3. Detailed BDD

### Scenario A: Complete Frontline Submission

Given 当前租户存在已锁定活跃订单、路线工序、QA 规程版本和 `PENDING` PQC 任务，任务计划数量与项目规则完整
When 当前一线 PQC 用户用有效签名提交所有检验方法的精确逐件明细和要求的设备选择
Then 服务端冻结任务/路线/规程/人员/签名/设备/逐件快照，生成唯一 `SUBMITTED` revision 和 payload hash
And 正式过程检验单、批次执行和放行记录的新增数量均为 0

### Scenario B: Optional Equipment

Given 一个项目 `equipmentRequired=false`，另一个项目 `equipmentRequired=true`
When 前者无设备提交且后者选择正式设备提交
Then 前者明确保存无需设备语义，后者保存提交时设备 ID/编码/名称/编号快照
And 后续确认与回填不查询当前设备配置替代快照

### Scenario C: Leader Confirmation

Given 一条属于当前组长正式人员范围的 `SUBMITTED` revision
When 组长携带同一 task/event/revision/hash、expectedVersion、幂等键和签名确认
Then 同一事务写复核审计、推进来源 `CONFIRMED`、创建唯一 aggregate version 和完整明细
And 不创建正式过程检验单或批次执行

### Scenario D: Return And Revision

Given 一条 `SUBMITTED` revision
When 组长填写原因退回
Then 原 revision 进入不可消费退回终态且无 aggregate
When 一线 PQC 修正并重新提交
Then 创建 revision+1，新旧 payload hash 与审计链均保留

### Scenario E: Idempotency And Concurrency

Given 同一复核动作已经成功
When 使用相同幂等键和相同 payload 重试
Then 返回原 review/aggregate identity，不新增记录
When 使用相同键提交不同 payload，或两个 actor 使用同一 expectedVersion 竞争
Then 明确返回幂等内容冲突或版本冲突，只有一个有效终态

### Scenario F: Fail Fast On Missing Source

Given 来源缺少逐件明细、规程版本、签名、设备快照、数量一致性或租户身份
When 任何提交、确认或完成消费命令执行
Then 返回稳定 blocker code，所有业务写入回滚
And 不读取 raw payload、当前 QA/设备配置、旧 IPQC、`formBindings` 或生产提交进行补齐

### Scenario G: Completion Consumption

Given 流程 2 已完成生产事实复核，活跃订单生产与检验进度均为 100%，存在唯一确认 aggregate，损耗事实和正式批记录绑定齐全
When 生产组长在流程 4 完成节点点击活跃订单完成
Then 流程 4 用精确 aggregate version 回填正式过程检验单，并与批记录/实际损耗单在同一业务节点完成，输出 `formalProcessInspectionDocumentId` 和不可变 `completionBackfillReceiptId/completionBackfillReceiptHash/completionVersion/sourceSnapshotHash`
And 流程 6 只能消费该完整 receipt 创建或复用批次执行，流程 7 再建立批次执行过程检验记录映射

### Scenario H: Release Traceability

Given 流程 6 已创建或复用批次执行，流程 7 已完成放行前 Origin/TraceLink 和适用的 PQC 过程检验映射，流程 8 已校验四份材料全部齐全
When 流程 10 完成最终放行且用户通过流程 7 追溯查询查看来源
Then 可追到活跃订单、生产工单、领料单、一线生产/PQC、双方复核、损耗、正式过程检验单、aggregate、任务、逐件和设备快照，流程 10 只提供放行审计事实

### Scenario I: Two Legal Batch Provision Branches

Given 一个活跃订单已取得流程 4 完整 `completionBackfillReceipt`
When 流程 6 创建或复用批次
Then 只消费该 receipt，不直接消费流程 3 aggregate
And 缺 receipt 返回 `BACKFILL_RECEIPT_REQUIRED`，来源快照不一致返回 `SOURCE_SNAPSHOT_MISMATCH`

Given 一个排产、手工、独立或独立 PQC 场景已由流程 9 签发 canonical `IndependentBatchPrerequisiteReceipt`
When 流程 6 创建或复用批次
Then 使用该场景凭证且不得伪造 `activeOrderId`、流程 4 receipt 或流程 3 aggregate
And 独立入口缺凭证返回 `ENTRY_PREREQUISITE_MISSING`，无效来源返回 `ENTRY_SOURCE_INVALID`；两种 receipt 同时出现时明确冲突

### Scenario J: Pre-Release Mapping Hard Gate

Given 流程 6 已建批，但流程 7 Origin/TraceLink 未完成、适用的 PQC 过程检验映射缺失或来源 hash 不一致
When 用户尝试上传材料或从任一合法入口申请最终放行
Then 映射缺失时传递 `TRACE_MAPPING_BLOCKED`，来源 hash 冲突时传递 `TRACE_SOURCE_CONFLICT`；流程 8 不接受材料状态推进，流程 10 不允许签名或转为 `RELEASED`
And 只有流程 7 返回 `preReleaseMappingStatus=READY` 后才进入四材料门禁

### Scenario K: Fixed Four Materials And Unified Release Entrances

Given 流程 7 放行前映射完成
When 用户从批次详情、PQC/生产申请、管理者代表批准或独立批次放行入口继续
Then 各入口只适配统一流程 8/10 gate，不得由流程 3 或 PQC 组长改变批次、材料或 `RELEASED` 状态
And 来料检报告、灭菌报告、成品检报告、成品检记录四份必须分别有效，成品检报告与成品检记录不可互代；gate 未满足返回 `RELEASE_GATE_BLOCKED`，最终快照不一致返回 `RELEASE_SNAPSHOT_MISMATCH`

## 4. Strict TDD Sequence

### RED 1: State Ownership

新增聚焦测试，证明当前对“已确认后再次退回/再次批准”的处理无法满足唯一终态与明确冲突。

`RED: planned backend focused test -> expected FAIL because production code does not yet enforce the frozen terminal-review and controlled-revision contract.`

### GREEN 1

实现最小状态机、expectedVersion CAS 和终态命令规则，使相同重试幂等、相反决策冲突。

`GREEN: NOT RUN -> 后续实现满足条件后的预期结果：相同聚焦测试通过且相反终态冲突被阻断。`

### RED 2: Source Revision And Aggregate Identity

新增测试要求确认结果返回唯一 aggregate version，且每行绑定 source revision/piece detail/payload hash。

`RED: planned aggregation contract test -> expected FAIL until aggregate-version contract is complete.`

### GREEN 2

补齐唯一身份、结构校验和原子写入，不改变现有逐件来源语义。

`GREEN: NOT RUN -> 后续实现满足条件后的预期结果：aggregate version、source revision、逐件明细和 payload hash 唯一绑定。`

### RED 3: No Premature Backfill

新增事务测试断言 submit/confirm 后正式过程检验单和 batch execution 数量为 0；完成前消费明确阻断。

`RED: planned cross-flow test -> expected FAIL wherever premature or ambiguous consumption remains.`

### GREEN 3

由流程 4 只在双 100% 和所有正式来源齐全时消费精确 aggregate version；流程 6 只创建/复用批次，流程 7 只做批次执行过程检验记录映射。

`GREEN: NOT RUN -> 后续实现满足条件后的预期结果：流程 4 仅在双 100% 完成并生成完整 receipt，流程 6 仅消费 receipt，流程 7 仅做批次映射。`

### RED 4: Four-Document Release

新增流程 8 测试，缺任一“来料检报告/灭菌报告/成品检报告/成品检记录”均阻断；流程 10 测试验证管理者代表签名和最终放行状态；流程 11 只做总体验证。

### GREEN 4

四类材料正式类型、唯一归属和放行门禁完成后通过。

`GREEN: NOT RUN -> 后续实现满足条件后的预期结果：流程 8 四份材料齐套有效后，流程 10 完成管理者代表签名并最终放行；流程 11 仅验证。`

### RED 5: Multi-Entry Provision And Pre-Release Mapping

新增流程 6/9 合同测试和流程 7/8/10 集成测试：证明当前两个建批 receipt 分支、Origin/TraceLink 硬门禁、适用 PQC 映射 hash 及多入口统一 finalization 尚未在生产代码闭合。

`RED: planned multi-entry and pre-release gate tests -> expected FAIL until receipt discrimination and mapping gates are implemented.`

### GREEN 5

实现最小 receipt 判别、流程 7 放行前映射状态、流程 8 前置校验和流程 10 唯一 CAS finalization。

`GREEN: NOT RUN -> 后续实现满足条件后的预期结果：两个合法建批分支均可通过，伪造/混用 receipt、缺映射、hash 不一致和旁路放行全部被阻断。`

## 5. Backend Test Plan

- 提交服务：任务状态、完整样本、所有方法、计划/实检数量、设备可选性、签名 actor、payload hash、Long ID。
- 复核服务：PQC 事件类型、人员范围、同租户、同 revision/hash、退回原因、终态冲突、受控修订。
- 汇集服务：仅 `SUBMITTED`、明细非空、任务/事件/record 一致、路线/规程/轮次冻结、CAS 行数、整事务回滚。
- 幂等：同键同 payload、同键不同 payload、不同 actor、跨租户、并发重复。
- 下游查询：0/1/多 aggregate、缺行、重复行、跨租户、当前配置变化不影响冻结快照。
- 流程 4：未双 100%、生产来源缺失、PQC 未确认、损耗为 0/大于 0、任一回填失败和重试。
- 流程 6/9：活跃订单 receipt、独立场景 receipt、两者同时出现/均缺失、伪造 activeOrder/PQC identity、创建/复用冲突和重试。
- 流程 7：Origin/TraceLink 缺失、PQC 适用性、正式单/aggregate 缺失、hash 不一致、重复映射、非适用场景伪造 PQC 映射和放行后追溯查询。
- 流程 8：流程 7 未 READY、四材料缺一、材料跨批次、类型无效、成品检报告与成品检记录互代；流程 10：缺签名、重复放行、并发 CAS 和旁路 RELEASED；流程 11：总体验证与迁移门禁。

未来实际命令必须在实施任务开始时从 Maven 模块和现有测试类中核对，不能在本设计中伪造已存在的测试入口。计划形式：

```text
mvn -pl yudao-module-mes -am -Dtest=<target tests> test
```

如果窄范围因兄弟模块缺符号失败，按经验门禁使用 `-am` 复验；零测试、class not found 或环境失败不能记为有效 RED/GREEN。

## 6. Frontend Contract Plan

- 所有 Long ID 保持字符串，不使用 `Number/parseInt/+`。
- 工序切换后使用当前正式 PQC task identity，切换检验类型/轮次后重新建立人员/任务上下文。
- 提交包含当前工序所有正式检验方法，每个任务独立 payload；逐件数组在 item results/raw payload/正式明细间完全一致。
- `equipmentRequired=false` 不显示设备卡且不强制；true 时在签名前校验正式设备身份。
- 只有提交动作运行严格数量/必填断言，中间编辑不触发 submit-only 校验。
- 提交/复核成功后按正式响应刷新；失败明确展示 blocker，不从中文提示推断状态、不默认成功。

计划命令须由实现任务核对实际脚本后运行：目标静态合同、相邻合同、`pnpm ts:check`，不得用静态合同替代真实 E2E。

## 7. Real Playwright E2E Plan

前置必须包含任务自有本地测试租户、生产组长/PQC 检验员/PQC 组长/放行角色、有效签名、菜单权限、活跃订单、生产工单/领料单、冻结路线/QA 规程/PQC 任务、正式批记录绑定和四类材料。任一缺失即 `BLOCKED`，且写请求应为 0。

真实页面路径：

1. 一线 PQC 登录，选择任务自有活跃订单和工序，完成多个检验方法的逐件数据与设备选择并签名提交。
2. PQC 组长登录，从正式待复核入口打开同一事件，核对 payload/hash 对应事实并确认。
3. 只读核验此时正式过程检验单和批次执行尚未创建。
4. 流程 2 完成生产事实复核；双进度达到 100% 后由生产组长在流程 4 完成节点点击完成；流程 4 统一回填并提交 `completionBackfillReceipt`。
5. 流程 6 仅消费流程 4 的完整 receipt 创建/复用批次执行，流程 7 建立 Origin/TraceLink 并映射流程 4 正式过程检验单与流程 3 aggregate；无实际损耗时无损耗单，有实际损耗时存在精确来源。
6. 在流程 7 未 READY 或来源 hash 不一致时验证流程 8/10 均阻断；READY 后上传四份固定材料，验证缺一或成品检报告/成品检记录互代均阻断。
7. 从批次详情、PQC/生产申请、管理者代表批准入口验证只能进入统一流程 8/10 gate；流程 10 放行后通过流程 7 查询完整来源链和四角色身份。
8. 通过真实页面清理任务自有数据或恢复状态；任何中途失败记录 cleanup 状态和残留身份。

独立场景真实路径另行使用任务自有排产/手工/独立或独立 PQC 数据：流程 9 签发 `IndependentBatchPrerequisiteReceipt`，流程 6 不要求或伪造 `activeOrderId`/流程 3 aggregate，流程 7 建立场景 Origin/TraceLink，随后仍必须经过相同流程 8 四材料门禁和流程 10 finalization。

本任务 `E2E: NOT RUN`。API 只允许在未来 E2E 最后做只读状态核验，不能代替页面路径。

## 8. Regression Set

- QA 规程到 PQC 任务生成：首检数量、巡检比例按工序独立，末检适用性项目级统一。
- 一线 PQC 连续提交、同工序多检验方法、默认样本物化、设备可选性。
- 组长人员范围、租户隔离、电子签名、事件时间线。
- 生产提交与 PQC 提交解耦。
- 活跃订单冻结路线身份、双进度完成、批记录/过程检验/损耗回填。
- 两类建批分支：活跃订单使用流程 4 receipt；排产、手工、独立或独立 PQC 使用流程 9 receipt；禁止混用或伪造身份。
- 流程 7 放行前 Origin/TraceLink、适用 PQC 过程检验映射、hash 一致性，以及放行后追溯读模型。
- 批次详情、PQC/生产申请、管理者代表批准、独立批次放行入口统一适配流程 8/10，不允许旁路改变材料或 `RELEASED`。
- 四份固定材料缺一/无效/互代门禁、重复放行幂等和并发 CAS。

## 9. Failure Blockers

- 正式 PQC task 或路线/QA 版本身份缺失。
- 逐件数量/项目与任务快照不一致。
- 流程 3 task-owned 提交回执身份、相同内容重试和相同幂等键冲突已在当前 `int_main` 通过定向验证；文档冻结的完整复核终态、受控修订、revision 与唯一 aggregate version 合同仍需后续完整实现/验证。
- 流程 4 无法在同一业务节点绑定精确 aggregate 并产生完整 `completionBackfillReceipt`；流程 6 若仅收到 `formalProcessInspectionDocumentId` 或直接读取流程 3 aggregate 必须阻断；流程 7 无法在批次执行中绑定流程 4 正式单并产生独立记录。
- 四份材料、流程 7/8/10/11 状态 owner 和禁止替代合同已经冻结；剩余 blocker 是生产实现、自动化测试、真实 E2E 和迁移证据未完成。
- 流程 6 两类 receipt 判别、流程 7 pre-release 映射或多入口统一 gate 未实现/未验证时，禁止进入生产 GREEN 或放行结论；跨线程失败必须断言 canonical 稳定码，不能按中文提示或内部细分码分支。
- 测试租户、角色、签名、权限、正式绑定或任务自有数据缺失。
- 历史数据无法证明唯一确认来源。

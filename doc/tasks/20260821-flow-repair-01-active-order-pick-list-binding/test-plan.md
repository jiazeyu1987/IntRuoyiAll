# Test Plan：活跃订单正式领料单绑定

## Validation Scope

- 本文件是流程1实现后的验证合同；已执行编译、定向后端测试、schema 合同和前端静态合同，不执行写入型 E2E。
- `Validation surface: real-browser` 适用于前端交互和最终业务路径。
- `Required tools: playwright` 适用于真实页面；后端合同使用 JUnit/Maven，前端静态合同使用 Node，schema 使用数据库迁移合同脚本。
- 所有 Long ID 的 HTTP JSON 断言按字符串；测试数据必须属于任务自有测试租户和可清理账号。

## Test Entry Preconditions

1. 后端 schema 已由唯一迁移 owner 应用并通过字段、索引、条件唯一性和回滚预检。
2. 流程修复6、7、9已确认 FR1-PICK-6/7/9 字段和状态 owner，没有同名平行字段。
3. 测试租户有：一个生产组长、一个一线生产、一个一线 PQC、一个 PQC 组长、管理者代表；有正式生产工单、至少一张已审核领料单和明细、路线/DCC/QA 配置。
4. 测试工单与领料单关系可通过正式 ERP 同步数据读取；禁止用 mock、SQL 直塞或前端静态选项。
5. 本地运行态、登录、菜单和权限按 `docs/login-access.md`、`docs/local-runtime.md`、`docs/e2e-rules.md` 通过门禁后，才执行真实浏览器路径。

## BDD Scenarios

### BDD: 生产组长必须显式选择正式领料单 -> Given/When/Then

- Given 生产组长打开“新增活跃订单”，输入一个可加入的生产工单。
- When 页面加载领料单候选。
- Then 只显示当前租户、当前工单、正式同步且可核验的领料单头及物料摘要；未选择领料单时提交按钮不可用，不能只提交工单。

### BDD: 成功加入同时落领料单头和明细快照 -> Given/When/Then

- Given 生产工单、已审核领料单 `documentStatus=C`、稳定分录号和正式物料关系均通过校验。
- When 生产组长选择该领料单并提交唯一 `idempotencyKey`。
- Then 同一事务创建活跃订单、一个 `BOUND` 绑定、全部明细快照和 `BIND_ACTIVE_ORDER_PICK_LIST` 审计；回执包含 `activeOrderId`、`pickListBindingId`、`pickListId`、`sourceSnapshotHash`。

### BDD: 未审核领料单阻断 -> Given/When/Then

- Given 用户选择的领料单头状态不是 `C`。
- When 提交加入请求。
- Then 返回 `PICK_LIST_NOT_APPROVED` 结构化 blocker，不创建活跃订单、绑定或快照。

### BDD: 工单和领料单不匹配阻断 -> Given/When/Then

- Given 领料单 `productionOrderNo` 与当前生产工单正式工单号不一致。
- When 提交加入请求。
- Then 返回 `PICK_LIST_WORK_ORDER_MISMATCH`，事务无任何新增写入。

### BDD: 明细稳定身份阻断 -> Given/When/Then

- Given 明细缺少 `sourceEntryId/sourceLineKey` 或分录号重复。
- When 提交加入请求。
- Then 返回对应 blocker（`PICK_LIST_DETAIL_ID_MISSING` 或 `PICK_LIST_DETAIL_DUPLICATE_SOURCE_ENTRY`），不写入部分快照。

### BDD: 同物料多明细确定性 -> Given/When/Then

- Given 同一领料单存在多个相同物料，但每条明细有不同正数 `sourceEntryId`。
- When 后续按物料读取来源字段。
- Then 全部明细都可追溯；canonical 行只用于单值字段解析时按 `sourceEntryId` 升序确定，不能替代绑定、批次关系或追溯中的全量明细。

### BDD: 同键同载荷幂等 -> Given/When/Then

- Given 第一次加入请求已成功提交 `idempotencyKey=K`。
- When 网络重试发送相同工单、领料单和快照哈希的 K。
- Then 返回相同活跃订单和绑定回执，不新增订单、绑定、明细或审计重复行。

### BDD: 同键不同载荷冲突 -> Given/When/Then

- Given K 已用于工单 A/领料单 P。
- When K 再用于工单 A/领料单 Q 或不同快照哈希。
- Then 返回 `IDEMPOTENCY_CONFLICT`，不得覆盖 P 或创建 Q。

### BDD: 活跃订单重复加入只允许相同绑定复用 -> Given/When/Then

- Given 当前工单已有 ACTIVE 活跃订单和 `pickListBindingId=P`。
- When 请求再次选择 P。
- Then 返回 `REUSE`；当请求选择 Q 时返回 `ACTIVE_ORDER_PICK_LIST_CONFLICT`，不更新既有绑定。

### BDD: 并发加入只形成一个绑定 -> Given/When/Then

- Given 两个相同权限用户并发提交同一工单和领料单但使用不同请求键。
- When 两个事务同时执行。
- Then 数据库唯一约束和锁只保留一个有效活跃订单/绑定；胜者返回 ADD，另一方重新读取并返回 REUSE 或明确冲突，不能生成两个有效绑定。

### BDD: 来源漂移不静默换单 -> Given/When/Then

- Given 绑定完成后 ERP 领料单状态、来源修改时间或头/明细哈希发生变化。
- When 活跃订单完成节点或批次执行创建读取来源。
- Then 绑定标记为 `INVALIDATED` 或返回 `PICK_LIST_SOURCE_STALE`，不得自动选择另一张领料单，不得创建不完整批次。

### BDD: 完成节点把绑定传给批次执行 -> Given/When/Then

- Given 活跃订单双 100%、三类回填成功且绑定为 `BOUND`。
- When 流程修复6创建/复用批次执行。
- Then 同一事务创建唯一批次关系行，关系包含 `batchExecutionId + activeOrderId + pickListBindingId + sourceSnapshotHash`，绑定状态升级为 `FROZEN`。

### BDD: 活跃订单链路必须消费绑定 -> Given/When/Then

Given 活跃订单完成节点创建批次。
When 请求缺少 pickListBindingId 或绑定 hash/version 不一致。
Then 返回关系 blocker，不得按 workOrderId 反查或换单。

### BDD: 合法独立入口使用等价正式来源 -> Given/When/Then

Given 合法独立入口没有 activeOrderId。
When 请求携带入口自有正式领料/物料来源凭证、稳定关系、完整快照、sourceSnapshotHash、幂等键和追溯根。
Then 按流程修复9分类合同创建/放行，并明确标记独立来源。

### BDD: 独立入口缺正式来源必须阻断 -> Given/When/Then

Given 独立入口只有工单、批号或路线。
When 请求创建或放行。
Then 返回来源 blocker，不得临时反查、默认成功或降级。

### BDD: 全量明细快照不被 canonical 削弱 -> Given/When/Then

Given 同物料存在多条稳定分录。
When 绑定、建批和查询追溯。
Then 保存并返回全部明细；canonical 只用于单值表单字段确定性解析。

### BDD: 生产工单号精确匹配 -> Given/When/Then

Given 领料单包含生产工单号列。
When 查询候选或提交绑定。
Then 仅当该列与当前生产工单正式工单号精确一致且领料单已审核时允许绑定。

### BDD: 追溯返回冻结来源 -> Given/When/Then

- Given 批次执行关系和绑定明细快照均存在，ERP 当前数据已更新。
- When 用户按批次执行或活跃订单查询追溯。
- Then 页面返回绑定时的领料单头、明细、来源哈希、当前状态核验结果和所有上游事实，并显示漂移原因；不把当前 ERP 新值冒充历史快照。

## Test Matrix

| ID | Layer | Case | Expected result | Evidence |
| --- | --- | --- | --- | --- |
| T1 | Backend contract | 请求字段、Long ID 字符串、结构化 blocker | PASS/FAIL 明确 | Controller/VO contract test |
| T2 | Backend unit | 候选过滤、审核状态、租户和工单匹配 | 只返回正式候选 | Service test |
| T3 | Backend unit | 明细稳定 ID、重复分录、全量快照与 canonical 单值解析边界 | 异常 fail-fast；全部明细保留 | Source resolver test |
| T4 | Backend unit | 领料单生产工单号精确匹配 | 候选过滤和提交复核一致；不匹配阻断 | Work-order match test |
| T5 | Backend transaction | ADD/REUSE/RECOVER 与绑定兼容性 | 相同绑定复用，不同绑定冲突 | Active-order service test |
| T6 | Backend transaction | 幂等同键同载荷/不同载荷 | 原回执/冲突，零重复写 | Idempotency test |
| T7 | Backend concurrency | 并发加入 | 条件唯一约束只留一条 | Concurrent integration test |
| T8 | Schema | binding/header/item/batch relation 表、索引、版本 | 字段和条件唯一符合设计 | Schema contract script |
| T9 | Frontend static | 领料单候选展示、必选校验、提交 payload | 不再只发送 `workOrderId` | Node static test |
| T10 | Frontend real-browser | 生产组长实际选择工单和领料单、刷新、重复提交 | 页面回执和 blocker 正确 | Playwright screenshot/trace |
| T11 | Batch integration | 完成节点创建批次关系 | 同事务、关系可复用 | Backend integration test |
| T12 | Release regression | 状态漂移、无绑定、四份材料未齐 | 明确 blocker，不放行 | Release preflight test |
| T13 | Trace real-browser | 通过批次执行查看领料单和明细快照 | 展示稳定快照和漂移审计 | Playwright screenshot + API final read |

## Strict TDD Sequence

源码检索、任务目录阅读和文档结构扫描属于审计/结构证据，不计为 TDD RED 或 GREEN；以下 RED 保留为历史设计证据，GREEN/REGRESSION 记录当前真实命令。

### RED

- 历史 RED：`mvn ... MesTeamLeaderActiveOrderPickListBindingTest` -> FAIL，原因是实现前缺少绑定字段/聚合；该证据已由后续实现关闭。
- 历史 RED：前端静态合同 -> FAIL，原因是实现前只有 `workOrderId`；该证据已由后续实现关闭。
- 历史 RED：schema 合同 -> FAIL，原因是实现前缺少绑定头/明细表；该证据已由后续实现关闭。

历史 RED 不重复运行；当前 PASS 证据统一记录在 `execution-log.md` 和 `verification-report.md`。

### GREEN

- `mvn -pl yudao-module-mes -DskipTests compile` -> PASS。
- 定向 JUnit/schema 合同（绑定服务、来源解析、回填、完成节点、批次 writer、Mapper/schema）-> PASS，100 tests，0 failures/errors。
- `node src/api/mes/pro/processpool/teamLeaderPickListBinding.static.spec.cjs` -> PASS。
- `git diff --check` 和 `scripts/preflight/branch-runtime-port-guard.ps1` -> PASS。

不把这些静态/单元证据扩大解释为流程4、6、7、8、9、10、11全链路 GREEN。

### REGRESSION

- 定向回归 JUnit：确认旧的加入、排序、移除、重建和 QA 快照行为不被破坏；结果包含 59 个活跃订单服务测试、12 个完成节点测试。
- `pnpm run ts:check`：PASS，确认 API 类型和活跃订单页面无 TS 回归。
- 既有放行 writer 测试：确认领料单来源改为绑定快照后仍覆盖物料首条确定性、唯一已审核单据和证据哈希。
- Playwright 真实路径：加入 -> 刷新 -> 完成 -> 批次执行 -> 四份材料 -> 放行 -> 追溯；任何登录/租户/服务/数据缺失必须 BLOCKED，不得用 API-only 代替。

## Pass/Fail Rules

- PASS 必须同时有业务码、响应字段、持久化行、来源哈希和审计证据；HTTP 200 或页面 toast 不算通过。
- 任一关系缺失、字段类型不一致、状态 owner 不清、同键不同载荷未冲突、来源漂移后换单或批次执行无关系，均为 FAIL。
- 真实浏览器通过的每个 case 必须引用截图、trace、HAR 或视频；没有证据只能是 NOT RUN/BLOCKED。
- 禁止 mock、默认成功、静默空值、直接 SQL、API-only 或吞异常。

## Current Verification Blockers

- 没有确认测试租户/账号/真实审核领料单/可清理数据，因此真实写入型 E2E 未运行。
- 流程修复6/7/8/9/10/11的后续阶段不在流程1完成声明内。

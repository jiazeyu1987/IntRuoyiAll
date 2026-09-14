# 开发文档 - 一线报工活跃订单自动分配

## Purpose and Scope

本开发文档把 PRD 转为后续实现方案。目标是修复现有代码与业务要求的差距：一线提交必须精确携带活跃订单 ID，后端在同一事务内创建初始分配，允许订单级超量并在组长列表红色标识，组长仍可调整分配。

## Evidence Reviewed

- 当前一线前端已选择活跃订单，但正式提交接口只传 `workOrderId`。
- 当前后端 `MesProFrontlineProcessPoolContextReqVO` 没有 `activeOrderId`。
- 当前一线提交服务只校验活跃订单上下文并创建报工事件，没有创建初始分配。
- 当前组长分配服务会把请求数量按订单剩余量和报工池剩余量截断，不符合“初始超量允许并红色标识”。
- 当前组长页面已有红色 `待调整` 标签、原订单预填和分配弹窗，可作为 UI 基础。

## Current Gap Analysis

| 业务要求 | 当前代码状态 | 差距 |
| --- | --- | --- |
| 一线选择活跃订单提交 | 页面已要求选择，后端按工单校验 | 缺正式 `activeOrderId` 入参和事件持久化 |
| 提交后自动分配到所选订单 | 组长弹窗打开时前端预填 | 未在后端提交事务内保存初始分配 |
| 超过订单数量仍允许提交 | 一线提交未按订单剩余量阻塞 | 分配保存会截断超量，不能保留 O1=Q 的初始事实 |
| 组长列表红色标识该订单 | 页面已有待调整标签 | 计算口径需改为“当前订单分配量 > 当前工序可承接量” |
| 组长可再分配到其它订单 | 已有分配弹窗和保存接口 | 保存逻辑需允许显式超量或至少不静默截断，且保留初始分配审计 |

## Backend Design

### API Contract

- 扩展一线提交上下文，新增必填 `activeOrderId`。
- 前端 `ProFrontlineProcessPoolContextReqVO` 与后端 `MesProFrontlineProcessPoolContextReqVO` 同步新增 `activeOrderId`。
- 后端提交校验改为按 `activeOrderId + workOrderId + routeId + routeProcessId + processId` 精确校验。
- 提交响应继续返回 `processPoolEventId`，可增加初始分配版本或分配快照字段，便于前端和测试验证。

### Transaction Flow

1. 校验登录账号、实际员工、签名员工、快照、活跃订单和生产上下文。
2. 创建正式报工。
3. 创建可选记录本原始条目。
4. 创建工序池 `PRODUCTION_SUBMIT` 事件，事件必须保存或可关联到 `selectedActiveOrderId`。
5. 调用正式分配服务创建初始分配：`activeOrderId=selectedActiveOrderId`，`allocatedQuantity=outputQuantity`，`allocationMode=FRONTLINE_SELECTED`。
6. 同事务提交；任一步失败全部回滚。

### Allocation Rules

- 初始分配路径不得按订单剩余量截断数量。
- 组长调整路径不得静默截断用户输入；如果某订单超量但业务允许，保存并标记超量；如果业务不允许某类状态调整，必须显式报错。
- 分配总量必须等于本次提交数量。
- 当前分配版本要能区分初始自动分配和组长调整分配。
- 审计要记录每次变更的 before / after、模式、操作者和原因。

### Data Model

- 选项 A：在工序池事件表新增 `selected_active_order_id`，并在读模型返回。
- 选项 B：用初始分配记录作为选中订单事实，同时审计中标记初始来源。
- 推荐：事件保存 `selected_active_order_id`，分配表保存当前分配。这样即使组长后续调整，也不会丢失一线最初选择。
- 分配表或读模型需要提供 `overageQuantity`、`needsAdjustment` 或可计算字段。

### 已落地实现决策

- 最终采用选项 B：一线选择的 `activeOrderId` 进入提交上下文、事件创建 BO 和事件原始载荷，同时以版本 1、`FRONTLINE_SELECTED` 当前分配及 `INITIAL_BASELINE` 审计保存原始选择事实；组长后续版本化调整不会删除初始审计。
- 未新增事件表 `selected_active_order_id` 列，避免为重复事实增加第二个可漂移来源；正式初始分配和审计是订单归属的权威持久化链路。
- 订单超量口径已固定为同一 `activeOrderId + routeProcessId + processId` 下全部 CURRENT 分配之和超过正式订单工序 `planned_quantity_snapshot` 的数量；缺该正式快照时服务直接失败，不使用订单总量、未分配量或前端推断。
- 报工管理列表和当前分配快照均直接返回 `overageQuantity/needsAdjustment`；前端把这些字段及分配数组作为必需正式合同，缺失时不降级为空分配。

### Error Model

- 缺 `activeOrderId`：提交失败，提示“请选择活跃订单后再提交”。
- 活跃订单不属于当前组长范围或不是 ACTIVE：提交失败，提示订单不可用。
- 提交事件创建成功但初始分配失败：事务回滚，整体提交失败。
- 调整总量不等于提交数量：保存失败，提示分配总量必须等于本次提交数量。
- 目标订单已移除或已锁定：保存失败，提示订单不可调整。

## Frontend Design

### 一线生产页面

- 选择活跃订单后，保存 `activeOrderId` 到提交上下文。
- 提交按钮仍要求活跃订单、工序、员工、签名和数量完整。
- 提交 payload 的 `feedbackPayload`、`processPoolContext`、`rawPayload` 均应包含可追溯的选中订单信息。
- 成功提示应表达“已提交并分配到所选订单”，避免只提示报工成功。

### 生产组长报工管理

- 列表显示一线最初选择的订单和当前分配订单。
- 当任一当前分配订单超出该订单当前工序可承接数量时，该订单标签或行内标识使用红色。
- 红色标识展示超量数，不只展示未分配数量。
- 打开分配弹窗时，若已有初始分配，直接读取后端当前分配，不再由前端临时预填冒充分配事实。
- 组长调整后刷新当前分配、超量标识和审计。

## State and Data Flow

一线页面选择活跃订单 -> 一线正式提交 -> 后端创建报工事件和初始分配 -> 组长列表读取事件、选中订单、当前分配和超量状态 -> 组长调整分配 -> 后端保存新版本和审计 -> 列表刷新状态。

## Security and Permissions

- 一线提交仍使用当前登录设备账号、实际员工、签名员工和会话快照校验。
- 活跃订单必须属于当前生产组长授权范围。
- 组长分配保存必须校验当前登录用户是生产组长且具备目标工序权限。
- 不允许前端传入其它组长订单或非活跃订单绕过权限。

## Observability

- 一线提交日志记录事件 ID、选中活跃订单 ID、初始分配版本和分配数量。
- 组长调整日志记录版本号、调整前后订单和数量。
- 错误日志区分提交失败、初始分配失败、列表刷新失败和调整保存失败。

## Milestone Plan

### 里程碑 1：后端合同 RED

目标：先用可执行失败测试锁定 activeOrderId、提交事务内初始分配和超量不截断三个后端合同。

涉及文件：

- IntRuoyiBackend/yudao-module-mes/src/test/java
- IntRuoyiBackend/yudao-module-mes/src/test/js

交付物：

- 后端 RED 测试及预期失败证据。

- 新增/更新单元测试，断言一线提交上下文必须包含 `activeOrderId`。
- 新增测试，断言一线提交后创建初始分配且数量等于提交数量。
- 新增测试，断言提交数量超过订单剩余量仍成功并生成超量状态。

### 里程碑 2：后端实现 GREEN

目标：在一线正式提交事务内保存精确活跃订单身份和全量初始分配，并让组长调整不再静默截断。

涉及文件：

- IntRuoyiBackend/yudao-module-mes/src/main/java
- IntRuoyiBackend/yudao-module-mes/src/main/resources
- IntRuoyiBackend/yudao-module-mes/src/test/java
- IntRuoyiBackend/sql/mysql

交付物：

- 后端接口、事务、分配服务和读模型实现。
- 后端 GREEN 与相邻回归证据。

- 扩展 VO、BO、事件创建和读模型。
- 在一线提交事务内调用初始分配保存能力。
- 修改分配服务，避免超量路径被静默截断；必要时拆分初始分配与普通快捷分配的规则。

### 里程碑 3：前端合同 RED

目标：先用前端静态合同锁定提交 activeOrderId、订单级红色超量标识和正式当前分配读取。

涉及文件：

- IntRuoyiFronted/tests/e2e

交付物：

- 前端 RED 静态合同及预期失败证据。

- 静态合同断言一线提交 payload 包含 `activeOrderId`。
- 静态合同断言组长列表红色标识基于订单超量，而不是仅基于未分配数量。
- 静态合同断言分配弹窗读取后端当前分配，不用前端预填冒充已保存。

### 里程碑 4：前端实现 GREEN

目标：让一线提交和生产组长报工管理页面完整消费新的活跃订单与初始分配合同。

涉及文件：

- IntRuoyiFronted/src/api/mes/pro
- IntRuoyiFronted/src/views/mes/pro
- IntRuoyiFronted/tests/e2e

交付物：

- 一线提交 payload、组长列表红色标识和分配弹窗实现。
- 前端 GREEN、类型检查和相邻回归证据。

- 一线提交 payload 加入 `activeOrderId`。
- 组长列表展示初始选中订单、当前分配和订单级超量标签。
- 组长调整后刷新分配快照和超量状态。

### 里程碑 5：真实 E2E

目标：通过真实一线提交和生产组长调整路径证明目标业务闭环，并完成回归、清理和融合前验证。

涉及文件：

- IntRuoyiFronted/tests/e2e
- doc/tasks/20260814-frontline-active-order-submit-allocation-docs

交付物：

- 真实 Playwright E2E 证据。
- 定向回归、清理和融合 int_main 证据。

- 构造任务自有活跃订单 O1、O2，其中 O1 当前工序可承接数量小于提交数量。
- 一线账号选择 O1 提交 Q。
- 生产组长账号查看报工管理列表，确认 O1 红色标识。
- 生产组长把部分数量调整到 O2，确认当前分配、审计和红色标识变化。

### 里程碑 6：融合后 int_main 真实 E2E 纠偏验证

目标：在 `E:\IntRuoyi` 当前 `int_main` 的固定运行态 `8081/48081` 上重新执行完整真实页面链路，补齐融合后运行证据；旧 worktree 的 `8099/48099` 事件 227/228 只保留为融合前证据。

涉及文件：

- IntRuoyiFronted/tests/e2e
- doc/tasks/20260814-frontline-active-order-submit-allocation-docs

交付物：

- 当前 `int_main` 分支、融合提交和 `8081/48081` 运行态归属证据。
- 使用任务自有 fixture 新建 O1/O2、独立一线/组长账号和正式权限后执行的真实 Playwright E2E 证据。
- 一线选择 O1 提交 10、O1 计划 6、版本 1 `FRONTLINE_SELECTED`、O1=10、红色待调整 4 的融合后证据。
- 组长改配 O1=6/O2=4、版本 2 `MANUAL`、总量 10、未分配 0、红色消失和审计完整的融合后证据。
- 目标业务写请求仅包含一线提交与组长确认，页面错误、目标请求失败、目标 HTTP 错误、目标控制台错误均为 0。
- finally cleanup 与独立二次 cleanup 均为 `CLEAN`、`remainingTaskDataCount=0`，并记录运行态按项目规则保留或停止。

纠偏边界：

- E2E 脚本必须支持显式 `POST_MERGE_INT_MAIN` 模式和 `8081/48081`，不得默认或硬编码旧 worktree 端口。
- 不得放宽业务断言，不得用静态合同、API-only、直接 SQL、旧事件或旧截图代替本轮真实页面证据。
- 若发现运行态或 E2E 验证链路缺陷，先记录 BDD 与 RED，再做最小正式修复并取得 GREEN。

### 里程碑 7：芋道源码 admin 补充真实 E2E

目标：按用户明确要求，在 `E:\IntRuoyi` 当前 `int_main` 的 8081/48081 运行态中，使用“芋道源码/admin”通过真实页面补跑同一业务闭环；该补充验证不替代 P5/P6 对独立一线与组长账号的角色隔离验收。

涉及文件：

- IntRuoyiFronted/tests/e2e
- doc/tasks/20260814-frontline-active-order-submit-allocation-docs

交付物：

- 显式 admin 补充运行模式及其静态合同，原租户 122、独立账号和 `POST_MERGE_INT_MAIN` 合同保持不变。
- “芋道源码”租户下只创建带任务标识、可追踪、可精确清理的 O1/O2、工序、路线和人员绑定；不得修改 admin 用户、密码、角色或既有业务基线。
- admin 真实登录后选择 O1 提交 10，验证 O1 计划 6、版本 1 `FRONTLINE_SELECTED`、O1=10、红色待调整 4。
- admin 通过生产组长真实页面改配 O1=6/O2=4，验证版本 2 `MANUAL`、总量 10、未分配 0、红色消失和审计完整。
- 目标业务写请求、四类目标错误、admin 基线前后指纹、finally cleanup、独立二次 cleanup 与任务数据残留证据。

补充边界：

- admin 模式必须显式启用并绑定租户 `1/芋道源码`、账号 `admin` 和主运行态 8081/48081；禁止静默切换或把 admin 模式作为原独立账号模式的 fallback。
- 不得新增、删除或修改 admin 用户、密码、角色、既有签名授权和正式业务数据；只允许创建并清理本轮任务自有业务 fixture。
- 若 admin 缺少页面入口、签名或业务身份前置，必须在目标业务写入前 BLOCKED，不得修改基线配置制造通过条件。
- 如需修复验证链路，必须先记录 BDD/RED，再做最小正式修复并保持 P5/P6 既有断言继续 GREEN。

## Open Questions

- 已解决：初始分配模式使用 `FRONTLINE_SELECTED`，与现有 `FIFO/MANUAL/SYSTEM` 并列。
- 已解决：超量按正式订单工序 `planned_quantity_snapshot` 与该订单工序全部 CURRENT 分配累计值计算。

## Design Blockers

- 缺正式 `activeOrderId` 持久化路径时不得实现为 `workOrderId` 推断。
- 如果分配表或读模型无法表达超量状态，必须先补数据合同。
- 如果测试环境缺真实账号、签名或任务自有订单，真实 E2E 必须阻塞。

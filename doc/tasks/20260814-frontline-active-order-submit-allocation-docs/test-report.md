# Test Report

## Environment Used

- Evaluation mode: phase-gated
- Validation surface: task-defined

## Results

## P1

- Phase: 后端合同 RED
- Overall outcome: PASS（RED 阶段通过）

### Acceptance Results

- P1-AC1 — PASS：已形成可执行 RED 合同，覆盖精确 `activeOrderId`、提交事务内全量初始分配、超量不截断和订单级超量状态。
- P1-AC2 — PASS：`node IntRuoyiBackend\yudao-module-mes\src\test\js\frontline-active-order-initial-allocation-static.spec.cjs` 退出码为 1，5/5 项均因当前生产代码缺少目标业务合同而失败；不存在脚本路径、源文件缺失、Node 语法或测试装载错误。
- P1-AC3 — PASS（静态 RED 已确认，Java 执行受基线阻塞）：Node 合同断言后端上下文必须有带 `@NotNull` 的 `activeOrderId`，Java 合同测试通过反射锁定字段、类型和必填校验，并检查授权调用必须同时携带精确 `activeOrderId` 与工单上下文。
- P1-AC4 — PASS（静态 RED 已确认，Java 执行受基线阻塞）：Node 合同同时锁定 `submit` 的 `@Transactional` 边界，以及事件创建后、成功返回前调用 `createInitialAllocation`；Java 合同锁定初始分配使用所选 `activeOrderId` 和完整 `outputQuantity`。两者组合能够阻止“只建事件不落初始分配”及“事务外补写分配”。
- P1-AC5 — PASS（测试设计已确认，Java 执行受基线阻塞）：`MesReportAllocationCommandServiceTest` 将容量为 20、请求为 80 的旧截断断言改为持久化 80、未分配为 0、超量为 60、`needsAdjustment=true`，并捕获实际批量写入数量为 80；新增合同测试还锁定 `overageQuantity: BigDecimal` 与 `needsAdjustment: Boolean`。
- P1-AC6 — PASS：补入仅供隔离验证的最小编译前置后，Java/Maven 定向测试已进入 Surefire，17 项中 5 项按预期业务原因失败、其余 12 项通过；取证后验证前置已清除，不属于任务交付。

### Independent Evidence

- Command: `node IntRuoyiBackend\yudao-module-mes\src\test\js\frontline-active-order-initial-allocation-static.spec.cjs`
  Result: FAIL（预期 RED），`tests 5 / pass 0 / fail 5`。失败分别对应：缺少必填 `activeOrderId`、授权仍只按 `workOrderId`、未在成功返回前创建初始分配、仍以 `min` 静默截断超量、分配行缺少 `overageQuantity`。所有目标源文件均成功读取，测试运行器正常完成。
- Command: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineActiveOrderInitialAllocationContractTest,MesReportAllocationCommandServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  Result: BLOCKED，退出码 1；在 `yudao-module-dcc` 主代码编译阶段失败，`yudao-module-mes` 被跳过。缺少 `DccProjectCodeAssignmentCandidatePageReqVO` 和 `DccProjectCodeAssignmentCandidateRespVO`。
- Command: `mvn -pl yudao-module-mes "-Dtest=MesFrontlineActiveOrderInitialAllocationContractTest,MesReportAllocationCommandServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  Result: BLOCKED，退出码 1；MES 主代码编译出现 39 个引用错误，主要缺少 `MesProductionReportManagementSummaryService`、`MesTeamLeaderActiveOrderDetailRespVO`、`MesTeamLeaderActiveOrderDetail`、`MesTeamLeaderActiveOrderDetailService`、`MesProRouteProductCandidateCopyReqVO`、`MesProSchedulerWorkbenchAutoScheduleJobStatusRespVO`、`MesProSchedulerWorkbenchNightShiftCapacityStatusRespVO`、`MesProSchedulerWorkbenchRuntimeStatusService`、`MesFrontlineSessionSnapshot`、`MesFrontlineSessionSnapshotService`，未进入 testCompile 或 surefire。
- Command: `git cat-file -e int_main:<missing-class-path>` 与 `git ls-files -- <missing-class-path>`
  Result: 上述 DCC 前置类不在当前 worktree，且 `git cat-file` 对 `int_main` 返回 128；同名文件仅在 `E:\IntRuoyi` 显示为 `??` 未跟踪文件。MES 缺失前置类也与 `E:\IntRuoyi` 的未跟踪文件清单一致，不属于当前 `int_main` Git 基线。本次复核未复制、未修改这些文件。
- Command: `git diff --check`
  Result: PASS；P1 测试改动未发现空白错误。仅有现有行尾转换提示。

### Tester Review

- 新增测试不是以“测试类缺失”制造 RED：Node 测试可完整装载并逐条命中生产代码的真实业务缺口。
- Java 合同测试对提交编排采用源码合同，适合先锁定同一事务方法中的调用顺序；分配数量、超量和红色状态则由 `MesReportAllocationCommandServiceTest` 的真实服务行为断言兜底。
- 当前不能把 Maven 编译失败当作业务 RED，也不能用 `E:\IntRuoyi` 的未跟踪类补齐 worktree。正式前置类进入 `int_main` Git 基线后，必须重新运行两项 Maven 定向测试，确认预期业务断言失败，P1 才能解除 BLOCKED。

### P1 Unblock Addendum

- 初次 Maven 编译失败没有作为业务 RED。按项目编译基线差异门禁临时同步最小前置后，Java 测试已真实进入 Surefire：`tests 17 / failures 5 / errors 0`，5 项失败均为目标业务断言。
- 21 个验证专用未跟踪前置文件和 1 处 tracked 编译前置差异已在取证后清除；当前功能分支不交付这些其它任务文件。

## P2

- Phase: 后端实现 GREEN
- Overall outcome: PASS

### Acceptance Results

- P2-AC1 — PASS：后端已把一线明确选择的 `activeOrderId` 贯穿请求合同、授权、事件原始载荷和正式初始分配；提交数量完整落到所选活跃订单，订单工序容量不足不再导致静默截断。
- P2-AC2 — PASS：实现包含必填请求字段、精确活跃订单授权、提交事务内初始分配、`FRONTLINE_SELECTED` 模式、版本 1 分配与初始审计、可空 `review_id` 迁移，以及订单级 `overageQuantity/needsAdjustment` 读模型。
- P2-AC3 — PASS：Node 静态合同 5/5 通过；Maven 7 类定向回归共 47 项全部通过，无失败、错误或跳过。
- P2-AC4 — PASS：`activeOrderId` 同时进入提交上下文、工序池事件 BO 和持久化原始载荷；组长当前分配快照能够按当前事件分配加其它当前事件分配计算订单工序超量。
- P2-AC5 — PASS：一线提交方法保留 `@Transactional(rollbackFor = Exception.class)`，在创建事件后、返回成功前调用初始分配；服务测试确认写入所选订单、完整数量、版本 1、无组长复核 ID、初始基线审计、数量片段重建、工序完成量协调及列表汇总刷新。
- P2-AC6 — PASS：组长保存路径不再按订单工序剩余量取 `min`；测试确认订单可承接 20、其它事件已分配 80 时，本事件请求 80 仍完整保存，并返回超量 60、`needsAdjustment=true`。同时，超过本次报工池总量仍显式拒绝，不会破坏总量守恒。
- P2-AC7 — PASS：独立复核已完成，验证命令、实际测试计数、业务合同检查和临时编译前置清理均有证据。

### Independent Evidence

- Command: `node IntRuoyiBackend\yudao-module-mes\src\test\js\frontline-active-order-initial-allocation-static.spec.cjs`
  Result: PASS，`tests 5 / pass 5 / fail 0`。必填 `activeOrderId`、精确授权、事务内初始分配、超量不截断、超量读模型五项合同均转为 GREEN。
- Command: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineActiveOrderInitialAllocationContractTest,MesFrontlineSubmitAuthorizationTest,MesProFrontlineFeedbackSubmitServiceTest,MesProFrontlineFeedbackPayloadSplitterTest,MesProcessPoolSubmitEventServiceAdapterTest,MesReportAllocationCommandServiceTest,MesFrontlineInitialAllocationSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  Result: PASS，`tests 47 / failures 0 / errors 0 / skipped 0`，Reactor 24 个项目全部 SUCCESS。分项为 schema 1、后端合同 4、payload splitter 3、提交服务 13、活跃订单授权 10、事件适配 2、分配服务 14。
- Code review: 组长版本 1 分配调整测试会把原订单 A 的当前分配置为 superseded、在版本 2 写入其它活跃订单 C，并重建数量片段、协调 A/C 两个受影响订单，证明初始分配后仍能调整到其它订单。
- Code review: 当前超量计算明确排除本事件旧版本/当前行的重复累计，以“其它事件当前分配 + 本事件当前分配 - 当前工序计划数量”计算超量；只对本次报工池总量执行硬校验。
- Schema review: `20260814_mes_frontline_selected_initial_allocation.sql` 将 `review_id` 改为可空并正式声明 `FRONTLINE_SELECTED/FIFO/MANUAL/SYSTEM`，缺表或迁移后仍不可空时会显式失败。

### Temporary Baseline Cleanup

- 按 P1 已批准的编译基线差异门禁，从 `E:\IntRuoyi` 精确临时同步 21 个缺失前置文件；同步前确认 21 个源文件均存在、worktree 目标均不存在，同步后 SHA-256 差异数为 0。
- 仅为匹配并行基线中的构造合同，临时给 `MesFrontlinePqcContextServiceImpl` 加入 `workOrder.getBatchCode()`；Maven 完成后已反向撤销。
- 清理结果：21 个临时源文件全部删除，`remaining=0`；`MesFrontlinePqcContextServiceImpl` SHA-256 恢复为 `3581BC6B184F3C80279428591F1EAB8219410061D28709B0E441437E4394A667`，该文件 `git diff` 和 `git status` 均为空。
- `git diff --check` 退出码为 0；仅输出仓库既有的 LF/CRLF 转换提示。临时编译前置未进入本任务交付。

### Tester Review

- 未发现 P2 后端业务合同缺口。精确订单身份、全量初始分配、超量状态和组长后续版本化调整均有生产实现与可执行测试对应。
- Maven 结果依赖已批准的并行编译基线前置；这些文件当前仍未进入 `int_main` Git 基线，因此后续融合验证必须继续按门禁区分正式交付与临时验证前置。

### P2 第二次独立复核（最终）

- Outcome: **PASS**。
- 结论纠正：先前 `Node 5/5 + Maven 7 类 47/47` 的 PASS 只覆盖分配命令读模型，未覆盖“生产组长报工管理列表”的正式订单级投影，因此该结论不完整。列表投影缺口补齐后，本次以新增第 6 项 Node 合同和第 8 个 Maven 测试类重新复核；以下 `Node 6/6 + Maven 8 类 50/50` 为 P2 后端最终结论，并取代此前仅基于 47/47 的结论。

#### Business Contract Results

- 报工管理列表订单级超量投影 — PASS：列表读模型及响应合同正式返回 `overageQuantity/needsAdjustment`；查询按 `activeOrderId + routeProcessId + processId` 关联正式订单工序快照，并按同一订单工序汇总所有 CURRENT 分配后计算超量，能区分列表中不同订单的红色调整状态。
- 缺正式快照 fail-fast — PASS：计划数量快照缺失时不会用默认值、其它订单或前端推断代替；服务映射在 `overageQuantity` 或 `needsAdjustment` 缺失时抛出 `IllegalStateException`。新增投影测试已锁定该失败路径。
- 初始全量分配 — PASS：一线提交携带必填且精确授权的 `activeOrderId`，提交事务在事件创建后、成功返回前将完整 `outputQuantity` 作为版本 1、`FRONTLINE_SELECTED` 初始分配写入所选订单。
- 超量允许 — PASS：订单工序剩余量不足时不按容量取 `min` 或截断；容量 20、请求 80 的用例完整写入 80，并返回超量 60、`needsAdjustment=true`。仅超过本次报工池总量时仍显式拒绝，以保持总量守恒。
- 组长后续重分配 — PASS：已有测试从一线初始版本 1 的订单 A 出发，将其标记为 superseded，并在版本 2 把数量分配到其它活跃订单 C，同时重建数量片段并协调 A/C 两个受影响订单，证明提交后仍允许组长改配。

#### Final Verification Evidence

- Command: `node IntRuoyiBackend\yudao-module-mes\src\test\js\frontline-active-order-initial-allocation-static.spec.cjs`
  Result: PASS，`tests 6 / pass 6 / fail 0`；第 6 项合同专门锁定报工管理列表的正式快照、订单级累计超量和 `overageQuantity/needsAdjustment` 投影。
- Command: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineActiveOrderInitialAllocationContractTest,MesFrontlineSubmitAuthorizationTest,MesProFrontlineFeedbackSubmitServiceTest,MesProFrontlineFeedbackPayloadSplitterTest,MesProcessPoolSubmitEventServiceAdapterTest,MesReportAllocationCommandServiceTest,MesFrontlineInitialAllocationSchemaTest,ProcessPoolTimelineReportAllocationProjectionTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  Result: PASS，`tests 50 / failures 0 / errors 0 / skipped 0`，`BUILD SUCCESS`。分项为 schema 1、后端合同 4、payload splitter 3、提交服务 13、活跃订单授权 10、事件适配 2、列表投影 3、分配服务 14。
- Command: `git diff --check`
  Result: PASS，退出码 0；仅有仓库既有 LF/CRLF 转换提示。

#### Final Temporary-Baseline Cleanup

- Maven 严格复用 P1 已批准的临时编译前置：精确同步 21 个当前 `int_main` 基线尚未包含的其它任务正式类，并仅在编译期间为 `MesFrontlinePqcContextServiceImpl` 补齐 `workOrder.getBatchCode()` 构造参数；未将这些前置视为本任务交付。
- 测试结束后 21 个临时文件全部删除，`removed=21 / remaining=0`，Git 状态按这些临时类名复查为 `temporaryStatusHits=0`。
- `MesFrontlinePqcContextServiceImpl` 已恢复，SHA-256 为 `3581BC6B184F3C80279428591F1EAB8219410061D28709B0E441437E4394A667`；该文件 `git diff --exit-code` 为 0、`git status --short` 为空。临时编译前置零残留。

## P3

- Outcome: **FAIL**。
- 现有静态合同均能执行且当前全部通过，但合同覆盖不完整：它们锁定了 `activeOrderId`、后端 `overageQuantity/needsAdjustment`、红色订单标签和删除前端初始分配预填，却没有锁定“正式列表/快照字段缺失时必须 fail fast”。因此测试通过不能证明无 fallback。

### P3 Static Contract Evidence

- `node tests\e2e\frontline-production-active-order-picker-static.spec.cjs` — PASS。
- `node tests\e2e\frontline-production-active-order-submit-attribution-static.spec.cjs` — PASS；提交上下文从所选活跃订单读取并发送必填 `activeOrderId`。
- `node tests\e2e\frontline-production-submit-payload-detail-static.spec.cjs` — PASS；相邻正式提交明细合同无回归。
- `node tests\e2e\team-leader-report-allocation-static.spec.cjs` — PASS；组长仍可从活跃订单列表选择其它订单并保存正式分配。
- `node tests\e2e\team-leader-report-allocation-clear-static.spec.cjs` — PASS，退出码 0。
- `node tests\e2e\team-leader-report-overage-highlight-static.spec.cjs` — PASS；不再用未分配量或订单总量猜测超量，也不再调用前端初始分配预填。
- `node tests\e2e\team-leader-report-shared-allocation-static.spec.cjs` — PASS；超量订单标签读取 `needsAdjustment` 并显示为红色。
- Coverage blocker：缺少负向合同禁止 `event.reportAllocations || []`、`snapshot.lines || []` 等静默空数组降级。

## P4

- Outcome: **FAIL**。
- 已通过的部分：一线正式提交携带所选 `activeOrderId`；列表订单标签直接读取后端 `needsAdjustment/overageQuantity`；分配弹窗删除了 `prefillSelectedOrderAllocation`，并从后端当前快照加载；组长修改后仍通过正式确认接口保存并刷新报工列表与活跃订单。
- Blocker 1：`TeamLeaderWorkbenchPage.vue` 的列表模板和 `resolveProductionReportOverageQuantity` 使用 `row.reportAllocations || []` / `event.reportAllocations || []`。后端正式分配投影缺失时会被静默解释成“没有分配、没有超量”，红色标识消失，违反 no-fallback 和正式投影要求。
- Blocker 2：`applyAllocationSnapshot` 使用 `(snapshot.lines || [])`，FIFO 预览同样使用 `(preview.lines || [])`。正式当前分配字段缺失时会被静默解释成空白分配表，组长可在错误基础上继续操作，未做到 fail fast。
- 吞异常审查：本次变更涉及的加载、保存和版本冲突分支均显示明确错误，未发现新增空 `catch` 或默认成功；未放行原因是上述静默空数组 fallback。
- Required before re-review：正式列表和分配快照必须在消费前验证数组与订单级超量字段完整，缺失时明确报错并停止操作；删除这些 `|| []` 降级，并补充静态负向合同后重跑本节全部命令。

### P4 Verification Evidence

- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` — PASS，退出码 0。
- `git diff --check` — PASS，退出码 0；仅有 LF/CRLF 转换提示。

## P5

- Outcome: **BLOCKED**。
- 已完成运行前检查：当前 worktree 已预留 slot 18，前端端口 8099、后端端口 48099 均空闲；本机 MySQL 23306 和 Redis 26379 正在监听。
- 已补齐专用真实 E2E 合同：旧 `team-leader-workbench-real-flow.e2e.js` 只覆盖“提交后再做 FIFO”，不能证明本任务的提交即初始分配；现已新增本任务专用脚本，静态合同和 Node 语法检查通过。
- 专用脚本会断言一线提交载荷携带精确 O1 `activeOrderId`、版本 1 `FRONTLINE_SELECTED` 全量分配、组长列表红色待调整、组长改配 O2 后版本 2 快照和两阶段审计，并检查页面错误、控制台错误及目标接口 HTTP 失败。
- 缺少已确认的可写本机测试租户、一线账号、生产组长账号及密码，以及任务自有 O1/O2、生产任务与工艺、员工、设备、记录本、电子签名、组长角色和人员范围。
- 影响：无法执行“一线选择 O1 超量提交 -> 组长列表 O1 红色 -> 组长改配 O2”的真实写入型 Playwright E2E；因此尚不满足融合 `int_main` 的前置条件。
- 禁止替代：不得使用 `芋道源码/admin`、mock、API-only、静态合同或直接 SQL 冒充真实 E2E 通过。

## Final Verdict

- Outcome: P1 RED passed; P2 backend GREEN passed; P3 frontend contract FAIL; P4 frontend implementation FAIL
- Blocker: P3/P4 的 7 个静态测试、类型检查和差异格式检查均通过，但生产组长列表与分配快照仍会把缺失的正式数组静默降级为空数组，当前不能放行。并行编译基线的 21 个正式类尚未进入 `int_main` Git 基线，后续 Maven 或融合验证仍须按相同门禁临时补入并清理，禁止纳入本任务交付。

## P4 第二次独立复核（2026-08-14）

- Outcome: **PASS**。本结论取代上述 P3/P4 的历史 FAIL；上一轮阻塞项已修订并重新验证。
- 正式合同完整性 — PASS：`ProcessPoolTimelineEventVO.reportAllocations` 已改为必需数组，列表模板和超量汇总直接消费该正式投影；不再把字段缺失解释成“无分配/无超量”。
- 禁止 fallback — PASS：生产页面已删除 `row.reportAllocations || []`、`event.reportAllocations || []`、`snapshot.lines || []`、`preview.lines || []`，负向静态合同也已覆盖这四种降级。
- 缺正式投影显式失败 — PASS：列表缺 `reportAllocations` 时，必需类型合同和直接 `reduce` 会立即失败；订单级 `needsAdjustment/overageQuantity` 缺失或不合法时显式抛错。当前快照或 FIFO 预览缺 `lines` 时，直接 `map` 会失败，并由调用方显示“报工分配加载失败”“FIFO 自动分配失败”或保存/刷新错误，不会继续以空分配表操作。
- 正式状态来源 — PASS：列表与分配弹窗仅消费后端 `needsAdjustment/overageQuantity`，未使用未分配量、订单总量或前端推断决定红色状态；已删除前端 `prefillSelectedOrderAllocation` 初始分配伪造路径。
- 后续改配能力 — PASS：组长分配弹窗仍加载其它活跃订单，并通过正式保存/确认接口提交改配；本轮修订未移除该路径。
- 异常审查 — PASS：本次 P4 相关加载、FIFO 预览、保存及版本冲突路径均有明确错误反馈；未发现新增空 `catch`、默认成功或目标字段的兼容降级。

### 第二次复核命令证据

- `node tests\e2e\frontline-production-active-order-picker-static.spec.cjs` — PASS。
- `node tests\e2e\frontline-production-active-order-submit-attribution-static.spec.cjs` — PASS。
- `node tests\e2e\frontline-production-submit-payload-detail-static.spec.cjs` — PASS。
- `node tests\e2e\team-leader-report-allocation-static.spec.cjs` — PASS。
- `node tests\e2e\team-leader-report-allocation-clear-static.spec.cjs` — PASS，退出码 0。
- `node tests\e2e\team-leader-report-overage-highlight-static.spec.cjs` — PASS。
- `node tests\e2e\team-leader-report-shared-allocation-static.spec.cjs` — PASS。
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` — PASS，退出码 0。
- `git diff --check` — PASS，退出码 0。

## Final Verdict（第二次复核）

- Outcome: P1 RED passed; P2 backend GREEN passed; P3 frontend contract passed; P4 frontend implementation passed; P5 blocked.
- Blocker: P3/P4 无阻塞。此前正式投影数组被 `|| []` 静默降级的问题已消除，并由负向静态合同、类型检查和源码独立审查共同验证。
- P5 Blocker: 缺少真实写入型 E2E 所需的已确认本机测试租户、账号和任务自有业务数据；P5 通过前不融合 `int_main`。
- P5 Harness: 专用真实 E2E 执行合同已补齐并在无前置条件时按预期 BLOCKED；不再复用旧 FIFO 流程冒充本任务验证。

## P5 独立验证复核（2026-08-15）

- P5 Outcome: **BLOCKED**。专用 E2E harness 的语法、静态合同、行为合同和阻塞探针均通过，但真实浏览器业务闭环没有执行，不能据此宣称 P5 或业务 E2E PASS。
- 当前运行前置：13 项必需 `FAS_*` 环境变量全部缺失；slot 18 注册表精确归属当前 worktree，固定端口为前端 8099、后端 48099，但两端口监听数均为 0。按任务约束未启动服务、未创建 fixture、未写业务数据。
- 独立结论：harness 门禁 **PASS**；真实 Playwright 业务 E2E **BLOCKED**；P5 总体 **BLOCKED**。

### P5 AC1-AC8

- `P5-AC1` — **BLOCKED**：真实一线提交、组长调整、回归、清理与融合前闭环尚未完成。
- `P5-AC2` — **BLOCKED**：没有真实 Playwright 页面路径通过证据；静态/行为合同不得替代该证据。
- `P5-AC3` — **BLOCKED**：8 项定向静态回归通过，但成功业务流的清理证据和融合 `int_main` 证据尚不存在。
- `P5-AC4` — **BLOCKED**：未提供并核验任务自有 O1/O2 fixture，未创建测试数据。
- `P5-AC5` — **BLOCKED**：一线测试账号未在真实页面选择 O1 并提交 Q。
- `P5-AC6` — **BLOCKED**：生产组长未在真实报工管理列表观察到 O1 红色标识。
- `P5-AC7` — **BLOCKED**：生产组长未在真实页面把数量调整到 O2，也没有真实当前分配、审计和红色状态变化证据。
- `P5-AC8` — **BLOCKED**：本轮已记录 harness 与阻塞证据，但当前里程碑要求的真实业务验证尚未完成，因此不能把阻塞探针单独记为里程碑验收通过。

### 独立执行证据

- `node --check tests\e2e\frontline-active-order-submit-allocation-real.e2e.js` — PASS，退出码 0。
- `node tests\e2e\frontline-active-order-submit-allocation-real-static.spec.cjs` — PASS；该合同实际执行无 `FAS_*` 子进程探针，并验证退出码 2、`BLOCKED/TASK_DATA_PREREQUISITE`、目标业务写请求 0、`NOT_REQUIRED` 清理、`cleanupVerified=true`、剩余任务数据 0，随后恢复原证据文件。
- 8 项定向静态回归 — PASS：活跃订单选择、所选订单提交归属、提交明细、组长分配、清空分配、订单级超量红色标识、共享分配合同及专用真实 E2E 合同均退出 0。
- 环境只读复核 — PASS：`FAS_ENV_COUNT=0`；slot 18 唯一匹配当前 worktree、`active=true`、端口固定为 8099/48099；`LISTENER_COUNT=0`。
- 证据保护 — PASS：静态/行为合同运行后，既有 `result.json` 与 `evidence.md` SHA-256 保持为 `C4695AF539EA3D5A3F6ADE84021897F5BF4E7A6A3BA1726AE27B86F5CAA88E53`、`B0E36CB83AE5C58855B4EA08CE72E9643581526D5544B1C0701D51FFA4CFD4DF`；本轮未留下额外 E2E 产物。

### Harness 行为审查

- 状态分类 — PASS：只有显式 `E2EBlockedError` 前置异常进入 BLOCKED；普通业务断言和页面断言进入 FAIL，清理失败不会覆盖更严重的业务 FAIL。
- 清理门禁 — PASS：识别到 fixture 身份后即使配置校验提前失败也进入外部清理；只有 `status=CLEAN`、`cleanupPerformed=true`、`cleanupVerified=true` 且剩余任务数据为 0 才允许成功。未识别 fixture 且浏览器和写请求均未开始时，允许明确的 `NOT_REQUIRED` 零残留结果。
- 安全与精度 — PASS：manifest 递归拒绝敏感键，结果与证据递归脱敏；Java Long 标识以十进制字符串/`BigInt` 精确处理，不转为 `Number`；脚本不依赖 Node 18 `fetch` 或 `.at(-1)`。
- 租户与运行态 — PASS：固定本机测试租户必须同时匹配 `122/测试租户` 和显式白名单；本阶段只接受当前 worktree 的 8099/48099 及机器可读运行证据，拒绝 8081/48081 和 post-merge 模式。
- 业务断言覆盖 — PASS（仅 harness 能力）：脚本要求提交 payload 精确携带 O1，版本 1 为 `FRONTLINE_SELECTED` 全量初始分配，组长列表显示红色待调整，改配 O2 后产生版本 2 `MANUAL` 快照并核验两阶段只读审计。由于真实运行前置缺失，这些断言本轮未在业务系统中执行。

## P5 真实 E2E 独立复核（2026-08-15）

- P5 Independent Test Outcome: **PASS**。本结论取代上一节历史 BLOCKED；本轮独立重建任务自有 fixture，并通过真实页面重新执行一线 O1 超量提交、组长红色识别和改配 O2，全链路退出码 0。
- 固定边界：仅使用本机测试租户 `122/测试租户`、当前 worktree slot 18 的前端 8099/后端 48099；前端 PID 4880，后端按同一任务 Jar 重启后 PID 36136，运行源码 revision、工作树指纹和 Jar SHA-256 均通过 harness 归属校验。
- 任务数据：独立 run `FAS-20260814-20260814225404-31632`；O1 activeOrderId 87、计划量 6，O2 activeOrderId 88、计划量 20，提交量 10。账号、角色、权限、签名、路线、人员范围和订单均由任务自有 fixture 创建，不使用 admin 账号执行业务页面。
- 真实结果：一线提交生成事件 228；初始版本 1 为 `FRONTLINE_SELECTED`、O1=10、超量 4；生产组长列表直接断言 O1 标签含 `el-tag--danger` 且显示“待调整 4”；页面改配后版本 2 为 O1=6、O2=4，合计 10、未分配 0、两订单超量均为 0，红色待调整标识消失，审计共 3 条并包含版本 1 初始基线及版本 2 的 O1/O2 手工变更。
- 目标写请求严格为 2 个：一线正式提交和组长分配确认。`pageErrors`、目标请求失败、目标 HTTP 错误和目标 console error 均为 0；记录到的其它 `ERR_ABORTED` 均为页面切换时取消的非目标请求或外部资源，不影响目标控件和业务断言。
- 清理结论：E2E `finally` 外部清理删除 67 行，返回 `CLEAN`、`cleanupPerformed=true`、`cleanupVerified=true`、`remainingTaskDataCount=0`；测试结束后再次独立运行同一精确 cleanup，删除行数为 0 且仍为 `CLEAN/0`，证明无任务数据残留。

### P5 AC1-AC8 最终复核

- `P5-AC1` — **PASS**：真实一线提交、组长调整、定向回归、零残留清理和融合前运行门禁均通过。
- `P5-AC2` — **PASS**：真实 Playwright 由两个独立非 admin 账号完成页面登录、选择、提交、查看和调整；未以静态合同或 API-only 替代页面路径。
- `P5-AC3` — **PASS（融合前口径）**：8 项定向静态回归全部退出 0，harness/fixture 合同通过，真实业务结果与二次 `CLEAN/0` 清理通过。本轮未执行、也未宣称 Git 融合；按当前 Git 授权仅证明已满足融合前测试门禁。
- `P5-AC4` — **PASS**：任务自有 O1/O2 经外部 verify 返回 `READY`，权限、任务数据和清理能力均为 true；O1 计划 6 小于提交量 10，O2 计划 20 足以承接改配。
- `P5-AC5` — **PASS**：一线账号在真实页面明确选择 O1，并通过正式提交接口成功提交 10，事件 ID 为 228。
- `P5-AC6` — **PASS**：生产组长真实页面直接断言 O1 初始分配 10、正式超量 4、danger 红色标签和“待调整 4”。
- `P5-AC7` — **PASS**：生产组长通过页面改配为 O1=6/O2=4；版本 2、总量守恒、红色状态消失及 3 条审计均通过只读后置核验。
- `P5-AC8` — **PASS**：本节记录了独立执行、结果、截图审查、回归和清理证据。`execution-log.md` 当前末节仍保留早期构建阻塞的历史描述，主 Agent 在推进机器状态前应由其所有者补记本轮 PASS；tester 按边界未修改该文件。

### 独立执行命令与证据

- `node --check tests\e2e\frontline-active-order-submit-allocation-real.e2e.js` — PASS。
- `node tests\e2e\frontline-active-order-submit-allocation-real-static.spec.cjs` — PASS；覆盖普通业务断言为 FAIL、明确前置为 BLOCKED、早期清理、CLEAN/0 成功硬门禁、递归脱敏和 Long ID 精度。
- `python -X utf8 ..\doc\tasks\20260814-frontline-active-order-submit-allocation-docs\fas_fixture_orchestrator.py --self-test` — PASS。
- `node tests\e2e\frontline-active-order-submit-allocation-fixture-static.spec.cjs` — PASS。
- 8 项相关静态回归 — PASS，`STATIC_TOTAL=8 / STATIC_FAILED=0`。
- 专用真实 E2E — PASS，退出码 0；`result.json.status=PASS`、事件 228、目标业务写请求 2、目标链路错误 0、版本 2 总分配 10、审计 3、清理 `CLEAN/0`。
- 独立二次 cleanup — PASS，`status=CLEAN`、`cleanupVerified=true`、`remainingTaskDataCount=0`、`deletedRowCount=0`。
- 证据脱敏扫描 — PASS；manifest/result 无未允许的敏感键，三项口令字段均为 `[REDACTED]`。

### 截图审查

- `initial-overage-red.png` 为本轮事件的初始分配弹窗，显示 O1 计划量 6、分配量 10、池总量/已分配均为 10。弹窗覆盖了后方列表标签，因此截图本身没有完整露出红色标签；红色状态由截图前同一真实 DOM 行上的订单标签文本、`el-tag--danger` class 和“待调整 4”三项直接断言证明。
- `after-manual-reallocation.png` 显示组长报工管理页刷新成功及“分配已保存”；最终 O1/O2 数量、红色状态消失和版本审计由同轮页面 DOM 与只读后置核验共同证明。

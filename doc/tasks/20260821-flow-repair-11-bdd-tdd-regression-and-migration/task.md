# 流程修复 11：BDD/TDD、回归测试与历史迁移总方案

## 任务目标

基于当前代码、测试、产品规则以及流程修复 1-10 的设计证据，形成活跃订单到批次放行全链路的需求追踪、BDD/TDD、分层回归和历史迁移总方案，并交付流程11可独立拥有的只读历史分类与可回滚迁移计划模块。流程11不修改生产业务状态、不写数据库、不替代流程1-10的状态所有者。

## 里程碑

- [x] M1：建立独立任务目录并记录范围、门禁和用户意图。
- [x] M2：完成现有代码、测试、产品规则和流程修复 1-10 的只读审计。
- [x] M3：完成目标态、根因、接口/数据/状态、幂等、追溯和跨线程契约设计。
- [x] M4：完成需求追踪矩阵、BDD、严格 TDD、分层回归和历史迁移/回滚计划。
- [x] M5：完成文档结构与一致性验证并记录结论。
- [x] M6：同步流程修复 06 的 Tx-A 原子回滚、无失败 receipt 和流程 6 BATCH_* 状态 owner 合同。
- [x] M7：同步流程 04/05/06/07/09/10 最新合同，固定四材料、逐工序损耗、流程 7 Tx-C 映射、流程 6 四个 BATCH_* 状态、多入口凭证和合同已冻结/代码未落地的 Go/No-Go 口径。

- [x] M12：在独立 worktree 以 BDD/TDD 交付五类历史迁移分类器和只读回滚计划生成器；不执行数据库迁移。
- [x] M13：完成流程11 Python runner、`py_compile`、`pytest` 和 runtime guard 复验；记录 task-owned 提交及主线融合证据。
- [x] M14：恢复发布版/保存版 QA DTO 的设备选项类型契约，Maven 生产源码编译和 `MesFrontlinePqcContextServiceTest` 定向回归通过。
- [x] M15：完成 task-owned 边界复核、主工作树 runtime guard 和受保护 fast-forward-only 融合尝试。
- [x] M16：在干净 Flow11 集成 worktree 合入已提交的 `int_main` 前后端代码，补齐被全局 ignore 错误排除的 BPM/ERP 编译源和运行时合同测试，完成全模块编译并推送远端 `int_main`。
- [x] M20：接管流程 8 提供的全 MES 回归工件，完成 152 条 failure/error 的逐条分类、owner 矩阵和环境/工具阻断通知；不修改流程 8 或其它业务代码。

## 预期验证

- 任务目录包含 `task.md`、`development-plan.md`、`test-plan.md`、`execution-log.md`、`verification-report.md`。
- 文档覆盖目标态、当前代码事实、根因、修改边界、接口/数据/状态设计、BDD、RED/GREEN/REGRESSION、blocker、迁移/回滚及流程修复 1-10 的接口契约。
- 当前流程固定四个独立必填材料节点：来料检报告、灭菌报告、成品检报告、成品检记录；旧三项资料仅允许归入 BLOCKED_LEGACY 历史迁移分类。
- 需求追踪矩阵覆盖后端、合同、前端、真实 Playwright E2E 和历史迁移边界。
- 流程11 runner 实际覆盖历史矩阵、分类计数、批次 ID 唯一性、人工批准门禁和回滚范围；生产历史数据 dry-run 仍需授权后执行。
- 明确禁止 mock、API-only、直接 SQL、默认成功及任何未获授权的 fallback。
- 明确 Tx-A 失败返回 BACKFILL_ATOMIC_ROLLBACK、不提交 receipt，流程 6 仅消费成功 BACKFILL_SUCCEEDED receipt。
- 明确流程 5 逐工序 REQUIRED/NO_LOSS/BLOCKED、订单 receipt SUCCESS/NOT_REQUIRED 及 hasActualLoss/lossQuantity/lossReportStatus；流程 6 BATCH_PROVISIONING/BATCH_PROVISIONING_RETRYABLE/BATCH_PROVISIONING_BLOCKED/BATCH_READY；流程 7 Tx-C 映射先于材料；流程 9 IndependentBatchPrerequisiteReceipt 先于流程 6。
- 使用无第三方依赖的流程11合同 runner、只读 dry-run fixture 和文档结构检查验证；不运行服务、生产数据库迁移或写入型 E2E。

## 适用经验门禁

- 已读取并应用 `docs/experience-index.md`：活跃订单冻结正式路线、领料单和 PQC 汇总；批记录、过程检验、损耗与放行资料必须使用正式来源；真实 E2E 必须走 Playwright 真实角色路径；历史关系不得运行时猜测。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；以正式来源、明确状态所有者、幂等与可追溯合同为设计主线。
- 是否存在临时补丁或绕过：否。

## Historical Scope Status

completed（流程11专项范围）：流程11迁移分类器、只读 dry-run、12 个 Python 合同场景、BPM/ERP 编译源修复、全模块 Maven 编译、task-owned 文件恢复及本地受保护融合已完成并验证；本轮主线文档提交为 `8401fa800`（包含流程11 `8fe9228b2`），当前 `int_main` 已完成 Flow7/10 定向复验 4/4，并完成 F4/F6/F8 从 `8f4d843ad` 创建的 v6 integration worktree 门禁协调。ERP 定向 JUnit 6/6、BPM 46/46、runner、pytest、py_compile、前端 TS/Node 静态检查、runtime guard 和 diff-check 均通过。主工作树其它 dirty/untracked 文件未被覆盖。全链路仍为 No-Go：流程1-10生产闭环、真实 Playwright E2E、生产历史迁移、人工批准和回滚演练尚未完成。

## 修改边界

- 允许：修改本任务目录 Markdown，以及流程11拥有的无副作用迁移分类器和对应合同测试。
- 禁止：生产业务状态、数据库写入、迁移执行、配置、服务进程、运行态数据、写入型 E2E，以及绕过流程1-10状态所有者的兼容分支。

## Cleanup Keep

## Current Status

状态：ready_for_closeout / No-Go。当前 int_main=27386bbc4 的 MES compile 已进入 24 模块并编译 2929 个 MES 源码，但被并行 dirty 基线/构建产物依赖错误阻断，未进入 Surefire。流程11 runner 12 场景、pytest 12 passed、runtime guard 通过；py_compile 因拒绝写入 script/__pycache__ 失败。现有四材料共享 gate 报告有 1 条 everyReleaseEntryUsesSharedServerGate failure（期望 shared gate=true，实际=false），不可标记 GREEN。流程4/6/7/8/10完整链路、迁移、真实租户和 Playwright 仍缺证据，全链路保持 No-Go。

- doc/tasks/20260821-flow-repair-11-bdd-tdd-regression-and-migration/task.md
- doc/tasks/20260821-flow-repair-11-bdd-tdd-regression-and-migration/development-plan.md
- doc/tasks/20260821-flow-repair-11-bdd-tdd-regression-and-migration/test-plan.md
- doc/tasks/20260821-flow-repair-11-bdd-tdd-regression-and-migration/execution-log.md
- doc/tasks/20260821-flow-repair-11-bdd-tdd-regression-and-migration/verification-report.md
- doc/tasks/20260821-flow-repair-11-bdd-tdd-regression-and-migration/flow8-mes-regression-classification.md

# Execution Log

## 2026-07-27

- User intent: 将测试管理的多个测试节点组织为可独立筛选查看的串行节点串，便于未来按业务链路测试。
- Current behavior audit:
  - 页面和执行模型已有 `SEQUENTIAL` 与 `PARALLEL`。
  - 当前 Runner 默认并发数和领取数为 `1`，但后端没有前置节点依赖。
  - 当前后端会把所有执行项一次性创建为 `PENDING`；前置节点失败后，后续节点仍可能继续领取。
- BDD: 节点串筛选 -> Given 测试管理存在多个节点串；When 用户选择一个节点串；Then 列表只显示该节点串测试项，并显示串内序号。
- BDD: 节点串严格顺序 -> Given 用户选择同一节点串的全部节点并以顺序方式执行；When 后端创建执行项；Then 执行项按串内序号排列且任一时刻只允许最前面的未完成节点被领取。
- BDD: 前置失败停止 -> Given 节点串前置节点执行失败、阻塞或超时；When 后端汇总执行状态；Then 后续待执行节点全部标记为阻塞并说明前置节点未通过。
- BDD: 非节点串兼容 -> Given 测试项不属于节点串；When 用户按现有方式顺序执行或并行执行；Then 原有行为保持不变。
- BDD: 节点串选择确定性 -> Given 用户选择节点串测试项；When 选择中混入另一节点串或独立测试项；Then 后端拒绝创建含义不明确的执行批次并提示按单个节点串执行。
- GREEN: experience-preflight -> PASS，已读取测试管理 schema、前端静态契约、Codex Runner 和测试节点闭环门禁；本阶段只运行静态契约与 H2 单元测试，不操作真实租户数据。

## 2026-07-27 Review-Fix Worker Round 1

- User intent: 修复评审阻塞项，仅补齐完整节点串选择和独立 `SEQUENTIAL` 测试项回归保护；不停止或重启本地服务，不修改本地数据库数据。
- BDD: 完整节点串选择 -> Given 一个节点串已有第 1 和第 2 节点；When 用户只选择第 2 节点发起 `SEQUENTIAL` 执行；Then 后端拒绝该请求并提示必须从第 1 节点连续选择。
- BDD: 独立顺序测试项领取 -> Given 两个不属于节点串的 `SEQUENTIAL` 测试项和容量为 2 的 Runner；When Runner 领取任务；Then 两个独立项都可领取。
- BDD: 独立顺序测试项失败后继续 -> Given 两个不属于节点串的 `SEQUENTIAL` 测试项；When 第一个失败；Then 第二个仍可领取并完成，不会被标记为节点串阻断。
- RED: `mvn -pl yudao-module-system -am "-Dtest=CodexTestExecutionServiceImplTest#startSequentialExecution_rejectsIncompleteNodeChainSelection" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 只选择第 2 节点时未抛出 `ServiceException`。
- RED: `mvn -pl yudao-module-system -am "-Dtest=CodexTestExecutionServiceImplTest#startSequentialExecution_rejectsIncompleteNodeChainSelection,CodexTestRunnerServiceImplTest#claimTasks_independentSequentialCasesUseAvailableCapacity+completeCase_failedIndependentSequentialCaseAllowsRemainingCaseToRun" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 不完整节点串未被拒绝，独立 `SEQUENTIAL` 容量为 2 时只领取 1 项。
- RED: `mvn -pl yudao-module-system -am "-Dtest=CodexTestRunnerServiceImplTest#completeCase_failedIndependentSequentialCaseAllowsRemainingCaseToRun" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 第一个独立测试项失败后后续项被阻断，无法领取。
- RED: `python -X utf8 -m pytest script/tests/test_codex_test_node_chain_migration.py` -> FAIL, 缺少 `node_chain_execution` 执行快照字段的迁移和 H2 测试表契约。
- GREEN: `mvn -pl yudao-module-system -am "-Dtest=CodexTestExecutionServiceImplTest,CodexTestRunnerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 17 tests passed.
- GREEN: `python -X utf8 -m pytest script/tests/test_codex_test_node_chain_migration.py` -> PASS, 2 tests passed.
- Completed work: 节点串执行批次持久化 `nodeChainExecution` 快照；严格领取和前置失败阻断仅作用于该快照为真的节点串执行；选择节点串时必须精确覆盖完整的连续 `1..N` 节点集合。
- Remaining supervisor verification: 构建并使 `48081` 加载包含 `node-chain-options` 路由的后端，然后在确认的测试租户用 Playwright 验证节点串筛选、顺序领取、失败阻断和独立测试项路径。Worker 未停止/重启本地服务，未修改本地数据库数据。

## 2026-07-27 Local Runtime Data Plan

- BDD: 多节点串可见 -> Given 本机 `tenant_id=1` 已有工艺路线 4 项、批记录 6 项、智能排产 4 项；When 应用节点串 schema 并执行任务自有赋值脚本；Then 页面节点串选项显示 3 条不同节点串，数量分别为 4、6、4，且各串序号从 1 连续到 N。
- Data preflight: 仅允许 14 个精确名称目标项，执行前要求 `node_chain_name/node_chain_sort` 全部为空；任一目标缺失、已分配或影响行数不等于 14 时 fail fast。
- Rollback: 对同一批精确名称恢复 `node_chain_name=NULL,node_chain_sort=NULL`，复核影响行数 14；不修改方法、目标、状态或业务数据。

## 2026-07-27 独立后续项只读验证

- User intent: 使用真实 Playwright 浏览器打开 `http://127.0.0.1:8081`，进入 `系统管理 > 测试管理`，只读确认页面标题、`测试项` 页签和 `Runner 状态` 区域可见。
- BDD: 独立后续项被实际执行 -> Given 租户 `tenant_id=1` 的本机系统可登录；When 用户通过真实前端菜单进入测试管理；Then 页面同时显示 `测试管理`、`测试项` 和 `Runner 状态`。
- Scope: 只读查看，不创建、修改、执行或删除任何测试项，不修改其他业务数据。
- GREEN: experience-preflight -> PASS，已读取 `docs/e2e-rules.md`、`docs/login-access.md`、`docs/local-runtime.md`、`docs/worktree-restrictions.md` 和 Playwright skill；使用本机 `int_main` 固定入口 `8081/48081`。
- GREEN: `playwright-cli -s=independent-followup-20260727` 真实页面路径 -> PASS；从首页依次点击 `系统管理`、`测试管理`，最终 URL 为 `http://127.0.0.1:8081/system/codex-test-management`，浏览器标题为 `瑛泰管理系统 - 测试管理`。
- GREEN: 页面可见断言 -> PASS；`测试管理` 可见，`测试项` 页签可见且 `aria-selected=true`，`Runner 状态` 可见，状态文案为 `可用`。
- Data verification: 未点击新增、执行、修改、删除或其他写入动作；本次只发生登录和页面导航，没有修改业务数据。
- Artifact: 检查点通过，按用户要求未生成失败截图；Playwright 会话已关闭。

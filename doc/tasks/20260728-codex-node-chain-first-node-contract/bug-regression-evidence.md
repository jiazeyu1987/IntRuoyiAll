# Bug Regression Evidence

## Bug Summary And Expected Behavior

- Bug: 在 `系统管理 > 测试管理` 中选择节点串第 1 节点执行时，后端误报 `Runner 回写结果不符合结构化契约：节点串必须从第 1 节点开始连续选择`。
- Expected: 同一节点串允许执行从第 1 节点开始的任意连续前缀，例如只执行第 1 节点；但不允许只选第 2 节点或跳号选择。

## Reproduction Command Or Path

- RED command: `mvn.cmd -pl yudao-module-system -am "-Dtest=CodexTestExecutionServiceImplTest#startSequentialExecution_allowsFirstNodePrefixWithoutRequiringWholeChain" "-Dsurefire.failIfNoSpecifiedTests=false" test`。
- Real path: 登录 `http://127.0.0.1:8083` 的 `芋道源码/admin`，进入 `系统管理 > 测试管理`，创建同一节点串第 1/2 节点，只点击第 1 节点所在行的 `执行`。

## Root Cause

- `validateNodeChainSelection` 旧逻辑把“合法连续前缀”误当成“必须选择完整节点串”：它读取完整节点串后要求本次选中的 case id 集合与完整节点串 case id 集合完全相等。
- 因此，只选择第 1 节点会被误判为不完整，并抛出“节点串必须从第 1 节点开始连续选择”。

## Regression Test Added Or Updated

- Added: `CodexTestExecutionServiceImplTest#startSequentialExecution_allowsFirstNodePrefixWithoutRequiringWholeChain`。
- Coverage: 已存在第 1/2/3 节点时，只提交第 1 节点应创建执行批次且只生成一条执行明细。

## RED Command And Expected Failure

- RED: `mvn.cmd -pl yudao-module-system -am "-Dtest=CodexTestExecutionServiceImplTest#startSequentialExecution_allowsFirstNodePrefixWithoutRequiringWholeChain" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL。
- Expected reason: 旧实现抛出 `CODEX_TEST_RESULT_SCHEMA_INVALID`，消息包含 `节点串必须从第 1 节点开始连续选择`。

## GREEN Command And Passing Result

- GREEN: `mvn.cmd -pl yudao-module-system -am "-Dtest=CodexTestExecutionServiceImplTest#startSequentialExecution_allowsFirstNodePrefixWithoutRequiringWholeChain" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。
- GREEN: `mvn.cmd -pl yudao-module-system -am "-Dtest=CodexTestExecutionServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，`Tests run: 8, Failures: 0, Errors: 0, Skipped: 0`。
- GREEN: `node E:\IntRuoyi\doc\tasks\20260728-codex-node-chain-first-node-contract\node-chain-first-node-real.e2e.cjs` -> PASS，执行批次 `28`，Runner session `60`，执行和检查点均 `PASS`。

## Verification

- Verification: 后端目标回归、后端相邻回归、前端节点串静态合同、前端测试管理静态合同、真实 Playwright E2E 均已通过。
- Verification: 真实 E2E 后只读核验 `Codex首节点契约-%` 活跃测试项数量为 `0`。

## Risk And Regression Scope

- Scope: 仅修改测试管理节点串启动前的后端选择契约校验。
- Preserved failures: 混选多个节点串、混入独立测试项、节点串并行执行、节点序号重复、跳过第 1 节点仍失败。
- E2E cleanup: 本任务创建的 `Codex首节点契约-%` 测试项已通过页面清理，活跃行数为 `0`。

## Blockers And Follow-Up Actions

- Non-task blocker: worktree 全量 `yudao-server` package 被无关 MES eDHR VO 编译问题阻塞，未作为本任务范围修复。
- Closeout blocker: 主工作区存在多项无关脏改动，未执行混合提交或推送；本任务状态保持 `ready_for_closeout`。

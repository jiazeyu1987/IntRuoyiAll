# Execution Log

## User Intent

- 用户报告：从第一个节点开始执行测试管理节点串仍报错 `Runner 回写结果不符合结构化契约：节点串必须从第 1 节点开始连续选择`。

## Preconditions Read

- `C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\SKILL.md`
- `C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\references\bug-contract.md`
- `E:\IntRuoyi\docs\task-closeout-rules.md`
- `E:\IntRuoyi\docs\frontend-development.md`
- `E:\IntRuoyi\docs\backend-development.md`
- `E:\IntRuoyi\docs\e2e-rules.md`
- `E:\IntRuoyi\docs\database-rules.md`
- `E:\IntRuoyi\docs\local-runtime.md`
- `E:\IntRuoyi\docs\login-access.md`
- `E:\IntRuoyi\docs\powershell-encoding.md`
- `E:\IntRuoyi\docs\worktree-restrictions.md`

## BDD

- BDD: 从第 1 节点开始连续选择节点串执行 -> Given 测试管理页存在同一节点串且节点序号从 1 开始连续的测试项；When 用户在真实页面选择第 1 节点起的连续节点并点击执行；Then Runner 回写结构化结果通过节点串契约校验，不出现“节点串必须从第 1 节点开始连续选择”错误。

## Evidence

- RED: `mvn.cmd -pl yudao-module-system -am "-Dtest=CodexTestExecutionServiceImplTest#startSequentialExecution_allowsFirstNodePrefixWithoutRequiringWholeChain" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 新增用例只选择第 1 节点时当前实现抛出 `Runner 回写结果不符合结构化契约：节点串必须从第 1 节点开始连续选择`，证明后端把连续前缀误判为必须选择完整节点串。
- GREEN: `mvn.cmd -pl yudao-module-system -am "-Dtest=CodexTestExecutionServiceImplTest#startSequentialExecution_allowsFirstNodePrefixWithoutRequiringWholeChain" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`。
- REGRESSION: `mvn.cmd -pl yudao-module-system -am "-Dtest=CodexTestExecutionServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, `Tests run: 8, Failures: 0, Errors: 0, Skipped: 0`。
- REGRESSION: `node tests/e2e/system-codex-test-node-chain-static.spec.js` -> PASS, Codex test node chain static contract。
- REGRESSION: `node tests/e2e/system-codex-test-management-static.spec.js` -> PASS, exit code 0。
- E2E: `node E:\IntRuoyi\doc\tasks\20260728-codex-node-chain-first-node-contract\node-chain-first-node-real.e2e.cjs` -> PASS, 入口 `http://127.0.0.1:8083`，后端 `http://127.0.0.1:48083/admin-api`，租户/用户 `芋道源码/admin`，执行批次 `28`，Runner session `60`，只执行 `Codex首节点契约-1785213706432-第1节点`，执行状态 `PASS`，检查点状态 `PASS`。
- E2E cleanup: 通过测试管理页面删除本任务残留测试项 `45-50` 以及本轮测试项 `51-52`；只读 DB 核验 `system_codex_test_case WHERE name LIKE "Codex首节点契约-%" AND deleted=0` 返回 `0`。
- Artifact: `E:\IntRuoyi\output\playwright\20260728-codex-node-chain-first-node-contract\summary.json` 记录执行批次 `28`、Runner session `60`、`consoleErrorCount=0`。
- Artifact: `E:\IntRuoyi\output\playwright\20260728-codex-node-chain-first-node-contract\final.png` 保留真实页面最终截图。
- EVIDENCE: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence E:\IntRuoyi\doc\tasks\20260728-codex-node-chain-first-node-contract\bug-regression-evidence.md` -> PASS, `Bug regression evidence is valid.`
- CHECK: `git -C E:\IntRuoyi diff --check -- <task scoped files>` -> PASS；Java 文件仅有仓库换行规则提示。

## Root Cause

- `CodexTestExecutionServiceImpl#validateNodeChainSelection` 旧逻辑读取完整节点串后要求已选 case id 与完整节点串 case id 完全一致，导致“只选择第 1 节点”这种合法连续前缀被误判为不连续。

## Fix

- 后端正式校验改为只检查本次已选节点串序号排序后必须等于 `1..N`，保留多节点串混选、独立测试项混选、非顺序执行、重复序号和跳过第 1 节点的 fail-fast 校验。
- 新增后端回归测试覆盖“存在 1/2/3 节点时只执行第 1 节点应成功”。
- 真实 E2E 脚本按可见 body 行的节点串名称和 `第 1 节点` 定位行级执行按钮，并修正 Element Plus 删除确认按钮为“确定”，避免页面清理超时。

## Runtime Evidence

- Worktree: `D:\IntRuoyiWorktree\20260728-codex-node-chain-first-node-contract`，profile `int_main` slot `2`。
- Frontend: `http://127.0.0.1:8083` HTTP `200`。
- Backend: `http://127.0.0.1:48083/actuator/health` -> `UP`。
- 本轮真实 E2E 创建测试项 id `51`、`52`，执行时只启动首节点 case id `51`，结束后页面清理成功。
- 经验沉淀：已读取 `project-experience-consolidation`；本次经验点已由 `docs\e2e-rules.md` 的 Element Plus 表格定位、Codex Runner 自动测试和页面清理证据规则覆盖，未新增长期经验文档。

## int_main Fusion Evidence

- PRECHECK: `8081` 监听进程为 `E:\IntRuoyi\IntRuoyiFronted` Vite `env.local`；`48081` 监听进程 PID `46196` 属于 `E:\IntRuoyi` int_main 后端运行态，repo-root 指向 `E:\IntRuoyi\IntRuoyiBackend`。
- RUNTIME: `http://127.0.0.1:48081/actuator/health` -> `UP`；运行 Jar `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260728-125850.jar`，进程启动时间 `2026-07-28 12:59:00`，Jar 修改时间 `2026-07-28 12:58:47`。
- JAR CHECK: 运行 Jar 内 `BOOT-INF/lib/yudao-module-system-2026.04-SNAPSHOT.jar` SHA256 为 `19B4D5C401AAE2811A72C5FE01E7C6B8BA70498A43FC588696174829B2CA0FF4`，与本任务修复 Jar `backend-node-chain-prefix-int-main-20260728-125750.jar` 内同模块 SHA256 一致。
- GREEN: `mvn.cmd -pl yudao-module-system -am "-Dtest=CodexTestExecutionServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，`Tests run: 8, Failures: 0, Errors: 0, Skipped: 0`。
- REGRESSION: `node tests/e2e/system-codex-test-node-chain-static.spec.js` -> PASS。
- E2E: `node E:\IntRuoyi\doc\tasks\20260728-codex-node-chain-first-node-contract\node-chain-first-node-real-int-main.e2e.cjs` -> PASS，入口 `http://127.0.0.1:8081`，后端 `http://127.0.0.1:48081/admin-api`，租户/用户 `芋道源码/admin`，执行批次 `29`，Runner session `60`，只执行 `Codex首节点契约-1785215060489-第1节点`，执行状态 `PASS`，检查点状态 `PASS`，console error 数量 `0`。
- E2E cleanup: 通过测试管理页面删除本轮测试项 `53`、`54`；只读 DB 核验 `system_codex_test_case WHERE name LIKE "Codex首节点契约-%" AND deleted=0` 返回 `0`。
- Artifact: `E:\IntRuoyi\output\playwright\20260728-codex-node-chain-first-node-contract-int-main\summary.json` 记录执行批次 `29`、Runner session `60`、`consoleErrorCount=0`。
- Artifact: `E:\IntRuoyi\output\playwright\20260728-codex-node-chain-first-node-contract-int-main\final.png` 保留真实页面最终截图。

## Current Status

- ready_for_closeout：代码修复、回归测试、worktree 真实 E2E、`int_main` 主端口真实 E2E 和任务自有数据清理完成；最终 cleanup/commit/push 仍受主工作区多项无关脏改动边界约束，未混入无关改动。

# Verification Report

## Scope

- 修复 `系统管理 > 测试管理` 节点串从第 1 节点开始执行仍被误判为不连续的问题。
- 验证后端节点串契约、前端静态合同和真实 Playwright + Runner 回写路径。

## Implementation Verified

- `CodexTestExecutionServiceImpl#validateNodeChainSelection` 现在校验已选节点序号为 `1..N` 连续前缀，允许只执行第 1 节点。
- 保留 fail-fast：不同节点串混选、节点串与独立测试项混选、并行执行节点串、节点序号缺失/重复、跳过第 1 节点仍会失败。
- `CodexTestExecutionServiceImplTest#startSequentialExecution_allowsFirstNodePrefixWithoutRequiringWholeChain` 覆盖本次回归。

## Commands

- `mvn.cmd -pl yudao-module-system -am "-Dtest=CodexTestExecutionServiceImplTest#startSequentialExecution_allowsFirstNodePrefixWithoutRequiringWholeChain" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。
- `mvn.cmd -pl yudao-module-system -am "-Dtest=CodexTestExecutionServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，`Tests run: 8, Failures: 0, Errors: 0, Skipped: 0`。
- `node tests/e2e/system-codex-test-node-chain-static.spec.js` -> PASS。
- `node tests/e2e/system-codex-test-management-static.spec.js` -> PASS。
- `node E:\IntRuoyi\doc\tasks\20260728-codex-node-chain-first-node-contract\node-chain-first-node-real.e2e.cjs` -> PASS。
- `node E:\IntRuoyi\doc\tasks\20260728-codex-node-chain-first-node-contract\node-chain-first-node-real-int-main.e2e.cjs` -> PASS。
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence E:\IntRuoyi\doc\tasks\20260728-codex-node-chain-first-node-contract\bug-regression-evidence.md` -> PASS，`Bug regression evidence is valid.`。
- `git -C E:\IntRuoyi diff --check -- <task scoped files>` -> PASS；仅提示 Java 文件下次 Git 触碰会按仓库换行规则从 LF 转 CRLF。

## Real E2E Evidence

- Frontend URL: `http://127.0.0.1:8083`。
- Backend URL: `http://127.0.0.1:48083/admin-api`。
- Login identity: `芋道源码/admin`。
- Test data: `Codex首节点契约-1785213706432-第1节点` and `Codex首节点契约-1785213706432-第2节点`。
- Execution id: `28`。
- Runner session id: `60`。
- Executed case: first node only, case id `51`。
- Final state: execution `PASS`, execution case `PASS`, checkpoint `PASS`。
- Actual checkpoint text: `visible:测试管理; visible:测试项; visible:Runner 状态; url=http://127.0.0.1:8083/system/codex-test-management; identity=芋道源码/admin`。
- Console errors: `0`。
- Artifacts: `E:\IntRuoyi\output\playwright\20260728-codex-node-chain-first-node-contract\summary.json` and `E:\IntRuoyi\output\playwright\20260728-codex-node-chain-first-node-contract\final.png`。

## Cleanup Verification

- Prior failed E2E residual cases `45-50` were deleted through the real test management page.
- Final run cases `51-52` were deleted through the real test management page.
- Read-only DB check: `system_codex_test_case WHERE name LIKE "Codex首节点契约-%" AND deleted=0` -> `0` active rows.

## int_main Fusion Verification

- Source workspace: `E:\IntRuoyi` on `int_main` contains the node-chain prefix validation fix and regression test.
- Runtime frontend: `http://127.0.0.1:8081/` -> HTTP `200`.
- Runtime backend: `http://127.0.0.1:48081/actuator/health` -> `UP`; PID `46196`, repo-root `E:\IntRuoyi\IntRuoyiBackend`.
- Runtime Jar: `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260728-125850.jar`; running Jar timestamp is earlier than process start, so the process is using a stable copied runtime Jar.
- Module hash: running Jar nested `yudao-module-system-2026.04-SNAPSHOT.jar` SHA256 `19B4D5C401AAE2811A72C5FE01E7C6B8BA70498A43FC588696174829B2CA0FF4`, matching the task-fixed int_main Jar module hash.
- Main-port E2E: `http://127.0.0.1:8081` + `http://127.0.0.1:48081/admin-api` created task-owned cases `53/54`, executed only case `53`, execution id `29`, Runner session `60`, execution/case/checkpoint all `PASS`, console errors `0`.
- Main-port cleanup: cases `53/54` were deleted through the test management page; final active `Codex首节点契约-%` cases in DB = `0`.
- Main-port artifact: `E:\IntRuoyi\output\playwright\20260728-codex-node-chain-first-node-contract-int-main\summary.json` and `final.png`.

## Known Non-Task Blockers

- Worktree full `yudao-server` package remains blocked by unrelated MES compile errors around missing `getAssistUserId()` / `setAssistUserId()` on eDHR VO classes; this is outside the node-chain contract fix.
- Main workspace has unrelated dirty files from other tasks. This report does not claim final commit/push closeout.

## Experience Consolidation

- 已按 `project-experience-consolidation` 检查长期经验归宿；现有 `docs\e2e-rules.md` 已覆盖 Element Plus 表格可见行定位、测试管理 Runner 前置和页面清理证据要求，本次不新建长期经验文档。

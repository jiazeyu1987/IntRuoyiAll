# Bug Regression Evidence

## Bug Summary

`codex-test-run-monitor-runtime` 的未提交状态新增了测试项 `project` 必填校验，但共享测试夹具 `buildCaseReq` 未设置合法项目，导致 Case 与 Execution 回归测试在进入目标行为前失败。

## Expected Behavior

测试夹具应构造符合当前生产校验契约的合法测试项，使回归测试继续验证创建、更新、执行和 Runner 行为，而不是因缺少 `project` 提前失败。

## Reproduction

RED: `mvn.cmd -pl yudao-module-system -am "-Dtest=CodexTestRunnerServiceImplTest,CodexTestRunnerControllerTest,CodexTestExecutionServiceImplTest,CodexTestCaseServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，12 tests 中 6 errors，均为“测试项项目必须是 智能排产、文控 或 批记录”。

## Root Cause

生产代码已经要求 `CodexTestCaseSaveReqVO.project` 属于正式项目集合，但 `CodexTestCaseServiceImplTest.buildCaseReq` 仍沿用旧夹具，没有设置 `project`。Execution 测试复用该夹具，因此同步失败。

## Regression Fix

更新共享测试夹具，显式设置 `project = 智能排产`；不修改生产校验，不引入 fallback 或默认成功。

## Verification

GREEN: 同一 Maven 命令 -> PASS，12 tests / 0 failures / 0 errors / 0 skipped。

## Risk And Scope

- 仅修改测试夹具，使其符合已存在的生产契约。
- 覆盖 Case 创建/更新、Execution 启动和 Runner Controller/Service。
- 未放宽项目校验，也未改变运行时业务行为。

## Blockers And Follow-up

- 无当前回归阻塞。
- worktree 合并后仍需在 `int_main` 上运行合并回归。

## Merge Integration Regression

### Bug Summary

合入 work-order 与 route 相关分支后，后端通用 `parseCandidateSourceNames` 被专用附件解析重构误删，同时前端 `ProRouteFlowConfigApi` 出现三组重复附件接口属性。

### Expected Behavior

- 通用表单绑定与持久化快照继续使用通用候选名称解析契约。
- 批记录附件负责人使用独立的规范化解析契约。
- API 对象中每个接口属性只定义一次。

### Reproduction

RED: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellLinkServiceImplTest,MesProBatchRecordCellLinkSchemaTest,MesProRouteFlowConfigServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，编译提示两处找不到 `parseCandidateSourceNames`。

RED: `pnpm ts:check` -> FAIL，`flowconfig.ts` 三处 `TS1117`，对象属性名称重复。

### Root Cause

- 一个独立 runtime worktree 将附件候选名称解析从通用方法中拆出时删除了仍被其他路线配置路径调用的通用方法。
- route attachment 分支与主线基线都已包含相同 API 方法，自动合并保留了两份定义。

### Regression Fix

- 恢复通用 `parseCandidateSourceNames(Object)`，同时保留 `parseBatchRecordAttachmentCandidateSourceNames(Object)` 专用规范化逻辑。
- 删除第二组重复的附件负责人 API 方法。

### Verification

GREEN: 同一 Maven 命令 -> PASS，38 tests / 0 failures / 0 errors。

GREEN: `node tests/e2e/mes/batch-record-cell-link-static.spec.js`、真实 E2E 脚本语法检查和 `pnpm ts:check` -> PASS。

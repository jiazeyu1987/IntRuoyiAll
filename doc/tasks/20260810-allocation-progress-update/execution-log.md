# Execution Log

## User Intent

- 用户要求：不管是 FIFO 自动分配还是手动分配给订单，只要分配满了，对应订单生产进度就要更新。
- 用户补充：分配数量允许为 0 或空，空就是 0。

## BDD Scenarios

- BDD: 满额分配更新生产进度 -> Given 活跃订单当前工序目标数量为 10 且尚未满额 When FIFO 自动分配或手动确认累计分配达到 10 Then 活跃订单生产进度显示为 100%。
- BDD: 空或 0 分配数量合法 -> Given 分配弹窗中某一行分配数量为空或 0 When 用户确认分配 Then 系统按 0 处理该行，不提示“必须为正整数”，且仍校验总分配量与剩余量。

## Evidence

- Preflight: 已读取 `bug-regression-fix-loop` 技能和 `references/bug-contract.md`。
- Preflight: 已读取任务、前端、后端、数据库、E2E、PowerShell 编码与技术栈规则；相关经验门禁记录在 task.md。
- Root cause: `MesReportAllocationCommandService.aggregateDesired` 把 null/0 分配数量当成非法值；`save` 在当前分配与请求一致时直接返回，未触发 `completionService.reconcileAffectedAllocations` 补同步生产进度。
- Change: null/0 分配行按 0 跳过，负数仍失败；当前分配与请求一致且已有分配行时仍调用订单工序完成进度重算，不新建分配版本。
- E2E Preflight: 已读取 Playwright 技能、E2E、登录、本地运行、worktree、PowerShell 编码和任务收尾规则。
- E2E Preflight: 本机 `npx` 可用；前端 `http://127.0.0.1:8081/` 返回 HTTP 200；后端 `http://127.0.0.1:48081/actuator/health` 返回 UP。
- E2E Runtime: 当前 48081 后端运行 Jar 早于本次修复；真实 E2E 前需按本地运行门禁把本次服务类加载进本机后端，且不记录命令行中的数据库凭据。

## RED / GREEN

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesReportAllocationCommandServiceTest#shouldTreatNullAndZeroAllocationQuantitiesAsZeroWhenSaving" "-DfailIfNoTests=false" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，旧逻辑在 `aggregateDesired` 抛 `PRO_PROCESS_POOL_REPORT_ALLOCATION_QUANTITY_REQUIRED`。
- GREEN: `mvn -q -pl yudao-module-mes -am "-Dtest=MesReportAllocationCommandServiceTest#shouldTreatNullAndZeroAllocationQuantitiesAsZeroWhenSaving+manualFullAllocationSameAsCurrentMustStillReconcileCompletionProgress+fifoFullAllocationSameAsCurrentMustStillReconcileCompletionProgress" "-DfailIfNoTests=false" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，3 tests / 0 failures / 0 errors。
- REGRESSION: `mvn -q -pl yudao-module-mes "-Dtest=MesReportAllocationCommandServiceTest" "-DfailIfNoTests=false" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。
- STATIC: 残留旧校验文案和 `allocatedQuantity` 正数注解搜索为空。
- E2E SCRIPT CHECK: `node --check tests\\e2e\\team-leader-workbench-real-flow.e2e.js` -> PASS。
- E2E BLOCKED: `TLW_FRONTEND_URL=http://127.0.0.1:8081 TLW_BACKEND_URL=http://127.0.0.1:48081 pnpm e2e:team-leader-workbench:real` -> non-zero；脚本生成 `IntRuoyiFronted/test-results/team-leader-workbench-real-flow/result.json`，状态 `BLOCKED`，原因是缺少真实写入型 E2E 前置条件。
- E2E RERUN 2026-08-10T05:15:04Z: `node --check tests\\e2e\\team-leader-workbench-real-flow.e2e.js` -> PASS；`pnpm e2e:team-leader-workbench:real` -> exit 1 / lifecycle exit 2，`result.json` 状态仍为 `BLOCKED`。
- E2E Missing: `TLW_TENANT`、`TLW_USERNAME`、`TLW_PASSWORD`、`TLW_WORK_ORDER_ID`、`TLW_WORK_ORDER_CODE`、`TLW_TASK_ID`、`TLW_ROUTE_ID`、`TLW_ROUTE_PROCESS_ID`、`TLW_PROCESS_ID`、`TLW_ITEM_ID`、`TLW_EMPLOYEE_PROFILE_ID`、`TLW_DEVICE_ID`、`TLW_RECORDBOOK_ID`、`TLW_SIGNATURE_ID`、`TLW_SIGNATURE_EMPLOYEE_ID`、`TLW_APPROVE_USER_ID`、`TLW_FEEDBACK_CODE`、`TLW_FEEDBACK_TYPE`。

## Blockers

- 真实写入型 E2E 缺少任务自有 `TLW_*` 测试夹具；不能使用默认 admin 基线数据、mock、API-only 或非任务自有业务记录替代。

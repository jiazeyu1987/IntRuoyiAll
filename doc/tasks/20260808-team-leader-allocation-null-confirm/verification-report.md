# Verification Report

## Summary
- 修复生产组长“确认分配”前端提交契约，避免确认接口从可清空/可漂移的筛选态读取 `leaderType`。
- 手工新增分配行不再预填首个活跃订单，必须由用户明确选择正式活跃订单；候选列表只暴露含正式正数 ID 的活跃订单。
- FIFO 预览结果进入可提交状态前会校验返回的 `activeOrderId`，缺少正式 ID 时直接暴露错误，不把 `null` 发送到后端。

## Root Cause
- 确认分配载荷中的 `leaderType` 取自 `queryParams.leaderType`，该字段属于列表筛选状态，可能被重置、清空或与当前页签不同步。
- 手工分配行会自动取 `allocatableActiveOrderOptions[0]?.id ?? 0`，在候选 ID 缺失或非正式时容易形成不明确的默认选择。

## Verification
- RED: `pnpm e2e:team-leader-report-allocation:static` -> FAIL，缺少当前页签 `leaderType` 来源合同。
- GREEN: `pnpm e2e:team-leader-report-allocation:static` -> PASS。
- REGRESSION: `pnpm e2e:team-leader-workbench:static` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `git diff --check` -> PASS。

## User-Requested Verification
- Runtime: `http://127.0.0.1:8081` -> HTTP 200；`http://127.0.0.1:48081/actuator/health` -> HTTP 200，backend status UP。
- Static contract: `pnpm e2e:team-leader-report-allocation:static` -> PASS。
- Adjacent static contract: `pnpm e2e:team-leader-workbench:static` -> PASS。
- Real E2E syntax: `node --check tests\e2e\team-leader-workbench-real-flow.e2e.js` -> PASS。
- Type check: `pnpm ts:check` -> PASS。
- Whitespace check: `git diff --check` -> PASS。
- Full write-path E2E: BLOCKED，因为当前进程没有配置 `TLW_*` 任务自有数据环境变量，缺少可写租户/账号、生产订单、任务、路线工序、员工档案、设备、记录本、签名和反馈单号等正式前置；未用 mock、API-only 或未知真实数据替代。

## Risk
- 未运行真实页面写入型 E2E；当前任务通过静态合同和类型检查覆盖确认分配载荷构造，避免触碰真实业务数据。

# Bug Regression Evidence

## Bug Summary
生产组长在“分配报工/确认分配”弹窗点击确认时，后端返回 `请求参数不正确:不能为空null`。

## Expected Behavior
确认分配写接口必须从当前页签解析正式 `leaderType`，手工分配行必须由用户明确选择正式活跃订单；缺少正式必填值时前端应在请求前 fail-fast。

## Reproduction
RED: `pnpm e2e:team-leader-report-allocation:static` -> FAIL，新增合同证明确认分配仍可能依赖可漂移的筛选态 `leaderType`。

## Root Cause
确认分配提交逻辑使用 `queryParams.leaderType` 作为写接口必填上下文；`queryParams` 是列表筛选状态，不是确认写接口的正式页签上下文。手工分配行还会预填首个候选订单，不利于暴露缺少正式活跃订单 ID 的状态。

## Fix
确认分配、FIFO 预览、详情和列表请求统一从当前页签解析 `leaderType`；手工新增分配行保持未选择，活跃订单候选过滤为含正式正数 ID 的记录，FIFO 返回行也校验 `activeOrderId` 后再进入提交状态。

## Regression Test
Updated `IntRuoyiFronted/tests/e2e/team-leader-report-allocation-static.spec.cjs` to lock:
- current-tab `leaderType` resolver
- FIFO preview using the resolver
- confirm submit using the resolver
- manual active-order options requiring formal positive IDs
- new manual rows starting unselected

## Verification
- GREEN: `pnpm e2e:team-leader-report-allocation:static` -> PASS。
- REGRESSION: `pnpm e2e:team-leader-workbench:static` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `git diff --check` -> PASS。

## Blockers
- 未运行真实页面写入型 E2E；原因是当前任务没有确认测试租户、账号和任务自有写入数据，按项目规则不能用未知真实数据冒险复现。

## Risk And Follow-Up
真实页面写入型 E2E 未运行，避免在未确认测试租户和任务自有数据前触碰业务数据；如需要可后续用已确认测试账号执行完整 Playwright 写入链路。

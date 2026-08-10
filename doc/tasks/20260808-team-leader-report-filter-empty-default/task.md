# 生产组长报工管理默认筛选为空修正

## Task Goal

修正生产组长「报工管理」筛选默认仍选中提交日期的问题。用户确认默认筛选应为空/无，而不是默认选中“提交日期”条件。

## Milestones

- [x] 复核当前筛选初始化与列表请求约束
- [x] 增加 RED 静态合同锁定默认筛选为空
- [x] 实施最小前端修复
- [x] 运行定向验证和类型检查

## Expected Verification

- `node tests/e2e/team-leader-report-default-filter-empty-static.spec.cjs`
- `node tests/e2e/team-leader-report-nearest-submit-date-static.spec.cjs`
- `node tests/e2e/pqc-leader-management-default-submit-date-static.spec.cjs`
- `node tests/e2e/pqc-leader-standard-list-template-static.spec.js`
- `node tests/e2e/mes-process-pool-team-leader-static.spec.js`
- `node tests/e2e/production-leader-function-tabs-static.spec.js`
- `node tests/e2e/team-leader-workbench-static.spec.cjs`
- `node tests/e2e/production-leader-active-order-pool-tab-static.spec.js`
- `pnpm ts:check`

## Current Status

completed - 已按“默认无筛选”口径修正；可见筛选 state 保持为空，列表内部仍保留后端必填提交日期查询，查询动作不再强制用户添加可见提交日期；cleanup 已完成。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；只调整筛选默认 UI state，不吞接口错误。
- `是否从根因和长期维护角度解决`：是；已区分内部列表默认查询日期、用户可见筛选条件、以及查询 handler 对可见条件的要求。
- `是否存在临时补丁或绕过`：否；不通过 CSS 隐藏筛选、不造假数据。

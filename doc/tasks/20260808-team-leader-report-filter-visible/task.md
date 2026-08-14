# 生产组长报工管理默认筛选可见修复

## Task Goal

修复生产组长工作台「报工管理」多条件筛选首屏显示“暂无筛选条件”的问题。默认提交日期是正式必填条件，必须在筛选 UI 中可见，而不是只存在于隐藏 query 参数。

## Milestones

- [x] 复核多条件筛选组件空态渲染逻辑
- [x] 增加 RED 静态合同，锁定默认提交日期筛选 Tab 可见
- [x] 实施最小前端修复
- [x] 运行定向验证和类型检查

## Expected Verification

- `node tests/e2e/team-leader-report-default-submit-date-visible-static.spec.cjs`
- `node tests/e2e/team-leader-report-nearest-submit-date-static.spec.cjs`
- `node tests/e2e/pqc-leader-management-default-submit-date-static.spec.cjs`
- `pnpm ts:check`

## Current Status

completed - 已补齐默认提交日期筛选的可见条件和已应用状态；清理预览/应用通过，目标合同、相邻合同、类型检查均通过。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；只补齐正式默认筛选状态，不造假数据、不吞接口错误。
- `是否从根因和长期维护角度解决`：是；根因是默认提交日期 query 与多条件筛选 state 初始化/同步不一致。
- `是否存在临时补丁或绕过`：否；不改数据库、不隐藏筛选组件、不用 CSS 遮挡空态。

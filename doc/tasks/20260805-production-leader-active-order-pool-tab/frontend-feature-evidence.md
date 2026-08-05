# Frontend Feature Evidence

## Feature Goal

在生产组长页面新增“活跃订单池”功能 Tab，使用统一标准列表展示全部活跃订单，并通过对话框新增活跃订单。

## Non-goals

- 不修改后端 API、数据库、权限或主导航路由。
- 不修改 PQC 组长页面行为。
- 不引入 mock、fallback 或兼容分支。

## Requirements

- AC1：生产组长功能 Tab 包含“活跃订单池”。
- AC2：活跃订单池使用 `UnifiedListTemplate` 展示 `getTeamLeaderActiveOrderList` 返回的全部数据。
- AC3：模板 actions 区域包含“新增活跃订单”按钮。
- AC4：新增对话框调用 `addTeamLeaderActiveOrder`，成功后关闭并刷新列表。
- AC5：列表保留正式 `removeTeamLeaderActiveOrder` 移出能力。
- AC6：原班组配置模块不再重复展示活跃订单维护卡片。

## UI Entry Points And Owned Files

- Entry: `/mes/pro/process-pool/production-leader`
- Component: `IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue`
- Tests: `IntRuoyiFronted/tests/e2e/production-leader-active-order-pool-tab-static.spec.js`

## API Contracts And Data States

- List: `getTeamLeaderActiveOrderList()`
- Create: `addTeamLeaderActiveOrder(payload)`
- Remove: `removeTeamLeaderActiveOrder(payload)`
- Loading: 统一列表 loading 状态。
- Empty: 统一列表空态展示。
- Error: 请求失败通过现有错误解析和 `ElMessage.error` 明确暴露。

## BDD Scenarios

- Given 生产组长进入页面，When 点击活跃订单池 Tab，Then 标准列表显示全部正式活跃订单。
- Given 活跃订单池 Tab 已打开，When 点击新增并提交合法数据，Then 正式加入接口成功且列表刷新。
- Given 列表存在活跃订单，When 确认移出，Then 正式移出接口成功且列表刷新。

## RED

- Command: pending
- Expected failure: 当前生产功能 Tab 缺少“活跃订单池”，维护区仍位于班组配置且未使用 `UnifiedListTemplate`。

## GREEN

- Command: pending
- Result: pending

## Responsive And Accessibility

- 新增按钮使用 Element Plus 按钮并保留可见文本。
- 对话框表单字段保留明确标签。
- 标准列表保持现有统一模板的响应式、空态和 loading 行为。

## E2E Or Component Verification

- 聚焦静态合同覆盖 Tab、统一模板、actions、新增对话框、正式 API 和配置模块去重。
- 相邻生产组长模块合同与 TypeScript 检查作为回归。

## Blockers

- 暂无。


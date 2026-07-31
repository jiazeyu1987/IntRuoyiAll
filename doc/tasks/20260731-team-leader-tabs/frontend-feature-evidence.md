# Frontend Feature Evidence

## Feature Goal

将班组长工作台的生产组长与 PQC 组长功能边界明确拆开；生产组长保留现状，PQC 组长先显示占位内容。

## Non-goals

- 不实现 PQC 组长真实业务功能。
- 不修改后端接口、权限码、菜单、数据库或生产组长现有操作。
- 不新增 mock 数据或成功返回。

## Acceptance Criteria

- `AC-1`：页面有“生产组长”和“PQC 组长”两个一级页签。
- `AC-2`：生产组长页签显示当前提交看板、异常上报、班组维护功能。
- `AC-3`：PQC 组长页签只显示明确的占位内容，不显示生产组长操作区。
- `AC-4`：PQC 组长页签不会触发生产组长提交列表查询。

## Entry Points And Owned Files

- Route: `/mes/pro/process-pool/team-leader`
- Permission: `mes:pro-process-pool-team-leader:query`
- Component: `src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue`
- Static contract: `tests/e2e/mes-process-pool-team-leader-static.spec.js`

## API And Data States

- Production leader tab keeps the existing team leader API wrappers.
- PQC leader tab is an explicit empty implementation state and makes no production submission query.
- Existing backend error handling remains unchanged.

## BDD Scenarios

- `BDD: 生产组长页签保留当前功能 -> Given 用户选择生产组长 / When 页面渲染 / Then 当前工作台功能可见。`
- `BDD: PQC 组长页签占位 -> Given 用户选择 PQC 组长 / When 页面渲染 / Then 只显示占位信息。`

## RED / GREEN

待执行并记录在 `execution-log.md`。

## Responsive / Accessibility / Error / Permission Checks

- 两个一级页签使用现有 Element Plus 页签模式。
- PQC 占位内容不依赖接口或 mock 数据，避免空白页误判为加载成功。
- 现有工作台路由查询权限保持不变。
- 生产组长页签原有加载错误提示和权限行为保持不变。

## E2E Path

优先使用现有前端静态契约；若本地运行态和登录前置可用，再通过真实页面访问同一路由验证页签切换。

## Blockers

工作区已有其他任务未提交改动；不得混入本任务实现提交，也不得回滚。

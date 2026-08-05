# Feature

## Feature Goal

移除生产人员档案页的独立“操作追溯”列表，追溯信息由已有表单日志能力展示。

## Non-Goals

- 不修改后端 API。
- 不新增 mock、placeholder 或 fallback。
- 不改变人员档案新增、关联、禁用、重置密码等业务动作。

## Acceptance

- AC1: 人员管理/生产人员档案页不再渲染“操作追溯”标题。
- AC2: 页面不再渲染 `data-team-leader-personnel-audit-list` 独立表格。
- AC3: 前端不再在生产人员档案页加载 `employeeAuditRows` 专用数据源。
- AC4: 既有表单日志入口和文案仍保留在系统中。

## BDD:

- BDD: 生产人员档案不再显示独立操作追溯列表 -> Given 生产组长打开人员管理/生产人员档案, When 页面加载完成, Then 页面只显示人员维护表单和人员列表，不再渲染独立“操作追溯”表格。
- BDD: 追溯入口归属表单日志 -> Given 用户需要查看人员档案相关操作历史, When 查看审计追溯, Then 通过已有表单日志能力承载，不在人员档案页重复维护独立列表。

## RED:

- 待执行。

## GREEN:

- 待执行。

## Verification

- 待执行静态合同和 TypeScript 检查。

## Blockers

- 暂无。

# Frontend Feature Evidence

## Feature Goal

- 在生产人员档案页签新增“新增人员”按钮，并将正式工关联与临时工录入两块新增内容迁移到按钮打开的弹框中。

## Non-goals

- 不修改生产人员接口、权限、后端数据结构、签名密码规则或员工列表查询契约。
- 不引入 fallback、默认成功、mock 数据或静默错误处理。

## Requirements And Acceptance

- A1：页面列表工具栏附近显示“新增人员”按钮。
- A2：点击“新增人员”后打开弹框，弹框内显示“搜索选择正式工”和“手动录入临时工”。
- A3：生产人员档案主页面不再内联显示这两块新增表单内容。
- A4：原有“关联正式工”“新增临时工”方法、loading 状态和错误提示保持不变。

## UI Entry Points

- Route/component: `IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue`
- Tab: `生产人员档案`
- Test scope: `IntRuoyiFronted/tests/e2e/production-personnel-add-dialog-static.spec.cjs`

## API Contracts And Data States

- Existing formal employee candidate search and personnel create/link APIs remain unchanged.
- Existing list loading, enabled filter, pagination, and table operations remain unchanged.

## BDD Scenarios

- BDD: 新增人员弹框入口 -> Given 生产组长打开生产人员档案页签 When 点击页面列表上方的“新增人员”按钮 Then 弹出对话框，并在弹框内显示“搜索选择正式工”和“手动录入临时工”两块内容。
- BDD: 页面内联新增区块移除 -> Given 生产人员档案页签已渲染 When 未打开新增人员弹框 Then 主页面不再直接显示“搜索选择正式工”与“手动录入临时工”的红框内容，列表筛选区域保留在页面上。
- BDD: 原有新增动作保留 -> Given 新增人员弹框已打开 When 使用正式工关联或临时工新增按钮 Then 仍调用原有提交方法、loading 状态和输入校验，不改变后端接口契约。

## RED Command

- RED: `node tests/e2e/production-personnel-add-dialog-static.spec.cjs` -> FAIL before implementation because the page lacked `productionPersonnelAddDialogVisible` and the add forms were still inline.

## GREEN Command

- GREEN: `node tests/e2e/production-personnel-add-dialog-static.spec.cjs` -> PASS.
- GREEN: `node tests/e2e/production-personnel-management-static.spec.cjs` -> PASS.
- GREEN: `node tests/e2e/production-personnel-audit-inline-static.spec.cjs` -> PASS.

## Responsive / Accessibility / States

- The dialog uses explicit `productionPersonnelAddDialogVisible` state.
- The “新增人员” button is rendered in the list toolbar extra-filter slot with a stable `data-team-leader-open-personnel-dialog` selector.
- The dialog keeps existing submit handlers and loading state for formal and temporary employee creation.
- Dialog card grid collapses to one column under the existing `max-width: 1180px` media gate.

## E2E Or Component Verification Path

- Primary: task-specific static contract.
- Regression: adjacent production personnel static contract where compatible.

## Blockers

- `pnpm ts:check` is blocked by unrelated concurrent report-list template symbols in `TeamLeaderWorkbenchPage.vue`; personnel-specific static contracts pass.

## Evidence Validator

- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260805-production-personnel-add-dialog/frontend-feature-evidence.md` -> PASS.

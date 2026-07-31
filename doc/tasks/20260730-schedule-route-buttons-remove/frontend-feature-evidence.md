# Frontend Feature Evidence

## Feature Goal

删除排产设置弹窗中黄框标出的两个排产工艺路线导入导出按钮。

## Non-Goals

- 不修改排产策略保存 API。
- 不修改全部数据包导入导出能力。
- 不调整权限、路由、后端接口或运行态端口。

## Requirements And Acceptance

- AC1: “导出排产工艺路线”按钮不再渲染。
- AC2: “导入排产工艺路线”按钮不再渲染。
- AC3: “导出全部数据包”“导入全部数据包”“保存策略”仍渲染。

## UI Entry Points

- `/mes/pro/scheduler-workbench` 排产员工作台的 `排产设置` 弹框。
- Owned component: `IntRuoyiFronted/src/views/mes/pro/scheduler-workbench/index.vue`。
- Owned tests:
  - `IntRuoyiFronted/tests/e2e/mes-pro-scheduler-workbench-route-import-export-static.spec.js`
  - `IntRuoyiFronted/tests/e2e/mes-scheduler-workbench-settings-dialog-static.spec.js`

## API Contracts And Data States

- 本任务只删除前端按钮入口，不改 API contract。

## BDD Scenarios

- BDD: 排产设置隐藏排产工艺路线导入导出按钮 -> Given 用户打开排产设置弹窗 When 查看策略区域底部操作按钮 Then 页面不显示“导出排产工艺路线”和“导入排产工艺路线”。
- BDD: 排产设置保留其它操作按钮 -> Given 用户打开排产设置弹窗 When 查看数据包和策略保存操作 Then 页面仍显示“导出全部数据包”“导入全部数据包”和“保存策略”。

## RED

- RED: `node tests/e2e/mes-pro-scheduler-workbench-route-import-export-static.spec.js` -> FAIL, old `导出排产工艺路线` button still exists.
- RED: `node tests/e2e/mes-scheduler-workbench-settings-dialog-static.spec.js` -> FAIL, old `openRouteConfigImport` entry still exists in settings dialog.

## GREEN

- GREEN: `node tests/e2e/mes-pro-scheduler-workbench-route-import-export-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/mes-scheduler-workbench-settings-dialog-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/mes-pro-scheduler-workbench-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/mes-scheduler-workbench-noise-reduction-static.spec.js` -> PASS.
- GREEN: `pnpm ts:check` -> PASS.

## Responsive Accessibility Loading Empty Error Permission

- 本任务不新增状态流；验证聚焦按钮显示合同，保留现有保存按钮和数据包按钮。

## E2E Or Component Verification

- Static component contract verification used for this UI-entry removal.
- No runtime service, route permission, API, or backend changes were required.

## Blockers And Follow-Up Skills

- None currently.

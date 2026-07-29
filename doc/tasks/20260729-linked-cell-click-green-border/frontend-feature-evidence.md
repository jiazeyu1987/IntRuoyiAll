# Frontend Feature Evidence

## Feature Goal

在批记录填写配置的辅助表单映射模式中，已链接的原表单元格仍可点击；点击后原表单当前单元格和辅助表单被链接格子同步显示绿色边框。

## Non-goals

- 不修改保存接口、`assistRows` 数据结构或责任主体模型。
- 不引入 fallback、mock 数据或兼容分支。
- 不启动本地前后端服务；本次先覆盖静态合同与类型检查。

## Requirements And Acceptance

- 已链接原表单元格不得 disabled。
- 点击已链接原表单元格必须同步选中对应责任主体和辅助表格格子。
- 原表单当前选中格子和辅助表格被链接格子必须显示绿色边框。
- 未链接单元格继续沿用现有“先选辅助格再映射原表单格”的流程。

## UI Entry Points

- `IntRuoyiFronted/src/views/mes/pro/batchrecordformlist/BatchRecordCellRulesConfirmDialog.vue`
- 模式：填写配置 > 辅助表单映射。

## Owned Files

- `IntRuoyiFronted/src/views/mes/pro/batchrecordformlist/BatchRecordCellRulesConfirmDialog.vue`
- `IntRuoyiFronted/tests/e2e/edhr-fill-config-linked-cell-click-green-border-static.spec.js`
- `IntRuoyiFronted/tests/e2e/edhr-visual-fill-config-static.spec.js`

## API Contracts And Data States

- API 合同未变更。
- `assistRows` 仍以 `ASSIST_GRID_U..._R..._C...` rowKey 表达辅助格位置。
- 已链接原表单格子通过 `sourceCellGridAssignmentMap` 找到对应 `rowKey` 和责任主体。

## BDD Scenarios

- BDD: 已链接单元格可再次选择 -> Given 原表单单元格已经映射到辅助表单格子 When 用户点击该已链接原表单单元格 Then 页面必须更新当前选中单元格而不是忽略点击。
- BDD: 原表和辅助表联动绿框 -> Given 用户点击一个已链接原表单单元格 When 该单元格存在辅助表映射 Then 原表单当前选中单元格与辅助表单被链接格子都显示绿色边框。
- BDD: 未链接格子保持原有映射流程 -> Given 用户点击未链接原表单单元格 When 再点击辅助表单格子 Then 仍按现有规则建立映射。

## RED

- RED: `node tests/e2e/edhr-fill-config-linked-cell-click-green-border-static.spec.js` -> FAIL，旧实现仍通过 disabled 禁止点击已链接原表单格子。

## GREEN

- GREEN: `node tests/e2e/edhr-fill-config-linked-cell-click-green-border-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-visual-fill-config-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-fill-config-redbox-hide-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。

## Responsive, Accessibility, Loading, Empty, Error, Permission Checks

- Accessibility：保留原按钮 `aria-pressed`，移除 disabled 后已链接格子可由同一点击入口选择。
- Responsive：未改布局尺寸或断点。
- Loading/empty/error：未改异步请求、空态或错误链路。
- Permission：未改保存权限、读取权限或接口参数。

## E2E Or Component Verification Path

本次先以静态合同覆盖点击链路和绿色边框样式；真实页面路径依赖本地运行态、登录态和任务数据，未作为本窄范围完成门禁。

## Blockers And Follow-up Skills

- 无实现阻塞。

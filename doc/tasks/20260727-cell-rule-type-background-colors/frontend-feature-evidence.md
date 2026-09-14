# Frontend Feature Evidence

## Feature Goal

批记录单元格规则弹窗的预览表格按字段类型显示不同背景色，提升文本、数字、日期、签名、下拉框等规则的视觉区分度。

## Non-Goals

- 不改变后端 cell-rules API 合同。
- 不改变字段类型、控件类型、必填、可填写等业务状态。
- 不新增 mock 数据或绕过真实页面入口。

## Requirements And Acceptance

- AC-1: 有规则的可填写单元格必须按字段类型输出稳定的 CSS 类。
- AC-2: 文本、数字、日期、签名、下拉框必须使用不同背景色。
- AC-3: 选中态、必填态和不可填写态必须继续可辨识。

## UI Entry Points And Owned Files

- Entry: 批记录表单列表 -> 单元格规则弹窗。
- Component: `IntRuoyiFronted/src/views/mes/pro/batchrecordformlist/BatchRecordCellRulesConfirmDialog.vue`
- Static contract: `IntRuoyiFronted/tests/e2e/edhr-cell-rule-type-background-colors-static.spec.js`

## API Contracts And Data States

- 继续使用现有 `BatchRecordReportApi.getCellRules` / `saveCellRules` 数据结构。
- 使用现有 `valueType` 与 `componentFlag` 判断显示类型，不新增后端字段。

## BDD Scenarios

- BDD: cell rule preview colors by field type -> Given 单元格规则弹窗加载出文本、数字、日期、签名、下拉框等可填写规则 When 用户查看只读表单预览 Then 每种字段类型的单元格必须拥有不同的稳定背景色类，选中态和必填态仍可见。

## RED

- RED: `node tests/e2e/edhr-cell-rule-type-background-colors-static.spec.js` -> FAIL, expected reason: 缺少 `resolveCellRuleTypeClass` 统一字段类型背景类解析。

## GREEN

- GREEN: `node tests/e2e/edhr-cell-rule-type-background-colors-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-cell-control-type-switch-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/batch-record-cell-rule-default-fullscreen-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。

## Responsive Accessibility Loading Empty Error Permission Checks

- 仅调整单元格背景样式和类名，保留现有按钮、aria-label、aria-pressed、加载、空态、错误提示和权限入口。

## E2E Or Component Verification Path

- 使用任务专用静态合同锁定样式类和颜色映射。
- 复跑相邻单元格规则控件类型静态合同，确认下拉框类型改动未被破坏。
- 复跑单元格规则默认全屏静态合同，确认近期 Dialog 改动未被破坏。
- 补充真实只读页面验收：fresh Playwright context 登录本机 `http://127.0.0.1:8081`，进入 `批记录表单列表 -> 规则`，读取规则弹窗内可见单元格 computed background color；确认文本 `rgb(239, 246, 255)`、数字 `rgb(236, 253, 243)`，首轮扫描同时命中布尔类型类名。未点击保存规则，未触发 MES 写入请求。截图：`doc/tasks/20260727-cell-rule-type-background-colors/real-ui-cell-rule-colors.png`。

## Blockers And Follow-Up Skills

- 无当前任务阻塞。真实页面验收期间出现一次 `系统异常` toast，已定位为列表辅助填写人规则 GET 某非目标报表返回业务 `code=500`，目标 `cell-rules` 渲染接口正常。

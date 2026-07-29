# Frontend Feature Evidence

## Feature Goal And Non-Goals

- Goal: eDHR 填写辅助模式字段卡片内输入控件高度增加 50%，字段卡片整体高度缩减到当前约 80%，辅助网格普通内容保持 1/2 紧凑字号，字段标题文字从当前大小增大一倍。
- Non-goals: 不修改字段解析、字段值保存、提交、权限、工序切换、填写人切换、`assistRows`、`ASSIST_GRID_U` rowKey 或后端接口。

## Requirements And Acceptance

- R1: 普通辅助填写字段行最小高度从 `74px` 缩减到 `59px`。
- R2: 辅助填写网格卡片最小高度从 `118px` 缩减到 `94px`。
- R3: 单行输入、数字输入、选择框和日期输入统一高度为 `48px`。
- R4: 互斥选项组、多行输入和带单位数字输入在增高后不溢出、不挤压单位。
- R5: 辅助网格每个单元格继承字号为 `50%`，输入/单位约从 `14px` 缩小为 `7px`，校验提示从 `12px` 缩小为 `6px`。
- R6: 辅助网格字段标题从当前 `7.5px` 增大一倍到 `15px`。

## UI Entry Points Routes Components Owned Files

- Entry: eDHR 执行填写页辅助模式。
- Component: `IntRuoyiFronted/src/views/mes/pro/edhr/ExecutionPage.vue`
- Static contract: `IntRuoyiFronted/tests/e2e/edhr-fill-workspace-card-density-static.spec.js`

## API Contracts And Data States

- No API contract changes.
- No state model changes.
- Existing draft values, validation messages, save and submit paths remain unchanged.

## BDD Scenarios

- BDD: 辅助填写卡片密度调整 -> Given 用户打开 eDHR 填写辅助模式 / When 字段卡片以网格方式展示 / Then 每张卡片整体高度缩减到当前约 80%，卡片内输入控件高度提升 50%，字段名称、控件、单位和真实校验错误仍正常展示。
- BDD: 辅助网格单元格字号减半 -> Given 用户打开 eDHR 填写辅助模式并查看辅助网格单元格 / When 单元格展示字段名、输入内容、单位或校验提示 / Then 单元格内文字字号均缩小为当前的 1/2，字段值、校验、保存和提交逻辑不变。
- BDD: 辅助网格标题文字增大一倍 -> Given 用户打开 eDHR 填写辅助模式并查看辅助网格字段卡片 / When 卡片展示字段标题和输入控件 / Then 字段标题文字从当前 `7.5px` 增大一倍到 `15px`，输入内容、单位、校验提示仍保持当前紧凑字号。

## RED

- RED: `node tests/e2e/edhr-fill-workspace-card-density-static.spec.js` -> FAIL，当前普通字段行仍为 `min-height: 74px`，未缩减到 80%。
- RED: `node tests/e2e/edhr-fill-workspace-card-density-static.spec.js` -> FAIL，当前样式缺少辅助网格字段名/输入/单位/校验字号减半规则。
- RED: `node tests/e2e/edhr-fill-workspace-card-density-static.spec.js` -> FAIL，当前辅助网格标题仍为 `7.5px`，未增大到 `15px`。

## GREEN

- GREEN: `node tests/e2e/edhr-fill-workspace-card-density-static.spec.js` -> PASS
- GREEN: `node tests/e2e/edhr-fill-workspace-hide-side-panels-static.spec.js` -> PASS
- GREEN: `node tests/e2e/edhr-assist-fill-mode-static.spec.js` -> PASS
- GREEN: `node tests/e2e/edhr-fill-workspace-static.spec.js` -> PASS
- GREEN: `node tests/e2e/edhr-assist-fill-mode-configured-grid-static.spec.js` -> PASS
- GREEN: `pnpm ts:check` -> PASS

## Responsive Accessibility Loading Empty Error Permission

- Responsive: 使用现有辅助填写列表/网格布局，仅调整卡片和控件 CSS 尺寸。
- Accessibility: 字段名称和真实校验错误保持可见。
- Loading/empty/error: 未修改加载态、空态或错误链路。
- Permission: 未修改保存、提交、切换或只读权限。

## E2E Or Component Verification

- 本轮使用静态合同验证目标样式和相邻辅助模式回归；未启动本地服务或运行真实 Playwright。

## Blockers And Follow-Up Skills

- None. Prior `pnpm ts:check` blocker is resolved in the current baseline.
- 当前工作区仍有无关并发改动；本任务提交需选择性暂存任务文件，避免混入其它任务。

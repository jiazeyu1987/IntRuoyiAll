# Frontend Feature Evidence

## Feature

将真实 `生产填写` 页面匹配两个一线生产报工 HTML 原型，并移除员工手填“上工序输入数量”的前端合同入口。

## Scope

- Owned component: `IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue`
- Focused contracts:
  - `IntRuoyiFronted/src/views/mes/pro/feedback/frontline-template-render.spec.cjs`
  - `IntRuoyiFronted/src/views/mes/pro/feedback/frontline-template-switch.spec.cjs`
  - `IntRuoyiFronted/tests/e2e/edhr-frontline-fill-tabs-static.spec.cjs`
- Protected: database, mock data, seed data, unrelated routes. Backend/template contract changes are covered separately in `backend-api-evidence.md`.

## Acceptance

- Production top area shows only `工序 / 员工 / 主页`.
- Production body shows `完成数量`, read-only `损耗数量`, and seven inline defect controls.
- Defect controls include `密封件划伤 / 装配不到位 / 外观磕碰 / 尺寸超差 / 泄漏 / 压力异常 / 其他不良`.
- Device process shows compact selector for up to three real devices and visible `压力 / 时间` parameter inputs.
- No-device process removes the old empty equipment placeholder and lets the quantity/defect panel fill the body.
- Production page and frontend submit API do not expose `上工序输入数量`, `PREVIOUS_PROCESS_INPUT_QUANTITY`, `previousProcessInputQuantity`, `生产工单`, `生产订单`, statistics, explanatory text, or a defect popup.

## BDD

- BDD: 有设备生产填写 -> Given 当前生产工序绑定 1 到 3 台设备 When 一线员工打开生产填写页 Then 页面顶部只显示工序、员工、主页，主体左侧显示完成数量、只读损耗数量和七类不良明细，右侧显示最多三台设备及其参数输入。
- BDD: 无设备生产填写 -> Given 当前生产工序没有设备 When 一线员工打开生产填写页 Then 页面不显示设备空状态面板，数量和七类不良明细占满主体区域。
- BDD: 损耗数量自动汇总 -> Given 员工调整任一不良类型数量 When 不良数量变化 Then 损耗数量显示七类不良数量合计，员工不需要单独填写损耗数量。
- BDD: 不手填上工序输入数量 -> Given 员工提交生产填写 When 前端构造模板和记录本 payload Then 不包含 `PREVIOUS_PROCESS_INPUT_QUANTITY` 或 `previousProcessInputQuantity`。

## RED

- RED: `node src\views\mes\pro\feedback\frontline-template-render.spec.cjs` -> FAIL, expected reason: old production UI did not include the approved `完成数量`/inline defect/no-device layout.
- RED: `node src\views\mes\pro\feedback\frontline-template-render.spec.cjs` -> FAIL, expected reason: frontend API still exposed previous-process input quantity in template / recordbook contracts.

## GREEN

- GREEN: `node src\views\mes\pro\feedback\frontline-template-render.spec.cjs` -> PASS.
- GREEN: `node src\views\mes\pro\feedback\frontline-template-switch.spec.cjs` -> PASS.
- GREEN: `node tests\e2e\edhr-frontline-fill-tabs-static.spec.cjs` -> PASS.
- GREEN: `pnpm ts:check` -> PASS.

## Verification

- Static contract and TypeScript verification passed for the changed frontend surface.
- Local frontend and backend ports are listening; no authenticated write E2E was executed in this pass.

## API Contracts And Data States

- Existing real process and employee switching remains unchanged.
- Existing real device source remains unchanged and still caps visible production devices to 3.
- Formal production payload fields are now `DEVICE`, `DEVICE_PARAMETERS`, `OUTPUT_QUANTITY`, and `SCRAP_QUANTITY`.
- Frontline recordbook submit API no longer requires `previousProcessInputQuantity`.
- `SCRAP_QUANTITY` is populated from the defect quantity total.
- Defect detail fields are not sent as separate backend fields because the current formal contract has no defect-detail field.
- `PREVIOUS_PROCESS_INPUT_QUANTITY` / `previousProcessInputQuantity` are no longer employee-entered or submitted; no fallback value is invented.

## UI States

- Responsive CSS keeps production controls single-column below 1280px.
- Disabled submit state remains visible when process, employee, or template binding is missing.
- Error paths are not swallowed; formal submit still uses the existing `FrontlineTemplateApi.validatePayload` path.

## Blockers

- Login-authenticated real E2E was not run in this pass. Local frontend/backend were not used for a task-owned write submission in this field-removal pass.

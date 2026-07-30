# Frontend Feature Evidence

## Feature Goal

把一线生产填写和 PQC 填写作为 eDHR 批记录页面级独立页签落地，复用真实前端数据流和现有一线简化填写组件。

## Non-goals

- 不新增后端接口、数据库迁移或菜单 SQL。
- 不引入静态 HTML/PNG 作为运行页面。
- 不改生产报工、记录本、资源池正式后端契约。

## UI Entry Points

- `/mes/pro/feedback/edhr-batch-production-fill`
- `/mes/pro/feedback/edhr-batch-pqc-fill`

## Routes And Components

- `IntRuoyiFronted/src/views/mes/pro/edhr-batch/EdhrBatchRecordTabs.vue`
- `IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchProductionFillPage.vue`
- `IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchPqcFillPage.vue`
- `IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue`
- `IntRuoyiFronted/src/router/modules/remaining.ts`

## API Contracts

- 复用 `ProFeedbackApi.getFrontlineDeviceAccountProcesses`。
- 复用 `ProFeedbackApi.getFrontlineEmployeeCandidates`。
- 复用 `ProFeedbackApi.switchFrontlineActualEmployee`。
- 复用 `FrontlineTemplateApi.validatePayload`。

## Data States

- 工序/员工/上下文缺失：页面显示缺失原因并阻塞提交。
- 员工模板类型与当前页签不一致：页面显示模式不匹配并阻塞提交。
- 设备数量超过 3：生产填写只展示前三个设备卡片。
- 无设备：生产填写展示“本工序无设备，直接填数量”。

## Acceptance

- `生产填写` 必须作为 eDHR 批记录页签存在，并锁定生产一线 UI。
- `PQC填写` 必须作为 eDHR 批记录页签存在，并锁定 PQC 一线 UI。
- 员工切换返回的模板类型不得自动改变当前页签 UI；不一致时必须阻塞提交。
- 生产页不得显示工单或生产订单。
- PQC 页不得显示检验方法、成功/失败结果行或巡检摘要。

## BDD

- BDD: eDHR 页签入口 -> Given 用户进入 eDHR 批记录页签区域 When 查看页签栏 Then 能看到 `批次执行`、`历史批记录`、`生产填写`、`PQC填写`，且四个页签跳转稳定。
- BDD: 生产一线填写 -> Given 用户打开 `生产填写` When 页面渲染 Then 页面只显示工序、员工、主页、数量、最多三个设备参数和提交，不显示工单或生产订单。
- BDD: PQC 一线填写 -> Given 用户打开 `PQC填写` When 页面渲染 Then 页面显示生产订单、工序、员工、主页、可输入检验内容、首检/巡检/末检、检验数量和损耗数量，不显示检验方法、成功/失败或巡检摘要。
- BDD: 固定模板模式 -> Given 员工切换后后端返回模板类型 When 模板类型与当前页签模式不一致 Then 页面显式阻塞提交，不自动切换到另一套 UI。

## RED

- RED: `node tests/e2e/edhr-frontline-fill-tabs-static.spec.cjs` -> FAIL，原因符合预期：`BatchProductionFillPage.vue must exist.`

## GREEN

- GREEN: `node tests/e2e/edhr-frontline-fill-tabs-static.spec.cjs` -> PASS。
- GREEN: `node src/views/mes/pro/feedback/frontline-template-render.spec.cjs` -> PASS。
- GREEN: `node src/views/mes/pro/feedback/frontline-template-switch.spec.cjs` -> PASS。
- GREEN: `node tests/e2e/edhr-batch-execution-unified-list-template-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: Playwright 本机只读打开两个新页签并保存 1920×1080 截图 -> PASS。

## Verification

- RED/GREEN evidence recorded in `execution-log.md`。
- Static contracts passed:
  - `node tests/e2e/edhr-frontline-fill-tabs-static.spec.cjs`
  - `node src/views/mes/pro/feedback/frontline-template-render.spec.cjs`
  - `node src/views/mes/pro/feedback/frontline-template-switch.spec.cjs`
  - `node tests/e2e/edhr-batch-execution-unified-list-template-static.spec.js`
- Type check passed: `pnpm ts:check`。
- Real browser read-only checks passed on local `http://127.0.0.1:8081` with backend health `UP` at `http://127.0.0.1:48081/actuator/health`。
- Screenshot artifacts:
  - `IntRuoyiFronted/output/playwright/20260730-edhr-frontline-fill-tabs/production-fill-1920.png`
  - `IntRuoyiFronted/output/playwright/20260730-edhr-frontline-fill-tabs/pqc-fill-1920.png`

## Accessibility And State Checks

- 顶部卡片使用 button，工序和员工卡片可点击切换。
- 提交缺少正式上下文或模板不匹配时禁用并展示原因。
- 无设备时展示真实空状态，不生成假设备。
- PQC 详细检验字段当前未纳入正式 payload 时直接阻塞，不返回默认成功。

## Blockers

- 无当前阻塞。
- 已知后续边界：PQC 详细检验字段若要正式提交，需要后续补充正式 payload/API 契约；本次按计划不临时改后端、不 mock 成功。

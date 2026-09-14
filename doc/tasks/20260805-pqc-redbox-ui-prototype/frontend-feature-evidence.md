# PQC 检验项 10-Tab 与填写区压缩正式实现证据

## Feature Goal

将 PQC 填写页左侧检验内容区域从纵向展开列表改为正式 tab 交互：最多 10 个检验项以 2 行 x 5 列完整显示，只展开当前检验项详情，并把检验设备、设备编号、接收标准、检验方法统一为触控式信息卡；按最新截图反馈，右侧填写区隐藏黄框内说明文字和签名编号展示，压缩标签为“检验 / 损耗 / 不良”。

## Non-Goals

- 不修改 PQC 后端接口、提交 payload 字段或 QA 规程快照来源。
- 不改变逐件检验弹框、标准弹框、方法弹框的业务数据来源。
- 不扩大为整页重设计，不删除右侧签名状态、签名提交字段或正式提交校验链路。

## Requirements And Acceptance IDs

- AC-PQC-TAB-01：检验项必须作为 tab 渲染，当前只展开一个详情面板。
- AC-PQC-TAB-02：tab 区必须支持 10 个完整显示，采用 5 列固定网格形成两行。
- AC-PQC-TAB-03：每个 tab 必须分别显示项目名称、状态、要求和已填进度，不能依赖省略号隐藏关键状态。
- AC-PQC-TAB-04：设备、设备编号、标准、方法必须保留正式选择/弹框/提交链路，同时视觉上不再裸露原生 select。
- AC-PQC-TAB-05：选中 tab 必须使用黄色背景表达当前项，且不显示旧绿色顶部状态条。
- AC-PQC-TAB-06：正式系统 PQC tab 必须与更新后 HTML 预览保持关键样式一致，要求/已填字段完整显示，不使用省略号隐藏关键状态。
- AC-PQC-TAB-07：正式系统最大化态必须与 HTML 预览的画布、外边距、主体圆角阴影、顶部卡片比例、左右面板间距和右侧控件尺寸保持一致。
- AC-PQC-TAB-08：真实页面不得通过 mock、占位 tab 或改写正式业务数据来伪造 10-tab 视觉；若真实 PQC 数据不足 10 项，必须明确记录为数据前置阻塞。
- AC-PQC-FILL-09：右侧填写区不得显示黄框中的说明文字和签名编号输入；“检验数量 / 损耗数量 / 不良说明”必须压缩为“检验 / 损耗 / 不良”。

## UI Entry Points And Owned Files

- Route：`/mes/pro/feedback/edhr-batch-pqc-fill`
- Component：`IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue`
- Static contract：`IntRuoyiFronted/tests/e2e/pqc-inspection-tabs-layout-static.spec.js`
- Adjacent contract：`IntRuoyiFronted/tests/e2e/pqc-item-equipment-standard-method-static.spec.js`
- Real visual script：`doc/tasks/20260805-pqc-redbox-ui-prototype/pqc-real-visual-alignment.e2e.cjs`

## API Contracts And Data States

- `pqcInspectionItems` 继续来自 `deviceState.selectedProcess?.inspectionItems`。
- `pqcItemSelections` 继续保存每个检验项的 `selectedEquipmentId` 与 `selectedEquipmentNumber`。
- `buildPqcItemResultsPayload()` 继续发送 `selectedEquipmentId`、`selectedEquipmentNumber` 和 `sampleValues`。
- `pqcSignatureId` 状态和提交校验继续保留；本轮只隐藏右侧签名编号输入展示，不改写提交 payload 或业务必填规则。
- 空态仍显示“暂无检验项目”，不新增 mock、默认成功或降级数据。
- 真实视觉复验使用正式本机数据；当前 `11. 光固Ⅱ工序` 只返回 7 个检验项，无法与 10-tab HTML 预览达成肉眼完全一致。

## BDD Scenarios

- BDD: PQC 10-tab 完整显示 -> Given 当前 PQC 任务包含最多 10 个检验项 When 操作员查看检验内容区 Then 页面以 2 行 x 5 列 tab 完整显示每个检验项名称、要求和已填进度。
- BDD: 仅当前检验项展开 -> Given 操作员点击某个检验项 tab When tab 成为当前项 Then 页面只展开该检验项的设备、编号、标准、方法和逐件操作，其他检验项保持 tab 摘要。
- BDD: 正式设备链路保留 -> Given 当前检验项要求选择检验设备 When 操作员点击设备或编号信息卡 Then 页面仍通过正式 select 选择并写入原有 `pqcItemSelections`。
- BDD: 选中 tab 黄色高亮且无绿条 -> Given 检验项 tab 已渲染 When 操作员选中某个检验项 Then 该 tab 使用黄色背景，旧绿色顶部状态条不可见。
- BDD: 正式 PQC tab 与 HTML 预览一致 -> Given 更新后的 HTML 预览已确认 When 正式 PQC 填写页渲染检验项 tab Then 正式页的选中态、状态条隐藏和 tab 内字段完整显示与预览一致。
- BDD: 真实最大化画布视觉对齐 -> Given 操作员点击 PQC 页面最大化 When 页面进入正式最大化态 Then 画布外边距、主体宽度、圆角、阴影、顶部卡片比例、左右面板间距和右侧控件尺寸与 HTML 预览一致。
- BDD: 真实数据不足时阻塞完全一致结论 -> Given HTML 预览展示 10 个 tab When 真实 PQC 任务只返回 7 个检验项 Then 视觉脚本必须记录 `BLOCKED_BY_REAL_DATA`，不得以 mock tab 冒充通过。
- BDD: 右侧黄框内容隐藏且标签压缩 -> Given 操作员查看 PQC 右侧填写区 When 页面渲染数量、损耗和不良输入 Then 不显示说明文字和签名编号输入，字段标签分别显示为“检验”“损耗”“不良”。

## RED

- RED: `node tests/e2e/pqc-inspection-tabs-layout-static.spec.js` -> FAIL, expected reason: old page did not render `data-pqc-active-inspection-panel` and still used vertically expanded item list.
- RED: `node tests/e2e/pqc-inspection-tabs-layout-static.spec.js` -> FAIL, expected reason: formal Vue tab fields did not yet match the updated HTML preview's full visibility rule.
- RED: `node tests/e2e/pqc-inspection-tabs-layout-static.spec.js` -> FAIL, expected reason: formal maximized PQC canvas still used full-screen edge-to-edge layout instead of the preview frame.
- RED: `node tests/e2e/pqc-inspection-tabs-layout-static.spec.js` -> FAIL, expected reason: right-side fill panel still displayed the yellow-box helper/signature UI and long labels.
- BLOCKED: `node doc/tasks/20260805-pqc-redbox-ui-prototype/pqc-real-visual-alignment.e2e.cjs` -> FAIL, expected reason: real local PQC data renders 7 tabs, but the approved HTML preview renders 10 tabs.

## GREEN

- GREEN: `node tests/e2e/pqc-inspection-tabs-layout-static.spec.js` -> PASS，覆盖 10-tab 网格、单一当前项详情、黄色 active 背景、无绿色顶部状态条、tab 字段完整显示、右侧黄框隐藏和短标签文案。
- GREEN: `node tests/e2e/pqc-item-equipment-standard-method-static.spec.js` -> PASS.
- GREEN: `pnpm ts:check` -> PASS.
- GREEN: `git diff --check -- IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue IntRuoyiFronted/tests/e2e/pqc-inspection-tabs-layout-static.spec.js doc/tasks/20260805-pqc-redbox-ui-prototype` -> PASS.

## Responsive And Accessibility Checks

- Responsive：普通后台嵌入态使用更紧凑右侧控件和短标签避免裁切；最大化态使用 `max-width: 1480px`、外边距 26px、主体 padding 24px、左右列 `minmax(760px, 1.72fr) + minmax(390px, 0.78fr)` 和 `28px` 间距对齐 HTML 预览，tab 使用 `repeat(5, minmax(0, 1fr))`，10 个 tab 固定两行。
- Accessibility：tab 使用 `aria-pressed` 表达当前项；设备和编号保留原生 select 并带 `aria-label`；focus-visible 样式保留。
- Loading：未改变页面现有加载链路。
- Empty：无检验项时显示 `data-pqc-empty-inspection` 空态。
- Error：未改变原有正式校验错误；缺设备、缺编号、缺上下文仍由现有 submit 校验抛出。
- Permission：未改变路由、权限或任务快照读取规则。

## E2E Or Component Verification Path

本轮已使用任务专用静态契约验证当前行为，并在本机 `http://127.0.0.1:8081` 运行真实 Playwright 截图脚本。真实截图输出：`output/playwright/20260805-pqc-redbox-ui-prototype/pqc-real-fullscreen-1440.png`，结果 JSON：`output/playwright/20260805-pqc-redbox-ui-prototype/pqc-real-visual-result.json`。

## Blockers And Follow-Up Skills

- 当前真实 PQC 数据不足 10 个检验项，无法与 10-tab HTML 预览达到肉眼完全一致；需用户明确批准后创建任务自有 10 项 PQC 测试数据，或接受“布局/样式一致，tab 数量按真实数据渲染”。
- 当前工作区存在多项非本任务脏改动，按项目 Git 门禁，提交/推送需先隔离或处理并发改动。

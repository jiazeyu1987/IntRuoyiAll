# Execution Log

## User Intent

用户要求：点击“生产订单”的弹框，其弹框大小布局、内部子卡片大小布局，要与一线生产点击“工序”的弹框一致。
用户追加要求：订单卡片里面只显示订单号，其它产品、路线等信息不显示。

## BDD / TDD

- BDD: 生产订单弹框复用一线生产工序弹框布局 -> Given 一线 PQC 顶部存在“生产订单”选择入口 When 点击生产订单打开选择弹框 Then 弹框画布、选项网格和子卡片尺寸布局与一线生产“选工序”弹框一致。
- BDD: 订单卡片只显示订单号 -> Given 一线 PQC 生产订单弹框内存在订单候选卡片 When 候选卡片渲染订单文本 Then 卡片只显示订单号/订单编码，不拼接产品、路线或其它上下文信息。

## Evidence

- Trigger docs read: `docs/task-closeout-rules.md`, `docs/frontend-development.md`, `docs/powershell-encoding.md`, `docs/experience-index.md`.
- Skills read: `frontend-feature-delivery`, `project-experience-consolidation`.
- RED: `node tests\e2e\mes-frontline-pqc-order-picker-production-layout-static.spec.cjs` -> FAIL，PQC 生产订单弹框缺少专用布局类，未复用一线生产工序弹框的大画布与 6 列子卡片布局。
- RED: `node tests\e2e\mes-frontline-pqc-order-picker-production-layout-static.spec.cjs` -> FAIL，`formatActiveOrderLabel` 仍拼接 `productText`、`routeText` 并用 `join(' / ')` 展示产品和路线。
- GREEN: `node tests\e2e\mes-frontline-pqc-order-picker-production-layout-static.spec.cjs` -> PASS。
- REGRESSION: `node tests\e2e\edhr-frontline-production-pixel-parity-static.spec.cjs` -> PASS。
- REGRESSION: `node tests\e2e\edhr-frontline-production-fullscreen-toggle-static.spec.cjs` -> PASS。
- REGRESSION: `node tests\e2e\mes-frontline-pqc-active-order-switching-static.spec.js` -> PASS。
- CHECK: task-owned `git diff --check` -> PASS，仅 CRLF 工作区提示。

## Implementation Notes

- `FrontlineFixedTemplatePanel.vue` 中 PQC picker 在 `activePicker === 'order'` 时增加 `frontline-picker--production-order` 修饰类。
- `frontline-picker--production-order` 的弹框画布、卡片宽度、16:9 比例、6 列选项网格、子卡片尺寸、关闭按钮尺寸均按一线生产 `is-production-mode .frontline-picker` 的布局 token 同步。
- `formatActiveOrderLabel` 改为只返回 `workOrderCode` / `workOrderName` / `订单 ${workOrderId}`，不再拼接产品、路线信息。
- 经验沉淀检查：本次属于一次性局部 UI 对齐，未形成新的通用长期经验；不新建经验文档。

## Blockers

- 当前共享工作区存在大量本任务外未提交改动，未执行提交/推送，避免混入无关任务。

# Frontend Feature Evidence

## Feature Goal And Non-Goals

- Goal: 批记录单元格链接工作台左侧来源支持选择生产工单字段，并将字段编码随链接规则保存。
- Non-goal: 不重做批记录表单设计器、不引入 mock 数据、不改变已有跨表单单元格链接行为。

## Requirements And Acceptance

- 用户要求：左边可以选择生产工单里的字段，批次执行需要从生产工单带入数据。
- Acceptance: 来源类型可切换为“生产工单字段”；字段可选；保存 payload 包含 `sourceType=PRODUCTION_WORK_ORDER`、`sourceFieldCode`、`sourceFieldName`；批次执行来源展示为“生产工单字段 / 字段名”。

## UI Entry Points And Owned Files

- Entry: `IntRuoyiFronted/src/views/mes/pro/batchrecordcelllink/index.vue`
- API: `IntRuoyiFronted/src/api/mes/pro/batchrecordcelllink/index.ts`
- Runtime display: `IntRuoyiFronted/src/views/mes/pro/edhr/ExecutionPage.vue`
- Static contract: `IntRuoyiFronted/tests/e2e/mes/batch-record-cell-link-static.spec.js`

## API Contracts And Data States

- Workbench context reads `sourceFields?: BatchRecordCellLinkSourceFieldVO[]`.
- Rule payload supports `sourceType`, `sourceFieldCode`, `sourceFieldName`.
- Prefill item supports production work order source metadata for runtime display.

## BDD Scenarios

- BDD: Work order source selectable -> Given 批记录单元格链接页面打开 When 用户选择“生产工单字段”来源 Then 左侧展示生产工单字段矩阵并允许选字段。
- BDD: Work order source payload -> Given 已选择生产工单字段和目标单元格 When 保存链接 Then 请求 payload 包含来源类型和字段编码。
- BDD: Runtime source display -> Given 批次执行收到生产工单字段预填项 When 显示自动带入来源 Then 来源文案显示“生产工单字段 / 字段名”。

## RED And GREEN

- RED: `node tests\e2e\mes\batch-record-cell-link-static.spec.js` -> FAIL，缺少 `batch-record-cell-link__source-type-select`、`生产工单字段` 等契约。
- GREEN: `node tests\e2e\mes\batch-record-cell-link-static.spec.js` -> PASS，`batch-record-cell-link static contract passed`。

## Verification

- Responsive/accessibility/loading/empty/error/permission: 未启动真实前端服务，未执行浏览器 E2E；本次以静态契约覆盖入口、按钮、payload、运行态显示路径。
- Type check: 未运行，`IntRuoyiFronted\node_modules` 缺失。

## Blockers

- 无阻塞当前静态契约验证的前端问题。

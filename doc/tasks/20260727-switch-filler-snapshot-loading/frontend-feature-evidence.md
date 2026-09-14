# Frontend Feature Evidence

## Feature

“切换填写人”弹窗从当前执行详情读取 `assistSwitchTasks` 快照生成可选填写人，不再打开弹窗时调用全量批次详情接口。

## Acceptance

用户在 eDHR 执行页切换填写人时，应看到当前执行创建后固定的两个协助填写人候选；若执行详情确实缺少快照，页面保留明确错误提示，不能静默降级。

## BDD

- BDD: 使用执行详情快照渲染切换填写人弹窗 -> Given 执行详情包含 `assistSwitchTasks` When 用户点击“切换填写人” Then 前端从 `execution.value?.assistSwitchTasks` 渲染候选项且不调用 `getEdhrBatchExecution`。

## RED/GREEN

- RED: `node yudao-module-mes\src\test\js\mes-edhr-assist-filler-switch-snapshot-static.spec.cjs` -> FAIL，前端类型和弹窗逻辑仍依赖全量批次详情。
- GREEN: `node tests\e2e\edhr-switch-filler-selectability-static.spec.js` -> PASS，静态 E2E 合同确认另外 2 个填写人可选择，且弹窗读取执行详情快照。

## Verification

- PASS: `pnpm exec eslint src\api\mes\pro\feedback\index.ts src\views\mes\pro\edhr\ExecutionPage.vue --format stylish`
- PASS: `pnpm ts:check`
- PASS: `node tests\e2e\edhr-switch-filler-selectability-static.spec.js`

## Blockers

无 frontend blocker；真实浏览器写入路径未在本轮替代定向静态合同，未使用 mock 或 API-only 作为成功依据。

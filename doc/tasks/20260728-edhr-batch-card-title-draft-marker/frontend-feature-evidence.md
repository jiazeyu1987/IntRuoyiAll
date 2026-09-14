# Frontend Feature Evidence

## Changed Surface

- `IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue`
- `IntRuoyiFronted/tests/e2e/edhr-batch-card-title-draft-marker-static.spec.js`
- 相邻 eDHR 批次详情右侧栏静态合同

## User-Visible Behavior

- 右侧当前工序表单卡片主标题显示表单名称。
- 草稿任务标题追加 ASCII `*`。
- 非草稿任务不追加 `*`。
- 批次执行编号仍保留在页面顶部批次上下文，不作为每张卡片主标题。
- 状态标签、填写人、门禁原因、打开/查看/接管/跳过动作保持原逻辑。

## Verification

- `node tests/e2e/edhr-batch-card-title-draft-marker-static.spec.js` -> PASS
- `node tests/e2e/edhr-batch-companion-forms-right-panel-static.spec.js` -> PASS
- `node tests/e2e/edhr-batch-process-companion-forms-static.spec.js` -> PASS
- `node tests/e2e/edhr-batch-detail-hide-red-box-static.spec.js` -> PASS
- `node tests/e2e/edhr-batch-process-form-card-fillers-static.spec.js` -> PASS
- `pnpm ts:check` -> PASS

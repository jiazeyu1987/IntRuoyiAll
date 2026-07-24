# eDHR 工序复盘左右区域宽度互换执行日志

BDD: 左右栏宽度互换 -> Given 用户打开 eDHR 批次详情页 / When 查看工序复盘主区域 / Then 左侧工序列表使用 156px 宽度，右侧当前工序摘要使用 260px 宽度，中间表单区域保持自适应。

RED: node tests/e2e/edhr-review-width-swap-static.spec.js -> FAIL, 当前仍为左 260px / 右 156px。
GREEN: node tests/e2e/edhr-review-width-swap-static.spec.js -> PASS
GREEN: node tests/e2e/edhr-batch-basic-info-dialog-static.spec.js -> PASS
GREEN: node tests/e2e/edhr-review-summary-right-rail-static.spec.js -> PASS
GREEN: node tests/e2e/edhr-process-form-action-columns-static.spec.js -> PASS
GREEN: node tests/e2e/edhr-pending-task-rail-relocation-static.spec.js -> PASS
GREEN: node --check tests/e2e/edhr-review-width-swap-static.spec.js -> PASS
GREEN: pnpm exec eslint src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue tests/e2e/edhr-review-width-swap-static.spec.js tests/e2e/edhr-batch-basic-info-dialog-static.spec.js tests/e2e/edhr-review-summary-right-rail-static.spec.js tests/e2e/edhr-process-form-action-columns-static.spec.js tests/e2e/edhr-pending-task-rail-relocation-static.spec.js -> PASS
GREEN: python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260707-edhr-review-width-swap --mode preview -> PASS, delete=<none>, blocked=<none>, warnings=<none>

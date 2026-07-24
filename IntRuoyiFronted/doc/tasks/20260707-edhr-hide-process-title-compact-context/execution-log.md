# eDHR 工序栏隐藏标题并压小上下文执行日志

BDD: 工序栏标题隐藏 -> Given 用户打开批次详情页 / When 查看工序栏顶部 / Then 左侧不显示 `工序` 标题，顶部仅保留当前批记录上下文。
BDD: 上下文字号更小 -> Given 当前生产工单号较长 / When 页面渲染工序栏顶部上下文 / Then 工单号和批记录号使用更小字号完整展示。

RED: node tests/e2e/edhr-process-header-compact-context-static.spec.js -> FAIL, 红框位置仍渲染 `edhr-batch-detail__review-subtitle` 工序标题，绿框上下文仍为旧字号。
GREEN: node tests/e2e/edhr-process-header-compact-context-static.spec.js -> PASS
GREEN: node tests/e2e/edhr-process-header-context-static.spec.js -> PASS
GREEN: node tests/e2e/edhr-process-header-context-values-only-static.spec.js -> PASS
GREEN: node tests/e2e/edhr-process-form-action-columns-static.spec.js -> PASS
GREEN: node tests/e2e/edhr-pending-task-rail-relocation-static.spec.js -> PASS
GREEN: node --check tests/e2e/edhr-process-header-compact-context-static.spec.js -> PASS
GREEN: pnpm exec eslint src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue tests/e2e/edhr-process-header-compact-context-static.spec.js tests/e2e/edhr-process-header-context-static.spec.js tests/e2e/edhr-process-form-action-columns-static.spec.js tests/e2e/edhr-process-header-context-values-only-static.spec.js tests/e2e/edhr-pending-task-rail-relocation-static.spec.js -> PASS
GREEN: python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260707-edhr-hide-process-title-compact-context --mode preview -> PASS, delete=<none>, blocked=<none>, warnings=<none>

# eDHR 工序栏上下文左侧铺满执行日志

BDD: 工序栏上下文左侧铺满 -> Given 工序栏顶部不显示 `工序` 标题 / When 查看当前批记录上下文 / Then 工单号和批记录号从左侧开始显示，红框位置不留空。

RED: node tests/e2e/edhr-process-header-left-fill-static.spec.js -> FAIL, 上下文仍为右对齐，红框位置留空。
GREEN: node tests/e2e/edhr-process-header-left-fill-static.spec.js -> PASS
GREEN: node tests/e2e/edhr-process-header-compact-context-static.spec.js -> PASS
GREEN: node tests/e2e/edhr-process-header-context-static.spec.js -> PASS
GREEN: node tests/e2e/edhr-process-header-context-values-only-static.spec.js -> PASS
GREEN: node --check tests/e2e/edhr-process-header-left-fill-static.spec.js -> PASS
GREEN: pnpm exec eslint src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue tests/e2e/edhr-process-header-left-fill-static.spec.js tests/e2e/edhr-process-header-compact-context-static.spec.js tests/e2e/edhr-process-header-context-static.spec.js tests/e2e/edhr-process-header-context-values-only-static.spec.js -> PASS
GREEN: python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260707-edhr-process-context-left-fill --mode preview -> PASS, delete=<none>, blocked=<none>, warnings=<none>

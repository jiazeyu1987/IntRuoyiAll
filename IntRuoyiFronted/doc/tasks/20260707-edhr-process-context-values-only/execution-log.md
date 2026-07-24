# eDHR 工序栏上下文仅显示值执行日志

BDD: 工序栏上下文只显示值 -> Given 用户打开批次详情页 / When 查看工序栏顶部上下文 / Then 只显示当前生产工单号和批记录号，不显示 `生产工单：` 或 `批记录号：` 标签。
BDD: 工序栏上下文完整显示 -> Given 当前生产工单号较长 / When 页面渲染工序栏顶部上下文 / Then 工单号和批记录号允许换行完整显示，不使用 ellipsis 截断。

RED: node tests/e2e/edhr-process-header-context-values-only-static.spec.js -> FAIL, 当前上下文仍包含 `生产工单：` / `批记录号：` 标签并使用 ellipsis 截断。
GREEN: node tests/e2e/edhr-process-header-context-values-only-static.spec.js -> PASS
GREEN: node tests/e2e/edhr-process-header-context-static.spec.js -> PASS
GREEN: node tests/e2e/edhr-pending-task-rail-relocation-static.spec.js -> PASS
GREEN: node tests/e2e/edhr-batch-pending-form-entry-static.spec.js -> PASS
GREEN: node --check tests/e2e/edhr-process-header-context-values-only-static.spec.js -> PASS
GREEN: pnpm exec eslint src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue tests/e2e/edhr-process-header-context-values-only-static.spec.js tests/e2e/edhr-process-header-context-static.spec.js tests/e2e/edhr-pending-task-rail-relocation-static.spec.js tests/e2e/edhr-batch-pending-form-entry-static.spec.js -> PASS
GREEN: python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260707-edhr-process-context-values-only --mode preview -> PASS, delete=<none>, blocked=<none>, warnings=<none>

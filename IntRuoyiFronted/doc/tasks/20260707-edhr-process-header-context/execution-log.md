# eDHR 工序栏顶部上下文显示执行日志

BDD: 工序栏顶部显示批记录上下文 -> Given 用户打开批次详情页 / When 查看工序复盘顶部 / Then 页面在绿色位置展示当前批记录对应的生产工单和批记录号。
BDD: 左侧待处理标题删除 -> Given 批次详情页存在待处理工序 / When 用户查看左侧工序列表 / Then 红框位置不再显示“待处理工序”标题，列表直接展示工序卡片。

RED: node tests/e2e/edhr-process-header-context-static.spec.js -> FAIL, 缺少 edhr-batch-detail__process-context 上下文展示。
GREEN: node tests/e2e/edhr-process-header-context-static.spec.js -> PASS
GREEN: node tests/e2e/edhr-pending-task-rail-relocation-static.spec.js -> PASS
GREEN: node tests/e2e/edhr-batch-pending-form-entry-static.spec.js -> PASS
GREEN: node --check tests/e2e/edhr-process-header-context-static.spec.js -> PASS
GREEN: pnpm exec eslint src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue tests/e2e/edhr-process-header-context-static.spec.js tests/e2e/edhr-pending-task-rail-relocation-static.spec.js tests/e2e/edhr-batch-pending-form-entry-static.spec.js -> PASS
GREEN: python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260707-edhr-process-header-context --mode preview -> PASS

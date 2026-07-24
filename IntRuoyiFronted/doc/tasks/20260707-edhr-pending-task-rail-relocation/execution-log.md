# eDHR 待处理工序信息右侧栏迁移执行日志

BDD: 待处理工序详情右侧展示 -> Given 用户在批次详情页选择一个待处理工序 / When 右侧当前工序摘要栏展示 / Then 表单说明、可填写人、批记录/记录本选择、状态、角色和打开动作在右侧可见并复用原处理函数。
BDD: 左侧待处理工序保持可扫描 -> Given 批次详情页存在多个待处理工序 / When 用户查看左侧工序列表 / Then 待处理卡片只保留序号和工序名，不再堆叠表单详情、可填写人、承载选择和操作按钮。

RED: node tests/e2e/edhr-pending-task-rail-relocation-static.spec.js -> FAIL, 左侧待处理工序卡片仍包含 resolvePendingTaskDescription(task) 等详情内容。
GREEN: node tests/e2e/edhr-pending-task-rail-relocation-static.spec.js -> PASS
GREEN: node tests/e2e/edhr-batch-pending-form-entry-static.spec.js -> PASS
GREEN: node --check tests/e2e/edhr-pending-task-rail-relocation-static.spec.js -> PASS
GREEN: pnpm exec eslint src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue tests/e2e/edhr-pending-task-rail-relocation-static.spec.js tests/e2e/edhr-batch-pending-form-entry-static.spec.js -> PASS
GREEN: python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260707-edhr-pending-task-rail-relocation --mode preview -> PASS

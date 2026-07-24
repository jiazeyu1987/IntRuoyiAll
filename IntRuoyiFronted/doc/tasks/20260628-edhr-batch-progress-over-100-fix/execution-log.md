# 执行日志：eDHR 批次执行完成进度超过 100% 前端修复

- 2026-06-28：收到用户截图，列表页某批次“完成进度”显示 `127%`，创建前端任务包并定位 `BatchExecutionListPage.vue` / `progress.ts`。
- `BDD: 批次完成进度不把特殊节点计入模板进度 -> Given 批次包含必填批记录任务和必填特殊节点 / When 列表页展示完成进度 / Then 进度百分比只由已完成模板必填任务数除以模板必填任务总数得出，不能超过 100%。`
- `BDD: 批次详情任务进度文案与列表口径一致 -> Given 批次详情包含特殊节点 / When 查看“任务进度”描述 / Then 文案只显示模板必填任务完成数与模板必填任务总数。`
- `INSPECT: src/views/mes/pro/edhr-batch/progress.ts -> 当前 resolveBatchRequiredProgress 使用 taskApprovedCount 作为分子。`
- `INSPECT: ruoyi-vue-pro/.../MesProEdhrBatchExecutionServiceImpl.java -> taskApprovedCount 包含 requiredFlag 非 false 的特殊节点 APPROVED/SKIPPED。`
- `结论：前端模板必填任务分母与后端 taskApprovedCount 分子口径不一致，导致进度可超过 100%。`
- `BLOCKER: priority-switch -> 用户于 2026-06-29 切换到更高优先级需求“eDHR 批次下载打印版 PDF”，当前前端进度口径任务暂停。`
- `RESUME: user-continue -> 2026-06-29 用户明确要求“继续”，恢复本任务并继续完成 helper 修复与验证。`
- `RED: node scripts/edhr-batch-required-progress.test.mjs -> FAIL，helper 仍直接依赖 taskApprovedCount，无法阻止特殊节点把模板进度抬到 100% 以上。`
- `GREEN: apply_patch -> PASS，新增 resolveBatchRequiredCompletedCount，列表进度和详情文案统一只统计有模板的必填任务完成数。`
- `GREEN: node scripts/edhr-batch-required-progress.test.mjs -> PASS`
- `GREEN: node tests/e2e/edhr-batch-template-preview-static.spec.js -> PASS`
- `GREEN: node node_modules/eslint/bin/eslint.js src/views/mes/pro/edhr-batch/progress.ts src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue scripts/edhr-batch-required-progress.test.mjs -> PASS`
- `GREEN: python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260628-edhr-batch-progress-over-100-fix --mode preview -> PASS，前端 closeout preview 就绪，仅 task.md / execution-log.md 为默认保留项。`
- `GREEN: python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260628-edhr-batch-progress-over-100-fix\frontend-feature-evidence.md -> PASS`
- `GREEN: python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260628-edhr-batch-progress-over-100-fix\bug-regression-evidence.md -> PASS`

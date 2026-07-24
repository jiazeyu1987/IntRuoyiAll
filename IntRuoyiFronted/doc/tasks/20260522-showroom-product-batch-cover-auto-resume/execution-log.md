# Execution Log: 展厅一键封面 10 分钟后台续跑（前端）

BDD: 批量封面首轮后未完成时必须提示后台自动续跑 -> Given 用户在产品列表触发 `一键封面` / When 后端返回仍有未完成产品的后台续跑任务元数据 / Then 前端必须提示“已开启后台定时检查，每 10 分钟自动续跑，全部完成后自动停止”，并在结果弹窗展示任务编号、任务状态、剩余未完成数量和下一次检查时间。

BDD: 存在未完成后台续跑任务时前端必须暴露后端拒绝错误 -> Given 后端检测到已有活动中的一键封面后台任务 / When 用户再次点击 `一键封面` / Then 前端必须直接展示后端错误，不得静默吞掉或改成成功提示。

RED: `node --test scripts/showroom-admin-batch-cover-auto-resume.test.mjs` -> FAIL，当前 `ShowroomProductBatchGenerateRespVO` 尚未暴露 `taskId / taskStatus / remainingPendingCount / nextCheckAt`，`index.vue` 也还没有“已开启后台定时检查，每 10 分钟自动续跑，全部完成后自动停止”及批量结果弹窗扩展文案。

GREEN: `node --test scripts/showroom-admin-batch-cover-auto-resume.test.mjs scripts/showroom-admin-batch-cover-mode.test.mjs` -> PASS。
GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
GREEN: `node node_modules/eslint/bin/eslint.js src/api/showroom-admin/index.ts src/views/showroom-admin/index.vue scripts/showroom-admin-batch-cover-auto-resume.test.mjs --format stylish` -> PASS。
GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-showroom-product-batch-cover-auto-resume\frontend-feature-evidence.md` -> PASS。
GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260522-showroom-product-batch-cover-auto-resume --mode preview` -> PASS。

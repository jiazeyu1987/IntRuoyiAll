# 执行日志：展厅产品批量封面增加生成模式选择

- BDD: 企宣用户点击批量封面时必须先选择生成模式 -> Given 当前用户拥有 `showroom_publicity` 权限且产品列表出现 `一键生成所有封面` 按钮 / When 用户点击该按钮 / Then 前端必须先提示用户选择 `重新生成所有` 或 `只生成未上传的`，而不是直接按单一路径发起批量请求。
- BDD: 前端必须把用户选择的批量封面模式传给后端 -> Given 用户已在批量封面提示里做出模式选择 / When 前端调用批量封面接口 / Then 请求 payload 必须显式携带本次选择的封面生成模式，并继续沿用当前筛选条件。
- BDD: 用户关闭批量封面模式弹框时不得误发真实请求 -> Given 用户已打开批量封面模式选择弹框 / When 用户点击关闭而不是选择生成模式 / Then 页面不得发出真实 `POST /admin-api/showroom/product/batch-generate-cover-image` 请求。
- RED: `node --test scripts/showroom-admin-batch-cover-mode.test.mjs` -> FAIL，前端 API 类型尚无 `ShowroomProductCoverGenerationMode / skippedExistingCount`，页面也还没有 `ElMessageBox` 二选一模式提示。
- GREEN: `node --test scripts/showroom-admin-batch-cover-mode.test.mjs` -> PASS。
- GREEN: `pnpm exec eslint src/api/showroom-admin/index.ts src/views/showroom-admin/index.vue scripts/showroom-admin-batch-cover-mode.test.mjs --format stylish` -> PASS。
- GREEN: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-batch-cover-mode-parallel-cli\scripts\verify-batch-cover-mode-live-node.cjs` -> PASS，真实页面出现 `重新生成所有 / 只生成未上传的`，且关闭弹框后未发出真实批量封面请求。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-batch-cover-mode-parallel-cli\frontend-feature-evidence.md` -> PASS。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-product-batch-cover-mode-parallel-cli --mode preview` -> PASS，预览仅建议清理任务脚本、截图和证据文件，`task.md / execution-log.md` 保留。
- FACT: `src/api/showroom-admin/index.ts` 当前同时承载公司字段翻译任务的在途改动；本次前端实现虽已验证完成，但无法在不混入并行需求内容的前提下生成纯任务提交。

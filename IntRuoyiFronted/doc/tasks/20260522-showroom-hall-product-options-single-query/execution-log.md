# 执行日志：展柜维护产品改为单次候选查询

BDD: hall mapping dialog should fetch candidate products with one dedicated request -> Given 用户点击 `展柜管理` 中任一展柜的 `维护产品` When 弹窗加载候选产品 Then 前端应调用一次专用候选接口获取真实产品集合，而不是逐页串行拉取 `/showroom/product/page`

RED: `node --test scripts/showroom-admin-product-hall-operability.test.mjs` -> FAIL，修复前映射弹窗仍使用 `ShowroomAdminApi.getProductPage` 和分页循环，未命中新接口断言。

GREEN: `node --test scripts/showroom-admin-product-hall-operability.test.mjs` -> PASS。

GREEN: `pnpm exec eslint src/api/showroom-admin/index.ts src/views/showroom-admin/components/HallProductMappingDialog.vue src/views/showroom-admin/hall/contracts.ts scripts/showroom-admin-product-hall-operability.test.mjs` -> PASS。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-showroom-hall-product-options-single-query\frontend-feature-evidence.md` -> PASS。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260522-showroom-hall-product-options-single-query --mode preview` -> PASS，preview 结果为 `ready`。

GREEN: `D:\ProjectPackage\Int\IntRuoyi\restart-ruoyi.bat` -> PASS，已拉起当前代码对应的 `8081/48081` 本地运行时。

GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli -s=showroom-hall-product-options-single-query open http://127.0.0.1:8081/showroom/hall --headed` + `run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-showroom-hall-product-options-single-query\scripts\verify-showroom-hall-product-options-single-query.mjs` -> PASS，真实点击 `维护产品` 后新增请求仅有一次 `/admin-api/showroom/hall/product-options`，且点击后不再追加 `/admin-api/showroom/product/page`。

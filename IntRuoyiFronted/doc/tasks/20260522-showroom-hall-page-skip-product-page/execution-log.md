# 执行日志：展柜管理页按需跳过产品分页加载

BDD: showroom hall page should not request product page on first load -> Given 用户直接进入 `http://127.0.0.1:8081/showroom/hall` When 后台壳页根据 `activeSection` 加载当前页面数据 Then `hall` 页首屏不应额外请求 `/showroom/product/page`

RED: `node --test scripts/showroom-admin-frontend.test.mjs` -> FAIL，修复前 `loadShowroomAdminData()` 仍无条件执行 `Promise.all([loadProductRows(), loadHallRows()])`，新增断言未通过。

GREEN: `node --test scripts/showroom-admin-frontend.test.mjs` -> PASS。

GREEN: `pnpm exec eslint src/views/showroom-admin/index.vue scripts/showroom-admin-frontend.test.mjs` -> PASS。

GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli -s=showroom-hall-page-skip-product-page open http://127.0.0.1:8081/showroom/hall --headed` + `run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-showroom-hall-page-skip-product-page\scripts\verify-showroom-hall-page-skip-product-page.mjs` -> PASS，真实 `hall` 首屏请求中命中 `/admin-api/showroom/hall/page`，未命中 `/admin-api/showroom/product/page`。

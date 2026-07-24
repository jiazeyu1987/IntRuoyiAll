# 执行记录

BDD: 重复导入真实 Excel 后前端必须展示结果而不是抛异常 -> Given 用户在测试租户已经导入过 `产品资料修改版.xlsx` / When 再次通过展厅产品管理导入同一文件且后端返回成功、跳过、失败统计 / Then 前端必须展示总行数、成功、跳过、失败明细，缺省数组按空数组处理，不得出现 `Cannot read properties of undefined (reading 'length')`。

RED: `node --test scripts/showroom-admin-product-list.test.mjs` -> FAIL，当前缺少导入结果归一化断言，`ShowroomProductImportForm.vue` 直接读取 `result.successProductCodes.length`、`result.skippedProductCodes.length`、`result.failures.length`，运行时遇到缺省数组或 upload 包装形态会抛前端异常。

GREEN: `node --test scripts/showroom-admin-product-list.test.mjs` -> PASS，19 tests passed，导入结果归一化断言通过。

GREEN: `node --test scripts/showroom-admin-product-company-field-layout.test.mjs scripts/showroom-admin-product-list.test.mjs scripts/showroom-admin-version-center.test.mjs scripts/showroom-admin-frontend.test.mjs` -> PASS，49 tests passed。

GREEN: `node tests\e2e\showroom-product-toolbar-layout.spec.js` -> PASS。

GREEN: `pnpm ts:check` with `NODE_OPTIONS=--max-old-space-size=16384` -> PASS。

GREEN: Playwright `芋道源码/admin` readonly at `http://127.0.0.1:18081/showroom/product` -> PASS，产品页显示 `一键在售国家`，新增弹窗显示 `BU`、`在售国家`、`Countries on Sale`，导入弹窗显示文字导入说明且不显示旧语义。

GREEN: Playwright `测试租户/aoteman` import at `http://127.0.0.1:18081/showroom/product` with `D:\ProjectPackage\Int\IntRuoyi\resource\产品资料修改版.xlsx` -> PASS，HTTP 200，`code=0`，`totalRows=164`，`successCount=0`，`skippedCount=164`，`failureCount=0`，前端展示汇总且未出现 `Cannot read properties of undefined`。

REGRESSION: backend log `backend-int-main-showroom-verify-3g.out.log` after import -> PASS，仅出现 `/admin-api/showroom/product/import-excel` start/end，未出现 `product-*-ruoxi.wav`、`SHOWROOM_AUDIO_GENERATION_FAILED`、`Native memory allocation` 或 `OutOfMemoryError`。

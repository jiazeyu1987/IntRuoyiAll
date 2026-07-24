# 执行日志：展厅 Excel 初始化导入

BDD: Excel 初始化导入 -> Given workbook `展厅产品与描述清单.xlsx` contains 166 products, 8 halls, and 15 non-empty companies, When the seed importer runs, Then showroom product and hall data are initialized in the database with fail-fast validation.

BDD: 展厅后台读取正式种子数据 -> Given showroom admin frontend loads `/showroom/company/current`, `/showroom/product/page`, and `/showroom/hall/page`, When the backend switches from in-memory content maps to MyBatis persistence and the initialization seed is loaded, Then the frontend sees the scaffold company state plus 166 products and 8 halls without API errors。

RED: live backend showroom probe -> FAIL，`/admin-api/showroom/product/page` 与 `/admin-api/showroom/hall/page` 返回空数组，`/admin-api/showroom/company/current` 只有空草稿脚手架。

GREEN: `mvn -pl yudao-module-showroom test` -> PASS，31 个 showroom 模块测试通过。

GREEN: `python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_showroom_excel_seed_tooling.py -q` -> PASS，seed 生成脚本输出与 committed SQL 完全一致。

GREEN: `mvn -pl yudao-server -am -DskipTests package` -> PASS，后端可重新打包为包含 showroom 持久化逻辑的新 jar。

GREEN: MySQL schema + seed execution on `127.0.0.1:23306/ruoyi-vue-pro` -> PASS，`showroom_company=15`、`showroom_product=166`、`showroom_product_revision=166`、`showroom_hall=8`、`showroom_hall_product=166`。

GREEN: authenticated `GET /admin-api/showroom/product/page` and `GET /admin-api/showroom/hall/page` -> PASS，分别返回 166 条产品与 8 个展厅。

GREEN: Playwright smoke via home entry -> PASS，真实前端路径进入展厅后台后，在“产品管理”标签看到 `166 个产品`，在“展厅管理”标签看到 `8 个展厅`。

REGRESSION: unauthenticated `GET /admin-api/showroom/display/home` -> PASS for route registration，返回 `401` 而不是 `No static resource`。

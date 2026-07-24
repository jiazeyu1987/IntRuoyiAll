# 执行日志：芋道源码/admin 真实数据产品导入 E2E

BDD: 芋道源码/admin 可进入产品导入入口 -> Given 使用本地芋道源码/admin 登录真实前端 / When 进入展厅产品管理并打开产品导入弹窗 / Then 能看到“相同产品处理”以及“跳过/覆盖”选择。

BDD: 真实 Excel 默认跳过导入不应跳过 product_001 图片变化 -> Given `product_001` 当前显示 `V44` 且封面是旧式导入 URL / When 使用 `产品资料正式版.xlsx` 执行默认跳过导入 / Then `product_001` 发布新版本，封面 URL 变为带内容哈希的导入 URL。

BDD: 空白单元格提示保持保留当前值 -> Given 打开产品 Excel 导入弹窗 / When 查看导入说明 / Then 页面提示“空白单元格会保留当前数据”。

GREEN: 本地前置条件 -> PASS, `8081` 前端与 `48081` 后端均监听成功，`GET http://127.0.0.1:48081/admin-api/system/auth/get-permission-info` 返回 HTTP 200。

GREEN: Playwright real frontend login -> PASS, `http://localhost:8081/login?redirect=/index` 使用 `芋道源码/admin` 登录，登录接口返回 `code=0`。

GREEN: Playwright navigate product management -> PASS, 进入 `http://localhost:8081/showroom/product`，产品管理列表加载成功，导入前 `product_001` 显示 `V44`。

GREEN: Playwright open import dialog -> PASS, 导入弹窗包含“空白单元格会保留当前数据”和“相同产品处理 / 跳过 / 覆盖”。

GREEN: Playwright set real Excel file -> PASS, 使用 `D:\ProjectPackage\Int\IntRuoyi\resource\产品资料正式版.xlsx` 选择文件成功，弹窗显示 `产品资料正式版.xlsx`。

GREEN: Playwright submit real Excel import -> PASS with backend completion evidence, 点击“确定”后后端日志记录 `/admin-api/showroom/product/import-excel` 完成，耗时 `38992 ms`。Playwright 未捕获到响应事件，脚本超时关闭页面后请求显示 `net::ERR_ABORTED`，但后端已完成导入写入。

GREEN: Playwright reload product management -> PASS, 重新进入产品管理页，`product_001` 显示 `V45`。

GREEN: Final API verification -> PASS, `product_001` 当前版本 `revisionNo=45`，`status=PUBLISHED`，`cover_image=/admin-api/infra/file/28/get/showroom/product/cover/20260531/product-product_001-imported-cover-b7a35f69730887ea.png`。

GREEN: Final cover hash verification -> PASS, 当前封面 URL 返回文件 SHA-256 为 `b7a35f69730887ead9da9e7866834635161afa8286783e9fa63dff718769d611`。

OBSERVED: 本地自动发布定时任务日志出现 `Dirty showroom release requires configured auto-publish site key and stage.`，该问题不影响本次导入路径验证，但会影响本地自动发布验证。

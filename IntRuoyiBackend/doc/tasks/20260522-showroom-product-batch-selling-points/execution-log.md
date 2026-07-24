# 执行日志：展厅产品管理增加一键卖点（后端）

BDD: 一键卖点会为当前版本补齐中英文核心卖点 -> Given 当前筛选命中的产品版本缺少中文核心卖点或英文核心卖点 / When 企宣用户启动 `一键卖点` / Then 后端必须基于该产品当前版本的真实字段生成缺失中文卖点，并同步生成缺失英文卖点，已存在语言自动跳过。

BDD: 一键卖点只处理命中的缺口产品 -> Given 当前筛选范围外产品或中英文卖点均已齐全的产品 / When 批量卖点任务执行 / Then 这些产品不得被重写，统计结果必须真实区分命中、跳过、补齐与失败。

BDD: 一键卖点失败必须暴露真实原因 -> Given AI 生成中文卖点失败、英文翻译为空，或产品版本前置条件缺失 / When 后端执行到对应产品 / Then 必须返回具体失败原因，不得写入空卖点、mock 成功结果或 fallback 内容。

RED: `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomProductNarrationRegressionTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 编译期缺少 `generateCoreSellingPoints(...)` 和 `batchGenerateProductSellingPoints(...)`，新回归测试无法通过。

GREEN: `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomProductNarrationRegressionTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS

GREEN: `Invoke-RestMethod` 调用 `POST http://127.0.0.1:48081/admin-api/showroom/product/batch-generate-selling-points` 并使用零命中关键字 `__NO_MATCH_SHOWROOM_PRODUCT_BATCH_SELLING_POINTS__` -> PASS，真实接口返回 `matchedCount=0 / updatedProductCount=0 / failedCount=0`，未修改数据。

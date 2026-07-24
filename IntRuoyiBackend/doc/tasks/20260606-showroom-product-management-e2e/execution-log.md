# 展厅产品管理页签 E2E 验证执行日志

## BDD

- BDD: 产品管理页签加载成功 -> Given 本机后端已创建 `showroom_product_revision_attachment` 表且本机前端运行在 `http://localhost:8081` / When 使用测试租户 `aoteman` 登录并进入展厅产品管理页签 / Then 页面应显示产品管理内容且不出现缺表错误。

## Evidence

- GREEN: `Invoke-WebRequest -UseBasicParsing -Uri http://127.0.0.1:48081/actuator/health` -> PASS，HTTP 200。
- GREEN: `Invoke-WebRequest -UseBasicParsing -Uri http://localhost:8081` -> PASS，HTTP 200，前端 Vite 页面可访问。
- BLOCKED: `npx.cmd --yes --package @playwright/cli playwright-cli -s=showroom-product-management-e2e run-code --filename ruoyi-vue-pro\doc\tasks\20260606-showroom-product-management-e2e\showroom-product-management-e2e.run-code.js --raw` -> FAIL，真实登录测试租户并进入 `/showroom/product` 成功，产品分页接口 HTTP 200 且列表渲染 20 行；点击首行“基础”后 60 秒内未出现编辑弹框。
- CHECK: Playwright 当前页结构化采集 -> PASS，`url=http://localhost:8081/showroom/product`，`rows=20`，`basicButtons=20`，`summary=共 179 条，共 9 页`，`hasMissingTableError=false`。
- BLOCKER: 浏览器控制台 -> `Error: 展柜公司信息缺失，无法设置产品归属`，调用栈为 `buildFallbackProductCompanyOption -> requireYingtaiProductCompany -> applyFixedProductOwner -> openProductEdit`。
- SCREENSHOT: `doc/tasks/20260606-showroom-product-management-e2e/showroom-product-management-current.png`。

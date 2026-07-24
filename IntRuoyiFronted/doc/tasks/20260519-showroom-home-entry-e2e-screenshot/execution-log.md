# 执行日志：展厅首页入口 E2E 截图验证

BDD: 首页进入展厅前台 -> Given 用户使用真实账号登录系统首页 When 用户点击“进入展厅前台” Then 系统进入展厅前台页面且不显示 404。

BDD: 首页进入展厅后台 -> Given 用户使用真实账号登录系统首页 When 用户点击“进入展厅后台” Then 系统进入展厅后台页面且不显示 404。

RED: 待执行 -> FAIL，尚未运行真实浏览器 E2E。

RED: python -X utf8 <inline playwright e2e> -> FAIL，临时 E2E 脚本将 Playwright Python 的 URL 字符串误当成对象读取 `url.path`，未形成业务验证结论。

GREEN: python -X utf8 <inline playwright e2e> -> PASS，使用真实登录路径进入首页，点击“进入展厅前台”到达 `/showroom/display/home` 且未显示系统 404，点击“进入展厅后台”到达 `/showroom-admin/company` 且未显示系统 404。

BLOCKER: 展厅前台数据加载 -> FAIL，页面请求 `admin-api/showroom/display/home` 返回 `No static resource admin-api/showroom/display/home.`，前台页面展示“加载展厅前台数据失败”。

SCREENSHOT: 首页入口 -> `D:/ProjectPackage/Int/IntRuoyi/yudao-ui-admin-vue3/output/playwright/showroom-home-entry-e2e/01-home-entry.png`

SCREENSHOT: 展厅前台 -> `D:/ProjectPackage/Int/IntRuoyi/yudao-ui-admin-vue3/output/playwright/showroom-home-entry-e2e/02-frontstage.png`

SCREENSHOT: 展厅后台 -> `D:/ProjectPackage/Int/IntRuoyi/yudao-ui-admin-vue3/output/playwright/showroom-home-entry-e2e/03-admin.png`

# 任务：展厅首页入口 E2E 截图验证

## 目标

通过真实浏览器用户路径验证首页进入展厅前台与展厅后台的入口，并输出截图供确认。

## 里程碑

- [x] 准备 E2E 验证记录与截图目录
- [x] 使用真实登录路径进入系统首页
- [x] 从首页点击展厅前台入口并截图
- [x] 从首页点击展厅后台入口并截图
- [x] 记录验证结论、截图路径与阻塞项

## 预期验证

- E2E 使用 Playwright 操作前端，不通过直接输入业务页面地址进入展厅。
- 登录后首页可见“数字展厅入口”。
- 点击“进入展厅前台”后不出现 404 页面。
- 点击“进入展厅后台”后不出现 404 页面。
- 关键截图保存到本地输出目录。

## 当前状态

已完成。

## 验证结果

- 首页入口可见，路径来自真实登录后的首页点击操作。
- 点击“进入展厅前台”后进入 `/showroom/display/home`，未出现系统 404 页面。
- 点击“进入展厅后台”后进入 `/showroom-admin/company`，未出现系统 404 页面。
- 前台页面暴露接口缺口：`admin-api/showroom/display/home` 返回 `No static resource admin-api/showroom/display/home.`，导致页面提示“加载展厅前台数据失败”。

## 截图

- 首页入口：`D:/ProjectPackage/Int/IntRuoyi/yudao-ui-admin-vue3/output/playwright/showroom-home-entry-e2e/01-home-entry.png`
- 展厅前台：`D:/ProjectPackage/Int/IntRuoyi/yudao-ui-admin-vue3/output/playwright/showroom-home-entry-e2e/02-frontstage.png`
- 展厅后台：`D:/ProjectPackage/Int/IntRuoyi/yudao-ui-admin-vue3/output/playwright/showroom-home-entry-e2e/03-admin.png`

## 剩余阻塞

- 展厅前台真实数据接口缺失或未被当前后端服务暴露，影响前台内容加载；入口导航本身已验证通过。

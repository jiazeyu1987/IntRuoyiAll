# Execution Log：电子批记录表单宽度自适应真实页面核验

BDD: 登录后电子批记录右侧表单按容器宽度铺满 -> Given 用户通过本机真实登录页进入电子批记录页面并选中一个可预览报表 / When 右侧真实 Jimu 预览加载完成 / Then 表单应在预览容器中按宽度等比铺满，而不是保持窄宽度。

BDD: 页面变窄时表单继续等比缩放 -> Given 右侧真实 Jimu 预览已经显示 / When 浏览器视口缩窄或预览容器宽度减小 / Then 表单仍应保持等比缩放，不出现工具区回退。

GREEN: experience-preflight -> PASS，本次仅执行本机 `http://localhost:8081` 登录预检与只读 Playwright 视觉核验；已命中登录、PowerShell 和前端样式经验门禁，未涉及写入、服务器操作或跨环境验证。

GREEN: `node scripts/preflight/login-preflight.mjs --base-url http://localhost:8081 --tenant 测试租户 --username aoteman --password 111111 --target-path /mes/pro/batch-record-template --target-text 电子批记录` -> PASS，真实登录已进入电子批记录页面。

GREEN: Playwright 宽屏只读核验 -> PASS，视口 `1600x980` 下右侧预览容器约 `796px`，iframe 同步约 `796px`，真实表单已按容器宽度铺满。

GREEN: Playwright 窄屏只读核验 -> PASS，视口 `1120x980` 下右侧预览容器约 `316px`，iframe 与表单同步缩小；当前视觉偏小由三栏固定列宽导致，而非自适应逻辑失效。

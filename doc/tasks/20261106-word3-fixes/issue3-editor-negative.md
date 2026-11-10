# 问题3：普通编辑账号权限负向验收

真实 Playwright UI PASS。使用已创建的 `word1106Editor` 登录任务环境8063，未新增授权或更改任何引用。

1. 受控浏览页面左侧菜单只有个人中心、审批中心、文控中心下的受控浏览；没有文控权限或文件类型维护入口。证据 `output/word1106-e2e/issue1/issue3-editor-menu-scope.png`。
2. 通过浏览器直接导航管理员已确认的 `/dcc/controlled-file/categories`，普通编辑账号显示“抱歉，您访问的页面不存在。”，页面只有“返回首页”按钮。没有新增、添加、保存或提交按钮，无法进入文件类型及审批矩阵维护。证据 `issue3-editor-category-route-denied.png/.txt` 与 `issue3-editor-category-negative-trace.zip`，同上目录。

这是实际前端路由授权表现（未授权页面未注册），并非观察到 HTTP 403。验收未使用 API 请求或 DB 写入，也未访问或恢复问题2已取消的引用。

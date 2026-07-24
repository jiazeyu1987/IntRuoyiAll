# Execution Log: 六路识别页签增加一键清空电子批记录报表按钮

BDD: 六路识别页签暴露清空电子批记录报表入口 -> Given 用户进入 `报表管理 -> 报表设计器 -> 六路识别` 页签 When 页面加载完成 Then 操作区应出现“清空电子批记录报表”按钮，并保留现有 A-F 路线按钮与刷新按钮。
BDD: 用户确认后调用批量删除接口并刷新列表 -> Given 用户点击“清空电子批记录报表”并确认 When 后端成功删除目录下报表 Then 前端应提示删除数量并重新拉取六路识别报表列表。
RED: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\report-management-six-route-page.test.mjs` -> FAIL, 页面缺少 `清空电子批记录报表` 按钮、`handleDeleteAll` 处理函数和 `deleteAllGeneratedReports` API 封装。
GREEN: 同一 node 源码契约测试命令 -> PASS, 2/2 断言通过。
GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm --dir D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 ts:check` -> PASS, 提高 Node 堆后前端类型检查通过。
GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session six-route-clear-verify run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-six-route-tab-clear-electronic-batch-record-reports\scripts\verify-six-route-clear-button.mjs` -> PASS, 真实主租户链路完成两次清空与一次 A 路生成，最终列表 `total = 0`。

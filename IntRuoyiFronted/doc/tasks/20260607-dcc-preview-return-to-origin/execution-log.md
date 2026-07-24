# Execution Log: DCC 受控预览返回来源页

BDD: 受控浏览进入预览后返回浏览页 -> Given 用户从受控浏览页打开预览 / When 点击预览页左上角“返回” / Then 页面返回受控浏览，并保留当时目录/分类/状态 query。

BDD: 我的文件进入预览后返回我的文件 -> Given 用户从我的文件页打开预览 / When 点击“返回” / Then 页面返回我的文件页。

BDD: 审批任务进入预览后返回审批任务 -> Given 用户从审批任务页打开预览 / When 点击“返回” / Then 页面返回审批任务页。

BDD: 详情进入预览后返回详情 -> Given 用户从受控文件详情打开预览 / When 点击“返回” / Then 页面返回当前文件详情页。

BDD: 缺少或非法来源时回详情 -> Given 用户直接访问预览 URL 或 `returnTo` 非法 / When 点击“返回” / Then 页面回退到当前文件详情页，不跳外部地址。

RED: node scripts/dcc-controlled-file-preview-detail-panel.test.mjs -> FAIL, expected preview route builder to carry returnTo and viewer close action to prefer source-route navigation.

GREEN: node scripts/dcc-controlled-file-preview-detail-panel.test.mjs -> PASS, 8 tests.

GREEN: $env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check -> PASS.

GREEN: node doc/tasks/20260607-dcc-preview-return-to-origin/verify-preview-return-to-origin.e2e.mjs -> PASS, browser returned to /dcc/controlled-file/browser?directoryId=906200&categoryId=906101&status=ACTIVE, mine returned to /dcc/controlled-file/mine, approval returned to /dcc/controlled-file/approval-tasks, and detail returned to /dcc/controlled-file/detail/2054545668044046252.

# 执行日志

BDD: 未归档执行记录不弹错误 -> Given eDHR 执行列表包含未生成归档的记录, When 用户进入列表页, Then 页面应显示“未归档”，不弹出“批记录执行归档不存在”。

RED: 代码检查 -> FAIL，`getLatestEdhrExecutionArchive` 未传 `ignoreErrorMessage: true`，后端返回“批记录执行归档不存在”时全局 Axios 会先弹错误消息。

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。

REGRESSION: Playwright 真实页面验证 `http://localhost:8081/mes/pro/feedback/edhr-execution?batchCode=E2E-881MO090863-20260610-104752` -> PASS，`hasArchiveNotExistsToast=false`、`hasUnarchived=true`、`rowCount=4`、`hasArchiveColumn=true`、`hasExecutionCode=true`。截图：`output/playwright/edhr-execution-list-archive-not-exists-toast.png`。

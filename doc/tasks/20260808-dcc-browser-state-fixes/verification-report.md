# Verification Report

## Summary

已完成 DCC 受控浏览状态一致性修复：列表请求失败时清空旧数据并标记失效，筛选标签/URL/本地记忆只在查询成功后提交；预览打开失败会提示原因；分页 jumper Enter 会显式跳转或失败回滚。

## Changes Verified

- 鉴权失败保护：`getList()` 失败时清空 `list/total`、清除 loaded 标记、显示 `列表数据已失效`，并重新抛出错误避免默认成功。
- 筛选状态提交顺序：查询、目录、范围、批量识别筛选和分页均改为请求成功后再同步 URL 与记忆状态。
- 筛选标签回滚：`useTableQuickFilter` 在 reload 失败时恢复上一次已成功应用条件。
- 分页输入：共享 `Pagination` 捕获 jumper Enter，非法页码恢复当前页，合法页码向父组件传递目标 page/limit；受控浏览分页失败时恢复旧页码。
- 预览反馈：文件 ID 缺失或 `window.open` 被拦截时通过 `ElMessage` 明确提示。

## Verification

- `node --check "doc\\tasks\\20260808-dcc-browser-state-fixes\\readonly-real-regression.cjs"` -> PASS。
- `pnpm --dir "IntRuoyiFronted" e2e:dcc:browser-state-consistency:static` -> PASS。
- `pnpm --dir "IntRuoyiFronted" e2e:dcc:browser-tab-return-no-reload:static` -> PASS。
- `pnpm --dir "IntRuoyiFronted" e2e:dcc:browser-version-summary:static` -> PASS。
- `pnpm --dir "IntRuoyiFronted" ts:check` -> PASS。
- `node "doc\\tasks\\20260808-dcc-browser-state-fixes\\readonly-real-regression.cjs"` -> PASS；本机 `芋道源码/admin` 只读验证中，失败查询返回业务 `401`，失败后标签保持 `类别: 其他`，表格行数 `0`，空态显示旧数据已清空，URL 未提交 `市场调研报告`，DCC 写请求数 `0`。
- `validate_bug_regression.py --evidence doc\\tasks\\20260808-dcc-browser-state-fixes\\bug-regression-evidence.md` -> PASS。
- `validate_frontend_feature.py --evidence doc\\tasks\\20260808-dcc-browser-state-fixes\\frontend-feature-evidence.md` -> PASS。
- `node --check "doc\\tasks\\20260808-dcc-browser-state-fixes\\dcc-browser-state-real-e2e.cjs"` -> PASS。
- `node "doc\\tasks\\20260808-dcc-browser-state-fixes\\dcc-browser-state-real-e2e.cjs"` -> PASS；真实 Playwright 只读验证覆盖预览按钮、文件名按钮、分页 jumper Enter、非法页码恢复和会话失效筛选回滚。

## E2E Details

- Preview feedback: 模拟浏览器拦截 `window.open` 后，点击首行 `预览` 与文件名按钮均显示 `预览窗口打开失败，请检查浏览器弹窗拦截设置。`。
- Pagination: 全域列表总数 `15917`，20 条/页末页 `796`；在第 2 页 jumper 输入 `796` 并 Enter 后，URL 为 `pageNo=796`，jumper 值为 `796`，末页可见行数 `17`；输入非法 `797` 后提示 `请输入有效页码` 并恢复 `796`。
- Session failure: 0 QM 当前目录初始 `3` 条，`类别: 其他` 成功返回 `3` 条；移除 token 后查询 `市场调研报告` 返回业务 `401`，标签保持 `类别: 其他`，表格行数 `0`，空态显示 `列表数据已失效` 和旧数据已清空说明，URL 未提交失败类别。
- Target requests: `dccWriteRequests=[]`，`httpErrors=[]`，`consoleErrors=[]`；`登录超时,请重新登录!` 为会话失效场景的预期提示，导航中止和百度统计中止为非目标链路。

## Known Non-Blocking Failure

- `pnpm --dir "IntRuoyiFronted" e2e:dcc:browser-search-usability:static` -> FAIL；该旧静态合同仍要求历史单输入框 `queryParams.keyword`，当前页面已使用 `UnifiedListTemplate/TableMultiFilter`，与本次状态一致性修复无直接关系。

## Safety

- 未修改后端、数据库、权限、业务数据或下载确认流程。
- 真实回归只读执行，DCC `POST/PUT/PATCH/DELETE` 请求数为 `0`。
- 未引入 fallback、降级、吞异常、mock 数据或默认成功。
- Cleanup apply 已完成，仅保留 `task.md`、`execution-log.md` 和 `verification-report.md`。

- Added E2E cleanup apply 已完成，仅保留 `task.md`、`execution-log.md` 和 `verification-report.md`。

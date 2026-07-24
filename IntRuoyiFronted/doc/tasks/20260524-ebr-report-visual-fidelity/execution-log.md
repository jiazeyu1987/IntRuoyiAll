# 执行日志：电子批记录报表视觉保真优化（前端验证）

## BDD

- BDD: 真实入口触发清除和生成 -> Given 测试租户用户登录本任务专用前端 `http://localhost:18081` 且前端代理到后端 `http://localhost:18083` / When 用户进入电子批记录页面并依次点击 `清除电子批记录报表` 与 `A 直接 doc` / Then 前端必须通过真实页面入口发起操作，不能使用测试专用控件、mock 成功或静默吞掉失败。
- BDD: 入口缺失必须暴露 -> Given 后端存在清除或生成能力但前端没有用户入口 / When 执行真实路径验证 / Then 必须先修复真实入口或报告阻塞，不能改用接口调用冒充 E2E 成功。

## RED / GREEN

- BDD: 前端 API 不得二次解包已解包响应 -> Given axios wrapper 已经返回后端 `data` payload / When `recognizeFixedRoute` 或 `deleteAllGeneratedReports` 调用完成 / Then 页面应拿到 `importedCount/deletedReportCount` 等真实字段，不能再取 `result.data` 导致 undefined。
- BDD: 识别和清空失败必须暴露 -> Given 后端清空或识别接口失败 / When 用户从 `六路识别` 页点击按钮 / Then 前端必须显示错误消息，不能空 `catch` 静默吞掉失败。
- RED: `node --test scripts\report-management-six-route-page.test.mjs` -> FAIL, `recognizeFixedRoute` 使用 `request.post<{ data: ... }>` 并 `return result.data`，同时 `handleRecognize` / `handleDeleteAll` 为空 `catch`。
- GREEN: `node --test scripts\report-management-six-route-page.test.mjs` -> PASS, API wrapper 返回未二次解包 payload，识别/删除失败用 `resolveErrorMessage` 和 `message.error` 暴露。
- GREEN: `node --test scripts\electronic-batch-record-jimu-list.test.mjs scripts\electronic-batch-record-open-button-proxy.test.mjs` -> PASS, 电子批记录列表与预览代理相关静态回归通过。
- GREEN: `pnpm exec eslint src\api\mes\pro\batchrecordreport\index.ts src\views\report\jmreport\index.vue scripts\report-management-six-route-page.test.mjs` -> PASS.
- RED: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm run ts:check` -> FAIL, repository-wide relaxed typecheck is blocked by unrelated pre-existing `src/api/showroom-admin/version-center.ts` `VersionCenterHistoryQuery` / `VersionCenterDetailQuery` index-signature errors; no error was reported in this task's touched files.

## 真实路径验证

- RED: first Playwright clear + A direct `.doc` run -> FAIL after successful backend response because the page table stayed empty; root cause was frontend API wrapper returning undefined data fields after double-unwrapping.
- GREEN: current-worktree frontend on `http://127.0.0.1:8081` + Playwright clear + A direct `.doc` -> PASS, result `{deletedReportCount:15, deletedMetadataCount:0, importedCount:15, createdCount:0, updatedCount:15, firstReportId:"34cae20da60d4b5b9c1c91cb5344581e"}` and visible Route A table rows.
- INFO: 用户要求本任务不再使用固定端口 `8081` / `48081`，后续真实路径验证切换为前端 `http://127.0.0.1:18081`、后端 `http://127.0.0.1:18083`。
- GREEN: current-worktree frontend on `http://127.0.0.1:18081` + backend `http://127.0.0.1:18083` + Playwright clear + A direct `.doc` -> PASS, result `{deletedReportCount:15, deletedMetadataCount:0, importedCount:15, createdCount:0, updatedCount:15, firstReportId:"34cae20da60d4b5b9c1c91cb5344581e"}` and visible Route A table rows.
- GREEN: Route A viewer screenshot capture -> PASS, captured 15 current Jimu viewer screenshots under `doc/tasks/20260524-ebr-report-visual-fidelity/artifacts/jimu-route-a/`.
- GREEN: Route A viewer screenshot capture via backend `18083` -> PASS, captured 15 current Jimu viewer screenshots with Jimu view URLs pointing at `http://127.0.0.1:18083/jmreport/view/...`.
- INFO: task-closeout-cleanup preview @ frontend repo -> BLOCKED for apply, preview would keep `task.md` / `execution-log.md` and delete screenshots/helper scripts/logs, but no checked-out worktree for main branch `master` was found; no cleanup deletion was applied.

## 最终结论

- 保留修改：前端固定路线识别 API 不再二次解包；识别/删除失败通过页面错误消息暴露；真实验证脚本切到专用端口 `18081 -> 18083`。
- 测试结果：前端定向 node 测试和 eslint 通过；全量 `pnpm run ts:check` 被既有 `version-center.ts` 类型错误阻塞。
- 真实路径：端口切换后，清除 15 张并 A 路重新生成 15 张，通过。

# 任务：DCC 受控文件下载鉴权修复

## 目标

修复 DCC 受控文件下载入口在新开页面时丢失鉴权头的问题，避免用户在 `我的文件`、受控文件详情或受控浏览页点击下载后跳转到 `{"code":401,"msg":"账号未登录","data":null}`。

## 前置任务检查

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260520-showroom-product-codex-bilingual-narration\task.md`
- 启动前状态：completed
- 影响：前一个前端任务已完成，不阻塞本次 DCC 下载缺陷修复。

## 范围

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\api\dcc\controlledFile\workflow.ts`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\dcc\controlled-file\detail\index.vue`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\dcc\controlled-file\mine\index.vue`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\dcc\controlled-file\browser\index.vue`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\dcc-controlled-file-download-auth.test.mjs`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260520-dcc-controlled-file-download-auth-fix\**`

## 非范围

- 不修改后端 Java 下载接口权限模型。
- 不新增匿名下载、签名 URL、兼容性 fallback 或绕过鉴权的下载方案。
- 不顺手处理与本次缺陷无关的其他 DCC 页面问题。

## 里程碑

- [x] M1：创建任务文档并记录前置任务检查
- [x] M2：补 BDD 与 RED 回归测试，证明下载入口仍走未鉴权的新开页
- [x] M3：实现统一鉴权下载修复
- [x] M4：完成定向验证并记录证据
- [x] M5：执行 closeout 预览并准备独立提交

## 预期验证

- `node --test scripts/dcc-controlled-file-download-auth.test.mjs`
- `pnpm exec eslint src/api/dcc/controlledFile/workflow.ts src/views/dcc/controlled-file/detail/index.vue src/views/dcc/controlled-file/mine/index.vue src/views/dcc/controlled-file/browser/index.vue scripts/dcc-controlled-file-download-auth.test.mjs`
- 如本地测试账号与页面数据可用，使用 Playwright 从 `http://localhost:8081` 进入真实 DCC 页面验证下载不再跳转 401

## Current Status

completed

## Final Verification Result

- PASS：`node --test scripts/dcc-controlled-file-download-auth.test.mjs`
- PASS：`pnpm exec eslint src/api/dcc/controlledFile/workflow.ts src/views/dcc/controlled-file/detail/index.vue src/views/dcc/controlled-file/mine/index.vue src/views/dcc/controlled-file/browser/index.vue scripts/dcc-controlled-file-download-auth.test.mjs`
- FAIL-FAST：`npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-controlled-file-download-auth run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260520-dcc-controlled-file-download-auth-fix\scripts\verify-dcc-controlled-file-download-auth.mjs` 返回 `mine_table_rows_missing`
- FAIL-FAST：`npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-controlled-file-browser-download-auth run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260520-dcc-controlled-file-download-auth-fix\scripts\verify-dcc-controlled-file-browser-download-auth.mjs` 返回 `browser_downloadable_previewable_row_missing`
- PASS：测试租户前置条件复核
  - `GET /admin-api/dcc/controlled-files/page?pageNo=1&pageSize=20&requesterId=113` -> `total=0`
  - `GET /admin-api/dcc/controlled-files/page?pageNo=1&pageSize=20&latestVersionOnly=true` -> `total=0`
- PASS：`python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260520-dcc-controlled-file-download-auth-fix --mode preview`
- PASS：`python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260520-dcc-controlled-file-download-auth-fix --mode apply`

## Blockers

- 阻塞：共享测试租户 `测试租户(id=122) / aoteman` 当前没有任何 DCC 受控文件数据，`我的文件` 与 `受控浏览` 列表都返回 `total=0`。
- 影响：本次无法用真实测试租户复现并回放“点击下载当前受控副本”的完整页面路径，只能依赖源码级 RED/GREEN 回归与接口前置条件核验确认修复。

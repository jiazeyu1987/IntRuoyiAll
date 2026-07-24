# Execution Log: DCC 文控中心子页签乱码修复

BDD: DCC 文控中心子页签显示可读中文 -> Given 用户从真实前端入口打开 `DCC文控中心` 下相关子页签, When 页面内容、按钮、提示和隐藏路由标题加载完成, Then 用户应看到规范简体中文，而不是乱码或 mojibake。

- M1: Paused `20260518-dcc-process-detail-stage-name-normalization` due user priority switch and created this task package before production code changes.
- RED: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260518-dcc-doc-center-subtab-garbled-content-fix\scripts\verify-dcc-doc-center-subtab-garbled-content-fix.mjs`
  -> FAIL, `distribution/index.vue`、`CategoryDepartmentRulesSection.vue` 与 `remaining.ts` 仍包含多处 `鏂囦欢绫诲埆 / 鍒嗗彂閮ㄩ棬 / 鎴戠殑鍩硅 / 鍒犻櫎` 等乱码文案。
- M2: Added the regression script and captured the failing garbled-copy evidence before production edits.
- M3: Replaced the remaining mojibake copy in `distribution/index.vue`, `CategoryDepartmentRulesSection.vue`, and the DCC hidden training-route titles in `remaining.ts`.
- GREEN: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260518-dcc-doc-center-subtab-garbled-content-fix\scripts\verify-dcc-doc-center-subtab-garbled-content-fix.mjs`
  -> PASS
- GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-doc-center-subtab-garbled-content-fix run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260518-dcc-doc-center-subtab-garbled-content-fix\scripts\verify-dcc-doc-center-subtab-garbled-content-real-e2e.mjs`
  -> PASS, real frontend pages `/dcc/controlled-file/distribution` and `/dcc/controlled-file/training-mine` both rendered readable Chinese text, and the final page title was `瑛泰管理系统 - 我的培训`.
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json`
  -> PASS

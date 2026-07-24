# Execution Log：Smart Release 前端构建回归修复

BDD: 前端发布构建通过 -> Given 展厅管理表格包含音频预览列 / When 运行 Vite test 模式发布构建 / Then ESLint 不因 HTML 自闭合规则失败，构建继续执行。

## Evidence

RED: `publish-int-ruoyi.ps1 -Mode build-release -ReleaseTag 20260606_smart_release_goal_0120 ...` -> FAIL, expected reason: frontend Vite build failed with `vue/html-self-closing` on `src/views/showroom-admin/components/HallListTable.vue` lines 51 and 63.

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; $env:VITE_BASE_URL=''; $env:VITE_BASE_PATH='/'; $env:VITE_OUT_DIR='dist-intruoyi-test'; node node_modules/vite/bin/vite.js build --mode test` -> PASS, frontend build completed and wrote `dist-intruoyi-test`.

GREEN: `task_closeout.py --task-id 20260606-smart-release-build-publish-goal --mode preview` -> PASS, keep `task.md`, `execution-log.md` and `bug-regression-evidence.md`, delete none, blocked none.

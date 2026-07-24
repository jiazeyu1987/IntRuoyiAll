# 执行日志：修复前端测试模式构建 EMFILE 阻塞

## 2026-05-24

- BDD: 测试模式构建应可用于发布 -> Given 当前 `int_main` 已合入版本中心且发布脚本依赖 `pnpm exec vite build --mode test` / When 按发布口径执行测试模式构建 / Then 构建必须成功产出前端静态资源，而不是因 `EMFILE` 中断
- RED: `pnpm exec vite build --mode test` -> FAIL, `EMFILE: too many open files`
- RED: 发布脚本口径模拟 `NODE_OPTIONS=--max-old-space-size=8192` + `VITE_BASE_URL/VITE_BASE_PATH/VITE_OUT_DIR` + `pnpm exec vite build --mode test` -> FAIL, `EMFILE: too many open files`
- RED: `node --test scripts/showroom-build-emfile-guard.test.mjs` -> FAIL, 还未配置 build 阶段关闭 auto-import dts 与 rollup 文件并发护栏
- GREEN: `node --test scripts/showroom-build-emfile-guard.test.mjs` -> PASS
- GREEN: 发布脚本口径模拟 `NODE_OPTIONS=--max-old-space-size=8192` + `VITE_BASE_URL/VITE_BASE_PATH/VITE_OUT_DIR` + `pnpm exec vite build --mode test` -> PASS
- REGRESSION: 同口径第二次重跑发布构建 -> PASS
- REGRESSION: `node --test scripts/showroom-admin-version-center.test.mjs scripts/showroom-admin-version-center-interaction.test.mjs scripts/showroom-admin-product-version-browser.test.mjs scripts/showroom-admin-frontend.test.mjs scripts/showroom-admin-company-dashboard-history.test.mjs scripts/showroom-admin-company-version-tab.test.mjs` -> PASS（48 tests）

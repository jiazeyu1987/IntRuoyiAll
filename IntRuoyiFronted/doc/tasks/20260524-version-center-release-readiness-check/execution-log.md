# 执行日志：版本中心合并后发布就绪性核查

## 2026-05-24

- BDD: 发布前核查 -> Given 版本中心已合入前端 `int_main` / When 核对工作树、验证结果和发布前置条件 / Then 明确输出是否可直接发布及阻塞项
- GREEN: `node --test scripts/showroom-admin-version-center.test.mjs scripts/showroom-admin-version-center-interaction.test.mjs scripts/showroom-admin-product-version-browser.test.mjs scripts/showroom-admin-frontend.test.mjs scripts/showroom-admin-company-dashboard-history.test.mjs scripts/showroom-admin-company-version-tab.test.mjs` -> PASS（48 tests）
- RED: `pnpm exec vite build --mode test` -> FAIL，`[unplugin-auto-import] EMFILE: too many open files`
- RED: 发布脚本口径模拟 `NODE_OPTIONS=--max-old-space-size=8192` + `VITE_BASE_URL/VITE_BASE_PATH/VITE_OUT_DIR` + `pnpm exec vite build --mode test` -> FAIL，`element-plus` 依赖加载阶段 `EMFILE: too many open files`
- GREEN: `npm run build` @ `D:\ProjectPackage\Website` -> PASS
- GREEN: 修复后 `node --test scripts/showroom-build-emfile-guard.test.mjs` -> PASS
- GREEN: 修复后发布脚本口径模拟 -> PASS
- REGRESSION: 修复后同口径第二次重跑 -> PASS

# 执行日志：恢复登录页默认租户为芋道源码

BDD: 登录页默认回到芋道源码 -> Given 用户清空浏览器登录缓存, When 打开普通登录页或社交登录页, Then 租户、用户名和密码默认显示 `芋道源码 / admin / admin123`。

BDD: 旧测试租户默认缓存被清理 -> Given 浏览器保存了旧默认 `测试租户 / aoteman / admin123` 或 `测试租户 / admin / admin123`, When 重新打开登录页, Then 页面显示 `芋道源码 / admin / admin123`，不被旧缓存带回测试租户。

BDD: 非默认测试租户缓存不被误清理 -> Given 浏览器保存了 `测试租户` 下的非默认账号, When 重新打开登录页, Then 该自定义历史记录保留，不被改写为芋道源码。

INFO: 已采用 `frontend-feature-delivery` 工作流。
INFO: 已确认上一前端任务 `20260525-showroom-manual-release-timeout` 状态为已完成。
GREEN: `node --check scripts\login-tenant-name-default.test.mjs` -> PASS。
RED: `npx.cmd --yes --package @playwright/cli playwright-cli -s=login-tenant-yudao-restore-red run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\login-tenant-name-default.test.mjs` -> FAIL, `login_default_tenant_mismatch: expected=芋道源码, actual=测试租户`。
INFO: 已修改 `.env`，默认登录从 `测试租户 / aoteman / admin123` 恢复为 `芋道源码 / admin / admin123`。
INFO: 已修改 `src/utils/auth.ts`，租户名称迁移只保留 `瑛泰源码 -> 芋道源码`，并清理旧默认 `测试租户 / aoteman|admin / 空密码或admin123` 缓存。
GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli -s=login-tenant-yudao-restore-green run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\login-tenant-name-default.test.mjs` -> PASS, 默认 `芋道源码/admin/admin123`，旧测试租户默认缓存回到芋道源码，非默认测试租户缓存保留，社交登录默认芋道源码。
GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json` -> PASS。
GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; $env:VITE_BASE_URL='http://172.30.30.58:48081'; $env:VITE_BASE_PATH='/'; $env:VITE_OUT_DIR='dist-intruoyi-test'; pnpm exec vite build --mode test` -> PASS。
GREEN: 构建产物检查 -> PASS, `LoginForm-Ck5vsVmk.js` 与 `SocialLogin-CaQ0ClzQ.js` 包含 `芋道源码/admin123`，不包含默认 `测试租户/aoteman`。
GREEN: `D:\ProjectPackage\Int\IntRuoyi\publish-int-ruoyi-to-test.bat skip-data` -> PASS, 已发布测试服，未同步数据库或 MinIO；前端、后端健康检查均 HTTP 200。
GREEN: 测试服发布产物检查 -> PASS, `LoginForm-rwxfEo6n.js` 与 `SocialLogin-DSgA7nv1.js` 包含 `芋道源码/admin123`，不包含默认 `测试租户/aoteman`。
GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli -s=test-yudao-login-after-restore-clean run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260525-login-default-yudao-restore\scripts\verify-test-yudao-login.mjs` -> PASS, clean session 默认 `芋道源码/admin`，真实登录进入 `http://172.30.30.58:8081/index`。
GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260525-login-default-yudao-restore/frontend-feature-evidence.md` -> PASS。
GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260525-login-default-yudao-restore --mode preview` -> PASS, delete only task temporary verification script and Playwright screenshots。
GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260525-login-default-yudao-restore --mode apply` -> PASS, removed task temporary verification script and Playwright screenshots。

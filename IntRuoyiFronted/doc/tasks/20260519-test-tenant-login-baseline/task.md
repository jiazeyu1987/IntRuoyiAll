# 任务：前端默认测试租户登录基线收口

## 目标

将前端默认登录入口和共享顶层测试脚本统一切换到现有测试租户 `测试租户(id=122)` 与共享测试账号 `aoteman/admin123`，避免后续测试继续落到正式租户默认值。

## 范围

- 修改前端默认登录环境配置。
- 收口普通登录页、社交登录页与 remembered loginForm 的默认租户行为。
- 更新共享顶层测试脚本的默认测试租户、账号和断言。
- 若真实登录被已批准测试租户的本地有效期阻塞，则联动修正该本地运行库租户有效期后继续验证。
- 若共享测试租户缺少脚本覆盖所需菜单权限，则联动补齐该测试租户的 `tenant_admin` 菜单权限后继续验证。
- 若 DCC 浏览仍无目录数据，则联动通过既有 IntAuth 目录导入路径初始化测试租户独立目录树后继续验证。
- 记录 BDD/TDD、前端验证证据与收尾预览结果。
- 不批量重写历史任务文档、旧截图结论或历史执行证据。

## 前置任务检查

- 上一个前端任务：`yudao-ui-admin-vue3/doc/tasks/20260519-showroom-single-parent-tabs/task.md`
- 启动前状态：已完成。
- 影响：可独立开展本次前端测试租户基线收口任务。

## 里程碑

- [x] M1：确认前置任务完成并创建前端任务包。
- [x] M2：记录 BDD 场景并跑出默认登录与共享脚本的 RED 证据。
- [x] M3：完成默认登录、缓存迁移和共享脚本收口。
- [x] M4：修正测试租户有效期、菜单权限与 DCC 目录数据前置条件。
- [x] M5：执行 GREEN 验证、更新任务文档并完成收尾预览。

## 预期验证

- `node --check D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\login-tenant-name-default.test.mjs`
- `npx.cmd --yes --package @playwright/cli playwright-cli --session login-tenant-name-default open http://127.0.0.1:8081/login`
- `npx.cmd --yes --package @playwright/cli playwright-cli --session login-tenant-name-default run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\login-tenant-name-default.test.mjs`
- `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-controlled-browser-directory-display run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\dcc-controlled-browser-directory-display.test.mjs`
- `node --test D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\electronic-batch-record-route.test.mjs`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-test-tenant-login-baseline\frontend-feature-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260519-test-tenant-login-baseline --mode preview`

## Current Status

Completed on 2026-05-19. 前端默认登录、缓存迁移和共享顶层测试脚本都已统一到测试租户基线，真实登录验证与共享脚本验证均已通过。

## Final Verification Result

- PASS：`node --check D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\login-tenant-name-default.test.mjs`
- PASS：`npx.cmd --yes --package @playwright/cli playwright-cli -s=login-tenant-name-default open http://127.0.0.1:8081/login`
- PASS：`npx.cmd --yes --package @playwright/cli playwright-cli -s=login-tenant-name-default run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\login-tenant-name-default.test.mjs`
- PASS：`npx.cmd --yes --package @playwright/cli playwright-cli -s=login-tenant-name-default run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\dcc-controlled-browser-directory-display.test.mjs`
- PASS：`node --test D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\electronic-batch-record-route.test.mjs`
- PASS：`python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-test-tenant-login-baseline\frontend-feature-evidence.md`
- PASS：`python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260519-test-tenant-login-baseline --mode preview`
- PASS：`python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260519-test-tenant-login-baseline --mode apply`

## 阻塞与影响

- 阻塞：无。
- 影响：后续共享前端验证可默认使用 `测试租户 / aoteman / admin123` 与本地后端 `48081` 联调入口；本任务附属前端证据文件已按 closeout 基线清理。

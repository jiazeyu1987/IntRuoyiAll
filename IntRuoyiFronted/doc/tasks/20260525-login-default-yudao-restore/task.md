# 任务：恢复登录页默认租户为芋道源码

## 任务目标

- 将前端全局默认登录恢复为 `芋道源码 / admin / admin123`。
- 不再把 `芋道源码` 作为旧租户迁移到 `测试租户`。
- 自动清理旧测试租户默认登录缓存，避免测试服发布后仍被浏览器历史缓存带回 `测试租户`。

## 非目标

- 不修改后端接口。
- 不修改真实租户数据。
- 不移除手动输入 `测试租户 / aoteman` 的能力。
- 不新增 fallback、mock 或默认成功路径。

## 前置任务检查

- 上一个前端任务：`20260525-showroom-manual-release-timeout`。
- 上一任务状态：`已完成`。
- 影响：上一任务已完成，不阻塞本次登录默认租户恢复。

## 里程碑

- [x] M1：建立任务记录并确认上一同仓任务已完成。
- [x] M2：补充登录默认租户与缓存迁移 RED 测试。
- [x] M3：恢复默认租户和登录缓存迁移逻辑。
- [x] M4：执行本机 GREEN、测试服构建口径验证与真实测试服发布验证。
- [x] M5：更新证据、执行 closeout 预览，并按策略提交本任务变更。

## BDD 场景

- BDD: 登录页默认回到芋道源码 -> Given 用户清空浏览器登录缓存, When 打开普通登录页或社交登录页, Then 租户、用户名和密码默认显示 `芋道源码 / admin / admin123`。
- BDD: 旧测试租户默认缓存被清理 -> Given 浏览器保存了旧默认 `测试租户 / aoteman / admin123` 或 `测试租户 / admin / admin123`, When 重新打开登录页, Then 页面显示 `芋道源码 / admin / admin123`，不被旧缓存带回测试租户。
- BDD: 非默认测试租户缓存不被误清理 -> Given 浏览器保存了 `测试租户` 下的非默认账号, When 重新打开登录页, Then 该自定义历史记录保留，不被改写为芋道源码。

## 预期验证

- `node --check scripts/login-tenant-name-default.test.mjs`
- `npx.cmd --yes --package @playwright/cli playwright-cli -s=login-tenant-yudao-restore run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\login-tenant-name-default.test.mjs`
- 发布口径构建：`pnpm exec vite build --mode test`
- 构建产物检查：登录 chunk 默认值包含 `芋道源码` 与 `admin`，不包含默认 `测试租户/aoteman`。
- 测试服发布脚本：`D:\ProjectPackage\Int\IntRuoyi\publish-int-ruoyi-to-test.bat`
- 测试服真实前端登录：`http://172.30.30.58:8081/login?redirect=/index` 使用 `芋道源码 / admin / admin123` 进入 `/index`。
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260525-login-default-yudao-restore/frontend-feature-evidence.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260525-login-default-yudao-restore --mode preview`

## Current Status

completed

## 当前状态

- 状态：completed
- 已完成：
  - 已确认上一前端任务已完成。
  - 已建立任务记录。
  - 已将全局默认登录改为 `芋道源码 / admin / admin123`。
  - 已将旧租户迁移逻辑收口为 `瑛泰源码 -> 芋道源码`。
  - 已增加旧默认测试租户缓存清理，且保留非默认测试租户账号历史。
  - 已通过本机 Playwright 登录默认值与缓存迁移验证。
  - 已通过测试服发布口径构建验证。
  - 已使用 `skip-data` 发布测试服，未同步数据库或 MinIO。
  - 已通过测试服真实前端默认值与 `芋道源码 / admin / admin123` 登录验证。
  - 已通过 frontend feature evidence 校验。
  - 已执行 task-closeout-cleanup 预览与 apply，已清理本任务临时脚本与截图。
- 阻塞与影响：
  - 暂无阻塞。

## Final Verification Result

- PASS: `node --check scripts\login-tenant-name-default.test.mjs`
- PASS: 本机 Playwright 登录默认值与缓存迁移验证。
- PASS: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json`
- PASS: 发布口径 `pnpm exec vite build --mode test`
- PASS: 测试服发布：`publish-int-ruoyi-to-test.bat skip-data`
- PASS: 测试服真实前端默认 `芋道源码/admin` 并登录进入 `/index`
- PASS: frontend feature evidence 校验
- PASS: task-closeout-cleanup preview
- PASS: task-closeout-cleanup apply

## Cleanup Keep

- `doc/tasks/20260525-login-default-yudao-restore/task.md`
- `doc/tasks/20260525-login-default-yudao-restore/execution-log.md`
- `doc/tasks/20260525-login-default-yudao-restore/frontend-feature-evidence.md`

## Cleanup Candidates

- `doc/tasks/20260525-login-default-yudao-restore/scripts/verify-test-yudao-login.mjs`
- `output/playwright/login-tenant-name-default-red.png`
- `output/playwright/login-tenant-name-default-green.png`
- `output/playwright/test-yudao-login-after-restore.png`

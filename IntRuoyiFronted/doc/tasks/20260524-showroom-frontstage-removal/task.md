# 任务：移除 IntRuoyi 展厅前台入口

## Goal

按 fail-fast 方式从 `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3` 中移除展厅前台展示入口，包括左侧 `前台大屏` 菜单、首页前台入口按钮、旧前台 alias 路由与对应前台页面代码；不保留 Website 跳转兼容入口。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\router\modules\showroom.ts`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\Home\Index.vue`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\showroom-frontstage\**`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\home-showroom-entry.test.mjs`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\permission-hidden-shell-route-merge.test.mjs`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\showroom-frontstage*.mjs`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\showroom-phase1-frontstage-display.e2e.mjs`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\showroom-phase1-e2e.manifest.mjs`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\run-showroom-phase1-e2e.mjs`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260524-showroom-frontstage-removal\**`

## Non-Scope

- 不修改 `D:\ProjectPackage\Website`
- 不修改后端公开 `website-config` 数据契约
- 不修改 `src\api\showroom-frontstage\index.ts`
- 不接手 `20260524-showroom-product-cover-prompt-management` 的提示管理实现

## Previous Task Check

- Previous same-repo task record:
  `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260524-showroom-product-cover-prompt-management\task.md`
- Status before this task: `Blocked`
- Impact on this task:
  上一任务未提交且当前根工作区已占用 `src/router/modules/showroom.ts`。本任务允许在该文件上继续增量修改，但不得回退 `ShowroomAdminPrompt` 路由相关在途改动；提交阶段若无法拆分干净，必须显式报告阻塞。

## BDD

- BDD: 左侧菜单不再暴露前台大屏 -> Given 用户登录 IntRuoyi 后台并加载展柜菜单 / When 权限路由合并静态与动态菜单 / Then 左侧菜单不得再出现可见的 `前台大屏` 子项。
- BDD: 首页只保留展厅后台入口 -> Given 用户打开后台首页 `http://localhost:8081` / When 首页渲染数字展厅入口卡片 / Then 页面只显示进入展厅后台的入口，不再显示 `进入展厅前台` 按钮或前后台双入口文案。
- BDD: IntRuoyi 不再注册前台展示路由 -> Given 应用加载展柜路由模块 / When 读取 `showroom.ts` 路由定义 / Then 不得再注册 `display/screen/*`、`display/pad/*`、`display/mobile/*` 与旧 alias `home/company-intro/display-hall/display-product/settings/narration`。

## Milestones

- [x] M1：核对同仓前置任务状态并建立本任务文档、执行日志。
- [x] M2：先补 RED，锁定首页入口、菜单合并与路由删除三个可观察行为。
- [x] M3：删除展厅前台路由、首页前台按钮与前台页面源码，保留后台依赖的公开 API 模块。
- [x] M4：清理/替换前台存在性测试，补充真实 Playwright 验证脚本。
- [ ] M5：运行定向验证、记录结果、检查提交边界并执行 closeout preview。

## Expected Verification

- `node --test scripts/home-showroom-entry.test.mjs scripts/permission-hidden-shell-route-merge.test.mjs scripts/showroom-frontstage-runtime.test.mjs`
- `pnpm exec eslint src/router/modules/showroom.ts src/views/Home/Index.vue scripts/home-showroom-entry.test.mjs scripts/permission-hidden-shell-route-merge.test.mjs scripts/showroom-frontstage-runtime.test.mjs`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`
- `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-frontstage-removal run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260524-showroom-frontstage-removal\scripts\verify-showroom-frontstage-removal.mjs`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260524-showroom-frontstage-removal --mode preview`

## Current Status

- Completed on 2026-05-24.
- 已完成前端实现：
  - `src/router/modules/showroom.ts` 已收缩为纯展厅后台路由，保留 `ShowroomAdminPrompt` 在途路由，不再注册任何前台 `display/*` 或旧 alias。
  - `src/views/Home/Index.vue` 已删除前台按钮和双入口文案，只保留展厅后台入口。
  - `src/views/showroom-frontstage/**` 已整体删除。
  - 旧前台存在性脚本已删除，保留 `scripts/showroom-frontstage-runtime.test.mjs` 继续校验后台仍依赖的公开 `website-config` 接口。
- 已完成定向验证：
  - `node --test scripts/home-showroom-entry.test.mjs scripts/permission-hidden-shell-route-merge.test.mjs`
  - `node --test scripts/showroom-admin-frontend.test.mjs`
  - `pnpm exec eslint src/router/modules/showroom.ts src/views/Home/Index.vue scripts/home-showroom-entry.test.mjs scripts/permission-hidden-shell-route-merge.test.mjs scripts/showroom-frontstage-runtime.test.mjs`
  - `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`
  - `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-frontstage-removal run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260524-showroom-frontstage-removal\scripts\verify-showroom-frontstage-removal.mjs`
  - `$env:INT_RUOYI_ADMIN_API_BASE='http://127.0.0.1:48082/admin-api'; node --test scripts/showroom-frontstage-runtime.test.mjs`
  - `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260524-showroom-frontstage-removal --mode preview`
  - `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260524-showroom-frontstage-removal\frontend-feature-evidence.md`

## Risks / Blockers

- 提交说明：
  - 前台入口删除代码已独立提交为 `be9aa73a` `任务: 移除展柜前台入口`
  - runtime 数据前置条件已通过后端仓 `c5b7eeb172` `任务: 修复本地展厅 website-config 验证数据` 解除
- 当前前端仓仍存在无关未跟踪目录 `doc/tasks/20260524-showroom-product-pagination-diagnosis/`，不属于本任务范围，也未混入本任务提交。

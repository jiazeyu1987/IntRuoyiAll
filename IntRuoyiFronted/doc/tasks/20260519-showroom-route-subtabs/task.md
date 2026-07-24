# 任务：展厅页签路由子页签化

## 目标

将展厅后台页面内的管理页签、数字展厅页面内的展示页签改为路由驱动的展厅子页签，使后续可以按菜单/权限分别控制每个子页签入口。

## 非目标

- 不修改后端接口契约。
- 不新增 mock 数据或静态兜底数据。
- 不调整展厅业务字段、审批流或音频生成逻辑。

## 里程碑

- [x] 记录 BDD/TDD 验证目标
- [x] 补充失败测试，覆盖后台和前台子页签路由化要求
- [x] 改造展厅后台页签为路由联动子页签
- [x] 改造数字展厅页签为路由联动子页签
- [x] 运行回归验证、记录证据并提交本任务改动

## 预期验证

- `node --test scripts/showroom-admin-frontend.test.mjs scripts/showroom-frontstage.test.mjs`
- `pnpm exec eslint src/router/modules/showroom-admin.ts src/router/modules/showroom-frontstage.ts src/views/showroom-admin/index.vue src/views/showroom-frontstage/index.vue`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260519-showroom-route-subtabs/frontend-feature-evidence.md`，证据文件已在校验通过后按 closeout 预览清理。

## 当前状态

已完成。

## 验证结果

- `node --test scripts/showroom-admin-frontend.test.mjs scripts/showroom-frontstage.test.mjs` 通过。
- `$env:NODE_PATH='D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\node_modules'; D:\Programs\node.exe D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\node_modules\eslint\bin\eslint.js ...` 通过。
- `python -X utf8 <inline Playwright route subtabs smoke>` 通过，真实登录后从首页入口进入展厅后台/前台并点击子页签验证 URL 与选中态同步。
- `task-closeout-cleanup --mode preview` 已确认无可删除产物，但自动并回主线被阻塞：当前未检出 `master` 主工作树。

## 已知阻塞

- 前台 `/showroom/display/home` 与 `/showroom/display/company` 的后端接口仍返回 `No static resource admin-api/showroom/display/*`，本任务未隐藏该错误，后续仍需后端接口补齐。
- linked worktree 自动 closeout/merge 被阻塞：清理脚本检测到默认主分支为 `master`，但本机没有 `master` 的已检出 worktree。

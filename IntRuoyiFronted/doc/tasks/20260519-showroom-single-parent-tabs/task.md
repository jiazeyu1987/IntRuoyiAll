# 任务：展厅菜单合并为单一父级子页签

## 目标

将展厅后台页签和数字展厅页签合并到同一个“展厅”父菜单下，所有页签作为“展厅”的一级子路由，便于后续按菜单权限统一控制。

## 里程碑

- [x] 记录 BDD/TDD 目标
- [x] 补充失败测试，确认只存在一个“展厅”父路由
- [x] 合并路由模块并更新首页入口
- [x] 更新后台与前台页签跳转路径
- [x] 运行测试、lint、记录结果并提交

## 预期验证

- `node --test scripts/showroom-admin-frontend.test.mjs scripts/showroom-frontstage.test.mjs scripts/home-showroom-entry.test.mjs`
- `pnpm exec eslint src/router/modules/showroom.ts src/router/modules/remaining.ts src/views/showroom-admin/index.vue src/views/showroom-frontstage/index.vue src/views/Home/Index.vue scripts/showroom-admin-frontend.test.mjs scripts/showroom-frontstage.test.mjs scripts/home-showroom-entry.test.mjs`

## 当前状态

已完成。

## 验证结果

- `node --test scripts/showroom-admin-frontend.test.mjs scripts/showroom-frontstage.test.mjs scripts/home-showroom-entry.test.mjs` 通过。
- `$env:NODE_PATH='D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\node_modules'; D:\Programs\node.exe D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\node_modules\eslint\bin\eslint.js ...` 通过。
- `node scripts/run-showroom-phase1-e2e.mjs --dry-run` 通过。

## 说明

- 前端菜单路由已合并为单一父级：`展厅`。
- 展厅后台页签路径收敛为 `/showroom/company`、`/showroom/product`、`/showroom/hall`、`/showroom/approval`、`/showroom/history`、`/showroom/assignment`、`/showroom/discussion`、`/showroom/narration-workbench`。
- 数字展厅页签路径收敛为 `/showroom/home`、`/showroom/company-intro`、`/showroom/settings`、`/showroom/narration`。

# 任务：展厅父级菜单可见

## 目标

将统一后的“展厅”父级路由显示在前端菜单中，让用户可以从菜单看到并进入展厅各子页签。

## 里程碑

- [x] 记录 BDD/TDD 目标
- [x] 补充失败测试，确认展厅父级菜单不再隐藏
- [x] 调整路由元信息
- [x] 运行测试和 lint
- [ ] 提交并合并回主分支

## 预期验证

- `node --test scripts/showroom-admin-frontend.test.mjs`
- `pnpm exec eslint src/router/modules/showroom.ts scripts/showroom-admin-frontend.test.mjs`

## 当前状态

已完成实现与验证，待提交合并。

## 验证结果

- `node --test scripts/showroom-admin-frontend.test.mjs` 通过。
- `$env:NODE_PATH='D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\node_modules'; D:\Programs\node.exe D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\node_modules\eslint\bin\eslint.js src/router/modules/showroom.ts scripts/showroom-admin-frontend.test.mjs` 通过。

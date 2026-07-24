# 任务：删除展厅后台页面内部页签

## 目标

展厅父级菜单已经提供子页签切换能力，展厅后台页面不应再渲染重复的内部 tab，只根据当前子路由显示对应内容。

## 里程碑

- [x] 记录 BDD/TDD 目标
- [x] 补充失败测试，确认页面内部 tab 被删除
- [x] 删除后台页面内部 tab 渲染和跳转逻辑
- [x] 运行测试和 lint
- [x] 提交并合并回主分支

## 预期验证

- `node --test scripts/showroom-admin-frontend.test.mjs`
- `pnpm exec eslint src/views/showroom-admin/index.vue scripts/showroom-admin-frontend.test.mjs`

## 当前状态

已完成。

## 验证结果

- `node --test scripts/showroom-admin-frontend.test.mjs` 通过。
- `$env:NODE_PATH='D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\node_modules'; D:\Programs\node.exe D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\node_modules\eslint\bin\eslint.js src/views/showroom-admin/index.vue scripts/showroom-admin-frontend.test.mjs` 通过。

## 说明

- 本任务只删除截图中后台页面的重复内部 tab。
- 主工作区存在未提交的前台页面重构改动，本任务未触碰该文件，避免混入无关改动。

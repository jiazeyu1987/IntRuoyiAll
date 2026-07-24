# 任务：展厅产品与展厅 CRUD 前端

## 目标

产品管理和展厅管理必须支持新增、删除、查找、修改，去掉页面顶部后台 banner，并保证列表请求每页最多 20 条。

## 里程碑

- [x] 记录 BDD/TDD 目标
- [x] 补充失败测试
- [x] 实现前端 CRUD UI 与真实 API 调用
- [x] 运行测试、lint 和实际构建验证
- [x] 提交并合并回主分支

## 预期验证

- `node --test scripts/showroom-admin-frontend.test.mjs scripts/showroom-admin-product-list.test.mjs scripts/showroom-admin-hall-list.test.mjs`
- `pnpm exec eslint src/api/showroom-admin/index.ts src/views/showroom-admin/index.vue src/views/showroom-admin/components/ProductListTable.vue src/views/showroom-admin/components/HallListTable.vue scripts/showroom-admin-frontend.test.mjs scripts/showroom-admin-product-list.test.mjs scripts/showroom-admin-hall-list.test.mjs`
- `pnpm exec vite build --mode env.local`

## 当前状态

已完成，已合并回 `int_main`。

## 完成内容

- 产品管理支持查询、新增、编辑、删除，更新走真实 `saveProductDraft`，新增/删除走真实接口。
- 展厅管理支持查询、新增、编辑、删除，更新走真实 `updateHall`，展厅编码在编辑时只读以匹配当前后端契约。
- 移除页面顶部冗余 banner，保留路由页签作为权限控制入口。
- 产品、展厅和审批列表请求统一使用 `pageSize: 20`。

## 验证证据

- RED 已记录：新增测试在实现前失败。
- GREEN 已记录：结构/契约测试 14 项通过。
- ESLint 已通过：使用主前端依赖执行当前 worktree 文件。
- Vite 构建已通过：`pnpm exec vite build --mode env.local`。
- `vue-tsc` 全量检查未作为通过项：当前项目基线大量既有文件缺少自动导入类型声明，非本任务新增问题，已在执行记录中标明。
- 合并后主工作区复验通过：结构/契约测试 14 项、ESLint、Vite 构建。

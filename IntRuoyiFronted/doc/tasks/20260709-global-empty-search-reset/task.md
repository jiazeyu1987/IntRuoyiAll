# 全局空搜索触发重置

## 任务目标

所有类似 `搜索` / `查询` 按钮在对应输入栏为空时，统一执行重置逻辑；有输入条件时保留原搜索逻辑。

## 里程碑

1. 已完成：读取 PowerShell、经验索引、统一前端样式、前端交付契约。
2. 已完成：定位通用搜索组件和页面级 `handleQuery` / `resetQuery` 模式。
3. 已完成：新增空搜索触发重置的失败契约测试。
4. 已完成：实现通用逻辑并运行目标验证。
5. 已完成：更新执行证据和提交边界。

## 预期验证

- `node tests/e2e/empty-search-reset-static.spec.js`
- `node tests/e2e/table-quick-filter-static.spec.js`
- `node node_modules/eslint/bin/eslint.js <changed-files>`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm.cmd ts:check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260709-global-empty-search-reset/frontend-feature-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260709-global-empty-search-reset --mode preview`

## 经验门禁

- PowerShell / Windows shell：已读取 `docs/powershell-memory.md`；中文读写显式 UTF-8，命令串联不用 `&&`。
- 前端页面 / 表格 / 样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`；不调整视觉样式，仅统一行为。
- 前端交付：已读取 `frontend-feature-delivery` 与 `frontend-contract.md`；不修改后端接口、权限、路由、数据状态，不引入 mock 或 fallback。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；优先抽取统一空搜索判断，避免逐页复制分叉逻辑。
- `是否存在临时补丁或绕过`：否。

## 当前状态

COMPLETED：已完成通用搜索组件、标准列表快速过滤和页面级搜索按钮的空输入重置逻辑；静态契约、ESLint 与 `ts:check` 均通过。

## Current Status

completed

## 完成结果

- 新增 `src/utils/search.ts`，统一判断搜索表单是否只有空输入。
- 更新 `src/components/Search/src/Search.vue`，通用搜索组件空输入搜索时执行 `reset()`。
- 批量更新 `src/views/**/*.vue` 中具备 `handleQuery` / `resetQuery` 配对的列表搜索按钮，空输入搜索时执行对应 `resetQuery()`；重置内部刷新使用 `handleQuery(true)` 避免递归。
- 保持 `src/hooks/web/useTableQuickFilter.ts` 的标准列表快速过滤空搜索重置逻辑通过回归契约。
- 新增 `tests/e2e/empty-search-reset-static.spec.js` 覆盖通用组件、页面级搜索按钮和重置递归保护。

## 最终验证

- `node tests/e2e/empty-search-reset-static.spec.js` -> PASS。
- `node tests/e2e/table-quick-filter-static.spec.js` -> PASS。
- `node node_modules/eslint/bin/eslint.js src/utils/search.ts src/components/Search/src/Search.vue tests/e2e/empty-search-reset-static.spec.js tests/e2e/table-quick-filter-static.spec.js src/views/mall/product/spu/components/SpuTableSelect.vue src/views/mall/promotion/combination/components/CombinationTableSelect.vue src/views/mall/promotion/point/components/PointTableSelect.vue src/views/mall/promotion/seckill/components/SeckillTableSelect.vue src/views/system/menu/index.vue` -> PASS。
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm.cmd ts:check` -> PASS。
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260709-global-empty-search-reset/frontend-feature-evidence.md` -> PASS。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260709-global-empty-search-reset --mode preview` -> PASS。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260709-global-empty-search-reset --mode apply` -> PASS，已删除临时 evidence 文件。

## 提交状态

- Git commit -> BLOCKED，当前前端仓存在大量多任务混合脏改，且本任务批量触达的列表文件与既有未提交改动存在文件级重叠；为避免把非本任务 hunk 混入提交，未创建提交。

# 标准列表模板空搜索触发重置

## 任务目标

标准列表模板的快速过滤输入栏为空时，点击 `查询` 不再提示“请输入快速过滤值”，而是执行现有重置命令：清空快速过滤条件、回到第一页并重新加载列表。

## 里程碑

1. 已完成：读取 PowerShell、经验索引、统一前端样式、前端交付契约和标准列表模板实现。
2. 已完成：新增标准列表空搜索触发重置的失败契约测试。
3. 已完成：最小修改快速过滤 Hook，空输入查询时执行重置命令。
4. 已完成：目标静态测试与 ESLint 通过；全量 TypeScript 检查被既有非本任务 `RouteFlowGraphDesigner.vue` 类型错误阻塞。
5. 已完成：记录最终验证和提交状态。

## 预期验证

- `node tests/e2e/table-quick-filter-static.spec.js`
- `node tests/e2e/unified-list-template-static.spec.js`
- `node node_modules/eslint/bin/eslint.js src/hooks/web/useTableQuickFilter.ts tests/e2e/table-quick-filter-static.spec.js`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm.cmd ts:check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260709-standard-list-empty-search-reset/frontend-feature-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260709-standard-list-empty-search-reset --mode preview`

## 经验门禁

- PowerShell / Windows shell：已读取 `docs/powershell-memory.md`；中文读写显式 UTF-8，命令串联不用 `&&`。
- 前端页面 / 表格 / 样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`；不调整视觉结构，保持标准列表模板现有工具栏样式。
- 前端交付：已读取 `frontend-feature-delivery` 与 `frontend-contract.md`；不修改后端接口、权限、路由、数据状态，不引入 mock 或 fallback。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；在统一快速过滤 Hook 中复用现有重置命令，覆盖所有接入标准列表模板的页面。
- `是否存在临时补丁或绕过`：否。

## 当前状态

COMPLETED：标准列表模板空搜索触发重置的实现与目标契约已完成；`table-quick-filter-static`、全部当前变更静态测试、目标 ESLint 和全量 `pnpm.cmd ts:check` 均通过，原非本任务类型检查阻塞已不存在。

## Current Status

completed

## 验证结果

- RED: `node tests/e2e/table-quick-filter-static.spec.js` -> FAIL，旧 Hook 未识别空输入查询并执行重置。
- GREEN: `node tests/e2e/table-quick-filter-static.spec.js` -> PASS。
- GREEN: `node node_modules/eslint/bin/eslint.js src/hooks/web/useTableQuickFilter.ts tests/e2e/table-quick-filter-static.spec.js` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260709-standard-list-empty-search-reset/frontend-feature-evidence.md` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260709-standard-list-empty-search-reset --mode preview` -> PASS，仅建议删除临时前端证据文件，未执行 apply。
- BLOCKER: `pnpm.cmd ts:check` -> FAIL，`src/views/mes/pro/route/RouteFlowGraphDesigner.vue(1330,36): Property 'id' does not exist on type 'NodeChange'`，该文件为既有脏改且不属于本任务修改范围。
- BLOCKER: `node tests/e2e/unified-list-template-static.spec.js` -> FAIL，当前排产工单页结构已被其他任务改动，旧静态契约要求 `<ContentWrap title="排产工单">` 标题栏插槽，不属于本任务修改范围。

## 提交状态

- READY：当前统一回归与暂存边界检查通过，纳入本次独立前端提交。

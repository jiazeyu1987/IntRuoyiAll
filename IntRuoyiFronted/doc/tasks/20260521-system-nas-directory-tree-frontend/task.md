# 任务：系统管理 NAS 管理页签目录列表（前端）

## Goal

增强 `系统管理 -> NAS管理` 页面：

- 增加目录列表/目录树区域
- 增加“刷新目录”按钮
- 仅在测试连接成功后允许点击刷新目录
- 点击后把 NAS 目录结构同步展示到当前页签下

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\system\nas\**`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\api\system\nas\**`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\system-nas-management.test.mjs`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-system-nas-directory-tree-frontend\**`

## Non-Scope

- 不新增新的菜单结构。
- 不更改已有 NAS 参数表单字段。
- 不在未测试成功前自动偷偷刷新目录。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-remove-status-assignee-columns\task.md`
- Status before this task: `In Progress`
- Reported impact: 该任务与当前 NAS 管理页不在同一功能域，本次只在 NAS 管理页和其脚本内增量修改，不覆盖展厅任务文件。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Current state: 仓库存在大量与本任务无关的 showroom 改动。
- Impact: 本任务只修改 NAS 管理 API、页面和本任务文档，避免混入无关变更。

## Milestones

- [x] M1: 创建任务文档并确认本次前端改动边界。
- [x] M2: 记录 BDD 与 RED，锁定“测试成功后刷新目录并展示目录树”的契约。
- [x] M3: 实现目录区域、刷新目录按钮和树渲染。
- [x] M4: 跑定向验证并记录 GREEN。
- [x] M5: 运行 closeout preview 并完成收尾。

## Expected Verification

- `node --test scripts\\system-nas-management.test.mjs`
- `node --max-old-space-size=8192 node_modules/vue-tsc/bin/vue-tsc.js --noEmit -p tsconfig.relaxed.json`
- `pnpm exec eslint src/api/system/nas/index.ts src/views/system/nas/index.vue scripts/system-nas-management.test.mjs --format stylish`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260521-system-nas-directory-tree-frontend/frontend-feature-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-system-nas-directory-tree-frontend --mode preview`

## Current Status

Completed on 2026-05-21. 刷新目录按钮、目录树展示、定向验证、证据校验和 closeout preview 均已完成。

## Blockers And Impact

- Blocker: none.
- Impact: pending implementation result.

## Final Verification Result

- `node --test scripts\system-nas-management.test.mjs` -> PASS，2 tests green。
- `node --max-old-space-size=8192 node_modules/vue-tsc/bin/vue-tsc.js --noEmit -p tsconfig.relaxed.json` -> PASS。
- `pnpm exec eslint src/api/system/nas/index.ts src/views/system/nas/index.vue scripts/system-nas-management.test.mjs --format stylish` -> PASS。
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260521-system-nas-directory-tree-frontend/frontend-feature-evidence.md` -> PASS。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-system-nas-directory-tree-frontend --mode preview` -> READY，默认 keep `task.md` / `execution-log.md`，若 apply 会删除 `frontend-feature-evidence.md`；本次仅执行 preview。

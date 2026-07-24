# 任务：系统管理 NAS 管理页签（前端）

## Goal

在 `yudao-ui-admin-vue3` 中新增一个挂在“系统管理”下的 NAS 管理页面，页面提供：

- NAS 服务器、共享名、用户名、密码等连接参数输入
- 保存按钮
- 测试连接按钮
- 测试结果反馈

并对接后端的 NAS 参数读取/保存/测试接口。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\system\nas\**`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\api\system\nas\**`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\**`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260520-system-nas-management-frontend\**`

## Non-Scope

- 不改动除 NAS 管理页外的系统管理页面布局。
- 不做前端 mock 成功、占位测试结果或静默吞错。
- 不处理菜单数据本身的后端下发逻辑；前端只提供与菜单组件路径匹配的页面实现。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260520-showroom-publicity-product-assignment\task.md`
- Status before this task: `Completed on 2026-05-20`
- Impact: 无，可继续处理系统管理 NAS 管理页面。

## Milestones

- [x] M1: 创建前端任务文档并确认上一同仓任务完成状态。
- [x] M2: 记录 BDD 与前端 RED，锁定页面字段、保存按钮和测试连接按钮契约。
- [x] M3: 实现 NAS 管理页面和前端 API 对接。
- [x] M4: 跑前端定向验证并记录 GREEN。
- [x] M5: 运行 task-closeout-cleanup 预览，完成任务文档。

## Expected Verification

- `node --test scripts\\system-nas-management.test.mjs`
- `node --max-old-space-size=8192 node_modules/vue-tsc/bin/vue-tsc.js --noEmit -p tsconfig.relaxed.json`
- `pnpm exec eslint src/api/system/nas/index.ts src/views/system/nas/index.vue scripts/system-nas-management.test.mjs --format stylish`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260520-system-nas-management-frontend/frontend-feature-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260520-system-nas-management-frontend --mode preview`

## Current Status

Completed on 2026-05-21. 前端 NAS 管理页、API 对接、静态契约测试、类型检查、定向 lint、证据校验和 closeout preview 均已完成。

## Blockers And Impact

- Blocker: none.
- Impact: pending implementation result.

## Final Verification Result

- `node --test scripts\system-nas-management.test.mjs` -> PASS，2 tests green。
- `node --max-old-space-size=8192 node_modules/vue-tsc/bin/vue-tsc.js --noEmit -p tsconfig.relaxed.json` -> PASS。
- `pnpm exec eslint src/api/system/nas/index.ts src/views/system/nas/index.vue scripts/system-nas-management.test.mjs --format stylish` -> PASS。
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260520-system-nas-management-frontend/frontend-feature-evidence.md` -> PASS。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260520-system-nas-management-frontend --mode preview` -> READY，默认 keep `task.md` / `execution-log.md`，若 apply 会删除 `frontend-feature-evidence.md`；本次仅执行 preview。

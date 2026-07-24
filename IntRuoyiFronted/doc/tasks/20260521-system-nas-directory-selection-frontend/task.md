# 任务：NAS 管理页目录选择与导出占位（前端）

## Goal

在 `系统管理 -> NAS管理` 页的目录区域增加两个按钮：

- `选择`：进入目录选择模式，可多选目录
- `导出`：仅在已选择目录后激活，当前先做占位按钮

并满足：

- 非选择模式下不显示目录勾选
- 进入选择模式后可以多选目录
- 选择目录后导出按钮激活
- 导出按钮当前不接后端导出能力，只做前端占位反馈

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\system
as\**`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\api\system
as\**`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\system-nas-management.test.mjs`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-system-nas-directory-selection-frontend\**`

## Non-Scope

- 不新增后端导出接口。
- 不改变 NAS 懒加载目录读取契约。
- 不修改系统管理其他页面。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-system-nas-lazy-directory-frontend\task.md`
- Status before this task: `Completed on 2026-05-21`
- Impact: 目录懒加载能力已完成，本轮在该基础上补目录选择模式与导出占位，不回退现有懒加载行为。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Current state: 仓库存在多处与 showroom 相关的用户改动。
- Impact: 本任务只修改 NAS 管理页面、静态脚本和本任务文档，避免混入无关变更。

## Milestones

- [x] M1: 创建任务文档并确认上一同仓 NAS 任务完成状态。
- [x] M2: 记录 BDD 与 RED，锁定“进入选择模式、多选目录、导出按钮激活”的契约。
- [x] M3: 完成最小前端实现并保留懒加载与错误提示。
- [x] M4: 跑定向验证并记录 GREEN。
- [x] M5: 运行 closeout preview 并完成收尾。

## Expected Verification

- `node --test scripts\system-nas-management.test.mjs`
- `node --max-old-space-size=8192 node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p tsconfig.relaxed.json`
- `pnpm exec eslint src/api/system/nas/index.ts src/views/system/nas/index.vue scripts/system-nas-management.test.mjs --format stylish`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260521-system-nas-directory-selection-frontend/frontend-feature-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-system-nas-directory-selection-frontend --mode preview`

## Current Status

Completed on 2026-05-21. 已完成选择模式、多选目录和导出占位实现，并通过静态验证、类型检查、定向 lint、真实页面验证与 closeout preview。

## Blockers And Impact

- Blocker: none.
- Impact:
  - 目录区已增加 `选择` 与 `导出` 按钮。
  - 选择模式支持多选目录，导出按钮会在选中目录后激活。
  - 导出当前仅做前端占位提示，不触发新的后端导出请求。

## Final Verification Result

- `node --test scripts\system-nas-management.test.mjs` -> PASS，2 tests green。
- `node --max-old-space-size=8192 node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p tsconfig.relaxed.json` -> PASS。
- `pnpm exec eslint src/views/system/nas/index.vue scripts/system-nas-management.test.mjs --format stylish` -> PASS。
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260521-system-nas-directory-selection-frontend/frontend-feature-evidence.md` -> PASS。
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-system-nas-directory-selection-frontend --mode preview` -> READY，默认 keep `task.md` / `execution-log.md`，preview 计划清理 `frontend-feature-evidence.md`。
- 真实页面验证 -> PASS：
  - 登录 `芋道源码 / admin / admin123`
  - 打开 `http://127.0.0.1:8081/system/nas`
  - 进入选择模式后，目录树显示 `9` 个勾选框
  - 未选择目录时 `导出` 按钮禁用，选择目录后激活
  - 点击 `导出` 后页面提示 `已选择 2 个目录，导出功能暂未开放`
  - 页面网络请求仍只有 `nas-config/test` 和 `nas-files`，未新增导出接口请求

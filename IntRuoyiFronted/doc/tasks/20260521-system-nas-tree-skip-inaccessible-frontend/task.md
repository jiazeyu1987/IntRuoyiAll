# 任务：NAS 管理页展示跳过目录（前端）

## Goal

增强 `NAS管理` 页面目录区块，使“刷新目录”在后端跳过无权限目录时：

- 仍展示成功同步到的目录树
- 在页面上显式展示被跳过的目录路径和原因

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\system\nas\**`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\api\system\nas\**`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\system-nas-management.test.mjs`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-system-nas-tree-skip-inaccessible-frontend\**`

## Non-Scope

- 不改变保存/测试连接表单字段。
- 不隐藏 skipped 结果。
- 不更改其他系统管理页面。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-system-nas-directory-tree-frontend\task.md`
- Status before this task: `Completed on 2026-05-21`
- Impact: 页面已有刷新目录按钮和目录树；本任务只补 skipped 结果展示。

## Milestones

- [x] M1: 创建任务文档并记录真实失败背景。
- [ ] M2: 补 RED 契约，锁定“目录树 + skipped 列表”页面行为。
- [ ] M3: 做最小前端修复并展示 skipped 结果。
- [ ] M4: 跑定向验证并记录 GREEN。
- [ ] M5: 运行 closeout preview 并完成收尾。

## Expected Verification

- `node --test scripts\\system-nas-management.test.mjs`
- `node --max-old-space-size=8192 node_modules/vue-tsc/bin/vue-tsc.js --noEmit -p tsconfig.relaxed.json`
- `pnpm exec eslint src/api/system/nas/index.ts src/views/system/nas/index.vue scripts/system-nas-management.test.mjs --format stylish`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260521-system-nas-tree-skip-inaccessible-frontend/frontend-feature-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-system-nas-tree-skip-inaccessible-frontend --mode preview`

## Current Status

Completed with blockers on 2026-05-21. 前端已补 skipped 目录展示和目录树刷新结果区；但如果后端继续执行整棵共享全量递归，当前页面仍会受后端超时影响。

## Blockers And Impact

- Blocker:
  - 后端真实整棵共享递归在当前 NAS 体量下仍可能超过页面默认超时。
- Impact:
  - 页面已经能展示 skipped 目录结果。
  - 但在真实环境下，刷新目录是否能在前端超时时间内完成，仍取决于后端递归耗时。

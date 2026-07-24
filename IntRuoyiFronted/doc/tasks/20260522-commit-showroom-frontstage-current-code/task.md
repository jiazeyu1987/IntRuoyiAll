# 任务：提交 Showroom Frontstage 当前代码

## 目标

在 `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3` 中提交当前 `showroom-frontstage` 已通过验证的前端源码与测试改动，不混入 showroom-admin 历史残留、无关 task 文档或其他在途修改。

## 范围

- `scripts\showroom-frontstage*.mjs`
- `src\api\showroom-frontstage\**`
- `src\router\modules\showroom.ts`
- `src\views\showroom-frontstage\**`
- `doc\tasks\20260522-commit-showroom-frontstage-current-code\**`

## 非范围

- 不提交 `doc\tasks\20260519-showroom-frontstage-shell-wave-d\**` 等历史 reviewer 记录残留。
- 不提交 showroom-admin、权限、CRM 或其他无关前端改动。
- 不回退用户未明确要求处理的其他工作区内容。

## 前置检查

- 前端真实仓库为 `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`。
- 关联前台收敛任务 `doc\tasks\20260519-showroom-frontstage-shell-wave-d\task.md` 已完成，可作为本次前台代码提交的前序完成依据。

## 里程碑

- [x] M1：锁定 frontstage 当前提交范围并确认前序任务状态。
- [x] M2：复核 frontstage 相关验证命令通过。
- [ ] M3：精确暂存 frontstage 当前代码与本任务记录。
- [ ] M4：完成前端 Git 提交并复核剩余工作区状态。
- [x] M5：执行 closeout preview 并记录结果。

## 预期验证

- `node --test scripts/showroom-frontstage.test.mjs scripts/showroom-frontstage-runtime.test.mjs`
- `pnpm exec eslint src/api/showroom-frontstage/index.ts src/router/modules/showroom.ts src/views/showroom-frontstage/index.vue src/views/showroom-frontstage/mobile/composables/useShowroomMobileView.ts src/views/showroom-frontstage/shared/payload.ts src/views/showroom-frontstage/shared/types.ts scripts/showroom-frontstage.test.mjs scripts/showroom-frontstage-runtime.test.mjs`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260522-commit-showroom-frontstage-current-code --mode preview`

## 当前状态

Verified, pending commit.

## 当前结果

- frontstage 相关验证已通过：
  - `node --test scripts/showroom-frontstage.test.mjs scripts/showroom-frontstage-runtime.test.mjs`
  - `pnpm exec eslint src/api/showroom-frontstage/index.ts src/router/modules/showroom.ts src/views/showroom-frontstage/index.vue src/views/showroom-frontstage/mobile/composables/useShowroomMobileView.ts src/views/showroom-frontstage/shared/payload.ts src/views/showroom-frontstage/shared/types.ts scripts/showroom-frontstage.test.mjs scripts/showroom-frontstage-runtime.test.mjs`
- closeout preview 已通过：
  - `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260522-commit-showroom-frontstage-current-code --mode preview`

# Task: MES 工艺路线状态开关报错排查修复

## Goal

排查并修复 `http://localhost:8081/mes/pro/route` 工艺路线列表中状态开关在真实用户路径下的报错，明确问题是前端交互异常、接口调用异常，还是导入路线缺少启用前置条件，并补齐回归验证证据。

## Scope

- 前端仓库优先，使用真实登录与真实页面路径复现问题。
- 先记录准确报错和触发对象，再决定是否需要前后端代码修复。
- 若需要改前端，保持现有 Int 运营台风格，不新增测试专用控件。
- 若问题根因落在后端约束或导入数据映射，记录结论并在对应仓库补建任务文档后再改动生产代码。
- 全程遵循 BDD + 严格 TDD，先 RED 复现，再最小修复，再 GREEN 回归。

## Previous Task Check

- Previous frontend task: `doc/tasks/20260514-remove-auto-schedule-worktree/task.md`
- Status before this task: blocked by user priority switch.
- Impact: the paused worktree cleanup does not block this runtime bug task.

## Milestones

- [x] M1: Confirm the previous frontend task state and block it explicitly.
- [x] M2: Create this task document and execution log before code changes.
- [x] M3: Reproduce the route status-toggle error on the real local page and capture the exact blocker.
- [x] M4: Add RED regression evidence for the failing behavior.
- [x] M5: Implement the minimal fix in the correct repo scope if this is a code bug.
- [x] M6: Run GREEN verification and update the task evidence.

## Expected Verification

- Real Playwright login reaches `http://localhost:8081/mes/pro/route`.
- The failing route row and exact error message are captured.
- If the issue is a code bug, targeted verification passes after the fix.
- If the issue is a missing business prerequisite rather than a code bug, the exact precondition and impact are recorded without fallback behavior.

## Current Status

Completed. The real route status-toggle flow now shows the exact backend blocker `工艺路线必须要有关键工序` instead of the generic `服务器错误,请联系管理员!`.

## Blocker And Impact

- Blocker: none.
- Impact: operators can now see the exact enable precondition and fix route data accordingly.

## Final Verification Result

- RED: `npx.cmd --yes --package @playwright/cli playwright-cli --session route-status-toggle-repro run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260515-route-status-toggle-runtime-error\scripts\verify-route-status-business-message.mjs`
  - Result before the fix: FAIL, the real page showed `服务器错误,请联系管理员!` instead of the backend business blocker.
- GREEN: same Playwright verification command
  - Result after the fix: PASS, the real page showed `工艺路线必须要有关键工序`.
- Focused static verification:
  - `pnpm exec eslint src/config/axios/service.ts`
  - Result: PASS

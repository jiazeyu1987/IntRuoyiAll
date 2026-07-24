# Task: DCC 终审后状态滞留 FINALIZING 修复

## Goal

修复 DCC 受控文件在真实四阶段审批全部通过后仍长期停留在 `FINALIZING`
状态的问题，使最终状态能正确进入 `ACTIVE`，从而允许受控预览与下载权限按
已发布版本生效。

## Scope

- 在后端仓库创建任务文档、执行日志和回归证据。
- 仅修改 `yudao-module-dcc` 中与审批终态状态切换直接相关的流程/测试。
- 先补失败回归测试，再做最小实现修复。
- 不新增 fallback 状态分支，不放宽预览权限去掩盖终态状态错误。

## Previous Task Check

- Previous backend task:
  `doc/tasks/20260516-dcc-special-position-list-visibility/task.md`
- Status before this task: completed.
- Impact: no finished-latest backend task blocks this DCC final status fix.
- Note: the repository also contains unrelated in-progress DCC preview-metadata
  work; this task stays scoped to `workflow/finalization` files and does not
  touch those other pending edits.

## Milestones

- [x] M1: Create this task package and capture the live blocker context.
- [x] M2: Add RED regression coverage for the final approval path that should
  hand off to finalization without persisting a stale `FINALIZING` status.
- [x] M3: Implement the minimal workflow status fix.
- [x] M4: Run GREEN verification and update bug evidence.
- [x] M5: Commit only this backend task's files if verification fully passes.

## Expected Verification

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc -Dtest=DccControlledFileWorkflowServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`

## Current Status

Completed on 2026-05-16. The workflow layer no longer persists a stale
`FINALIZING` status after the final generic approval task. Targeted backend
regression tests pass, and the real frontend E2E can now reach an `ACTIVE`
controlled file again.

## Blocker And Impact

- Blocker: none at task creation time.
- Impact: until this fix lands, real DCC approval-to-preview E2E cannot reach a
  previewable `ACTIVE` file even when finalization side effects already exist.

## Final Verification Result

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc -Dtest=DccControlledFileWorkflowServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS
- Real browser follow-up from `yudao-ui-admin-vue3` task `20260516-dcc-controlled-preview-stamp-real-e2e` -> PASS, controlled file `2054545668044042268` reached status `现行` and exposed the preview entry again.

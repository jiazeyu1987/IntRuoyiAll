# Verification Report

## Scope

- 修复「新增测试项」弹窗中测试目标项行的序号控件溢出和目标项名称显示不完整问题。
- 范围限定为 `IntRuoyiFronted/src/views/system/codex-test-management/index.vue` 与对应静态合同。

## Root Cause

- 测试目标项行使用 `grid-template-columns: 120px 180px 1fr 60px`，但 Element Plus `el-input-number` 默认宽度大于第一列，导致序号控件溢出并挤压后面的目标项名称输入框。

## Fix

- 为目标项序号、名称和目标内容控件添加专用类。
- 将目标项行网格调整为 `112px minmax(220px, 0.8fr) minmax(260px, 1fr) 48px`。
- 设置序号控件 `width: 100%`，设置名称/目标内容输入框 `min-width: 0` 和 `width: 100%`。

## Verification

- RED: `pnpm e2e:system:codex-test-management:static` -> FAIL，失败在新增目标项布局断言。
- GREEN: `pnpm e2e:system:codex-test-management:static` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `git diff --check -- IntRuoyiFronted/src/views/system/codex-test-management/index.vue IntRuoyiFronted/tests/e2e/system-codex-test-management-static.spec.js doc/tasks/20260726-codex-test-target-item-input-display/task.md doc/tasks/20260726-codex-test-target-item-input-display/execution-log.md` -> PASS，仅有 CRLF 工作区提示。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260726-codex-test-target-item-input-display --mode preview` -> PASS，无删除项。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260726-codex-test-target-item-input-display --mode apply` -> PASS，未删除任何文件。

## Risks

- 未启动真实前后端服务，未执行写入型 Runner E2E；本次为窄范围布局修复，使用静态合同和类型检查验证。
- 工作区存在大量非本任务脏改动，且本任务涉及文件已有同文件并行改动；为避免混入其它任务，本任务未执行 commit/push，状态保留为 `ready_for_closeout`。

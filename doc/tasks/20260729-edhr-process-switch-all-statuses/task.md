# eDHR 辅助填写工序切换全状态展示

## Task Goal

红框内“工序”切换弹窗展示当前批次/订单下全部普通工序，不再只展示当前可打开或进行中的任务；不同状态必须可区分，并复用批次执行工序列表的状态背景色口径。

## Milestones

1. 已完成：读取任务、前端、E2E、PowerShell/Git、技能与经验门禁。
2. 已完成：补充 RED 静态合同覆盖全工序展示与状态颜色。
3. 已完成：实现辅助填写页工序切换列表分组、状态标签和背景色的代码草案。
4. 已完成：修复工序任务缺少执行记录/工作任务时的切换入口，改为进入批次详情并选中对应工序。
5. 已完成：运行目标静态合同、相邻切换合同和 `pnpm ts:check`。
6. 待完成：收尾清理、提交并推送。

## Expected Verification

- `node tests/e2e/edhr-assist-process-switch-all-statuses-static.spec.js`
- `node tests/e2e/edhr-assist-fill-mode-static.spec.js`
- `node tests/e2e/edhr-switch-filler-selectability-static.spec.js`
- `node tests/e2e/edhr-switch-filler-formcenter-slot-static.spec.js`
- `pnpm ts:check` 或记录无关阻塞

## Applicable Gates

- 前端静态契约隔离门禁：若全量检查先失败于无关历史问题，使用当前任务聚焦静态合同证明行为。
- eDHR 当前工序运行态展示门禁：运行态黄色只表示当前可执行/当前工序展示，不得放宽 `OPEN_FORM` 或填写权限。
- 前端 Route Query ID 比较门禁：当前选中项继续用 route query ID 语义比较，不得用字符串/数字严格等于。
- 切换填写人 FormCenter 槽位导航门禁：本任务不得破坏 FormCenter 槽位分支和 `assistUserId` 透传。
- Strict No-Fallback：缺少执行记录或工作任务时不得伪造可打开状态；本次使用正式批次详情工序选择入口，不走 mock、默认成功或吞异常。

## Design Constraints Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，复用批次执行状态口径并保持后端 openTask 门禁。
- `是否存在临时补丁或绕过`：否。

## Current Status

ready_for_closeout

## Blocker Resolution

- `doc/tasks/20260729-edhr-fill-workspace-redbox-hide/task.md` 目标要求隐藏 `ExecutionPage.vue` 非追踪填写模式下的辅助填写顶栏。
- 本任务目标要求红框内“工序”切换按钮可用，并且该按钮位于同一辅助填写顶栏。
- 用户随后反馈实际点击工序切换时报错 `工序任务 7169 缺少可查看执行记录或工作任务，不能切换。`，当前修复聚焦该正式入口缺失：缺执行记录/工作任务时进入批次详情并选中对应工序，不再阻断。

## Baseline

- Dirty worktree baseline commit: `d432110f`
- Baseline files: `IntRuoyiFronted/src/utils/edhrWorkTaskNavigation.ts`, `IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue`, `IntRuoyiFronted/src/views/mes/pro/edhr/ExecutionPage.vue`, `IntRuoyiFronted/tests/e2e/edhr-assist-fill-mode-configured-grid-static.spec.js`, `doc/tasks/20260729-edhr-assist-mode-process-form-mismatch/task.md`, `doc/tasks/20260729-edhr-assist-mode-process-form-mismatch/execution-log.md`
- Dirty worktree baseline before navigation fix: `612dc065`

## Cleanup Keep

- doc/tasks/20260729-edhr-process-switch-all-statuses/frontend-feature-evidence.md

## Remaining Blocker

- Push blocked: `git push origin int_main` failed twice with HTTPS/TLS connection reset errors on 2026-07-29. Local implementation and verification are complete, but project completion requires a successful push.

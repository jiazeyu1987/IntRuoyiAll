# Execution Log

## User Intent

用户指出生产组长“活跃订单分配”下拉候选重叠，并询问为什么候选中会出现“未返回订单编号”。

## BDD

- BDD: 活跃订单分配下拉不重叠 -> Given 生产组长打开活跃订单分配弹框 When 活跃订单下拉展示包含编码、产品、数量三行信息的候选 Then 每个候选项必须按内容高度完整展示，不与下一项重叠。
- BDD: 订单编号缺失不伪装 -> Given 活跃订单列表响应缺少正式 `workOrderCode` When 分配下拉渲染候选 Then 前端必须显式暴露正式订单编号缺失，不能用 `workOrderId` 或活跃订单 `id` 当作订单编号。

## Evidence

- Task directory created: `doc/tasks/20260808-active-order-allocation-select-overlap/`
- Read rules: `docs/frontend-development.md`, `docs/task-closeout-rules.md`, `docs/powershell-encoding.md`
- Read experience index: `docs/experience-index.md`
- Read skills: `bug-regression-fix-loop`, `frontend-feature-delivery`

## RED / GREEN / Regression

- RED: `node tests/e2e/team-leader-active-order-option-label-static.spec.js` -> FAIL，预期失败原因：分配活跃订单下拉缺少专属 `popper-class`，多行候选仍套用 Element Plus 单行选项高度。
- GREEN: `node tests/e2e/team-leader-active-order-option-label-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/team-leader-report-allocation-static.spec.cjs` -> PASS。
- GREEN: `node tests/e2e/team-leader-workbench-static.spec.cjs` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `git diff --check -- IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue IntRuoyiFronted/tests/e2e/team-leader-active-order-option-label-static.spec.js doc/tasks/20260808-active-order-allocation-select-overlap` -> PASS，只有 CRLF 工作区提示。
- GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260808-active-order-allocation-select-overlap\bug-regression-evidence.md` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260808-active-order-allocation-select-overlap\frontend-feature-evidence.md` -> PASS。

## Root Cause

- 下拉候选模板已经渲染三行“编码 / 产品 / 数量”，但 `el-option` 默认固定 `height` / `line-height` 仍按单行选项计算，导致多行内容溢出并覆盖下一条候选。
- “未返回订单编号”由 `formatActiveOrderCode()` 输出；它表示当前活跃订单列表响应中的正式 `workOrderCode` 为空。前端此前已禁止用内部 `workOrderId` / 活跃订单 `id` 作为可见订单号兜底，以免掩盖正式数据链路缺失。

## Implementation

- `TeamLeaderWorkbenchPage.vue`：给分配表里的活跃订单 `el-select` 增加 `popper-class="team-leader-workbench__allocation-order-popper"`。
- `TeamLeaderWorkbenchPage.vue`：仅在该专属 popper 中解除 `.el-select-dropdown__item` 默认单行高度，设置 `height: auto`、`line-height: normal`、`min-height: 68px` 和候选分隔线。
- `team-leader-active-order-option-label-static.spec.js`：锁定专属 popper、选项高度解除、候选分隔线和 `:value="order.id"` 提交身份不变。

## Experience Consolidation

- 已按 `project-experience-consolidation` 检查长期经验归宿。
- 现有 `docs/frontend-development.md#复合输入控件交互保留门禁` 已覆盖 `el-select` 候选渲染、正式选择能力和静态合同要求。
- 现有 `docs/frontend-development.md#用户可见描述与内部编码隔离门禁` 已覆盖正式 `workOrderCode` 缺失时不得用内部 ID 兜底。
- 本次未新增长期经验文档。

## Cleanup

- PREVIEW: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260808-active-order-allocation-select-overlap --mode preview` -> PASS，keep `task.md`、`execution-log.md`、`verification-report.md`，delete `bug-regression-evidence.md`、`frontend-feature-evidence.md`。
- APPLY: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260808-active-order-allocation-select-overlap --mode apply` -> PASS，已删除本任务临时 evidence 文件。
- Git: 未执行 commit/push；项目级 Git Policy 规定未获用户明确要求时不提交。

## Current Status

completed：实现、验证、经验归宿检查和 cleanup 均已完成。

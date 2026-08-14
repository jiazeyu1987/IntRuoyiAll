# 20260806 Hide Review Copy Columns

## Task Goal

隐藏截图红框内的列表列：`审核副本` 与 `复核判定`，保留其它表格字段、操作按钮、数据请求和错误暴露链路不变。

## Milestones

- [x] 定位红框对应页面、组件和列定义。
- [x] 先补充最小静态回归合同，证明当前两列仍可见时 RED。
- [x] 做最小前端修复，使目标列表不再渲染 `审核副本` 与 `复核判定`。
- [x] 运行目标合同、相邻验证和结构检查，记录 RED/GREEN/REGRESSION。
- [x] 完成任务文档、验证报告和收尾状态。

## Expected Verification

- `node <target-static-contract>` 先 RED 后 GREEN。
- 受影响前端相邻静态合同通过。
- `pnpm ts:check` 或记录明确阻塞。
- `git diff --check` 通过。

## Applicable Experience Gates

- 多角色共享表格列池隔离：若页面使用列池、显示字段或用户列配置，隐藏字段必须从列定义/列池层移除，不能只靠单个 `v-if` 或 CSS 遮挡。
- 前端截图按钮/红框隐藏类门禁：截图命中的红框内容必须用静态合同先 RED，再最小修改目标调用方；禁止用 CSS 透明、遮挡或删除共享能力冒充不显示。
- 前端列表跨账号默认列布局统一门禁：若存在 `useUserTableColumns` 或 `data-user-table-key`，需要检查默认列集合和稳定 key，避免历史个人列配置继续暴露目标字段。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，计划从正式列定义或渲染入口隐藏目标列。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed

Implementation, required verification, project-experience check, task-closeout-cleanup apply, `int_main` fusion verification, and remote synchronization are complete. `codex/20260806-production-reporting-submit-implementation` is already an ancestor of `int_main`; the merge-base is `b0b38693e6a7b04a3480e8efddcc10405fc48359` and the candidate branch has no actual delta to merge. The target page and focused static contract are tracked by the current `int_main`.

The task-owned fusion evidence commit is `66b0aff29`. Subsequent shared `int_main` synchronization preserved the implementation and focused regression contract; the final branch synchronization is verified during closeout.

## Verification Summary

- RED: `node tests/e2e/team-leader-hide-review-copy-columns-static.spec.cjs` failed before implementation because `审核副本` still rendered in the submission table.
- GREEN: target hide-column static contract passed after removing the two table columns and production default column entries.
- REGRESSION: `pqc-leader-sample-values-detail-only-static`, `mes-process-pool-team-leader-static`, and `pqc-leader-list-fill-form-parity-static` passed.
- STRUCTURE: `pnpm ts:check` passed and `git diff --check` passed.
- CLEANUP: `task_closeout.py --task-id 20260806-hide-review-copy-columns --mode apply` deleted only task-local temporary evidence files and kept `task.md`, `execution-log.md`, and `verification-report.md`.
- FUSION: the candidate branch is an `int_main` ancestor and its merge-base delta is empty, so the requested integration is already present without creating another merge commit.
- PUSH: final `git push origin int_main` and `git rev-list --left-right --count origin/int_main...HEAD` verification confirm that local `int_main` is synchronized.

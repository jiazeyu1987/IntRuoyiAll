# 隐藏一线 PQC 接收标准指标区

## Task Goal

一线 PQC 的“接收标准”弹框仅显示标准说明，不显示截图红框中的下限、上限、单位和精度区域；保留弹框标题、关闭操作、正式接收标准数据来源和检验方法弹框现有行为。

## Milestones

- [x] 建立任务文档并冻结 BDD 验收口径。
- [x] 定位目标组件、现有静态合同和相关经验门禁。
- [x] 先修改聚焦静态合同并取得 RED。
- [x] 删除接收标准指标区并调整为单列布局。
- [x] 运行 GREEN、相邻回归、类型检查和结构校验。
- [x] 完成 evidence 校验、清理预览/应用和最终收尾。

## Expected Verification

- `node tests/e2e/frontline-pqc-fact-dialog-static.spec.cjs`：接收标准弹框保留正式标准说明和关闭操作，且不再渲染下限、上限、单位、精度指标网格；检验方法弹框保持不变。
- `node tests/e2e/frontline-pqc-qa-process-standard-method-source-static.spec.cjs`：接收标准正式字段来源和提交快照映射保持不变。
- `pnpm ts:check`。
- `git diff --check -- src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue tests/e2e/frontline-pqc-fact-dialog-static.spec.cjs`。
- `frontend-feature-delivery` evidence validator。

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，直接移除不应展示的模板节点并为接收标准正文使用明确的单列布局，不采用 CSS 隐藏或数据替代。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- 命中 `docs/backend-development.md#MES PQC 项目级检验快照门禁`：弹框保留正式 `acceptanceStandard` 来源，不用上下限合成文案，也不改结构化提交快照。
- 命中 `docs/frontend-development.md#Element Plus 全屏弹框挂载门禁`：弹框继续位于 `data-pqc-fullscreen-root` 子树内，不改变全屏挂载与关闭行为。
- 命中 `docs/e2e-rules.md#静态合同与真实 E2E 同步门禁`：本任务使用聚焦静态合同记录 RED/GREEN；静态合同结果不会冒充真实 Playwright 页面 E2E。

## Cleanup Keep

- doc/tasks/20260809-hide-pqc-standard-metrics/task.md
- doc/tasks/20260809-hide-pqc-standard-metrics/execution-log.md
- doc/tasks/20260809-hide-pqc-standard-metrics/verification-report.md

## Cleanup Candidates

## Closeout Evidence

- `frontend-feature-delivery` evidence validator 与 self-test 均 PASS，关键结论已归档到 `execution-log.md` 和 `verification-report.md`。
- task-closeout-cleanup 已完成两轮精确删除：临时 evidence/截图目录，以及 10 个本任务 Playwright CLI 文件；未删除并发任务文件。
- 最终空计划 preview/apply 均 PASS：仅保留三份核心任务文档，delete/blocked/warnings 为空。
- 当前为主工作区 `int_main`，未执行 worktree merge/remove；用户未要求 Git 操作，未 stage/commit/push。
- Final verification: PASS，详见 `verification-report.md`。

# 批记录测试历史结果颜色

## Task Goal

调整“批记录测试”五个内部页签的逐行历史按钮颜色：测试成功显示绿色，测试失败显示红色；未产生终态结果时保持灰色，并继续使用正式执行状态作为唯一判断来源。

## Milestones

- [x] 定位逐行历史状态与按钮颜色现状。
- [x] 补充结果颜色静态合同并记录 RED。
- [x] 实现历史按钮按正式结果状态着色。
- [x] 完成聚焦回归、类型检查和证据验证。
- [x] 完成任务清理与收尾。

## Expected Verification

- `node tests/e2e/edhr-batch-record-test-row-history-static.spec.cjs`
- `node tests/e2e/edhr-batch-record-test-tab-run-all-static.spec.cjs`
- `node tests/e2e/batch-record-test-codex-cli-response-static.spec.cjs`
- `pnpm ts:check`
- `git diff --check -- IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchRecordTestPage.vue IntRuoyiFronted/tests/e2e/edhr-batch-record-test-row-history-static.spec.cjs doc/tasks/20260809-batch-record-test-history-result-color`
- `python -X utf8 C:/Users/BJB110/.codex/skills/frontend-feature-delivery/scripts/validate_frontend_feature.py --evidence doc/tasks/20260809-batch-record-test-history-result-color/frontend-feature-evidence.md`

## Current Status

completed

五个页签的历史按钮已统一绑定正式结果颜色 helper；聚焦合同、相邻回归、TypeScript、空白检查、技能证据校验和 cleanup preview/apply 均通过。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。颜色直接复用正式执行状态映射，不新增页面猜测或独立状态副本。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

### 状态显示一致性

- 历史按钮颜色必须来自当前行 `history.data.status`。
- `PASS` 使用绿色；`FAIL`、`BLOCKED`、`TIMEOUT` 使用红色；未完成和取消保持灰色或现有中性语义。
- 五个内部页签统一使用同一 helper，不复制状态判断。

### Experience Gate

- `docs/experience-index.md` 已存在。
- 已将“历史入口颜色必须读取正式结果状态，不能只按 ready 统一变绿”合并到 `docs/e2e-rules.md#测试管理测试节点闭环门禁`，并更新经验索引。

## Cleanup Keep

- doc/tasks/20260809-batch-record-test-history-result-color/task.md
- doc/tasks/20260809-batch-record-test-history-result-color/execution-log.md
- doc/tasks/20260809-batch-record-test-history-result-color/verification-report.md

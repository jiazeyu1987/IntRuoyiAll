# Verification Report: 20260728-edhr-scrap-assist-switch

## Summary
- 修复范围：eDHR 执行页切换填写人选择 FormCenter 表单槽位候选时，改为进入批次详情路线表单抽屉，不再要求传统批记录 `executionId`。
- 根因：辅助切换的批次任务导航没有区分传统批记录和 FormCenter 表单槽位，损耗单候选被导向传统 eDHR 填写页和批记录路线校验。
- 设计约束：未引入 fallback、降级、吞异常、默认成功或 mock 成功。

## Verification
- RED: `node tests\e2e\edhr-switch-filler-formcenter-slot-static.spec.js` -> FAIL，旧实现先要求 `executionId`，缺少 FormCenter 槽位详情页分支。
- GREEN: `node tests\e2e\edhr-switch-filler-formcenter-slot-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\edhr-switch-filler-selectability-static.spec.js` -> PASS。
- GREEN: `node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-assist-filler-switch-snapshot-static.spec.cjs` -> PASS。
- GREEN: `node tests\e2e\edhr-work-task-formcenter-navigation-static.spec.js` -> PASS。
- REGRESSION: `node tests\e2e\edhr-loss-form-open-action-static.spec.js` -> PASS。
- REGRESSION: `node tests\e2e\edhr-dynamic-form-card-preview-static.spec.js` -> PASS。
- REGRESSION: `node tests\e2e\edhr-batch-detail-assist-preview-switch-static.spec.js` -> PASS。
- REGRESSION: `pnpm ts:check` -> PASS after `pnpm install --frozen-lockfile` restored current worktree dependencies.
- REGRESSION: `git diff --check` -> PASS with LF/CRLF warning only.

## Risk
- 当前修复仅改变前端导航分流和自动打开时的 `assistUserId` 透传；传统批记录任务仍走原 `/mes/pro/feedback/edhr-execution/form` 路径。
- 本地 `int_main` 存在并行脏改动和本任务分支历史中存在无关基线提交；最终融合必须使用干净 `origin/int_main` 分支 cherry-pick 本任务提交，避免混入非本任务文件。

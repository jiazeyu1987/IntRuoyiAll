# PQC 检验项目 Tab 显示项目名称

## Task Goal

修复一线 PQC 填写页底部检验项目 tab：红框里的短 tab 标题必须显示正式检验项目名称 `itemName`，不得显示内部项目编码 `itemCode`。

## Milestones

- [x] 记录红框 tab 显示行为和回归场景。
- [x] 增加聚焦静态合同，先证明当前 tab 标题仍可能从 `itemCode` 回退。
- [x] 最小修改 PQC tab 可见标题逻辑，保留 `itemCode` 作为 key 和提交身份。
- [x] 运行目标合同、相邻 PQC 合同和 diff 检查。
- [x] 更新验证报告并执行 closeout 清理。

## Expected Verification

- `node tests/e2e/pqc-tab-item-name-display-static.spec.cjs`
- `node tests/e2e/pqc-active-title-method-display-static.spec.cjs`
- `node tests/e2e/pqc-inspection-tabs-layout-static.spec.js`
- `node tests/e2e/frontline-pqc-formal-submit-static.spec.js`
- `git diff --check -- IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue IntRuoyiFronted/tests/e2e/pqc-tab-item-name-display-static.spec.cjs IntRuoyiFronted/tests/e2e/pqc-active-title-method-display-static.spec.cjs doc/tasks/20260808-pqc-tab-item-name-display`

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；移除 tab 可见标题对 `itemCode` 的展示回退，不新增降级分支。
- `是否从根因和长期维护角度解决`：是；用户可见名称与内部编码分离，提交身份仍保留正式编码。
- `是否存在临时补丁或绕过`：否。

## Applicable Experience Gates

- 用户可见描述与内部编码隔离门禁：tab 可见名称必须使用正式 `itemName`，`itemCode` 只作为 key 和提交身份。
- PQC 项目级检验快照门禁：PQC 项目事实来自正式 QA/PQC 项目快照，不使用固定字段或前端文案替代。
- 前端静态契约隔离门禁：用任务专用静态合同覆盖当前 tab 行为，避免无关大合同阻塞本次修复。

## Verification Result

- PASS: `node tests/e2e/pqc-tab-item-name-display-static.spec.cjs`
- PASS: `node tests/e2e/pqc-active-title-method-display-static.spec.cjs`
- PASS: `node tests/e2e/pqc-inspection-tabs-layout-static.spec.js`
- PASS: `node tests/e2e/frontline-pqc-formal-submit-static.spec.js`
- PASS: `git diff --check -- IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue IntRuoyiFronted/tests/e2e/pqc-tab-item-name-display-static.spec.cjs IntRuoyiFronted/tests/e2e/pqc-active-title-method-display-static.spec.cjs doc/tasks/20260808-pqc-tab-item-name-display`

## Cleanup Keep

- `doc/tasks/20260808-pqc-tab-item-name-display/task.md`
- `doc/tasks/20260808-pqc-tab-item-name-display/execution-log.md`
- `doc/tasks/20260808-pqc-tab-item-name-display/verification-report.md`

## Closeout Result

- PASS: task-closeout-cleanup preview kept only task core records and found no blockers or warnings.
- PASS: task-closeout-cleanup apply completed with no deleted paths; current repository is the main worktree, so no merge or worktree removal was needed.
- Experience consolidation: existing `docs/frontend-development.md#用户可见描述与内部编码隔离门禁` already covers this reusable lesson; skipped editing dirty long-term docs to avoid mixing unrelated changes.

# PQC 红框 Tab 显示检验方法

## Task Goal

修复一线 PQC 填写页红框检验项目 tab 和检验方法弹窗：用户看到的可见主标题必须显示正式检验方法名称，例如 `目视检验`，不得显示内部检验项目代码或英文项目名 `AO5 final inspection`。

## Milestones

- [x] 记录红框 tab 仍显示 `AO5 final inspection` 的回归场景。
- [x] 增加聚焦静态合同，先证明红框 tab 仍从项目名回退。
- [x] 最小修复红框 tab 与检验方法弹窗，统一显示正式检验方法名称。
- [x] 运行目标合同、相邻 PQC 合同和 diff 检查。
- [x] 更新验证报告并执行 closeout 清理。

## Expected Verification

- `node tests/e2e/pqc-tab-method-display-static.spec.cjs`
- `node tests/e2e/pqc-active-title-method-display-static.spec.cjs`
- `node tests/e2e/pqc-tab-item-name-display-static.spec.cjs`
- `node tests/e2e/pqc-inspection-tabs-layout-static.spec.js`
- `node tests/e2e/frontline-pqc-formal-submit-static.spec.js`
- `git diff --check -- IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue IntRuoyiFronted/tests/e2e/pqc-tab-method-display-static.spec.cjs IntRuoyiFronted/tests/e2e/pqc-active-title-method-display-static.spec.cjs IntRuoyiFronted/tests/e2e/pqc-tab-item-name-display-static.spec.cjs doc/tasks/20260808-pqc-active-title-runtime-method-display`

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；红框 tab 不再用项目名、项目编码或 key 作为检验方法显示回退。
- `是否从根因和长期维护角度解决`：是；用户可见主标题统一绑定 `formatPqcMethodSummary(item)`，内部 `itemCode/itemName/inspectionMethod` 仍分别保留。
- `是否存在临时补丁或绕过`：否。

## Applicable Experience Gates

- 用户可见描述与内部编码隔离门禁：用户可见标题必须显示正式名称，编码仅用于身份。
- 前端静态契约隔离门禁：用任务专用静态合同覆盖当前红框 tab 行为，避免相邻大合同误判。

## Verification Result

- PASS: `node tests/e2e/pqc-tab-method-display-static.spec.cjs`
- PASS: `node tests/e2e/pqc-active-title-method-display-static.spec.cjs`
- PASS: `node tests/e2e/pqc-tab-item-name-display-static.spec.cjs`
- PASS: `node tests/e2e/pqc-inspection-tabs-layout-static.spec.js`
- PASS: `node tests/e2e/frontline-pqc-formal-submit-static.spec.js`
- PASS: `git diff --check -- IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue IntRuoyiFronted/tests/e2e/pqc-tab-method-display-static.spec.cjs IntRuoyiFronted/tests/e2e/pqc-active-title-method-display-static.spec.cjs IntRuoyiFronted/tests/e2e/pqc-tab-item-name-display-static.spec.cjs doc/tasks/20260808-pqc-active-title-runtime-method-display`

## Cleanup Keep

- `doc/tasks/20260808-pqc-active-title-runtime-method-display/task.md`
- `doc/tasks/20260808-pqc-active-title-runtime-method-display/execution-log.md`
- `doc/tasks/20260808-pqc-active-title-runtime-method-display/verification-report.md`

## Closeout Result

- PASS: task-closeout-cleanup preview kept only core task records and found no blockers or warnings.
- PASS: task-closeout-cleanup apply completed with no deleted paths; current repository is the main worktree, so no merge or worktree removal was needed.
- Experience consolidation: existing `docs/frontend-development.md#用户可见描述与内部编码隔离门禁` already covers this reusable lesson; skipped editing dirty long-term docs to avoid mixing unrelated changes.

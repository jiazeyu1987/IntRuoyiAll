# QA 规程末检开关

## Task Goal

将 QA 规程配置页从独立“检验规则”配置改为固定首检、上午巡检、下午巡检必检，仅在“工序检验方法与抽样方案”工具栏保留“是否需要末检”开关，并保持保存/发布 payload 的末检适用性字段可追溯。

## Milestones

- [x] M1 记录 BDD/TDD 验收口径并建立最小静态契约。
- [x] M2 修改 QA 规程配置页 UI 与末检规则状态绑定。
- [x] M3 运行目标静态合同、相邻合同和 diff 检查。
- [x] M4 完成收尾记录与清理检查。

## Expected Verification

- `node tests/e2e/qa-regulation-final-inspection-switch-static.spec.cjs`
- `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs`
- `node tests/e2e/qa-regulation-final-applicability-static.spec.cjs`
- `node tests/e2e/qa-regulation-publish-tab-hidden-static.spec.cjs`
- `git diff --check -- IntRuoyiFronted/src/views/mes/pro/processpool/QaRegulationPage.vue IntRuoyiFronted/tests/e2e/qa-regulation-final-inspection-switch-static.spec.cjs IntRuoyiFronted/tests/e2e/qa-regulation-final-applicability-static.spec.cjs IntRuoyiFronted/tests/e2e/qa-regulation-publish-tab-hidden-static.spec.cjs IntRuoyiFronted/tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs doc/tasks/20260806-qa-final-inspection-switch/task.md doc/tasks/20260806-qa-final-inspection-switch/execution-log.md`
- `pnpm ts:check`：执行但被无关 `TeamLeaderWorkbenchPage.vue` 缺少 PQC helper 函数阻塞。

## Applicable Gates

- 前端静态契约隔离门禁：本任务先新增专用最小静态合同 RED/GREEN，再运行相邻 QA 合同。
- Element Plus 选择框显示门禁：新增 switch 需有专用布局类和稳定选择器，避免状态提示挤压或不可见。
- PQC 末检适用性必须有发布规程依据：末检开关只驱动正式 `finalInspectionApplicable`，关闭时仍要求非空不适用依据；不得用默认 false 或空规则替代正式依据。

## Design Constraint Check

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，移除多余规则配置入口并保留正式末检适用性字段。
- 是否存在临时补丁或绕过：否。

## Cleanup Candidates

- doc/tasks/20260806-qa-final-inspection-switch/frontend-feature-evidence.md

## Current Status

ready_for_closeout

- cleanup preview/apply 已通过并删除临时 rontend-feature-evidence.md。
- 提交/推送未执行：当前 int_main 落后 origin/int_main 7 个提交，且工作区有大量并发脏改动；为避免混入其它任务，保持待提交状态。

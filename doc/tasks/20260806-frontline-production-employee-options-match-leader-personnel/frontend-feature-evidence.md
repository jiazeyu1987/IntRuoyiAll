# Frontend Feature Evidence

## Feature Goal

一线生产填写页员工弹窗的候选员工必须与当前生产组长“人员管理”列表一致。

## Non-Goals

- 不改变生产组长人员管理的新增、禁用、启用、改名或审计逻辑。
- 不新增后端接口兜底，不改生产人员档案正式范围。
- 不调整一线生产页面布局、全屏或填写提交逻辑。

## Requirements

- `REQ-FP-EMP-001`：一线生产员工弹窗使用生产组长人员管理正式列表来源。
- `REQ-FP-EMP-002`：员工弹窗不得使用全量系统用户、设备员工候选或本地过滤兜底。
- `REQ-FP-EMP-003`：员工选择后仍走现有正式切换员工接口和模板校验。

## Acceptance

- `AC-FP-EMP-001`：生产组长人员管理列表使用 `/employee-profile/list`，一线生产运行配置员工来源与当前生产组长启用人员档案一致。
- `AC-FP-EMP-002`：运行配置不再按工序员工绑定限定员工弹窗候选，禁用人员和其它组长人员不进入候选。
- `AC-FP-EMP-003`：一线生产员工切换接口仍使用运行配置校验，不能出现前端可选但后端校验不同源。

## UI Entry Points

- 一线生产填写页：`IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue`
- 生产组长人员管理：`IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue`

## API Contracts

- 生产组长人员管理列表：`getProductionPersonnelList` -> `/mes/pro/process-pool/team-leader/employee-profile/list`
- 一线生产员工切换：`switchFrontlineActualEmployee` 保持原链路。

## BDD Scenarios

- BDD: 一线生产员工弹窗复用生产组长人员管理列表 -> Given 当前生产组长已在人员管理维护生产人员 When 一线生产填写页点击员工 Then 弹窗候选员工只来自同一生产组长启用人员档案。
- BDD: 禁止全量或设备候选兜底 -> Given 一线生产页面打开员工弹窗 When 正式生产人员列表接口不可用或未加载 Then 页面不得改用全量系统用户、设备候选或本地猜测结果兜底。

## RED

- RED: `node tests\e2e\edhr-frontline-production-employee-options-match-leader-personnel-static.spec.cjs` failed because runtime config did not derive employee popup options from current leader personnel profiles.
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineRuntimeConfigServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` failed because old runtime config returned only the process-bound employee.

## GREEN

- GREEN: `node tests\e2e\edhr-frontline-production-employee-options-match-leader-personnel-static.spec.cjs` passed.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineRuntimeConfigServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` passed.

## Verification

- `node tests\e2e\edhr-frontline-fill-tabs-static.spec.cjs` passed.
- `node tests\e2e\frontline-team-config-static.spec.cjs` passed.
- `node tests\e2e\production-personnel-management-static.spec.cjs` passed.
- `node tests\e2e\team-leader-workbench-static.spec.cjs` passed.
- `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineEmployeeSwitchServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` passed.
- `pnpm ts:check` passed.
- `git diff --check -- <task-owned files>` passed.

## Blockers

- Commit/push closeout is pending because the workspace contains extensive unrelated dirty files from other tasks.

# Execution Log

## User Intent

用户要求：末检只有 QA 里对应产品的末检 switch 开关打开时才可以显示选择；关闭时隐藏不显示。

## BDD

- `BDD: QA末检开关开启显示末检 -> Given 对应产品发布态 QA 规程 finalInspectionApplicable=true / When 一线 PQC 进入该产品待检工序 / Then 首检、巡检、末检都可见且末检可选择`
- `BDD: QA末检开关关闭隐藏末检 -> Given 对应产品发布态 QA 规程 finalInspectionApplicable=false 且有不适用依据 / When 一线 PQC 进入该产品待检工序 / Then 末检按钮不渲染且 selectPqcInspectionType('FINAL') 不会切到末检`

## Gate Evidence

- 已读取 `docs/task-closeout-rules.md`、`docs/frontend-development.md`、`docs/powershell-encoding.md`、`docs/e2e-rules.md`。
- 已读取经验索引并命中 `docs/backend-development.md#mes-pqc-项目级检验快照门禁` 中的 PQC 末检适用性门禁。

## RED/GREEN

- RED: `node tests/e2e/frontline-pqc-final-inspection-switch-static.spec.cjs` -> FAIL，后端一线 PQC 工序响应未暴露发布态 QA 规程的 `finalInspectionApplicable`，前端无法按 QA 末检开关控制末检按钮。
- GREEN: `node tests/e2e/frontline-pqc-final-inspection-switch-static.spec.cjs` -> PASS，静态合同确认后端响应/候选对象/控制器映射/正式版本读取/前端类型/按钮 `v-if`/选择函数保护均以 `finalInspectionApplicable === true` 为准。

## Implementation Evidence

- 后端 `MesFrontlineRouteProcessRespVO` 增加 `finalInspectionApplicable`，`MesFrontlineRouteProcessCandidate` 携带该字段，`MesFrontlineDeviceAccountController` 将 candidate 字段映射到响应。
- 后端 `MesFrontlinePqcContextServiceImpl` 读取 `versionMapper.selectById(task.getRegulationVersionId())`，要求发布版本 `finalInspectionApplicable` 非空；关闭末检时必须有 `finalInspectionNotApplicableReason`，并把正式字段传入工序候选。
- 前端 `FrontlineDeviceRouteProcessVO` 增加 `finalInspectionApplicable?: boolean`，`FrontlineFixedTemplatePanel.vue` 只在 `selectedProcess?.finalInspectionApplicable === true` 时渲染“末检”，且 `selectPqcInspectionType('FINAL')` 在未开启时直接拒绝。

## Verification

- GREEN: `node tests/e2e/frontline-pqc-final-inspection-switch-static.spec.cjs` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `mvn -pl yudao-module-mes -am "-DskipTests" compile` -> PASS。
- GREEN: `git diff --check` -> PASS，仅输出既有 LF/CRLF 提示，无 whitespace error。
- BLOCKED: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL during `testCompile`，MES 模块既有测试源缺失多组类，例如 `MesTeamLeaderReportConfirmationReqBO`、`MesProEdhrReleaseDossierRequirementSettingService`、`MesProRouteProcessService`，导致无法进入目标单测执行。
- BLOCKED: `node src/views/mes/pro/feedback/frontline-template-render.spec.cjs` -> FAIL，既有断言 `production UI must have a no-device full-width layout` 在第 39 行失败，未进入本次末检控制断言。

## Experience Consolidation

- 已按项目收尾基线检查经验沉淀需求；本次未产生新的通用长期经验，已命中的规则仍为现有 `docs/backend-development.md#mes-pqc-项目级检验快照门禁`，不新建经验文档。

## Closeout

- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260808-frontline-final-inspection-switch-visibility --mode preview` -> READY，keep 仅包含 `task.md`、`execution-log.md`、`verification-report.md`，delete/blocked/warnings 均为空。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260808-frontline-final-inspection-switch-visibility --mode apply` -> APPLIED，未删除任何文件。

## Current Status

completed

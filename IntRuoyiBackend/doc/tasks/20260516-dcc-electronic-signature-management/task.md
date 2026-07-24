# Task: DCC 电子签名管理与审批签名留痕

## Goal

在 DCC 文控中心页签下新增“电子签名管理”页签，功能实现对齐
`D:\ProjectPackage\Int\IntAuth` 中已有的电子签名管理能力，并把 DCC
范围内所有审批动作收口为必须经过电子签名确认且全量留痕记录。

## Scope

- 在 `ruoyi-vue-pro` 仓库创建本任务文档、执行日志和前后端交付证据。
- 对照 `IntAuth` 电子签名管理实现，梳理可复用的前端页签、后端接口、
  签名校验与签名记录模型。
- 在 DCC 文控中心增加电子签名管理页签，并保持前端风格符合
  `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`。
- 为 DCC 审批相关后端流程增加电子签名前置校验与签名记录落库/查询。
- 为新增或变更的可观察行为补充 RED -> GREEN 测试与真实用户路径验证。
- 不引入 silent fallback、mock success 或绕过签名的兼容分支。

## Previous Task Check

- Previous backend/frontend task:
  `doc/tasks/20260516-oauth2-access-token-runtime-fallback/task.md`
- Status before this task: blocked by user reprioritization.
- Impact: the OAuth2 runtime fallback task remains paused and does not share
  scope with this DCC electronic signature delivery.

## Milestones

- [x] M1: Create this task package and record the requested DCC signature scope.
- [x] M2: Inspect IntAuth electronic-signature implementation and DCC approval
  touchpoints, then refine the delivery plan and BDD scenarios.
- [x] M3: Add RED coverage for DCC approval signature enforcement and signature
  record persistence.
- [x] M4: Implement the backend signature management integration for DCC.
- [x] M5: Implement the DCC document-center electronic-signature management tab
  and approval interaction changes.
- [x] M6: Run GREEN verification, real-browser validation, and commit only this
  task's files if verification fully passes.

## Expected Verification

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc,yudao-module-bpm -Dtest=DccBaseSchemaTest,DccControlledFileSignatureServiceTest,DccElectronicSignatureAuthorizationServiceTest,DccElectronicSignatureManagementServiceTest,BpmTaskExternalSignatureGuardTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `pnpm --dir D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 exec eslint src/api/dcc/controlledFile/signatures.ts src/views/dcc/controlled-file/signatures/index.vue src/views/bpm/processInstance/detail/ProcessInstanceOperationButton.vue`
- `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-electronic-signature-management run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-electronic-signature-management\scripts\verify-dcc-electronic-signature-management.mjs`

## Current Status

Completed for implementation and verification. Backend signature
authorization/query APIs, BPM generic-approval guarding, frontend DCC
signature-management page, BPM-page redirect hint, and the repo-wide frontend
type-baseline repairs all landed and the requested DCC signature path now works
end to end on the local runtime.

## Blocker And Impact

- Blocker: none at task creation time.
- Impact: until this task is complete, DCC approvals can still bypass the
  requested mandatory electronic-signature control and centralized signature
  record trail.

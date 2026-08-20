# Verification Report

## Scope

- 本次仅验证并准备提交 `IntRuoyiBackend` 与 `IntRuoyiFronted` 下的前后端源码、测试和配置变更。
- 根目录规则、历史任务产物、资源文件、迁移包和其它非前后端代码文件不纳入本次暂存范围。
- 本次执行本地 commit，不执行 push。

## Verification Results

- PASS: `git branch --show-current` -> `int_main`。
- PASS: `git remote -v` -> `origin https://github.com/jiazeyu1987/IntRuoyiAll.git`。
- PASS: `git diff --check -- IntRuoyiBackend IntRuoyiFronted` -> 退出码 0；仅有 Git LF/CRLF 提示，无空白错误。
- PASS: `scripts\preflight\branch-runtime-port-guard.ps1` -> `int_main/int_main` 前端 `8081`、后端 `48081` 端口守卫通过。
- PASS: `node yudao-module-mes\src\test\js\mes-pqc-task-generation-static.spec.cjs`。
- PASS: `node tests/e2e/frontline-pqc-continuous-submit-static.spec.cjs`。
- PASS: `node tests/e2e/frontline-pqc-formal-submit-static.spec.js`。
- PASS: `node tests/e2e/mes-frontline-pqc-submit-to-leader-chain-static.spec.js`。
- PASS: `node tests/e2e/qa-regulation-current-published-version-static.spec.js`。
- PASS: `node tests/e2e/production-report-overage-conflict-static.spec.cjs`。
- PASS: `node --check tests/e2e/frontline-active-order-submit-allocation-real.e2e.js`。
- PASS: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest,MesTeamLeaderActiveOrderDetailServiceImplTest,MesTeamLeaderActiveOrderReleaseProcessInspectionQaProvenancePortImplTest,MesTeamLeaderBatchRecordBackfillServiceTest,MesTeamLeaderOrderProcessCompletionServiceTest,MesTeamLeaderReportConfirmationServiceTest,MesProductionReleaseApplySp1Test,MesQaInspectionRegulationServiceTest,MesQaInspectionRegulationWordImportServiceTest,MesFrontlinePqcEmployeeSwitchServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> 148 tests, 0 failures, 0 errors, BUILD SUCCESS。
- PASS: `node src/views/mes/pro/feedback/frontline-pqc-fullscreen-layout.spec.cjs` -> 提交后补充验证通过。
- PASS: 追加前端合同复跑：`frontline-pqc-continuous-submit-static.spec.cjs`、`frontline-pqc-formal-submit-static.spec.js`、`mes-frontline-pqc-submit-to-leader-chain-static.spec.js`、`qa-regulation-current-published-version-static.spec.js`、`production-report-overage-conflict-static.spec.cjs`、`node --check tests/e2e/frontline-active-order-submit-allocation-real.e2e.js`。
- PASS: `git diff --check -- IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue`。
- PASS: `git diff --cached --check` for code commit `3805912ea` and fix commit `a1e24fd7e`。
- PASS: `task-closeout-cleanup` preview/apply -> kept `task.md`、`execution-log.md`、`verification-report.md`; deleted temporary `bug-regression-evidence.md`。

## Commits

- `3805912ea chore: 提交前后端代码`
- `a1e24fd7e fix: 收窄PQC全屏布局留白`

## Residual Not Submitted

- `IntRuoyiFronted/tests/e2e/dcc-project-route-governance-static.spec.js`：提交后出现的残余修改，当前 `node tests/e2e/dcc-project-route-governance-static.spec.js` 失败于 “不得继续展示 损耗单 状态列”，未提交。
- `IntRuoyiFronted/tests/e2e/frontline-pqc-process-navigation-buttons-static.spec.cjs`：提交后出现的未跟踪测试，当前失败于缺少 `handleNavigatePqcProcess`，未提交。

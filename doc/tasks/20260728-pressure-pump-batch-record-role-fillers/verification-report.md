# Verification Report

## Status

PASS for implementation and runtime verification; repository closeout remains pending because the shared `int_main` workspace is behind `origin/int_main` by 22 commits and contains unrelated dirty changes.

## Scope

- Tenant: `芋道源码`, `tenant_id=1`.
- Product/batch record: `球囊扩张压力泵`.
- Batch record version: `V14.0`, `batchRecordVersionId=130`.
- Target forms: 15 `MAIN` batch record forms.

## Evidence

- Backend unit regression: `mvn -pl yudao-module-mes "-Dtest=MesProEdhrProcessFormPermissionRuleServiceImplTest" test` -> PASS, `Tests run: 33, Failures: 0, Errors: 0`.
- Backend package: `mvn -pl yudao-server -am "-DskipTests" package` -> PASS.
- Runtime reload: `restart-int-ruoyi-local.ps1 -Component backend -WorktreeName int_main` -> PASS; health `http://127.0.0.1:48081/actuator/health` -> `UP`.
- Runtime jar: `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260729-001727.jar`, SHA256 `91583596BAFA1979F385279430DE448D6137A38D00C238B86354D49B454D00AB`.
- API verification: `python -X utf8 doc\tasks\20260728-pressure-pump-batch-record-role-fillers\verify_pressure_pump_role_fillers.py --verify` -> PASS, `reports=15 roles=15 usersPerRole=3 apiVerified=15`.
- UI verification: `node doc\tasks\20260728-pressure-pump-batch-record-role-fillers\pressure_pump_role_filler_ui_readonly.e2e.js` -> PASS, target row and dialog show `粗洗工序填写者角色`.
- Backend API evidence validator: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260728-pressure-pump-batch-record-role-fillers/backend-api-evidence.md` -> PASS.
- Closeout cleanup: preview/apply -> PASS, no task-owned files deleted.
- Final repository check: `git diff --check` -> PASS with line-ending normalization warnings only; `git status --short --branch` still reports unrelated dirty changes and `int_main...origin/int_main [behind 22]`.

## Result

- 15/15 `MAIN` 批记录表单的 form-level `FILL` 规则均为 `candidateSourceType=ROLE`。
- 15/15 角色均存在且每个角色绑定 3 个当前租户启用普通账号。
- 登录态 `/admin-api/mes/pro/edhr-process-form-permission-rule/get-by-report` 对 15 张表单均返回对应 `candidateSourceNames`。
- 真实前端批记录表单列表和“批记录表单填写人设置”弹窗可见角色名称。

## Residual Risk

- 未执行提交和推送；当前共享工作区有无关脏改动并且本地分支落后远端 22 个提交，需在单独收口时处理。

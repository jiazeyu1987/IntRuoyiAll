# Execution Log: 修复瑛泰医疗公司信息缺少 companyType

BDD: 瑛泰医疗公司信息可打开 -> Given 瑛泰医疗 admin 已登录 When 访问 `/showroom/company` Then 公司信息工作台正常显示且不出现 `companyType` 缺失错误

BDD: 公司接口满足前端契约 -> Given 瑛泰医疗租户存在公司当前数据 When 请求 `/admin-api/showroom/company/current` Then 响应 `data.companyType` 为字符串

BDD: 源租户与既有账号不受影响 -> Given 本任务仅修复瑛泰医疗公司数据 When 修复完成 Then `芋道源码/admin` 与 `瑛泰医疗/yingtai` 仍可登录

## Bug

使用 `瑛泰医疗 / admin / admin123` 访问前端 `/showroom/company` 时，页面提示 `公司工作台缺少字符串字段：companyType`。

## Expected

公司信息工作台可打开；即使瑛泰医疗租户尚无公司版本数据，`/admin-api/showroom/company/current` 也必须返回字符串字段 `companyType`、`displayName`、`displayNameEn`，前端不得收到 `null`。

## Reproduction

RED: `GET http://127.0.0.1:48081/admin-api/showroom/company/current` with `tenant-id=162` and `瑛泰医疗/admin` token -> FAIL, response contained `companyId=0` and `companyType=null`.

RED: `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomHttpApiIntegrationTest#companyCurrentShouldReturnFrontendContractStringsWhenTenantHasNoCompanyRevision" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected empty string but was `null`.

## Root Cause

`ShowroomApiRuntime#getCompanyCurrent()` 在没有公司版本数据的空态分支返回 `new CompanyCurrentRespVO(..., null, null, null, false)`；前端 `showroom-admin/company/contracts.ts` 对 `companyType` 使用 `expectString(record.companyType, 'companyType', true)`，允许空字符串但不允许 `null`。这说明问题不是两个租户程序不同，而是瑛泰医疗租户缺少公司版本数据时触发了接口空态契约缺陷。

## Fix

将公司当前接口空态分支改为返回空字符串 `""`，保持前端严格契约，不增加 fallback，不复制芋道源码租户业务数据。

## Regression Test

新增 `ShowroomHttpApiIntegrationTest#companyCurrentShouldReturnFrontendContractStringsWhenTenantHasNoCompanyRevision`，覆盖 `tenant_id=162` 无公司版本数据时的空态响应契约。

## GREEN

GREEN: `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomHttpApiIntegrationTest#companyCurrentShouldReturnFrontendContractStringsWhenTenantHasNoCompanyRevision" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`。

GREEN: rebuilt backend from clean temporary worktree with only this code fix using `mvn -pl yudao-server -am "-DskipTests" package` -> PASS, backend restarted on `48081`, `/actuator/health` returned `UP`。

GREEN: `瑛泰医疗/admin` API login then `GET /admin-api/showroom/company/current` -> PASS, response `companyId=0 companyType='' displayName='' displayNameEn='' live=false`。

GREEN: Playwright real frontend path `http://localhost:8081/login` -> login `瑛泰医疗/admin/admin123` -> open `/showroom/company` -> PASS, page did not show `公司工作台缺少字符串字段：companyType` and frontend response contained `companyType=''`。

## Verification

REGRESSION: `瑛泰医疗/admin` login -> PASS, roles `common,super_admin,showroom_publicity`。

REGRESSION: `瑛泰医疗/yingtai` login -> PASS, roles `tenant_admin`。

REGRESSION: `芋道源码/admin` login -> PASS, roles `common,super_admin,showroom_publicity`。

The frontend dev server was restarted with Vite `--force` after it returned `504 Outdated Optimize Dep`; this was an environment cache issue needed before the real browser path could render.

## Risk and Regression Scope

风险范围限定在展厅公司当前接口没有公司版本数据的空态分支。已有公司版本数据的租户继续走原有 `toCompanyCurrentResp` 映射；本任务没有写入、删除或复制任何公司/产品/展柜业务数据。

## Blockers

Blockers: none.

## Closeout

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence ...\execution-log.md` -> PASS, bug regression evidence is valid.

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260531-yingtai-companytype-company-workbench --mode preview` -> PASS, only one-off `e2e-companytype-smoke.cjs` selected for deletion.

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260531-yingtai-companytype-company-workbench --mode apply` -> PASS, one-off E2E helper removed; task records retained.

# DF04 Independent Test Report

## Result

**PASS**

DF04 implements `requireEnabledByRoute(routeId)` with the formal route-DCC relation and the directly referenced DCC project ID only. The reviewed change fails fast for invalid route identity, missing or duplicate formal relations, invalid relation rows, and missing, deleted, disabled, or cross-tenant projects. No production write, fallback, schema, port, product/material/QA/form/process inference, or unrelated production change was found.

## Findings

No Critical, High, Medium, or Low findings.

## Reviewed Scope

- Baseline: `HEAD` / `int_main` at `5d503ea5e5d754461031ddb4271e5db77c2c4d91`.
- Production diff: new `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/DccProjectResolver.java`.
- Test diff: new `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlineDccProjectResolverTest.java`.
- Task evidence: `task.md`, `execution-log.md`, `verification-report.md`, and `backend-api-evidence.md` under this task directory.
- Contract sources: supervisor `prd.md`, `dev-plan.md`, `test-plan.md`, and approved `DF04-unique-dcc-project.md`.
- The complete untracked file content was reviewed with `git diff --no-index` because the DF04 implementation had not yet been staged or committed.

## Contract Coverage

| Requirement | Independent evidence | Result |
| --- | --- | --- |
| Resolve by formal route-DCC relation | Resolver performs a route-ID equality query on `MesRouteDccProjectBindingMapper`; no alternate relation source exists in the class | PASS |
| Use direct DCC project ID | The sole binding's `dccProjectCodeId` is passed directly to `DccProjectCodeMapper.selectById` | PASS |
| Return exact identity | Success test asserts `dccProjectCodeId`, `projectCode`, and `projectName` | PASS |
| Missing relation | Dedicated test asserts stable required-binding error and no project lookup | PASS |
| Duplicate relation | Dedicated test supplies two current rows and asserts stable ambiguous-binding error without choosing either project | PASS |
| Deleted/cross-tenant relation | Dedicated tests cover deleted and cross-tenant relation handling before project lookup; production guard also rejects null/non-positive direct project IDs | PASS |
| Missing/deleted/disabled/cross-tenant project | Dedicated tests cover all four cases with the same invalid-reference semantic | PASS |
| Tenant isolation | Current tenant is required; relation and project tenant IDs are validated; mapper access does not bypass tenant interception | PASS |
| No inference | Dependency and source scan found no product code, material code, product master, QA regulation, form binding, route name, or process lookup | PASS |
| Read only / no schema or ports | Resolver calls only two read mapper methods; changed-path inventory contains only resolver, test, and task evidence files | PASS |

## Commands And Results

### Target Maven Test

```powershell
mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesFrontlineDccProjectResolverTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

- Exit code: `0`
- Maven result: `BUILD SUCCESS`
- Surefire: `Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`

### DF02 + DF03 + DF04 Combined Regression

```powershell
mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesFrontlineActiveOrderSnapshotResolverTest,MesRouteDccProjectBindingServiceTest,MesRouteDccProjectBindingControllerTest,MesFrontlineDccProjectResolverTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

- Exit code: `0`
- Maven result: `BUILD SUCCESS`
- Aggregate Surefire: `Tests run: 25, Failures: 0, Errors: 0, Skipped: 0`
- `MesFrontlineActiveOrderSnapshotResolverTest`: 5 / 0 / 0 / 0
- `MesRouteDccProjectBindingServiceTest`: 7 / 0 / 0 / 0
- `MesRouteDccProjectBindingControllerTest`: 3 / 0 / 0 / 0
- `MesFrontlineDccProjectResolverTest`: 10 / 0 / 0 / 0

### Forbidden-Inference And Write Scan

```powershell
rg -n -i '\b(materialCode|productMasterId|formBindings|routeName|qaRegulation|inspectionRegulation|processId|processMapper|productMapper|materialMapper|formMapper)\b' IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/DccProjectResolver.java
rg -n '\.(insert|update|updateById|delete|deleteById|save|remove)\s*\(' IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/DccProjectResolver.java
```

- Forbidden inference: `0` matches, PASS.
- Write calls: `0` matches, PASS.
- Positive source inspection confirmed only the route binding query and direct `selectById(dccProjectCodeId)` lookup.

### Diff Quality And Scope

```powershell
git diff --check HEAD
git diff --no-index --check -- NUL IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/DccProjectResolver.java
git diff --no-index --check -- NUL IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlineDccProjectResolverTest.java
git diff --name-only HEAD
git ls-files --others --exclude-standard
```

- Repository `git diff --check HEAD`: exit `0`, PASS; because the two implementation files were untracked, the following `--no-index --check` commands supplied their explicit whitespace coverage.
- Both untracked-file diff checks: no whitespace errors, PASS (`git diff --no-index` returns `1` because each file differs from `NUL`).
- Changed-path inventory: only the two DF04 code/test files and this task's evidence directory; no schema, configuration, port, frontend, or unrelated production file.
- Git emitted only the existing LF-to-CRLF working-copy warning; it reported no whitespace defect.

### Backend Evidence Validator

```powershell
python -X utf8 C:/Users/BJB110/.codex/skills/backend-api-delivery/scripts/validate_backend_api.py --evidence doc/tasks/20260812-frontline-pqc-dcc-qa-df04/backend-api-evidence.md
```

- Exit code: `0`
- Result: `Backend API evidence is valid.`

## Residual Risks

- Tests are mapper-mocked unit/contract tests. Actual database tenant and logic-delete filtering is inherited from the existing MyBatis/TenantBaseDO infrastructure and was not integration-tested against a live database in DF04 scope; the explicit post-read tenant/deleted checks still fail fast if an unexpected row reaches the resolver.
- `DccProjectResolver` is intentionally not yet exercised through its future DF06 consumer. That integration remains a downstream task and does not block the isolated DF04 contract.
- The implementation and evidence remain uncommitted/untracked at the time of this report. Commit and merge readiness are supervisor responsibilities and were not part of this independent test authorization.

## Gate Decision

DF04 is independently verified and may proceed to supervisor review/commit/merge. No production code was changed by the independent tester.

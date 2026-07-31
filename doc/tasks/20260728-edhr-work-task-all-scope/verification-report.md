# Verification Report

## Summary

已修复 `scopeKey=ALL` 普通批记录填写人规则在创建 eDHR 批次执行时无法生成工作任务责任范围快照的问题。修复从正式批记录报表成员生成整表 `ranges`，不读取表单槽位 `formBindings`，不复用工序开始配置，也不对缺失报表元数据做默认成功。

## Changed Scope

- `MesProEdhrWorkTaskServiceImpl`: 普通批记录 `ALL` 规则缺少显式 `fillableScopeJson` 时，按正式批记录定义和版本读取 `MesProBatchRecordReportDO.sourceTableIndex` 并生成整表范围。
- `MesProEdhrWorkTaskServiceImplTest`: 增加成功路径和缺少正式报表成员 fail-fast 回归测试。

## Verification Commands

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrWorkTaskServiceImplTest#createInitialFillTask_buildsAllScopeSnapshotFromReportMembersWhenRuleScopeIsBlank" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, `ServiceException: eDHR 工作任务责任范围快照无效：scopeKey=ALL`
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrWorkTaskServiceImplTest#createInitialFillTask_buildsAllScopeSnapshotFromReportMembersWhenRuleScopeIsBlank" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 1 test
- REGRESSION: `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrWorkTaskServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 71 tests, 0 failures, 0 errors

## Design Constraints

- No fallback/degradation/exception swallowing introduced.
- Dynamic route form slots continue to use the frozen task `fillableScopeJson`.
- Missing formal report definition, version, report members, or `sourceTableIndex` still fails fast with `PRO_EDHR_WORK_TASK_RESPONSIBILITY_SCOPE_INVALID`.

## Closeout Status

Task implementation and verification are complete. Final closeout is pending because the current branch has non-task local commits ahead of `origin/int_main`; pushing or marking completed must not mix unrelated task ownership.

## Runtime Reload Verification

- PASS: `mvn -pl yudao-server -am "-DskipTests" package`
- PASS: stopped old local backend PID `42652` and started new local backend PID `42800` on `48081`
- PASS: runtime Jar `output\runtime\int_main\backend-runtime-control-20260728-100956.jar` SHA256 matches `yudao-server\target\yudao-server-exec.jar`
- PASS: `http://127.0.0.1:48081/actuator/health` returned `UP`

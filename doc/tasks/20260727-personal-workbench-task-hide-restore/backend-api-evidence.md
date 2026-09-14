# Backend API Evidence

## Scope

- Endpoint scope: `GET /system/profile-workbench-task-visibility/hidden-keys`, `PUT /system/profile-workbench-task-visibility/hide`, `DELETE /system/profile-workbench-task-visibility/restore`.
- Service scope: current-login-user task visibility persistence under `yudao-module-system`.
- Data contract: `system_profile_workbench_task_visibility` stores `tenant_id + user_id + task_key` with task metadata and `hidden_at`.

## Contract

- Auth/user behavior: service requires `SecurityFrameworkUtils.getLoginUserId()` and `TenantContextHolder.getRequiredTenantId()`.
- Validation behavior: blank or oversized task keys, task types, sources, business IDs, or details fail fast with `PROFILE_WORKBENCH_TASK_VISIBILITY_INVALID`.
- Restore behavior: restore physically deletes the current tenant/user/task row so a later hide can reuse the same unique key.
- No fallback: missing user/tenant context or invalid request is not converted to success or an empty mock result.

## Validation

- Required fields are trimmed and checked for blank values.
- Optional metadata is length-limited and normalized to `null` when blank.
- Missing login user or tenant context fails fast instead of returning mock success.

## BDD

- BDD: Hide personal workbench task -> Given 用户在个人工作台看到一个待办任务，When 用户点击隐藏并确认，Then 服务端记录当前用户隐藏状态。
- BDD: Restore hidden personal workbench task -> Given 用户已隐藏任务，When 用户点击恢复，Then 当前用户隐藏状态被清除。
- BDD: User scoped hidden tasks -> Given 两个用户访问个人工作台，When 用户 A 隐藏任务，Then 用户 B 的隐藏列表不受影响。
- BDD: Error visibility -> Given 请求缺少 taskKey，When 调用隐藏或恢复，Then 服务端 fail-fast 返回业务错误。

## RED / GREEN

- RED: `mvn -pl yudao-module-system -am "-Dtest=ProfileWorkbenchTaskVisibilityServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，原因：隐藏/恢复 service、mapper、VO、错误码、测试表尚未实现。
- GREEN: `mvn -pl yudao-module-system -am "-Dtest=ProfileWorkbenchTaskVisibilityServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，`Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`。

## Verification

- Targeted Maven test passed across the system module and required reactor dependencies.

## Migration And Fixtures

- Migration: `IntRuoyiBackend/sql/mysql/20260727_system_profile_workbench_task_visibility.sql`.
- H2 create fixture updated: `IntRuoyiBackend/yudao-module-system/src/test/resources/sql/create_tables.sql`.
- H2 clean fixture updated: `IntRuoyiBackend/yudao-module-system/src/test/resources/sql/clean.sql`.

## Blockers

- None for backend targeted verification.

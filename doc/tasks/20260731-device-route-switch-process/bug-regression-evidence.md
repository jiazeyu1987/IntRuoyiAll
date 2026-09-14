# Bug Regression Evidence

## Bug
PCQ/PQC 一线填写页调用设备账号工序切换接口时，后端可能返回“设备账号 1 未绑定启用工艺路线，无法切换工序”。根因是设备账号绑定源依赖 `AdminUserApi.getUser()` 返回的 `postIds`，但该 API 只映射 `system_users.post_ids` 字段，没有读取正式 `system_user_post` 用户岗位关系，导致已有正式岗位关系的设备账号被误判为没有启用工艺路线绑定。

## Expected
设备账号存在正式 `system_user_post` 岗位关系，且岗位通过工作站/工艺路线配置可解析到启用路线时，`AdminUserApi.getUser()` 必须暴露正式岗位关系，MES 一线设备账号工序池才能继续按岗位、工作站和启用路线解析可切换工序；不得使用空岗位、旧字段或默认成功掩盖缺失关系。

## Reproduction
`mvn -pl yudao-module-system -am "-Dtest=AdminUserApiImplPostIdsTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`

## Root Cause
`AdminUserApiImpl#getUser` 直接 `BeanUtils.toBean(user, AdminUserRespDTO.class)`，没有从 `UserPostMapper#selectListByUserId` 补齐正式用户岗位关系。`MesFrontlineWorkstationPostRouteBindingSource` 因此拿到空 `postIds`，返回空路线绑定，最终触发设备账号未绑定启用路线的 fail-fast 错误。

## RED
RED: `mvn -pl yudao-module-system -am "-Dtest=AdminUserApiImplPostIdsTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: `AdminUserApiImplPostIdsTest.getUser_shouldExposeFormalPostRelationIds` expected `[701, 702]` but was `[]`.

## GREEN
GREEN: `mvn -pl yudao-module-system -am "-Dtest=AdminUserApiImplPostIdsTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 1 test, 0 failures, 0 errors.

## Verification
- `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineWorkstationPostRouteBindingSourceTest,MesFrontlineDeviceAccountContextServiceTest,MesFrontlineEmployeeSwitchServiceTest,MesFrontlineSubmitAuthorizationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 10 tests, 0 failures, 0 errors.
- Fix scope: `AdminUserApiImpl#getUser` now populates `AdminUserRespDTO.postIds` from formal `system_user_post` relation through `UserPostMapper#selectListByUserId`.
- No fallback, mock success, silent downgrade, or UI-only suppression was introduced.

## Blockers
- Real browser verification was not run in this turn because no confirmed local frontend/backend runtime, login account, tenant, and task-owned data setup were available.
- Commit/push closeout is blocked by pre-existing and parallel dirty worktree state plus `int_main...origin/int_main [ahead 16]`; this task did not stage or commit unrelated changes.

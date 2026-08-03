# Backend API Evidence

## Scope

- Scope: MES 工艺路线工序开始生产组长配置接口与一线生产填写切换授权运行态。
- Owned files include route flow config controller/service/VO and frontline device account context service/tests.

## Contract

- API contract:
  - `GET /mes/pro/route/flow-config/route-start-production-leader-production-lines`
  - `GET /mes/pro/route/flow-config/route-start-production-leaders`
  - `POST /mes/pro/route/flow-config/route-start-production-leaders/save`
- Data contract: route candidate snapshot key `configSnapshots.routeStartProductionLeaders`, item fields `productionLineId`, `candidateSourceType`, `candidateSourceIds`, `candidateSourceNames`, `remark`.
- Auth contract: route config query/update reuses existing batch-record-config permissions; production filling tab visibility remains existing menu permission only; switching scope is not granted by extra pressure-pump menu permission.

## Validation

- Validate production line belongs to current route process workstation production line.
- Validate candidate source type is `USERS` or `ROLE`.
- Validate candidate ids are non-empty enabled users or roles.
- Fail fast on invalid snapshot, invalid production line mapping, or authorized line without matching route process.

## BDD:

- BDD: 菜单权限只控制页签可见 -> Given 用户拥有批次执行页签菜单权限 When 进入批次执行/生产填写页面 Then 页面可见性由现有菜单权限决定，不要求额外压力泵全工序切换菜单权限。
- BDD: 生产组长按工序开始配置授权切换 -> Given 工艺路线的工序开始卡片配置了生产组长账号或角色 When 命中配置的账号进入生产填写界面 Then 可切换其负责产线下的工序和员工。
- BDD: 未配置生产组长不扩大授权 -> Given 登录账号未命中当前路线工序开始生产组长配置 When 进入生产填写界面 Then 不因菜单权限、岗位或工作站绑定获得压力泵跨工序/跨员工切换能力。

## RED:

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineDeviceAccountContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: constructor and route version snapshot dependency missing before implementation.

## GREEN:

- GREEN: `mvn -pl yudao-module-mes -am "-DskipTests" compile` -> PASS.
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesFrontlineDeviceAccountContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 6 tests, 0 failures, 0 errors.

## Verification

- Verified account source `USERS` and role source `ROLE` authorize only production lines configured in route start production leader snapshot.
- Verified old pressure-pump all-process menu permission does not grant switching without route start leader config.
- Verified authorized production line without matching route process/workstation production line fails fast.

## Blockers

- `-am` targeted test was blocked by unrelated DCC test compile error in `DccControlledFileNasTransferServiceTest.java`; MES-module-only test passed and is the current task-owned backend verification.

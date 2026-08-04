# Execution Log

## User Intent

- 用户确认：批次执行页签可见性只看现有菜单权限；生产填写界面的工序/员工切换不应由额外菜单权限、岗位或工作站绑定决定，而应由工艺路线“工序开始”卡片新增的生产组长配置决定。
- 生产组长配置可绑定账号或角色；用户 2026-08-04 澄清当前工艺路线本身就是工作站产线/负责范围，一个组长可负责多条工艺路线，一条工艺路线可配置多个组长；命中配置的账号可切换负责工艺路线里的工序和对应员工。
- 用户补充：菜单权限里能看到批次执行页签的都可以看见，不需要额外压力泵全工序权限。

## Rule And Skill Reads

- Read: `C:\Users\BJB110\.codex\skills\backend-api-delivery\SKILL.md`
- Read: `C:\Users\BJB110\.codex\skills\backend-api-delivery\references\backend-contract.md`
- Read: `C:\Users\BJB110\.codex\skills\frontend-feature-delivery\SKILL.md`
- Read: `C:\Users\BJB110\.codex\skills\frontend-feature-delivery\references\frontend-contract.md`
- Read: `C:\Users\BJB110\.codex\skills\database-schema-delivery\SKILL.md`
- Read: `C:\Users\BJB110\.codex\skills\database-schema-delivery\references\database-contract.md`
- Read: `C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\SKILL.md`
- Read: `C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\references\bug-contract.md`
- Read: `C:\Users\BJB110\.codex\skills\project-experience-consolidation\SKILL.md`
- Read: `docs\backend-development.md#MES 一线设备账号权限门禁`
- Read: `docs\frontend-development.md#前端静态契约隔离门禁`
- Read: `docs\task-closeout-rules.md#验收范围变更门禁`
- Read: `docs\task-closeout-rules.md#技能证据文件清理前归档门禁`
- Read: `docs\powershell-encoding.md`
- Read: `C:\Users\BJB110\.codex\skills\playwright\SKILL.md`
- Read: `C:\Users\BJB110\.codex\skills\quality-assurance-test-suite\SKILL.md`
- Read: `docs\e2e-rules.md`
- Read: `docs\login-access.md`
- Read: `docs\local-runtime.md`
- Read: `docs\worktree-restrictions.md`
- Read: `docs\task-closeout-rules.md`
- Read: `docs\database-rules.md`

## BDD

- BDD: 菜单权限只控制页签可见 -> Given 用户拥有批次执行页签菜单权限 When 进入批次执行/生产填写页面 Then 页面可见性由现有菜单权限决定，不要求额外压力泵全工序切换菜单权限。
- BDD: 生产组长按工序开始配置授权切换 -> Given 工艺路线的工序开始卡片配置了生产组长账号或角色 When 命中配置的账号进入生产填写界面 Then 可切换其负责工艺路线范围内的工序和员工。
- BDD: 未配置生产组长不扩大授权 -> Given 登录账号未命中当前路线工序开始生产组长配置 When 进入生产填写界面 Then 不因菜单权限、岗位或工作站绑定获得压力泵跨工序/跨员工切换能力。
- BDD: 多组长多路线可共存 -> Given 一个组长负责多条工艺路线且一条工艺路线有多个组长 When 不同组长登录生产填写界面 Then 各自只能看到并切换配置范围内的工序和员工。

## RED

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineDeviceAccountContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: `MesFrontlineDeviceAccountContextServiceImpl` 构造器尚未接入 `MesProRouteVersionMapper`，生产组长快照读取链路未实现。
- RED: `node tests/e2e/mes-route-start-production-leaders-static.spec.js` -> FAIL, expected reason: `RouteFlowGraphDesigner.vue` 缺少 `data-flow-boundary-field="productionLeader"` 和生产组长面板/API 合同。
- RED: `node tests\e2e\mes-route-start-production-leaders-static.spec.js` after 2026-08-04 clarification -> FAIL, expected reason: frontend still required “工作站产线/路线工序已绑定工作站产线” instead of current-route scope.
- RED: `mvn -pl yudao-module-mes "-Dtest=MesFrontlineDeviceAccountContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` after 2026-08-04 clarification -> FAIL, expected reason: old runtime authorization still matched `routeStartProductionLeaders` against workstation `productionLineId`.

## Implementation Notes

- 后端新增工艺路线工序开始生产组长配置接口：
  - `GET /mes/pro/route/flow-config/route-start-production-leader-production-lines`
  - `GET /mes/pro/route/flow-config/route-start-production-leaders`
  - `POST /mes/pro/route/flow-config/route-start-production-leaders/save`
- 持久化采用既有候选路线快照：`configSnapshots.routeStartProductionLeaders`；未新增物理表、未新增菜单权限 SQL。
- 运行态授权改为读取激活路线版本快照，按登录账号或 `PermissionApi.getUserRoleIdListByUserId(loginUserId)` 命中生产组长账号/角色，并按当前工艺路线范围授权可切换工序，不再按路线工序工作站的 `productionLineId` 过滤。
- 前端在工艺路线关系图“工序开始”卡片新增“生产组长”字段，配置项包含当前工艺路线负责范围、来源类型（账号/权限角色）和候选账号/角色。
- 2026-08-04 口径修正：保留旧 API 字段名 `productionLineId` 以降低联动面，但字段值和页面语义改为当前工艺路线 ID/编码/名称。

## GREEN

- GREEN: `mvn -pl yudao-module-mes -am "-DskipTests" compile` -> PASS, backend compile success.
- GREEN: `node tests\e2e\mes-route-start-production-leaders-static.spec.js` -> PASS, frontend static contract success.
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesFrontlineDeviceAccountContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 6 tests, 0 failures, 0 errors.
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesFrontlineDeviceAccountContextServiceTest,MesProRouteFlowConfigServiceImplTest#getRouteStartProductionLeaderProductionLines_shouldUseCurrentRouteAsResponsibleScope" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 7 tests, 0 failures, 0 errors.
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; node node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p tsconfig.relaxed.json --pretty false` -> PASS.
- GREEN: `git diff --check -- <task-owned files>` -> PASS.
- GREEN: backend/frontend/database/bug evidence validators -> PASS.

## Additional Verification

- `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineDeviceAccountContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> BLOCKED before MES tests by unrelated DCC test compile error: `DccControlledFileNasTransferServiceTest.java:[609,9] cannot find symbol assertNull(String)`; switched to MES-module-only target test and recorded blocker.
- `node node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p tsconfig.relaxed.json --pretty false` -> BLOCKED by Node default heap OOM; rerun with project memory option passed.
- `rg "frontline-pressure-pump:all-processes|PRESSURE_PUMP_ALL_PROCESS_PERMISSION" ...` -> runtime code only retains constant and regression-test reference; frontend production code has no pressure-pump all-process menu-permission dependency.
- GREEN: `node --check tests\e2e\mes-route-start-production-leaders-real.e2e.js` -> PASS, real E2E script syntax is valid after login diagnostics, maximized graph click path, and paginated route scan updates.
- GREEN: `node tests\e2e\mes-route-start-production-leaders-static.spec.js` -> PASS, production-leader UI/API static contract still passes after E2E script changes.
- GREEN: `Invoke-RestMethod http://127.0.0.1:48081/actuator/health` -> PASS, backend runtime health is `UP`.
- Runtime loaded: PID `46388`, jar `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260803-222421-pressure-pump-leader.jar`.
- BLOCKED: `node tests\e2e\mes-route-start-production-leaders-real.e2e.js` using default local identity `芋道源码/admin` -> FAIL, real login succeeded but no eligible route data existed; route scan checked 4 of 4 routes and found no route with bindable production lines. No MES write requests or target network failures were recorded. Evidence copied to `E:\IntRuoyi\output\playwright\20260803-pressure-pump-leader-switch-scope\mes-route-start-production-leaders-real-default-route-data-blocked.json`.
- BLOCKED: `node tests\e2e\mes-route-start-production-leaders-real.e2e.js` using test identity label `测试租户/aoteman` with the local default password source -> FAIL, login API returned `code=1002000000` / account-password mismatch. No MES write requests or target network failures were recorded. Evidence copied to `E:\IntRuoyi\output\playwright\20260803-pressure-pump-leader-switch-scope\mes-route-start-production-leaders-real-test-tenant-login-blocked.json`.
- BLOCKED: `node tests\e2e\mes-route-start-production-leaders-real.e2e.js` rerun with user-confirmed identity label `芋道源码/admin` -> FAIL, real login reached the route scan but the local tenant still has no route with bindable production lines; scanned route count `4/4`. No MES write requests, target network failures, console errors, or page errors were recorded. Evidence: `E:\IntRuoyi\output\playwright\20260803-pressure-pump-leader-switch-scope\mes-route-start-production-leaders-real-failure.json`.
- GREEN: follow-up validators after the user-confirmed E2E rerun -> `git diff --check -- <task-docs-and-e2e-files>` PASS, `node --check tests\e2e\mes-route-start-production-leaders-real.e2e.js` PASS, `node tests\e2e\mes-route-start-production-leaders-static.spec.js` PASS.
- GREEN: 2026-08-04 current-route-scope validators -> `node tests\e2e\mes-route-start-production-leaders-static.spec.js` PASS; `node --check tests\e2e\mes-route-start-production-leaders-real.e2e.js` PASS.
- BLOCKED: 2026-08-04 real E2E runtime refresh -> current `48081` PID `14800` runs `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260804-dcc-nas-uncontrolled-import.jar`, an unrelated DCC task runtime. Per task ownership rules, did not stop or replace it to run this MES E2E.
- Cleanup: removed task-owned temporary runtime loader `doc\tasks\20260803-pressure-pump-leader-switch-scope\load-pressure-pump-leader-runtime.ps1`.

## Experience Consolidation

- Existing long-term gates already cover this task class: MES 一线设备账号权限门禁, 前端静态契约隔离门禁, Maven `-D` 参数引号门禁, 技能证据文件清理前归档门禁.
- No new long-term experience document created; task-specific facts remain in this task folder.
- 2026-08-03 E2E follow-up: existing long-term E2E/login gates already cover the observed blockers (real page path required, login precondition must fail fast, no API-only substitute). No new long-term experience document was created.

## Blockers

- Closeout blocker: shared worktree is dirty with many unrelated DCC/MES/frontend/task-doc changes and current branch is behind `origin/int_main` by 2 commits. No task commit or push performed to avoid mixing unrelated work and because the branch is not in a clean publishable state.
- Real E2E blocker: implementation is now corrected so current route is the responsible scope; however the local backend runtime on `48081` is owned by another DCC task Jar and has not been safely refreshed to this code. `测试租户/aoteman` still cannot log in with the current local default password source.

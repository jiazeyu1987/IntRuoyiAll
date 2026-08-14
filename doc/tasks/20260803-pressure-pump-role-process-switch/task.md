# 压力泵角色授权一线工序切换

## Task Goal

实现一线生产填写页面的压力泵工序切换授权：拥有指定权限角色/权限的账号登录后，可以切换压力泵相关启用工艺路线的全部工序；该授权不依赖账号岗位工作站绑定，同时不得影响普通账号仍按岗位工作站正式链路授权。

## Milestones

- [x] 核对现有一线生产填写前后端调用链路与角色/权限 API。
- [x] 记录需求变更边界和 BDD 场景。
- [x] 编写 RED 回归测试覆盖“有权限可看压力泵全部工序”和“无权限仍按岗位链路”。
- [x] 实施最小后端正式授权链路，不引入默认路线或空成功。
- [x] 运行 GREEN、相邻回归、迁移门禁和 evidence 校验。

## Expected Verification

- `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineDeviceAccountContextServiceTest,MesFrontlineWorkstationPostRouteBindingSourceTest,MesFrontlineEmployeeSwitchServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python -X utf8 IntRuoyiBackend\script\release\run-release-migration-policy-gate.py --sql-root IntRuoyiBackend\sql\mysql --sql-file IntRuoyiBackend\sql\mysql\20260803_mes_frontline_pressure_pump_all_process_permission.sql --output doc\tasks\20260803-pressure-pump-role-process-switch\migration-policy-gate.json`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260803-pressure-pump-role-process-switch/backend-api-evidence.md`
- `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc/tasks/20260803-pressure-pump-role-process-switch/database-schema-evidence.md`

## Applicable Gates

- Strict no-fallback：无授权、无启用路线、无压力泵工序配置时必须明确失败，不得返回默认全量或空成功。
- BDD + strict TDD：生产代码前先记录 BDD 和 RED。
- 权限边界：普通账号仍走岗位/工作站链路；角色授权只覆盖压力泵范围。

## Design Constraint Check

- `是否引入 fallback/降级/吞异常`：否；角色授权是显式新授权链路，不作为岗位链路失败后的兜底。
- `是否从根因和长期维护角度解决`：是；目标是把压力泵特殊全工序授权建模为正式权限，而不是岗位绑定伪装。
- `是否存在临时补丁或绕过`：否；禁止硬编码账号 ID、岗位 ID、前端放行或默认路线。

## Current Status

completed

- 已完成压力泵全工序权限服务链路、权限迁移 SQL、定向 JUnit、release migration policy gate、backend evidence validator、database evidence validator 和 task-closeout-cleanup apply。
- 追加修复运行时错误：`设备账号上下文不完整或不一致：post workstation binding loginUserId=1, postIds=[14]` 的根因是压力泵权限判定曾走显式角色 ID 检查，现已改为标准登录用户权限检查。
- 已补充 bug regression evidence、复验目标 JUnit、执行 task-closeout-cleanup，并将权限判定经验沉淀到 `docs/backend-development.md` 与 `docs/experience-index.md`。
- 最终收尾：本任务收尾记录已提交，当前分支全部本地提交已推送到 `origin/int_main`，最终状态验证不再 ahead。

## Completed Work

- `MesFrontlineDeviceAccountContextServiceImpl` 增加正式权限 `mes:pro-feedback:frontline-pressure-pump:all-processes`，拥有该权限的角色按启用压力泵路线读取全部有效工序。
- `MesFrontlineDeviceAccountContextServiceImpl#hasPressurePumpAllProcessPermission` 使用 `permissionApi.hasAnyPermissions(loginUserId, permission)`，保持与系统标准用户权限、动态授权和超级管理员语义一致。
- 普通账号仍走既有岗位、工作站、工艺路线工序工作站绑定链路；压力泵权限链路不会在岗位链路失败后兜底。
- 缺少启用压力泵路线、有效路线工序、启用工序、启用工作站或设备主数据时均 fail fast。
- `20260803_mes_frontline_pressure_pump_all_process_permission.sql` 增加可分配权限菜单并合并到已有包含父菜单的租户套餐和租户管理员角色。

## Verification Evidence

- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineDeviceAccountContextServiceTest,MesFrontlineWorkstationPostRouteBindingSourceTest,MesFrontlineEmployeeSwitchServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 11 tests.
- GREEN: `python -X utf8 IntRuoyiBackend\script\release\run-release-migration-policy-gate.py --sql-root IntRuoyiBackend\sql\mysql --sql-file IntRuoyiBackend\sql\mysql\20260803_mes_frontline_pressure_pump_all_process_permission.sql --output doc\tasks\20260803-pressure-pump-role-process-switch\migration-policy-gate.json` -> PASS, 1 migration, sha256 `4ff6ac8bc5cf101d1a4bdb453451860b39735773191cb790d29dd253b1d2bf46`。
- GREEN: backend API evidence validator -> PASS。
- GREEN: database schema evidence validator -> PASS。
- RED: 旧显式角色 ID 权限检查下，`MesFrontlineDeviceAccountContextServiceTest#shouldListAllPressurePumpProcessesWhenRoleHasPressurePumpAllProcessPermission` 失败并落到 `PRO_FRONTLINE_DEVICE_ACCOUNT_BINDING_SOURCE_MISSING`，复现账号 1 / 岗位 14 的岗位绑定链路误判。
- GREEN: 改为 `permissionApi.hasAnyPermissions(loginUserId, PRESSURE_PUMP_ALL_PROCESS_PERMISSION)` 后，`MesFrontlineDeviceAccountContextServiceTest` -> PASS, 5 tests；相邻三类目标测试 -> PASS, 11 tests。
- GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260803-pressure-pump-role-process-switch/bug-regression-evidence.md` -> PASS；随后 cleanup apply 删除临时 evidence 文件并保留核心任务记录。
- GREEN: `git -c http.https://github.com.proxy= push origin int_main` -> PASS；`git status --short --branch` -> `## int_main...origin/int_main`，不再 ahead。

# 20260807-team-leader-workbench-frontline-device-context

## Task Goal

修复班组长工作台在一线运行态 `deviceId=41` 场景下缺少负责范围上下文的问题，确保班组长工作台读取、展示和维护链路都使用正式生产组长负责范围，不用设备账号、一线岗位绑定或空上下文替代。

## Milestones

- [x] M1: 复现并定位 `frontline runtime deviceId=41` 进入班组长工作台时缺失的负责范围字段或上下文来源。
- [x] M2: 增加失败优先的回归测试，锁定班组长工作台必须携带正式负责范围上下文。
- [x] M3: 实现最小根因修复，保持设备账号运行态和班组长负责范围链路分离。
- [x] M4: 运行定向后端/前端验证与相关静态合同，记录 RED/GREEN/REGRESSION 证据。
- [x] M5: 完成任务文档、验证报告和必要清理。

## Expected Verification

- 后端定向测试覆盖 `deviceId=41` 或等价设备上下文下，班组长工作台按正式生产组长负责范围解析上下文。
- 相邻一线运行态测试确认不把生产组长负责范围降级为设备账号岗位/工作站绑定。
- 若触及前端页面，运行对应静态合同或 `pnpm ts:check`；若具备本机运行态和登录前置，再执行真实页面只读验证。

## Applicable Gates

### MES 生产人员档案正式工重复关联门禁

- Trigger: 一线运行配置、运行配置员工、生产员工继承组长负责路线工序、班组长人员管理和负责范围上下文。
- Preflight check: 一线生产运行配置员工和切换员工校验必须同源于当前负责生产组长启用的生产人员档案；生产员工请求一线工序时，必须先按启用人员档案解析唯一负责生产组长，再读取该组长正式“工序开始”配置中负责路线下的全部工序。
- Blocker: 员工档案存在却进入设备/岗位路线来源、一线生产弹窗和生产组长人员管理列表不一致、或归属异常回退岗位/设备路线时必须停止。
- Verification: 覆盖运行配置员工来源、切换员工校验同源、生产员工继承唯一负责组长的全部正式路线工序，且不触发设备路线来源。
- Forbidden action: 禁止用工序员工绑定、设备账号候选、岗位/工作站绑定、当前登录人或空列表替代人员管理列表和正式负责范围。

### MES 一线设备账号权限门禁

- Trigger: 一线生产填写、设备账号切换工序、`frontline/device-account/processes`、设备账号上下文和岗位/工作站绑定。
- Preflight check: 区分系统标准权限、岗位/工作站绑定、生产组长负责范围三条链路；不能把设备账号运行态候选当作生产组长工作台负责范围。
- Blocker: 班组长工作台负责范围缺失却从设备账号路线、岗位或工作站绑定静默补齐时必须停止。
- Verification: 相邻回归必须证明设备账号运行态仍使用正式设备上下文，班组长工作台使用正式生产组长负责范围。
- Forbidden action: 禁止硬编码账号、设备、岗位或路线；禁止默认路线、空成功或前端放行。

## Current Status

completed

## Verification Evidence

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineRuntimeConfigServiceTest#getRuntimeConfig_keepsLeaderScopeWhenRouteStartCandidateDeviceHasNoTeamBinding" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，复现 `班组长工作台缺少负责范围上下文：frontline runtime deviceId=41`。
- GREEN: 同一目标 JUnit -> PASS，Tests run: 1, Failures: 0, Errors: 0。
- REGRESSION: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineRuntimeConfigServiceTest,MesFrontlineDeviceAccountContextServiceTest,MesFrontlineProductionEmployeeLeaderProcessScopeTest,MesFrontlineEmployeeSwitchServiceTest,MesFrontlineSubmitAuthorizationTest,MesFrontlineSubmitIdentityTraceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 23, Failures: 0, Errors: 0。
- RECHECK: same regression command rerun on 2026-08-07 22:12 -> PASS，Tests run: 23, Failures: 0, Errors: 0, Skipped: 0。
- STATIC: `git diff --check -- <task-owned files>` -> PASS，仅有 Git LF/CRLF 提示，无 whitespace error。
- BUG EVIDENCE VALIDATION: `validate_bug_regression.py --evidence doc\tasks\20260807-team-leader-workbench-frontline-device-context\verification-report.md` -> PASS。
- EXPERIENCE: 已将 route-start 生产组长来源与班组设备绑定分离门禁合并到 `docs/backend-development.md`，并在 `docs/experience-index.md` 增加关键词。
- CLEANUP: `task_closeout.py --task-id 20260807-team-leader-workbench-frontline-device-context --mode preview/apply` -> PASS，无删除项，无 blocked/warnings。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是补齐正式负责范围上下文链路，不用设备账号或岗位绑定替代。
- `是否存在临时补丁或绕过`：否。

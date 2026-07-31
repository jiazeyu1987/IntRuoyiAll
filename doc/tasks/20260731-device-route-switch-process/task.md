# 设备账号未绑定启用工艺路线导致无法切换工序

## Task Goal
修复 PCQ 填写/设备账号切换工序时误报“设备账号 1 未绑定启用工艺路线，无法切换工序”的问题，确保按正式启用工艺路线绑定判定工序切换能力。

## Milestones
- [x] 复现并定位错误提示来源、接口链路和数据判定口径。
- [x] 编写 BDD 场景与 RED 回归测试，证明当前误判。
- [x] 实施最小根因修复，不引入 fallback、降级或吞异常。
- [x] 跑通 GREEN 与相关回归验证，记录证据。
- [x] 收尾前更新验证报告和状态。

## Expected Verification
- 聚焦回归测试先 RED 后 GREEN。
- 相关前端/后端静态或单元测试通过。
- 如运行态前置齐备，补充真实页面只读/操作路径验证；若缺前置则记录 BLOCKED 原因。

## Current Status
ready_for_closeout

## 设计约束检查
- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，需先定位正式启用工艺路线绑定来源。
- 是否存在临时补丁或绕过：否。

## 经验门禁
- `docs/experience-index.md` 已读取；未命中与 `AdminUserApi` / `system_user_post` / 设备账号路线绑定直接相关的既有长期经验条目。
- `project-experience-consolidation` 已读取并检索现有 memory/login/backend 文档；无合适归宿，且未获得新建长期经验文档授权，因此不新增长期经验文档。

## Verification Evidence
- RED: `mvn -pl yudao-module-system -am "-Dtest=AdminUserApiImplPostIdsTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，expected `[701, 702]` but was `[]`。
- GREEN: `mvn -pl yudao-module-system -am "-Dtest=AdminUserApiImplPostIdsTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，1 test。
- REGRESSION: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineWorkstationPostRouteBindingSourceTest,MesFrontlineDeviceAccountContextServiceTest,MesFrontlineEmployeeSwitchServiceTest,MesFrontlineSubmitAuthorizationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，10 tests。

## Closeout Blocker
- 当前工作区存在大量非本任务改动，且分支为 `int_main...origin/int_main [ahead 16]`；本任务未暂存、提交或推送，避免混入并行任务改动。

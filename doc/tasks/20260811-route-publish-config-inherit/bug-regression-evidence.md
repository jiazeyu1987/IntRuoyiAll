# Bug Regression Evidence

## Bug Summary

- 工艺路线重新发布会生成新的 routeProcessId，但生产组长损耗原因和设备参数标准仍绑定旧 routeProcessId，导致当前一线生产读取为空。

## Expected Behavior

- 发布新路线版本后，当前正式路线工序应继承未变化工序的正式生产组长配置；运行态只读取当前 routeProcessId，不回读旧 ID。

## Reproduction

- `mvn -f IntRuoyiBackend\pom.xml -pl yudao-module-mes -am "-Dtest=MesProRouteVersionPublishProjectionServiceTest,MesProRouteVersionPublishProjectionServiceImplTest,MesTeamLeaderProcessConfigServiceImplTest,MesTeamLeaderLossReasonServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- 初次相邻回归失败，证明发布投影新增链路未在同服务测试类完整注入依赖；新增回归场景证明发布生成新 routeProcessId 时必须迁移旧配置。

## Root Cause

- 发布投影会删除并重建 `mes_pro_route_process`，损耗/不良原因和设备参数标准仍按旧 `routeProcessId` 精确绑定。发布完成后运行态只按当前 routeProcessId 读取，因此旧配置仍在旧 ID 上但页面显示为空。

## Regression Test

- `projectCandidate_shouldInheritTeamLeaderLossReasonsAndDeviceParameterRulesToNewRouteProcessIds`
- `projectCandidate_shouldNotInheritTeamLeaderConfigsFromClientRouteProcessId`

## RED

- RED: `mvn -f IntRuoyiBackend\pom.xml -pl yudao-module-mes -am "-Dtest=MesProRouteVersionPublishProjectionServiceTest,MesProRouteVersionPublishProjectionServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，新增 Mapper 未注入相邻发布投影测试类导致 NPE，长 actionCode 既有断言失败。

## GREEN

- GREEN: `mvn -f IntRuoyiBackend\pom.xml -pl yudao-module-mes -am "-Dtest=MesProRouteVersionPublishProjectionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，12 tests。
- GREEN: `mvn -f IntRuoyiBackend\pom.xml -pl yudao-module-mes -am "-Dtest=MesProRouteVersionPublishProjectionServiceTest,MesProRouteVersionPublishProjectionServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，18 tests。

## Verification

- GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260811-route-publish-config-inherit\bug-regression-evidence.md` -> PASS.

## Risk And Regression Scope

- 路线发布投影会复制配置表新增行，需要防止重复插入和覆盖已有目标配置。
- 运行态仍必须只读当前 `route_process_id`，不能 fallback 到旧工序。

## Blockers And Follow-Up

- Merge to `int_main` pending final verification and closeout.

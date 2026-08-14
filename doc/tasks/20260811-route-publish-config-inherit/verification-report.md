# Verification Report

## Scope

- 工艺路线版本发布投影。
- 生产组长工序配置损耗/不良原因继承。
- 生产组长/一线生产设备参数标准继承。
- 同服务长动态表单 actionCode 长度约束。

## Results

- RED: `mvn -f IntRuoyiBackend\pom.xml -pl yudao-module-mes -am "-Dtest=MesProRouteVersionPublishProjectionServiceTest,MesProRouteVersionPublishProjectionServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL before final fix; failure exposed missing process-pool Mapper mocks in adjacent projection test and existing long actionCode assertion.
- GREEN: `mvn -f IntRuoyiBackend\pom.xml -pl yudao-module-mes -am "-Dtest=MesProRouteVersionPublishProjectionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> BUILD SUCCESS; 12 tests, 0 failures, 0 errors.
- REGRESSION: `mvn -f IntRuoyiBackend\pom.xml -pl yudao-module-mes -am "-Dtest=MesProRouteVersionPublishProjectionServiceTest,MesProRouteVersionPublishProjectionServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> BUILD SUCCESS; 18 tests, 0 failures, 0 errors.
- GUARD: `powershell -NoProfile -ExecutionPolicy Bypass -File scripts\preflight\branch-runtime-port-guard.ps1` -> PASS.
- EVIDENCE: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260811-route-publish-config-inherit\bug-regression-evidence.md` -> PASS.
- Merge: pending final int_main verification and closeout.

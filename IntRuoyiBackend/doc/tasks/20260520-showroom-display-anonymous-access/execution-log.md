BDD: showroom display endpoints should be anonymous -> Given `ShowroomDisplayController` is the public frontstage display API / When an unauthenticated client requests showroom display routes / Then Spring Security must not block the request with `401 / 账号未登录`.

BDD: anonymous access should not change app-config contract -> Given `GET /showroom/display/app-config` is already the confirmed Website data contract / When the controller is opened for anonymous access / Then the payload contract and fail-fast business behavior must remain unchanged.

BLOCKED: `mvn -pl yudao-module-showroom "-Dtest=ShowroomRuntimeStructureTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, build initially stopped in `compile` because previous unfinished task `20260520-showroom-assignee-product-scope` left [ShowroomAdminController.java](D:/ProjectPackage/Int/IntRuoyi/ruoyi-vue-pro/yudao-module-showroom/src/main/java/cn/iocoder/yudao/module/showroom/controller/admin/ShowroomAdminController.java) in a broken state.

GREEN: resolved the upstream showroom compile blocker from `20260520-showroom-assignee-product-scope` -> PASS, showroom module returned to a compilable state.

GREEN: add method-level `@PermitAll` on `ShowroomDisplayController.getAppConfig()` -> PASS

GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomRuntimeStructureTest,ShowroomHttpApiIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS

GREEN: `mvn -pl yudao-server -am -DskipTests package` -> PASS

GREEN: `cmd /c D:\ProjectPackage\Int\IntRuoyi\restart-ruoyi.bat` -> PASS

GREEN: real runtime probe after rebuild + restart -> PASS at auth layer, `Invoke-WebRequest http://127.0.0.1:48081/showroom/display/app-config` no longer returns `{"code":401,"msg":"账号未登录","data":null}`.

INFO: current live-data blocker after auth fix -> `{"success":false,"message":"SHOWROOM_TARGET_NOT_FOUND: live company preview asset is required","code":500,"result":null,"timestamp":...}`

## Outstanding Blockers

- Anonymous-access blocker is closed.
- Remaining blocker belongs to local showroom live data completeness, not authentication.

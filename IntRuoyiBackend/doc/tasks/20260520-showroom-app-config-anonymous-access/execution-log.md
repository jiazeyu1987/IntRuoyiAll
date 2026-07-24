BDD: app-config should be anonymously readable -> Given Website frontstage must boot without后台登录态 / When a browser requests `GET /showroom/display/app-config` / Then Spring Security must allow anonymous access to this endpoint only.

BDD: showroom admin APIs remain protected -> Given showroom management flows are still backend-only / When an unauthenticated client requests non-public showroom admin routes / Then those routes must continue to require authentication.

BDD: app-config should still fail fast on missing live data -> Given the endpoint is anonymous but required showroom live data is incomplete / When the browser requests `GET /showroom/display/app-config` / Then the backend should return the real live-data blocker instead of `401`.

RED: `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest#appConfigEndpointShouldBeAnnotatedPermitAllWithoutOpeningOtherDisplayRoutes" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, `ShowroomDisplayController#getAppConfig()` lacked `@PermitAll`.

GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomRuntimeStructureTest,ShowroomHttpApiIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS

GREEN: `mvn -pl yudao-server -am -DskipTests package` -> PASS

GREEN: `cmd /c D:\ProjectPackage\Int\IntRuoyi\restart-ruoyi.bat` -> PASS

GREEN: real runtime probe after rebuild + restart -> PASS at auth layer, `GET http://127.0.0.1:48081/showroom/display/app-config` no longer returns `{"code":401,"msg":"账号未登录","data":null}`.

INFO: current live-data blocker after auth fix -> `{"success":false,"message":"SHOWROOM_TARGET_NOT_FOUND: live company preview asset is required","code":500,"result":null,"timestamp":...}`

## Outstanding Blockers

- Anonymous-access blocker is closed.
- Remaining blocker belongs to local showroom live data completeness, not authentication.

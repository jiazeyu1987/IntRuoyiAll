# Task: Showroom App Config Anonymous Access

## Goal

让 `GET /showroom/display/app-config` 支持前台匿名访问，同时保持 showroom 管理接口和其他非公开接口的认证要求不变。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\test\java\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260520-showroom-app-config-anonymous-access\**`

## Non-Scope

- 不放开 `/showroom` 下的管理接口。
- 不更改 app-config payload 业务字段。
- 不引入全局 permitAll 模糊规则。

## Expected Verification

- `mvn -pl yudao-module-showroom "-Dtest=ShowroomRuntimeStructureTest,ShowroomHttpApiIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `mvn -pl yudao-server -am -DskipTests package`
- `cmd /c D:\ProjectPackage\Int\IntRuoyi\restart-ruoyi.bat`
- 若本地 runtime 可控，匿名 `GET /showroom/display/app-config` 不再返回认证层 `401`

## Current Status

- Status: Completed
- Completed work:
  - 已确认当前匿名请求 `http://127.0.0.1:48081/showroom/display/app-config` 原先返回 `401`
  - 已确认当前安全框架通过 `@PermitAll` 自动收集免登录规则
  - 已在 `ShowroomDisplayController#getAppConfig()` 增加 `@PermitAll`
  - 已补充聚焦测试并通过
  - 已执行 `mvn -pl yudao-server -am -DskipTests package`
  - 已执行 `cmd /c D:\ProjectPackage\Int\IntRuoyi\restart-ruoyi.bat`
  - 已确认匿名请求不再返回 `{"code":401,"msg":"账号未登录","data":null}`
- Remaining blockers:
  - 当前本地 showroom live 数据仍不完整，匿名请求现在已进入业务层并返回 `SHOWROOM_TARGET_NOT_FOUND: live company preview asset is required`

## Final Verification Result

- PASS: `mvn -pl yudao-module-showroom "-Dtest=ShowroomRuntimeStructureTest,ShowroomHttpApiIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- PASS: `mvn -pl yudao-server -am -DskipTests package`
- PASS: `cmd /c D:\ProjectPackage\Int\IntRuoyi\restart-ruoyi.bat`
- PASS: `GET http://127.0.0.1:48081/v3/api-docs` -> HTTP 200
- PASS: 匿名 `GET http://127.0.0.1:48081/showroom/display/app-config` 不再返回认证层 `401`

# Task: 放通展厅公开展示接口匿名访问

## Goal

放通 `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro` 中 showroom 前台展示接口的匿名访问能力，解决 `Website` 真实联调时 `GET /showroom/display/app-config` 返回 `401 / 账号未登录` 的阻塞问题。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\test\java\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260520-showroom-display-anonymous-access\**`

## Non-Scope

- 不修改 `Website` 前端 consumer 代码。
- 不引入新的前台登录流程或临时 token 方案。
- 不修改 `infra/file/*/get/**` 现有匿名下载策略。
- 不改 showroom 业务聚合字段契约。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260520-intruoyi-showroom-app-config-provider\task.md`
- Status before this task: `Completed on 2026-05-20`
- Impact: `app-config` 聚合接口已完成，但真实联调仍被匿名访问策略阻塞，因此可继续处理本次安全放通缺口。

## Milestones

1. 复现并确认 `showroom/display` 公开展示链路被 Spring Security 拦截。
2. 先补 RED 回归测试，锁定公开展示 controller 必须具备匿名访问声明。
3. 以最小改动放通 showroom display 接口匿名访问。
4. 运行受影响测试并记录 GREEN 结果。
5. 更新任务文档、bug evidence 与后端证据。

## Expected Verification

- `mvn -pl yudao-module-showroom "-Dtest=ShowroomRuntimeStructureTest,ShowroomHttpApiIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `mvn -pl yudao-server -am -DskipTests package`
- `cmd /c D:\ProjectPackage\Int\IntRuoyi\restart-ruoyi.bat`
- `curl http://127.0.0.1:48081/showroom/display/app-config`

## Current Status

- Status: Completed
- Completed work:
  - 已确认 `Website` 仓库侧 consumer 已完成到 W4，原先真实联调阻塞为 `GET /showroom/display/app-config` 返回 `401 / 账号未登录`
  - 已确认 `infra/file/{configId}/get/**` 现有就是匿名可读，不是当前阻塞点
  - 已确认本项目 Security 配置会自动把 `@PermitAll` 控制器映射为匿名访问 URL
  - 已在 [ShowroomDisplayController.java](D:/ProjectPackage/Int/IntRuoyi/ruoyi-vue-pro/yudao-module-showroom/src/main/java/cn/iocoder/yudao/module/showroom/controller/display/ShowroomDisplayController.java) 的 `getAppConfig()` 上补齐 `@PermitAll`
  - 已增加 `appConfigEndpointShouldBeAnnotatedPermitAllWithoutOpeningOtherDisplayRoutes` 回归测试
  - 已完成源码重打包与本地 runtime 重启
  - 已确认匿名请求不再返回认证层 `401`
- Remaining blockers:
  - 本地 showroom live 数据不完整，当前真实返回为 `SHOWROOM_TARGET_NOT_FOUND: live company preview asset is required`

## Final Verification Result

- PASS: `mvn -pl yudao-module-showroom "-Dtest=ShowroomRuntimeStructureTest,ShowroomHttpApiIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- PASS: `mvn -pl yudao-server -am -DskipTests package`
- PASS: `cmd /c D:\ProjectPackage\Int\IntRuoyi\restart-ruoyi.bat`
- PASS: 匿名 `GET http://127.0.0.1:48081/showroom/display/app-config` 不再返回 `401 / 账号未登录`

## Remaining External Blocker

- 本地 showroom live 数据不完整，当前真实返回为 `SHOWROOM_TARGET_NOT_FOUND: live company preview asset is required`。

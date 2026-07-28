# Backend API Evidence

## Scope

- Controller：`ShowroomClientDownloadController`。
- 下载描述：`ShowroomClientDownloadFile`。
- 目标行为：删除 Win7 桌面客户端下载映射，仅保留 Android 下载。

## API Contract

- 保留：`GET /showroom/client-downloads/android`。
- 删除：`GET /showroom/client-downloads/desktop-win7`。
- 数据库、请求体、响应 DTO：无变化。

## Auth, Permissions, Validation And Errors

- 保持现有 Controller 安全边界不变。
- Android 资源缺失时仍由 `ShowroomClientDownloadService` fail fast。
- 不为被删除的 Win7 能力增加兼容接口、空响应或默认成功。

## Prerequisites

- Java 17。
- Maven 可构建 `yudao-module-showroom`。
- Android APK 继续存在于 classpath。

## BDD

- Given Win7 客户端资产已批准删除, When 检查展厅客户端下载 Controller, Then 只存在 Android 下载映射。
- BDD: 下线 Win7 客户端下载 -> Given Win7 客户端资产已批准删除 / When 检查展厅客户端下载 Controller / Then 只存在 Android 下载映射。

## RED

- `ShowroomClientDownloadControllerTest#desktopWin7ClientShouldBeRetired` 已先于生产代码修改。
- Maven RED 尝试因同一工作区多路并发构建持续阻塞，超过 6 分钟无 surefire 报告；本任务 Maven PID `60176` 已停止。
- 可独立执行的跨前后端静态契约 RED 已确认后端和前端仍保留 Win7 引用。
- RED: `node tests\e2e\showroom-client-download-retirement-static.spec.js` -> FAIL，前端和后端仍存在 Win7 下载引用。

## GREEN

- `mvn -pl yudao-module-showroom "-Dtest=ShowroomClientDownloadControllerTest" test` -> PASS。
- Tests run: 3, Failures: 0, Errors: 0, Skipped: 0。
- GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomClientDownloadControllerTest" test` -> PASS。

## Contract Verification

- Android 下载测试继续验证 HTTP 200、APK content type、文件名、响应头和 PK 文件头。
- Win7 退休测试验证 Controller 不存在 `/desktop-win7` 映射，下载描述不存在 `DESKTOP_WIN7`。
- 跨前后端静态契约验证前端不再导出或调用 Win7 下载能力。

## Observability

- 无新增日志或指标；被删除路径由 Spring 路由层返回不存在。

## Blockers

- 无。

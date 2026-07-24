# 执行日志：product_001 发布 readback HTTP/1.1 修复

BDD: public readback 使用 Vite 兼容协议 -> Given Website 本机由 Vite dev server 暴露 scoped release JSON / When 后端发布后执行 public readback / Then readback 请求必须使用 HTTP/1.1，避免 Java HttpClient 默认协议探测导致超时。

BDD: 手动发布返回成功 releaseId -> Given 真实后台点击“手动发布展厅”且 Website 8083 正常 / When `/admin-api/showroom/release/publish` 完成 / Then 响应 `success=true` 且包含新 releaseId。

INFO: live reproduction -> Standalone Java `HttpClient` GET `http://127.0.0.1:8083/showroom/sites/yingtai-showroom/stages/TEST/release/current` with the current default request settings timed out after about 10 seconds, while Python/Node probes against the same URL returned `200 application/json` in under 100ms.

RED: `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomPublicReleaseReadbackVerifierTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 新增 `shouldUseHttp11ReadbackRequestsForViteWebsiteCompatibility` 断言三个 readback `HttpRequest` 必须指定 `HttpClient.Version.HTTP_1_1`，当前实现未指定版本。

GREEN: `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomPublicReleaseReadbackVerifierTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, `ShowroomPublicReleaseReadbackVerifier.fetchJson` 对 current、manifest、root document 三个 Website readback 请求均固定 HTTP/1.1。

GREEN: `mvn -pl yudao-server -am -DskipTests package` -> PASS, 修复后的 `yudao-server.jar` 已完成本机打包。

INFO: local backend restart -> 修复后 48081 使用新 `yudao-server.jar` 启动；本地启动补齐 DCC 下载加密必填参数，避免完整应用启动因缺少 `yudao.dcc.download.encryption.*` 快速失败。

GREEN: Playwright real admin manual publish -> PASS, 真实 UI 在 `/showroom/company` 点击“手动发布展厅”，请求 `POST http://localhost:48081/admin-api/showroom/release/publish`，payload 为 `{"siteKey":"yingtai-showroom","stage":"TEST"}`，响应 `code=0`，返回 `releaseId=20260531T183422Z-be276b74dfa8-428f69663d1f`。


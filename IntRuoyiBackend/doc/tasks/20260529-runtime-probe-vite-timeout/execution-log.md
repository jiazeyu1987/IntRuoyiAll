# 执行日志：修复运行控制台本地前端探针超时

## 2026-05-29

- BDD: Java 探针访问本地 Vite 前端 -> Given 本地 Vite 前端使用 HTTP/1.1 可返回首页 / When 运行控制台探针客户端请求该 URL / Then 探针必须在超时时间内返回 HTTP 状态码和耗时，而不是 request timed out。
- BDD: 运行控制台探针快照更新 -> Given 本地 backend、admin frontend 和 Website frontend 都已监听 / When 通过真实 `/probes/run` 执行探针 / Then 最新快照中 local 三个组件均为 PASS，且不生成新的探针失败告警。
- REPRO: `Invoke-WebRequest -UseBasicParsing http://127.0.0.1:8081/` -> PASS，HTTP 200。
- REPRO: `Invoke-WebRequest -UseBasicParsing http://127.0.0.1:4173/` -> PASS，HTTP 200。
- RED: 真实 `POST /admin-api/infra/runtime-control/probes/run` -> FAIL，`local/intruoyi-frontend` 与 `local/website-frontend` 返回 `NO_GO`，错误均为 `探针不可达：request timed out`。
- RED: `mvn -pl yudao-module-infra -Dtest=RuntimeOpsProbeDefaultHttpClientTest test` -> FAIL，预期原因：默认 JDK `HttpClient` 对本地前端服务发起 h2c upgrade 请求，伪造前端服务不返回可用响应，客户端在 300ms 内 `request timed out`。
- GREEN: `mvn -pl yudao-module-infra -Dtest=RuntimeOpsProbeDefaultHttpClientTest test` -> PASS，1 test。
- GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeProbeServiceImplTest,RuntimeOpsProbeDefaultHttpClientTest" test` -> PASS，2 tests。
- GREEN: 重启本地后端并复跑真实 `POST /admin-api/infra/runtime-control/probes/run` -> PASS，`local/intruoyi-backend`、`local/intruoyi-frontend`、`local/website-frontend` 全部返回 `PASS`，`alert=null`。

## Bug Regression Evidence

- Bug: 运行控制台 Java 探针访问本地 Vite 前端时超时，导致 `local/intruoyi-frontend` 与 `local/website-frontend` 被误判为 `NO_GO`。
- Expected: 本地前端 URL 已返回 HTTP 200 时，后端探针必须返回真实 HTTP 状态码和耗时。
- Reproduction: 真实 `POST /admin-api/infra/runtime-control/probes/run` 在修复前返回两个本地前端 `request timed out`，而 PowerShell 直接访问两个 URL 均为 HTTP 200。
- Root Cause: JDK `HttpClient` 默认请求版本会向本地前端服务发起 h2c upgrade 形态的请求，Vite 本地服务路径上未在探针超时时间内返回可用响应。
- RED: `mvn -pl yudao-module-infra -Dtest=RuntimeOpsProbeDefaultHttpClientTest test` -> FAIL，预期失败为 `request timed out`。
- GREEN: `mvn -pl yudao-module-infra -Dtest=RuntimeOpsProbeDefaultHttpClientTest test` -> PASS。
- Verification: `mvn -pl yudao-module-infra "-Dtest=RuntimeProbeServiceImplTest,RuntimeOpsProbeDefaultHttpClientTest" test` -> PASS；真实 `/probes/run` -> PASS，9 个探针全绿。
- Blockers: None.

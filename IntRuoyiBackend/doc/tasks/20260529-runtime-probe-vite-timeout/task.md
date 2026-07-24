# 任务：修复运行控制台本地前端探针超时

## 任务目标

- 修复运行控制台 `/infra/runtime-control/probes/run` 对本地 Vite 前端 `http://127.0.0.1:8081/` 与 Website `http://127.0.0.1:4173/` 探针超时的问题。
- 保持 fail-fast：探针失败必须继续返回真实错误，不引入 mock、fallback 或静默降级。
- 修复后重新运行真实本地探针，确认 `local/intruoyi-frontend` 与 `local/website-frontend` 返回 PASS。

## BDD 场景

- BDD: Java 探针访问本地 Vite 前端 -> Given 本地 Vite 前端使用 HTTP/1.1 可返回首页 / When 运行控制台探针客户端请求该 URL / Then 探针必须在超时时间内返回 HTTP 状态码和耗时，而不是 request timed out。
- BDD: 运行控制台探针快照更新 -> Given 本地 backend、admin frontend 和 Website frontend 都已监听 / When 通过真实 `/probes/run` 执行探针 / Then 最新快照中 local 三个组件均为 PASS，且不生成新的探针失败告警。

## 里程碑

- [x] M1：记录复现证据并新增失败回归测试。
- [x] M2：最小修复探针 HTTP 客户端。
- [x] M3：运行目标后端测试通过。
- [x] M4：重启本地后端并执行真实 `/probes/run` 验证。
- [x] M5：更新任务证据、运行收尾清理预览并提交。

## 预期验证

- `mvn -pl yudao-module-infra -Dtest=RuntimeOpsProbeDefaultHttpClientTest test`
- `mvn -pl yudao-module-infra "-Dtest=RuntimeProbeServiceImplTest,RuntimeOpsProbeDefaultHttpClientTest" test`
- 真实 API：`POST http://127.0.0.1:48081/admin-api/infra/runtime-control/probes/run`

## 当前状态

completed

## 当前进展

- 已定位根因：`RuntimeOpsProbeDefaultHttpClient` 默认 `HttpClient` 在本地前端探针上触发超时，实际 curl / PowerShell 可正常访问。
- 已将探针请求固定为 HTTP/1.1，保留 fail-fast 和既有超时语义。
- 已重启本地 `48081` 后端到新 jar `backend-probe-http11-20260529-100536.jar`，并重新执行真实 `/infra/runtime-control/probes/run`，local 三个组件全部 PASS，`alert=null`。
- 已执行 task-closeout-cleanup 预览，确认仅保留 `task.md` 与 `execution-log.md`，无删除项、无阻塞项。

## 最终验证结果

- PASS: `mvn -pl yudao-module-infra -Dtest=RuntimeOpsProbeDefaultHttpClientTest test`
- PASS: `mvn -pl yudao-module-infra "-Dtest=RuntimeProbeServiceImplTest,RuntimeOpsProbeDefaultHttpClientTest" test`
- PASS: `mvn -pl yudao-server -am -DskipTests package`
- PASS: 真实 `POST /admin-api/infra/runtime-control/probes/run`，9 个探针全部 PASS。

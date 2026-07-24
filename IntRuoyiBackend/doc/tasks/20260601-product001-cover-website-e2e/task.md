# 任务：product_001 发布 readback HTTP/1.1 修复

## 任务目标

修复本机 `product_001` 透明封面 E2E 中 `/admin-api/showroom/release/publish` 手动发布失败的问题。后端 public readback 访问 `http://127.0.0.1:8083/showroom/sites/yingtai-showroom/stages/TEST/release/current` 时必须兼容本机 Website Vite dev server，并成功返回 releaseId。

## BDD 场景

- BDD: public readback 使用 Vite 兼容协议 -> Given Website 本机由 Vite dev server 暴露 scoped release JSON / When 后端发布后执行 public readback / Then readback 请求必须使用 HTTP/1.1，避免 Java HttpClient 默认协议探测导致超时。
- BDD: 手动发布返回成功 releaseId -> Given 真实后台点击“手动发布展厅”且 Website 8083 正常 / When `/admin-api/showroom/release/publish` 完成 / Then 响应 `success=true` 且包含新 releaseId。

## 里程碑

- [x] M1：建立任务文档与 BDD 场景。
- [x] M2：增加 RED 测试复现 readback 未强制 HTTP/1.1。
- [x] M3：最小修复 `ShowroomPublicReleaseReadbackVerifier`。
- [x] M4：运行 GREEN/回归验证并回到真实 UI E2E。

## 预期验证

- `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomPublicReleaseReadbackVerifierTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- 真实 UI 手动发布返回 `success=true` 和 releaseId。

## 当前状态

status: completed

最终结果：`ShowroomPublicReleaseReadbackVerifier` 的 Website readback 请求已固定为 HTTP/1.1，定向测试通过；修复后本机 48081 重新打包启动，真实后台 UI 手动发布成功返回 `releaseId=20260531T183422Z-be276b74dfa8-428f69663d1f`。

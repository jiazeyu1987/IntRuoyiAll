# 任务：修复正式与备份前端 PDF worker MIME 异常

## 任务目标

- 修复运行控制台中 Production、Backup 前端显示 `pdfWorker=ERROR: expected application/javascript but got application/octet-stream`。
- 保持状态探测严格要求真实 `application/javascript`，不得把 `application/octet-stream` 静默视为成功。
- 明确区分代码模板、发布包与远端当前运行配置，避免后续发布再次出现 `.mjs` MIME 错误。

## 前序任务检查

- 已确认上一任务 `doc/tasks/20260601-runtime-control-overview-timeout/task.md` 状态为 `completed`。
- 当前仓库存在无关未跟踪 `runtime/`，本任务不触碰、不提交。

## BDD 场景

- BDD: PDF worker 以 JavaScript MIME 提供 -> Given 前端受控文件预览依赖 `/pdfjs/pdf.worker.mjs` / When 浏览器请求该 worker / Then 响应 `Content-Type` 必须为 `application/javascript`。
- BDD: 状态探测暴露真实 MIME 错误 -> Given 远端 worker 返回 `application/octet-stream` / When 运行控制台探测前端 / Then 对应单元格应显示异常原因，不得降级为运行中。

## 里程碑

- [x] M1：建立任务文档、BDD 场景与预期验证。
- [x] M2：复现 Production、Backup MIME 异常并定位当前运行配置来源。
- [x] M3：增加失败回归测试并最小修复。
- [ ] M4：验证代码、发布包或远端运行配置恢复，记录证据。
- [ ] M5：收尾清理预览并提交本任务改动。

## 预期验证

- `HEAD http://172.30.30.57:8081/pdfjs/pdf.worker.mjs` 与 `HEAD http://172.30.30.59:8081/pdfjs/pdf.worker.mjs` 修复前可复现 `application/octet-stream`。
- 回归测试先 RED 后 GREEN。
- 状态脚本仍严格要求 `application/javascript`。
- 修复后 Production、Backup 对该 worker 返回 `application/javascript`。

## 当前状态

status: blocked_on_missing_dcc_runtime_secrets

## 当前结论

- Test 服务器 `/pdfjs/pdf.worker.mjs` 返回 `application/javascript`；Production、Backup 当前运行服务仍返回 `application/octet-stream`。
- 仓库内 Nginx 模板与状态脚本已正确，问题来源是 Production/Backup 当前运行包或前端容器配置仍为旧状态。
- 已补充发布脚本 `Wait-HttpContentTypeOk` 门禁，后续发布会在前端首页 HTTP 200 后继续验证 PDF worker MIME，不再把该异常发布为成功。
- 用户已在 2026-06-01 授权并触发测试服部署发布包 `26-06-01 15:37:24`，运行控制操作 `9a11bc63-3fa2-41ff-83c2-56e55c70688d` 在部署前置检查中 fail-fast。
- 当前本机用户/进程环境缺少 DCC viewer token、OnlyOffice 与下载加密运行时密钥，不能安全生成测试服远端 `.env`，不得用默认值、mock 值或临时绕过继续发布。
- 远端运行状态恢复仍需要重新发布或重建 Production/Backup 前端容器；该操作涉及正式/备份服务器，等待用户明确授权，并且必须先补齐本次暴露的 DCC 运行时密钥前置条件。

## 当前阻塞

- 缺少 `DCC_VIEWER_TOKEN_HMAC_SECRET`，脚本要求至少 32 字符显式 HMAC secret。
- 继续部署还需要显式提供 `DCC_ONLYOFFICE_JWT_SECRET`、`DCC_ONLYOFFICE_BASE_URL`、`DCC_ONLYOFFICE_PUBLIC_FILE_BASE_URL`、`DCC_DOWNLOAD_ENCRYPTION_POLICY_VERSION`、`DCC_DOWNLOAD_ENCRYPTION_KEY_ID`、`DCC_DOWNLOAD_ENCRYPTION_BASE64_KEY`、`DCC_DOWNLOAD_ENCRYPTION_ARTIFACT_DIRECTORY`。
- 影响：发布脚本无法写入完整远端 compose `.env`，测试服部署不能继续，M4/M5 暂不能完成。

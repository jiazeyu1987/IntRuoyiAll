# 执行日志：修复正式与备份前端 PDF worker MIME 异常

BDD: PDF worker 以 JavaScript MIME 提供 -> Given 前端受控文件预览依赖 `/pdfjs/pdf.worker.mjs` / When 浏览器请求该 worker / Then 响应 `Content-Type` 必须为 `application/javascript`。

BDD: 状态探测暴露真实 MIME 错误 -> Given 远端 worker 返回 `application/octet-stream` / When 运行控制台探测前端 / Then 对应单元格应显示异常原因，不得降级为运行中。

REPRO: `Invoke-WebRequest -UseBasicParsing -Method Head -Uri http://172.30.30.58:8081/pdfjs/pdf.worker.mjs` -> HTTP 200 `application/javascript`。

REPRO: `Invoke-WebRequest -UseBasicParsing -Method Head -Uri http://172.30.30.57:8081/pdfjs/pdf.worker.mjs` -> HTTP 200 `application/octet-stream`。

REPRO: `Invoke-WebRequest -UseBasicParsing -Method Head -Uri http://172.30.30.59:8081/pdfjs/pdf.worker.mjs` -> HTTP 200 `application/octet-stream`。

GREEN: `python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -k "frontend_nginx_serves_pdf_worker_modules_as_javascript or status_script_reports_onlyoffice_and_frontend_pdf_worker_health"` -> PASS，现有 Nginx 模板与状态脚本均已严格要求 `application/javascript`。

RED: `python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -k "publish_script_verifies_frontend_pdf_worker_mime_after_deploy"` -> FAIL，发布脚本缺少 `Wait-HttpContentTypeOk`，部署成功门禁没有校验 `/pdfjs/pdf.worker.mjs` MIME。

GREEN: `python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -k "publish_script_verifies_frontend_pdf_worker_mime_after_deploy or frontend_nginx_serves_pdf_worker_modules_as_javascript or status_script_reports_onlyoffice_and_frontend_pdf_worker_health"` -> PASS，发布后会检查 PDF worker 返回 `application/javascript`。

GREEN: PowerShell parser `ParseFile(script/deploy/publish-int-ruoyi.ps1)` -> PASS。

REGRESSION: `python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py` -> FAIL，存在 3 个既有旧断言未通过：NAS 共享名默认值断言、两个固定 `docker compose up -d onlyoffice backend frontend` 断言；与本次 PDF worker MIME 发布门禁无关。

BLOCKER: Production 与 Backup 当前运行包仍返回 `application/octet-stream`，需要用户明确授权后按修正后的发布流程重新发布或重建前端容器；本次未对正式服/备份服执行变更操作。

BDD: 测试服部署必须显式携带 DCC 运行时密钥 -> Given 已构建发布包 `26-06-01 15:37:24` 且用户触发测试服部署 / When `publish-int-ruoyi.ps1 -Mode deploy-release -Environment test` 生成远端 compose `.env` / Then DCC viewer token、OnlyOffice 与下载加密配置必须全部来自显式运行时变量，缺失时 fail-fast，不得使用默认值、mock 值或降级发布。

RED: 运行控制操作 `9a11bc63-3fa2-41ff-83c2-56e55c70688d` -> FAIL，发布包已从 NAS 下载，但脚本在远端部署前 fail-fast：缺少 `DCC_VIEWER_TOKEN_HMAC_SECRET`，DCC viewer token 要求显式 HMAC secret 且至少 32 字符。

VERIFY: 本机运行时环境盘点 -> FAIL，`DCC_SIGNATURE_EVIDENCE_HMAC_SECRET` 与 `DCC_SIGNATURE_EVIDENCE_KEY_VERSION` 已存在；`DCC_VIEWER_TOKEN_HMAC_SECRET`、`DCC_ONLYOFFICE_JWT_SECRET`、`DCC_ONLYOFFICE_BASE_URL`、`DCC_ONLYOFFICE_PUBLIC_FILE_BASE_URL`、`DCC_DOWNLOAD_ENCRYPTION_POLICY_VERSION`、`DCC_DOWNLOAD_ENCRYPTION_KEY_ID`、`DCC_DOWNLOAD_ENCRYPTION_BASE64_KEY`、`DCC_DOWNLOAD_ENCRYPTION_ARTIFACT_DIRECTORY` 在当前发布进程可见环境中缺失。影响：不能写出完整远端 `.env`，测试服部署必须停止。

# Execution Log

用户要求：使用新的识别前后端，在批记录表单页签中导入 `C:\Users\BJB110\Desktop\文档\批记录压力泵.doc` 解析并生成 V14 版本。

## BDD

- BDD: import pressure pump Word as V14 through UI -> Given 本机前后端运行且用户已登录到批记录表单页签, When 上传并解析 `批记录压力泵.doc`, Then 系统应生成目标批记录的 V14 版本并在页面或只读核验中可见。
- BDD: no backend bypass for version generation -> Given 需要验证新的识别前后端, When 执行导入, Then 必须走真实页面上传路径，不得用 API-only、SQL 直塞或 mock 数据替代。

## Command And Evidence Log

- PRECHECK: `docs/task-closeout-rules.md`, `docs/local-runtime.md`, `docs/login-access.md`, `docs/e2e-rules.md`, `docs/database-rules.md`, `docs/powershell-encoding.md`, `docs/backend-development.md`, `docs/worktree-restrictions.md`, `playwright` skill read -> PASS。
- PRECHECK: `Get-Command npx` -> PASS，`D:\Programs\npx.ps1`。
- PRECHECK: source DOC exists -> PASS，`C:\Users\BJB110\Desktop\文档\批记录压力泵.doc`。
- GREEN: frontend health -> PASS，`http://127.0.0.1:8081/` 返回 200。
- GREEN: backend package -> PASS，`mvn -pl yudao-server -am -DskipTests package` 构建 `yudao-server-exec.jar` 成功；SHA256 `E9042EBCAA4C7F403B6D287FBE3F397F729C7FE704B1D15E52511DE7DC7F84F8`。
- GREEN: backend restart -> PASS，旧 48081 后端退出后使用本机 `java -jar yudao-server-exec.jar --server.port=48081 --spring.profiles.active=local` 重启，最终 `/actuator/health` 返回 `{"status":"UP"}`。
- RED: `node doc\tasks\20260726-batch-record-import-v14\import-v14-ui.e2e.cjs` with broad keyword `压力泵` -> FAIL，真实页面导入成功但下拉误选相邻产品 `按压式球囊扩充压力泵`，预检目标为 `V4.0`，不满足用户要求的 `V14`；证据 `artifacts/02-word-import-preflight.png`。
- GREEN: script hardening -> PASS，将页面选择逻辑改为按项目名称精确匹配，而不是选择包含关键词的第一项；该改法是全局下拉选择规则，不针对某个表单或工序特例。
- GREEN: `BATCH_RECORD_IMPORT_PRODUCT_KEYWORD=球囊扩张压力泵 BATCH_RECORD_IMPORT_PRODUCT_EXACT=球囊扩张压力泵 BATCH_RECORD_IMPORT_EXPECT_VERSION=V14 node doc\tasks\20260726-batch-record-import-v14\import-v14-ui.e2e.cjs` -> PASS through real UI import; 页面截图 `artifacts/03-v14-list-visible.png` 显示 `V14.0`。
- GREEN: `node doc\tasks\20260726-batch-record-import-v14\verify-v14-ui.cjs` -> PASS，只读页面会话 API 核验 `球囊扩张压力泵 / V14.0` 返回 15 条批记录表单，`versionStatus=PENDING_APPROVAL`，源文件名 `批记录压力泵.doc`；截图 `artifacts/04-v14-final-verification.png`，数据 `artifacts/v14-readonly-verification.json`。

## Current Status

completed

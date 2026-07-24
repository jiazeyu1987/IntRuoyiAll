# 执行日志：签名记录跳转到对应表单

INFO: skill -> 使用 `bug-regression-fix-loop` 与 `frontend-feature-delivery`，并读取 bug/frontend 证据契约。

INFO: experience-index -> matched `docs/login-access.md`, `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`。

BDD: 签名记录进入对应表单 -> Given 用户打开电子签名一级页签的批记录签名记录 / When 点击某条真实签名记录的执行编号 / Then 页面进入该执行记录的表单视图并展示电子批记录表单与签名证据。

BDD: 签名入口不进入详情摘要 -> Given 签名记录行存在 executionId / When 前端构造跳转 / Then 使用 /mes/pro/feedback/edhr-execution/form 且携带 viewMode=tracking，不再使用 /mes/pro/feedback/edhr-execution/detail 作为签名入口目标。

RED: node tests/e2e/edhr-signature-change-execution-entry-static.spec.js -> FAIL，当前 `SignaturePage.vue` 仍将签名记录执行编号跳转到 `/mes/pro/feedback/edhr-execution/detail`，缺少 `/mes/pro/feedback/edhr-execution/form` 表单入口与 `viewMode=tracking`。

GREEN: node tests/e2e/edhr-signature-change-execution-entry-static.spec.js -> PASS，签名记录执行编号已改为 `/mes/pro/feedback/edhr-execution/form`，并携带 `viewMode=tracking`。

GREEN: node tests/e2e/signature-governance-e2e-static.spec.js -> PASS。

GREEN: node scripts/signature-governance-page-contract.test.mjs -> PASS，5 tests passed。

GREEN: $env:NODE_OPTIONS='--max-old-space-size=8192'; npm run ts:check -> PASS。

GREEN: node tests/e2e/edhr-signature-page-ui-static.spec.js -> PASS。

GREEN: node tests/e2e/edhr-tracking-execution-entry-static.spec.js -> PASS。

GREEN: experience-preflight -> PASS，`npx --version` 返回 `11.6.2`，本机前端 `http://localhost:8081/login?redirect=/index` HTTP 200，后端 `http://127.0.0.1:48081/actuator/health` 返回 `{"status":"UP"}`；真实 E2E 使用本机测试租户 `测试租户/aoteman/111111`，只做读路径点击验证，不写业务数据、不切换环境。

BLOCKER: real Playwright E2E first run -> FAIL，PowerShell 管道导致脚本内中文 placeholder selector 变成问号，登录页用户名输入框定位失败；已按登录门禁输出 DOM 输入框探针，未继续盲等。

GREEN: real Playwright E2E -> PASS，真实登录 `测试租户/aoteman/111111`，登录请求 `tenant-id=122`；打开 `/signature-governance?tab=batch-signatures`，点击真实签名记录 `BRE202606241216518420560`，进入 `/mes/pro/feedback/edhr-execution/form?id=560&viewMode=tracking`，目标页展示 `电子批记录表单`，未展示 `执行摘要`，`/mes/pro/batch-record-execution/signature-page` 与 `/mes/pro/batch-record-execution/get` 均成功。

GREEN: task-closeout-cleanup --mode preview -> PASS，未发现需要删除的临时产物。

GREEN: git commit -> PASS，前端提交 `8c6a914dd`。

# eDHR 字段审计真实路径 E2E Evidence

- Task ID: `20260528-edhr-field-audit-real-e2e-gate`
- 生成时间：2026-05-30T16:22:45.870Z
- 前端 worktree：D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3
- 固定前端入口：`http://localhost:8081`
- 默认测试租户：`测试租户`
- 默认账号名：`aoteman`；密码由 `EDHR_FIELD_AUDIT_PASSWORD` 或登录基线注入，不写入仓库证据。
- 真实 E2E 复跑命令：`pnpm e2e:edhr:field-audit`
- 静态语法检查命令：`pnpm e2e:edhr:field-audit:check`
- 证据文件：默认写入本任务目录 `doc/tasks/20260528-edhr-field-audit-real-e2e-gate/real-e2e-evidence.md`，作为可提交任务证据。
- 临时产物目录：`test-results/edhr-field-audit/`（截图、trace、result.json 与下载文件不提交）
- 当前状态：PASS
- executionId：`56`

## BDD

- BDD: 字段审计列表可追溯 -> Given 测试租户存在真实字段审计执行记录 / When 用户登录并打开 `/mes/pro/feedback/edhr-field-audit?executionId=<id>` / Then 前端请求真实 `/field-audit/page` 并展示执行编号、字段路径、旧值、新值、原因、修改人、签名和 hash 状态。
- BDD: 字段审计详情可核验 -> Given 列表中存在可点击的真实审计行 / When 用户点击“详情” / Then 页面进入 `/mes/pro/feedback/edhr-field-audit/detail` 并展示 items 字段路径、旧值、新值、原因、修改人、签名或审计 hash 以及 hashVerification。
- BDD: 字段审计链可校验 -> Given 详情页已加载真实审计批次 / When 用户点击“校验链” / Then 前端调用真实 `/field-audit/verify-chain` 且返回的 hashVerification.status 必须为 VALID。
- BDD: 字段审计链可导出 -> Given 真实审计链可校验 / When 用户点击“导出审计链” / Then 前端调用真实 `/field-audit/export` 并返回 fileName、contentType、sha256、recordCount、hashVerification 与非空 content。
- BDD: 字段审计定位执行记录 -> Given 字段审计列表展示目标审计行 / When 用户点击“定位执行记录” / Then 前端进入 `/mes/pro/feedback/edhr-execution/detail?id=<executionId>` 并展示同一执行记录。

## GREEN

- GREEN: `pnpm e2e:edhr:field-audit` -> PASS, 真实字段审计列表、详情、校验链和导出已完成。
- 字段审计列表可追溯 -> PASS, screenshot: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\test-results\edhr-field-audit\01-field-audit-list.png`
- 字段审计详情可核验 -> PASS, screenshot: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\test-results\edhr-field-audit\02-field-audit-detail.png`, detail.executionId=56, detail.executionCode=BRE202605281813460410056, detail.hashVerification=VALID, detail.items=1
- 字段审计链可校验 -> PASS, screenshot: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\test-results\edhr-field-audit\03-field-audit-verified.png`, verify.hashVerification=VALID, verifiedCount=1, fieldAuditRevision=1, fieldAuditHeadHash=ccfedd36c3aa3c7ca54775a1e4b523ca720f2eb0329f7511eb2c19657e6dd6c5, cellValuesHash=99aa9365df92f620ab78c5c3433817423c1a2bf8b24455370c3765fe0b5c6c50
- 字段审计链可导出 -> PASS, screenshot: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\test-results\edhr-field-audit\04-field-audit-exported.png`, fileName=field-audit-56.xlsx, contentType=application/vnd.openxmlformats-officedocument.spreadsheetml.sheet, sha256=8a0091687c963fea719b681891e75edb8869892bd6aa4b9d61148ca81fd0c93e, recordCount=1, downloadedSha256=8a0091687c963fea719b681891e75edb8869892bd6aa4b9d61148ca81fd0c93e
- 字段审计定位执行记录 -> PASS, screenshot: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\test-results\edhr-field-audit\05-field-audit-open-execution.png`
- Trace: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\test-results\edhr-field-audit\trace.zip`

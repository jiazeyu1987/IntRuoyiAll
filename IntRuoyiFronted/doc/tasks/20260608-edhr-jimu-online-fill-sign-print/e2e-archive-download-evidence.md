# eDHR 最终表单归档下载真实路径 E2E Evidence

- 生成时间：2026-06-08T07:18:51.754Z
- 固定入口：`http://localhost:8081`
- 测试租户：`测试租户`
- 账号名：`aoteman`；密码由环境变量注入，不写入证据。
- 当前状态：PASS

## BDD

- BDD: 最终表单可下载打印 -> Given 测试租户存在已关闭且 SEALED 的 eDHR 执行记录 / When 用户在详情页点击“下载归档打印件” / Then 浏览器通过真实 UI 请求归档下载接口，响应体非空，SHA-256 等于最新归档摘要，可作为最终表单打印件。

## GREEN

- GREEN: `node doc/tasks/20260608-edhr-jimu-online-fill-sign-print/scripts/edhr-archive-download-real-e2e.cjs` -> PASS
- executionId：`40`
- executionCode：`BRE202605280518101280040`
- archiveId：`25`
- archiveSha256：`6146b2141dabc9677c043802410d3c36b25b812466e1e4bc2dee15b7c50b03ca`
- downloadedSha256：`6146b2141dabc9677c043802410d3c36b25b812466e1e4bc2dee15b7c50b03ca`
- downloadedBytes：`14740`
- downloadedType：`application/pdf`
- 已关闭执行详情可见 -> PASS, screenshot: `D:\ProjectPackage\Int\IntRuoyi\worktrees\edhr_jimu\yudao-ui-admin-vue3\test-results\edhr-archive-download\01-sealed-archive-visible.png`
- 重新生成最终表单归档 -> PASS, screenshot: `D:\ProjectPackage\Int\IntRuoyi\worktrees\edhr_jimu\yudao-ui-admin-vue3\test-results\edhr-archive-download\02-archive-regenerated.png`
- 归档打印件下载响应 SHA-256 校验 -> PASS, screenshot: `D:\ProjectPackage\Int\IntRuoyi\worktrees\edhr_jimu\yudao-ui-admin-vue3\test-results\edhr-archive-download\03-archive-download-clicked.png`
- Trace: `D:\ProjectPackage\Int\IntRuoyi\worktrees\edhr_jimu\yudao-ui-admin-vue3\test-results\edhr-archive-download\trace.zip`

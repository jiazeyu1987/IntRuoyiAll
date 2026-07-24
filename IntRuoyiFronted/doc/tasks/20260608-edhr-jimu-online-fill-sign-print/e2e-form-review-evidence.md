# eDHR 表单复核签名真实路径 E2E Evidence

- 生成时间：2026-06-08T05:55:08.611Z
- 固定入口：`http://localhost:8081`
- 测试租户：`测试租户`
- 账号名：`aoteman`；密码由环境变量注入，不写入证据。
- 当前状态：PASS

## BDD

- BDD: 同一张表单追加复核签名 -> Given 测试租户存在草稿、待审批或已驳回 eDHR 执行记录 / When 用户在详情页点击“复核签名”并输入当前账号密码 / Then 后端 `/cosign` 返回 `FORM_REVIEW`，签名页展示“表单复核”记录，且签名证据绑定当前 cellValuesHash、fieldAuditRevision、fieldAuditHeadHash。

## GREEN

- GREEN: `node doc/tasks/20260608-edhr-jimu-online-fill-sign-print/scripts/edhr-form-review-real-e2e.cjs` -> PASS
- executionId：`72`
- executionCode：`BRE202605310021328320072`
- signatureId：`124`
- cellValuesHash：`84b9a938bd9a94b26da55f087f6a2fab21c438a2333cfb6d45fc85e34388690b`
- fieldAuditRevision：`0`
- fieldAuditHeadHash：`c89790f1db795880e667042c652ac63aaba03b9a91c1a14ae34c7d0fbf855a42`
- 找到真实复核候选记录 -> PASS, screenshot: `D:\ProjectPackage\Int\IntRuoyi\worktrees\edhr_jimu\yudao-ui-admin-vue3\test-results\edhr-form-review\01-candidate-detail.png`
- 表单复核签名提交 -> PASS, screenshot: `D:\ProjectPackage\Int\IntRuoyi\worktrees\edhr_jimu\yudao-ui-admin-vue3\test-results\edhr-form-review\02-form-review-signed.png`
- 签名页展示 FORM_REVIEW -> PASS, screenshot: `D:\ProjectPackage\Int\IntRuoyi\worktrees\edhr_jimu\yudao-ui-admin-vue3\test-results\edhr-form-review\03-signature-page-form-review.png`
- Trace: `D:\ProjectPackage\Int\IntRuoyi\worktrees\edhr_jimu\yudao-ui-admin-vue3\test-results\edhr-form-review\trace.zip`

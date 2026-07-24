# eDHR 附件上传真实路径 E2E Evidence

- Task ID: `20260612-edhr-attachment-prepare-upload-api`
- 状态：PASS
- 前端入口：`http://localhost:8081`
- 测试租户：`测试租户`；账号名默认 `aoteman`，密码和签名密码由环境变量注入，不写入仓库。
- 真实 E2E 命令：`pnpm e2e:edhr:attachment-upload`
- 静态语法检查命令：`pnpm e2e:edhr:attachment-upload:check`

## BDD

- BDD: 真实页面附件上传预登记 -> Given 测试租户存在真实 DRAFT 执行记录且模板包含附件字段 / When 操作员登录执行页并选择文件 / Then 前端必须调用真实 `/mes/pro/batch-record-execution/attachment/prepare-upload` 并获得 fileId、storagePath、sha256 与 storageRetentionHash。
- BDD: 附件签名保存进入审计链 -> Given prepareUpload 返回完整结构化元数据 / When 操作员填写原因并输入电子签名密码保存 / Then 前端必须调用真实 `/field-audit/save-changes`，请求体包含 attachmentChanges，响应 hashVerification.status 必须为 VALID。
- BDD: 保存后附件当前态可见 -> Given 附件保存成功 / When 执行页刷新详情 / Then 页面“当前附件证据”展示同一文件名、sha256 和附件 Hash。

## Result

- GREEN: 真实附件上传、签名保存和当前附件证据展示已完成。
- executionId：`326`
- workTaskId：`103`
- fileName：`edhr-attachment-1781272423266.txt`
- sha256：`c6e224125df18357d63def782a469bd15a313a7f9f5f0e02c2995f107dcc1813`
- attachmentHash：`053062f244f9c9245808e40776b0ab22256397f282b4a29bc890a4611655d712`
- DB 只读附件账本核验：PASS，记录数 `1`，auditBatchId `2065432367519318000`，signatureId `911`。
- Trace：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\test-results\edhr-attachment-upload\trace.zip`

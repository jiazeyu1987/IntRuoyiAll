# eDHR Phase 2 迁移体验真实 E2E Evidence

- Task ID: `20260708-edhr-version-implementation`
- 生成时间：2026-07-09T02:34:38.359Z
- 命令：`pnpm e2e:edhr:batch-version-phase2`
- 状态：PASS
- 批记录名称：`E2E-PHASE2-1783564189622`
- 写入租户：`测试租户/aoteman`
- 只读复验：`芋道源码/admin`

## BDD

- BDD: structured migration diff -> Given 测试租户已有已批准 V1 / When aoteman 通过页面导入同名 Word 升级 / Then 生成六类结构化迁移差异且 CONFIRM_REQUIRED 阻断审批。
- BDD: draft reupload -> Given V2 因 CONFIRM_REQUIRED 处于 PRECHECK_FAILED / When aoteman 在治理页选择真实 Word 和真实产品名称重新上传 / Then 旧版本 VOIDED，新版本生成，并带有新的迁移证据。
- BDD: confirm migration -> Given 新版本存在 CONFIRM_REQUIRED / When aoteman 在治理页填写确认意见并授权确认 / Then 页面显示确认审计，审批就绪变为是，版本进入 PRECHECK_PASSED。
- BDD: admin read-only verification -> Given 芋道源码/admin 登录同一路径 / When 查询同一定义和版本 / Then 不发送治理写请求。
- BLOCKED marker: 缺少本机运行态、真实测试租户产品数据或 EDHR_PHASE2_ADMIN_PASSWORD 时必须失败，不允许 mock 成功。

## GREEN

- GREEN: `pnpm e2e:edhr:batch-version-phase2` -> PASS，definitionId=`18`，v1=`31`，v2Voided=`32`，v3=`33`，adminWriteCount=`0`。

# eDHR 版本治理真实 E2E Evidence

- Task ID: `20260708-edhr-version-implementation`
- 生成时间：2026-07-09T02:48:49.093Z
- 真实 E2E 命令：`pnpm e2e:edhr:version-governance`
- 静态契约命令：`pnpm e2e:edhr:version-governance:check`
- 当前状态：PASS
- 前端入口：`http://127.0.0.1:8096`
- write tenant：`测试租户/aoteman`
- admin read-only verification：`芋道源码/admin`
- definitionId：`18`
- versionId：`33`

## BDD

- BDD: write tenant governance dashboard -> Given 测试租户存在真实批记录定义和版本 / When aoteman 登录进入版本治理页 / Then 页面展示治理看板、槽位版本化、影响面、巡检和运营指标。
- BDD: write tenant controlled rollback request -> Given 目标旧版本已审批通过 / When aoteman 从版本治理页提交受控回滚审批 / Then 仅调用 rollback/request 创建 BATCH_RECORD_VERSION / ROLLBACK 申请，不直接切换当前版本。
- BDD: admin read-only verification -> Given 芋道源码/admin 登录同一路径 / When 查看同一定义和版本 / Then 只允许读取治理接口，不允许发送 POST/PUT/PATCH/DELETE 写请求。

## GREEN

- PASS: 写入租户创建回滚申请 `EDHR-CHANGE-20260709104841`，admin 只读复验写请求数 `0`。


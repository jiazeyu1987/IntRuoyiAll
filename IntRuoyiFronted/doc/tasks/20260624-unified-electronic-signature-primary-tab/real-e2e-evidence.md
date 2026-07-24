# 真实数据 E2E 证据：统一电子签名一级页签

## 2026-06-24 融合后真实 E2E

- 环境：`int_main` 融合结果，前端 `http://localhost:8081`，后端 `http://localhost:48081`，本机 Docker MySQL/Redis。
- 登录：真实登录页，租户 `测试租户`，账号 `aoteman`，登录请求 `tenant-id=122`。
- 操作：打开 `/signature-governance`，依次点击 `总览`、`文件签名记录`、`批记录签名记录`、`用户授权`、`统一策略`。
- 结果：页面非 404，无可见错误；最终 URL `http://localhost:8081/signature-governance?tab=policy`。
- 真实接口：`/signature-governance/portal/overview`、`/signature-governance/policies/current`、`/dcc/electronic-signatures/page`、`/mes/pro/batch-record-execution/signature-page`、`/dcc/electronic-signature-authorizations/page` 均返回 HTTP 200、业务 `code=0`。

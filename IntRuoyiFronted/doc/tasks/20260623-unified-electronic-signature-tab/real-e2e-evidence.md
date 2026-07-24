# 真实数据 E2E 证据：统一电子签名页签

## 运行态归属

- 前端：`http://127.0.0.1:8086`，sign2 前端 worktree，commit `d98e3182d`。
- 后端：`http://127.0.0.1:48086`，sign2 后端 jar，commit `653b21782f`。
- 数据库：`jdbc:mysql://127.0.0.1:23306/ruoyi-vue-pro`。
- Redis：`127.0.0.1:26379`。
- 租户账号：`测试租户/aoteman`，密码按 `docs/login-access.md` 本机测试租户基线读取。

## 执行命令

- `mvn -pl yudao-server -am -DskipTests package` -> PASS。
- `node scripts/preflight/login-preflight.mjs --base-url http://127.0.0.1:8086 --tenant 测试租户 --username aoteman --password ****** --target-path /signature-governance --target-text 刷新电子签名` -> PASS。
- `SIGNATURE_GOVERNANCE_E2E_* node tests/e2e/signature-governance-policy.e2e.js` -> FAIL，真实返回 `portal overview must return READY`，实际为 `BLOCKED`。
- Playwright 真实浏览器诊断脚本抓取 `portal/overview` 与 `policies/current` -> PASS，接口均命中 `http://127.0.0.1:48086/admin-api/...`。
- Playwright 真实浏览器点击 DCC/eDHR 正式入口 -> PASS。
- `mvn -pl yudao-server -Dtest=SignatureGovernancePolicySourceConfigTest test` -> PASS，权威策略源配置回归测试通过。
- `mvn -pl yudao-server -am -DskipTests package` -> PASS，重新生成可执行 Spring Boot jar 并重启 sign2 后端。
- `SIGNATURE_GOVERNANCE_E2E_* node tests/e2e/signature-governance-policy.e2e.js` -> PASS，真实登录测试租户后统一电子签名策略页签达到 `READY`。
- `node D:\ProjectPackage\Int\IntRuoyi\scripts\preflight\login-preflight.mjs --base-url http://127.0.0.1:8086 --tenant 测试租户 --username aoteman --password ****** --target-path /signature-governance --target-text 刷新电子签名` -> PASS。

## 真实返回摘要

- `portal/overview`：HTTP 200，业务 `code=0`。
- 电子签名授权：`ENABLED`，`enabled=true`。
- DCC/eDHR 聚合摘要：`moduleTotal=2`，`pendingTotal=99`，`signatureTotal=811`。
- DCC：`pendingCount=98`，`signatureCount=0`，正式入口 `/dcc/controlled-file/signatures`，二级入口 `/dcc/controlled-file/approval-tasks`。
- eDHR：`pendingCount=1`，`signatureCount=811`，正式入口 `/mes/pro/feedback/edhr-signatures`，二级入口 `/mes/pro/feedback/edhr-work-task`。
- 页面可见阻断：DCC 和 eDHR 均显示 `POLICY_SOURCE_MISSING`，未发生 fallback 或静默通过。

## 导航验证

- 从统一页签点击 DCC `签名管理`：跳转到 `http://127.0.0.1:8086/dcc/controlled-file/signatures`，页面不是 404。
- 从统一页签点击 eDHR `签名记录`：跳转到 `http://127.0.0.1:8086/mes/pro/feedback/edhr-signatures`，页面不是 404。

## 历史阻塞（已修复）

真实数据 E2E 没有达到 `READY` 的原因是运行数据缺少权威电子签名策略源：

- DCC：`POLICY_SOURCE_MISSING`，`DCC authoritative signature policy source is missing`。
- eDHR：`POLICY_SOURCE_MISSING`，`EDHR authoritative signature policy source is missing`。
- `policies/current` 同时显示 Showroom 与 IntAuth 预留模块也缺少策略源。

处理结果：后端通过 `signature.governance.policy.modules` 正式配置补齐 DCC、EDHR、SHOWROOM、INTAUTH 的权威策略源、策略版本、确认状态、负责人和批准引用，并新增配置回归测试。该阻塞已不再复现。

## 最终放行结果

- `portal/overview`：真实浏览器登录测试租户后返回 `READY`，E2E 断言通过。
- `policies/current`：四个模块均满足 `policySourcePresent=true` 与 `authorityConfirmed=true`。
- `test-results/signature-governance/policy.json`：`status=PASS`，`verifiedAt=2026-06-23T08:09:00.660Z`。
- 结论：统一电子签名页签真实数据 E2E 已通过，旧 `POLICY_SOURCE_MISSING` 为已修复历史阻塞。

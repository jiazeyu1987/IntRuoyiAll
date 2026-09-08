# Worker Result

## 修改摘要

- `RuntimeTimeEvidenceExporter` 在生成 ZIP 前强制校验 `trusted-time-prod` 与 `trusted-time-audit` 两项可信时间证据；缺失、重复、缺少 `trustedTime` 或目标环境/主机错配时明确失败。
- `RuntimeTrustedTimeParser` 将同步 NTP Stratum 有效范围收紧为 `1..15`。
- eDHR 活动执行表单与只读表单统一复用 `selectLatestSignature`，按服务器 `signedAt` 优先、数值 `id` 次序选择最新签名；缺少有效 `id` 或服务器签署时间时 fail fast。

## 已验证项

- `mvn -q -pl yudao-module-infra -am "-Dtest=RuntimeOpsTrustedTime*Test,RuntimeOpsInspection*Test,RuntimeControlSpringWiringTest,RuntimeControlCanonicalContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。
- `mvn -q -pl yudao-module-mes -am "-Dtest=ExecutionArchiveRendererTest,MesProBatchRecordExecutionSignatureServiceTest,MesProEdhrBatchArchivePdfAComplianceTest,MesProBatchRecordExecutionArchiveContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。
- `node tests\e2e\edhr-latest-signature-selection-static.spec.js` -> PASS。
- `node tests\e2e\runtime-control-trusted-time-static.spec.js` -> PASS。
- `pnpm ts:check` -> PASS。
- `git diff --check` -> PASS，仅有 Windows LF/CRLF 提示。

## 未解决项

- 无本轮 required changes 遗留。

## 剩余风险

- 本轮为静态修复与本地定向回归；真实页面运行态已在 P4/P5 验证，P6 未新增真实 E2E 路径。

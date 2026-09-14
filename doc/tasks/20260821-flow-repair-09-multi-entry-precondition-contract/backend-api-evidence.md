# Flow 9 backend API evidence

## Controlled receipt API

- `POST /mes/pro/edhr-batch-entry-receipt/issue` (`mes:pro-edhr-batch-entry-receipt:issue`) accepts only source facts and an idempotency key. It never accepts issuer, signature, hash, issue time, expiry, status, or a complete receipt from the caller.
- `POST /mes/pro/edhr-batch-entry-receipt/verify` (`...:verify`) accepts only `receiptId`, expected `entryType`, and current `sourceSnapshotHash`; the service re-reads the persisted row by the security tenant.
- `POST /mes/pro/edhr-batch-entry-receipt/revoke` (`...:revoke`) accepts only `receiptId` and a reason. Revoke is backend-controlled and idempotent only as an explicit already-revoked error.

## Verification contract

The service recomputes the fixed-order canonical payload, SHA-256 `payloadHash`, HMAC-SHA256 signature using the configured backend secret, and `receiptHash = SHA-256(payloadHash + "|" + signature)`. It checks tenant, entry type, source snapshot, issuer system, lifecycle timestamps, status, revocation, credential version, persisted canonical payload, and audit/idempotency fields. Missing signing configuration fails with `PRO_EDHR_INDEPENDENT_RECEIPT_INVALID`; it never defaults to a trusted signature.

Flow 6 integration is intentionally an interface boundary: it must pass `receiptId` and the formal verification result to its own provision command. The Flow 9 verifier rejects missing/unknown IDs, stale source snapshots, tampered payload/hash/signature, expired rows, revoked rows, and tenant mismatches with stable error codes.

## Verification status

## Internal Port

- `MesIndependentBatchPrerequisiteReceiptPort#getVerifiedByReceiptId(tenantId, receiptId, entryType, sourceSnapshotHash)` 由持久化服务实现。实现按 receiptId/tenantId 重读数据库并复用严格 verify；调用方不得传入或信任完整凭证对象。
- 流程6建批主逻辑尚未接入该内部 port；本次仅提供正式读取/验真边界。

- Service contract tests cover issue, repeated verification, tampering, source change, expiry, revoke, cross-tenant access, and idempotency conflict.
- Full MES Maven compilation is currently blocked by unrelated untracked Flow 7 source references to missing `MesProEdhrBatchTraceSourcePrecheckRespVO`; no Flow 9 source error was reported before that external failure.
- 本轮新增测试编译还暴露并行流程4测试缺失 `MesFlow6CompletionBackfillReceipt.getBatchRecordStatus/getProcessInspectionStatus`，未修改该并行任务。

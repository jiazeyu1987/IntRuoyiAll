# Flow 9 backend API evidence

## Controlled receipt API

- `POST /mes/pro/edhr-batch-entry-receipt/issue` (`mes:pro-edhr-batch-entry-receipt:issue`) accepts only source facts and an idempotency key. It never accepts issuer, signature, hash, issue time, expiry, status, or a complete receipt from the caller.
- `POST /mes/pro/edhr-batch-entry-receipt/verify` (`...:verify`) accepts only `receiptId`, expected `entryType`, and current `sourceSnapshotHash`; the service re-reads the persisted row by the security tenant.
- `POST /mes/pro/edhr-batch-entry-receipt/revoke` (`...:revoke`) accepts only `receiptId` and a reason. Revoke is backend-controlled and idempotent only as an explicit already-revoked error.

## Verification contract

The service recomputes the fixed-order canonical payload, SHA-256 `payloadHash`, HMAC-SHA256 signature using the configured backend secret, and `receiptHash = SHA-256(payloadHash + "|" + signature)`. It checks tenant, entry type, source snapshot, issuer system, lifecycle timestamps, status, revocation, credential version, persisted canonical payload, and audit/idempotency fields. Missing signing configuration fails with `PRO_EDHR_INDEPENDENT_RECEIPT_INVALID`; it never defaults to a trusted signature.

Flow 6 integration is intentionally an interface boundary: it must pass `receiptId` and the formal verification result to its own provision command. The Flow 9 verifier rejects missing/unknown IDs, stale source snapshots, tampered payload/hash/signature, expired rows, revoked rows, and tenant mismatches with stable error codes.

## Verification status

- Service contract tests cover issue, repeated verification, tampering, source change, expiry, revoke, cross-tenant access, and idempotency conflict.
- Full MES Maven compilation is currently blocked by unrelated untracked Flow 7 source references to missing `MesProEdhrBatchTraceSourcePrecheckRespVO`; no Flow 9 source error was reported before that external failure.

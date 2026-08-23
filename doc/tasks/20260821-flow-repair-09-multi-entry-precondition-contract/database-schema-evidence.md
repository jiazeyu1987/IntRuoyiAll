# Flow 9 database schema evidence

Migration: `IntRuoyiBackend/sql/mysql/20260823_mes_independent_batch_prerequisite_receipt.sql`.

The table persists the canonical receipt identity and lifecycle (`receipt_id`, `tenant_id`, `entry_type`, `credential_version`, `status`, `issued_at`, `expires_at`, `revoked_at`, `revocation_reason`), all formal work-order/route/batch/source relation IDs and snapshot hashes, backend issuer/audit fields, `canonical_payload`, `source_evidence_json`, `payload_hash`, `receipt_hash`, `signature`, and `idempotency_key`. Tenant + receipt and tenant + entry type + idempotency keys are unique while deleted rows remain distinct.

The migration is additive and carries release metadata. It was not applied to a real database in this task; runtime schema and rollback execution remain Flow 11 / release-operations gates and are explicitly NOT RUN.

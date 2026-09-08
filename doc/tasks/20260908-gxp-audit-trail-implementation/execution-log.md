# Execution Log - 20260908 GxP Audit Trail Implementation

## BDD Scenarios

BDD: GXP audit append success -> Given a registered GxP write operation with required reason and before/after state, When the business service commits the change through the audit contract, Then the audit event is appended in the same transaction with actor, timestamp, action, reason, before/after, hash and policy version.

BDD: Missing audit policy blocks business write -> Given a GxP write operation is not registered in the approved policy, When the business service attempts to commit the change, Then the audit contract fails with GXP_AUDIT_POLICY_NOT_FOUND and the business data is rolled back.

BDD: Missing required reason blocks business write -> Given a registered operation requires reason text, When the caller omits the reason, Then the audit contract fails with GXP_AUDIT_REASON_REQUIRED and the business data is rolled back.

BDD: Audit append failure rolls back business write -> Given the database cannot append the audit event, When a GxP business write executes, Then no successful business state is committed and no default-success audit result is returned.

BDD: Idempotency conflict is rejected -> Given the same idempotency key was used with a different canonical payload, When the audit contract receives the second command, Then it fails with GXP_AUDIT_IDEMPOTENCY_CONFLICT.

BDD: Coverage gate rejects unregistered write entry -> Given a new GxP write method is added without policy registration, When the coverage check runs in CI, Then the check fails and reports the missing source locator and operationId.

## TDD Evidence

RED: pending -> write focused failing tests/contracts before production implementation.
GREEN: pending -> record passing commands after implementation.

## Current Notes

- 任务从文档任务中拆出，后续代码、测试、CI 证据均记录在本目录。
- E2E 未由用户当轮明确要求，本任务默认不执行 Playwright E2E。

# Flow 8 Four Material Gate

## Current Status

in_progress

## Goal

Implement a server-side batch-execution gate that requires the current valid versions of incoming inspection report, sterilization report, finished product inspection report, and finished product inspection record before returning MATERIALS_READY. The gate must not create batches or write the final RELEASED state.

## Milestones

- [x] M1: Confirm clean 8af0aa8f2f740cfa8e125a31e695bedbb4c9d619 baseline and task ownership.
- [x] M2: Record BDD and obtain a business RED test.
- [x] M3: Implement the minimal Flow8 gate and GREEN tests.
- [x] M4: Run targeted regression, compile, contract checks, and git diff --check.
- [x] M5: Commit only Flow8-owned code, tests, and task evidence (commit `609fcd5fc` / integrated as `4b764f835`).
- [x] M6: Re-run Flow8 targeted tests, MES compile, diff check, and runtime guard on `int_main` after integration.
- [ ] M7: Run real Playwright, migration verification, and formal Flow7 source integration evidence.

## Expected Verification

- Targeted JUnit tests prove all four independent material nodes are mandatory.
- Latest invalid or changed attachment evidence blocks readiness without falling back to an older version.
- Batch creation remains independent from material readiness.
- The gate returns MATERIALS_READY only and never writes RELEASED.
- Maven MES compile and git diff --check pass.

## Applicable Gates

- Use only persisted batch task and attachment evidence; no mock/default resolver or inferred source.
- A missing formal Flow7 Origin/TraceLink source must fail fast as an external integration blocker.
- Real Playwright remains NOT RUN until a writable tenant, role accounts, an existing batch execution, four cleanable files, and cleanup authority are supplied.

## Verification Summary

- Focused Flow8 tests: 10 passed, 0 failed, 0 errors.
- Latest focused source-binding recheck test: 1 passed, 0 failed, 0 errors; latest combined targeted run: 12 passed, 0 failed, 0 errors.
- Flow8 plus release regression: 40 passed, 0 failed, 0 errors.
- MES 24-module compile: PASS.
- Runtime guard: PASS for v6 slot 9 (`int_main`, frontend 8090, backend 48090).
- Expanded batch-execution suite: NOT GREEN because 167 tests cannot create the existing `MesBatchExecutionEntryContractService` bean and 7 task-gate tests target an absent legacy reflection signature; these failures predate and do not exercise the Flow8 gate.
- Real Playwright: NOT RUN because writable tenant, role accounts, an existing batch execution, four cleanable files, and cleanup authority were not supplied.

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；统一服务端门禁，不依赖前端隐藏或旧配置。
- 是否存在临时补丁或绕过：否。

# PQC Submit Receipt Route 404

## Task Goal

Fix and verify the missing request address `admin-api/mes/pro/feedback/frontline/device-account/pqc/submit-receipt` so PQC formal-submit recovery can read the submitted receipt by `pqcTaskId`.

## Milestones

- [x] Create task evidence and load applicable project gates.
- [x] Reproduce the committed-baseline route gap.
- [x] Add backend regression coverage for the submit receipt endpoint.
- [x] Run targeted verification and record RED/GREEN evidence.
- [x] Complete closeout records.

## Expected Verification

- `git show HEAD:IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/feedback/MesFrontlineDeviceAccountController.java | rg -n "pqc/submit-receipt|getPqcSubmitReceipt"` proves committed baseline lacks the receipt route.
- Targeted Maven test for `MesFrontlinePqcSubmitReceiptControllerTest` passes with `-pl yudao-module-mes -am`.
- Bug regression evidence validator passes before cleanup; retained `verification-report.md` records the validator PASS after cleanup deletes temporary evidence.

## Applicable Gates

### Backend endpoint gate

- Keep the route in the MES backend controller; do not hide a missing endpoint with frontend fallback, mock success, or empty success.
- Verify API behavior with backend controller coverage and run affected module tests with Maven reactor `-am` if sibling modules are involved.

### Frontend write/receipt recovery gate

- `submit-receipt` is the formal read-only receipt confirmation path after an uncertain PQC submit response.
- Missing receipt confirmation is a blocker because the frontend cannot safely distinguish “submitted but response lost” from “not submitted”; do not allow duplicate re-submit as recovery.

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；用正式后端 GET 回执端点和回归测试锁定路由。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed

# 20260802 Local MinIO Object Storage E2E Preflight

## Task Goal

Restore only the local IntRuoyi development object storage prerequisite required by DCC E2E, so `upload-preview` no longer fails because `127.0.0.1:9000` refuses connections.

## Scope

- Allowed: inspect existing local configuration and restore the project-standard local MinIO/object storage service.
- Forbidden: modify DCC business code, switch storage implementation, mock upload success, use API-only upload verification, use SQL to change business state, print storage secrets.

## Milestones

1. Read required project rules and create task evidence files. `completed`
2. Identify object storage configuration source and standard startup method. `completed`
3. Restore the existing local object storage service without changing DCC code. `completed`
4. Verify `127.0.0.1:9000` listens and backend health remains available. `completed`
5. Trigger DCC `upload-preview` through the real frontend page path and record PASS/BLOCKED evidence. `completed`

## Expected Verification

- `127.0.0.1:9000` is listening and belongs to a project-approved local object storage service.
- Backend `http://127.0.0.1:48081/actuator/health` reports `UP`.
- A real Playwright page flow triggers `upload-preview` and file upload preview succeeds, or a precise E2E BLOCKED reason is recorded.
- No API-only or SQL state-change workaround is used.

## Current Status

ready_for_closeout

## Applicable Gates

- E2E must use real frontend Playwright page operations; API may only support final read-only verification.
- Element Plus upload verification must prove the file is accepted by the visible upload component or that the target upload request is triggered.
- DCC `upload-preview` must not be replaced by mocked upload success or direct API-only file creation.
- Secrets must not be printed in commands, logs, or reports.
- Current task-owned Playwright verification script is retained as evidence:
doc/tasks/20260802-local-minio-object-storage-e2e-preflight/dcc-upload-preview-only-e2e.cjs

## Final Evidence

- Backend runtime was restored on `48081`; health returned `UP` after the standard local runtime restarted.
- Frontend `http://127.0.0.1:8081/` returned HTTP `200`.
- MinIO `http://127.0.0.1:9000/minio/health/ready` returned HTTP `200`.
- Real Playwright page flow `node doc/tasks/20260802-local-minio-object-storage-e2e-preflight/dcc-upload-preview-only-e2e.cjs` passed with non-admin user `pengyunfeng`.
- `upload-preview-result.json` records `status=PASS`, run ID `20260802090735`, task-owned file number `CODX-MINIO-PRE-20260802090735`, `previewFileName=批记录节点-解析样本.docx`, `previewKind=OFFICE`, and empty `targetNetworkFailures` / `consoleErrors` / `pageErrors`.
- DCC business code, storage implementation, SQL state, and mock behavior were not changed.

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，先核查项目配置来源和标准启动方式，再恢复正式本机对象存储依赖。
- `是否存在临时补丁或绕过`：否。

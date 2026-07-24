# 执行日志：补齐正式发布 eDHR S3 Object Lock 配置

BDD: 正式发布缺少 eDHR S3 配置时必须失败 -> Given 发布脚本进入 `build-release` / When `EDHR_S3_ENDPOINT` 等必需变量缺失 / Then 发布必须 fail-fast，并指出缺少的变量。

BDD: 正式发布 eDHR Object Lock 目标必须真实可验证 -> Given 已补齐 `EDHR_S3_*` / When 发布脚本运行 verifier / Then verifier 必须真实访问 S3、校验 versioning、Object Lock、retention 与 legal hold，返回 PASS 后才允许继续发布。

GREEN: NAS config path restore -> PASS, copied existing local NAS release config to `runtime/runtime-control/nas-release-config/ec4db044-0abf-4910-a4bf-190e97c7c01d.json`; secret value redacted from task evidence.

BLOCKED: local `EDHR_S3_*` presence check -> all 8 required variables are missing from current process environment, and repository search found no production-ready non-placeholder eDHR S3 target config.

VERIFY: production MinIO read-only bucket check -> PASS, production server has running MinIO and `yudao` bucket, but `yudao` is un-versioned and does not support locking; it is not a valid eDHR protected storage target.

GREEN: production eDHR protected bucket provisioning -> PASS, created/configured `edhr-protected-storage-20260601` on production MinIO with versioning enabled and default Object Lock retention `COMPLIANCE` / `7DAYS`; created a dedicated MinIO user and attached a bucket-scoped policy; secrets are not recorded.

GREEN: Windows user environment config -> PASS, persisted all 8 required `EDHR_S3_*` variables to the current Windows user environment; access key and secret key redacted from evidence.

GREEN: `python -X utf8 tool\edhr-storage-retention-verifier\verify.py` -> PASS, bucket `edhr-protected-storage-20260601`, versionId `edbe0867-0dba-40d9-85a7-660656a171bf`, retentionMode `COMPLIANCE`, retainUntil `2026-06-08T07:08:17Z`, legalHoldStatus `ON`, protected-version delete rejected and protected version remained readable.

GREEN: local runtime control restart -> PASS, restarted local backend/frontend after loading persisted `EDHR_S3_*`; `http://localhost:48081/actuator/health` returned HTTP 200 and `http://localhost:8081/` returned HTTP 200.

NOTE: failed first setup attempt left an unused empty production MinIO bucket `edhr-protected-storage`; it is not referenced by local `EDHR_S3_BUCKET` and was not deleted because production storage deletion requires explicit approval.

GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260601-release-edhr-s3-env-config --mode preview` -> PASS, status `ready`, keep only `task.md` and `execution-log.md`, no delete, no blocked, no warnings.

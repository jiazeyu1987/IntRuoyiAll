# CI/CD Environment Evidence: backup/prod publish parity

## Environment

- Local repo: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`.
- Publish script: `script/deploy/publish-int-ruoyi.ps1`.
- Runtime-control environments: `prod` host `172.30.30.57`, `backup` host `172.30.30.59`.
- `prod` remote MinIO source: `ragflow_compose-minio-1`.
- `backup` remote MinIO source: `intruoyi-minio`.
- Backup data disk/release root: `/mnt/intruoyi-data/runtime-data` and `/mnt/intruoyi-data/intruoyi-releases` on `/dev/mapper/cl-home`.

## Commands

- Build/test gate: `mvn -pl yudao-module-infra "-Dtest=RuntimeControlServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`.
- Script contract gate: `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py script/tests/test_runtime_control_scripts.py script/tests/test_runtime_control_ops_scripts.py -q`.
- PowerShell parser gate: parse `script/deploy/publish-int-ruoyi.ps1` with `[System.Management.Automation.Language.Parser]::ParseFile`.
- Deploy command shape when MinIO source is configured: `publish-int-ruoyi.ps1 -Mode deploy-release -Environment prod|backup -RequireTested -ConfirmText PROD -RemoteMinioContainer <environment-profile-value>`.

## Secrets

No secrets are committed. NAS credentials are still written at operation time to runtime-control state from the configured NAS settings service. MinIO access key and secret are read from the declared remote container only for with-data MinIO sync. DCC and eDHR secrets remain explicit fail-fast prerequisites.

## Pipeline

This repository uses local Maven and pytest gates for the runtime-control and deploy-script contract. No CI provider workflow file was changed in this task.

## Verification

- Maven runtime-control test passed, 32 tests.
- Script contract pytest passed, 66 tests.
- Target publish selector pytest passed, 4 tests.
- PowerShell parser passed.
- Authorized backup MinIO readiness passed: `intruoyi-minio` running, host `9000` live, backend container can reach `host.docker.internal:9000`, `yudao` bucket exists.
- Backup deploy-release for tested code-only package `26-06-02 20:13:57` reached final smoke after service startup; final image smoke failed because the package carries no MinIO snapshot and the newly created backup MinIO has no matching object baseline.

## Rollback

Rollback is a code rollback of the task-specific changes in:

- `yudao-module-infra/src/main/java/cn/iocoder/yudao/module/infra/framework/runtimecontrol/config/RuntimeControlProperties.java`
- `yudao-module-infra/src/main/java/cn/iocoder/yudao/module/infra/service/runtimecontrol/RuntimeControlOperationAction.java`
- `script/deploy/publish-int-ruoyi.ps1`
- related tests and task evidence

If rolled back, backup deploy-release will again risk reading the historical global RagFlow container default.

## Blockers

Backup with-data deploy remains blocked by release-artifact validity, not by target MinIO infrastructure. NAS package `20260603_website_assets_cache_immutable` has prod history and tested evidence but declares `publishScope=with-data` while missing the required MySQL dump and MinIO snapshot. The corrected script fails fast on that mismatch.

The tested code-only package `26-06-02 20:13:57` cannot populate the newly provisioned backup MinIO. Final backup smoke failed on showroom image readback with HTTP 200 `application/json` instead of `image/*`. Remote production validation was not run in this task.

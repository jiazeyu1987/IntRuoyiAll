# 20260528-edhr-protected-storage-publish-gate

## Task Goal

回答并落实“目标环境 protected storage/Object Lock 配置是否要修改发布脚本”：发布脚本必须在测试发布、正式晋级、直发正式路径中 fail-fast 校验 eDHR 目标环境 `EDHR_S3_*` 前置条件，运行真实 storage retention verifier，且把通过校验的目标配置写入目标运行时 `.env` 与数据库 `infra_file_config`，确保后端 eDHR 归档实际使用可验证的 protected storage。

不得在仓库、脚本默认值、文档、日志或测试中写入真实密钥、默认密钥或 mock 成功路径。

## Milestones

- M1: 用 BDD + RED 测试定义发布脚本 eDHR protected storage 门禁契约。
- M2: 修改发布/晋级脚本与 compose runtime env，使缺失或验证失败时在构建、MinIO 同步、数据库重置前停止。
- M3: 更新任务证据并运行脚本契约测试、存储保留契约测试、TDD 合规检查。
- M4: reviewer 放行并提交本任务直接相关改动。

## Expected Verification

- `python -X utf8 -m pytest script/tests/test_edhr_protected_storage_publish_tooling.py -q`
- `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py script/tests/test_edhr_storage_retention_contract.py -q`
- `python -X utf8 tool/verify_tdd_compliance.py --task-dir doc/tasks/20260528-edhr-protected-storage-publish-gate`
- `git diff --check`

## Current Status

- status: completed
- owner: main reviewer
- subagent evidence:
  - Explorer `019e6e29-aacc-7d53-925c-fd792b3a01bb` inspected deploy scripts and recommended fail-fast verifier gates before build/sync/DB reset.
  - Explorer `019e6e29-ed87-7f73-86ad-d19e7f301624` inspected deploy tooling tests and recommended static contract tests for EDHR_S3 publish gates.

## Implementation Summary

- `script/deploy/publish-int-ruoyi-to-test.ps1`
  - Reads required `EDHR_S3_*` values from operator environment or explicit parameters.
  - Fails fast before backend build, MinIO sync, database reset, and backend start when any value is missing or invalid.
  - Runs `tool/edhr-storage-retention-verifier/verify.py` and accepts only JSON `status=PASS` with exit code `0`.
  - Writes verified values into target `.env` and `post-import.sql`.
  - Recreates `infra_file_config` id `28` as the master S3 config with `objectLockRequired=true`, retention mode/days, and legal hold requirement.
- `script/deploy/promote-int-ruoyi-test-to-prod.ps1`
  - Requires target production `EDHR_S3_*` values from operator environment.
  - Runs the same verifier before tested-runtime promotion proceeds.
  - Writes target production `EDHR_S3_*` into production `.env` and post-import SQL.
- `script/deploy/int-ruoyi-test/docker-compose.yml`
  - Exposes `EDHR_S3_*` to the backend runtime environment for operational visibility and verifier reuse.
- `script/tests/test_edhr_protected_storage_publish_tooling.py`
  - Adds static BDD/TDD contract coverage for publish, promote, skip-minio non-bypass, direct-prod inheritance, and no hardcoded EDHR secrets.

## Verification Evidence

- RED: `python -X utf8 -m pytest script/tests/test_edhr_protected_storage_publish_tooling.py -q` failed before implementation.
- GREEN: `python -X utf8 -m pytest script/tests/test_edhr_protected_storage_publish_tooling.py -q` passed, 5 tests.
- GREEN: `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -q` passed, 28 tests.
- GREEN: PowerShell parser checks passed for publish and promote scripts.
- REGRESSION: `python -X utf8 -m pytest script/tests/test_edhr_storage_retention_contract.py -q` passed, 7 tests.
- REGRESSION: combined script/storage contract pytest passed, 40 tests.
- CHECK: `git diff --check` passed with only LF-to-CRLF warnings.
- CHECK: `python -X utf8 tool\verify_tdd_compliance.py --all-changed --task-dir ...\doc\tasks\20260528-edhr-protected-storage-publish-gate` passed.

## Reviewer Notes

- The scripts do not store real EDHR S3 keys in the repository.
- The verifier output is allowed because the verifier sanitizes access key and secret key values.
- Direct production publish inherits the PowerShell gate because the `.bat` wrapper still delegates to `publish-int-ruoyi-to-test.ps1`.
- The actual target environment must provide real `EDHR_S3_*` values before publish; missing values intentionally stop the release.
- Reviewer decision: PASS for this deploy-script gate slice. Target server publish itself was not executed in this turn because no real remote target `EDHR_S3_*` secrets were provided to this shell; the scripts now fail fast in that condition instead of proceeding.

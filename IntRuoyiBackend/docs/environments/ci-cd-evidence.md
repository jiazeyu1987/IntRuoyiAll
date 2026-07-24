# IntRuoyi Test Publish Evidence

## Environment

Current delivery target is the isolated IntRuoyi test runtime on `172.30.30.58`.

## Environments And Targets

- Source workspace:
  `D:\ProjectPackage\Int\IntRuoyi`
- Owning backend repository:
  `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Frontend source repository:
  `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Target environment:
  test server `172.30.30.58`
- Remote application directory:
  `/opt/intruoyi/runtime`
- Remote exposed ports:
  frontend `8081`, backend `48081`
- Remote runtime units:
  `intruoyi-mysql`, `intruoyi-redis`, `intruoyi-backend`, `intruoyi-frontend`

## Files Changed

- `script/deploy/int-ruoyi-test/docker-compose.yml`
- `script/deploy/int-ruoyi-test/Dockerfile.backend`
- `script/deploy/int-ruoyi-test/Dockerfile.frontend`
- `script/deploy/int-ruoyi-test/nginx.conf`
- `script/deploy/publish-int-ruoyi.ps1`

## Commands

This release used local build, image packaging, remote transfer, remote compose start, database import, and MinIO mirror commands.

## Build And Package Commands

- Backend package:
  `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-server -am -DskipTests package`
- Frontend package:
  `pnpm exec vite build --mode test`
  Runtime overrides used during build:
  `VITE_BASE_URL=http://172.30.30.58:48081`
  `VITE_BASE_PATH=/`
  `VITE_OUT_DIR=dist-intruoyi-test`
- Compose render validation:
  `docker compose -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\int-ruoyi-test\docker-compose.yml --env-file <temp-env> config`
- Release image export:
  `docker save -o <release-dir>\intruoyi-images_<tag>.tar intruoyi-backend:<tag> intruoyi-frontend:<tag>`

## Deploy Flow

1. Package the current backend jar and build the current Vue3 frontend static assets.
2. Build `intruoyi-backend:<tag>` and `intruoyi-frontend:<tag>` locally.
3. Dump the current local MySQL `ruoyi-vue-pro` database from `int-ruoyi-mysql`.
4. Copy the compose file, runtime `.env`, image tar, database dump, and post-import SQL into `/opt/intruoyi/releases/<tag>` on the test server.
5. Load the image tar on the test server.
6. Mirror the local MinIO bucket `yudao` into the test-server MinIO and set the destination bucket to public download.
7. Start remote MySQL and Redis with `docker compose up -d mysql redis`.
8. Import the current database dump into `intruoyi-mysql`.
9. Rewrite imported file URLs and the master MinIO config so the test runtime points to `172.30.30.58:9000` externally and `host.docker.internal:9000` internally.
10. Start `intruoyi-backend` and `intruoyi-frontend`.

## Secrets

This release requires runtime-only SSH, MySQL, and MinIO credentials. The repository stores no credential values; the publish script reads them from running containers or operator access at execution time.

## Required Secrets And Owners

- SSH access to `root@172.30.30.58`.
  Owner: current operator / server administrator.
- Local MySQL root password from the running `int-ruoyi-mysql` container.
  The publish script reads it from container environment at runtime and does not store it in the repository.
- Local MinIO access key / secret from the running `docker-minio-1` container.
  The publish script reads them from container environment at runtime and does not store them in the repository.
- Remote MinIO access key / secret from the running `ragflow_compose-minio-1` container.
  The publish script reads them from remote container environment at runtime and does not store them in the repository.

## Pipeline

The effective release pipeline for this task is:
local package -> local image build -> image tar export -> remote transfer -> remote image load -> remote data sync -> remote compose start -> remote verification.

## RED Evidence

- Remote precheck initially failed because the test server had no existing IntRuoyi runtime slot; the deployment proceeded only after a new isolated slot was defined at `/opt/intruoyi/runtime` with ports `8081 / 48081`.
- The first end-to-end publish attempt failed because Docker could not fetch `eclipse-temurin:21-jre` from Docker Hub on the current machine.
- The second end-to-end publish attempt failed at MinIO sync because the `minio/mc` image entrypoint is `mc`, not `sh`.

## Verification

Verification covered local package/build success, remote service startup, remote HTTP health, synchronized MinIO file access, and rewritten database file URLs.

## GREEN Evidence

- Backend package completed successfully.
- Frontend build completed successfully.
- Compose render validation completed successfully.
- Remote image load completed successfully.
- Remote MySQL and Redis started successfully.
- Current local database dump imported successfully.
- Current local `yudao` MinIO bucket objects were mirrored successfully.
- Backend health endpoint returned `{"status":"UP"}` on the test server.
- Frontend returned HTTP `200` on the test server.
- A synchronized file object URL returned HTTP `200`.
- Remote MySQL verification showed `48` rows in `infra_file` and `48` rows already rewritten to `http://172.30.30.58:9000/...`.
- The current canonical test publish entrypoint is `script/deploy/publish-int-ruoyi.ps1 -Environment test`; it retains the verified local build, data sync, remote compose start, and HTTP readiness flow.

## Release Package NAS Cleanup Evidence

Environment: local release package build mode uses `script/deploy/publish-int-ruoyi.ps1 -Mode build-release` and uploads the package to the configured NAS release root.

Commands: targeted regression commands are `mvn --% -pl yudao-server -Dtest=PublishIntRuoyiScriptTest -Dsurefire.failIfNoSpecifiedTests=false test` and `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -k "disconnects_nas_mapping_idempotently or frontend_nginx_allows_large_showroom_product_import_requests" -q`.

Pipeline: after `Release package uploaded to NAS`, `Disconnect-NasReleaseShare` now routes SMB cleanup through `Invoke-NasReleaseShareDisconnect`; only `net use /delete` exit code `2` with `NET HELPMSG 2250` is treated as an already-absent mapping, while all other non-zero cleanup results still fail the release.

Secrets: NAS server, share, username, and password still come from `NasConfigPath`; no credential values are stored in the repository or test code.

Verification: `PublishIntRuoyiScriptTest` verifies the guarded NAS cleanup helper exists and explicitly recognizes `NET HELPMSG 2250`; the script-level pytest also verifies the publish script keeps the 2250 cleanup rule and the frontend Nginx package keeps `client_max_body_size 300m;`.

Rollback: revert `script/deploy/publish-int-ruoyi.ps1` and `yudao-server/src/test/java/cn/iocoder/yudao/server/PublishIntRuoyiScriptTest.java`, then rerun the targeted Maven test before rebuilding a release package.

Blockers: none for the local script fix. Actual NAS availability and credentials remain runtime prerequisites for `build-release`.

## Artifacts And Release Output

- Remote backend image tag used in the successful release:
  `intruoyi-backend:20260518_202708`
- Remote frontend image tag used in the successful release:
  `intruoyi-frontend:20260518_202708`
- Remote access URLs:
  frontend `http://172.30.30.58:8081`
  backend health `http://172.30.30.58:48081/actuator/health`

## Blockers

All blockers are currently resolved. The initial missing deployment slot, local Docker Hub base-image pull failure, and `minio/mc` entrypoint mismatch were resolved during this task.

## Manual Gates And Blockers

- Manual decision gate:
  the operator approved a new isolated deployment slot instead of reusing unrelated `ragint-*` containers on the same test server.
- Resolved blocker:
  local base image download was unavailable, so the deployment-specific backend runtime image was changed to a locally cached Temurin 21 image family member without changing the application Java major version.
- Resolved blocker:
  remote MinIO bucket permission had to be updated to public download to match the imported file-config expectation `enablePublicAccess=true`.

## Rollback

1. Check the previous image tag if a rollback is needed:
   `ssh root@172.30.30.58 "docker images --format '{{.Repository}}:{{.Tag}}' | grep '^intruoyi-'"`
2. Edit `/opt/intruoyi/runtime/.env` and replace `IMAGE_TAG=<current>` with the previous known-good tag.
3. Recreate only the app containers:
   `ssh root@172.30.30.58 "cd /opt/intruoyi/runtime && docker compose up -d backend frontend"`
4. If the rollback must also restore data, import a known-good MySQL dump before recreating the backend.
5. Re-verify:
   `ssh root@172.30.30.58 "curl -fsS http://127.0.0.1:48081/actuator/health"`
   `ssh root@172.30.30.58 "curl -I -s http://127.0.0.1:8081/"`

## Internal Backend Runtime Base Image Evidence

Environment: local release package build mode uses `script/deploy/publish-int-ruoyi.ps1 -Mode build-release` and now requires the fixed internal backend runtime base image metadata before building the backend package image.

Artifact:

- Image: `intruoyi-backend-runtime-base:2026.06.05-jre21-noble-docker29.2.1`
- Version: `2026.06.05-jre21-noble-docker29.2.1`
- Image id: `sha256:b4f7d85f325665c3b372379e6d352c3c8be2d3a08add68ac0dc151f720160be7`
- Tar: `D:\ProjectPackage\Int\BaseImages\intruoyi-backend-runtime-base-2026.06.05-jre21-noble-docker29.2.1.tar`
- Tar SHA256: `5bcd568b46ba9f28bfa7ac8ee67e283aea95bd585a89842a14cc40827fa71603`

Commands:

- `powershell.exe -NoProfile -ExecutionPolicy Bypass -File script/deploy/build-backend-runtime-base-image.ps1 -Version 2026.06.05-jre21-noble-docker29.2.1`
- `docker load -i D:\ProjectPackage\Int\BaseImages\intruoyi-backend-runtime-base-2026.06.05-jre21-noble-docker29.2.1.tar`
- `docker image inspect intruoyi-backend-runtime-base:2026.06.05-jre21-noble-docker29.2.1 --format '{{.Id}}'`
- `docker build --no-cache --build-arg BACKEND_RUNTIME_BASE_IMAGE=intruoyi-backend-runtime-base:2026.06.05-jre21-noble-docker29.2.1 -t intruoyi-backend:codex-internal-base-verify -f script/deploy/int-ruoyi-test/Dockerfile.backend D:\ProjectPackage\Int\IntRuoyi`
- `python -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py script/tests/test_runtime_control_ops_scripts.py -q`
- `mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest test`

Secrets: none. The metadata is not a secret; NAS credentials and publish approvals remain runtime prerequisites and are not stored here.

RED evidence: base image creation initially failed on Ubuntu `security.ubuntu.com` and then `archive.ubuntu.com` apt source access while downloading Docker runtime packages. The base-image Dockerfile now declares `APT_MIRROR=http://mirrors.aliyun.com/ubuntu`, replaces both Ubuntu source hosts during the base image build, and keeps apt retry/timeout settings.

GREEN evidence: base image creation, tar export, tar SHA256 verification, `docker load`, image id verification, backend business image build from the internal runtime base image, script tooling pytest, and Runtime Control Java tests passed. The temporary `intruoyi-backend:codex-internal-base-verify` image was removed after verification.

Manual gates and blockers: real `build-release` still requires valid NAS configuration and operator authorization before uploading a package. This local configuration task did not connect to or modify the NAS release share.

Rollback: remove the user environment variables `INTRUOYI_BACKEND_RUNTIME_BASE_*` and `RUNTIME_CONTROL_BACKEND_RUNTIME_BASE_*`, remove the generated image and tar only after no release build references them, then restore `Dockerfile.backend-base` to the previous apt source policy and rerun the verification commands.

## Test Deploy Required SQL Evidence 2026-06-05

Environment: Runtime Control `publish-test` deploys NAS release package `26-06-05 09:49:55` to test server `172.30.30.58`.

Incident: the first deploy failed at `SHOWROOM_FILE_CONFIG_UNBOUND` because `infra_file_config.id=28` and 1434 `showroom/%` URLs still pointed at the production/source host `172.30.30.57` instead of the selected test target `172.30.30.58`. After authorized repair to `172.30.30.58`, the next deploy exposed a schema blocker: DCC code queried `dcc_controlled_file.product_name`, but the test database and existing release package had not applied `sql/mysql/20260604_dcc_controlled_file_product_name.sql`.

Fix: test server `infra_file_config.id=28` was rebound to `endpoint=http://host.docker.internal:9000`, `domain=http://172.30.30.58:9000/yudao`, `bucket=yudao`; the existing formal migration `20260604_dcc_controlled_file_product_name.sql` was applied to the test database; and `script/deploy/publish-int-ruoyi.ps1` now includes that migration in `$requiredDatabaseSqlScripts` so new release packages carry it.

Verification:

- Runtime Control operation `c3d258f7-1233-4a56-8cf9-0deaba31427e` succeeded and logged `Publish completed for test`.
- Test server health endpoints returned 200 for backend `48081`, frontend `8081`, Website `8083`, Website `/showroom`, and the selected showroom smoke image returned `HTTP 200 image/png`.
- `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py::test_publish_script_packages_and_applies_required_dcc_sql_for_all_deploys -q` passed.
- `python -X utf8 -m pytest script/tests/test_dcc_sql_scripts.py -q` passed.
- `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py script/tests/test_runtime_control_ops_scripts.py script/tests/test_restart_ruoyi_script.py -q` passed with 73 tests.

Rollback: if a rollback is required, deploy a prior tested release through Runtime Control and verify the same backend/frontend/Website/image smoke endpoints. Do not drop `dcc_controlled_file.product_name` without an explicit schema rollback plan, because current code reads this nullable column.

## Release Target Host Configuration Evidence 2026-06-05

Environment: Runtime Control and `script/deploy/publish-int-ruoyi.ps1` build/deploy release packages for test, production, and backup targets. The environment roles are test `172.30.30.58`, production `172.30.30.57`, and backup `172.30.30.59`, but the publish script must not hardcode those values.

Change:

- `publish-int-ruoyi.ps1` now accepts `-TestServerHost`, `-ProdServerHost`, and `-BackupServerHost` from server-side runtime-control configuration or process environment, and fails fast when build/deploy lacks required target hosts.
- Runtime Control build/deploy actions pass the configured target hosts into the publish script; deploy actions still pass the selected current `-ServerHost` for post-import SQL, remote probes, and MinIO URL checks.
- The remote compose template receives `RUNTIME_CONTROL_TEST_SERVER_HOST`, `RUNTIME_CONTROL_PROD_SERVER_HOST`, and `RUNTIME_CONTROL_BACKUP_SERVER_HOST` from the generated `.env`, so runtime-control inside the deployed backend also uses target configuration.

Commands:

- `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py::test_publish_script_uses_configured_target_hosts_instead_of_hardcoded_environment_ips script/tests/test_runtime_control_ops_scripts.py::test_test_server_compose_mounts_linux_backup_ops_runtime_prerequisites script/tests/test_runtime_control_scripts.py::test_publish_script_supports_backup_server_with_production_grade_confirmation -q`
- `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py script/tests/test_runtime_control_ops_scripts.py script/tests/test_runtime_control_scripts.py -q`
- `mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest test`

RED evidence: the new tests initially failed because the publish script still embedded environment IP mapping, compose did not inject runtime-control target host variables, and Runtime Control did not fail fast when a build-release target host was missing.

GREEN evidence: the targeted Python tests passed, the broader Python release/runtime-control script suite passed with 82 tests, and `RuntimeControlServiceImplTest` passed with 44 tests.

Manual gates and blockers: this change did not execute a real NAS upload or remote deploy. Real build/deploy still requires valid NAS release configuration, internal backend runtime base image metadata, and operator approval.

Rollback: restore the prior command-argument behavior and remove the generated `RUNTIME_CONTROL_*_SERVER_HOST` env entries only if the publish script is also restored to a known working target-host source. A rollback that leaves build/deploy without target host input will correctly fail fast.

## Test Database Quick SQL Apply Evidence 2026-06-15

Environment: Runtime Control adds `apply-test-db-sql` for test server `172.30.30.58` only. The action uploads one explicit local `.sql` file to `/opt/intruoyi/runtime/tmp/db-quick-apply`, executes it inside `intruoyi-mysql` against `ruoyi-vue-pro`, and checks backend `/actuator/health`.

Change:

- Added `script/deploy/apply-test-db-sql.ps1` with fail-fast gates for test server host proof, `.sql` extension, non-empty SQL file, SSH/SCP prerequisites, remote runtime dir, MySQL container, `system_tenant`, utf8mb4 MySQL execution, and backend health.
- Added Runtime Control action `apply-test-db-sql` and request field `sqlPath`; the action does not require NAS release repository arguments and does not mark release status.
- Added frontend operation entry `测试服数据库快应用` with required SQL file path input and explicit expected-result copy: only apply the selected SQL, no full database sync, no MinIO sync, no release package state change.

Commands:

- `python -X utf8 -m pytest script\tests\test_runtime_control_ops_scripts.py -q`
- `mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest#executeApplyTestDbSqlShouldDispatchOnlyTestServerScript+executeApplyTestDbSqlShouldRequireSqlPath+executeNonDbQuickApplyActionShouldRejectSqlPath test`
- `mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest test`
- `node tests\e2e\runtime-control-test-db-quick-apply-static.spec.js`
- `node tests\e2e\runtime-control-static.spec.js`
- `node tests\e2e\runtime-control-release-package-static.spec.js`

RED evidence: the new tests initially failed because the backend had no `sqlPath`, no `apply-test-db-sql` action, no quick apply script, and no frontend entry.

GREEN evidence: the script static tests passed with 6 tests, the targeted Runtime Control Java tests passed with 3 tests, the full `RuntimeControlServiceImplTest` passed with 65 tests, and the frontend static wiring tests passed.

Manual gates and blockers: this implementation did not connect to the test server and did not execute real SQL. A real run still requires explicit operator authorization and a reviewed SQL file with its own rollback plan. Broad frontend `pnpm ts:check` currently fails on unrelated `src/views/mes/pro/route-use/RouteUsePage.vue` type drift (`recordCategory` / `validationProfile` missing from batch record report saves), outside this task's modified files.

Rollback: remove the `apply-test-db-sql` action, `sqlPath` request field, frontend entry, and `script/deploy/apply-test-db-sql.ps1`. Existing publish, backup, rollback, and release status flows are not used by this quick apply path.

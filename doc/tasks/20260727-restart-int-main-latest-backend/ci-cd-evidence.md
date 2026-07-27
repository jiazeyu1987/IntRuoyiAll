# CI/CD Environment Evidence

## Environment

- Target runtime profile: local `int_main`.
- Target backend port: `48081`.
- Runtime workspace: `E:\IntRuoyi`.
- Build source commit: `97ecf51a1c1f930a6c9307646614418d4ab811dc`.
- Latest verified `origin/int_main`: `177ebefbb9195835ac47d55067306454c17644da`.
- Build and origin `IntRuoyiBackend` tree: `7c5ffc135ce21d4905b7b46d9747dee382578c51`.

## Build And Deploy Contract

- Build only from a clean, recorded remote commit.
- Run the focused MES regression before packaging.
- Package with the standard Maven reactor path.
- Record build and deployed Jar SHA-256 values and require exact equality.
- Stop only the listener whose command line is confirmed as the current `E:\IntRuoyi` backend.
- Do not change `application-local.yaml`, database credentials, service port, or data source.

## Commands

- Test: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellLinkAutoPersistServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" clean test`
- Package: `mvn.cmd -pl yudao-server -am "-DskipTests" package`
- Deploy: verify source tree and process ownership, stop PID `61040`, copy the verified Jar to the E-main target path, then start Java with `--server.port=48081 --spring.profiles.active=local`.
- Verify: `Invoke-RestMethod http://127.0.0.1:48081/actuator/health`.

## Artifact

- Build output: `D:\IntRuoyiWorktree\20260727_int_main_latest_backend_runtime\IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar`.
- Deployed output: `E:\IntRuoyi\IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar`.
- Size: `500645246` bytes.
- SHA-256: `89EB3023737BD704B92AB129C2D9176C392A6B7CE4D1E2DF2199128D02FCD98D`.

## Verification

- Focused MES regression: `4` tests passed, `0` failures, `0` errors, `0` skipped.
- Maven package: `BUILD SUCCESS`, all 30 reactor modules succeeded.
- Runtime: PID `44372`, port `48081`, health `UP`.
- API responsiveness: protected permission endpoint returned business code `401` in `164 ms`.
- Pipeline files changed: none.

## Rollback

- Preserve the previous runtime Jar with its SHA-256 before replacement.
- No automatic fallback is performed. If the new backend cannot become healthy, fail fast and report the preserved rollback artifact and exact failure.
- Preserved Jar: `E:\IntRuoyi\.runtime\int-main-backend\backups\yudao-server-exec-20260727-202427-7A3F2A015A08.jar`.
- Preserved SHA-256: `7A3F2A015A0816D9F6876DBAAE4D99DB1619F7C5011E79E0EF7D72AE43A7DA0C`.
- Manual rollback, only after explicit approval: stop the confirmed E-main listener, restore the preserved Jar, restart on `48081`, and repeat health/API verification.

## Secrets

- No secret values are added or recorded.
- Existing local profile configuration is reused without printing credentials.
- Required secret owner: existing local environment owner; this task introduced no new secret.

## Approval And Blockers

- Manual approval: the user explicitly authorized continuation and requested the latest backend restart.
- Runtime blocker: none.
- Closeout blocker: the E-main worktree contains unrelated concurrent dirty files, so cleanup must not merge or modify those files.

## Current Status

ready_for_closeout

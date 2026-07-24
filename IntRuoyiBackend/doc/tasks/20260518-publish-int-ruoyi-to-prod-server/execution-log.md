BDD: production wrapper should publish into an explicit isolated slot -> Given the production server currently has no `/opt/intruoyi/runtime` directory, no `intruoyi-*` containers, and no `yudao` MinIO bucket / When the operator runs `publish-int-ruoyi-to-prod.bat default --yes` / Then the release should create the isolated production runtime instead of overwriting an existing IntRuoyi deployment.
BDD: production release should complete end to end -> Given the production wrapper pins host `172.30.30.57` and the shared publish script is already verified on the test path / When the wrapper runs with explicit production confirmation bypass / Then it must complete backend build, frontend build, image transfer, MinIO sync, database import, remote startup, and live health verification on the production server.

- 2026-05-18 Asia/Shanghai: created this production release task after confirming the production wrapper task was already completed.
- GREEN: production slot precheck -> PASS, `172.30.30.57` had no `/opt/intruoyi/runtime`, no `intruoyi-*` containers, and no `yudao` MinIO bucket before the release.
- RED: first production publish attempts -> FAIL, real reruns exposed two release-script robustness issues on the fresh production slot:
  - `pnpm` invocation through the wrapper path needed Windows `.cmd` launcher preference instead of `.ps1`
  - remote MySQL import on reruns needed stale data-dir cleanup plus an explicit wait until MySQL accepted client connections
- GREEN: hardened `publish-int-ruoyi-to-test.ps1` for wrapper-driven releases by preferring sibling `.cmd` launchers, resetting the remote MySQL data directory before full database imports, and waiting for `mysqladmin ping --silent` success before import.
- GREEN: `cmd /c D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\publish-int-ruoyi-to-prod.bat default --yes` -> PASS, the production wrapper completed the full build, image export, remote transfer, MinIO sync, MySQL import, and remote startup flow.
- GREEN: production backend health -> PASS, `http://172.30.30.57:48081/actuator/health` returned `{"status":"UP"}`.
- GREEN: production frontend reachability -> PASS, `http://172.30.30.57:8081/` returned HTTP `200`.
- GREEN: production file-object proof -> PASS, `http://172.30.30.57:9000/yudao/dcc/original/20260518/审核会签.pdf` returned HTTP `200`.
- GREEN: production container state -> PASS, `intruoyi-backend` and `intruoyi-frontend` are running on image tag `20260518_225505`, with `intruoyi-mysql` and `intruoyi-redis` healthy.

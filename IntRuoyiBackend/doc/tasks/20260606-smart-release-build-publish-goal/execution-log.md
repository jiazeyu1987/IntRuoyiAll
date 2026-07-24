# Execution Log：Smart Release 构建并发布一次

BDD: 构建发布包成功 -> Given 本机具备离线后端基础镜像 tar、目标配置和发布参数 / When 运行 build-release / Then 产出发布包、manifest/report、后端镜像 tar 或包内产物，并返回成功。

BDD: 发布到目标环境成功 -> Given 已构建发布包且目标环境配置明确 / When 运行 deploy-release / Then 目标环境部署成功，部署日志和验证结果可追踪。

BDD: 缺少前置条件必须阻塞 -> Given 目标 host、基础镜像、resource proof 或 target config 缺失 / When 构建或发布 / Then fail fast，错误说明缺失项和影响，不 fallback。

## Evidence

GREEN: `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py script/tests/test_runtime_control_ops_scripts.py script/tests/test_runtime_control_local_config.py -q` -> PASS, 75 passed.

GREEN: `mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest test` -> PASS, 49 tests passed.

RED: `publish-int-ruoyi.ps1 -Mode build-release -ReleaseTag 20260606_smart_release_goal_0119 ...` -> FAIL, expected fail-fast reason: local backend process locked `yudao-server/target/yudao-server.jar`; script reported pid=13936 java.exe and instructed local backend restart before retry.

GREEN: `D:\ProjectPackage\Int\IntRuoyi\restart-ruoyi-backend.bat` -> PASS, local backend restart completed and `yudao-server/target/yudao-server.jar` lock check returned `UNLOCKED`.

RED: `publish-int-ruoyi.ps1 -Mode build-release -ReleaseTag 20260606_smart_release_goal_0120 ...` -> FAIL, expected reason: backend Maven build succeeded, then frontend Vite build failed on `vue/html-self-closing` for `yudao-ui-admin-vue3/src/views/showroom-admin/components/HallListTable.vue` lines 51 and 63.

GREEN: `NODE_OPTIONS=--max-old-space-size=8192 VITE_BASE_URL='' VITE_BASE_PATH=/ VITE_OUT_DIR=dist-intruoyi-test node yudao-ui-admin-vue3/node_modules/vite/bin/vite.js build --mode test` -> PASS, frontend build completed after replacing self-closing native `audio` tags with explicit closing tags.

GREEN: `publish-int-ruoyi.ps1 -Mode build-release -ReleaseTag 20260606_smart_release_goal_0121 ...` -> PASS, release package built and uploaded to NAS path `Backup/ReleasePackage/20260606_smart_release_goal_0121`; backend runtime base tar, tar sha256, image name, image id digest and version were supplied explicitly.

GREEN: `publish-int-ruoyi.ps1 -Mode deploy-release -ReleaseTag 20260606_smart_release_goal_0121 -Environment test ...` -> PASS, release package downloaded from NAS, images loaded on `172.30.30.58`, required SQL applied, backend/frontend/website services recreated, and script verified backend health, frontend root, PDF worker, showroom image proxy, Website root and Website showroom endpoints.

GREEN: `Invoke-WebRequest http://172.30.30.58:48081/actuator/health` -> PASS, HTTP 200 with `{"status":"UP"}`.

GREEN: `Invoke-WebRequest http://172.30.30.58:8081/` -> PASS, HTTP 200.

GREEN: `Invoke-WebRequest http://172.30.30.58:8083/showroom` -> PASS, HTTP 200.

GREEN: `task_closeout.py --task-id 20260606-smart-release-build-publish-goal --mode preview` in `ruoyi-vue-pro` -> PASS, keep only `task.md` and `execution-log.md`, delete none, blocked none.

GREEN: `task_closeout.py --task-id 20260606-smart-release-build-publish-goal --mode preview` in `yudao-ui-admin-vue3` -> PASS, keep `task.md`, `execution-log.md` and `bug-regression-evidence.md`, delete none, blocked none.

RED: `git commit -m "任务: 完成构建发布验证"` -> FAIL, expected repository compliance hook reason: backend production changes require a changed Java test under `src/test`.

GREEN: `mvn -pl yudao-module-infra -Dtest=RuntimeControlLocalConfigContractTest test` -> PASS, 1 test passed; Java contract now verifies `application-local.yaml` keeps runtime-control storage guard local log dir aligned to `${user.home}/logs`.

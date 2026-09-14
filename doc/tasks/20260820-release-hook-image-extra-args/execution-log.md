# Execution Log

- RESUME: 2026-08-20 继续仅测试服发布，`r260820e-r1` 已在 `publish-test` 的路线快照身份 BACKFILL Hook 阶段失败。
- BDD: release hook image consumes extra one-shot args -> Given publish-test launches the newly built backend image through docker compose run, When the script passes `INTRUOYI_EXTRA_ARGS`, Then the image command must append those args after compose `ARGS` so `--server.port=0` and scheduler-disable properties reach Spring Boot.
- GREEN: experience-preflight -> PASS，适用门禁为 clean worktree、committed-only release input、actual app Dockerfile source-of-truth、失败 tag 不复用、不得手工改包/远端 compose。
- RED: `python -X utf8 -m pytest script/tests/test_backend_release_image_contract.py -q` -> FAIL，`script/deploy/int-ruoyi-test/Dockerfile.backend` 缺少 `ENV INTRUOYI_EXTRA_ARGS=""`，镜像 `CMD` 只执行 `${ARGS}`。
- CHANGE: 主程序后端 Dockerfile 添加 `INTRUOYI_EXTRA_ARGS`，并在 `CMD` 中以 `${ARGS} ${INTRUOYI_EXTRA_ARGS}` 顺序追加，不覆盖 compose 运行参数。
- CHANGE: 路线快照迁移 Runner 成功执行后调用 `SpringApplication.exit(...)` 的返回码并执行进程退出，避免一次性 Hook 在成功路径继续驻留。
- GREEN: `git diff --check -- <task-owned files>` -> PASS。
- GREEN: `python -X utf8 -m pytest script/tests/test_backend_release_image_contract.py script/tests/test_runtime_control_scripts.py::test_remote_status_script_exposes_current_release_package_from_runtime_env -q` -> PASS，2 tests。
- RED: `mvn -pl yudao-module-mes "-Dtest=MesProRouteVersionSnapshotMigrationCommandTest" test` in `E:\IntRuoyi\IntRuoyiBackend` -> FAIL before Surefire because the shared main workspace contains unrelated dirty DCC/ERP API drift (`DccProjectCodeConfigurationQuery` and `ErpKingdeeProductionOrderClient` signatures). This is not a product assertion failure for the hook fix; clean release worktree build remains the required release verification.
- GREEN: prior isolated fix worktree command `mvn -pl yudao-module-mes -Dtest=MesProRouteVersionSnapshotMigrationCommandTest test` -> PASS，4 tests，same task-owned source patch before main-workspace selective commit.

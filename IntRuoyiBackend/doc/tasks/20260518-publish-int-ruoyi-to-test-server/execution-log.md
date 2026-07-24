BDD: 本地当前 IntRuoyi 状态发布到测试服务器且不影响其他应用 -> Given 测试服务器上当前承载的是其他业务容器而不是现成的 IntRuoyi 运行时 / When 执行本次本地到测试服发布 / Then 发布流程必须使用明确的 IntRuoyi 独立目录、独立容器名和独立端口，且不得覆盖无关服务。
BDD: 缺少明确部署位时必须失败直出 -> Given 测试服务器上还没有确认好的 IntRuoyi 部署目录、端口和运行方式 / When 执行发布前置检查 / Then 任务必须明确报告缺失前置条件和影响，而不是猜测部署位置或复用其他项目容器。

- 2026-05-18 Asia/Shanghai: created the deployment task package in `ruoyi-vue-pro` after confirming the latest same-repository backend task was already completed.
- 2026-05-18 Asia/Shanghai: inspected `D:\ProjectPackage\RagflowAuth\运维工具.bat` and traced its local-to-test release chain into `tool\\maintenance\\features\\release_publish_local_to_test.py`.
- 2026-05-18 Asia/Shanghai: confirmed the reusable reference pattern is `local build -> artifact staging -> scp to TEST -> remote load/recreate -> post-release verification`.
- RED: remote IntRuoyi runtime precheck on `172.30.30.58` -> FAIL, the server is reachable over SSH but currently has no existing `yudao/ruoyi/intruoyi` deployment directory or running container set for this application, so there is no safe in-place upgrade target yet.
- GREEN: `docker compose -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\int-ruoyi-test\docker-compose.yml --env-file <temp-env> config` -> PASS, the isolated test runtime renders valid compose configuration for `intruoyi-mysql`, `intruoyi-redis`, `intruoyi-backend`, and `intruoyi-frontend`.
- GREEN: `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-server -am -DskipTests package` -> PASS, the current backend state packaged successfully into `yudao-server.jar`.
- GREEN: `pnpm exec vite build --mode test` with deployment-specific runtime overrides -> PASS, the current Vue3 frontend built successfully into `dist-intruoyi-test`.
- RED: `powershell -NoProfile -ExecutionPolicy Bypass -File D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\publish-int-ruoyi-to-test.ps1` first run -> FAIL, the original backend Docker base `eclipse-temurin:21-jre` was not cached locally and Docker Hub token fetch was refused from the current machine.
- GREEN: switched the deployment-only backend image definition to a locally cached `maven:3.9.9-eclipse-temurin-21` base so the test release could continue without changing the application runtime Java major version.
- RED: `powershell -NoProfile -ExecutionPolicy Bypass -File D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\publish-int-ruoyi-to-test.ps1` second run -> FAIL at MinIO sync, because the `minio/mc` image entrypoint is `mc`, not `sh`.
- GREEN: `docker run --rm --add-host host.docker.internal:host-gateway --entrypoint /bin/sh minio/mc -c "mc alias set src ... && mc alias set dst ... && mc mb --ignore-existing dst/yudao && mc mirror --overwrite src/yudao dst/yudao"` -> PASS, the local `yudao` bucket objects were mirrored into the test-server MinIO.
- GREEN: `docker run --rm --add-host host.docker.internal:host-gateway --entrypoint /bin/sh minio/mc -c "mc alias set dst ... && mc anonymous set download dst/yudao"` -> PASS, the test-server `yudao` bucket was switched to downloadable public access to match `enablePublicAccess=true`.
- GREEN: `ssh root@172.30.30.58 "cd /opt/intruoyi/runtime && docker compose up -d mysql redis"` -> PASS, the isolated test MySQL and Redis services were started.
- GREEN: remote MySQL data sync -> PASS, the current local `ruoyi-vue-pro` database dump was imported into `intruoyi-mysql`.
- GREEN: remote post-import rewrite -> PASS, all `infra_file.url` rows now point at `http://172.30.30.58:9000/...` and the master `infra_file_config` endpoint/domain was rewritten for the remote runtime.
- GREEN: `ssh root@172.30.30.58 "cd /opt/intruoyi/runtime && docker compose up -d backend frontend"` -> PASS, the isolated backend and frontend containers were created and started.
- GREEN: remote backend health -> PASS, `curl http://127.0.0.1:48081/actuator/health` returned `{"status":"UP"}` after startup completed.
- GREEN: remote frontend health -> PASS, `curl -I http://127.0.0.1:8081/` returned `HTTP/1.1 200 OK`.
- GREEN: external backend/fronted reachability -> PASS, `http://172.30.30.58:48081/actuator/health` and `http://172.30.30.58:8081/` both responded from the operator machine.
- GREEN: synchronized file path proof -> PASS, `http://172.30.30.58:9000/yudao/dcc/original/20260513/dcc-sample.pdf` returned HTTP `200`.
- GREEN: `python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS, the deployment tooling contract test passed with `3 passed`.
- GREEN: `python C:\Users\BJB110\.codex\skills\ci-cd-environment-delivery\scripts\validate_cicd_environment.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\docs\environments\ci-cd-evidence.md` -> PASS, the delivery evidence satisfies the CI/CD validator contract.
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260518-publish-int-ruoyi-to-test-server --mode preview` -> PASS, closeout preview reported no blockers and no task-specific delete set.

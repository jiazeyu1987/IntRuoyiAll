# Execution Log

## 2026-07-27

- User intent: 本地访问受控浏览详情时，OnlyOffice 区域提示 `OnlyOffice 文档加载失败：错误码 -4，下载失败`。
- Screenshot evidence: 页面为文控中心受控浏览详情，文件名 `INT/RE/8.3-04（E/1）标签、说明书类打印及销毁记录表.xlsx`，右上角显示“受控预览 禁止截图/外传”。
- Rules loaded: `bug-regression-fix-loop`, `backend-api-delivery`, `docs/task-closeout-rules.md`, `docs/local-runtime.md`, `docs/worktree-restrictions.md`, `docs/backend-development.md`, `docs/frontend-development.md`, `docs/powershell-encoding.md`.
- Existing workspace state: branch `int_main` is ahead of origin and has unrelated dirty task documentation; this task will avoid unrelated files.
- BDD: 本地 OnlyOffice 受控预览可下载文档 -> Given 本地 `int_main` 前端和后端运行且用户打开受控浏览 xlsx 文件详情, When OnlyOffice 使用预览元数据中的 document URL 下载文件, Then 下载接口应返回有效文件内容而不是让 OnlyOffice 报 `-4 下载失败`。
- Root cause: OnlyOffice 文档服务运行在 Docker 容器内，预览元数据生成的 `onlyofficeDocumentUrl` 使用 `http://127.0.0.1:48081/.../onlyoffice-file` 时，容器内的 `127.0.0.1` 指向 OnlyOffice 容器自身，不是 Windows Host 后端。
- Reproduction: `docker exec onlyoffice curl http://127.0.0.1:48081/actuator/health` -> `000`；`docker exec onlyoffice curl http://host.docker.internal:48081/actuator/health` -> `200 192.168.65.254`。
- RED: `mvn -pl yudao-server -am "-Dtest=DccOnlyOfficeLocalConfigTest#localOnlyOfficePublicFileDefaultShouldBeReachableFromDockerDocumentServer" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，`application-local.yaml` 默认下载基址仍为容器不可达的 `127.0.0.1:${server.port}`。
- RED: `python -X utf8 -m pytest E:\IntRuoyi\IntRuoyiBackend\script\tests\test_dcc_onlyoffice_local_runtime_config.py -q` -> FAIL，新增本地 OnlyOffice 下载基址契约未满足。
- Implementation: `application-local.yaml` 的 `yudao.dcc.preview.onlyoffice.public-file-base-url` 默认值改为 `http://host.docker.internal:${server.port}`；Java/Python 静态测试的占位符解析改为支持嵌套 `${server.port}`。
- GREEN: `mvn -pl yudao-server -am "-Dtest=DccOnlyOfficeLocalConfigTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，`DccOnlyOfficeLocalConfigTest` 2 tests passed。
- GREEN: `python -X utf8 -m pytest E:\IntRuoyi\IntRuoyiBackend\script\tests\test_dcc_onlyoffice_local_runtime_config.py -q` -> PASS，3 passed。
- Runtime build: `mvn -pl yudao-server -am -DskipTests package` -> PASS；`yudao-server-exec.jar` SHA256 `E82735F6FE9F7D9D0C4667777B71466D7CE3EFC2B7C28F92372C8F548EBEF32C`。
- Runtime restart: confirmed old `48081` PID `23028` command line belonged to `E:\IntRuoyi\IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar`; stopped it, then replaced the temporary no-log `javaw.exe` start with the project restart script runtime process. Final `48081` PID `64760`, `/actuator/health` -> `{"status":"UP"}`。
- Runtime verification: rebuilt Jar contains `BOOT-INF/classes/application-local.yaml` line 310 with `public-file-base-url: ${DCC_ONLYOFFICE_PUBLIC_FILE_BASE_URL:http://host.docker.internal:${server.port}}`; actuator env reports property source as `application-local.yaml` line 310 with masked value.
- Route verification: unauthenticated `GET /admin-api/dcc/controlled-files/2054545668044052578/preview-metadata` returns business `401 账号未登录`, proving the route is loaded; full page verification with the user password was not automated to avoid recording credentials in command logs.
- Runtime cleanup: stopped transient non-listening local backend PID `60884`; final OnlyOffice container probe `host.docker.internal:48081/actuator/health` -> `200 192.168.65.254`。
- GREEN: evidence-validation -> PASS；`validate_bug_regression.py` and `validate_backend_api.py` accepted task evidence.
- GREEN: experience-consolidation -> PASS；added reusable local OnlyOffice Docker download URL gate to `docs/local-runtime.md` and route keyword to `docs/experience-index.md`.

# Bug Regression Evidence

## Bug Summary

本地受控浏览 xlsx 文件详情页显示 `OnlyOffice 文档加载失败：错误码 -4，下载失败`。

## Expected Behavior

OnlyOffice 容器根据预览元数据中的 `onlyofficeDocumentUrl` 能够从本机后端 `48081` 下载文件内容，页面不应因下载失败显示 `-4`。

## Reproduction

- 页面症状：受控浏览详情页 OnlyOffice 区域显示 `错误码 -4，下载失败`。
- 容器网络复现：`docker exec onlyoffice curl http://127.0.0.1:48081/actuator/health` -> `000`。
- 可达路径对照：`docker exec onlyoffice curl http://host.docker.internal:48081/actuator/health` -> `200 192.168.65.254`。

## Root Cause

本地 `application-local.yaml` 默认 `yudao.dcc.preview.onlyoffice.public-file-base-url` 使用 `http://127.0.0.1:${server.port}`。OnlyOffice 在 Docker 容器内下载文档时，`127.0.0.1` 指向容器自身，不是 Windows Host 上的后端服务，因此下载接口连接失败。

## Regression Tests

- `IntRuoyiBackend/yudao-server/src/test/java/cn/iocoder/yudao/server/DccOnlyOfficeLocalConfigTest.java`
- `IntRuoyiBackend/script/tests/test_dcc_onlyoffice_local_runtime_config.py`

## RED

- RED: `mvn -pl yudao-server -am "-Dtest=DccOnlyOfficeLocalConfigTest#localOnlyOfficePublicFileDefaultShouldBeReachableFromDockerDocumentServer" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，默认下载基址仍为 `127.0.0.1:${server.port}`。
- RED: `python -X utf8 -m pytest E:\IntRuoyi\IntRuoyiBackend\script\tests\test_dcc_onlyoffice_local_runtime_config.py -q` -> FAIL，静态契约未满足。

## GREEN

- GREEN: `mvn -pl yudao-server -am "-Dtest=DccOnlyOfficeLocalConfigTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，2 tests passed。
- GREEN: `python -X utf8 -m pytest E:\IntRuoyi\IntRuoyiBackend\script\tests\test_dcc_onlyoffice_local_runtime_config.py -q` -> PASS，3 passed。
- GREEN: `mvn -pl yudao-server -am -DskipTests package` -> PASS，Jar SHA256 `E82735F6FE9F7D9D0C4667777B71466D7CE3EFC2B7C28F92372C8F548EBEF32C`。

## Runtime Verification

- Old backend PID `23028` was confirmed as the local IntRuoyi backend on `48081` and stopped.
- Rebuilt backend was loaded through the project local restart script; final runtime PID `64760`; `http://127.0.0.1:48081/actuator/health` -> `{"status":"UP"}`.
- Transient non-listening local backend PID `60884` was stopped so only the final runtime owns `48081`.
- OnlyOffice container reaches backend via `host.docker.internal:48081` -> HTTP `200`; container-local `127.0.0.1:48081` remains unreachable -> `000`, matching the fixed URL requirement.
- Actuator env reports `yudao.dcc.preview.onlyoffice.public-file-base-url` source as `application-local.yaml` line 310.

## Risk And Regression Scope

Change is limited to local profile default config and static contract tests. It does not change remote/test-server release config, OnlyOffice browser base URL, token validation, callback URL, or download controller behavior.

## Blockers And Follow-Up

Full browser login verification was not automated because command logs must not record the provided password. Test server deployment was not performed; this local Docker networking fix only requires local backend restart to take effect.

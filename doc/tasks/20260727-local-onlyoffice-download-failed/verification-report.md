# Verification Report

## Summary

本地 OnlyOffice `-4 下载失败` 已定位为 Docker 容器访问 Windows Host 后端的地址配置问题，并通过本地配置、测试、打包和后端重启完成修复。

## Changed Behavior

- `application-local.yaml` local profile now defaults `yudao.dcc.preview.onlyoffice.public-file-base-url` to `http://host.docker.internal:${server.port}`.
- OnlyOffice browser base URL remains `http://127.0.0.1:8080` for local browser loading.
- No test-server or production config changed.

## Verification Evidence

- Java config test: `DccOnlyOfficeLocalConfigTest` -> PASS, 2 tests.
- Python local runtime config test: `test_dcc_onlyoffice_local_runtime_config.py` -> PASS, 3 tests.
- Backend package: `mvn -pl yudao-server -am -DskipTests package` -> PASS.
- Jar SHA256: `E82735F6FE9F7D9D0C4667777B71466D7CE3EFC2B7C28F92372C8F548EBEF32C`.
- Runtime: old PID `23028` stopped; final backend runtime PID `64760`; health `UP`; transient non-listening PID `60884` stopped.
- Container reachability: `host.docker.internal:48081` -> `200`; container-local `127.0.0.1:48081` -> `000`.
- Route check: unauthenticated preview metadata request returns business `401`, confirming route availability without logging credentials.

## Deployment Status

本地后端已重启并加载修复后的 Jar。测试服务器未发布；本地访问报错不需要发布测试服务器。若要让测试服务器上的 `preview-metadata` 行为变更生效，则需要单独按发布流程部署到测试服务器。

## Remaining Closeout

当前工作区仍有其他任务的未提交文件，且 `int_main` ahead origin；未执行本任务最终 push。

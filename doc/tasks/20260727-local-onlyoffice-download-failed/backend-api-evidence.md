# Backend API Evidence

## Scope

Local backend configuration for DCC controlled file OnlyOffice preview metadata and document download URL generation.

## API Contract

- Endpoint: `GET /admin-api/dcc/controlled-files/{id}/preview-metadata`
- Output field affected: `onlyofficeDocumentUrl`
- Local expected base for document download: `http://host.docker.internal:${server.port}` so the Docker OnlyOffice service can fetch the file from the Windows Host backend.

## Auth, Permissions, Validation, Error Behavior

Auth and permissions were not changed. Unauthenticated preview metadata requests still return business `401 账号未登录`. No fallback, mock success, or exception swallowing was introduced.

## Config And Services

- Config changed: `yudao.dcc.preview.onlyoffice.public-file-base-url` default in `application-local.yaml`.
- Browser-facing OnlyOffice base URL remains `http://127.0.0.1:8080`.
- Required local services verified: frontend `8081`, backend `48081`, Docker OnlyOffice `8080`.
- No database migration or schema change.

## BDD

BDD: 本地 OnlyOffice 受控预览可下载文档 -> Given 本地 `int_main` 前端和后端运行且用户打开受控浏览 xlsx 文件详情, When OnlyOffice 使用预览元数据中的 document URL 下载文件, Then 下载接口应返回有效文件内容而不是让 OnlyOffice 报 `-4 下载失败`。

## RED

- RED: `mvn -pl yudao-server -am "-Dtest=DccOnlyOfficeLocalConfigTest#localOnlyOfficePublicFileDefaultShouldBeReachableFromDockerDocumentServer" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，local 默认下载基址不可被 Docker OnlyOffice 访问。
- RED: `python -X utf8 -m pytest E:\IntRuoyi\IntRuoyiBackend\script\tests\test_dcc_onlyoffice_local_runtime_config.py -q` -> FAIL，同一配置契约失败。

## GREEN

- GREEN: `mvn -pl yudao-server -am "-Dtest=DccOnlyOfficeLocalConfigTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。
- GREEN: `python -X utf8 -m pytest E:\IntRuoyi\IntRuoyiBackend\script\tests\test_dcc_onlyoffice_local_runtime_config.py -q` -> PASS。

## Integration Verification

- Docker OnlyOffice -> `http://host.docker.internal:48081/actuator/health` returns HTTP `200`.
- Docker OnlyOffice -> `http://127.0.0.1:48081/actuator/health` returns `000`, proving the old URL cannot work from the container.
- Rebuilt local backend runtime PID `64760` is healthy on `48081`.

## Observability

Actuator env confirms the property source is `application-local.yaml` line 310 in the rebuilt Jar; values are masked by actuator sanitization.

## Blockers

No backend blocker remains for local runtime. Test server deployment was not in this local fix scope.

# Execution Log

BDD: DCC 受控预览必须显式配置 viewer token 密钥 -> Given 用户访问测试服受控文件详情页 `/dcc/controlled-file/detail/2054545668044048046?viewer=1&from=browser` / When 后端需要签发受控预览 viewer token / Then 后端必须读取显式配置的 `yudao.dcc.viewer-token.hmac-secret`，缺失时 fail-fast 并暴露配置缺失，不得使用默认密钥或前端绕过。

SETUP: 使用 `bug-regression-fix-loop` 缺陷回归流程。

SETUP: 上一个后端任务 `20260602-showroom-product-import-owner-company-124` 原状态为 `in_progress`；因用户中断并切换到当前紧急 DCC 访问缺陷，已记录为 blocked，未修改或暂存其代码改动。

RED: 用户截图 -> FAIL，受控文件详情页显示 `DCC viewer token config is missing: yudao.dcc.viewer-token.hmac-secret`。

DIAGNOSIS: `DccViewerTokenService.requireConfigured()` -> 发现当 `yudao.dcc.viewer-token.hmac-secret` 为空或 trim 后长度小于 32 时，后端抛出 `CONTROLLED_FILE_VIEWER_TOKEN_CONFIG_MISSING`，与截图一致。

DIAGNOSIS: `script/deploy/int-ruoyi-test/docker-compose.yml` -> 已包含 `--yudao.dcc.viewer-token.hmac-secret=${DCC_VIEWER_TOKEN_HMAC_SECRET}`。

DIAGNOSIS: `ssh root@172.30.30.58 'cd /opt/intruoyi/runtime && ... .env ...'` -> 当前测试服 `.env` 中 `DCC_VIEWER_TOKEN_HMAC_SECRET` 长度为 50，未打印密钥值。

DIAGNOSIS: `ssh root@172.30.30.58 "docker exec intruoyi-backend printenv ARGS ..."` -> 当前 backend 容器 `ARGS` 中 viewer token secret 长度为 50，未打印密钥值。

DIAGNOSIS: `ssh root@172.30.30.58 "docker exec intruoyi-backend ps -ef ..."` -> 当前 Java 进程参数中 viewer token secret 长度为 50，说明参数已进入 Java 进程。

DIAGNOSIS: `ssh root@172.30.30.58 'cd /opt/intruoyi/runtime && docker compose ps'` -> 当前 backend 镜像为 `intruoyi-backend:26-06-02_00-12-30`，容器运行约 39 分钟。

DIAGNOSIS: `ssh root@172.30.30.58 "docker logs intruoyi-backend --since 45m ..."` -> 近 45 分钟未检索到 `DCC viewer token config is missing`。

GREEN: `python -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -k "viewer_token or compose_uses_isolated_runtime_names_ports_and_dcc_config"` -> PASS，2 passed，确认发布脚本/compose 的 DCC viewer token 配置契约仍在。

GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260602-dcc-viewer-token-config-missing\bug-regression-evidence.md` -> PASS，Bug regression evidence is valid.

BLOCKED: `docs/login-access.md` 规定未经当前任务明确授权不得登录、联调、E2E、发布、重启或验证测试环境；因此不能继续用登录态复测目标页面，也不能重启或发布测试服。

BDD: DCC 详情链接必须指向真实受控文件 -> Given 用户访问 `/dcc/controlled-file/detail/2054545668044048046?viewer=1&from=browser` / When 前端调用 `GET /admin-api/dcc/controlled-files/2054545668044048046` / Then 如果该 ID 在当前租户数据中存在，应返回详情；如果不存在，应明确暴露数据不存在，不得把它伪装成下载失败或自动切换到其它文件。

RED: 用户浏览器控制台 -> FAIL，`getControlledFile()` 返回 `Controlled file does not exist`，调用栈位于 `workflow.ts:1189` 与详情页 `loadData()`。

DIAGNOSIS: `DccControlledFileController.getControlledFile()` -> 详情页接口为 `GET /admin-api/dcc/controlled-files/{id}`，需要 `dcc:controlled-file:query` 权限。

DIAGNOSIS: `DccControlledFileQueryServiceImpl.getControlledFile(userId, id)` -> `controlledFileMapper.selectById(id)` 返回 null 时抛 `CONTROLLED_FILE_NOT_EXISTS`；权限不通过时另抛 `CONTROLLED_FILE_ACCESS_DENIED`。因此当前错误表示目标 ID 查不到，而不是权限不足。

BLOCKED: 继续确认 `2054545668044048046` 是否存在、属于哪个租户、是否被删除或是否链接过期，需要访问测试服数据库/接口；`docs/login-access.md` 要求当前任务明确授权后才能继续测试服登录/联调/验证。

BDD: Viewer token 缺配置时允许使用固定密钥 -> Given 用户明确批准“密钥写死、暂时不需要那么严格” / When `DccViewerTokenService` 遇到空配置或长度不足 32 的 `hmacSecret` / Then 服务使用固定默认密钥继续签发和校验 viewer token，不再抛出 `CONTROLLED_FILE_VIEWER_TOKEN_CONFIG_MISSING`。

RED: `mvn -pl yudao-module-dcc -Dtest=DccViewerTokenServiceDefaultSecretTest test` -> FAIL，新单测 `issueAndVerify_useBuiltInSecretWhenConfigMissing` 断言未抛异常，但现状仍抛出 `DCC viewer token config is missing: yudao.dcc.viewer-token.hmac-secret`。

GREEN: 修改 `DccViewerTokenService`，新增固定默认密钥常量并在配置缺失或长度不足 32 时回退使用该值。

GREEN: `mvn -pl yudao-module-dcc "-Dtest=DccViewerTokenServiceDefaultSecretTest,DccControlledPreviewAccessServiceTest" test` -> PASS，3 tests passed；证明缺配置时可正常签发/校验 token，且既有 preview access token 流程未回归。

# 执行日志：修复测试服展厅产品导入 413

BDD: 测试服产品导入大 Excel 不应被前端 Nginx 413 拦截 -> Given 发布到测试服的前端 Nginx 代理 `/admin-api/` / When 用户上传约 174MB 的产品资料 Excel 到 `/showroom/product/import-excel` / Then 请求体上限必须至少达到后端 `300MB` 配置，请求应进入后端业务处理而不是由 Nginx 返回 413。

BDD: 上传大小配置必须显式可回归 -> Given 发布脚本从 `script/deploy/int-ruoyi-test/nginx.conf` 构建前端容器 / When 执行后端配置回归测试 / Then 测试必须校验 `client_max_body_size 300m;` 存在，防止后续发布配置丢失。

INFO: 已确认 `yudao-server/src/main/resources/application.yaml` 当前设置 `spring.servlet.multipart.max-file-size=256MB`、`max-request-size=300MB`，但 `script/deploy/int-ruoyi-test/nginx.conf` 未设置 `client_max_body_size`，Nginx 默认上限会在请求进入后端前返回 413。

RED: `mvn --% -pl yudao-server -Dtest=UploadMultipartLimitConfigTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL，新增 `frontendNginxShouldAllowConfiguredMultipartRequestSize` 断言失败：`frontend Nginx must set client_max_body_size 300m to match Spring multipart max-request-size`。

GREEN: `script/deploy/int-ruoyi-test/nginx.conf` -> PASS，已在 `server` 块显式加入 `client_max_body_size 300m;`，与后端 `spring.servlet.multipart.max-request-size=300MB` 对齐。

GREEN: `mvn --% -pl yudao-server -Dtest=UploadMultipartLimitConfigTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，3 tests，确认 Spring multipart 上限、Nginx 请求体上限和二进制响应编码保护均通过。

## Bug Regression Summary

Bug: 测试服务器后台展厅产品管理导入 Excel 时，浏览器请求 `POST /admin-api/showroom/product/import-excel` 返回 HTTP 413 `Request Entity Too Large`。

Expected: 当前产品资料 Excel 应在前端 Nginx 代理层允许进入后端，由后端按真实导入规则返回成功、跳过或业务失败明细，不应在进入后端前被 Nginx 默认请求体上限拦截。

Reproduction: 用户在测试服务器 `8081` 产品管理导入 Excel 时，浏览器控制台显示 `/admin-api/showroom/product/import-excel` 返回 413；本地代码检查确认 Spring multipart 已是 `256MB/300MB`，但发布用 `script/deploy/int-ruoyi-test/nginx.conf` 缺少 `client_max_body_size`。

Root Cause: 前端容器使用 Nginx 代理 `/admin-api/`，发布配置未显式设置 `client_max_body_size`，因此大 Excel 请求会在 Nginx 层按默认请求体上限失败，无法进入后端 multipart 配置。

Verification: 已新增并通过 `UploadMultipartLimitConfigTest.frontendNginxShouldAllowConfiguredMultipartRequestSize`，要求发布用前端 Nginx 配置存在 `client_max_body_size 300m;`；targeted Maven 测试通过。

Blockers: 缺少当前任务对测试服务器登录、发布、重启或验证的明确授权；本次未操作 `172.30.30.58`，测试服运行态需要后续授权后发布前端容器才会生效。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260601-test-server-showroom-product-import-413\execution-log.md` -> PASS。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260601-test-server-showroom-product-import-413 --mode preview` -> PASS，keep `task.md`、`execution-log.md`，delete `<none>`，blocked `<none>`。

GREEN: `docker run --rm intruoyi-frontend:26-06-01_21-17-12 cat /etc/nginx/conf.d/default.conf` -> PASS，本地发布包生成的前端镜像 Nginx 配置包含 `client_max_body_size 300m;`，且仍包含 `/admin-api/` 代理位置。

INFO: 用户提供的 build-release 日志显示 `Release package uploaded to NAS: Backup/ReleasePackage/26-06-01_21-17-12` 已输出；本次未额外登录测试服务器或部署该发布包。

GREEN: `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -k "disconnects_nas_mapping_idempotently or frontend_nginx_allows_large_showroom_product_import_requests" -q` -> PASS，2 passed，其中 `frontend_nginx_allows_large_showroom_product_import_requests` 覆盖发布用 Nginx 上传上限。

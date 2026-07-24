# 任务：修复测试服展厅产品导入 413

## 任务目标

修复测试服务器后台展厅产品管理导入 Excel 时 `POST /admin-api/showroom/product/import-excel` 返回 HTTP 413 `Request Entity Too Large` 的问题，使发布用前端 Nginx 请求体上限与后端 `spring.servlet.multipart.max-request-size=300MB` 保持一致。

## 前序任务检查

- 已确认上一相关任务 `doc/tasks/20260601-release-package-hardcoded-runtime-config/task.md` 状态为 completed。
- 已复核 `doc/tasks/20260601-showroom-product-import-timeout/task.md`：本地后端 multipart 上限与前端导入 timeout 已完成，当前测试服 413 更符合前端 Nginx 请求体上限缺失。

## BDD 场景

BDD: 测试服产品导入大 Excel 不应被前端 Nginx 413 拦截 -> Given 发布到测试服的前端 Nginx 代理 `/admin-api/` / When 用户上传约 174MB 的产品资料 Excel 到 `/showroom/product/import-excel` / Then 请求体上限必须至少达到后端 `300MB` 配置，请求应进入后端业务处理而不是由 Nginx 返回 413。

BDD: 上传大小配置必须显式可回归 -> Given 发布脚本从 `script/deploy/int-ruoyi-test/nginx.conf` 构建前端容器 / When 执行后端配置回归测试 / Then 测试必须校验 `client_max_body_size 300m;` 存在，防止后续发布配置丢失。

## 里程碑

- [x] M1：建立任务文档、确认旧任务状态与现有上传配置。
- [x] M2：新增 Nginx 请求体上限回归测试并记录 RED。
- [x] M3：在发布用前端 Nginx 配置中显式设置 `client_max_body_size 300m;`。
- [x] M4：运行 targeted Maven 回归验证。
- [x] M5：记录收尾证据，执行 task-closeout-cleanup 预览。

## 预期验证

- RED：`UploadMultipartLimitConfigTest` 新增断言在缺少 Nginx `client_max_body_size 300m;` 时失败。
- GREEN：同一测试通过，确认 Spring multipart 与前端 Nginx 发布配置同时满足产品资料 Excel 导入大小要求。
- REGRESSION：不改动业务导入解析逻辑、不新增 fallback、不吞掉真实导入错误。

## 当前状态

status: blocked_on_test_server_authorization

## 当前结果

本机发布配置修复与 targeted 回归验证已完成。测试服务器运行态尚未变更；按 `docs/login-access.md`，登录、联调、发布、重启或验证测试环境需要用户在当前任务中明确授权。

用户随后构建的本地发布包 `26-06-01_21-17-12` 已生成前端镜像；镜像内 `/etc/nginx/conf.d/default.conf` 已确认包含 `client_max_body_size 300m;`。该包是否部署到测试服仍取决于后续授权与发布动作。

## 验证记录

- RED：`UploadMultipartLimitConfigTest.frontendNginxShouldAllowConfiguredMultipartRequestSize` 在缺少 `client_max_body_size 300m;` 时失败。
- GREEN：`mvn --% -pl yudao-server -Dtest=UploadMultipartLimitConfigTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，3 tests。
- GREEN：bug-regression evidence validator -> PASS。
- GREEN：`task-closeout-cleanup --mode preview` -> PASS，delete `<none>`、blocked `<none>`。
- GREEN：`docker run --rm intruoyi-frontend:26-06-01_21-17-12 cat /etc/nginx/conf.d/default.conf` -> PASS，包含 `client_max_body_size 300m;`。
- GREEN：`python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -k "frontend_nginx_allows_large_showroom_product_import_requests" -q` -> PASS。

## 阻塞

- 缺少当前任务对测试服务器发布/验证的明确授权；影响是 `172.30.30.58:8081` 上当前运行容器仍可能继续返回 413，直到按发布流程重建并启动前端 Nginx 容器。

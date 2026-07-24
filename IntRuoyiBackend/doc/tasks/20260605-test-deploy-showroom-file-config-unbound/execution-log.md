# 执行日志：测试服部署失败于展厅文件配置绑定校验

BDD: 测试服部署必须阻断未绑定文件配置 -> Given 发布包部署到测试服 / When `infra_file_config.id=28` 未绑定到 `http://host.docker.internal:9000` 与 `http://172.30.30.58:9000/yudao` / Then `post-import.sql` 必须 fail fast 并输出 `SHOWROOM_FILE_CONFIG_UNBOUND`，不得继续启动或假装成功。

BDD: 受保护文件配置不得被发布脚本静默覆盖 -> Given `infra_file_config.id=28` 是受保护配置 / When 执行 code-only `deploy-release` / Then 发布脚本只能校验目标绑定，不得自动 UPDATE、INSERT 或改写 `showroom/%` 文件 URL。

BDD: 测试服修复必须有明确授权 -> Given 需要修复测试服 `infra_file_config.id=28` / When 未获得当前任务授权 / Then 只能报告缺失授权和影响，不得通过 SQL/API/脚本修改测试服配置。

VERIFY: 运行控制台日志 -> FAIL，`releaseTag=26-06-05 09:49:55`，失败阶段为 `Applying target-bound post-import SQL`，错误为 `SHOWROOM_FILE_CONFIG_UNBOUND: infra_file_config.id=28 is not bound to target MinIO endpoint/domain`。

VERIFY: 本地发布包 `E:\Int\CacheData\IntRuoyi\publish-int-ruoyi\26-06-05_09-49-55\post-import.sql` -> 只包含 `intruoyi_assert_showroom_file_storage_target` 校验，没有 `UPDATE infra_file_config` 或 `UPDATE infra_file`。

VERIFY: 当前脚本契约 -> `script/tests/test_publish_int_ruoyi_to_test_tooling.py::test_publish_script_does_not_mutate_fixed_showroom_file_config_28` 要求发布脚本不得在 `post-import.sql` 中 UPDATE/INSERT 受保护 `infra_file_config.id=28`。

ROOT_CAUSE: 测试服当前 `infra_file_config.id=28` 没有满足目标绑定前置条件；当前发布脚本按保护契约只校验、不自动修复，因此 fail fast 是预期阻断。

AUTH: 2026-06-05 用户回复“授权”，允许本任务受控修复测试服 `infra_file_config.id=28` 文件配置并重新部署发布包到测试服。

RED: 测试服只读 SQL 查询 -> FAIL，`infra_file_config.id=28` 当前 `endpoint=http://host.docker.internal:9000`、`domain=http://172.30.30.57:9000/yudao`、`bucket=yudao`，不含 `127.0.0.1:9000`，但未绑定到当前测试服 `172.30.30.58`；1434 条 `config_id=28 AND path LIKE 'showroom/%'` 的 URL 指向 `172.30.30.57`。

CHANGE: 按授权执行测试服受控修复：仅把 `infra_file_config.id=28` 的 domain 绑定为 `http://172.30.30.58:9000/yudao`，保持 endpoint `http://host.docker.internal:9000`、bucket `yudao`；同时将 1434 条明确指向旧测试服 `172.30.30.57` 的 `showroom/%` URL 改为 `172.30.30.58`。

GREEN: 测试服修复后 SQL 复核 -> PASS，`infra_file_config.id=28` 为 `endpoint=http://host.docker.internal:9000`、`domain=http://172.30.30.58:9000/yudao`、`bucket=yudao`，无 `127.0.0.1:9000` 和旧 `172.30.30.57`；1434 条 `showroom/%` URL 全部指向 `http://172.30.30.58:9000/yudao/`。

GREEN: 运行控制台真实 UI 提交 -> PASS，`operationId=56dc85ba-e37c-4154-9ffc-9c93c8ab47f3`，`releaseTag=26-06-05 09:49:55`。

RED: 部署重试 `56dc85ba-e37c-4154-9ffc-9c93c8ab47f3` -> FAIL，已通过 `Showroom file storage config 28 is protected and target-bound`，但展厅图片 smoke URL 返回 `HTTP 200 application/json`；后端响应体显示 `Unknown column 'product_name' in 'field list'`。

ROOT_CAUSE: 当前发布包和测试服运行库缺少正式迁移 `sql/mysql/20260604_dcc_controlled_file_product_name.sql`；当前代码的 `DccControlledFileMapper` 查询已包含 `product_name` 字段。

GREEN: 测试服应用正式迁移 `sql/mysql/20260604_dcc_controlled_file_product_name.sql` -> PASS，`dcc_controlled_file.product_name varchar(255) NULL` 存在。

GREEN: 迁移后展厅图片代理只读复核 -> PASS，`http://172.30.30.58:8081/admin-api/infra/file/28/get/showroom/product/cover/20260602/product-product_164-imported-cover-01183e2264aa155c.png` 返回 `HTTP 200 image/png`。

GREEN: 运行控制台真实 UI 再次提交 -> PASS，`operationId=c3d258f7-1233-4a56-8cf9-0deaba31427e`，`releaseTag=26-06-05 09:49:55`。

GREEN: 部署重试 `c3d258f7-1233-4a56-8cf9-0deaba31427e` -> PASS，operation 状态 `succeeded`，日志显示 `Publish completed for test`、`Showroom smoke image is readable through frontend proxy`、`Website entry verified`、`Website scoped current release verified`。

GREEN: 测试服最终 HTTP 复核 -> PASS，`http://172.30.30.58:48081/actuator/health`、`http://172.30.30.58:8081/`、`http://172.30.30.58:8083/`、`http://172.30.30.58:8083/showroom` 与展厅图片 smoke URL 均返回 200，图片 `Content-Type=image/png`。

RED: `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py::test_publish_script_packages_and_applies_required_dcc_sql_for_all_deploys -q` -> FAIL，发布脚本 required-sql 清单缺少 `20260604_dcc_controlled_file_product_name.sql`。

CHANGE: `script/deploy/publish-int-ruoyi.ps1` 的 `$requiredDatabaseSqlScripts` 已加入 `sql/mysql/20260604_dcc_controlled_file_product_name.sql`，避免后续新发布包漏带当前代码必需的 DCC schema 迁移。

GREEN: `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py::test_publish_script_packages_and_applies_required_dcc_sql_for_all_deploys -q` -> PASS。

GREEN: `python -X utf8 -m pytest script/tests/test_dcc_sql_scripts.py -q` -> PASS，8 passed。

GREEN: `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py script/tests/test_runtime_control_ops_scripts.py script/tests/test_restart_ruoyi_script.py -q` -> PASS，73 passed。

GREEN: `git diff --check -- script/deploy/publish-int-ruoyi.ps1 script/tests/test_publish_int_ruoyi_to_test_tooling.py doc/tasks/20260605-test-deploy-showroom-file-config-unbound/task.md doc/tasks/20260605-test-deploy-showroom-file-config-unbound/execution-log.md` -> PASS，仅 Git CRLF normalization warnings。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260605-test-deploy-showroom-file-config-unbound --mode preview` -> PASS，delete none，blocked none。

RED: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260605-test-deploy-showroom-file-config-unbound --mode apply` -> FAIL，cleanup 脚本未识别中文 `当前状态 completed`，返回 `current status: unknown`。

GREEN: 任务文档补充英文 `Current Status: completed`，用于 cleanup 脚本识别。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260605-test-deploy-showroom-file-config-unbound --mode apply` -> PASS，delete none，blocked none。

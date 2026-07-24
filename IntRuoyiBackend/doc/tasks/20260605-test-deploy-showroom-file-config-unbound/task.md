# 任务：测试服部署失败于展厅文件配置绑定校验

## 任务目标

分析运行控制台“部署发布包到测试服”失败原因。当前日志显示发布包 `26-06-05 09:49:55` 已从 NAS 下载、镜像已加载、MySQL/Redis 已启动，但在执行 `post-import.sql` 时失败：`SHOWROOM_FILE_CONFIG_UNBOUND: infra_file_config.id=28 is not bound to target MinIO endpoint/domain`。

本任务先做本地日志、发布包产物和脚本契约诊断；未经用户明确授权，不修改测试服数据库、MinIO、受保护 `infra_file_config.id=28` 或服务器运行状态。2026-06-05 用户已回复“授权”，允许本任务受控修复测试服 `infra_file_config.id=28` 文件配置并重新部署发布包到测试服。

## Previous Task Check

- 上一个同服务仓库任务：`doc/tasks/20260605-backend-runtime-base-local-config/task.md`
- 状态：`completed`
- 处理：上一任务已经完成内部后端基础镜像本机配置；本任务处理后续测试服部署失败。

## BDD 场景

- BDD: 测试服部署必须阻断未绑定文件配置 -> Given 发布包部署到测试服 / When `infra_file_config.id=28` 未绑定到 `http://host.docker.internal:9000` 与 `http://172.30.30.58:9000/yudao` / Then `post-import.sql` 必须 fail fast 并输出 `SHOWROOM_FILE_CONFIG_UNBOUND`，不得继续启动或假装成功。
- BDD: 受保护文件配置不得被发布脚本静默覆盖 -> Given `infra_file_config.id=28` 是受保护配置 / When 执行 code-only `deploy-release` / Then 发布脚本只能校验目标绑定，不得自动 UPDATE、INSERT 或改写 `showroom/%` 文件 URL。
- BDD: 测试服修复必须有明确授权 -> Given 需要修复测试服 `infra_file_config.id=28` / When 未获得当前任务授权 / Then 只能报告缺失授权和影响，不得通过 SQL/API/脚本修改测试服配置。

## Milestones

- [x] M1：读取用户提供的运行控制台日志并定位失败阶段。
- [x] M2：读取本地发布包 `post-import.sql`，确认 SQL 只校验、不重绑定。
- [x] M3：核对发布脚本与契约测试，确认当前正式策略是保护 28 号配置并 fail fast。
- [x] M4：执行测试服受控只读确认与最小修复。
- [x] M5：重跑“部署发布包到测试服”并记录验证结果。

## Expected Verification

- 本地读取 `E:\Int\CacheData\IntRuoyi\publish-int-ruoyi\26-06-05_09-49-55\post-import.sql`，确认失败消息与目标绑定校验。
- 本地核对 `script/deploy/publish-int-ruoyi.ps1` 与 `script/tests/test_publish_int_ruoyi_to_test_tooling.py`，确认无发布时自动 UPDATE。
- 获得授权后，只读查询测试服当前 `infra_file_config.id=28`，再通过受控方式恢复为目标绑定。
- 修复后重新部署同一或新发布包，验证部署成功并通过前端/后端/展厅 smoke。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。当前错误必须阻断发布，不允许绕过校验继续部署。
- `是否从根因和长期维护角度解决`：是。根因是测试服目标绑定前置条件不满足，不能通过关闭校验掩盖。
- `是否存在临时补丁或绕过`：否。若后续修复测试服配置，必须记录用户授权、实际配置值和验证证据。

## 当前状态

completed

## Current Status

completed

## 验证结果

- VERIFY：用户提供的运行控制台日志显示 `deploy-release` 已完成 NAS 下载、Docker 镜像加载、MySQL/Redis 启动，失败在 `Applying target-bound post-import SQL`。
- VERIFY：本地发布包 `post-import.sql` 只包含 `intruoyi_assert_showroom_file_storage_target` 校验，没有 `UPDATE infra_file_config` 或 `UPDATE infra_file`。
- VERIFY：当前契约测试 `test_publish_script_does_not_mutate_fixed_showroom_file_config_28` 明确要求发布脚本不得修改受保护 `infra_file_config.id=28`。
- AUTH：2026-06-05 用户回复“授权”，允许受控修复测试服 `infra_file_config.id=28` 并重新部署发布包到测试服。
- RED：测试服只读查询 -> `infra_file_config.id=28` 为 `endpoint=http://host.docker.internal:9000`、`domain=http://172.30.30.57:9000/yudao`、`bucket=yudao`；1434 条 `showroom/%` URL 指向 `172.30.30.57`。
- GREEN：测试服受控修复 -> `infra_file_config.id=28` 已更新为 `endpoint=http://host.docker.internal:9000`、`domain=http://172.30.30.58:9000/yudao`、`bucket=yudao`；1434 条 `showroom/%` URL 已从 `172.30.30.57` 更新到 `172.30.30.58`，无 `127.0.0.1:9000` 或旧 `172.30.30.57` 残留。
- RED：运行控制台重试部署 `56dc85ba-e37c-4154-9ffc-9c93c8ab47f3` -> FAIL，已通过文件配置绑定校验，但展厅图片 smoke 返回 `application/json`；响应体显示 `Unknown column 'product_name' in 'field list'`，测试服 `dcc_controlled_file` 缺少当前代码必需字段。
- GREEN：应用正式迁移 `sql/mysql/20260604_dcc_controlled_file_product_name.sql` 到测试服 -> PASS，`dcc_controlled_file.product_name varchar(255) NULL` 存在。
- GREEN：运行控制台重试部署 `c3d258f7-1233-4a56-8cf9-0deaba31427e` -> PASS，`releaseTag=26-06-05 09:49:55`，日志显示 `Publish completed for test`。
- GREEN：测试服只读 HTTP 复核 -> PASS，`/actuator/health`、前端 `8081`、Website `8083`、`/showroom` 与展厅图片 smoke URL 均返回 200，图片 `Content-Type=image/png`。
- RED：`python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py::test_publish_script_packages_and_applies_required_dcc_sql_for_all_deploys -q` -> FAIL，发布脚本 required-sql 清单缺少 `20260604_dcc_controlled_file_product_name.sql`。
- GREEN：发布脚本 required-sql 清单已补齐 `20260604_dcc_controlled_file_product_name.sql`。
- GREEN：`python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py::test_publish_script_packages_and_applies_required_dcc_sql_for_all_deploys -q` -> PASS。
- GREEN：`python -X utf8 -m pytest script/tests/test_dcc_sql_scripts.py -q` -> PASS，8 passed。
- GREEN：`python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py script/tests/test_runtime_control_ops_scripts.py script/tests/test_restart_ruoyi_script.py -q` -> PASS，73 passed。
- GREEN：`git diff --check -- <本任务相关文件>` -> PASS，仅 Git CRLF normalization warnings。
- GREEN：`python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260605-test-deploy-showroom-file-config-unbound --mode preview` -> PASS，delete none，blocked none。
- RED：`python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260605-test-deploy-showroom-file-config-unbound --mode apply` -> FAIL，cleanup 脚本未识别中文 `当前状态 completed`，返回 `current status: unknown`。
- GREEN：任务文档补充英文 `Current Status: completed`，用于 cleanup 脚本识别。
- GREEN：`python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260605-test-deploy-showroom-file-config-unbound --mode apply` -> PASS，delete none，blocked none。

## Blockers

- 当前无阻塞。测试服部署已成功，发布脚本 required-sql 清单已补齐产品名称字段迁移。

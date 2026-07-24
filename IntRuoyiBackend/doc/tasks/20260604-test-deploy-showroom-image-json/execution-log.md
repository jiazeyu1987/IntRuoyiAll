# 执行日志：分析部署发布包到测试服后展厅图片 smoke 失败

BDD: 测试服部署后展厅 smoke 图片应可读 -> Given 发布脚本已完成测试服服务启动与基础健康检查 / When 发布脚本通过前端代理访问 `config_id=28` 的展厅图片 / Then 响应必须为 `image/*`，否则应 fail fast 并暴露具体图片路径。

BDD: 受保护展厅文件配置只读诊断 -> Given 默认展厅文件配置 `infra_file_config.id=28` 与 `showroom/%` 媒体 URL 受保护 / When 分析 smoke 图片失败 / Then 只能读取日志、manifest、配置与对象存在性，不得自动切换 bucket/domain/endpoint 或回填对象。

VERIFY: 上一个同服务仓库任务 `doc/tasks/20260603-restore-release-code-before-dockerhub-preflight/task.md` 当前状态为 `completed`。

VERIFY: 最新部署操作 `51bd4b72-a2fc-4486-949a-7db64f018eb4` -> FAIL，`releaseTag=26-06-04 00:14:46`，日志 `E:\Int\CacheData\IntRuoyi\runtime-control\logs\51bd4b72-a2fc-4486-949a-7db64f018eb4.log` 在最终展厅图片 smoke 失败。

VERIFY: 最新构建操作 `67121ffb-1e6a-4817-9e08-9429e1fac21e` -> PASS，`publishScope=code-only`、`includeOnlyOffice=false`，发布包 manifest 确认不包含数据库 dump 或 MinIO 快照。

VERIFY: 失败 URL `http://172.30.30.58:8081/admin-api/infra/file/28/get/showroom/product/cover/20260530/product-product_164-imported-cover.png` -> FAIL，HTTP 200，`Content-Type=application/json`，body 为 S3 `Access Key Id you provided does not exist`。

ROOT_CAUSE: `deploy-release` 会重生成 target-bound `post-import.sql` 并重绑定 `infra_file_config.id=28` 到测试服 MinIO endpoint/domain/bucket；但 code-only 发布包会设置 `SkipMinioSync=true`，旧脚本因此不读取远端 MinIO 凭据，`post-import.sql` 又在凭据为空时保留了前面 eDHR S3 写入的 accessKey/accessSecret，导致展厅文件配置用 eDHR 凭据访问测试服 MinIO。

RED: `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -k "deploy_release_reads_remote_minio_credentials_for_showroom_file_rebind or publish_script_auto_rebinds_and_verifies_showroom_file_storage_after_restore" -q` -> FAIL，预期原因：当前脚本仍把远端 MinIO 凭据读取绑定到 `-not $SkipMinioSync`，且 SQL 仍可能沿用旧配置凭据。

CHANGE: `script/deploy/publish-int-ruoyi.ps1` 已将 `$requiresRemoteMinioCredentials` 调整为 `$publishBackend -and $Mode -ne 'build-release'`，确保 code-only `deploy-release` 也读取目标 MinIO 凭据用于展厅文件配置重绑定。

CHANGE: `New-ShowroomFileStoragePostImportSql` 生成的 SQL 已改为显式写入目标 MinIO `$accessKey` / `$accessSecret`，不再在参数为空时沿用旧 `infra_file_config.id=28` 里的 accessKey/accessSecret。

CHANGE: `script/tests/test_publish_int_ruoyi_to_test_tooling.py` 已更新契约测试，覆盖 code-only deploy-release 的远端 MinIO 凭据读取与 SQL 不沿用旧凭据。

GREEN: `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -k "deploy_release_reads_remote_minio_credentials_for_showroom_file_rebind or publish_script_auto_rebinds_and_verifies_showroom_file_storage_after_restore" -q` -> PASS，`2 passed, 51 deselected`。

GREEN: `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS，`53 passed`。

VERIFY: `git diff --check -- script/deploy/publish-int-ruoyi.ps1 script/tests/test_publish_int_ruoyi_to_test_tooling.py doc/tasks/20260604-test-deploy-showroom-image-json` -> PASS，仅提示 Git 将在下次触碰时把 LF 替换为 CRLF，无 whitespace error。

NOTE: 未重跑真实“部署发布包到测试服”，因为该操作会写入受保护的 `infra_file_config.id=28`；当前修复已通过本地契约测试验证。

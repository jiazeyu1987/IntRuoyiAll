# 执行日志：发布包 NAS 自动恢复流水线

BDD: NAS 发布包恢复后自动重绑定文件存储 -> Given 发布包从本机导出数据库和 MinIO 对象 / When 从 NAS 部署到测试服或正式服 / Then 脚本自动把 `infra_file_config.id=28` 的 endpoint/domain 绑定到目标环境可访问地址，且数据库中不得残留会让后端容器访问自身的 `127.0.0.1:9000`。

BDD: 自动恢复必须验证真实文件内容 -> Given 目标环境已经导入 MySQL 并同步 MinIO / When 后端、前端和 Website 启动完成 / Then 脚本必须抽取真实 `infra_file` 图片记录，通过 `/admin-api/infra/file/{configId}/get/{path}` 校验响应是 `image/*`，否则恢复失败。

BDD: 自动恢复支持测试服和正式服但不静默降级 -> Given 用户指定 `-Environment test` 或 `-Environment prod -ConfirmText PROD` / When 运行发布包恢复 / Then 脚本按对应服务器、端口、域名执行同一组门禁；缺少 NAS 包、MinIO 快照、数据库、凭据、健康检查或文件校验时直接失败。

SETUP: 使用 `ci-cd-environment-delivery` 与 `backup-disaster-recovery-readiness` 技能，目标是发布/恢复流水线自动化与恢复后验证。

SETUP: 未授权实际执行正式服发布或恢复；本任务只修改和验证本地脚本/静态契约，不访问正式服务器。

RED: `python -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -k auto_rebinds -q` -> FAIL, 当前 `publish-int-ruoyi.ps1` 缺少 `New-ShowroomFileStoragePostImportSql`、恢复后文件配置重绑定断言、后端容器 MinIO 可达断言和真实图片内容类型校验。

RED: `python -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -k auto_rebinds -q` -> FAIL, `deploy-release` 从 NAS 取包后还缺少按当前目标服务器重新生成 `post-import.sql` 的门禁，可能复用构建时目标地址。

GREEN: `python -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -k auto_rebinds -q` -> PASS, 发布脚本包含目标环境专属 `post-import.sql`、恢复后文件配置重绑定断言、后端容器 MinIO 可达断言和真实图片内容类型校验。

GREEN: `python -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py script/tests/test_edhr_protected_storage_publish_tooling.py -q` -> PASS, 42 个发布脚本与存储发布门禁测试通过。

GREEN: `PSParser Tokenize publish-int-ruoyi.ps1` -> PASS, PowerShell 脚本无解析错误。

SUMMARY: 发布包 `build-release` 后上传 NAS；`deploy-release` 从 NAS 取包后会按当前 `ServerHost` 重新生成 `post-import.sql`，自动修正 `infra_file_config.id=28` endpoint/domain，导入数据库后阻断 `127.0.0.1:9000` 残留，启动后从后端容器校验 MinIO 可达，并通过前端代理抽查真实展厅图片 `image/*`。

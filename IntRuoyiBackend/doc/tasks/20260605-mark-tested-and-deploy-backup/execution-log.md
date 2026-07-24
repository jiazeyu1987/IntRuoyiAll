# 执行日志：20260605-mark-tested-and-deploy-backup

BDD: 标记测试通过 -> Given 发布包 `26-06-05_15-28-sql-idempotent-release` 已成功部署并验证测试服 / When 在运行控制台点击标记测试通过 / Then NAS 发布包测试状态被标记为通过。

BDD: 备份服只接受测试通过包 -> Given 发布包已标记测试通过 / When 发布到 `backup` 环境 / Then 发布命令必须使用备份服 `172.30.30.59`、`/mnt/intruoyi-data` 路径、`intruoyi-minio` 容器和显式生产级确认。

BDD: 备份服发布后可访问 -> Given 备份服部署完成 / When 访问后端健康检查、前端、Website 根页和 `/showroom` / Then 均返回 HTTP 200，远端容器运行对应发布镜像。

RED: mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest#runtimeControlPropertiesShouldKeepBackupRuntimePathsAfterHostOnlyOverride test -> FAIL, backup 只配置 host 时使用了通用默认路径 `/var/lib/docker/intruoyi-releases`，会覆盖备份服应使用的 `/mnt/intruoyi-data/intruoyi-releases`。

GREEN: mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest#runtimeControlPropertiesShouldKeepBackupRuntimePathsAfterHostOnlyOverride test -> PASS，Runtime Control backup host-only 配置保留备份服专用路径、磁盘设备和 MinIO 容器名。

RED: 运行控制台操作 `4e039940-38dc-413a-913b-215113d848f7` -> FAIL，发布命令仍传入 `/var/lib/docker/...` 和 `ragflow_compose-minio-1`，备份服目标配置不正确。

GREEN: 运行控制台操作 `792a7a66-b324-4022-b1bd-108e0802f048` 命令参数 -> PASS，发布命令已传入 `-RemoteReleaseRoot /mnt/intruoyi-data/intruoyi-releases`、`-RemoteDataRoot /mnt/intruoyi-data/runtime-data`、`-RemoteDataDiskMount /mnt/intruoyi-data`、`-RemoteDataDiskDevice /dev/mapper/cl-home`、`-RemoteMinioContainer intruoyi-minio`。

RED: 运行控制台操作 `792a7a66-b324-4022-b1bd-108e0802f048` -> FAIL，备份服 `infra_file_config.id=28` 未绑定目标 MinIO endpoint/domain，脚本按受保护配置规则阻塞。

GREEN: 备份服只读/修复后校验 -> PASS，`infra_file_config.id=28` 绑定 `http://172.30.30.59:9000/yudao`，`showroom/%` 媒体 URL 剩余生产域计数为 0，备份域计数为 1434。

RED: 运行控制台操作 `cafc185a-7dc7-491a-b9dc-f96b02106552` -> FAIL，必须 SQL 文件存在性检查使用无输出 SSH 命令，Windows OpenSSH/PowerShell 采集输出时卡住。

GREEN: python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -q -> PASS，63 passed；必须 SQL 检查改为 `test -f '$remoteSqlPath' && echo REQUIRED_SQL_EXISTS`，避免无输出命令卡住。

GREEN: 运行控制台操作 `25152258-3453-485a-bb5d-4ab1ee9a5ae5` -> PASS，`标记测试通过`，`status=succeeded`，发布包 `26-06-05_15-28-sql-idempotent-release`，恢复集 `20260604-182827`。

GREEN: 运行控制台操作 `65bed745-c520-485b-958e-f1cfe0aeabd9` -> PASS，`上线备份服务器`，`environment=backup`，`status=succeeded`，日志记录 `Publish completed for backup.`。

GREEN: 备份服 URL 验证 -> PASS，`http://172.30.30.59:48081/actuator/health`、`http://172.30.30.59:8081/`、`http://172.30.30.59:8083/`、`http://172.30.30.59:8083/showroom`、展厅图片代理 URL 均返回 HTTP 200。

GREEN: 备份服容器验证 -> PASS，`intruoyi-backend` 和 `intruoyi-frontend` 均运行发布包镜像 `26-06-05_15-28-sql-idempotent-release`，`intruoyi-website` 正常运行。

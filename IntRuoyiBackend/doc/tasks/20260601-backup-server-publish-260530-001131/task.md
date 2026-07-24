# 任务：发布 26-05-30 00:11:31 到备用服务器

## 任务目标

- 使用已验证发布包 `26-05-30 00:11:31` 发布备用服务器 `172.30.30.59`。
- 发布完成后确认 `http://172.30.30.59:8081/index` 可以访问。
- 仅操作备用服务器，不操作正式服务器或测试服务器。

## 前序任务检查

- 已确认上一后端任务 `doc/tasks/20260601-showroom-product-import-timeout/task.md` 状态为 `completed`。
- 当前仓库存在无关未跟踪 `runtime/`，本任务不触碰、不提交。

## BDD 场景

- BDD: 备用服务器只部署指定发布包 -> Given NAS 存在发布包 `26-05-30 00:11:31` 且已测试通过 / When 执行备用服务器发布 / Then 远端 `IMAGE_TAG` 对应 `26-05-30_00-11-31`。
- BDD: 备用服务器发布遵守正式级门禁 -> Given 目标环境为 `backup` / When 执行发布 / Then 必须带 `PROD` 确认和已测试通过发布包，不得绕过数据盘或责任人门禁。
- BDD: 备用服务器前端可访问 -> Given 发布脚本完成 / When 访问 `http://172.30.30.59:8081/index` / Then 返回 HTTP 成功响应且页面不是错误页。
- BDD: 发布脚本尊重发布包 compose 服务清单 -> Given 历史发布包 compose 未声明 `onlyoffice` / When 执行 `deploy-release` / Then 脚本不得硬编码启动或等待不存在的 `onlyoffice`，必须按 compose 服务清单启动必需服务并 fail-fast 检查必需服务缺失。

## 里程碑

- [x] M1：建立任务文档、BDD 场景与预期验证。
- [x] M2：只读确认发布包、备用机 SSH、数据盘与运行目录前置条件。
- [x] M3：准备备用机专用发布数据盘并执行备用服务器发布。
- [x] M4：验证后端健康、前端 `/index`、运行控制台状态和远端 `IMAGE_TAG`。
- [x] M5：记录最终证据，执行 task-closeout-cleanup 预览，并按需提交本任务文档。
- [x] M6：修复发布脚本对历史发布包 `onlyoffice` 服务的硬编码，重新执行发布脚本并取得绿色发布状态。

## 预期验证

- 发布包检查：`26-05-30 00:11:31` 对应目录 `26-05-30_00-11-31` 存在，且 `tested.json` 存在。
- 发布命令：通过运行控制台或 `publish-int-ruoyi.ps1 -Mode deploy-release -Environment backup -ReleaseTag "26-05-30 00:11:31" -RequireTested -ConfirmText PROD` 执行。
- 访问验证：`Invoke-WebRequest -UseBasicParsing -Uri http://172.30.30.59:8081/index -TimeoutSec 30` 返回成功。

## 当前状态

status: completed

## 当前发现

- 发布包内容需补齐：运行控制台既有操作 `21645756-1ff1-41f7-96c2-57ad967cc36a` 已成功从 NAS 下载 `Backup/ReleasePackage/26-05-30_00-11-31` 并通过 `-RequireTested`，但发布包 `required-sql` 目录缺少当前发布脚本要求的 `20260528_dcc_controlled_file_protection.sql` 与 `20260529_dcc_audit_menu_permission.sql`。
- 备用机数据盘需准备：`172.30.30.59` 当前 `/var/lib/docker` 位于 `/dev/mapper/centos-root`，不是项目门禁要求的 `/dev/vdb`；同时 `/opt/intruoyi/runtime` 不存在。
- 发布包内容已补齐：已将上述两个 SQL 从本地 `sql/mysql/` 同步到 NAS 指定发布包与本地下载副本，并更新 `release-manifest.json` artifact 哈希清单；未修改镜像 tar、网站静态包或 `tested.json`。
- 备用机磁盘复核：`/dev/mapper/cl-home` 为 `941G` XFS，当前空目录，可作为专用发布数据盘挂载到 `/mnt/intruoyi-data`；为避免影响备份机已有 Docker 容器，本任务不迁移 `/var/lib/docker`。
- 备用机发布数据盘已准备：`/dev/mapper/cl-home` 已格式化为 ext4 并挂载到 `/mnt/intruoyi-data`，`/opt/intruoyi/runtime/data` 已 bind mount 到 `/mnt/intruoyi-data/runtime-data`。
- 当前发布阻塞：发布包 compose 使用 `mysql:8.0.39`，备用机 CPU 不支持该镜像所需的 `x86-64-v2` 指令集，`intruoyi-mysql` 启动即 `Fatal glibc error: CPU does not support x86-64-v2` 并以 `127` 退出，导致发布脚本无法等待 MySQL 健康通过。
- 备用机 CPU 证据：`lscpu` 显示 KVM 虚拟机暴露为 `Intel(R) Core(TM)2 Duo CPU T7700 @ 2.40GHz`；`/proc/cpuinfo` 缺少 `x86-64-v2` 所需的 `sse4_1` 与 `popcnt`，该前置条件无法在客户机内通过发布脚本修复。
- 已验证可选修复路径：`mysql:8.0.28` 在备份机上可执行 `mysqld --version`。但将运行时 MySQL 镜像从 `mysql:8.0.39` 改为 `mysql:8.0.28` 属于兼容替换，按无 fallback 策略需用户明确批准后才能继续。
- 同版本替代检查：`container-registry.oracle.com/mysql/community-server:8.0.39` 在备份机上同样报 `Fatal glibc error: CPU does not support x86-64-v2`；Docker Hub `mysql/mysql-server:8.0.39` 与 `percona:8.0.39` 本次拉取超时，未形成可用替代证据。
- 备用机 CPU 前置条件已修复：`lscpu` 显示当前暴露为 `Intel(R) Xeon(R) Gold 5218R CPU @ 2.10GHz`，`x86-64-v2` 必需 flags 无缺失；`docker run --rm --entrypoint mysqld mysql:8.0.39 --version` 已在备份机通过。
- 指定发布包为 `publishScope=code-only`，不包含 `ruoyi-vue-pro-current.sql`。备用机初始空库下直接发布会在 `20260526_dcc_other_template_category.sql` 处失败，因为表 `dcc_file_category` 不存在。
- 已按真实 NAS 备份点恢复基准数据：使用 `Backup/BackupPackage/20260530-233026/mysql/ruoyi-vue-pro.sql.gz`，传输字节数 `3429280196`，`gzip -t` 通过，导入后 `table_count=447`、`dcc_file_category_count=6378`。
- CPU 修复和基准库恢复后，发布脚本已完成发布包下载、eDHR Object Lock 门禁、镜像加载、MySQL/Redis 启动、四个必需 SQL 执行和 website 目录切换；随后在 `docker compose up -d onlyoffice backend frontend` 失败，原因是目标发布包 compose 只声明 `mysql/redis/backend/frontend/website`，没有 `onlyoffice` 服务。
- 测试服发布能成功的直接差异：测试服当前 `/opt/intruoyi/runtime/docker-compose.yml` 包含 `onlyoffice` 服务，`intruoyi-onlyoffice` 运行健康；备份服本次目标包 compose 不包含该服务。测试服仅做只读对比，未改动。
- 备份服已按目标发布包实际声明的服务集启动 `backend/frontend/website`，未伪造发布脚本状态。远端运行镜像为 `intruoyi-backend:26-05-30_00-11-31`、`intruoyi-frontend:26-05-30_00-11-31`，`http://172.30.30.59:48081/actuator/health` 返回 `{"status":"UP"}`，`http://172.30.30.59:8081/index` 返回 HTTP 200。
- 真实浏览器验证通过：Playwright Chromium 打开 `http://172.30.30.59:8081/index` 返回 HTTP 200，并按前端路由跳转到 `http://172.30.30.59:8081/login?redirect=/index`，页面标题为 `瑛泰管理系统 - 登录`。
- 遗留缺陷：当前发布脚本 `script/deploy/publish-int-ruoyi.ps1` 仍硬编码启动和等待 `onlyoffice`，与 `26-05-30_00-11-31` 目标发布包 compose 不一致；本任务不将该脚本失败状态改写为成功。
- 收尾清理预览通过：`task-closeout-cleanup` 仅保留 `task.md` 与 `execution-log.md`，无删除项、无阻塞、无警告。

## 最终验证结果

- 备用服务器目标服务运行：`mysql`、`redis`、`backend`、`frontend`、`website` 均已运行，MySQL 与 Redis healthy。
- 后端健康：`http://172.30.30.59:48081/actuator/health` 返回 `{"status":"UP"}`。
- 前端入口：`http://172.30.30.59:8081/index` 返回 HTTP 200。
- 真实浏览器：Playwright Chromium 访问 `/index` 成功并跳转登录页，标题为 `瑛泰管理系统 - 登录`。
- 说明：发布脚本状态未伪造为成功；遗留的 OnlyOffice compose 不一致问题已记录为后续缺陷。

## 续作目标

- 当前线程目标要求“发布备份服务器成功”，因此仅有手动恢复和页面可访问还不够；必须让 `publish-int-ruoyi.ps1 -Mode deploy-release -Environment backup -ReleaseTag "26-05-30 00:11:31" -RequireTested -ConfirmText PROD` 按发布包真实 compose 完成。
- 修复范围限定为发布脚本服务编排逻辑：必需服务 `mysql/redis/backend/frontend/website` 缺失时 fail-fast；`onlyoffice` 仅在发布包 compose 声明该服务时启动和等待健康。

## 续作完成证据

- 发布脚本已修复：`deploy-release` 现在读取远端 `docker compose config --services`，必需服务缺失时 fail-fast；`onlyoffice` 仅在 compose 声明时启动和等待健康。
- NAS 配置读取已修复：`NasShare` 默认不再用脚本内中文字面量覆盖 UTF-8 JSON 配置，避免 PowerShell 5.1 读取 UTF-8 无 BOM 脚本时产生乱码共享名。
- 备份发布历史写入已修复：`deploy` 历史不再要求 `TestConclusion`，该字段只保留给 `mark-tested`。
- 回归测试：`python -m unittest script.tests.test_publish_int_ruoyi_deploy_services` 通过，6 个用例 OK；PowerShell 语法解析通过。
- 收尾清理预览通过：`task-closeout-cleanup` 仅保留 `task.md` 与 `execution-log.md`，无删除项、无阻塞、无警告。
- 绿色发布命令已完成：`publish-int-ruoyi.ps1 -Mode deploy-release -Environment backup -ReleaseTag "26-05-30 00:11:31" -RequireTested -ConfirmText PROD -RemoteReleaseRoot /mnt/intruoyi-data/intruoyi-releases -RemoteDataRoot /mnt/intruoyi-data/runtime-data -RemoteDataDiskMount /mnt/intruoyi-data -RemoteDataDiskDevice /dev/mapper/cl-home -SkipDatabaseSync -SkipMinioSync` 返回 `Publish completed for backup.`，并写入 `backup-latest.json`。
- 最终运行态：`docker compose config --services` 返回 `mysql/redis/backend/frontend/website`；`intruoyi-backend:26-05-30_00-11-31`、`intruoyi-frontend:26-05-30_00-11-31` 与 `intruoyi-website` 均 running，MySQL 和 Redis healthy。
- 最终访问验证：`http://172.30.30.59:48081/actuator/health` 返回 HTTP 200，`http://172.30.30.59:8081/index` 返回 HTTP 200 且为 HTML。
- 真实浏览器验证：Playwright Chromium 打开 `http://172.30.30.59:8081/index` 返回 HTTP 200，并跳转到 `http://172.30.30.59:8081/login?redirect=/index`，标题为 `瑛泰管理系统 - 登录`。

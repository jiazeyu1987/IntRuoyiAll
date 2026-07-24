# 任务：标记测试通过并发布备份服务器

## 任务目标

将已在测试服验证成功的发布包 `26-06-05_15-28-sql-idempotent-release` 通过运行控制台标记为测试通过，然后使用同一发布包发布到备份服务器 `172.30.30.59` 并完成外部验证。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；标记测试通过、备份服务器部署、健康检查任一失败均停止。
- `是否从根因和长期维护角度解决`：是；使用运行控制台/发布脚本既有门禁和备份服专用路径，不手工绕过发布流程。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- BDD: 标记测试通过 -> Given 发布包 `26-06-05_15-28-sql-idempotent-release` 已成功部署并验证测试服 / When 在运行控制台点击标记测试通过 / Then NAS 发布包测试状态被标记为通过。
- BDD: 备份服只接受测试通过包 -> Given 发布包已标记测试通过 / When 发布到 `backup` 环境 / Then 发布命令必须使用备份服 `172.30.30.59`、`/mnt/intruoyi-data` 路径、`intruoyi-minio` 容器和显式生产级确认。
- BDD: 备份服发布后可访问 -> Given 备份服部署完成 / When 访问后端健康检查、前端、Website 根页和 `/showroom` / Then 均返回 HTTP 200，远端容器运行对应发布镜像。

## 里程碑

- [x] M1：确认本地运行控制台和发布包状态。
- [x] M2：通过运行控制台标记测试通过并记录证据。
- [x] M3：发布到备份服务器并记录部署日志。
- [x] M4：验证备份服务器 HTTP 与容器状态。

## 预期验证

- 运行控制台标记测试通过操作成功。
- `deploy-release -Environment backup -RequireTested -ConfirmText PROD` 成功。
- `http://172.30.30.59:48081/actuator/health`、`http://172.30.30.59:8081/`、`http://172.30.30.59:8083/`、`http://172.30.30.59:8083/showroom` 返回 HTTP 200。

## 完成证据

- 运行控制台操作 `25152258-3453-485a-bb5d-4ab1ee9a5ae5`：`标记测试通过`，`status=succeeded`，发布包 `26-06-05_15-28-sql-idempotent-release`。
- 运行控制台操作 `65bed745-c520-485b-958e-f1cfe0aeabd9`：`上线备份服务器`，`environment=backup`，`status=succeeded`，发布包 `26-06-05_15-28-sql-idempotent-release`。
- 备份服容器运行对应发布镜像：`intruoyi-backend:26-06-05_15-28-sql-idempotent-release`、`intruoyi-frontend:26-06-05_15-28-sql-idempotent-release`。
- 对外 URL 验证：后端健康、管理前端、Website 根页、Website `/showroom`、展厅图片代理均返回 HTTP 200。
- 回归测试：`python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -q` 通过；`mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest#runtimeControlPropertiesShouldKeepBackupRuntimePathsAfterHostOnlyOverride test` 通过。

## 当前状态

completed

## Current Status

completed

# 任务：发布包 NAS 自动恢复流水线

## 任务目标

让本机发布包上传到 NAS 后，可以由同一个脚本从 NAS 自动恢复到测试服务器或正式服务器，并在目标环境自动完成环境重绑定、MinIO 同步、数据库导入后配置修正、服务启动、健康检查和真实文件下载校验。流程中不得依赖人工登录后台改配置；任一关键条件失败必须 fail fast。

## 前序任务检查

- 最近后端任务 `doc/tasks/20260601-test-server-nas-snapshot-not-ready-deploy/task.md` 已标记 `blocked`，不混入本任务。
- 当前仓库存在未跟踪 `runtime/` 与前序任务目录产物；本任务不删除、不提交这些无关产物。

## BDD 场景

BDD: NAS 发布包恢复后自动重绑定文件存储 -> Given 发布包从本机导出数据库和 MinIO 对象 / When 从 NAS 部署到测试服或正式服 / Then 脚本自动把 `infra_file_config.id=28` 的 endpoint/domain 绑定到目标环境可访问地址，且数据库中不得残留会让后端容器访问自身的 `127.0.0.1:9000`。

BDD: 自动恢复必须验证真实文件内容 -> Given 目标环境已经导入 MySQL 并同步 MinIO / When 后端、前端和 Website 启动完成 / Then 脚本必须抽取真实 `infra_file` 图片记录，通过 `/admin-api/infra/file/{configId}/get/{path}` 校验响应是 `image/*`，否则恢复失败。

BDD: 自动恢复支持测试服和正式服但不静默降级 -> Given 用户指定 `-Environment test` 或 `-Environment prod -ConfirmText PROD` / When 运行发布包恢复 / Then 脚本按对应服务器、端口、域名执行同一组门禁；缺少 NAS 包、MinIO 快照、数据库、凭据、健康检查或文件校验时直接失败。

## 里程碑

- [x] M1：建立任务文档与 BDD 场景。
- [x] M2：补充流水线静态回归测试并记录 RED。
- [x] M3：实现目标环境文件存储重绑定与真实文件校验门禁。
- [x] M4：运行目标测试验证 GREEN。
- [x] M5：记录证据、执行收尾清理并提交本任务改动。

## 预期验证

- RED：`python -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -k auto_rebinds -q` 先失败，证明当前脚本缺少发布后真实文件内容校验；补充 `deploy-release` 目标重写要求后再次失败，证明 NAS 包恢复缺少目标环境重新绑定。
- GREEN：目标测试通过，证明脚本包含自动重绑定、禁止残留 `127.0.0.1:9000`、后端容器内 MinIO 可达校验和 `/admin-api/infra/file/{configId}/get/{path}` 图片内容校验。
- REGRESSION：`python -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py script/tests/test_edhr_protected_storage_publish_tooling.py -q` 通过；PowerShell `PSParser` 解析通过。

## 当前状态

status: completed

## Current Status

completed

## 阻塞

None.

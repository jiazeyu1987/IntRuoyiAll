# 任务：正式服与备份服发布链路一致化

## 任务目标

系统检查并修复运行控制台、发布脚本与环境配置链路，确保“上线备份服务器”与“上线已验证发布包”使用同一套发布语义：正式服发布包部署会执行的发布阶段、门禁、数据同步、文件存储重绑定、健康检查与发布历史记录，备份服也应执行同等逻辑，仅允许目标环境事实不同，例如服务器地址、数据盘挂载、发布历史命名空间和目标 MinIO 凭据来源。

## 上一任务检查

- 上一个后端任务 `20260603-restore-data-guide-alignment` 已标记 `blocked`，阻塞原因是用户切换任务；本任务不接管或回滚该任务的运行控制台恢复数据改动。
- 当前后端仓库存在既有未提交改动，本任务只追加与发布链路一致化直接相关的改动，不回滚无关文件。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。缺少目标环境 MinIO 凭据、发布包数据、磁盘挂载、健康检查或必需服务时仍 fail fast。
- `是否从根因和长期维护角度解决`：是。目标是把目标环境画像补齐，使正式服与备份服共用同一发布管线，而不是补一个一次性容器名参数。
- `是否存在临时补丁或绕过`：否。不通过创建 RagFlow 容器、跳过 MinIO 同步、跳过数据库同步或伪造发布成功来绕过问题。

## BDD 场景

BDD: 备份服发布复用正式服发布管线 -> Given 运维人员选择已测试通过的 NAS 发布包 / When 分别执行 `promote-prod` 与 `promote-backup` / Then 两者均调用 `publish-int-ruoyi.ps1 -Mode deploy-release`，均要求 `PROD` 确认与 tested gate，差异仅来自环境画像。

BDD: 备份服带数据发布不得依赖正式服历史 MinIO 容器名 -> Given 备份服环境不存在 `ragflow_compose-minio-1` / When 执行带数据 `deploy-release -Environment backup` / Then 脚本使用备份服环境声明的 MinIO 凭据来源并 fail fast，不读取未声明的 RagFlow 容器。

BDD: 正式服与备份服发布包恢复门禁一致 -> Given 发布包包含 MySQL dump 与 MinIO `yudao` 快照 / When 部署到 prod 或 backup / Then 两个环境都执行数据库导入、MinIO 同步、`infra_file_config.id=28` 目标重绑定、后端容器 MinIO 可达校验、前端/后端/Website 健康检查和对应环境发布历史记录。

BDD: 只发代码语义不被备份服特例破坏 -> Given 发布包或操作明确为 code-only / When 部署到 prod 或 backup / Then 两个环境都不恢复 MySQL 或 MinIO，但仍执行应用镜像加载、运行环境写入、服务启动与健康检查，且不会为跳过数据同步而读取目标 MinIO 凭据。

## 里程碑

- [x] M1：建立任务文档与监督式任务包，明确验收口径。
- [x] M2：复盘运行控制台、发布脚本、环境配置和测试覆盖，定位正式/备份差异面。
- [x] M3：先补充 RED 测试，覆盖备份服环境 MinIO 配置与 prod/backup 发布参数一致性。
- [x] M4：实现环境画像补齐与发布脚本一致化修复。
- [ ] M5：运行目标回归、证据验证与必要链路检查。当前本地回归与备份服 MinIO 前置条件通过；真实备份发布最终在展厅图片 smoke 阶段失败，等待 release 包/数据基线处置。
- [ ] M6：收尾清理预览，提交本任务直接改动。当前不得提交为完成态，因为端到端备份发布未通过。

## 预期验证

- `mvn -pl yudao-module-infra "-Dtest=RuntimeControlServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -k "backup or remote_minio or deploy_release" -q`
- `python -X utf8 -m pytest script/tests/test_runtime_control_scripts.py script/tests/test_runtime_control_ops_scripts.py -q`
- `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260603-backup-prod-publish-parity\bug-regression-evidence.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\ci-cd-environment-delivery\scripts\validate_cicd_environment.py --evidence docs\environments\20260603-backup-prod-publish-parity-ci-cd-evidence.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\backup-disaster-recovery-readiness\scripts\validate_backup_disaster_recovery.py --evidence docs\recovery\20260603-backup-prod-publish-parity.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260603-backup-prod-publish-parity --mode preview`

## 当前状态

blocked_invalid_release_package_data_baseline

## 已完成工作

- 运行控制台环境画像新增远端 MinIO 凭据来源字段；prod 默认传入既有 MinIO 源，backup 不再继承 prod/RagFlow 默认值，只有显式配置后才传入 `-RemoteMinioContainer`。
- 发布脚本移除全局 `ragflow_compose-minio-1` 默认值，改为目标环境画像填充。
- `deploy-release` 读取 `release-manifest.json` 的 `publishScope`；with-data 包缺 MySQL dump 或 MinIO `yudao` 快照时 fail fast；code-only 包设置跳过数据库与 MinIO 同步。
- 远端 MinIO 凭据读取只在需要 MinIO 同步时执行，code-only / `-SkipMinioSync` 不再触发远端 `docker inspect`。
- 备份服 `172.30.30.59` 已在用户授权后补齐真实 MinIO：容器名 `intruoyi-minio`、主机 `9000` 可达、后端容器可达 `host.docker.internal:9000`、`yudao` bucket 存在。
- 远端重启脚本已移除人工入口中的 RagFlow 隐式默认；backend/full 重启若缺少 `-RemoteMinioContainer` 会 fail fast。
- 真实备份发布已重跑同一已测 code-only 包 `26-06-02 20:13:57`，通过 NAS 下载、eDHR Object Lock、远端磁盘、镜像加载、SQL、服务启动、后端/前端/OnlyOffice/Website 健康检查与后端 MinIO 可达检查，最终在展厅图片 smoke 失败。

## 当前阻塞

- 最新正式服发布历史指向 `20260603_website_assets_cache_immutable`，且 NAS 上该包有 `prod-latest.json` 和 `tested.json`；但它的 `release-manifest.json` 标记 `publishScope=with-data`，实际缺少 `ruoyi-vue-pro-current.sql` 与 `minio/yudao`。修复后的发布脚本会按 fail-fast 合约拒绝该包，不能用它验证备份服同包发布。
- 同一已测 code-only 包 `26-06-02 20:13:57` 不包含数据库或 MinIO 快照；备份服新建 MinIO 没有历史对象基线，发布后 `http://172.30.30.59:8081/admin-api/infra/file/28/get/showroom/product/cover/20260527/product-product_001-cover.png` 返回 HTTP 200 但 Content-Type 为 `application/json`，未通过图片 smoke。
- 需要用户明确批准后选择正式处置路径：修正 NAS 上已部署但元数据错误的 release manifest 为 code-only、构建并测试新的完整 with-data 发布包，或提供已测试且同时包含 MySQL dump 与 MinIO 快照的正式 release 包。未批准前不得篡改发布审计记录、不得跳过 smoke、不得无 tested gate 部署旧 with-data 包。

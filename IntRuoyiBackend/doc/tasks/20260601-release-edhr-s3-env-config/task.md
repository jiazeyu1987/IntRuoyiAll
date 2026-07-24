# 任务：补齐正式发布 eDHR S3 Object Lock 配置

## 任务目标

- 为 `publish-int-ruoyi.ps1 -Mode build-release` 补齐必需的 `EDHR_S3_*` 发布环境配置。
- 只使用已有可信配置来源或真实远端运行配置，不编造、不 mock、不绕过 eDHR Object Lock 门禁。
- 不在任务文档、聊天输出或日志中记录 S3 access key、secret key 等敏感值。

## 前序任务检查

- 上一任务 `doc/tasks/20260601-pdf-worker-mime-runtime/task.md` 当前为 `blocked_on_remote_authorization`，阻塞点为正式/备份环境需要重新发布或重建前端容器。
- 本任务属于该阻塞的发布配置准备工作；只处理本机发布进程所需配置，不直接绕过发布门禁。

## BDD 场景

- BDD: 正式发布缺少 eDHR S3 配置时必须失败 -> Given 发布脚本进入 `build-release` / When `EDHR_S3_ENDPOINT` 等必需变量缺失 / Then 发布必须 fail-fast，并指出缺少的变量。
- BDD: 正式发布 eDHR Object Lock 目标必须真实可验证 -> Given 已补齐 `EDHR_S3_*` / When 发布脚本运行 verifier / Then verifier 必须真实访问 S3、校验 versioning、Object Lock、retention 与 legal hold，返回 PASS 后才允许继续发布。

## 里程碑

- [x] M1：建立任务文档、BDD 场景与预期验证。
- [x] M2：盘点本机与既有发布配置来源，确认可用字段。
- [x] M3：补齐当前发布进程所需环境变量或明确阻塞项。
- [x] M4：运行 eDHR verifier 或发布脚本前置检查，记录不含密钥的验证证据。
- [x] M5：收尾清理预览并按验证结果决定是否提交非敏感改动。

## 预期验证

- `python -X utf8 tool\edhr-storage-retention-verifier\verify.py` 在配置齐全时返回 `status=PASS`。
- 若配置缺失，必须记录缺失变量并停止，不使用默认值、mock 值或临时绕过。

## 当前状态

status: completed

## 进展记录

- NAS 配置：已从本机既有 NAS 配置复制到本次运行控制台指定路径；任务文档不记录 NAS 密码。
- eDHR S3 配置：本机环境与仓库配置未发现可直接用于正式发布的真实 `EDHR_S3_*` 值。
- 正式 MinIO：已创建并配置专用 bucket `edhr-protected-storage-20260601`，versioning enabled，默认 Object Lock retention 为 `COMPLIANCE` / `7DAYS`。
- 本机 Windows 用户环境：已写入 8 个 `EDHR_S3_*` 变量；任务文档不记录 access key 或 secret key。
- 本机运行控制后端：已重启，后端 `48081` 与前端 `8081` 健康检查通过。
- 遗留说明：一次失败尝试在正式 MinIO 创建了未使用的空 bucket `edhr-protected-storage`；未把它配置给发布脚本，也未擅自删除正式存储。
- 收尾清理预览：`task_closeout.py --task-id 20260601-release-edhr-s3-env-config --mode preview` 返回 ready，无待删除项、无阻塞、无警告。

## 最终验证

- `python -X utf8 tool\edhr-storage-retention-verifier\verify.py` -> PASS。
- 本机后端 `http://localhost:48081/actuator/health` -> HTTP 200。
- 本机前端 `http://localhost:8081/` -> HTTP 200。

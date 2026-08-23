# IntRuoyi Release Backup Restore Rules

## 触发场景

- 构建发布、测试服发布、正式服发布、备用服发布、备份、恢复、回滚或发布排障前，必须先读取本文件。
- 远端服务器操作还必须读取 `docs/server-access.md`。
- worktree 发布隔离还必须读取 `docs/worktree-restrictions.md`。

## 发布授权

- 默认不得操作测试服务器、正式服务器、备用服务器或共享存储。
- 用户明确授权后，必须记录目标环境、目标主机、发布范围、releaseTag、回滚或恢复路径。
- 正式服和备用服按生产等级处理，必须显式确认生产操作。

## 发布链路

- 完整发布只认单一 releaseTag 的闭环，不得拼接多轮 releaseTag 结果。
- 测试服发布成功范围必须明确，不得因未执行正式发布链路而误判测试服失败。
- 发布成功必须核对操作结果、manifest、远端环境变量、实际镜像 tag、后端 health、前端 HTTP 200。

## 备份与恢复

- 涉及备份、恢复或回滚时，必须记录数据范围、存储位置、保留策略和验证方式。
- 缺少备份目标、恢复脚本、数据盘、MinIO 容器或数据库连接证据时必须 fail fast。
- 不得删除、清空、重挂载或改写共享存储，除非用户明确授权且有回滚说明。

### 本机数据迁移包恢复门禁

- Trigger: 用户要求把当前电脑的本机 IntRuoyi 数据打包给另一台电脑、保持两台开发电脑数据一致、或生成给 Codex 使用的恢复 README。
- Preflight check: 先区分代码与运行数据，至少盘点 MySQL、MinIO/上传附件对象、Redis、SQLite/本地外部数据文件和前后端本地配置；导出包不得写入数据库密码、MinIO 密钥、Redis 密码或其它运行密钥。恢复说明必须要求目标电脑先备份再覆盖，并使用目标电脑自己的容器环境变量或本地配置读取凭据。
- Blocker: 本机 MySQL/MinIO/Redis 容器缺失、目标数据范围不清、磁盘空间不足、哈希校验不一致、MinIO `/data` 无法完整归档、目标电脑未确认可覆盖本地数据，或只能通过切换空库/远端库/随机端口来让页面有数据时必须停止。
- Verification: 记录迁移包目录、MySQL dump 字节数和数据库名、MinIO 归档字节数与 `gzip -t` 结果、Redis RDB 文件头、SHA-256 清单、README UTF-8 可读性，以及目标恢复后的数据库关键行数和 MinIO bucket 存在性检查。
- Forbidden action: 禁止只导 Git 或 MySQL 就宣称环境一致；禁止把 Redis 缓存当成业务主数据；禁止把源机密码写进迁移包；禁止恢复前不备份目标数据；禁止清空其它 Docker volume、改目标端口、切换数据源、使用 mock/空数据冒充恢复成功。
- Evidence: `doc/tasks/data-sync-package-20260818/verification-report.md`。

### 工艺路线删除恢复完整性门禁

- Trigger: 恢复被删除的 MES 工艺路线、路线工序、路线产品、流程边、开始/结束边或布局，尤其父路线仍有 ACTIVE 发布快照但正常页面不可见。
- Preflight check: 先冻结 `tenant_id + route_id` 精确范围，区分逻辑删除与物理删除；核对路线身份、原状态、ACTIVE 版本唯一性、不可变快照哈希、节点/边/边界/坐标数量、原 routeProcessId 映射、工序/工作站主数据、逐工序正式批记录报表以及可解析的正式产品。写入前必须精确备份所有相关父子表并计算校验值，恢复和回滚脚本都要用原始身份、行数、快照哈希和任务标记 fail fast。
- Blocker: ACTIVE 快照缺失或无效、快照与原工序 ID/工序/工作站不一致、正式批记录报表或产品主数据缺失、目标物理图表已有无法归属的数据、同范围存在活动写事务、备份或精确回滚路径缺失时必须停止。不得只恢复父路线后留下空流程图，也不得用 `formBindings`、无效 itemId、默认路线或重建新 routeProcessId 补齐。
- Verification: 提交后同时核对正常业务口径唯一可见、原状态保持、ACTIVE 快照哈希未变化、全部工序可从开始边到达结束边、流程边/边界/布局数量、逐工序正式批记录表单、正式产品和异常活动产品为 0；最后用 Playwright 从真实路线列表打开详情，确认页面节点、连线和产品行可见且无 MES 写请求，再复跑数据库终检。
- Forbidden action: 禁止把路线删除理解成单一删除标签；禁止从草稿、旧版本、其它路线或前端缓存拼接恢复；禁止恢复无法解析到正式主数据的历史孤儿绑定；禁止顺便启用、发布、改 QA/工单/排产/活跃订单或其它下游状态。
- Evidence: `doc/tasks/20260811-restore-pressure-pump-process-route/verification-report.md`。

## NAS 发布包批量删除门禁

- Trigger: 删除、清空、批量移除 NAS `Backup/ReleasePackage` 发布包目录，或释放 NAS 发布包占用空间。
- Preflight check: 必须先记录用户明确删除确认、目标 UNC、顶层发布包目录数量、递归文件数、总字节数、排序后目录名 SHA-256；删除脚本必须验证目标路径精确等于确认路径，且每个删除目标都是发布根目录的直接子目录并非重解析点。
- Blocker: 顶层目录数量或目录名 SHA-256 与确认快照不一致、发布根目录有散落文件、目标路径不是确认 UNC、存在重解析点、凭据缺失、或无法证明删除目标是直接子目录时必须停止。
- Verification: 删除后独立重新挂载或重新连接 NAS，只读确认发布根目录仍存在，顶层目录数、递归文件数和剩余字节数均为 0。
- Forbidden action: 禁止删除 NAS 共享根、`Backup/BackupPackage`、发布根目录本身或其它共享目录；禁止在 SMB 返回 `目录不是空的` 后静默继续，必须记录失败、重新统计剩余目录数量和目录名 SHA-256，再按用户已确认的同一删除范围继续。
- Evidence: `doc/tasks/20260802-delete-all-nas-release-packages/execution-log.md`，删除 210 个发布包时首轮在 94 个后遇到 SMB `目录不是空的`，重新锁定剩余 116 个目录哈希后继续，最终复核剩余目录、文件和字节数均为 0。

## 正式服备份计划任务状态门禁

- Trigger: 查询、启用、禁用、重注册或发布验证 `IntRuoyi Backup Scheduled` 等正式服备份计划任务。
- Preflight check: 先确认唯一实现、测试和打包源为 `E:\IntRuoyi\IntRuoyiBackend\script\backup-ops`；不得用维护仓 `ops/backup-ops` 历史副本的测试结果证明实际后端已修复。随后同时核对计划任务查询退出码、`Enabled/Status`、`NextRunTime`、`LastTaskResult`、`Task To Run`、脚本/config/secrets 路径、受保护生产确认、`taskPrincipal.principalId`、S4U/Limited、batch-logon、ACL identity、`backup.repositoryEnvironment`、最新成功点 `completedAt` 和已批准的 `backup.maxFreshnessHours`。
- Blocker: 运行/打包/测试源不一致，查询命令非 0，`NextRunTime=N/A`，任务禁用，脚本或配置路径漂移，受保护输入、principal 或 ACL 无效，仓库环境缺失/非法，`LastTaskResult` 非 0，成功备份点或 `completedAt` 缺失/不可解析，或 `now - completedAt` 超过新鲜度阈值时，不能宣布定时备份正常。
- Verification: 记录后端 source-of-truth 合同测试、计划任务名称、principal/logon type、ACL 验证身份、启用状态、下次运行时间、上次运行时间、上次结果、脚本/config/secrets 路径、仓库环境、最新成功点 `completedAt`、新鲜度阈值及实际年龄；秘密字段只记录脱敏证明，不记录明文。
- Implementation gate: 注册脚本合同转绿前，必须同时验证 `backup-ops.ps1` 主入口接受并校验同一个 `RepositoryEnvironment`，非交互正式备份只从 secrets 的 `auth.productionBackupConfirmText` 取得受保护确认；不得只让注册命令字符串或示例 JSON 变绿。
- Forbidden action: 禁止把维护仓副本、`schtasks` 错误/空输出、旧路径、禁用任务、默认仓库、当前用户/SYSTEM/最高权限、命令行明文凭据、仅有历史备份文件或缺少恢复/新鲜度证据包装成“定时备份正常”。
- Evidence: `D:\ProjectPackage\Int\IntRuoyiMaintance\doc\tasks\20260813-production-operations-hardening-plan\` 规划包及其独立复审报告。

## 禁止做法

- 禁止直接在主工作区构建发布包，除非任务明确证明无需发布隔离且用户授权。
- 禁止未授权操作远端服务器。
- 禁止发布失败后静默切换环境、镜像、端口、数据源或脚本。
- 禁止把缺少任一关键验证项的发布判定为完成。

## 验证方式

- 记录 releaseTag、构建命令、发布命令、目标主机和验证命令。
- 后端验证至少包含 health 状态。
- 前端验证至少包含 HTTP 200 或真实页面打开证据。
- 记录备份/恢复产物、manifest 和必要校验结果。

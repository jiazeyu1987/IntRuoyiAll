# IntRuoyi Release Backup Restore Rules

## 触发场景

- 构建发布、测试服发布、正式服发布、审查服发布、备份、恢复、回滚或发布排障前，必须先读取本文件。
- 远端服务器操作还必须读取 `docs/server-access.md`。
- worktree 发布隔离还必须读取 `docs/worktree-restrictions.md`。

## 发布授权

- 默认不得操作测试服务器、正式服务器、审查服务器或共享存储。
- 用户明确授权后，必须记录目标环境、目标主机、发布范围、releaseTag、回滚或恢复路径。
- 正式服和审查服按生产等级处理，必须显式确认生产操作。

## 发布链路

- 完整发布只认单一 releaseTag 的闭环，不得拼接多轮 releaseTag 结果。
- 测试服发布成功范围必须明确，不得因未执行正式发布链路而误判测试服失败。
- 发布成功必须核对操作结果、manifest、远端环境变量、实际镜像 tag、后端 health、前端 HTTP 200。

## 备份与恢复

- 涉及备份、恢复或回滚时，必须记录数据范围、存储位置、保留策略和验证方式。
- 缺少备份目标、恢复脚本、数据盘、MinIO 容器或数据库连接证据时必须 fail fast。
- 不得删除、清空、重挂载或改写共享存储，除非用户明确授权且有回滚说明。

### 灾备备份保留与法规记录归档分离门禁

- Trigger: 调整 `keepDaysRemote`、全量/增量备份保留、长期记录保存、Object Lock/Retention/legal hold 或到期销毁。
- Preflight check: 必须先区分灾备备份与法规长期归档。灾备备份按完整恢复链和 RTO/RPO 滚动保留；法规归档按记录类别、策略版本、起算事件和 `retainUntil` 保留完整业务记录、附件、审计追踪和电子签名证据。MinIO/S3 支持 Object Lock 不等于目标 bucket 已启用，必须逐 bucket、逐对象版本真实读取 versioning、Object Lock、retention 和 legal hold 证据。
- Blocker: 记录保存期限矩阵、质量/法规负责人、起算事件、目标 bucket 证据、对象 versionId、retain-until、legal hold 状态或完整归档包任一缺失时，不得宣称长期留存就绪；灾备链清理范围可能触及法规归档 bucket 或未到期记录时必须停止。
- Verification: 灾备侧验证全量基线与增量段按链保留、恢复演练和过期清理不破坏可恢复链；归档侧验证业务记录、附件、审计、签名、hash、策略版本和保存截止时间闭合，未到期或 hold 中对象无法删除。到期记录先生成 hash 固定的待处置清单，经质量电子签名和职责分离审批后按精确对象版本销毁，并永久保留处置审计。
- Forbidden action: 禁止仅把全部数据库备份延长到最长记录保存期来替代记录归档；禁止让 30 天等灾备清理策略删除仍在法规保存期的记录；禁止到期自动静默删除；禁止用配置声明、测试 bucket 或历史报告冒充正式目标 bucket 的 WORM 证据。
- Evidence: `doc/tasks/20260907-backup-full-incremental-recovery-plan/development-plan.md` 与 `test-plan.md`。

### 恢复一致性、秘密恢复与归档可用性门禁

- Trigger: 设计或实施跨 MySQL/对象存储备份、增量链、受保护归档副本、secret/key 恢复、归档故障切换或长期格式迁移。
- Preflight check: 建立覆盖 HTTP、Quartz/TenantJob、MQ/worker、外部集成、直接数据库账号和对象写凭据的 writer registry；备份 epoch 只能在全部 writer drained/fenced、数据库活动事务为 0 且数据库/对象测试写均被拒绝后建立。秘密不进入备份包，但 SSH、MySQL、MinIO、归档、签名、TLS、调度和通知 secret 必须有独立故障域 escrow、owner、历史 key 生命周期和隔离恢复证据。WORM 主存储还必须有独立故障域副本、源/副本 versionId 映射、内容 hash、retention/hold 等价和复制 RPO。
- Blocker: writer 清单或 storage fence 不完整、历史 signing key 不可恢复、归档副本缺失或保护语义弱化、failover 未经 preview/approval、旧归档缺独立 reader/格式验证时，必须停止备份封存、恢复、归档放行或处置。
- Verification: 故障注入证明遗漏 writer 时屏障不能 ACTIVE，隔离 secret recovery 能完成当前连接和历史验签，主/副本 hash 相同且目标 retainUntil 不早于源、legal hold 不弱化，显式 failover/failback 后记录/附件/审计/签名可读；年度抽检覆盖旧 schema、呈现件、原始文件和受控格式迁移，迁移不得覆盖原对象。
- Forbidden action: 禁止只拦 HTTP 就宣称跨存储一致，禁止把 secret store 与生产/NAS 放在同一故障域，禁止把 Object Lock 当成备份，禁止静默切换归档读取源，禁止因 reader 下线而把 hash 正确等同于长期可读，禁止格式迁移覆盖原始归档。
- Evidence: `doc/tasks/20260907-backup-full-incremental-recovery-plan/docs/system/` 与 `docs/security/security-privacy-compliance-review.md`。

### 紧急交付下的最小备份审查闭环

- Trigger: 交付窗口不足，需要先完成老师或审查方当前明确要求的全量、增量、恢复证明和证据导出。
- Preflight check: 最小闭环仍必须包含真实 FULL、真实 MySQL binlog/对象 INCREMENTAL、全载荷 SHA-256、连续恢复链、测试演练槽位真实恢复和后端生成的审查证据 ZIP。若当前项目所有 HTTP、Quartz/TenantJob 和应用内 consumer 都在同一后端进程，且能证明不存在外部直接 DB/MinIO writer，可用“停止 frontend/backend + 活动事务为 0”替代首版通用 writer registry；不能证明唯一 writer 时必须阻断。
- Blocker: binlog 不可用、对象删除 tombstone 不完整、停服窗口不可接受、外部 writer 未盘点、演练槽位缺失或老师不接受文本/JSON 证据包时，最小闭环不能实施，必须回到对应完整设计。
- Verification: 至少恢复 `FULL`、`FULL+I1`、`FULL+I1+I2`，验证数据库关键行数、对象新增/修改/删除、真实登录和 DCC 文件 hash；审查 ZIP 固定包含计划、调度、链、完整性、演练、操作和 evidence manifest，包内 hash 可重算且不含载荷或 secret。导出成功只表示证据包生成成功，摘要 PASS 仍要求调度健康、备份未过期、链完整和演练通过。
- Forbidden action: 禁止把每日全量改名为增量，禁止只停前端冒充无写入窗口，禁止用 API-only/mock 代替恢复演练，禁止为了赶时间省略载荷校验，禁止生成空 ZIP、默认 PASS、明文 secret 或把完整长期归档平台预埋成首版 fallback。
- Evidence: `docs/changes/20260907-backup-minimal-closure.md` 与 `doc/tasks/20260907-backup-full-incremental-recovery-plan/minimal-development-plan.md`。

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

## 审查服运行承载对齐门禁

- Trigger: 将审查服运行环境改成与正式服一致，尤其涉及 `/var/lib/docker/intruoyi-data/runtime-data`、`/var/lib/docker/intruoyi-releases`、`/var/lib/docker`、`/dev/vdb`、MinIO 容器名、运行数据目录或 `/etc/fstab`。
- Preflight check: 停服或迁移前必须只读证明目标块设备存在、目标挂载点落在目标设备、目标目录存在或可创建且非冲突目录、目标可用容量大于当前 `/opt/intruoyi/runtime/data` 与 release 包数据并保留增长空间；同时记录当前 `df/findmnt/lsblk/du`、运行容器、health、前端和展厅 HTTP、MinIO 桶可读性、当前 fstab 和回滚路径。
- Blocker: `/dev/vdb` 不存在、`/var/lib/docker` 落在根分区且容量不足、当前数据量大于目标可用空间、目标目录已有无法归属数据、MinIO 容器名不匹配、Docker/SSH/health 前置检查失败、或无法形成回滚路径时必须停止，不得停服迁移。
- Verification: 迁移后必须复核 `/opt/intruoyi/runtime/data`、`/var/lib/docker/intruoyi-data/runtime-data`、`/var/lib/docker/intruoyi-releases` 均落在目标设备；后端 health、前端、展厅、MySQL、Redis、MinIO、OnlyOffice 和容器状态通过，并记录迁移前后数据量和 fstab 变更。
- Forbidden action: 禁止用符号链接、伪造 `/dev/vdb`、迁到根分区、压缩删减业务数据、跳过 MySQL/MinIO 数据、绕过容量检查、或只改脚本参数冒充真实运行环境一致。
- Evidence: `doc/tasks/20260827-align-backup-runtime-with-prod/verification-report.md`。

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

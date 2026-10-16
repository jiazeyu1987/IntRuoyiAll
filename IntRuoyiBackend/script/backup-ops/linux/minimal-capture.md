# 单操作员 FULL / I1 采集入口

这是明确选择的主路径。它直接使用现有的真实 SQL 导出、binlog 采集、S3 版本导出、文件差量和 W03 材料绑定代码，不调用旧 producer 的 grant、lease、签发、保管人或技术审批流程。SmallApp 与 IntRuoyi 通过不同的显式源配置共用此入口。源采集不要求恢复目标数据库已经准备完毕。

## 命令与请求

在安装目录运行（原生 Linux，Python 3.9+，root，本机 Docker socket；此模块不提供 Windows 采集兼容路径）：

```sh
python3 -B /opt/backup-ops/linux/minimal_capture_entry.py \
  --configuration /etc/backup-ops/source-capture.json \
  --request /var/lib/backup-ops/requests/full.json \
  --output /var/lib/backup-ops/results/full-result-001.json
```

请求仍为原 Spug 七字段，不增加业务字段：

```json
{
  "schemaVersion": "SmallAppBackupUiRequest/v1",
  "operationId": "smallapp-full-20261015-001",
  "action": "FULL",
  "sourceConfigRef": "smallapp-source",
  "parentChainRef": null,
  "recoveryPointRef": null,
  "targetConfigRef": null
}
```

I1 使用 `action=INCREMENTAL`，`parentChainRef` 与配置中的父链引用完全一致。W02 只处理采集动作，RESTORE 交给恢复模块。配置是现有 registry 选择出的源配置，不推断应用或切换数据源。

退出码：`0` 为已观察到真实 worker 退出 0 且输出完整校验通过；`1` 为真实 worker/启动失败的保留结果；`3` 为未证明完成，应查询原操作；`2` 为调用参数、输入或输出路径等错误，stderr 只给错误码。结果 JSON 使用 `backup-restore-operation-result/v1`，保留原 operationId，requestId 等于 operationId，actualExitCode 为真实 worker 退出码，未观察到退出时为 null。

相同请求和相同配置的重复调用只查询原操作，不会重跑采集；请求文件可以换路径，但字节必须相同。查询仍使用原 `operationId` 和配置，选择一个新的 `--output` 文件，已有输出不会被覆盖。入口的客户端等待超时不会杀死后台 job/worker；后台 job 在真实 worker 退出后发布最终结果。若 worker 被信号终止、源恢复状态不明或后台 job 丢失，结果保持未知/失败，保留材料供检查，不自动启动新操作。操作目录不得为了重试而删除。

## 配置与真实运行输入

参考 `minimal-capture.configuration.example.json`。该文件是需要替换的模板，尖括号值不能作为实际配置运行。没有运行时默认值，没有自动补造资料。

- `archiveRoot`：已存在的私有本地 staging 目录。每个 operationId 建立一个独占子目录，不能与 MinIO 数据目录重叠。归档到 NAS 由 W03 完成。
- `sourceLockPath`：已存在父目录中的每源固定锁文件路径。同一真实源的所有配置必须使用同一路径；非阻塞 flock 仅串行化本路径的采集进程，不声称锁住外部管理员。
- `tools`：本机 `docker`、`ip` 的实际绝对可执行文件路径（不使用符号链接路径）。地址观察固定使用 `ip -o address show`，严格解析 IPv4/IPv6 地址及接口身份；不使用 `-j`，不自动切换其他观察方式，命令或解析失败即明确失败。容器网络地址仍来自所选容器的 Docker inspect。Docker 固定本机 `/var/run/docker.sock`；拒绝远程 host、context 和 TLS 覆盖。
- `sourceIdentity` 与 `sourceFingerprint`：均使用现有源身份文档的 sourceFingerprint，与 W03 选择一致。另给出实际 databaseUuid、数据库名、完整 64 位容器 ID、MinIO 数据宿主目录及容器挂载目标。
- `writerContainerIds`：该实例全部业务写入容器的显式集合，包含 HTTP、后台任务等写入进程。运行期间需暂停该集合之外的外部源管理写操作。代码停止选定容器、开启 MySQL read_only/super_read_only，离线复制 MinIO；检查同目录的其他运行容器写挂载，保留前后数据/位置/版本/内容一致性检查。不提供外部宿主进程排他证明。
- `applicationImageIds`：需要恢复的所有实际不可变镜像 ID，格式 `sha256:...`；必须覆盖选定 MySQL、MinIO 及写入容器。实际 docker save 成功并验证归档中的 image config 摘要后才继续。包括这里的数据库/对象服务，是为完整实例恢复保留版本。
- `passwordPin`：UTF-8 MySQL root 密码原文件，允许一个结尾换行，权限 0600。密码只通过子进程 stdin 传入，不放命令参数或日志。
- `objectSourcePin`：现有 `backup-capture-object-source/v1` 描述文件的真实 `{path,bytes,sha256}`，含实际 S3 endpoint、完整 MinIO ID、端口、buckets、credentialReference、sourceScopePin、versionPins、referenceRecipePin、derivedRecipePin、parentExportIndexPin。显式容器 endpoint 必须精确匹配所选容器 NetworkSettings.Networks 中唯一的实际网络地址，端口等于 containerPort；这条路径不要求 published-port bindings。显式宿主 endpoint 继续核对本机地址及容器发布端口映射。不会尝试其他 endpoint；凭据绑定和真实 S3 读取检查保持不变。
- S3 凭据文件：0600 的 `backup-source-s3-credential/v1` JSON，字段 `schemaVersion,endpoint,accessKey,secretKey`。不归档凭据文件。
- source scope、source identity、版本文档、引用查询/对象绑定、redis/queue 重建分类继续使用现有描述格式。它们是显式数据/恢复配置及其字节摘要，不是签发许可。源引用查询先 SHOW TABLES/DESCRIBE，再 SELECT；不得用缺省业务行或虚构对象替代。
- `configurationPins`：带 `role` 的实际配置原文件列表，保存为 `original-configs/<role>.bin`。`materialPins`：带 `purpose` 的部署、工具、外部秘密引用等实际文件列表，保存为 `recovery-materials/<purpose>.bin`。禁止把密码/S3 凭据文件作为公开材料引用；配置内容本身应使用秘密引用。两个列表都不能为空，名称不重复。
- `limits`：真实工作/清理/调用等待/单次查询/服务就绪时间预算、累计材料写入字节（含临时 SQL 和数据观察 scratch）、最小剩余空间、全表数据观察行数/节点数/字节预算。工作超时仍尝试在独立清理预算内恢复源服务。诊断控制 JSON 与进程日志不放入归档材料树。
- `parent`：FULL 必须 null。I1 给出匹配 parentChainRef、上个真实 capturePointPin、W03 repositoryPointPin、W03 已发布 objectContentIndexPin；源 object descriptor 的 parentExportIndexPin 必须与后者相同。检查父点字节、源/代际/恢复点、数据库 UUID、manifest 成员和父对象实际内容；不要求旧签发或人工 completed 回执。父 binlog 已清理/中间轮转缺失、跨源、哈希改变均明确失败，不转为 FULL。

缺少真实值时可以继续代码开发，但相关实际采集动作会明确失败。空恢复目标和后续浏览器验收不属于首个 FULL 的前置条件。

## W03 交接

成功结果 `data` 包含真实的 `captureRoot`、`captureInputsPin`、`archiveEntriesPin`、`capturePointPin`、`objectContentIndexPin`、`sourceRestorationPin`、`recoveryPoint`、`sourceIdentity`、`logRefs`。各 Pin 均为真实绝对路径、字节数、SHA-256；不表示权限。

材料布局继续使用：

```text
capture-recovery-inputs.json          backup-capture-recovery-inputs/v1
capture-archive-entries.json          现有 directory / file|bytes|sha256 映射
payload/database.sql.gz               FULL SQL 或 I1 binlog SQL gzip
payload/database.sql.incremental.raw  仅 I1；保留原始增量 SQL
payload/database.sql.incremental-scope.json  仅 I1；实际范围校验
payload/011-source-data.json          全表类型化行摘要/结构观察
payload/012-source-data-after.json    同点后观察
payload/capture-content-point.json    backup-capture-point/v1
payload/capture-file-inventory.json   完整物理文件清单
payload/minio-physical/               FULL 全量；I1 仅变化文件及目录
payload/original-application-images.tar
payload/050-original-images.json
semantic-capture/                     版本/删除标记/内容/引用/重建资料
recovery-materials/index.json         现有材料索引格式
original-configs/config-capture-stage.json
original-configs/<role>.bin
source-restoration.json               材料树之外的真实源恢复结果
```

I1 的完整 fileInventory、changedPaths/deletedPaths、父数据库位置、内容复用继续保留，不能从一个独立 I1 材料目录直接当作 FULL 恢复。capture point 的 `sourceRestored=false` 是既有归档前点格式；实际源恢复状态以独立 source-restoration.json 为准。这里的 `disasterRecoveryVerified=false` 不声称完成恢复验收。配置阶段文件使用 `minimal-capture-configuration-stage/v1`，记录实际复制原文件，不伪造旧生命周期回执。

W03 直接消费 inputs/entries 与 `formal_capture_recovery_inputs.py` 的现有 assembler，校验真实 source-restoration 后归档/独立读回，再生成 RepositoryManifest、RecoveryMaterial、已发布 objectContentIndex 和 RepositoryPoint。诊断日志、锁、scratch 和过程状态不进入 archiveFilePins；原始七字段请求与统一结果协议不变。

## 验证范围与运行边界

本地 Linux 测试命令：

```sh
python3 -B -m unittest discover -s /opt/backup-ops/linux/tests -p test_minimal_capture.py -v
```

测试使用任务自己的临时文件、实际本地子进程、明确无效的工具路径验证拒绝与原操作查询，不连接业务源，不构成真实 FULL/I1 或恢复验收。语法/导入/CLI 和材料校验测试可先完成；真实运行还需安装者提供上述源、凭据、目录和预算。W05 是唯一远程安装/运行负责人；本轮不访问远程、不修改基线业务记录/附件、不创建恢复目标。

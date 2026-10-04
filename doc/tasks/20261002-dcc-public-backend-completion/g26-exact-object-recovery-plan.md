# G26 本机缺失原文对象的精确恢复方案（待执行）

## 范围与当前事实
Root 已完成本机测试库 19 项正式迁移；该授权不包含本方案的对象写入。本方案仅涉及本机原有 `docker-minio-1`、`configId=28/storage=20`、`127.0.0.1:9000` 原 bucket 下的缺失原文对象，执行负责人为 Root。

Root 真实只读 G25 GetObject 核验 39 个源文件引用：35 项 SHA-256/字节数相符，4 项返回 `404 NoSuchKey`。Root 又以真实数据库 `infra_file.path` 核对，发现 1079 和 1095 两条 infra 记录实际指向同一个对象，因此本方案恢复 **3 个唯一对象，覆盖 4 条 infra 引用**。不修改任何数据库、文件编号、名称、Master、版本、签名、原引用或 bucket/container 设置。

| infra_file ID | 对象 key 的 UTF-8 SHA-256 | 原文字节数 | 原文 SHA-256 | MIME |
|---|---|---:|---|---|
| 9198354931064 | 8e0457c92be8e53d420496a5202065e667392fc6e2f06f1fb919d66ae161097e | 37120 | 2ad539f9095e70d70e94571207a51ca6ca1e3f7a6f7e04947c59db78100212c0 | application/pdf |
| 9198354931068 | 1484202bd71e6b66bf6d21d0900ddcd997ce4d5cdd01b51dac6debd98d58803a | 37120 | 2ad539f9095e70d70e94571207a51ca6ca1e3f7a6f7e04947c59db78100212c0 | application/pdf |
| 9198354931079 | 94de5840221043406c340aff0a96e0bc7244040fd1cea1ad195711a791b21779 | 36872 | cb3f40ac2ca8cebd85c5a895a64588e5b6298517a2061c8227c62e715a73a71f | application/vnd.openxmlformats-officedocument.wordprocessingml.document |
| 9198354931095 | 与 1079 完全相同 | 36872 | 与 1079 完全相同 | 与 1079 完全相同 |

固定的 PDF 候选是主工作区 `doc/tasks/20260918-dcc-void-e2e/obsolete-e2e-source.pdf` 或 `doc/tasks/20260918-dcc-upload-full-e2e/upload-source.pdf`。固定的 DOCX 候选是集成工作区或主工作区 `doc/tasks/20260729-test-server-wangsiyu-file-upload-simulation/input/codex-upload-simulation-20260729.docx`。工具每次重新读取、证明 SHA 和字节数完全相符；选定候选丢失/改变则停止，不自动换另一个文件。

证据由 Root 保存在 `C:/IntRuoyiBackups/20261003-dcc-integration/`：`g25-source-bytes-run4/receipt.json`、`source-bytes-results.jsonl`、`g25-missing-source-recovery-candidates.json`、`g26-four-source-object-identity.jsonl`、`g26-silo-topology-readonly.json`。凭据、实际对象 key 和正文不进入任务交付文档。

## 条件创建能力依据及限制
本机只读 binary/version 与启动拓扑证据确认服务器是 PGSTY Silo `RELEASE.2026-08-06T00-00-00Z`、commit `3be10fcc1a44f6620ded0bd303461f9d688cca23`，仅一个 `/data` 本地存储端点。它是 MinIO 派生项目，不能拿现行 AIStor 文档替代其运行证明。

当前 G20 Jar 中 AWS SDK 2.44.0 真实 `PutObjectRequest.Builder` 提供 `ifNoneMatch(String)`。离线测试使用真实 SDK marshalling/signing 和内存 HTTP transport，核对发送请求确有 `If-None-Match: *` 与正文 SHA checksum，实际网络为零。

对应实际 fork commit 的 [PUT handler 源码](https://github.com/pgsty/silo/blob/3be10fcc1a44f6620ded0bd303461f9d688cca23/cmd/object-handlers.go#L1911) 将条件请求交给前置条件检查；[单 erasure-set 写入源码](https://github.com/pgsty/silo/blob/3be10fcc1a44f6620ded0bd303461f9d688cca23/cmd/erasure-object.go#L1176) 在对象锁内检查现有对象后决定是否写入。此为静态依据，实际条件执行仍以响应为准。HTTP 412/409、条件不支持、拒绝或异常都停止，不退回无条件 PUT；客户端无法替一个忽略条件头的服务器补做原子检查，若运行版本或单 pool 拓扑不符则 Root 不得执行。

AWS 的 [条件创建定义](https://docs.aws.amazon.com/AmazonS3/latest/userguide/conditional-writes.html) 和 [PutObject API](https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutObject.html) 明确使用 `If-None-Match:*` 避免覆盖当前对象。这里即使遇到 409，也不按通用文档建议自动重试，保留精确人工判断边界。

## 已核实的实际 bucket 策略
Root 在真实本机 config 28 上运行只读 probe，退出码 0：GetBucketVersioning HTTP 200 返回 versioningStatus=`UNSET`、mfaDeleteStatus=`UNSET`；GetObjectLockConfiguration HTTP 404 返回 `ObjectLockConfigurationNotFoundError`，Object Lock=`NOT_CONFIGURED`，default retention mode/days/years 均为空，无生命周期错误。受保护证据位于 `C:/IntRuoyiBackups/20261003-dcc-integration/g26-bucket-policy-probe/bucket-policy.json` 和该目录 receipt。此核验没有数据库/对象写入，也不读取正文。

因此恢复工具沿用该实际 bucket，不发送 retention、hold 或 encryption 设置，不变更 bucket/container，不将应用候选 config 中的 7 天 COMPLIANCE、hold=true 冒充实际对象策略。这里的 DCC “作废后保存 20 年”是业务归档/编号名称占用规则；当前 bucket 未启用 Object Lock，本恢复不宣称具备 20 年 WORM 防删除能力，也不宣称补回历史 object metadata、versionId、ETag 或 LastModified。任何单独建立长期存储保护的需求应另行 review，而不混入这三个缺失正文的恢复。

## 执行前置条件
1. Root review 工具、离线测试和输入契约，重新核对文件与依赖指纹。
2. Root 获得用户对 **本机三个指定缺失对象、最多三次条件 PUT** 的具体授权并记录真实消息引用。技术 flag 或审批账号不能代替用户授权；数据库升级授权不能复用为这一步。
3. Root 从同一真实库只读取得 config 28、四条 infra_file 原始 key/MIME/size 与源 SHA，逐项匹配已封存事实和工具固定 scope。实际 bucket UTF-8 SHA 已冻结为 `eef43e6566706fff3d910f5ea7220e06c51ad4bbcd34aa71e8c863ca7cece381`，region 固定 `us-east-1`。凭据仅存在父进程内存和 Java stdin，不能写输入 JSON 文件、命令行或日志。
4. Root 重新核对实际 binary commit、单本地 pool、其他对象 writer 已停止/隔离、服务归属和目标容器。不得停止他人的服务，不能只停止前端就声称没有 writer。
5. 原 bucket 策略已由 Root 真实只读 probe 核实如上；执行前确认该证据仍对应同一 bucket/容器且策略未变。工具不发送 retention/hold/encryption 设置，不改任何 bucket 策略。如果必须额外补对象 metadata 才能创建，停止并准备具体方案；不能把 config 中的 7 天 COMPLIANCE+hold 猜成已确认的业务长期留存策略。

## 实际操作顺序
1. Root 建立新的受保护输出目录，持久化输入身份摘要、授权引用和工具/依赖哈希，不持久化凭据/实际 key/正文。
2. 工具严格解析四条输入，固定 ID、config、key SHA、body SHA、字节数、MIME、候选路径、精确 alias 和三唯一键。所有本地候选先完整核验并加载到任务内存；任何失败先于客户端创建。
3. 对三个唯一 key 逐个发 GetObject。仅 **实际 404 且错误码 NoSuchKey** 可继续；任一已有对象（即使内容相同）、403、缺 bucket、网络异常等都停止，PUT 为零。
4. 三个前置核验全通过后，按输入固定顺序每 key 最多一次 PutObject，条件 `If-None-Match:*`、已验证正文 buffer、正确 MIME/length/SHA checksum；实际 HTTP guard 拒绝改 method、host、port、bucket/key、移除条件、multipart/copy、retention/hold/encryption 或 query。SDK retry 为 zero。
5. 三 PUT 全被接受后，对四条 infra 引用分别重新 GetObject、比较实际 SHA 和字节数。1095 的读取指向与 1079 相同的真实 key，但仍单独记录引用核验结果。
6. Root 保存安全 JSONL、实际退出码、工具/输入事实/结果哈希和时间范围。仅三 PUT + 四最终原文核验均成功才报告本次对象恢复 PASS。随后 Root 重跑原 **39 项** 源字节只读核验，只有该独立原脚本 39 MATCH 才可把全量 sourceBytesVerified 标记为 true。这不是前端业务 E2E。

## 影响和失败处置
最多新增三个唯一对象正文，共 **111112 字节**；对象存储自身可能产生内部 metadata、版本记录和存储审计事件，这是条件创建的真实结果。共享 DOCX 只写一次。所有现存对象、历史版本与数据库值保持原样；若 bucket 开启 versioning，会产生新对象版本，不能宣称补回原历史 versionId/ETag/LastModified。

执行不具备跨对象原子事务。前两个成功、第三个失败时，已创建的对象保留，工具报告 `putAttempted/putAccepted` 及 `CREATED_NOT_VERIFIED`，未执行项为 `NOT_ATTEMPTED`；Root 做只读复核后再决定下一步。PUT 超时/连接中断可能已经落盘，报告 `PUT_OUTCOME_UNCERTAIN`，绝不重试或删除。最终 SHA/length 不符或 GET 失败时，也不删除已写对象；先冻结运行证据并阻止后续名称证据放行。

**没有自动回滚删除。** 这些是补回经 SHA 证明的缺失原文，常规恢复无需撤销。若之后确需撤销，Root 先以精确 key、当前 versionId、本次创建响应、实际 SHA/size、retention/legal hold 和后续引用使用情况准备单独 reviewable 处置清单，经用户/质量职责所需授权后走正式对象管理处置；未到期或 hold 中对象不能删除，不扩大权限/绕过策略。不得为了“回滚”修改 DB 引用、原文件名称、版本或签名。

## 用户可审核的授权描述
“是否授权按本方案，在本机现有 config 28 的三个精确缺失对象路径补回已核实 SHA-256 完全相同的原文，覆盖四条历史文件引用？最多三次带 If-None-Match:* 的条件创建，不覆盖任何已有对象，不修改数据库或历史签名；遇到冲突、拒绝或条件不支持立即停止，保留已产生结果供核对。”

只有 Root 准备完整并 review 后才能发出这一最终权限问题；本子任务不向用户重复索取授权。

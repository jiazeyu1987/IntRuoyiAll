# G39 三个缺失原件的执行核对清单

状态：READONLY_EXECUTION_REVIEW_READY_FOR_ROOT。2026-10-04。本 Agent 仅读现有执行代码、封存材料、G39真实授权和历史安全收据，未连接数据库、未执行 HTTP/GET/PUT、未启动服务或浏览器、未运行 Maven/Git、未修改执行代码；只新增本文。Root 是唯一实际执行人。

## 授权与当前依赖

最新实际授权来源是主任务 `g39-user-authorization.json` 中 exactOriginalObjectRecovery：4个 infra ID、最多3唯一对象、只恢复缺失原件、不覆盖、不改库历史、恢复后独立39GET。旧 G26 manifest 的 pending 文字是当时状态，不覆盖这次实际答复。

Root现有入口所要求的专用授权文件已经存在并经只读核对：

`C:/IntRuoyiAll-int_main/doc/tasks/20261001-dcc-integration-unblock/g26-object-recovery-authorization.json`

它包含 runner 强制字段：`actualUserApproval: true`、`maximumUniqueObjectPuts: 3`、sourceFileIds 恰为 `9198354931064/1068/1079/1095`（各为完整字符串 ID）、真实 `sourceMessageReference` 指向 G39 原答复 item2。SHA-256为 `0e65a62bbef6b563789b411592cd98e24e5ecfb22129395e9fed13d474cfb758`。`--authorization` 必须使用这个确切文件；G39总授权文件结构不同，不能直接代替。

恢复 runner7个资产、41个编译类、42个库及目录精确库存全部与 G26 seal 相等；独立39 reader 的2个源文件、17个编译类、41个库、extraction receipt 和额外 reactive-streams 库均相等。本次不重跑原111个离线测试。

当前 G35主应用 Jar SHA为 `668e7dae96c17a4e788aa8218dfd62031f60aa17b2100f9805c7ce051bf9af7a`。这两个 standalone helper 使用**原已封存 SDK runtime**，其来源仍是 G20 Jar `a4cf78f59d18ac333c64029dd86cf18bf4188c6f0e75762ac3a90eb1a516ef16`；caller没有从当前主 Jar 重新提取，也没有要求当前主 Jar 等于 G20。因此不需要、也不能用新 Jar 静默覆盖原 libs/class/extraction/manifest。旧来源通过已封存实际文件 SHA验证，不表示要启动旧应用。

## Root 临执行前一次核对

1. 当前 sourceDB仍为 `ruoyi-vue-pro`、UUID `92ca05d0-aec8-11f1-a944-02b4e226a5ef`，配置28/storage20、4个 storage/源版本与原facts相等。prepare/recover各自都会重新执行只读精确元数据查询，不能复用旧stdout或打印 config 原文。
2. Root核当前无应用/他任务 writer，并确认现有 `docker-minio-1` 仍是已审 Silo commit `3be10fcc1a44f6620ded0bd303461f9d688cca23`、一个本地 `/data` pool，没有分布式扩展。runner不会自行核Docker topology；旧 `g26-silo-topology-readonly.json` 是2026-10-03历史证据，临执行需核当前事实，不重建容器。
3. 实际恢复 helper限定 endpoint本机HTTP9000、region us-east-1、pathStyle=true和 bucketSHA `eef43e6566706fff3d910f5ea7220e06c51ad4bbcd34aa71e8c863ca7cece381`；3keySHA及原文SHA/size固定。旧bucket证据为 versioning/MFA UNSET、ObjectLock NOT_CONFIGURED，不能拿应用7天/hold配置当实际bucket策略。需要新只读probe时由Root用既有helper保存新的精确输出，不覆盖旧probe目录；旧 `g26-bucket-probe-runner.py` OUTPUT硬编码已存在目录，原样重跑会在读库前拒绝。
4. 两个指定候选文件当前仍符合源SHA/大小：PDF37120字节，SHA `2ad539f9095e70d70e94571207a51ca6ca1e3f7a6f7e04947c59db78100212c0`；DOCX36872字节，SHA `cb3f40ac2ca8cebd85c5a895a64588e5b6298517a2061c8227c62e715a73a71f`。Java读实际原件时再次NOFOLLOW/realpath/有界bytes核验，不使用别的正文替代。

凭据安全：两个caller将DB返回配置仅留在进程内，SDK凭据通过 Java stdin，命令行不带token/key/secret，不持久化payload、object key、bucket原名或正文；Java logging关闭，输出严格ID/status/hash/size/flags。不要在Shell前后打印捕获stdout、config或stdin载荷。查询失败只报告安全类型与退出码；恢复未知效果不能以重跑补证。

## 精确 CLI（只给Root；本 Agent 未执行）

以下输出目录必须尚不存在，且恢复目录必须是保护根的直属新目录。若已经存在，不删除、不覆盖，选择另一个可追踪新目录后核对实际运行状态。prepare只读库+本地原件证明，没有对象 GET/PUT；recover重新读取元数据并执行实际预检/条件创建，prepare成功不表示已经恢复。

```powershell
python -B -X utf8 C:/IntRuoyiAll-int_main/doc/tasks/20261001-dcc-integration-unblock/g26-object-recovery-runner.py --mode prepare --output-directory C:/IntRuoyiBackups/20261003-dcc-integration/g39-object-prepare
```

```powershell
python -B -X utf8 C:/IntRuoyiAll-int_main/doc/tasks/20261001-dcc-integration-unblock/g26-object-recovery-runner.py --mode recover --authorization C:/IntRuoyiAll-int_main/doc/tasks/20261001-dcc-integration-unblock/g26-object-recovery-authorization.json --output-directory C:/IntRuoyiBackups/20261003-dcc-integration/g39-object-recovery
```

recover流程固定：3个唯一key全部 actualGET返回404 NoSuchKey后才允许PUT；最多每key一次 `If-None-Match:*` 条件创建、SHA checksum、正确MIME/length，SDK retry zero；全部接受后对4个ID各做finalGET。1079/1095指向同key，不能把4个putAccepted=true计成4次物理写入。

## 结果与失败处置

- prepare `receipt.json` 必须 PREPARED_NOT_EXECUTED、uniqueKeys3、maximumPuts3、objectWritesAttempted=false、databaseWrites=false；它记录4个安全 body/key SHA证明和authorization/delivery哈希。
- recover开始前写 RUNNING_ACTUAL_RECOVERY；进程返回后先保存安全stdout/stderr长度与SHA及 `STOPPED_ACTUAL_OUTCOME_UNCERTAIN_PENDING_OUTPUT_VERIFICATION`，随后合法safeJSONL才能转 RECOVERY_PASS 或 RECOVERY_FAILED_PRESERVE_ACTUAL_EFFECTS。
- 恢复PASS严格为exit0、4个精确ID全部RESTORED_VERIFIED、actualSHA/len相符、HTTP200、putAccepted=true，acceptedUniqueKeys3。仍需下一步独立39GET。
- 若unexpected stderr、输出解析/schema错误或caller退出2，保留receipt的未知效果状态；不能因为Python报错就说零写或重新执行recover。
- `PUT_OUTCOME_UNCERTAIN`、408/网络/5xx可能已写；`CREATED_NOT_VERIFIED`、FINAL_READ_ERROR/MISMATCH表示已有实际效果。既有对象、delete marker、409/412、拒绝或501条件不支持均停止。无自动retry、无unconditional PUT、无删除回滚。Root只读查当前精确对象及原receipt，再决定后续；原fresh全3缺失协议也不能直接用于部分已恢复后的重跑。

## 恢复后独立39原文证明

```powershell
python -B -X utf8 C:/IntRuoyiAll-int_main/doc/tasks/20261001-dcc-integration-unblock/g25_source_bytes.py --facts C:/IntRuoyiBackups/20261003-dcc-integration/g23-legacy-name-facts.jsonl --reader-task C:/IntRuoyi/20261001-dcc-integration/doc/tasks/20261002-dcc-public-backend-completion --output-directory C:/IntRuoyiBackups/20261003-dcc-integration/g39-all39-source-bytes
```

这不是4条恢复结果的复制：caller重新核39个version及39个storage元数据（共80行runtime/config/version/storage事实），每个source ID独立GetObject。成功条件：exit0、receipt `SOURCE_BYTES_VERIFIED/sourceBytesVerified=true/objectsRead39/matches39`，39个精确ID全部MATCH且SHA、length、HTTP200、无error；databaseWrites/objectWrites/bodyPersisted/credentialsPersisted均false。缺任一项不具备历史名称登记正文前置，也不当作前端E2E。

使用以下原seal，不用新主应用构建改写旧源证明：

| 材料 | SHA-256 |
|---|---|
| g23-legacy-name-facts.jsonl | 8c17e87327b31160e86be9d4c9424670c9a5e8097c1d909c8382f021819a416b |
| g23 facts receipt | f4c863bda07b4b7b411cf5a5247c40faad1444030c8632d8bdf8bfc982e2dcbf |
| g26-delivery-fingerprints.json | 3238fc501af8d62115266bac0c8b12420641d8821cba02fdba32a1ef4211634b |
| g25-readonly-source-bytes-delivery-fingerprints.json | 7d6d0c3571691cdb96f17b9c175d3e8d6758b30de44c3754590861124c044429 |
| g25 runtime extraction-receipt.json | f5f29d74181ae11ed18bf546abf3a2e60322749964166a373922ed89af400ce9 |
| g25-extra reactive receipt.json | e4b148c3b6382d10b9f8f0729af39b78ebd323e8b0be395e496e3c3330c6b87c |
| Root g26-object-recovery-runner.py | 1a15fd11125e1898abeb8b65e89adc93076d5506d3ebb3d1a6c364eb498219bf |
| Root g25_source_bytes.py | ef88f74d359536cebd58859df2ea2a20be80c88a73cff4af5f55960b6d495894 |

当前新授权不等于实际质量批准。恢复、独立原文核验、新schema和后续审计/历史登记分别留真实收据；本文不把它们合成已执行结论。

# G25 实际运行 Review

2026-10-03；主管理已按用户真实授权执行隔离演练及本机测试库19项升级。此处是实际运行结论，不是工具离线测试或完整业务验收。

## 数据库

- 隔离库 `dcc_intqms_g18_rehearsal`：首次与重复19项脚本均成功。旧行原列逐行摘要未变，精确新增33条配置、19条新账本，17张新结构表无业务数据；首次/重复结构指纹相同。
- 实际源库 `ruoyi-vue-pro`：重新采集25项外部依赖、当前环境、全部16张受影响原表与账本的原列/原行快照；另生成三份新的受保护备份与命令收据。未启动应用写入方，实时事务/其他客户端/事件检查通过。
- 源库一次19项升级成功，正式同连接环境与执行材料指纹相符。历史文件41615、Master36685、签名339、原名称claim27以及其它受影响表的旧行摘要均不变。仅精确新增33条配置与19条新账本，17张新表为空。
- 不包含25项审计规则启用、质量批准登记、历史名称占用新增表及其登记、对象恢复或真实业务E2E。没有远程/生产操作。

受保护实际证据：`C:/IntRuoyiBackups/20261003-dcc-integration/g25-rehearsal-run/driver-receipt.json`、`g25-source-fresh-upgrade/collection-receipt.json`、`g25-source-upgrade-run/source-driver-receipt.json`及`source-history-proof.json`。原备份和新备份保留，不纳入Git业务代码。

## 原件读取

Root读取当前版本完整绑定与源元数据，逐项对比已封存283条事实中的39版本/39源文件。凭据只进程内/STDIN，正文流式计算，不保存配置秘密、对象key或文件正文。SDK只读取本机config28。

正式39次GetObject结果：35项SHA和长度匹配；4项HTTP404 `NoSuchKey`。没有改写/删除对象或业务记录，也没有标记整批sourceBytesVerified。

首次Java实际SDK构建发现缺少`org/reactivestreams/Publisher`依赖，不能把50项fake-stream离线测试视为真实SDK已可构建。Root从同一已验证G20 Jar提取唯一reactive-streams1.0.4到Root任务独立临时目录，额外依赖单独封存，原41依赖及reader指纹保持不变。后续真实读取返回上述35/4结果。

4个缺失sourceFileId：9198354931064、9198354931068、9198354931079、9198354931095。只读扫描本地任务原始附件已找到全部4项SHA完全相同且大小相同的候选；其中后两项实际使用同一object key。恢复范围应为3个唯一缺失对象，保留4条原记录；尚未执行恢复。候选清单`g25-missing-source-recovery-candidates.json`与实际key摘要`g26-four-source-object-identity.jsonl`保留在受保护目录。历史旧名称完整登记须等待真实原件全部MATCH，不删除缺失4项以换取PASS。

## 审计批准

用户答复“尚未批准”，记录在`g26-quality-not-approved.json`。已提供admin只表示拟用账号，不构成已发生质量电子签名。未执行25项审计配置或最多1条新版批准登记；之前批准的写入授权是条件授权，不能补造时间/引用或借旧版本批准。

现有源码未发现GxP审计策略operation/version的正式质量批准页面或接口；电子签名治理策略页属于另一条链路。后续需真实质量批准依据或明确补齐对应批准入口的实施范围，不能通过普通文件签名伪造审计策略批准。

## 继续工作

历史名称占用核心交给唯一Java/SQL/Maven Owner，在整合worktree以严格TDD实现；新的迁移不改已经封存/执行的19项。3对象恢复工具由另一个子Agent只做离线准备，Root Review完成后再请求这一具体对象写入范围。主管理保留实际DB/runtime/Git唯一执行权，公共E2E及本地融合尚未完成。

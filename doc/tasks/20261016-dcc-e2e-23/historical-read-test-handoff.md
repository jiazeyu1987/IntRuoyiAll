# C23 历史读取测试交接

当前状态：FIXTURE_PORT_REPAIRED_PENDING_ROOT_RERUN。Root已仅补`service.download.DccDownloadFileBinary` import并实际运行旧版本18例（3failure/8error/7pass）；H1确到缺A断言，但B正控metadata/preview通过后下载审计落位失败，不能称完整正控PASS。本轮只加一行`accessLogMapper.insert(AccessLogDO) -> 1`，与现有DccAttachmentDownloadTest成功夹具一致；前后SHA证明断言及其他测试代码未动，生产/主Test哈希、旧compiled-red XML均不变。证据`history-audit-fixture-repair.json`；未由本例重跑Maven，Root仍独占执行。

根因链：`recordSuccessfulDownloadAudit`进入真实`recordDownloadAccess`后，日志Mapper未stub的int默认0触发`DCC_DOWNLOAD_AUDIT_RECORD_FAILED`（源码5094）；事件insert主键、下载记录insert/update及事务端口已有声明。本次不mock审计方法、不吞异常、不减三次provider回调断言。请先重跑原B正控，再H1；其他归属/二次门禁因旧外层拒绝尚未到目标，仍不能记安全覆盖GREEN。

## 授权文件

- `IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccHistoricalAttachmentReadTest.java`：新增同包独立测试，不继承主Query Test，避免把整套主测试隐式带入。
- 同目录`DccAttachmentDownloadTest.java`：必要身份/历史夹具及旧合并断言拆分。原待生效、存储拒绝、跨租户/跨附件、警告、撤权负例均保留。
- Query、guard、主下载策略及主Query Test前后SHA保持，证据`history-test-only-baseline.json`与`history-tests-written.json`。

## 新类方法（12个方法、参数展开计划18例；未运行）

| 方法 | 作用及预期当前行为 |
| --- | --- |
| `currentBIsARealPositiveControlWithoutObsoleteAuthority` | B真实详情/两行归属/当前指针，实际附件预览及下载；metadata、binary、download共3次真实provider回调。应先PASS以证明夹具完整，无OBSOLETE能力 |
| `currentDetailsListExactObsoleteAWithoutGrantingObsoleteActions` | H1：B读成功后才断言精确A行1条；预期有效RED是A行缺失 |
| `historicalMetadataKeepsAOrdinaryAndTrainingFacts`（2） | H2：无上下文metadata及真实SelectedDetail+Query+guard显式历史两路；A两附件、目的、原作者/时间/轮次，不能变B |
| `historicalPreviewChecksMetadataAndBinaryThroughRealBusinessGate`（2） | A普通/培训两参数，metadata和binary分别经过真实Query/provider授权，精确token expected context为A；读取后默认DAILY业务门禁仍拒 |
| `historicalDownloadUsesDownloadQualificationAndAIdentity`（2） | H3：有VIEW/DOWNLOAD无OBSOLETE，实际A字节/名称/哈希及A附件审计；结束后主文件默认DOWNLOAD仍拒 |
| `historicalViewQualificationDoesNotGrantDownload` | 有真实VIEW但无DOWNLOAD拒绝，无storage读取 |
| `noViewAndDownloadOnlyCannotReadHistory`（2） | H4：无VIEW及仅DOWNLOAD两组；名称QUERY始终存在，不能借它读metadata/预览/下载 |
| `foreignProjectOrAssignmentScopeStillRejectsHistoricalReads`（2） | 跨项目、分配硬范围两组，四条直接读链都拒绝，无storage读取 |
| `dailyOldBodyAndMainDownloadRemainDenied` | DAILY旧正文metadata/binary、主文件download仍拒绝 |
| `currentAttachmentCannotBeSubstitutedIntoHistoricalVersion` | A路径+B附件ID必须被真实附件归属服务拒绝；当前旧外层授权先拒属于未到目标关口，不能误称归属通过 |
| `revokedViewIsRecheckedAtBinaryRead` | A metadata后撤VIEW，binary重新拒绝；当前先卡在A metadata，后续GREEN需继续到真实撤权断言 |
| `secondBusinessGateDenialDoesNotReturnAttachmentBytes`（2） | B/A两组：外层资格正常、底层business端口明确拒绝，断言确实到第2门禁（回调计数1），storage为0；A当前旧外层拒绝不能冒充第2门禁已验收 |

真实对象：Query、detail guard、assignment scope、category permission support、view matrix resolver、attachment service、download policy、DccBusinessFileAccessProvider。只隔离Mapper/目录及项目等外部资格端口、存储、token/登记端口；没有stub被测Query/guard/权限判断为true。单位夹具A20/B21与actor99仅内存，非冻结真实A4233/B4247。

## 旧测试拆分

`historyOrPendingStateAndBusinessStorageDenialNeverReturnAttachment`替换为：

1. `historicalAttachmentWithViewAndDownloadDoesNotNeedObsoletePermission`：按新需求改历史正例，补齐非作者、启用用户、同project/master、VIEW及独立DOWNLOAD、B当前指针，OBSOLETE=false；预期旧ACTIVE-only策略RED。
2. `pendingStateNeverReturnsAttachment`：保留待生效拒绝及0存储读取。
3. `businessStorageDenialNeverReturnsAttachment`：保留底层存储门禁拒绝及0字节。

Root建议先单跑新类`currentBIsARealPositiveControlWithoutObsoleteAuthority`，成功后跑`currentDetailsListExactObsoleteAWithoutGrantingObsoleteActions`取得目标服务RED，再跑两个授权测试类查看各直接读关口；没有成功编译/正控和目标断言输出时，不批准生产GREEN。具体Maven命令、日志、JUnit结果由Root提供，本例不执行或预写PASS。

# 问题2完整审批、签名与发布 E2E

结果：新任务自有 CUSTOM 升版链已完整通过。所有业务动作经 Playwright 真实8063页面完成；没有API/DB写入或模拟审批。

## 实际链路与身份

- 新受控导入：2054545668044084589，WORD1107-CUSTOM-REVISION-FULLFLOW，A/1，类型206/类别908777，SOURCE/F1。
- Reviewer 910538真实检出、首次检入：工作小版本2054545668044084590，A/1-1。
- Reviewer保存、重开本次独立CUSTOM：会签部门910384（WORD1107会签部门）、批准admin1、文控admin1，不培训、不下发，文控签名后立即生效。
- 真实提交新目标2054545668044084591，A/2，BPM808424ff-dab7-11f1-9ac1-b082e25ec548。
- admin直属主管签名1706；任务Signer910539签名指派1707、独立会签1708；admin批准1709；admin文控1710。五条证据均VALID。
- 正式结果ACTIVE，受控时间2026-11-07 22:49:04，实际生效22:49:02（来自文控签名1710），发布时间22:49:05，发布件与盖章件均可用，无finalizationError。
- Reviewer实际页面最终读回：currentActive为4591/A2，master2054545668044079398；旧4589为OBSOLETE且supersededByFileId=4591，通过旧版“只读记录”再次读取确认同master。
- 四个正式routeSnapshots均无公共版本号，精确候选分别1、910539、1、1，证明本次CUSTOM未借用公共矩阵。
- Reviewer打开正式A/2受控正文，实际PDF可见盖章与水印，正文标记WORD1106-REFERENCE-BODY搜索成功，无预览错误。

正文载荷证据边界：本次检入在PDF尾部增加注释字节，真实上传新正文载荷并改变哈希；原载荷e86f35a6bafb74d469e89f33c1c43257715871abe8099a15891bd51ebea23a4c，新载荷e623e185405ce1b2ceb245ecc9f3d40369616e8a43656101be4e1ebca45a66bc。PDF渲染文字仍为Version:A/1，不声称正文可见文字改成A/2或新版标记命中。A/2来自正式元数据、版本主指针及审批签名绑定。

## 证据

所有证据在output/word1106-e2e/issue2-fullflow。

- fresh-custom-source.json、fresh-custom-working-body.json：新导入及实际工作正文。
- fresh-custom-saved.json：CUSTOM保存重开精确响应。
- fresh-import-current-relations.json：真实页面读取新导入正式空关联集合code0/files[]。
- 13-custom-final-submit-confirmation.png：正式确认中的目标A/2、会签部门、批准规则。
- fresh-custom-submitted-facts.json、fresh-custom-submit.zip：真实提交和BPM。
- 19-requester-supervisor-and-signoff-signed.png：主管及指派/会签节点已签名。
- 20-requester-active-a2-signed-details.png：原申请人ACTIVE及签名详情。
- 21-requester-source-a1-obsolete-same-master.png、requester-old-source-replaced.json：旧版替代及同master。
- 23-requester-active-a2-formal-body-readable.png：正式A/2正文阅读。
- requester-final-active-result.json、requester-final-active-readback.zip：最终精确断言与原申请人真实页面trace。
- agent3的admin签核、agent1的Signer签核各自在issue3-fullflow及issue1-fullflow归档，由父报告关联。

## 真实故障修复与边界

旧任务4587/4588正式提交时因源受控导入缺current relation set失败，证据03-submit-current-relations-not-initialized.png；旧两条不写PASS、不补写历史数据。

根因修复仅影响新受控导入原事务：HistoricalImportService在保存正文归属、正式master定位后调用RelationStore，精确验证tenant/master/current ACTIVE、导入时间及来源、正文ownership/hash、原始固定与当前关联为空，再插入显式空集合并写独立GxP审计。原导入幂等重放不补写；GET不猜空、不写回；不伪造BPM、签名或受控事件。

修改生产文件：DccControlledFileHistoricalImportService、DccRelationStore、Backend/config/gxp-audit-policy.yaml、gxp-audit-policy-dcc-d.yaml。没有新增恢复端点或权限绕过。既有历史导入如需补齐，需另作正式迁移，不能据本次新导入PASS宣称历史数据已恢复。

## 回归与运行包

agent2-fullflow-import-relations-pass.log：BUILD SUCCESS，46.840s，114/114。

- HistoricalImportServiceTest16、真实HistoricalImportTransactionTest4、ControlledImportRelationsDatabaseTest26。
- BundleLoader14、StrictLoader54，包括实际classpath新审计operation/schema验证。
- 数据用例覆盖精确来源、tenant/master/ownership冲突、非空关系拒绝、其他租户隔离、原导入及审计失败全回滚、幂等不重插。

任务包2026-11-07-22-09-24，backend48063 PID1832；父核验312生产源码及内置audit policy一致。jar SHA256=14fa53c06c9bfac064adfd0ff2db126f5cade5b4bfb56dc53a9714226d393687。

当前仍保留自有数据及全部既有仓库改动，无reset、提交、推送。

## PUBLIC4584追加作废验收

Editor已在TARGET/F1固定引用实际阅读正文并核验只读权限边界。Reviewer随后从真实详情“更多操作”进入受控浏览，点击本行“作废”；保留原引用。页面明确显示主动关联0份、引用项目1个，受限项目明细不向Reviewer展开。

Reviewer真实保存作废草稿40（FCI-1-1794063995300），核对公共route914060/V4，选择会签部门910384（WORD1107会签部门），批准人为admin；NA/NA/NA/不转移实际属性。经“准备审批”确认、正式提交确认后，返回IN_APPROVAL，BPM4fdeed99-dabd-11f1-9ac1-b082e25ec548，页面显示“作废申请审批中”。未取消引用、未新增公共矩阵、未改冻结审批数据。

证据：27-public-obsolete-v4-rules.png、28-public-obsolete-final-confirmation.png、29-public-obsolete-submitted.png、public-obsolete-v4-rules.json、public-obsolete-submitted.json、public-obsolete-submit.zip。

最终结果PASS：admin主管签名1711（23:15:07），Signer910539负责人指派1712（23:21:35）、独立会签1713（23:23:37），admin批准1714（23:26:40）。四条均VALID且精确属于本次4fdeed99-dabd-11f1-9ac1-b082e25ec548作废轮次，未混入原上传1701–1705。

Reviewer原页面刷新实际读回OBSOLETE，作废时间2026-11-07 23:26:42，作废原因为本次输入，master2054545668044079394的currentActiveControlledFileId及currentActiveVersionNo均null。页面明确“已作废，查看历史版本”。再次通过真实轮次下拉选择“作废申请”读回1711–1714四签名。证据32-requester-public-obsolete-final.png、33-requester-public-obsolete-four-signatures.png、requester-public-final-obsolete.json、requester-public-final-obsolete.zip。

Editor最终撤销结果由agent1真实账号验证：原ACTIVE成功正文URL现在拒绝访问，刷新仍拒绝；TARGET/F1固定引用仍保留为A/1/已作废，浏览按钮disabled，并提示仅文控归档；账号认证仍有效。证据在issue1-fullflow的15/16/17截图、final-permission-result.json、editor-obsolete-revocation-trace.zip。未取消引用来制造拒绝结果。

父agent另修复作废/生效警告内“查看BPM详情”原先跳原版本流程的问题。我独立只读复核：index.vue190/222显式传本动作bpmProcessInstanceId，5539优先该参数并清旧selection.taskId，7166普通更多菜单无参数仍保留原selection.bpmRound/file.processInstanceId及taskId语义。所有调用签名与FormInstanceVO可选string类型一致，未发现原导航语义或事件参数回归；真实导航与类型检查由父/agent3留证。

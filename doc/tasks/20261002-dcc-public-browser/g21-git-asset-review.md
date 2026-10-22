# G21 Git候选资产与任务收口只读审查

2026-10-03，仅准备。主树真实分支int_qms，整合树codex/20261001-dcc-integration，两树HEAD仍为a801dc8b91579241e221d129ab34343997673f40。没有git add/commit/reset/stash/cleanup/delete、worker写入、生产修改、测试/build/Maven或服务/数据库操作。Root正在继续迁移白名单执行器与其他Owner交付，最终停写后必须重扫，不能用本轮快照直接stage all。

## 精确机器清单

`g21-git-candidate-manifest.json`按workspace分开列出每个repository-relative path、真实status、分类decision、理由、来源manifest和当前raw/UTF-8 LF SHA256。文件记录generatedAtUtc。只读生成器为`g21-build-candidate-manifest.py`，只写此任务的JSON，不制造自身manifest作为导入provenance。自身输出哈希留到最终停写后确认，避免自引用hash。

`candidate_*`是待Root核对的可提交候选，**不是业务PASS、迁移许可或已批准暂存**；`manual_review_*`不能自动纳入；`excluded_*`应保留原资产/运行证据并明确排除本次提交，不执行删除。

当前清单引用44份Root/子任务导入、冻结、交付及验证manifest。已声明删除的`relations/selector-browser-contract.ts`由h08-d-freeze-receipt记录，理由是正式loadDccSelectorPage取代重复映射；没有现行源码import，不能作为“漏交”补回。有效导入路径未发现其它缺文件。既有已经跟踪且未变化的45闭包依赖不需要为Git重新纳入；迁移执行白名单是另一个决定面。

最后复扫时Root新增收据已使provenance增至45份，main新增文件也继续增长；精确当前数量取JSON的generatedAtUtc及分类计数，上述初次扫描数字不应作为最终停写统计。

## 必须排除的六项

这是两树各三项，**只有三个不同repository-relative path**，不能把“六项”错误当六个不同路径：

- `AGENTS.md`
- `IntRuoyiBackend/yudao-module-infra/src/main/java/cn/iocoder/yudao/module/infra/controller/admin/file/FileController.java`
- `IntRuoyiBackend/yudao-module-infra/src/test/java/cn/iocoder/yudao/module/infra/controller/admin/file/FileControllerTest.java`

依据main主任务`goal-preserved-nontask-assets.json`的workspace+path精确排除。六个recorded raw SHA256与当前字节均一致。两树AGENTS有脏状态；main两个infra文件也有脏状态；integration两个infra文件本轮未在dirty列表中，但仍在保留核验。不能重写换行、拷贝覆盖、夹进脏基线提交或以宽泛infra目录排除误伤下述合法任务。

## 不按模块目录盲选的必要跨模块改动

| 精确文件/组 | 直接DCC依据与需要的Review |
|---|---|
| BPM `formcenter/runtime/FormCenterRuntimeServiceImpl.java` | diff只在`dcc-controlled-file-obsolete`分支冻结每个会签部门义务，重复leader仍是多个独立义务；DccWorkflowFormCenterObligationTest证明实际映射，属于O01/W01。 |
| BPM `service/task/BpmTaskServiceImpl.java`、`DccSignedTaskActionGuard.java`和guard test | 泛BPM approve/reject调用DCC签名guard，不能无签名从通用入口推进；guard仅匹配native上传/升版/作废process definition，不扩大其他BPM角色。 |
| System `AdminUserRespDTO.java`、`AdminUserApiImplPostIdsTest.java` | DTO新增tenantId，实际AdminUserApi映射test断言正式tenant。审核人/文件负责人同租户冻结依赖这个字段，不能遗漏测试或将同tenant判断猜成姓名。 |
| infra `service/job/JobStartupSyncRunner.java`和test | G19独立task已7case/compile交付，唯一变更ConditionalOnProperty，默认行为保留、显式false隔离全库Quartz启动同步。此文件与排除的infra FileController完全不同，属于Root隔离DCC runtime必要修复。 |
| `config/gxp-audit-policy.yaml`、`gxp-audit-policy-dcc-d.yaml` | DCC生命周期/项目/目录/关联/引用正式审计operation候选。完整policy头仍为PENDING-REVIEW，必须记录Root GxP批准/实际覆盖；D专属policy与整合policy部署权不能重复猜测。候选不等于正式policy已批准。 |
| `sql/mysql/20260920_dcc_project_product_create_approval.sql` | diff只把dependsOn错误`.sql`后缀规范为migration ID。属于依赖metadata修复，但仍改变历史文件digest；不能因此重执行旧DDL/改旧ledger或把source候选当MySQL许可。当前Root白名单执行器单独处理。 |
| 前端 `src/router/modules/remaining.ts` | 当前公共project/workbench管理入口detail guard正式接线，正确来源及returnTo限制属于真实页面闭环；不能因router目录不是dcc而漏收。 |
| `script/tests/test_dcc_b_schema_contract.py` | B项目/folder/属性正式schema合同随对应SQL保留，是永久测试不是任务日志。 |

以上完整相对路径在机器manifest的candidate_necessary_cross_module中逐条可复制；不是批量选择整个BPM/system/infra。

## 具体混入/遗漏疑点

1. **未跟踪生产快照/裸诊断误混入风险。** 后端task下`DccControlledFileQueryServiceImpl.java.p15-green`约263KB及`DccFileRelationPermissions.java.p15-green`是任务临时源码拷贝，正式生产文件已在src；不提交这两套副本。`g04-backend-diff-check.txt`、`g07-diff-check.txt`、`g08-b1-diff-check.txt`、`p05-p07-diff-check.txt`、`p14-p15-diff-check.txt`、`ui05-diff-check.txt`、`test-thread-diagnostic.txt`为裸输出，同样明确excluded，保留摘要后按Root授权清理。
2. **日志/截图/trace绝不能`git add doc/tasks`。** integration当前子任务共有约268份ignored原始日志/产物；main本task约172份ignored输出，另约137份可见旧任务trace/evidence产物。以最终JSON实际snapshot数量为准，不相加冒充业务场景数。main旧20260918/20260928 e2e evidence、trace.zip、probe/output JSON不属于当前实现提交；保留不删除其他任务资产。当前source/test永久目录没有发现被忽略的新增DCC生产/永久测试，仅下述两个任务脚本必须force-add。
3. **必须保留却被ignore的专属脚本。** `doc/tasks/20261002-dcc-public-browser/dcc-public-ui-acceptance.e2e.cjs`及`verify-ui-acceptance-preparation.cjs`命中`.gitignore:103 doc/tasks/**/*.cjs`，已经有准确Cleanup Keep和静态准备证据。最终Root需仅这两个路径`git add -f`；不force-add日志/截图/trace/whole task。当前没有实际E2E运行，脚本scope限定，不提交任何credentials/runtime state。
4. **当前Owner新增但未入早期导入manifest的18路径。** 包括ApproveTaskReqVO/RoutePreviewReqVO、application round summary/read HTTP tests、selected dept readiness tests、public placement test、OwnerPicker及detail children/approval-actions/relation-contract/lifecycle/ApplicationEvidencePanel。它们有当前业务代码/测试必要性，manifest标candidate_dcc_integration_source附reviewFlags；Root最终停写时应冻结到最终交付指纹，不能因旧导入表缺它们就漏掉，也不能只凭dcc目录宣称已验收。
5. **两树正式合同确有不同内容。** ownership.md和implementation-contract.md normalized hash相同；shared-contract.md main多了用户最新Shanghai/7天/每分钟确认段；dcc-final-requirements.html main D04已更新这些确定值，integration旧D04仍写提醒未定。正常本地merge前应由Root选择最新已确认文档并记录差异，不整树覆盖或合并旧文字回退需求。
6. **main当前Root task中100余个中间文件不能全部stage。** 导入/sync/record-patrol脚本、heartbeat记录、migration/backup/runtime receipts及rehearsal文件分别是历史过程、当前安全工具和运行产物；机器manifest留manual_review。实际可重放安全executor/只读前置validator应在Owner交付并写Cleanup Keep后逐个纳入；备份数据、运行输出、凭据、秘密配置不纳入。本轮没有读取备份内容或配置秘密。
7. **G21并行前置工具仍在交付。** detail task新增`g21-structure-prerequisite-queries.sql`、`g21-structure-prerequisites.json`、`g21_structure_prerequisites.py`、`test_g21_structure_prerequisites.py`当前snapshot属于manual_review_task_artifact，按其新DDL形状验证BDD看应是后续安全保留候选；Root必须等Owner正式receipt/Cleanup Keep，再复扫。不能将本轮分类误认为要丢弃正在写的必要工具。
8. **原D的两个real response JSON需确认保留定位。** integration只带`doc/tasks/20260930-dcc-d-relations/h02-real-selector-responses.json`和`reference-view-real-responses.json`，并没有此task的task/execution/report。它们是导入验收夹具/来源证据，不构成另一个完整任务。若当前永久测试不再读这些响应且最终report有摘要，可不提交；若继续以它们作实物合同，补主任务引用及Keep，不能孤立拿raw response冒充最终E2E。

## 主任务归属与重复状态

当前主任务唯一是main `doc/tasks/20261001-dcc-integration-unblock`，状态in_progress，承担G1–G6、实际迁移/配置/E2E及最终int_qms合入。integration的public-browser、detail-integration、backend-completion是明确分工子任务，不是相同请求的重复主任务；其in_progress/ready_for_closeout不等于整体completed。`20261003-g19-job-startup-sync`是infra原子子任务，ready_for_closeout有独立BDD与7测试证据，不应按“旧任务”删掉合法infra修改。

main `20260930-dcc-four-module-implementation`仍in_progress且描述原先手动四线程启动；当前原四worker已交付只读，其编排目标明显被现主任务接续。`20260930-dcc-parallel-management`blocked记录旧线程工具缺失/已cleanup，与现在用户授权子Agent不再同一阻塞；不能继续让这些旧状态引导“从头派四个线程”。Root收尾应明确写主任务归属、已接收原worker证据与旧编排终态：范围完全取代则按规则归档已交付编排并引用主任务；仍有未完成且被取代的义务则写blocked/superseded理由。禁止没有引用直接标完成或删除目录掩盖历史。

main `20260930-agents-int-qms-workstation-context`blocked是用户AGENTS文档/Git提交另一个任务，当前AGENTS明确排除DCC提交；不要为了本目标completed擅自关它。main旧20260918/20260928 DCC E2E/owner任务也不默认为本目标重复记录；保留原范围，只有目标/BDD/source一致且有当前移交证据时才由Root归并。

## 清理收据与Git的关系

当前后端task有多轮`*-cleanup-preview.log`，内容明确mode=preview；它们本身甚至被后续preview列delete。不能把“preview exit0”写成apply/真正清理完成。public-browser/detail仍in_progress且后续交付继续，final cleanup需要Owner停写、归档有用RED/GREEN/错误边界到默认保留报告、确认Keep，然后Root先ready_for_closeout再执行正式preview/apply，成功后才completed。

job-startup-sync task Cleanup Keep显式列四份原始日志，这与“最终Git不混入raw输出”的主目标规则需要区分：Keep只决定工作区保留，不自动批准提交raw logs。已存在test-count/fingerprint/report摘要应作为提交证据；Root可复核Keep和最终收据，不改别人的日志/Keep来伪装已清理。

清理收据若要永久提交，应在主execution/verification中归档mode、task实际状态、keep/delete/blocked/warnings、apply退出结果及源码快照；必要小型结构化收据明确Keep，原命令stdout/截图/临时脚本默认排除。本轮没有运行cleanup preview/apply，也没生成伪清理收据。

## Root最终使用步骤

1. Owners停写并交付最终指纹/未决Review；更新G21前置/白名单工具的正式Keep和责任。
2. 运行本只读生成器重扫；逐条处理reviewFlags/manual_review及两树文档差异，确认六项保留hash未变，检查所有candidate已有当前验证而非早期绿。
3. 只按每棵树的明确候选path选择本地baseline/实现/收尾资产；两棵树AGENTS/infra FileController不入stage，不stash/reset覆盖。不要将candidate总体等同审批；历史SQL/GxP policy仍独立Review。
4. 默认保留任务报告，归档命令结果并按Root范围清理自有产物；准确区分preview/apply和新旧编排终态。
5. 实际目标全部门禁与迁移/运行/UI成立后才正常本地合入int_qms，记录source ancestry/commit/postmerge验证及剩余非任务脏状态。本轮不要求origin推送，不能按历史旧规则擅推或将未推阻塞目标。

# 原D4206恢复边界核对

结论：**RETAIN_FAILED_FIXTURE_NO_FORMAL_UI_RECOVERY**。建议生产改动为零，保持 `SOURCE_FIX_REGRESSION_WITH_FAILED_FIXTURE_RETAINED`；原D仍FAIL/NOT_RECOVERED，D02和A/B/C/E不重跑。

## 范围与证据时效

运行恢复更新（2026-10-19 16:30）：已读取Root的dispatch.json及reboot1019-runtime-proof.json，证明int_qms 8061/PID20976、48061/PID25124，120105 Jar SHA ba0a8ead6e8e85cf29a8e339bbbc61a93ec30b3bb2adaf876d2736ff15f006b3，health UP、真实UI登录通过、32份文件/0待办前后相同、5627暂停。该包newFixesIncluded=false；Root明确10继续分析。运行已恢复不改变无正式恢复入口结论，本例未启动UI或写业务，不声明原4206实时复验或恢复成功。下段“尚未READY”描述分析开始时点。

依据 `../20261016-dcc-24-e2e-coordination/recovery-dispatch-20261019.md`，仅核对原工作稿2054545668044084206、Master2054545668044079175、proof8712。分支int_qms，Root尚未通知READY；本轮没有浏览器、API或数据库访问。以下是最后已知事实及当前源码分析，不宣称2026-10-19实时数据复验。

- `../20261016-dcc-24-e2e-coordination/e2e10-type-storage-failure-readonly.json`：2026-10-18 04:28只读快照。
- `../20261016-dcc-24-e2e-coordination/e2e10-root-review.json`：Root独立接受D02，原4206/proof8712未恢复，不作通过证据。
- `../20261016-dcc-24-e2e-coordination/verification-report.md`：E2E06–15最终表保留上述边界。

## 根因

旧版确认已经提交：4206由类别908747/类型90改为类别908748/类型91（T2），状态WORKING，但目录仍913909。其placement仍是项目298/逻辑文件夹36/目录913909，mapping18的类别仍为T1 908747。源驳回4197仍T1。新包修复后续确认事务，不追补旧记录。

proof8712/request35091bb3-0b8c-429c-85f4-43563ce0fb21的before/after目录均913909，均没有projectFolderId。故有两层不一致：当前类型与目录mapping不符；旧proof缺现行存储合同要求的folder事实。只移动目录会与旧after.directoryId再次失配，不能算完整恢复。

## 正式入口排除

后端源码以下相对 `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/`。

| 入口 | 方法/当前行号 | 结论 |
| --- | --- | --- |
| 详情读取 | `service/file/DccControlledFileQueryServiceImpl.java` populateDetailProjectStorageLocation:3965，类别校验3984–3989 | 旧事实触发DCC_FILE_PROJECT_STORAGE_MAPPING_INVALID；应保留严格守卫。Query本轮归E2E22，不修改或读时修数据。 |
| 主动变类型同键重放 | `service/file/DccWorkingApplicationTypeChangeService.java` confirm:57，重放合同校验73 | 重放只验证proof并回读，不补目录迁移。`service/file/DccApplicationExecutionContractService.java` readTypeChangeBoundary:79，111–112要求folder为空时placement/mapping也为空；旧proof不满足。 |
| 新键再次确认 | 同Service select:145 | 当前已是T2，目标仍T2被拒绝；选别的类型仍须读合法原来源，不是恢复原动作。不能重复确认或改回T1绕过。 |
| 前端变类型 | `IntRuoyiFronted/src/views/dcc/controlled-file/detail/DetailApplicationPanel.vue` beginTypeChange:316、confirmTypeChange:375 | 依赖完整详情与已保存工作属性，没有独立的旧转换恢复入口；不能手拼旧request代替真实UI。 |
| 恢复默认属性 | `service/file/DccControlledFileWorkflowServiceImpl.java` restoreWorkingApplicationAttributes:839 | 只恢复保留草稿属性，不改File/Master/placement/type-change proof。 |
| 修改基础信息/导入 | `service/file/DccControlledFileMetadataUpdateServiceImpl.java` updateMetadata:82，状态限制99 | 正式修改要求ACTIVE，4206为WORKING，不可借道修复。 |
| 项目文件夹/位置 | `controller/admin/projectcode/DccProjectFolderMaintenanceController.java`；`service/projectcode/folder/DccProjectFilePlacementService.java` rebindDerivedWorkingType:32、bind:112 | 页面只维护folder，没有任意文件位置重绑入口；bind不覆盖不同现有位置；type-change重绑要求合法before/after folder及新目录，旧proof不满足。 |
| 生命周期投影维护 | `service/file/DccLifecycleProjectionRepairService.java` prove:126 | 只接受已批准/发布的ACTIVE或CONTROLLED_PENDING_EFFECTIVE及FINALIZING投影，不改类别/目录；不适用于4206。 |
| 再检入/保存/提交 | `service/file/DccApplicationExecutionContractService.java` readTypeChangeBoundary:79；`service/file/DccControlledFileRevisionServiceImpl.java` createInitialCandidate链检查:101 | 新工作稿不能代替原4206；原来源链仍须接受旧proof。不能再检入绕过，或只改目录忽略proof。 |

准确边界：`DccPublicUploadPlacementService.lockSourceLocation:59`只锁正式位置并校验项目/文件目录，不完整校验mapping类别。同键重放无法恢复的关键是严格proof校验和重放零写语义，不将锁方法误说成类别mapping校验。

## 最小方案

保留4206、8712及原请求，不删除、不回填、不重签、不重复确认；生产diff与Maven/构建请求均为零。现有入口无恢复方案，服务READY也不会自动新增恢复能力，无需重放失败命令。D02已证明源头修复，不能替代原D恢复。

若以后必须恢复，须另行明确并授权专用审计动作：验证旧proof和实时源身份，原子迁移当前File/Master/placement，保留8712不改写，追加执行合同可精确识别的新恢复证据，并有不依赖故障详情的正式UI及幂等/回滚测试。这是新增业务能力，不能用查询fallback、放松proof或通用修复平台替代；本轮未实施该扩展。

## 验证与限制

仅核对文档结构、证据引用和结论一致性；无实时UI/DB复验，不声明原D当前已恢复。无新样本、生产修改、Maven、服务操作、Git写入、子Agent、清理删除或API/DB业务动作。

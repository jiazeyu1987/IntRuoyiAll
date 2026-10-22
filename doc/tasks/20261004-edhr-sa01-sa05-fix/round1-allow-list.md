# Round 1 allow-list

本清单为原SA01—SA05修复的第一轮追加范围；完整任务资产见task-owned-files.txt。源评审只读，无新线程/子Agent。管理者独立审核通过后可按用户授权提交本任务并本地融合；当前执行者不自行提交或融合，不推送、重启、写共享数据库或执行真实E2E。

| 项目 | 文件 | 范围 |
| --- | --- | --- |
| R1 | MES service/pro/productionrelease/pqc/MesActiveOrderDossierReadScopeService.java | 冻结PQC职责按正式申请阶段、任务状态和APPROVE判定；授权入口事先选择 |
| R1/R3 | 同包MesActiveOrderDossierReadScopeServiceTest.java（test源码） | DONE/APPROVE与多权限正向、驳回与终态负向、上传权限不授予删除 |
| R1 | 同包MesPqcApprovedDossierContractTest.java（test源码） | 调用真实approve服务、H2任务完成writer、真实授权和资料关系writer；终态/NCR负向 |
| R2 | MES service/pro/frontline/MesFrontlinePqcContextServiceImpl.java | 服务端loginUserId写入事件操作者，不接受客户端选择操作者 |
| R2必要相邻修正 | MES service/pro/processpool/MesProcessPoolEventServiceImpl.java | 操作者独立入账；物理deviceId/workstationId出现时仍要求二者及有效操作者完整，不造设备/工位 |
| R2 | MES service/pro/frontline/MesFrontlinePqcSignatureContractTest.java（test源码） | 真实Controller→上下文→统一签名→H2事件→真实证据读取；A=B/A≠B与篡改操作者拒绝 |
| R2 | MES service/pro/processpool/MesProcessPoolPqcEventTest.java（test源码） | 独立操作者正向；物理缺项和非法操作者负例；旧事件合同保留 |
| R2 | MES test/resources/sql/pqc-signature-contract.sql及pqc-signature-contract-clean.sql | 测试专用H2签名/逐件表和清理；没有生产数据库迁移 |
| R3 | MES service/pro/productionrelease/pqc/MesActiveOrderDossierFileService.java | delete传deleting=true，移除重复生命周期检查；职责与现行Controller/前端一致 |
| R3 | 同包MesActiveOrderDossierFileServiceTest.java（test源码） | 捕获实际delete授权参数true与生命周期检查仅一次 |
| V1 | MES service/pro/batchrecord/MesProEdhrNcrManagerDispositionTest.java（test源码） | 正式来源、返工审计目标、真实签名规范夹具、写后状态和辅助审计注入；不改生产NCR门禁 |
| 必要相邻回归夹具 | MES service/pro/productionrelease/MesReleaseParentAffectedStateTest.java（test源码） | 实际Persistence新增通知依赖以显式MockitoBean接入已有单测边界；真实H2业务与审计writer保留 |
| 必要相邻回归夹具 | MES service/pro/productionrelease/notification/MesReleaseTaskNotificationHandoffTest.java（test源码） | 上市initializer注入新增生命周期依赖；取消对没有该字段的Persistence错误注入，真实通知调度器保留 |
| JDK17必要验证修正 | MES dal/mysql/pro/batchrecord/MesProBatchRecordExecutionFieldResponsibilityMapperTest.java（test源码） | 第246行getFirst()→get(0)，仅一处表达式；前置size=1断言保留，用户当轮明确授权 |
| 记录 | doc/tasks/20261004-edhr-sa01-sa05-fix/ | 修正历史JDK21运行时记录；本轮最终以Microsoft JDK17.0.20执行为准 |
| 经验沉淀 | docs/worktree-memory.md既有Maven环境段落 | 按thread baseline调用project-experience-consolidation；补充编译目标/运行JDK区分及-Dtest不豁免全测试编译，不新建长期文档 |

MES包根为IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes；test源码使用src/test/java下同包路径。JDK17既有测试编译阻塞已按授权作等价修正，没有切换JDK21作为最终通过依据。

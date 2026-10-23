# 线程A：审批与文件生命周期

任务id：20260930-dcc-a-workflow。Owner：A。优先级P0。依据：需求v1.3及SC-1.0。先通过G0共同基线、G1契约Review，再实施。

## 目标与修改范围

1. 上传/升版调整为会签→批准→可选培训→文控审核→受控→下发。作废会签→批准，通过即作废结束。
2. 会签分为部门负责人指派及实际会签两步，均记录相应签名；允许负责人指派自己。同人多部门的义务分别完成。
3. 申请会签部门增删形成本轮冻结名单；批准配置按动作读取，不能缺少配置后借其他动作。
4. 受控与生效分开。A负责新旧版本状态和两个定位的正式切换，新版生效时才自动作废旧版；受控生成失败不能改变旧版执行事实。
5. 文控审核显示正文、基础信息、三组属性、会签/批准/培训证据，驳回须签名并回申请人。
6. 按生效日期升序提醒文控办理下发；下发真实保存后显示完成，不因业务没有失败分支而吞异常。
7. A调用C的版本与保留期占用服务，调用B的申请属性快照校验，调用D的会签整改安排；受控成功后通知事件、正式生效切换事件分开。

允许：ownership中A归属服务、路线、BPM相关文件、独立生命周期/会签组件和对应测试。公共DO/VO、upload/detail/browser大页面只提接入差异，不直接改。

## 当前代码依据

- DccControlledFileWorkflowServiceImpl.resolveNextStatusAfterApprove：批准后进入培训记录或PENDING_MANUAL_DISTRIBUTION。
- DccControlledFileFinalizationServiceImpl.releaseThreeWorkflowManualDistribution：下发推动到文控审核。
- 同服务activateRevision/supersedePreviousActiveRevision：受控时切currentActive并SUPERSEDED旧版；生效日期没有控制此切换。
- DccActionApprovalRoutePolicy和20260922_dcc_three_workflow_bpmn_seed.sql：作废仍带文控审核；后续20260923/26修正候选及会签多实例，不改变上述顺序。

上述为工作区静态现状，正式线程开工须重新核对fingerprints。

## 里程碑

- A1：交节点/状态/两个版本定位、轮次及权限设计，Review后新增失败测试。
- A2：修正三动作流程和文控审核/会签指派，GREEN＋回归。
- A3：受控/生效事务及任务调度、保留旧版、下发排序提醒，GREEN＋故障回归。
- A4：交前端组件、接口接入说明、迁移设计及Review证据；不自行部署模型或写库。

## 必须记录的BDD

| 编号 | Given | When | Then |
|---|---|---|---|
| A-01 | 上传needTraining=false | 会签与批准通过 | 下一办理为文控审核，不培训、不先分发 |
| A-02 | 升版needTraining=true | 批准通过、培训尚未完 | 不能进入文控审核；培训完成后进入 |
| A-03 | 独立作废申请 | 会签、批准通过 | 作废完成并结束，无后续四节点 |
| A-04 | 部门负责人指派本人/他人 | 指派签名确认 | 指定人员收到任务，指派证据不替代会签证据 |
| A-05 | 同人负责两个部门 | 完成一个部门会签 | 另一部门义务仍需完成，全部通过才批准 |
| A-06 | 本轮任一会签/文控审核驳回 | 签名提交 | 返工/重提保留历史并按规定重走，不能少签受控 |
| A-07 | 旧版已生效，新版未来生效 | 新版受控成功 | 新版待生效，旧版继续执行，最新关联指向新版 |
| A-08 | 已受控新版到达生效日 | 生效任务首次及重复/并发执行 | 新版生效、旧版作废一次完成，不重复通知/重复审计 |
| A-09 | 受控文件生成/版本切换失败 | 执行事务 | 旧版保持执行，明确失败，无成功事件 |
| A-10 | 已受控文件不同生效日期 | 文控查询待处置列表 | 生效日期升序，到期优先，已处置不重复未办提醒 |
| A-11 | 审批中断或受控失败 | 检查关联整改事件 | 没有成功受控通知；正式受控成功才触发 |
| A-12 | 无签名/无办理资格/错误租户或任务轮次 | 尝试审批 | 明确拒绝，不产生审批或签名成功事实 |
| A-13 | 文件已受控且预设未来生效日期 | 文控确认下发范围和方式 | 记录真实下发且不再审批，生效日期不改，提前下发标识生效前不得执行 |

## 验证计划

现有核心回归：DccControlledFileWorkflowServiceImplTest、DccControlledFileFinalizationServiceImplTest、DccControlledFileRouteReadinessServiceTest、DccControlledFileApprovalRouteAssigneeResolverTest、DccApprovalRouteAdminServiceImplTest、DccThreeWorkflowBpmnMigrationTest、DccThreeWorkflowMatrixMultiInstanceMigrationTest、DccControlledFileStatusListenerTest。新BDD需要新增真实业务断言，不能只查字符串。

在IntRuoyiBackend运行：

```powershell
mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileWorkflowServiceImplTest,DccControlledFileFinalizationServiceImplTest,DccControlledFileRouteReadinessServiceTest,DccControlledFileApprovalRouteAssigneeResolverTest,DccApprovalRouteAdminServiceImplTest,DccThreeWorkflowBpmnMigrationTest,DccThreeWorkflowMatrixMultiInstanceMigrationTest,DccControlledFileStatusListenerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

实际新增测试类应加入选择列表，并核对目标模块Surefire报告有实际执行数量。上游模块无这些类不构成失败，但目标模块零测试不能记PASS。BPM模块若有变更，另运行该模块确实存在的行为与triggerTask测试，不用DCC测试替代。

FormCenter作废与签名边界改动时，还必须执行存在的DccControlledFilePublicationFlowTest、DccControlledFileObsoleteServiceTest、DccControlledFileObsoleteFormEffectExecutorTest、DccFrozenApprovalSignaturesTest、DccControlledFileTaskActionApiTest。培训/下发改动对应DccTrainingAssignmentAckServiceTest、DccTrainingConcurrentAcknowledgementTest、DccTrainingTaskServiceTest、DccDistributionTaskServiceImplTest、DccDistributionReceiptServiceImplTest、DccPaperDistributionAckServiceTest。这些是实际受影响回归门禁，加入选择列表并逐类核对结果，不能以核心列表通过省略。

前端：独立组件的交互/状态合同、签名失败及返工提示测试；主管理整合后统一ts:check和build:local。真实BPM/日期调度/通知只可在另获授权的隔离环境验证，未验证如实标NOT RUN。

## 交付与Review重点

交付state-transition-table、BDD及RED/GREEN、最小改动清单、BPM迁移依赖、签名与幂等证据、两个定位所有读写调用点清单、integration-notes。主管理重点Review双版本事务、旧流程在途迁移、冻结轮次、签名真实性、受控事件与生效事件不混用。

提醒提前量、日期边界时区、培训完成细节尚未确定，给方案不硬编码。旧规则与需求冲突时按v1.3更新对应合同，不能加兼容补丁保留新旧混跑。

特别检查：独立作废目前经FormCenter批准效果执行，原生NATIVE_FINALIZATION_KEYS不包含作废。不可简单把作废加入监听键集合导致重复执行；BPM、FormCenter及审批中心快速办理入口必须同一语义。

## 可复制给线程A的指令

你负责线程A。先读AGENTS.md、对应docs规则、docs/dcc-parallel-delivery/README.md、shared-contract.md、ownership.md和本任务书。基线未锁定时只做设计与BDD，不修复生产代码。G1通过后在独立worktree严格RED→GREEN实施A归属文件；不得改公共页面/DO/VO或其他Owner文件，不提交推送、不写数据库、不部署BPM、不重启主服务、不跑真实E2E，除非本线程收到明确授权。分批交付修改清单、真实验证结果和integration-notes给主管理Review。

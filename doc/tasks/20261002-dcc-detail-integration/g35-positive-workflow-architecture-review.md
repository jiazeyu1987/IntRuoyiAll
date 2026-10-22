# G35 正向主流程架构只读核验

2026-10-04；inttegration分支codex/20261001-dcc-integration。有限范围：公共上传/INITIAL/REVISION/OBSOLETE申请→正式BPM→培训/文控审核→受控/生效→下发，不扩基础资料细节、审批平台或随机旧代码审计。未改生产、未Maven/DB/浏览器/服务/Git。G34主路线隔离已68/3通过并Rootaccepted，不重开。

结论：本轮追踪的确认主线已具备实际公共caller，未发现必须能力仅独立service无caller、当前native与FormCenter入口争用同审批效果或受控/生效pointer接线断链。下面是静态链路证据，**不是实际运行/E2E验收**。

## 上传与版本送审

- `DccControlledFileController` `/dcc/controlled-files/working`（347–354）调用`WorkflowServiceImpl.createWorkingControlledFile`，public source/ticket/placement/正式项目属性保存是真实入口；`/{id}/submit-iteration`（约358）调用`submitWorkingIteration`。
- `WorkflowServiceImpl.submitWorkingIteration`（724起）锁project/source/master/selected，真实幂等/权限/版本前置后，已有latest受控则`revisionService.createRevision`（768），尚无正式baseline的working则`createInitialCandidate`（776）；直接initial正式draft分支核INITIAL意图/真实effectiveDate，不把working检入当受控升版。
- 同方法通过`routeReadinessService.evaluateDepartments(...toActionType(processDefinitionKey))`取NEW/REVISION实际路线和本次departments，claim送审状态/BPM实例、route/task/属性快照保存后提交。与G34修复后旧LEGACY维护独立，不借其它动作fallback。
- `DccControlledFileFormEffectExecutor`旧`DCC_UPLOAD` FormCenter入口明确RETired并指向`/working`（execute/preflight直接failure），所以不是第二个可继续产生“假上传成功”的当前入口。保留旧executor识别不等于当前主线绕回旧流程。

## 会签、批准、培训、文控审核

- 正式`20260930_dcc_a_workflow_bpmn_v4.sql` upload/revision流程：Start→多实例MATRIX_REVIEW→MATRIX_APPROVAL→培训gateway；选培训走receiveTask TRAINING→DOC_CONTROL_REVIEW，不选直接DOC_CONTROL_REVIEW→End。不存在本主线批准后直接受控或培训替代文控审核。
- `DccWorkflowLifecycleController` assign-signoff→`WorkflowServiceImpl.assignSignoff`→`DccWorkflowSignoffAssignmentService`真实task/department义务、密码签名与同task转实际会签人；批准/驳回controller→Workflow正式签名方法后`authorizeSignedAction`保存本轮签名guard变量。平台`BpmTaskServiceImpl`Approve564/Reject813调用`DccSignedTaskActionGuard.require`，签名不只是独立证据service。
- 批准办理中`DccApprovalFileOwnerSelectionService.prepare/bind`在MATRIX_APPROVAL实际调用（Workflow约1551起），owner含在signedReason并绑定实际签名；没有新增owner节点，也不适用于独立作废。
- `WorkflowServiceImpl.uploadTrainingRecord`（1440起）是Controller真实公开caller：真实doc_control/类别APPROVE资格、当前file+BPM+round session/ticket；存trainingRecordFileId后明确`bpmTaskService.triggerTask(processId,"TRAINING")`推进到DOC_CONTROL_REVIEW。标准是文控上传线下文件，不要求逐人线上ack才能进入文控审核。
- `approveTaskWithProcessDefinitionKey`文控审核分支核effectiveDate/需要培训则当前trainingRecord必需，调用实际signatureverify、taskapprove并statussync；签名完整性及最终受控证据在结束event再次校验。下发scope不在文控审核预存成为提前受控条件，而由后续文控下发办理。

## 受控、预设日期生效、下发

- `DccControlledFileStatusListener.onApplicationEvent`实际ApplicationListener读取nativeFINALIZATION_KEYS（UPLOAD/REVISION/旧approval），排除FORM_ACTION businesskey，调用`FinalizationService.handleProcessInstanceStatusChanged`。不是无人调用的finalize独立service。
- `FinalizationService.finalizeOrdinaryApproval`（374起）eventidentity、actualMaster/File锁、currentstage、完整冻结route/task签名及验签；READY_TO_PUBLISH只本事务内部转换，不额外用户发布节点；直接`finalizeRevision`生成受控盖章正文并签名绑定。
- `FinalizationService.activateRevision`（约807）当前UPLOAD/REVISION命中`isThreeWorkflow...`后调用`lifecycleService.completeControl`并立即return；旧流程category.training gating/legacypublicationFollowup不混入本主线。
- `LifecycleService.completeControl`（约38起）受控时写controlledTime/publishedTime/CONTROLLED_PENDING_EFFECTIVE和Master.latestControlledFileId，emit正式CONTROLLED；预设日未到不会修改Master.currentActive。当天已到则同事务activateLocked；未来则`DccControlledFileActivationJob`真实TenantJob遍历dueVersions→activateDue（job已有caller，启用配置是运行门禁）。
- `activateLocked`真实当前链锁内比较版本、obsoleteLowerControlled/20年retain、statusACTIVE+activatedTime+Master.currentActive同事务完成并emit；不会把受控日覆盖effectiveDate，旧版到新版生效才作废。关联消费者使用controlled事件/最新受控解析，整改启动在受控完成后的正式事件，不依赖legacy全员followup。
- `DccWorkflowLifecycleController` `/workflow-lifecycle/{id}/distribute`→`DccWorkflowDistributionService.distribute`真实角色/受控事实/当前状态+部门人员方式保存与payloadhash→`finalizationService.releaseManualDistribution`；native分支使用已保存名单并写distributedTime。无需另一个隐含发布动作，不改变effectiveDate/currentActive；技术传输异常明确事务失败，未创造业务“驳回下发”节点。

## 独立作废

- 公共Controller `/{id}/obsolete`（约1100）→`DccControlledFileObsoleteServiceImpl.obsoleteControlledFile`，exact受控Master/File/旧baseline/权限/独立项目属性/本次departments，FormCenterRuntime createInstance+submitInstance采用OBSOLETE真实路线。
- 批准办理Workflow识别`signoffAssignmentService.isObsoleteProcessForFile`→reviewObsolete，禁止带fileowner；作废V4 BPM只有会签和批准End，不含TRAINING/DOC_CONTROL/受控/下发。
- nativeFINALIZATION_KEYS不含OBSOLETE，作废实际效果由`FormCenterRuntimeServiceImpl`效果执行（1474）调用注册的`DccControlledFileObsoleteFormEffectExecutor.execute`→`applyApprovedObsoleteControlledFile`（180起）；不是少listener，而是分配给FormCenter真实效果接口，避免native finalize再次制版。
- applyApproved核actual批准round/有效signature、sameMaster/File版本/status后OBSOLETE/obsoleteAt/by/reason/20年claim保留、清currentActive及正式审核事件；旧body/version/signatures保留，审批结束不再制造后续环节。

## 运行验收仍待解决的真实门禁

代码caller存在不代表本机真实服务已经按此版本运行。Root仍在核整合Jar/runtime；当前未质量批准审计策略、35/39原件缺失导致历史原名完整登记未激活，代码严格拒绝有关NEW/namegate不是可绕过缺陷。activation job每分钟及AsiaShanghai/7天提醒代码准备、JobHandler caller都有，但实际注册/Quartz启用/实际运行仍待Root已授权具体环境操作核对，不把SQL暂停注册当已调度。

G29/G30/G31真实UI脚本已准备九模式，actualDOM/真实账户/任务自有业务操作尚未执行；本报告不将579/324/68单元回归当完整真实主流程PASS。另Agent同步查正向UI和B/D链，若其报告新的具体caller缺口由Root统一处理，本轮不预先制造假设缺陷或新平台。

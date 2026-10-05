# G63 Native作废后续当前任务身份有限核验

状态：只读完成，无新增确定后端主线缺口；没有源码/Maven/测试/DB/API/浏览器/Git动作。Root已实际作废POST成功、新BPM d7c17fa8-c0d7-11f1-8698-b082e25ec548会签UserTask assignee1；原File4033及原受控BPM保留。当前前端指派panel入口显隐由G63 FE owner修，不借旧同admin页成功替代此核验。

| 后续环节 | 当前准确源码合同 |
|---|---|
| 指派context | DccWorkflowSignoffAssignmentService.assignmentContext:39–72从实际Task.processDefinitionId查询同tenant正式Definition.key；当前作废round与File原BPM不同走isObsoleteProcessForFile，context返回task当前processId/key，不从File旧UPLOAD/needTraining猜 |
| 冻结义务 | 同service.obligation:167按currentTask local dccObligationId+tenant/File读取保存snapshot，row.processId须匹配本轮；G62保存部门JSON已先于草稿create；taskSnapshot在ObsoleteService159–174保本轮BPM/form-ID部门义务 |
| 真实指派 | assign:79–140核leader/candidate启用岗位/部门/真实task授权；签名参数task当前BPM/MATRIX_REVIEW/ASSIGN；选中会签人写本轮义务并设置真实Flowable task.assignee；只REVISION才save整改，OBSOLETE不读写整改 |
| 签名会签 | Workflow.approveTask/rejectTask先从req.taskId取actualTask，isObsoleteProcessForFile匹配正式作废definition/tenant/variablesDCC objectFile/action，转reviewObsolete，不resolveFile旧key；requireObsoleteReview要求ASSIGN证据后原任务会签人及readiness。保存signature.processId为task新作废BPM，bindPublishedCopy用当前受控正文和本轮task事件 |
| 批准 | reviewObsolete批准读取当前作废BPM的原APPROVE_USER_SELECT_ASSIGNEES并沿formalBpmTask.approve；v4 OBSOLETE只有MATRIX_REVIEW→MATRIX_APPROVAL→EndEvent，File.needTraining=true也无训练/文控/分发节点 |
| 文件负责人 | Workflow作废task分支明确req.fileOwnerUserId不得传；FErequiresFileOwnerSelection3427–3430还要求approvalProcessId===fileDetail.processId，当前作废新BPM不同，故旧FileUPLOADkey不会要求Ownerpicker |
| 完成效果 | FormEffect.execute31–54校原FormAction context/已批准PENDING_EFFECT/BPMbinding，传独立obsoleteRound和原version；ObsoleteService.requireApprovedObsoleteRound270核exacttenant/applicant/key/globalAPPROVEstatus/FileId/version，再ObsoleteEvidenceGuard验证本轮每部门ASSIGN+会签批准和最终批准签名/历史结束任务/official验签 |
| 最终身份保护 | G60Service锁定File前像copy供platform；DCC只改原File状态/作废时间/原因，保存20年占号占名。sharedfrom按原ACTIVE/pending、保存原受控BPM，不把独立作废BPM替原source；原Filebody/version/approval/controlDate和已存签名保持 |
| 待办办理路由 | DccApprovalTaskAdapter.resolveControlledFileBusinessKey662读取formCenter作废process.variables.objectId，避免FORM_ACTION businessKey冒FileId；summary详情query由真实Task携taskId/processId，再由WorkflowactualTask分流；nativeMATRIX_REVIEW PROCESS_IN_MODULE，不quick绕指派 |

因此在当前源中，新的作废Task→contextkey→指派签名→会签签名→批准→FormEffect证据链有独立身份闭环，不依赖原File当前UPLOAD key或needTraining。没有确定的后端“再走培训/文控/负责人选择”caller误接。当前FEpanel隐藏是另owner已接任务，不重复更改。

Root下一真实最短路径：从统一DCC TODO处理此新作废BPM，正式本部门指派（可自己）并真实密码签名；重新读取该task后通过签名会签；出现新的MATRIX_APPROVAL task再通过真实批准签名；观察该独立BPM结束、原FileOBSOLETE/实际obsoleteTime、原trainingRecord/原受控轮次与历史仍保留，无后续交互节点。只凭HTTP200或最后status不能假多角色/签名验证成功，Root可只读核审批签名/taskround及效果账本。此报告只源方向核验，不声称这些动作实际完成。

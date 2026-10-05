# G56 文控线下培训记录的独立账号入口核验

状态：read-only review complete；发现一个主流程入口缺口，未实施。当前独立源码核验，不执行测试、浏览器、API、真实数据库、服务、Maven或Git。主管理刚完成的真实链为单admin兼任多角色，不能证明独立文控能发现自己的上传工作。

## 结论

现有培训记录上传及精确角色/文件/BPM/票据验证已实现。当前v4培训是receiveTask，没有面向文控的正式培训记录待办或通知，不能由个人BPM UserTask页发现。工作台“待培训确认”来自另一条600秒阅读确认链；它不代表本次批准后、文控审核前的线下培训记录。不同文控账号只能依靠另行告知文件身份、普通浏览搜索且有现行详情读取权限找到，系统没有已接通的主动工作入口。

另两处与新增入口直接相关的接线边界必须一起处理：现详情读取守卫不含培训等待角色资格；统一中心对所有DCC文件详情PROCESS_IN_MODULE自动插入sourceTaskId作为Flowable taskId，不能把native培训行送入该通用分支。

## 现有链与代码锚点

| 环节 | 当前真实源码 | 判定 |
|---|---|---|
| 培训BPM | `20260930_dcc_a_workflow_bpmn_v4.sql:37,83`，UPLOAD/REVISION均receiveTask TRAINING | 无UserTask assignee，不进入用户任务TODO |
| 批准转培训 | `DccControlledFileWorkflowServiceImpl.syncStatusAfterApprove:3287`无running UserTask的native MATRIX_APPROVAL分支；`resolveNextStatusAfterApprove:3450`返回PENDING_APPLICANT_TRAINING_RECORD | 当前只保存状态；未调用培训记录通知/派生任务服务 |
| 上传正式动作 | Workflow `uploadTrainingRecord:1489`及`validateTrainingRecordUpload:1533` | doc_control+category APPROVE、准确tenant/file/training状态、空record/published、BPM变量/业务键/session、真实票据；成功trigger TRAINING再文控审核 |
| HTTP动作 | `DccControlledFileWorkflowController:30` POST/{id}/training-record；approve菜单权限 | 接线完整，不是缺上传接口 |
| 详情按钮 | Query `buildActionProjection` training条件3250；detail/index.vue `canUploadApplicantTrainingRecord:3734`、培训按钮78、submit5740 | 已加载详情且服务端允许时可用 |
| 统一待办 | `DccApprovalTaskAdapter.pageTodo:189`只遍历BpmTaskService.getTaskTodoPage；page只再组合项目产品delegate | 无receiveTask派生行；536含training只是现UserTask动作选择条件，不能产生任务 |
| DCC工作台审批列表 | workbench/index.vue `buildTaskRows:521`只TaskApi.getTaskTodoPage | 同一空UserTask问题 |
| 工作台待培训确认 | workbench `loadTrainingTodos:597`→getMyTrainingTaskPage(PENDING_VIEW/READY_TO_ACKNOWLEDGE)；`DccTrainingTaskServiceImpl:106`从progress表按用户读取 | 属旧阅读培训任务，不是线下记录上传 |
| 旧培训消息 | `DccControlledFileFinalizationServiceImpl.createTrainingRecords:1080`，TRAINING message businessId为training记录；prepareTrainingGatedRevision:793 | 旧发布培训流程，不是native批准转receiveTask；不能将这个既有消息视作已通知文控 |
| 状态监听 | DccControlledFileStatusListener仅转发流程状态至finalization | 没有培训等待entry节点的通知caller |

## 详情读取与导航边界

`DccControlledFileDetailAuthorizationGuard.isAllowed:48`先检查现running审批UserTask，然后保正式assignment scope、申请人/目录管理/指定生命周期权限/正式browse或目录PREVIEW。其canManageNonActiveLifecycle、isPendingPreviewStatus及resolvePendingStageCode都没有PENDING_APPLICANT_TRAINING_RECORD。由此doc_control+category APPROVE并不独立保证训练等待详情可读；已有目录PREVIEW或管理权限可读的账号仍可能找到，不能断言所有文控均被拒。

Query.canViewFileName:2172及canManageNonActiveLifecycle:2304同样没有此培训角色分支。新增待办不得靠全员grant、manager或自动授正文权限解决。培训记录操作读取应与现POST精准资格共用，加现正式tenant/File/Master/项目硬范围；正文预览/下载仍走原独立授权。未决定扩大任何产品范围。

`approval-center/index.vue.resolveDccApprovalDetailLocation:916`现会对DCC TODO+PROCESS_IN_MODULE文件详情自动附handling=approval、processInstanceId、taskId=sourceTaskId。native培训sourceTaskId属于自己的命名空间，绝不是Flowable UserTask。需要专用sourceType分支，使用真实fileId/BPM、已受guard认可的管理返回上下文，省略taskId/审批handling。可复用workbench当前管理详情导航（同文件433，mode=manage/from=workbench/returnTo），经实际route guard和服务端详情权限验证后显示已存在培训上传按钮。

## 最小接入建议（等待Root定范围）

1. 保持现唯一DCC provider，将培训native delegate组合进adapter：只派生真实当前tenant的UPLOAD/REVISION、PENDING_APPLICANT_TRAINING_RECORD、needTraining=true、trainingRecord为空、published为空、有准确currentBPM的行。官方enabled doc_control账号、approve菜单权限和category APPROVE、正式文件硬范围、详情操作读取资格必须统一；严格检验actual process id/tenant/key/businessKey/controlledFileId及当前TRAINING等待，坏事实明确错误。查询不创建BPM、任务或currentset，不从文件旧轮次猜，不再建第二provider。
2. 专用sourceType建议DCC_TRAINING_RECORD_UPLOAD，sourceTaskId命名含tenant/file/currentBPM，真实businessKey=fileId字符串、currentNodeName=文控上传培训记录、requiresSignature=false、仅PROCESS_IN_MODULE。精确BPM字符串原样保留，不伪造UserTask/taskId或使用上一批准task；adapter完成统一keyword/count/pagination，状态离开训练立即消失。当前没有正式“上传完成本人任务历史”事实合同，不为DONE伪造上传人/时间。
3. 现DCC工作台新增一个同源的“待文控上传培训记录”区块/数量，进入现详情培训弹框；从同一派生查询服务读取，与统一中心同资格与身份。旧阅读培训保留自己的链，不能改成冒充此任务。
4. 批准进入培训状态的同一事务可调用现正式idempotent NotifyMessageSendApi或已有持久消息job，以tenant/file/currentBPM/recipient构造幂等键，专用培训记录模板。只通知实际有资格办理该文件且能打开entry的启用文控账号；不默认申请人/批准人/admin，也不授角色。使用现正式失败记录/重放机制，不能裸afterCommit发送后吞失败。新模板forward insert仅准备，由Root执行。
5. 专用通知和统一中心导航必须标识native培训，目标file/BPM一致、同源详情路径、原管理return合法；页面重新读服务器当前状态后才出现已存在upload按钮。状态已推进时不再次上传、不从链接taskId猜身份。正文权限不随消息或待办获得。

这仅修当前角色工作发现入口，不改变会签→批准→培训（如需）→文控审核→受控→下发，不新增节点，不将线下培训改为600秒阅读，不新增虚构签名或审批。

## 必要BDD与真实验证边界

- Given批准人A、独立文控B（enabled同tenant/doc_control+正式category权限/硬范围），When真实签名批准进入receiveTask TRAINING，Then B统一待办和DCC工作台能定位准确file/BPM，A不是B权限来源；没有Flowable UserTask也不能丢失任务。
- Given B点击该native入口，Then无假taskId进入已授权详情，上传现TRAINING_RECORD票据→POST→trigger实际等待→DOC_CONTROL_REVIEW；原上传/签名/正文权限守卫保持。
- Given外tenant/无doc_control/无类别权限/无读取硬范围/错BPM/已上传/已离开TRAINING，Then无可办任务且写动作仍拒；GET零写。
- Given批准事务失败或通知失败，Then任务状态与正式通知幂等事实符合既有事务/重放合同，不出现先通知但申请未进入TRAINING的假待办。
- 先做实际provider查询/正式permission+route行为的有效RED/GREEN，后由Root经两个真实账号/页面验收。当前报告没有执行测试，也不声称真实不同角色PASS。

## 当前读取资产指纹

| 源文件 | SHA-256 |
|---|---|
| `IntRuoyiBackend/sql/mysql/20260930_dcc_a_workflow_bpmn_v4.sql` | `5ddc77a61d77e56c0af863e1a88f09f3f3da9af2703512f6d66fa2acae904a52` |
| `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/approval/DccApprovalTaskAdapter.java` | `cda7b9dca74c24d0d90fd2d906201bb1ceb0a5272a949e475952943c212f3677` |
| `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileWorkflowServiceImpl.java` | `30d6a320a5eedb858e410dc0663ba8e7dd1ae17c4845d704f8e103faee0c0a4a` |
| `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileDetailAuthorizationGuard.java` | `0348ae1bd3110b7785430e16b4b9cb0a3aa6eda92347b6cab53ea4810d40d015` |
| `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java` | `e187d642f9e20784e82d7b0d59e239b0531caecfd2dd1b6feca7529c0fbc9b70` |
| `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccTrainingTaskServiceImpl.java` | `d78216cb600f34830ef2256ae4d99416472da5804b4fa8332a67e720e9ee0879` |
| `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileFinalizationServiceImpl.java` | `b002e6dc276da53bfc9ed676ff7b47cd45cf7a4dfc4f07dd69960e677406704e` |
| `IntRuoyiFronted/src/views/dcc/controlled-file/workbench/index.vue` | `77bfdc5b988d8a0162de2b0f55637f6fc6f0ee86d4b1f8d730e24f0bf5080949` |
| `IntRuoyiFronted/src/views/dcc/controlled-file/detail/index.vue` | `a56a9f564a7637872d76a0c90b2e7cf63f4178891bcf9cee7c1e4177e9e8eb6e` |
| `IntRuoyiFronted/src/views/approval-center/index.vue` | `faf9ac9b92c3bc27c4d2b6f418a79cec68f43498432d9ba07d8dc802c22a5045` |

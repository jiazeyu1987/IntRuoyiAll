# G54 空关联与非升版指派只读审查

Status: ready_for_closeout — 有限源级结论，Root实际UI已确认NEW0POST阻断；本Agent未修改源码、跑Maven/前端测试或操作DB/API/服务/Git。随后修复需按明确Owner分配先BDD/RED，不取消严格关系权限。

## R01 确认主P1：NEW指派误依赖升版整改加载

Root真实新上传File2054545668044084026，PENDING_MATRIX_REVIEW/BPM e847..4，部门100/107 leader1，uploadRelations=[]，正式signoff-context成功canAssign；点击签名确认指派却0POST，“整改安排尚未加载或校验完成”。

完整触发：Workflow冻结部门义务1206-1220 initially不写processInstanceId，NULL保留直到assign；SignoffAssignmentService#obligation:167-180允许NULLround配合真实task+obligationID，assignmentContext:39-71严核tenant/file/task/currentleader允许指派。RelationAccessPolicyImpl#requireParticipant:28-35则必须row.processInstanceId==round，故beforefirstassignment同合法leader仍ARRANGEMENT_FORBIDDEN。

DetailSignoffAssignment.vue:90-94无条件Promise.all历史relations/people/listRelationArrangements；业务code返回failure→catch error并readyfalse。SignoffAssignmentPanel arrangementsReady=ready&&canAssign，false在按钮submit:52直接拦截，Root看到无POST。因此不是“无关联内容应该宽放权限”，而是NEW错误调用仅升版适用业务能力。

HTML明确只有升版部门指派选关联负责人/期限。最小FE修复：提供正式服务器processDefinitionKey/changeType申请动作给DetailSignoffAssignment；明确REVISION才渲染/请求/等待/校验安排，UPLOAD/OBSOLETE省略该不适用字段，ready取合法指派context，不mock空API成功/不catch403默认ready。REVISION保真实round/candidate/期限严格，不从有无relations反推action；无relations的REVISION也是合法空候选集合但仍按真实动作协议。

BEassign:129当前unconditionalsaveArrangements；snapshotupdate:126先写round，所以NEW真正POSTempty可能通过，但这仍无明确notapplicable语义。有限建议BE只REVISION调用整改save，其他动作非empty明确拒、empty省略，不改电子签名payload/assignment真实caller/资格。Revisionbeforefirstassign的保存安排读取roundNULL问题独立存在，需真实exactobligation/task/round上下文并由owner修统一bridge，而非去掉participant检查或ALLroles授权。

## R02 当前关联缺set：预受控阶段的错误显示

RelatedFileService.validateAndBindRelatedFiles:153-156空选择直接return，非空也只建本版本审批historicalsnapshot；唯一currentset初始化DccRelationRemediationService.recordControlled:122-125在actuallatestcontrolled时创建/切换。

getCurrentRelationView:65-70严格需currentset且listCurrent也assertInitialized。当前NEW尚未受控时没有currentset是现有生命周期阶段事实，不等于数据损坏；DetailRelationsPanel:100无状态区分总调用current，展示CURRENT_SET_NOT_INITIALIZED错误。最小显示合同应新未受控显示“尚无受控关联”，独立审批snapshot空列表可读；不要GETlazywrite、把inflight当latestControlled或将历史缺set也默认为[]。若允许beforecontrol编辑需独立已确认working relation来源初始化写合同，不能本次偷造。

## 证据边界

Root真实UI证明NEW0POST；我只读源码解释链，没读实际DB rows。两defaultdept相同leader仍不同obligation，不从admin菜单猜其任意权限。RelationexceptionHandler业务失败常以HTTP200+非0CommonResultcode返回，因此网络HTTP200不是成功。历史关联[]为已授权空快照；arrangement API资格失败不能冒empty成功。本轮仅提出有限修复，不声称关系全部业务已验收。

## 读时rawSHA

- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileRelatedFileServiceImpl.java · 23307 bytes · SHA256 `04d9df75b808f1e39a90e6c5efc48bd3c3d45dcd16c394297e01d28735f44d10`
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/relations/DccRelationRemediationService.java · 13863 bytes · SHA256 `7ab5cb9f94d8c93104b6759aa4548f8f5c008964d293e0aefc2d6b0a46e3fa55`
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/relations/DccRelationAccessPolicyImpl.java · 3517 bytes · SHA256 `b437323ea091b8730b1f49241260956307f39fa870f84f1270dd20fc5ce56aee`
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccWorkflowSignoffAssignmentService.java · 17067 bytes · SHA256 `f9745c09400be895e9f35b5b33e912706441cd849956a3be6786102e979bc2d7`
- IntRuoyiFronted/src/views/dcc/controlled-file/detail/DetailSignoffAssignment.vue · 5662 bytes · SHA256 `08b193cd85a2226d49f98ff675a0edada5ee436bddb933bc94116b697bdd3209`
- IntRuoyiFronted/src/views/dcc/controlled-file/detail/DetailRelationsPanel.vue · 11202 bytes · SHA256 `8b2e3373ad72843b738967f13e7e62d8e5dd2e326586022a2a792c3ab922a1dd`

## 2026-10-05 10:54 +08:00 有限复核与证据更正

复核 main/int_qms 当前三份后端源码，raw SHA 与上表相同。当前 `DccWorkflowSignoffAssignmentService` 方法名仍为 `assign`，不是 `assignLocked`；`DccRelationAccessPolicyImpl#requireParticipant` 第27–35行仅核当前租户、精确流程轮次、MATRIX_REVIEW义务以及leader/assignee，未包含REVISION动作判定。因此不能据“现行只允许REVISION”的不存在守卫，宣称NEW省略安排后真实POST必然返回FORBIDDEN。

当前可证实的事实是：`selectedArrangements` 第138行把省略字段规范为合法空集合；`assign` 第125–129行先保存义务的真实轮次，随后第131行无条件调用 `saveArrangements`。`saveArrangements` 第52行继续执行真实任务资格守卫，空集合本身不会生成安排记录。这条POST还未经本Agent或Root实际复现，结果须由真实调用或严格行为测试证明；已有真实NEW点击0POST的R01证据不受此更正影响。

后端仍应由已分配Owner落实明确的动作适用合同：只有正式REVISION调用整改能力，NEW/OBSOLETE不调用该不适用能力，客户端非空安排不能趁省略规则绕过审批动作。该建议是业务范围修正，不要求取消租户、义务、真实任务或电子签名校验。FE与BE各自已由Root另行分配，本次仅补充只读结论，没有修改生产/测试、运行构建或访问DB/API/服务。

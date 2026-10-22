# G49 / LD04 后端独立有限审查

Status: in_progress — 当前是实现中读时快照，不是最终 source/test冻结或实际运行PASS。仅只读本轮方向；没有改后端/测试、运行Maven、DB/Redis/Token、服务、浏览器或Git。等待Owner最终pins后一次有限关闭复核。

## 当前方向

截至本次读取，没有确认新的主线P1。原项目创建申请直接接当前唯一DCC provider的delegate，原requestID/tenant/stage组合为taskSource身份，不建立BPM/假file/task；create/review/reject消息复用正式站内消息API同物理事务；批准决定与后续asset writer原两事务保持。审核人仍提交时frozen；批准人当前既有admin规则未擅自改配置方案。

## 源级证据（待最终freeze复核）

- `DccApprovalTaskAdapter#page:140-162` 先把原文件TODO/DONE的完整可见列表与native list合并、排序，再用同window切片，total=combined size；没有先分页native再随意拼total。原SOURCE_PAGE_SIZE200循环/去重/源total异常校验仍存在。
- `DccProjectProductTaskDelegate` 是@Component委托而不implements ApprovalTaskProvider，registry仍唯一module→provider。`ApprovalTaskProviderRegistry:29` putIfAbsent拒重复，DCCAdapter仍providerCode dcc-controlled-file-approval。
- Delegate.list按原request COMMITTED statuses读取，TODO只PENDING_REVIEW/PENDING_APPROVAL，个人审核按frozenReviewerID+currentupdate、批准按currentadmin资格；DONE按reviewedTime/reviewerUserID、approvedTime/approverUserID，不借文件ID。IDs含tenant/request/stage；detailRoute /mdm/product-catalog，query requestId是原Long字符串，actions仅PROCESS_IN_MODULE，requiresSignature=false，没有伪电子签名或BPM节点。
- `DccProjectProductNotificationService:21-55` MANDATORY，reviewTodo/approvalTodo/rejected调用正式sendSingleMessageIdempotentlyToAdmin；key为tenant/request/event/recipient，params带精确targettype/ID/actionURL，空messageID明确失败；不是外部消息或API业务seed。
- 正式NotifySendServiceImpl:43-76 REQUIRED事务复用当前数据源，NotifyMessageService真实INSERT参与外层事务；明确模板/params/幂等载荷检查和唯一冲突锁定回读，不afterCommit伪成功。此处只读Source，真实H2事务由Owner提供测试证明。
- Creator.create/resubmit原@Transactional中先写原request/claim/Gxp再通知reviewer；review@Transactional保存原决定/审计后approvalTodo或rejected同事务。State.markApprovalDecision@Transactional先写WRITING或REJECTED与批准Gxp，拒绝消息同事务；approve facade无新@Transactional、仍决定事务完成后执行独立writer/失败记录，不把新增通知变成晚期writer失败时悄悄抹原批准。
- 新GET byID读取任何status原request，不以pending列表冒详情；ActorSupport复核当前sameid/tenant启用目录/权限及原applicant/frozenReviewer/actualreviewer/approver或既有doc_control/approval_admin管理角色；反向next来自previousID query，>1拒绝，不改原row历史。RespVO全部Long标为JSONstring。
- Native timeline核source type、processInstanceId必须缺省、正整数字符串businessKey及精确tenant/request/stage sourceID，再调用合法byId读取。原file timeline继续process/tasks身份，不拿native造BPM。

## R01 — 实现中P2候选：TODO/消息资格与详情读权限不一致

可证实输入：同tenant启用frozenReviewer持有dcc:project-code:update与统一审批查询资格，但无dcc:project-code:query（批准人同样可出现）。

当前ActorSupport.canUpdate/isApprover、NotificationService.reviewTodo/approvalTodo及Delegate TODO只要求update；因此提交/审核可以创建其消息与待办。实际点击后`DccProjectProductCreateController#get:72-76`要求query，`ActorSupport#assertReadable`又必须query，故准确原申请无法打开，不能按消息办理。当前测试fixtures给所有账号query，未证明这个权限组合。

最小处理方向：对齐合法参与者的读/办理资格（例如exact participant允许其实际create/update读本人原申请，管理查看仍query），或者明确把query作为收到并展示TODO的资格且提交时校验。不能自动grant角色/权限、fallback pending列表或删详情授权。此为当前source候选，不在正在实现的Owner提交中误报最终P1；Root确认业务权限合同后有限case即可。

## 当前尚待Owner交付的边界

- 最终source freeze、新消息模板SQL/payload、真实sameTxn晚failure rollback、approve splitTx拒消息回滚及已批准writer失败历史保持。
- 混合原文件+native TODO/DONE keyword/page/count/order，native registry唯一与timeline sourceidentity/actor授权，Longstring真实HTTP。
- currentrequireAdmin当前读时仍username-only旧实现，而新ActorSupport定义currentenabled/update；Owner正在GREEN，最终须确认两者一致，不能凭本次途中源码给最终已关闭。
- 本review未执行任何tests或实际API/站内信页面。未把Owner的H2替身、文件名green日志或有效RED当实际运行PASS。

## 读时raw SHA

- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/approval/DccProjectProductTaskDelegate.java · 9119 bytes · SHA256 `7f23467a4bad0032b097f534529b90f9622e471dbedd63779bff0c659460b25b`
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/approval/DccApprovalTaskAdapter.java · 45017 bytes · SHA256 `cda7b9dca74c24d0d90fd2d906201bb1ceb0a5272a949e475952943c212f3677`
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/projectcode/productcreate/DccProjectProductActorSupport.java · 3296 bytes · SHA256 `38b5a5c8b7d28690e33341145b0262ad35470c17b63554749c35ec28ec70ec57`
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/projectcode/productcreate/DccProjectProductNotificationService.java · 3703 bytes · SHA256 `296c2c6f7aadd8fd7482bb44a6d612caf2af5544347732a6b9c0a250a274325c`
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/projectcode/productcreate/DccProjectProductCreateServiceImpl.java · 19623 bytes · SHA256 `a4b4131ec713e6a62e5d7003ffc28762cfebdc960a9c2a4c7ddb3535c1408cf0`
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/projectcode/productcreate/DccProjectProductCreateStateService.java · 5032 bytes · SHA256 `7a5bc8690d8a551d30a46089b37b34b7ceab41bc761d903d1b14d8e35e398de5`
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/projectcode/DccProjectProductCreateController.java · 6991 bytes · SHA256 `cba7985973ff3c43ffa5ce863846954c4f5b0d38588ca6b5c44bbaafd1527e70`
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/projectcode/vo/productcreate/DccProjectProductCreateRespVO.java · 3330 bytes · SHA256 `3ae6f9d366cfff91fd4ca9f1c88073cca65b78291d207fafd93955e6bc546afb`
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/mysql/projectcode/DccProjectProductCreateRequestMapper.java · 5140 bytes · SHA256 `b8e29a2055bdcd6bf007af48ca54f161cf213ed90312e8c2743e00548d38bb5f`
- IntRuoyiBackend/yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/approval/service/provider/ApprovalTaskProviderRegistry.java · 2827 bytes · SHA256 `aa59b5e18bcd479063546c04b388ad7a3c1313ce97bcbcf8c92ae8c8b6599c55`
- IntRuoyiBackend/yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/service/notify/NotifySendServiceImpl.java · 8203 bytes · SHA256 `9e0ed125ecdc368c8d1e8644eef5dbecb3e01badbb01b2208b28a01304bc64cb`
- IntRuoyiBackend/yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/service/notify/NotifyMessageServiceImpl.java · 4071 bytes · SHA256 `09d94899555dfc6cf67678666ae0d4a3e598f6b943e47cd22236680730228d9a`


## 最终有限闭合复核 — 2026-10-05

Final status: ready_for_closeout — LD04 当前封存源级审查通过，R01关闭；本次未发现需要返工的确定主流程P1。以上早期 in_progress/等待项是历史读时记录，以本段最终结论为准。仅追加本报告，未修改后端/测试/target、重跑Maven或访问DB/Token/服务/浏览器/Git。Root当前打包/后续实际验收不由此结论替代。

封存清单 g49-backend-delivery-fingerprints.json rawSHA 95ba26f307e21ad04975ddcacd136e792e9b4bd898a02d668398e4d57a3a9e41：19 files（10生产Java、7测试Java、1SQL、1isolatedSQLresource）全部rawbytes/SHA与当前文件一致，0drift。13 compiledClassInventory条目也全部匹配，其中1个相同 CreateServiceImpl.class 重复登记，实际12个unique class路径；重复不是第二provider/重复实现，本review不冒称13个独立class。

R01已关闭：

- ActorSupport.canUpdate现在是正式update且canQuery；isApprover复用此资格，且当前启用同tenant目录必须实际username admin（原批准人政策未擅自变配置）。
- Creator.create/resubmit先requireReadableAccount申请人；Notification.send所有收件人requireReadableAccount，冻结审核人reviewTodo又canUpdate，批准todo requireApprover；不授查询/角色，也不通知一个打不开原申请的人。
- Native Delegate.list先当前actor资格，再无project-query直接返回本source空列表，原DCC file待办仍独立保留；TODO/详情ById的query资格一致，byId仍exact original ID关系/tenant资格，没有pending fallback。
- 实际测试 reviewerWithoutFormalQueryPermissionCannotReceiveAnUnopenableTaskOrMessage 和 applicantAndApproverNeedTheirExistingReadPermissionWithoutAutomaticGrant 证明晚失败回滚原request/claim/Gxp/message、不可读admin不生成批准待办，原file source回归保留。这里只读实际归档测试证据，不重跑。

时间线结果已按持久化阶段事实修正：Submit=PENDING_REVIEW/SUBMIT；Review根据其reviewReason/rejectReason保持真实APPROVE→PENDING_APPROVAL或REJECT；Approval根据approvalReason/rejectReason保持WRITING/APPROVE或REJECTED/REJECT。下游批准驳回不把此前审核通过或提交写成“驳回”；原actor/time/comment、原request tenant/stage/sourceID保留，真实test nativeTimelineKeepsReviewPassWhenTheLaterApprovalRejects断言三阶段实际actor/time/result。没有以当前最终request状态覆盖每个历史阶段。

一次provider与事务主线保持：

- DccApprovalTaskAdapter仍唯一DCC provider；ProjectTaskDelegate不是provider；正式Registry.putIfAbsent仍拒相同module双provider。混合file/native先完整受权筛选和同排序后切同page window，count对应同combined列表，keyword真实code/name/originalID匹配，native IDs独立tenant/request/stage，PROCESS_IN_MODULE且无BPM/假file/电子签名。
- 实际正式Notify API→NotifySendService REQUIRED→NotifyMessage INSERT加入通知Service MANDATORY及原create/review/reject事务；幂等键tenant/request/event/recipient固定且payload冲突拒，缺模板/收件人/空ID拒，不afterCommit假成功。
- approve facade仍原非事务编排：State.markApprovalDecision事务保存原批准决定/Gxp（拒绝消息同它），之后writer独立事务执行资产并失败独立记录；新增通知未抹既有批准/失败重试证据。历史原行、旧消息不覆盖，不新增质量/批准节点或自动权限。
- 最终legacy action requireAdmin保留原username规则与Controller update边界；本轮新增native收件/展示/byId查询资格明确有query，并未重定义待确定的可配置批准人政策。

证据核实：7个原归档JUnit XML逐rawbytes/SHA和XML tests/failures/errors/skipped匹配：102/7全部0，CLI exit0，01:57:53。timeline最终2个归档XML同样匹配：43/2全部0，CLI exit0，02:07:41。两个实际log rawSHA也匹配；最后43包含前批重执行，不能102+43冒独立总数。最后仅native timeline结果source改动已由43/2必要相关回归覆盖。

本审查接受真实Spring/H2消息/请求/Gxp事务、isolated账号/模板读取端口与HTTP/registry混合分页测试的限定证据；不宣称实际MySQL模板升级、站内信真实页面、真实账号BPM或E2E已验收。Root负责实际升级、包与真实主流程验证。

最终重点source pin（其余见19文件manifest）：
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/projectcode/productcreate/DccProjectProductActorSupport.java · 3822 bytes · SHA256 `b916e03ed938a8bd0a4727f06035261cd6b94293091c95c712d7ec3974e1a345`
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/projectcode/productcreate/DccProjectProductNotificationService.java · 3711 bytes · SHA256 `82d437e2cfb791b6b10afde31065e6f989d134a77874d91e4d26bf2e557bbd61`
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/approval/DccProjectProductTaskDelegate.java · 9536 bytes · SHA256 `9e94364263717e4f07196888e7374383caca389a8bbc73e84aa2c4865dc569a5`
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/projectcode/productcreate/DccProjectProductCreateServiceImpl.java · 19745 bytes · SHA256 `4c2c2ab471841e06cc0a917f0f61f2d0dcc2c6c72f8230f9745e1683e8438109`
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/projectcode/productcreate/DccProjectProductCreateStateService.java · 5032 bytes · SHA256 `7a5bc8690d8a551d30a46089b37b34b7ceab41bc761d903d1b14d8e35e398de5`
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/approval/DccApprovalTaskAdapter.java · 45017 bytes · SHA256 `cda7b9dca74c24d0d90fd2d906201bb1ceb0a5272a949e475952943c212f3677`

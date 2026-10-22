# G67 当前关联整改与签名UI合同（只读）

2026-10-06；Root唯一实际UI/DB/runtime。没有Java/FE/test修改、Maven、接口/DB调用。以下为4041 ACTIVE、已保存4042、目标4028的具体执行前合同，不重新审全部27AC。

## 实际操作路线与当前阻断

1. 正式受控目录选4041，在管理详情“文件关联→当前关联（最新受控版本）”中编辑/选择4028，保存与确认。`DetailRelationsPanel.vue:14`→`DccFileRelations.vue`→`replaceCurrentRelations`→PUT `/dcc/file-relations/{sourceFileId}`。实际命令含所选fileIDs、旧Master集合/rowVersion、key/reason；服务器currentset CAS及名称权限不绕。
2. 仍通过正常详情“发起局部/换版变更”，选已有4042作为正文、REPLACEMENT B/1，属性按已保存CE来源回显（不是重新覆盖当前OTHER/Y），明确培训/日期/部门→两确认。新candidate/BPM从实际DOM取得，不猜ID/复用4041任务ID。
3. 真实部门负责人待办进入MATRIX_REVIEW指派弹框，REVISION才显示“整改/关联文件/审阅版本/负责人/整改期限”。选择本次审批快照中的4028相关Master、实际admin1作为整改责任人、明确秒精度期限；同时真实指派签名。被指派人另签会签→批准→可选培训→文控审核→受控。保存安排不会发消息，CONTROLLED提交后才唯一发；未来生效不延迟通知。

**上面第2→3步当前不能保证可达。** `DccControlledFileRelatedFileServiceImpl#replaceCurrentRelations:86`只写currentset；`inheritRelatedFiles:197`只复制`dcc_controlled_file_related_file`旧审批行，不读取currentset。4041现在新增关联不会改已存4042审批快照，候选由`RevisionService#freezeCandidate:309`继承4042旧快照；若4042原来无4028，指派界面仍无它。重新从受控版检入现在也沿这个只复制旧审批行的方法。本次已报Root，等Root真实二确认后观察candidate frozen rows/指派界面复现；不手补SQL、改历史、松冻结守卫。

最小待核修复边界：Given受控版的authoritativecurrentset经合法UI修改而旧working/历史快照仍冻结，When首次冻结新的revision申请，Then在同租户/同Master/锁定基线/currentset版本下冻结本轮当前所选stableMaster及当时latest受控目标；旧working已保存正文/属性与旧申请关系不回写。确切采样点须由Root在真实缺口确认后定scope，不能猜“所有检入都重建”、清空历史或lazy GET同步。若存在正式本次关系选择合同，应优先使用其明确选择，不无条件覆盖已有本次选择；当前iteration提交VO并无新增关系选择入口。

## 收件/任务的正式现有合同

`DccRelationRemediationService#saveArrangements:44`：源file+实际BPM轮次+relatedMaster/recipient/due，真实会签参与者、目标项目当前EDITOR/OWNER、启用账号。Rootadmin1也须该正式目标权限，不从申请人文本猜负责人。

`recordControlled:78`仅本轮所选arrangement生成任务及outbox，稳定键`dcc-remediation:<tenant>:<sourceFile>:<round>:<relatedMaster>`。`NotificationDispatcher#dispatch:28`提交后REQUIRES_NEW，官方sender返回真实messageID才SENT，失败FAILED可正式重放，不修改已受控事实。

`DccRelationPlatformNotificationSender#send:24`官方templateCode=`dcc_relation_remediation`，params精确为fileNumber/versionNo/dueAt(`yyyy-MM-dd HH:mm:ss`)/relatedMasterId(string)/sourceControlledFileId(string)/detailUrl=`/dcc/controlled-file/detail/<sourceID>?viewer=1&from=notification`。无Flowable taskId、QuickReview/sourceTaskType；通知查看的是主受控源，relatedMaster是要整改的另一文档身份，不能当fileID跳转或猜名称。

当前`notifyMessageNavigation#resolveDccPublicationTarget:173`只认followupUrl，现其他分支只认项目/培训typedmarker，因此旧sender的detailUrl未被识别。Root已授权FE在既有消息resolver/详情中按可信server templateCode+正Long tuple+sameOrigin/exact path/sourceID/两唯一query接原源只读入口，不改BE sender或新建平台；我未改FE，需FE当前交付/实际消息点击再闭合。没有正文/项目权限仍由现详情服务明确拒，通知不是权限授予。

后端已有GET `/dcc/relation-remediation/my-tasks`，`DccRelationRemediationController:23`只取当前用户；service:23按tenant/assignee过滤并dueAt/id排序，未过滤status。DTO只有string Long的id/sourceFile/relatedMaster/assignee、LocalDateTime dueAt、task status；**当前没有前端wrapper/caller/工作台区，也非统一DCC provider/Flowable task。** Root本轮收敛仅消息source只读入口，暂停新队列/metadataDTO；报告不能声称现已可打开整改待办。其dueAt未显式JsonFormat，不猜实际响应形状；消息deadline本身已有确定字符串。

## 4028旧共享投影维护（条件，不猜实库）

关系选择/受控通知用正式DCC latest解析，不依赖共享ref已ACTIVE；不因此强制提前修目标。将来真的对4028升版，`Adapter.recordSubmitted`→SharedCore.createDccCandidateRef会拒任何旧openCandidate；若Root只读发现4028仍FINALIZING/open1而DCC已ACTIVE，须先使用现“更多→校验生命周期投影”预览/确认维护，而非GET同步/改单表。

`DccLifecycleProjectionRepairService#prove:128`要求exact tenant/Master/project/file、原UPLOAD/REVISION已结束批准BPM、真实控制/发布日期/激活时间、真实盖章/发布原件、完整同轮ASSIGN/APPROVE及正式验签、历史完成任务、exact CONTROLLED/ACTIVATED事件与唯一原ref。维护只允许合法FINALIZING前像，重读hash/key/reason并同事务Gxp记当前维护时间，不造旧签名或时间。当前真实enabled doc_control+category:manage/query/update+hard file scope才可操作；910328训练资格不自动具备维护权限。

## 第二真实签名账号资格

`SignoffAssignmentService#assignmentContext:38/#assign:~98`：启用同租户账号、有岗位；他人必须属于本次冻结部门（负责人自己例外）。910328若是dept103，不能直接指派到dept100；应选择合法的对应部门/其真实负责人，不偷偷改共享矩阵或回填义务。

`RouteReadinessService#requireReadyParticipants:143`：MATRIX_REVIEW需`dcc:controlled-file:review`，有效电子签名授权、非有效锁定、真实ACTIVE签名图片且存储验真VALID；办理还需实际任务/本业务hard scope和已有类别资格，查看中心需其既有bpm读权。910328仅TRAINING_RECORD和文控培训职责不自动等于会签资格。

正式入口：系统用户页面配置任务自有启用账号/正确部门与岗位/实际角色；电子签名→授权管理（操作者signature:manage+electronic_signature_admin）启用并留原因；本人登录且具现路由`signature-governance:policy:query`读取资格→“我的签名”`/signature-governance/my-signature`上传本人真实PNG/JPEG并启用/留原因，后台`ElectronicSignatureAuthorizationController:75/84`固定当前本人ID、ImageService验证实际图片/hash。不得生成签名图、复用admin签名冒他人、缺岗位/权限时mock通过。只有这些条件真的齐备，才可证明“指派他人收任务、独立会签”和两合法账号检出竞争。


## Later actual reproduction and finite source repair

Root confirmed current4041 contains4028 while selected4042/submitted4043 frozen snapshots0 and normal signed-assignment table empty. This supersedes the earlier prepared-only risk. Root withdrew4043 through the formal page; its history remains unchanged. New formal revision freeze-current method now fixes the new-candidate path and passed meaningful RED→final55/5. Public iteration has no explicit relationship selection field; new formal candidates deliberately freeze current baseline relations, not conditionally fill an empty inherited list. Old submitted/working audit snapshots and saved CE attributes are preserved. See g67-current-relations-freeze-bdd.md and pinned receipt. Root owns actual corrected B1 resubmission/notification acceptance; no old4043 repair or direct business SQL.

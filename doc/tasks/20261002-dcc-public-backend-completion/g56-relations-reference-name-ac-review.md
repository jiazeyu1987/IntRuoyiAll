# G56 关联、引用、名称与共同任务边界独立AC矩阵

Status: ready_for_closeout — 2026-10-05 15:57 +08:00最后有限复核冻结。AC-13 R01源级67/6、R02 BE39/3+FE90及named-import r2通过源码/接口审查；R03含关联提交顺序源码1/test2及257/6已闭合，新包真实重试待Root。基线按Root提供main/int_qms HEAD8090e7193，G55空关联同admin主链仍有效但不覆盖非空。此Agent本轮仅只读/doc，无源/测试/构建/实际DB/UI/服务/Git。

## AC矩阵

| AC | 当前正式实现及调用链 | 当前结论与实际证据边界 | 最小下一步 |
|---|---|---|---|
| AC-07 指派自己/其他人并分别签名 | DetailSignoffAssignment→正式context→assign-signoff；当前Task定义ID/tenant/三个key明确，部门leader、当前义务、候选岗位与签名校验，签名后保存义务并setAssignee。Flowable TASK_ASSIGNED事件有正式监听。 | 自己指派、同admin两部门义务独立ASSIGN/APPROVE已有G55实际签名406–409；不同被指定人收件和会签仍未真实验，不冒多用户PASS。代码入口已接通。 | 真实不同账号的合法指派→被指定人任务→签名一组。 |
| AC-13 部分关联负责人/期限，受控后通知 | 仅REVISION指派才读取/保存安排；signed assign同事务保存安排。Lifecycle CONTROLLED同步MANDATORY consumer→实际成功事件→任务/outbox；只遍历选定安排，afterCommit才发正式幂等站内消息。 | 最初R01首次NULLround读取循环已被本轮有限Policy源码修复，原业务RED→11 GREEN→67/6相关回归全0，未松write或回填轮次。无安排的G55 NEW受控仍不证明整改通知。 | Root真实首次升版弹框/部分选择，受控才通知及中断零通知验收，不能以源码GREEN称端到端PASS。 |
| AC-14 跨项目当前最新/历史固定 | selector/current最新与history固定ID/快照、独立正文守卫已实现；来源bool/阶段显示及R03 WORKING合法窗口冻结关联→pending/BPM同事务已通过有限源码/组合审查。 | 旧14:45实际含关联NEW提交Frozen拒/File0保留；后继R03 257/6全0源级关闭，**新包非空关系真实重试未完成**。G55空集成功不能代替跨项目目标新版current/history切换。 | Root新包真实含关联提交、再跨项目目标新版/待生效与原历史正文验收。 |
| AC-16 当前目标项目leader引用、橙色、按项目计数 | ProjectBrowserState.canReference取正式projectLeaderUserId与启用当前账号；createBatch/create在project/folder/Master锁下调用正式ProjectLeaderService，无admin/OWNER替代leader；固定所选受控ID，不复制正文。COUNT(DISTINCT project_id)。 | 正式FE/API/service/审计事务已接通，源名及引用入口都有颜色/文字/计数；仅负责人规则在服务端再次校验。尚无本轮真实多项目/非leader/同项目两folder引用证据。 | 两项目负责人分别引用、同项目两folder计数仍1，非leader前后端拒绝。引用是否跟最新版是HTML D10未定；当前固定版本不能当“关联最新”缺陷。 |
| AC-17 取消引用/源色恢复 | FE二确认与当前project/folder/referenceId；cancel再核leader、锁Master和expectedReferenceId，删除仅该引用，真实Gxp同事务。source计数使用当前distinctprojects，badge颜色由count>0，changed后refreshUsage。 | 最后项目/全局引用解除的计数/颜色源级闭环；无文件、版本、签名、关联删除。正式事务及冲突测试有现有资产，未以其总数冒真实页面PASS。 | 实际单folder、同项目最后一个、全局最后一个取消，以及非leader拒绝。 |
| AC-21 完整名称精确匹配 | 正常NEW从真实source ticket取得sourceOriginalFileName→claimNewSubmission；UTF8 byte[]/VARBINARY source_name_key、tenant全局唯一与统一reservation，normalize返回原名，不lowercase/去后缀。改项目、folder、编号不改变同名键。 | 代码及正式forward SQL有真正binary唯一约束；G55源名完整保存但只单个新名字，不能称SOP四种组合已验。历史已核占用不由客户端布尔跳过。 | 真实SOP.pdf重复跨项目拒，sop.pdf/SOP.PDF/SOP.docx成功；无源正文权限不泄露其他项目。 |
| AC-24 作废20年占名/编号，自身版本不阻 | 独立作废及新版本生效自动作废在原事务调用DccObsoleteRetentionService.retainedUntil(actualObsoleteAt.plusYears(20))→NameClaim retain；立即release明确拒。编号按正式project/type/number身份、完整名binary占用；自己的真实EXISTING_MASTER_VERSION资格与NEW分开。 | 作废/自动作废调用、deadline和未过期守卫已接通；当前G55仅INITIAL受控，没有实际作废后新链冲突验收。期满处置/自动删除不在已确认合同，不为此擅自重写Master unique或历史。 | 独立作废后两种新链冲突、同Master合法后续版本；生效自动作废同样核20年，实际只读核截止，不造历史。 |

## R01：首次升版安排读取的实际源链缺口（P1）

`DccControlledFileWorkflowServiceImpl#persistDepartmentTaskAssigneeSnapshots`1180–1218为首次提交冻结部门义务时没有processInstanceId；`DccWorkflowSignoffAssignmentService#obligation`允许NULLround但要求实际task-local obligation与文件/tenant对应，合法leader因此可取得canAssign。仅在signed `assign`130–134写真实round。

`DccRelationAccessPolicyImpl#requireParticipant`27–34却必须义务processInstanceId==传入round，首次NULL义务永远不匹配；`RemediationService#listArrangements`39调用该read权限。`DetailSignoffAssignment.vue`100–104在首次REVISION指派之前Promise.all安排读取，错误保readyfalse，saveAssignment134不能POST。这个结论来自当前正式写入/读资格/FE顺序，不从不存在的REVISION-only旧guard或单条字符串推断；尚未本Agent真实页面复现。

既有`DccWorkflowSignedArrangementTransactionTest`83–85直接插入已带round的测试义务，因此其实际保存/签名/晚失败回滚证据无法覆盖首次NULLround读取窗口。Root已确认此缺口，授权范围仅Policy与必要测试，不注入Signoff/remediation形成循环、不放松write、不用admin/角色或File旧key、不回填历史。

## R02：预受控空当前关联的阶段显示（P2）

`RelatedFileServiceImpl#getCurrentRelationView`68–70要求当前set存在；唯一初始化来自`RemediationService#recordControlled`119–125的真实最新受控事件。未受控NEW没有当前set属正常生命周期事实，`DetailRelationsPanel.vue#load`100无条件读取current而显示CURRENT_SET_NOT_INITIALIZED；独立history上下文仍建立。G54真实前置页面已出现此错误，G55受控后为空列表正常不代表新NEW阶段缺口关闭。最小修复为明确未有受控当前来源时显示阶段说明/审批历史，不GET懒初始化或catch业务拒绝伪空成功。

## 通知、生命周期与调度边界

`DccControlledFileLifecycleService#control`先保存control时间及latestControlled指针、记录真实CONTROLLED生命周期事实，再同步emit；`DccRelationControlledEventConsumer`为唯一MANDATORY事件桥。`recordControlled`核tenant/file/Master/round/time及真实lifecycle事件后插入受控event、所选安排任务与outbox。源正式受控含待生效即可通知，未等待未来生效日；检入和失败/撤回无CONTROLLED事件，不创建此次通知。current relation set更新只允许该event仍为Master最新受控，历史事件不能倒退current。

提交后scheduler、dispatcher、正式NotifySendApi有固定业务幂等键和平台消息ID核验；发送失败保PENDING/FAILED并有文控受权重试接口，未把已提交受控伪装失败或送成功。当前链由afterCommit触发，不依赖扫描timer；未选择者零task/outbox。该链仍需受控前/中断/受控后的真实账号消息验收。HTML D08把“整改任务完成以关联文件新版受控为准”列为建议；当前service只生成/读取PENDING任务，没有目标新版受控后更新任务完成状态的正式caller，不能声称该建议已实现，也不在本次AC13通知检查中擅自新增政策或闭环平台。

未来生效的每分钟Job handler与注册SQL存在，开启状态属运行配置，本次不访问DB/调度器，不从源@Component声称当前已每分钟执行。它在实际生效事务里作废旧版并调用20年保留，关联current始终按latestControlled区分；G55同日立即生效不能替代未来日期Job验收。

## 培训与独立作废的共同权限边界

培训按HTML flow09e已是文控线下文件上传，不要求逐人学习。FE详情按钮以当前状态needTraining及server actionProjection决定；正式validateTrainingRecordUpload/uploadTrainingRecord1484–1555核当前file/category/tenant、doc_control、类别APPROVE、未发布、准确session包含file/真实BPM轮次并核process定义及businessKey。原件票据绑定、状态到DOC_CONTROL_REVIEW、触发真实TRAINING receiveTask同事务，失败不能报告完成。v4 BPM TRAINING receiveTask后到文控审核；旧逐人Training API不自动加入新native主线。本轮已验无培训分支，线下文件真实上传及权限不足拒绝仍缺E2E。

独立作废的BPM属于同file的另一round，不能套file原上传key：public readiness/approve/reject均先判断真实obsolete task及process变量objectId，再进入Signoff的正式obsolete guard；指派context已按exact任务definitionId/tenant核三keys，作废不读整改、不选文件负责人。批准最终效果核独立round的全部ASSIGN/APPROVE签名及完成task，通过则作废并结束，没有培训/文控/受控新节点。原UPLOAD/REVISION仍按原native轮次、tasktenant/actor/status守卫。本次未发现这些当前入口新的确定代码冲突；培训及作废完整真实页面尚未由G55覆盖。

## 读时重点指纹与后续边界

Policy `b437323ea091b8730b1f49241260956307f39fa870f84f1270dd20fc5ce56aee`；Signoff `cb6f0010cb5f4fd60946071f5f4100f18c6fc798628dff13a1b21c78f3799754`；Workflow `fb96ffa000189edf44a38f3788e6d6d95e40b85234d0b653933e513959baf626`；Remediation `7ab5cb9f94d8c93104b6759aa4548f8f5c008964d293e0aefc2d6b0a46e3fa55`；RelatedFileService `04d9df75b808f1e39a90e6c5efc48bd3c3d45dcd16c394297e01d28735f44d10`；ReferenceService `37668a9af33bb69d2525b2f64b705a6b51027833f1780205c46d066965b2a7a3`；NameClaimService `909e6a7f6086f61c8aa98c5928ff660d519eca484a9fb6b01d997169a559fa99`。

最初只读审查仅把R01/R02列为确定实现差异；其它AC为正式实现与尚缺真实验收区别，不拿历史review未实现、测试总数或G55单admin成功扩大结论。随后Root有限授权R01修复，当前已源级关闭，准确后继证据如下；R02及真实AC验收另由Root分派。

## 后继授权修复 — 2026-10-05 14:21:54 +08:00

R01已完成源级关闭：唯一Policy增初始READ资格，已bound历史协议保留，write assertCanArrange未改；初始NULLround必须真实file/当前round/PENDING_MATRIX_REVIEW、同tenant启用actor、活跃MATRIX_REVIEW真实REVISION定义及task-local精确义务，并经过正式Bpm.validateTask。没有GET回填、admin/旧Filekey/unsigned写旁路。

有效RED是旧正式业务FORBIDDEN（1test/1error），不是setup失败；专属11及最终67/6全0，原六XML和log已封存，不等于真实UI或完整AC13已验收。当前Policy SHA `eddb9dbfef40aec2f18b45c0a70632d7c1a09e88b297890daf1237985309ee9e`；新test SHA `dff18184f42ebab8ae76c7b9d8333d7a31bbc35523cdd5e68eb0475f65e7ba3c`。先前指纹/缺口段为读时事实，最新以此段为准；R02保持未修。

## 后继接口与新实际阻断 — 2026-10-05 15:14 +08:00

R02当前来源布尔/预受控history呈现已按正式BE2源1test manifest b77f0bf…c4fcf和FE7源manifest c13204fc…cba407冻结，39/3及90定向记录核匹配；源码初始显示缺口关闭，真实新页面尚待Root。native培训receiveTask与ASSIGN职责两途中问题亦已关闭，Folder后台权限和viewer exact只读入口未放宽。详见`g56-independent-interface-integration-review.md`最终段，旧“R02未修”只作历史。

新增R03是Root真实含1关联/培训上传实际POST后File0，14:45:37 Frozen异常，与R02只读显示或R01整改读取不同。正常submit把候选提前pending再冻结关系，违反RelatedService正确只许未冻结目标写历史快照的守卫。Workflow Owner已受权修顺序而不松guard，当前不finalpin/不冒源码PASS或端到端PASS。因该新增实际证据，早先“非空关联提交链已接通”推导已失效；G55空集成功证据保持，其他已实现部分不一并重开。

## 15:57 后继冻结源码结果

上述动态R03已完成源级闭合，唯一Workflow SHA `30d6a320a5eedb858e410dc0663ba8e7dd1ae17c4845d704f8e103faee0c0a4a`、manifest `faf64853f2c9b38e800946e9197ba1739083d83c8184f953a730b78f7643c095`，257/6原XML全0及公共H2/Flowable5case已核。正式NEW/withoutApproval均在合法WORKING窗口冻结关系/票据，然后pending/原FINALIZING；历史Frozen守卫、initial/revision原继承和same-key前置返回保留，晚失败全事务回滚。新包真实页面验收仍未由这些离线证据完成。

FE父源码缺named import已有限r2补齐，当前父SHA `6d2a11b8506f81959ce1fc73f1ca44551fa7ca96dd86377bc209936050081d30`；r2 manifest `e4e3eeda96e4fd73ab4f9d432a296ace7edede429dc485b6feff8d3548282ea2`，其它15原pin不变。旧完整types10656 exit2/旧build23344 exit0与新r2定向2/lint0分开，新完整types/build结果由Root另报。精确源码/测试/证据界限见`g56-independent-interface-integration-review.md`最终段，不再次扩大到完整HTML已验。

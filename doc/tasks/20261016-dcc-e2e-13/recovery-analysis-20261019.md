# E2E-13 2026-10-19 原O辅助区恢复分析

状态：ANALYZED_PENDING_ROOT_FILE_OWNERSHIP_AND_RUNTIME_READY。分支int_qms；尚未新跑浏览器、Maven、构建，未修改生产或直接测试。既有两分支核心Root接受状态不撤回。

## 证据与口径纠正

已读恢复分派recovery-dispatch-20261019.md、e2e13-root-review.json的boundary/scope、原任务run manifest和final-result；原线程manifest的PASS_PENDING_ROOT_REVIEW早于Root接受，以Root review为准。原文件2054545668044084222/A1，BPM fb0c1653-ca6f-11f1-9342-b082e25ec548，O910463，批准签名860；禁止重建重签。

旧报告将辅助错误统称历史审批权限拒绝，粒度不足。原运行日志output/runtime/int_qms/logs/dcc24-trainingmetadata/application.log在2026-10-18 07:42:40.436明确记录DccControlledFileQueryServiceImpl.assertRelationNameVisible（当时行1471），不是requireBodyRead。原最终DOM同时保留owner/signature可读及Current user cannot access this controlled file、请先选择并核验正式申请轮次。

## 根因一：申请/关联证据的源文件名称元数据规则不一致

链路：DetailApplicationHistory.vue selectRound -> applicationRead.ts getControlledFileApplicationEvidence -> Controller getApplicationEvidence -> Query getApplicationEvidence -> relationStageSnapshotService.readRoundEvidence -> access.assertNameVisible(actor,fileId) -> Query.assertRelationNameVisible -> canViewFileName -> canAccessQuery。

Query.getApplicationEvidence先按canAccessDetail校验；Guard.isAllowed包含offlineTraining.canReadProgress，O当前确认职责因此通过。随后源文件名称检查走canAccessQuery，其培训特例只有isNativeWaitingFile && canUpload(actor,file)，仅U满足；O不是历史审批参与人，等待态又不属于当前受控VIEW浏览范围，故拒绝。当前O类别显式USER VIEW本身不能改变该等待态规则。readRoundEvidence在源文件名称处就退出，不是证据不存在，也不是正文二进制拒绝。前端selectRound清空selection后，DetailRelationsPanel因applicationSelection=null继续显示“请先选择并核验正式申请轮次”，是上游失败结果。

最小提案：在Query.assertRelationNameVisible对所选文件补同现有职责metadata一致的窄准入：当前原生等待培训 + 本轮确认O的canOrganizeTraining（内部requireWaiting校验正式活动receive-task）+ 当前任务硬文件范围；保留正式名称规则及所有其他文件/历史版本拒绝。只授该文件名称元数据，不改canViewFileName/global browse，不跳过关联成员各自名称权限，不替代正文鉴权。该方法由多个关联读取调用，因此直接测试必须证明非O/错轮次/已结束职责/越scope/其他关联文件不继承本例准入。

拟改文件：DccControlledFileQueryServiceImpl.java的assertRelationNameVisible（本轮22独占，必须Root协调后才能实施）；DccW08ApplicationRelationEvidenceTest.java或现有QueryServiceTest的真实Query+真实Guard/Offline职责装配定向用例；必要时DccRelationFormalQueryGuardTest.java验证桥接不扩大其他文件权限。不在SelectedDetailReadService绕开Query/关联权限，不在前端吞掉申请证据错误。

## 根因二：正文入口把培训职责上下文推成审批历史

后端DccOfflineTrainingRecordService.canReadTrainingBody=canOrganizeTraining：只允许当前live培训阶段的确认负责人。DccControlledFileDetailAuthorizationGuard.requireBodyRead(TRAINING_APPLICATION)还要求精确文件/版本、正式绑定round、当前tenant/有效账号/硬file scope以及可读项目。APPROVAL_HISTORY则要求原发起者或本人历史task。两条规则有意独立；不能令O成为历史审批人或用元数据可读替代项目/body授权。

前端detail/index.vue resolveDetailBodyReadContext优先处理route.query.processInstanceId，无task就选APPROVAL_HISTORY；原O工作台链接恰含该参数。后面的TRAINING_APPLICATION分支只看canUploadApplicantTrainingRecord，而O没有上传职责，且该分支在路由分支之后。因此已有培训正文规则在原O入口不可达。Query.canReadBinary等待态已使用canReadTrainingBody投影，适合以前端既有PREVIEW放行事实为准，不能仅canReadTrainingApplicationProgress就推断二进制权限。

最小提案：在detail/index.vue的resolveDetailBodyReadContext，对当前原生等待培训、无task、已核验scope与原文件当前BPM完全一致的O，使用TRAINING_APPLICATION；以正式PREVIEW动作放行及当前owner身份/同round为条件，保留显式bodyReadContext优先且不覆盖真实任务/任意历史round。服务端仍独立requireBodyRead；无可读项目或不满足职责时应明确拒绝，而非补项目权限。前端不把培训upload资格当正文资格。无需新增API或修改SelectedDetailReadService。

拟改文件：IntRuoyiFronted/src/views/dcc/controlled-file/detail/index.vue中的resolveDetailBodyReadContext；现有IntRuoyiFronted/tests/unit/dcc-g07-body-read-context.test.cjs补当前O原route/无upload/同round为TRAINING_APPLICATION及错误round/task/已完成职责不转用的直接行为测试。既有后端Guard g07_trainingHasItsOwnBoundRoundGrantAndCannotBeUsedForDailyRead作为不放宽正文边界的定向回归，Maven由Root执行。

## BDD / 直接RED与GREEN设计

Given 原4222/A1仍等待培训、O是同round签名确认负责人且当前职责有效，O没有TRAINING_RECORD上传职责。
When O从原工作台打开同file/BPM，并查看本轮冻结申请证据和正文入口。
Then 源文件名称/本轮申请属性及节点证据应按职责元数据授权读取，关联成员仍逐个检查名称；上传按钮仍不可见。正文只有精确TRAINING_APPLICATION及服务端project/file/body授权全部满足时才可读。

Given 同O使用DAILY/APPROVAL_HISTORY、错误round/task/文件/版本，或职责结束/越项目或硬文件范围。
When 读取正文或借该职责查看其他关联文件。
Then 保留明确拒绝；不得由metadata、负责人文本或通用VIEW补齐权限。

RED计划：先仅新增Query源名称真实行为回归与前端resolveDetailBodyReadContext实际函数测试；前者Root跑Maven（预期源名称访问拒绝），后者定向Node（预期APPROVAL_HISTORY != TRAINING_APPLICATION）。有效RED后再实施对应最小修复。没有声明当前新RED/GREEN已经执行。

## 运行复验边界

等待Root READY及生产归属安排。只用O910463的新独立context从原工作台点击4222，核对受影响申请/关联区和正文实际正式预览；记录精确file/version/round、辅助可见DOM、自然请求状态、截图trace及正文上下文。不得使用API/DB业务动作，不重跑已PASS批准/签名/U上传dialog。若O正式项目/body范围不足，正文拒绝是正确结果，准确记录依据，不改权限配置。原两单和全部任务资产继续保留。

## Root归属确认后的实际执行

- Root明确本线程唯一修改DccW08ApplicationRelationEvidenceTest.java；Query保持22独占，不改QueryServiceTest。前端仅detail/index.vue的resolveDetailBodyReadContext及dcc-g07-body-read-context.test.cjs。
- Root READY：frontend8061/PID20976，backend48061/PID25124，120105 Jar SHA ba0a8ead6e8e85cf29a8e339bbbc61a93ec30b3bb2adaf876d2736ff15f006b3。dispatch与reboot1019-runtime-proof.json已核对；此包不含本轮Query候选修复。
- O910463独立context原入口诊断：4222/A1元数据仍可读；申请区Current user cannot access this controlled file，关联区请先选择并核验正式申请轮次。页面“预览受控文件”按钮数量0，故没有实际点击正文读取，也不将静态上下文缺陷冒充本轮正文网络拒绝。业务写请求0，browser正常关闭，原单和权限未改。第一次截图过早停在轮次核验中已单独留档initial-pending-observation.json，不作为辅助区最终结果。
- 前端有效行为RED：14tests/13pass/1fail，exit1，实际APPROVAL_HISTORY != TRAINING_APPLICATION；没有缺依赖或测试装配错误。随后仅改指定函数：当前等待培训、无task、同owner/file/owner签名及round、scope轮次准确和正式PREVIEW放行才选择TRAINING_APPLICATION；所有显式bodyReadContext、任务/历史及服务端鉴权保持。定向GREEN14/14 exit0，日志runs/20261019-recovery/frontend-green.log。新负例断言仅接受明确身份校验错误，不吞其他异常。
- 后端RED测试currentTrainingOwnerReadsApplicationEvidenceThroughRealMetadataNamePolicies已准备：真实Query、Guard、Offline方法（含真实requireWaiting、版本策略）及桥接；外部账号/DB/BPM端口mock，stage reader端口回调真实桥接名称检查。不是DB/BPM集成测试。Root未给有效Maven RED前，不宣称后端RED成立。
- 交接recovery-red-handoff-20261019.json；Query候选patch仅存doc目录，未应用生产。下一步Root串行Maven RED和Query归属安排；收到新包确认后仅原O受影响分区复验，不整体重跑已通过用例。

## 有效后端RED后生产授权与补充BDD

Root实跑e2e13-training-evidence-name-red：1test/0failure/1error，真实Query -> 关联桥接assertRelationNameVisible返回1080000012；不是编译/夹具异常。22两投影GREEN后冻结并释放Query，Root仅授权本线程追加该名称方法的窄准入，其他字节保持。

Given 当前确认O和真实有效等待receive-task；When 名称/申请证据读取；Then 本文件源名称元数据可读，但不是upload/browse/body授权。
Given 非负责人、错owner round、职责结束、无签名、越硬scope或其他关联文件；When 名称或证据读取；Then 明确拒绝，不能把本文件职责复制到关联成员。
Given 确认负责人但真实requireWaiting读取到错误执行、inactive绑定、缺失receive-task或正式文控证据校验失败；When 源名称读取；Then 原异常传播，不能吞掉、按普通VIEW重试或返回成功。新增测试调用真实Offline requireWaiting，仅BPM/DB/账号外部端口mock。

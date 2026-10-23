# CC-2：A/D交付Review后的接续合同

日期2026-10-01，依据需求v1.3、IC-1及现存共同HEAD a801dc8。本文是主管理对下一批实现的技术合同，不是已实现或运行验收结论。沿用现有worktree和任务，禁止重建/reset，未授予Git/实库/服务/E2E权限。

## 1. 已确认的整合事实

A的整改安排规范化签名和同事务saveArrangements已实现；D旧签名变载荷负向测试在整合后通过。A直接recordControlled与D同步listener同时存在时真实复现一次受控两次调度；主管理修正为A持久化正式事件后同步publish，由D唯一listener保存必需事实，提交后才投递。D失败仍回滚A，不使用AFTER_COMMIT保存必需事实，不再要求A另调recordControlled。

D项目引用写事务已先锁project→folder→sorted Master→exact selected File，委托B唯一leader/目录授权。create/cancel不能被外层Master锁倒置；所选版固定且当前锁读，作废不可新引用，同项目多目录计数1，历史latest晚到事件不倒退当前关系。保留正式source名称权限和正文权限分离。

Root整合定向回归：DCC985＋BPM37＝1022项0失败/错误/跳过；包含A/D、B实际服务与H2/内存Flowable。31项A前端＋53项D前端＝84项离线行为/编译验证。不是真实页面E2E、MySQL或线上通知验收。

## 2. CC2-B-DRAFT：真实文件草稿属性（B实现，A接线）

未送审fileId是真实草稿身份，不能造BPM字符串或猜round=1。授权B在自身worktree小范围修改Root的DccApplicationRoundService、其专属测试及新增前向迁移/H2fixture，以提供以下合同；不改A Workflow或公共页面。

1. reserveDraft(projectId,type,fileId)：MANDATORY事务，校验当前tenant/正式项目/对应File身份，在正式项目锁内从持久记录分配attributeRound，保存尚未绑定BPM的草稿行并回读。bpm_round为NULL表示真实尚未创建BPM，不保存draft/UUID占位字符串。相同仍开放草稿重复调用返回原轮次；已绑定轮次不能被本入口清空/伪装草稿。
2. requireDraft(type,fileId)：只读取唯一尚未绑定的持久草稿轮次，缺失/歧义准确拒绝，不补建默认。
3. bindReservedDraft(projectId,type,fileId,attributeRound,realBpmId)：在真实BPM已创建的同一申请事务中，将确切已预留草稿绑定真实BPM。相同轮次/同BPM重放回读，不同BPM/跨项目/错动作拒绝，失败整体回滚。原bind保留“未建立可恢复业务草稿的直接送审”合同；遇到开放预留草稿时应要求显式绑定，不另分配轮次绕开。
4. 不重写已交付20261001_dcc_application_round_link.sql；新增前向迁移仅允许bpm_round为空并保留两项原唯一性/历史映射，零历史业务回填。项目锁保障同文件并发reserve唯一；测试首次/重复、并发、已绑定冲突、租户/项目、实际回滚。

B的DccProjectApplicationSnapshotService新增prepareDraft(user,project,type,fileId,actual)，调用reserveDraft→beginDraft，已有来源只回读；用户提供实际值时saveDraft。首次真实File创建事务立即调用，项目默认以后改变不能覆盖该草稿来源。普通保存/恢复默认只用requireDraft或已保存真实BPM映射，不在普通保存补来源。A负责真实文件归属/申请人/可编辑状态和准确送审动作权限，B不新增接受任意fileId的HTTP写入口。

## 3. CC2-B-FORK：跨文件返工属性（B实现，A接线）

B新增forkToNewApplication(user,project,type,sourceFileId,sourceAttributeRound,targetFileId,targetAttributeRound)，复用唯一属性表，不另造快照表。源必须为正式已提交快照，项目/动作/tenant一致；目标必须为本次真实候选及Root持久分配轮次。锁内深复制原defaultSourceJson/actualAttributesJson，目标submitted=false，原记录不改；重复调用只接受同源/同目标事实，冲突拒绝。不得重取今日默认或冒用旧applicationId。

A以旧真实BPM→require取得来源round，创建真实新File/预留草稿后调用fork，后续手改只改目标actual。真实BPM建立时bindReservedDraft，再freeze。验证连续两次返工、项目默认在返工前改变、实际值手改、原作废/上传历史不变及外层晚失败回滚。A跨文件返工实现仍待B合同源码同步，不能继续把已同步普通begin/freeze当完全未实现。

## 4. CC2-C-INITIAL/DATE：首次正式候选与本次预设日期（C实现，A接线）

授权C在自身worktree小范围修改公共SubmitIterationReqVO及既有前端提交类型，登记本次effectiveDate（LocalDate及严格原有日期格式）；不改A Workflow和Root公共大页面。C为createRevision自身幂等载荷补本次日期、projectAttributes、selectedSignoffDepartmentIds，不能仅靠A提交hash保护；数组规范化语义应与A合同一致，变载荷重放拒绝。

C新增createInitialCandidate(userId,selectedIterationId,SubmitIterationReqVO)：新链尚无latestControlled/currentActive，正式锁定并核对tenant/master/项目/完整源名/编号/正文及当前申请人/小版资格。所选A/1-1等小版只冻结为候选正文，生成该链原initial正式版本并record INITIAL；不调用局部/换版算法增加版本，不重命名原小版，不以ID大小猜版本。正式候选清空controlled/activated/distributed/原BPM/旧签名事实，源和关系历史保留。本次effectiveDate由实际申请明确传入并保存，过期日期按正式策略拒绝，不能静默改为今天。

同key同事实返回已有真实候选，日期/属性/部门/所选正文/意图变更拒绝；至少覆盖首次检入后送审、连续检入、已有受控源拒绝、并发/回滚、错租户/权限、原小版及原版本名不变。A收到后在无受控baseline的selected working入口调用此合同，既有受控链继续createRevision，不将当前准确阻断当成功功能。

## 5. CC2-D-SELECTOR：正式候选投影（C实现，Root接页面）

C复用既有browser-page/查询服务，提供D loader实际global/project-folder scope。请求独立projectFolderId，不能塞进NAS directoryId；使用B的dcc_project_file_placement显式身份。服务端租户/名称权限过滤、server total，每Master只正式latestControlledFileId，包含待生效标记及独立canPreview、来源项目/逻辑文件夹展示；前端不去重/假分页。历史未登记位置显示未记录，不按名称/数字猜。现有Query接口和canPreview已经存在，缺的是以上身份/最新定位及组合验明。

Root审批/文控/历史页面读取本file/type/真实BPM映射冻结快照时，须沿正式文件/任务读授权，不能为显示属性新增项目EDIT，也不能借申请人身份读B接口。C提供同一正式详情/任务投影方案供Root接；B的编辑草稿读取继续保留EDITOR/OWNER。缺历史事实明确未记录，不用今日项目默认补齐。

## 6. CC2-B-FOLDER-DELETE：目录删除（B实现，D/Root复核）

采用逻辑删除保留稳定历史身份；二次确认，精确project/folder，正式配置权限及启用账号项目范围。与D写入共享project→folder锁顺序，锁后当前读取子目录、B文件位置与D现存引用。仍有子目录/正式位置/引用则拒绝；删除不改源File/NAS/模板/引用，历史账本和原位置事实可追溯。不能物理删除曾用身份或凭current引用0宣称从未使用。具体是否允许“曾引用后已取消且现在为空”的逻辑删除按上述历史保留实现，无需伪造everUsed；若B已有更严格限制必须在Review明确差异。

覆盖D引用创建先锁时删除拒绝，删除先锁时新引用以B正式inactive/deleted检查拒绝，取消引用与删除不死锁、最后引用取消保源、历史账本不变。D已有lockReferenceContext可直接复用合同，不另建第二套引用计数/授权服务。

## 7. 本批Owner与剩余门禁

A还有可独立继续的A-REV-04：Lifecycle.completeControl/activateDue及ObsoleteService.applyApprovedObsoleteControlledFile写入受控/生效/自动或独立作废事实，仅有领域生命周期事件/作废audit，尚无对应文件状态统一账本append。D的relation.controlled记录的是整改事件，统一签名事件记录的是电子签名，不能替代这些正式文件状态前后。A输出每个精确操作/对象/版本/原因/真实before-after合同给Root登记，复用统一GxpAuditService与同一业务事务；自动生效是明确系统事实/原因，不伪造人类操作者或签名。用真实H2账本回读、缺政策/append失败的状态/指针/20年占用/D事件回滚、重放零重复验证。当前为静态源码Review发现，未执行该缺陷的新RED，不冒充已复现或已修复。

B：原B-REV-01审计修复＋CC2-B-DRAFT/FORK/FOLDER-DELETE；其中只有本文明确的Root round小范围文件例外，其他公共字段交Root。C：INITIAL/DATE及SELECTOR和正式只读投影，接源date公共模型是本批明确例外。A：先继续A-REV-04，收到真实B/C合同后接create/save/submit/rework事务与组合验证。D：接收最新A签名/单一事件消费后复核，独立实现继续保留。Root：公共上传/详情/浏览、培训正式session、受控后下发/提醒及I-01..I-08整合。

产品审核人员和提醒提前量仍需业务决定；不擅填账号或天数。这不阻止上述确定的源码工作。本合同和离线测试通过均不关闭全部业务验收，不执行未经授权的Git/实库/服务/E2E。

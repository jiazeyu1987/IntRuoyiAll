# G56 接口配合有限独立审查

Status: ready_for_closeout — 2026-10-05 15:57 +08:00最后有限复核冻结。R01/R02及F02–F04源码配合、R03冻结顺序修复已通过审查；FE named-import r2最新pin已覆盖旧父源。新包实际含关联上传仍待Root重试，不能以源码/257回归关闭实际验收。此Agent仅读源/hash/已有XML与日志并写两review，无生产/测试/Maven/types/build/DB/UI/服务/Git动作。

## R02 当前受控来源布尔合同

已读`g56-current-controlled-source-contract.md`与当前BE代码，方向符合：`getRelationPermissions`完成原名称授权和真实Master/project/placement身份后，用`hasCurrentControlledRelationSource`单独判定latestControlled指针，不借selected.controlled/canPreview/canEdit作来源资格。当前Query1057–1077：NULL指针false；精确latestFile同tenant/Master/project、未删、controlledTime及正式非WORKING版本完整；ACTIVE/CONTROLLED_PENDING_EFFECTIVE true；完整OBSOLETE且obsoletedTime存在false；dangling/foreign/错误身份/无control/错误版本/其它状态抛异常，没有currentActive或扫链回退。

这是一项只读来源事实，不赋正文/编辑权限，不创建currentset。true应继续尝试原current API，currentset或当前读取异常不得catch成[]；false应仅呈现暂无受控来源，保selectedSource及真实history端口。FE DTO必须严格收到boolean，缺字段/非boolean拒绝，不默认为false。读时`applicationRead.ts`和`DetailRelationsPanel.vue`仍是G54旧源，尚未消费新field；Owner已声明将接入，不能把此开发途中状态记成最终交付缺陷或PASS。

已有BE green日志当时为39 executions中1错误，发生在OBSOLETE夹具的原名称权限守卫“Current user cannot access this controlled file”；本review不运行或修其夹具，也不把正在修正的日志当最终PASS。正式断言与最终XML/指纹待Owner冻结交付。

## F04 native进度及职责：两项需Owner收口的合同风险

当前`native-approval-progress.ts`7–22只在exact file/BPM匹配唯一已核application round时取UPLOAD/REVISION/OBSOLETE。LEGACY只在同原BPM、准确流程定义ID及旧`dcc-controlled-file-approval`定义下成立；缺映射/矛盾明确拒绝。父`loadApprovalDetail`4945–4953保Task列表BPM校验、正式round查询与当前route generation，方向正确，不用File旧upload key推独立作废。

读时尚有两条可证实的接线风险，已交Root协调Owner；不把途中源码判成冻结交付失败：

1. **培训等待/完成显示**：当前helper为培训增加`APPLICANT_TRAINING_RECORD`，但只统计同key用户Task，输入facts也没有trainingRecordFileId。正式v4 BPM培训为`TRAINING` receiveTask，不出现在用户Task列表。故PENDING_APPLICANT_TRAINING_RECORD时会显示0/0且非当前，上传成功进入文控后仍不能显示该阶段完成。建议按准确所选round的正式状态及真实线下记录事实投影，不虚构Task或人员确认节点；旧round事实不能拿当前file最新状态补齐。
2. **指派职责动作取值**：当前`resolveSignatureDuty`比较actionType=`MATRIX_REVIEW_ASSIGN`；实际`SignatureVerificationServiceImpl`保存的actionType是`ASSIGN`，MeaningCode才为`MATRIX_REVIEW_ASSIGN`，Query3757直接返回原actionType。G55实际406/408为ASSIGN，因此当前helper会把负责人指派误标成会签人。现新测试使用MeaningCode样式的actionType，不能证明真实契约。建议用actual ASSIGN/APPROVE（或明确MeaningCode字段）并继续exact taskId/round关联；不可从签名人/admin猜职责。

此处只要求必要状态/真实动作契约测试，不扩进度平台或未确认D08整改完成政策。

## F03 viewer只读档案/版本历史

当前`detail/index.vue#openViewerTraceability`5149–5152核file已加载、same ID及当前route generation，调用既有`buildControlledFileTraceabilityPath(file.id,'viewer',route.fullPath,'trace')`，没有改成latestID或management参数。helper生成traceability/scope只读URL，原管理按钮与正文权限继续独立。当前离线10项folder/trace日志全0仅是Owner有限证据，不是本Agent重跑或真实页面PASS；最终源/manifest待冻结一次复核。

## F02 公共项目文件夹维护

`ProjectBrowserPanel.vue`11–14按钮、100–111实际handler复用`ProjectFolderEditor`，global update权限与当前准确project/folder identity，未用canReference/leader代替维护权限；saved/deleted回调只刷新仍为同项目的目录，切项目不回跳旧项目。正式Controller save/delete都要求`dcc:project-code:update`；原MaintenanceService再核update+ProjectEditorOrOwner、租户、启用项目、project→folder锁、理由/二确认及子文件夹/placement/reference/mapping占用，原Gxp事务不改。当前源未看到绕此后台权限或另建NAS CRUD的路径。

## 读时指纹与验证边界

- Query：`e187d642f9e20784e82d7b0d59e239b0531caecfd2dd1b6feca7529c0fbc9b70`；DccFileRelationPermissions：`44ae09285796203c3d871bef7a3529e37c738e96fa2018d73f2583ee694f1c37`。
- Native helper：`459665472810262aeb35295cf81fc6221f8d86e7120a03196bdbd3c252783404`；ProjectBrowserPanel：`6961595efd717934fb0cd2a21f893cea1146aa8da28217a8a252043215588e64`。
- R02 FE旧panel：`8b2e3373ad72843b738967f13e7e62d8e5dd2e326586022a2a792c3ab922a1dd`；旧metadataAPI：`6fa4714ac2d023ec70ff7cdc4689f9d883c386a5cbbb815310ccb557465f507e`；父detail读时：`34d02f9987415e226d4c107aa00f29741746f7197367e78d0c65c02b4291e65a`。

上述为开发过程中原字节，后继Owner修改会产生新SHA，不推断为formatter或未知无关漂移。代码完成与实际验证分开：G55四方向同admin主链已真实通过；R02、新native进度/职责、F02和F03本批新页面行为仍需Root实际验收，不能用旧G55正文成功替代新入口/新状态验证。待冻结通知后仅补最终闭合段，不重复已绿67或新增宽泛审查。

## 最终冻结闭合 — 2026-10-05 15:14 +08:00

BE `g56-current-source-fingerprints.json` rawSHA `b77f0bf1e59dba86c0fe3cba5a17d47abd4955895574ff05ed1bcfd1e07c4fcf`：2源1test保持所述精确latestControlled合同。3份原XML分别21/15/3、合计39项且fail/error/skip全0，与封存bytes/SHA一致；当前正式NULL/current/pending/完整OBSOLETE/损坏error、不赋正文权限合同源级通过。早期OBSOLETE夹具错误已由准确授权selected分离修正，没放松名称守卫，以14:37:45最终39/3为准。

FE `g56-f02-f04-r02-fingerprints.json` rawSHA `c13204fc2f4d9d27790d3a9c98bed77cc885eb35280a1d2e2564b00112cba407`：7生产/9测试及12原日志全部匹配。与BE三资产及三XML共34项核0漂移。最终15文件90 tests/90 pass/0 fail/0 skip及7源lint收据核实；此Agent没重跑，Vue与网络为显式隔离端口，不称实际E2E。

R02 FE现在严格要求boolean。`DetailRelationsPanel`92–97在正式false时提示无可用受控来源、切真实history页并保selectedSource；不请求current、不造空set、不删除原审批快照。true仍执行准确currentSource、rowVersion、最新目标、内容与edit守卫，错误保留，待生效不转currentActive。

F04两途中风险关闭：helper现用actual ASSIGN/APPROVE/REJECT与sameBPM/taskId/key识别真实职责，未知动作不猜；TRAINING receiveTask无userTask，现按准确scope的needTraining、实际等待状态及正式trainingRecordAvailable/后续状态投影，缺availability不判完成，OBSOLETE仅两阶段。父真实computed/职责网格及syncStageProgress接该helper，相关真实contract/placement RED证据保留，不能再用旧MeaningCode样式fixture误称通过。exactFile/BPM/applicationType/LEGACY边界与late读取守卫保留。

F02公共入口仍复用原FolderEditor与正式update + backend editor/owner及非空占用合同；F03只读viewer档案按成功读取的exact selectedID/context跳trace，没有management授权。F01正文侧栏正式项目与当前执行版本保持独立，不用产品code/name伪装项目。上述当前范围未发现新确定P1，实际新增页面行为待Root部署验收；Root统一types/build在运行，不由本review代替。

最终重点FE SHA：native helper `87436db252c7c93afd7ed0045789b62d27f0da31e0e8e6790f6ef8edf723a904`；父detail `d7051dcec5fbcfdb9622b0b298fe2e087c45efc6d0484f30907324bd161707ea`；RelationPanel `e8a0f8e9225db37262d1c766234bcb3b1ba9482a2d14f9dc62e7f980b2586558`；metadata API `07aff39518252ebaf8be5499af1d7301820186d9d83a350ddc4e84112cba5a9d`。其余见冻结manifest，原记录保留。

## R03真实非空关联提交P1：本次配合通过不能覆盖

Root真实`g56-real-ui-r1/022–031.json`选择1条关联及培训后提交，031自然POSTsubmit虽HTTP200仍未创建File；Root只读核File0，backend-attempt-5/application.log681–682于14:45:37明确`DCC_RELATION_APPROVAL_SNAPSHOT_FROZEN`。031 rawSHA `3316220c0d3c709e46f797a5f2c65860182d9d21053f857c26d0d0b6cfa8a314`；022/029显示所选关联与确认资料，证明这不是仅静态推测。G55空关联提交早return成功不覆盖此非空分支。

旧private submit先insert候选，内部prepareDraft后立即把状态从WORKING改为pending，再调用bind关系；RelatedService严格拒pending/BPM/已受控目标写审批快照，拒绝是保护历史的正确守卫。Owner已在受权Workflow动态实现“WORKING候选阶段冻结关联/票据→pending→BPM同事务”，本review只确认方向，不对尚在RED/GREEN中的源码作最终pin或PASS。legacy createPlatform的1335关联调用同样应先冻结后转换正式状态；Related frozen guard不改。

checked-in initial和REVISION候选路径当前`submitWorkingIteration`委托RevisionService先生成WORKING并inheritRelatedFiles，再claim pending/BPM，原顺序是合法快照窗口。必要组合证据应包含非空关系实际继承、同key重放前置返回不重写pending/历史、晚失败与BPM/关系/票据/placement一同回滚；不把这些用真实fixture替换成宽放guard。Root已独占分派Workflow Owner，R03待其冻结和真实重试；本Agent不改源/测试或运行构建。

## 最终后继R03与FE import r2闭合 — 15:57 +08:00

R03 manifest `g56-linked-upload-order-fingerprints.json` rawSHA `faf64853f2c9b38e800946e9197ba1739083d83c8184f953a730b78f7643c095`，唯一生产Workflow SHA `30d6a320a5eedb858e410dc0663ba8e7dd1ae17c4845d704f8e103faee0c0a4a`，两必要测试。正常native submit1114–1128先插WORKING、完成prepareDraft/关联/票据/附件，再transition pending和冻结route/部门/BPM；withoutApproval1333–1361亦先完成该新候选快照，再回到原READY_TO_PUBLISH或FINALIZING并沿原平台事件/激活端口。新helper只允许尚未受控/生效的WORKING对象，核状态更新rowcount，不用于打开已提交旧历史。原RelatedService SHA `04d9df75b808f1e39a90e6c5efc48bd3c3d45dcd16c394297e01d28735f44d10`不变，十种历史/已冻结目标拒写守卫保留。

正常same-key提交1069–1072在创建前匹配真实payload返回原File；冲突只处理根File insert，关系/票据/BPM异常不能冒并发成功。checked-in initial/revision由原RevisionService在WORKING阶段inherit后再claim pending，未增加重写；其真实持久化继承与源快照不变测试保留。撤回重提test2861–2875精确三次更新：901仅新pending、901仅新BPM、900仅supersededBy901；旧900仍WITHDRAWN/proc-old且不删除，新增次数不掩盖历史覆盖。

6归档XML与source/test/receipt/log全部hash匹配，170+5+50+15+3+14=257 executions全0fail/error/skip，CLI0、15:42:21。公共组合5实际通过，包含Controller Java→同Spring事务H2正式票据/关系/mapping及真实Flowable创建、same-key零重复、票据晚失败和BPM之后审计端口失败全回滚。withoutApproval是原有同tenant受控来源的REVISION fixture、保持FINALIZING，不冒证明另一条legacy NEW默认tenant路径；签名认证、目录授权、物理存储和激活跨端口为明确隔离夹具。首4/5组合及精确更新次数旧失败均保留，以最终原XML为准，不相加成更多独立测试。Root包69419退出0由其报告，5类包字节核/启动/真实重试待其完成，本review不启动服务。

FE r2 manifest `g56-f02-f04-r02-fingerprints-r2.json` rawSHA `e4e3eeda96e4fd73ab4f9d432a296ace7edede429dc485b6feff8d3548282ea2`：父detail只新增`buildControlledFileTraceabilityPath`正式named import，最新父SHA `6d2a11b8506f81959ce1fc73f1ca44551fa7ca96dd86377bc209936050081d30`。其余原15资产不变；16源/测试与2原r2日志均匹配，2 viewer定向行为全0及lint0是原两case的重复必要验证，不与90相加。此前Root完整types10656实际exit2缺named import、旧build23344 exit0仅为旧包记录，不能称新r2全类型已通过；新types64158/build44337由Root运行，结果另列。没有修改类型规则、路由、字段或正文授权。

本次R03/FE r2核34项source/test/原receipt/log/XML bytes/SHA为0漂移，未发现冻结范围新的P1。源码关闭与真实业务验收分开；原14:45含关联提交失败保留，必须Root在已核新包重新通过真实页面，不能借G55空集成功或本组257通过代替。

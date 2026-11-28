# 前三单上传500及0648领料匹配只读诊断

- 委托：82fc3818-60f5-4f18-9dfa-95a9d97f7161；检查日期：2026-11-21（北京时间）。
- 目标：localhost:8081／测试1（tenant_id=186）。本次仅诊断，无业务上传、接口写入、数据库写入、配置权限变更、停止/启动/重启、构建或发布。
- 结果：A为已确认实现/数据库契约不一致，当前未修复；B为已确认冻结输入与本地正式领料编码不一致，尚不能裁决哪方业务定义错误，未改历史。

## A：服务和本次上传异常

1. 8081监听PID128844；48081监听PID121580，复查时相同。后端父进程66852为标准start-branch-backend.ps1 -NoDebug；Java创建时间2026-11-20 23:16:16。运行Jar为仓库output/runtime/int_main/backend-f15673cd24884ce984a1b6174ef3dd6e/yudao-server-exec.jar，local profile，端口48081；不是目标服务未启动，故未再启动或重启。
2. 本人内置浏览器实际读取健康页返回{"status":"UP"}；正常localhost:8081首页显示测试1／管理员／个人工作台。这只证明当前服务和首页可用，不证明附件或放行业务恢复。
3. 直接日志文件的当前输出不足以覆盖本次异常，未用旧日志推断；改为在显式READ ONLY事务中核验既有infra_api_error_log持久化异常记录。只查询目标租户/本轮对象，不发起业务请求，不写库。
4. 日志429—440共12条，均来自/admin-api/mes/pro/process-pool/team-leader/active-order/dossier-files/upload，2026-11-21 10:18:09—10:36:13，覆盖三岗位及全部12正式任务；根异常均为DataIntegrityViolationException，SQLException: Field 'tenant_id' doesn't have a default value。
5. 实际失败SQL为INSERT INTO infra_file (config_id,name,path,url,type,size,create_time,update_time,creator,updater) VALUES (...)，遗漏tenant_id。只读SHOW COLUMNS确认infra_file.tenant_id为NOT NULL且无默认值。此处不是文件无效、待办未派或后端未开。
6. 实际栈定位MesDossierUploadStagingService.java:57，FileDO构造后直接FileMapper.insert。FileDO extends BaseDO且标记TenantIgnore，没有tenantId属性；该上传路径未向当前要求租户字段的infra_file契约供值。不是仅凭类名或源码猜测，异常SQL与表结构已交叉核实。
7. mes_dossier_upload_intent的177—188共12行均UPLOADING、file_id=NULL；本轮业务资料关联表三单有效行数为0。存储写入在文件登记之前发生，故不能声称存储对象不存在；本轮未删除/清理任何对象。其24小时到期清理为现有实现，未验证实际清理成功，也不新增清理前置。

|异常ID|时间（北京）|周期|任务|类别|
|---|---|---|---|---|
|429|10:18:09|1009200645|3078|来料|
|430|10:19:27|1009200645|3079|灭菌|
|431|10:21:09|1009200645|3080|成品报告|
|432|10:24:04|1009200646|3083|来料|
|433|10:24:10|1009200645|3081|成品记录|
|434|10:25:33|1009200646|3084|灭菌|
|435|10:27:58|1009200647|3088|来料|
|436|10:29:50|1009200647|3089|灭菌|
|437|10:29:52|1009200646|3085|成品报告|
|438|10:30:47|1009200646|3086|成品记录|
|439|10:35:20|1009200647|3090|成品报告|
|440|10:36:13|1009200647|3091|成品记录|

### A实际调用链与代码依据（仓库根目录相对）

- IntRuoyiFronted/src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue:4303、4332：按当前订单、申请、类别、workTaskId及所选文件上传；成功后重新加载，失败显示异常。
- IntRuoyiFronted/src/api/mes/pro/processpool/teamLeader.ts:1172、1185：multipart上传到本次异常URL。
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/MesActiveOrderDossierFileController.java:58：权限校验并组装上传命令。
- 同模块service/pro/productionrelease/pqc/MesActiveOrderDossierFileService.java:134、152、157：前置校验真实任务与可变性；153进入staging，154才绑定业务资料。实际已进入staging，不应通过改权限解决。
- 同目录MesDossierUploadStagingService.java:32、44、45：意图显式tenant归属及独立事务；47先存储；55、57构造并直接插入FileDO；58后才READY。
- IntRuoyiBackend/yudao-module-infra/src/main/java/cn/iocoder/yudao/module/infra/dal/dataobject/file/FileDO.java:23、24：TenantIgnore／BaseDO。对应dal/mysql/file/FileMapper.java:16使用通用mapper，没有本链专用租户字段写入。

### A最小恢复范围

交软件维护责任岗，需另行授权仅修复该资料上传暂存文件登记与当前infra_file租户契约的适配，保留真实tenant=186及隔离，不以默认tenant=0、删NOT NULL或改权限绕过。实现后针对当前表结构验证登记、业务绑定和失败残留处理，再按已授权发布程序协调主环境更新；本次诊断授权不含代码修复或主后端重启。由三资料岗先重新查回文件/任务，仅缺失项通过原前端续传。当前没有可用的纯启动恢复动作，附件未恢复。

## B：0648实际匹配数据

- 对象881MO100160／1009200648／SIM202611210648／申请342；列表放行状态及原页面报错沿用已保存的生产岗回执，本轮未重新办理或签署。
- 完工BATCH_RECORD回填1113，SUCCESS，生成2026-11-21 09:35:42；其formalSourceSnapshot来源绑定7张正式领料、37行。
- 冻结路线版本2121453182031057223，工序routeProcessId=2121453182219802463（processId=2117828464390516957）batchUseConfigs.inputMaterialIds含2120759356167581696；本地物料主数据对应A006.011.2003／三通旋塞-Ⅰ，且该ID不在本路线OUTPUT集合。详情因此按外部INPUT进行正式领料匹配。
- 本单当前本地ERP领料源37行，无A006.011.2003；完工快照同样无2003。37条来源ID集合及每条编码与当前本地源相同，没有发现“本地源有该2003而冻结漏了”的证据。
- 当前有效同名领料为A006.011.2008，源行2120759357121342147，单881SOUT00295714（单据状态C），批S20260801，实发/基本实发1120；对应本地物料ID2121333283992541884。绑定2123953460938407936，绑定行2123953460946796544同样为2008。名称相同不意味着两编码可互换。
- 冻结外部输入所需编码与37条正式源比对，唯一缺匹配编码为A006.011.2003。未把2008按名称强配给2003，未同步ERP或修改冻结记录。
- 边界：只确认本地已同步的正式源及已冻结快照，不证明远端金蝶当前无2003，也不能凭此判路线配置还是原领料业务错误。ERP岗位缺菜单不是根因。

### B调用链与实现解释

- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/MesProcessPoolTeamLeaderController.java:620、625：详情读取调用服务。
- 同模块service/pro/processpool/team/MesTeamLeaderActiveOrderDetailServiceImpl.java:252、253：先读回填快照并挂输入物料，之后255才组装放行申请及响应；挂接异常会让整页读取失败，不会撤销已处理列表里的正式放行结果。
- 上述文件742、830：读BATCH_RECORD已成功回填，核对订单/工单/状态/hash和正式领料来源结构。
- 上述文件753、760、773：基于冻结工艺INPUT/OUTPUT分类；外部INPUT转入toCompletedInputMaterialDetail。
- 上述文件871：精确比较materialCode与materialNumber（trim后），不是名称/模糊编码匹配；888：无匹配抛出“完工领料回填未匹配输入物料”。本轮2003/2008差异足以解释当前具体异常。
- 同模块service/pro/frontline/MesFrontlineProcessMaterialServiceImpl.java:125、138、154：从锁定路线configSnapshots.batchUseConfigs读取inputMaterialIds/outputMaterialIds。
- 同模块service/pro/processpool/team/MesTeamLeaderActiveOrderPickListCompletionSourceService.java:40、135：冻结正式领料且保留原编码与源行；MesTeamLeaderActiveOrderCompletionBackfillPortImpl.java:161、167、178、753：核对绑定/非空明细并形成来源快照，并未在此逐个校验冻结工序外部INPUT编码覆盖；详情才遇到该差异。这解释“完工/放行已有结果，详情仍失败”的时点差，不把源码规则当作本轮请求抓包。

### B责任和最小恢复

1. 工艺责任岗与有本单正式领料权限的ERP责任岗，先核准本轮应使用2003还是2008及有无获准替代关系；本地已有2008来源和冻结一致，无需生产岗重复完工/申请342。
2. 若2003才是正确用料：需有真实/本轮获授权正式源及受控补正路径；仅同步当前ERP不会自动改变已冻结快照，禁止私改历史。
3. 若2008为获准实际用料：由工艺/质量责任明确后，另行授权软件维护设计最小受控关联/纠正或读取修复；不能改成按名称匹配、跳过输入证据或直接编辑冻结JSON。
4. 软件维护可另行审查完工前一致性校验与详情读取故障隔离，但本次不实施。具体采取数据补正、工艺纠正或展示修复，须待业务编码归属决定，不能仅凭现有证据裁决。
5. 恢复验收：生产岗原前端重新打开0648详情，核对原342放行签名与四类资料任务。当前这些页面信息未由本轮直接核实，不报告已恢复或全链完成。

## 本次未实施与证据边界

- 无重复上传/业务提交、无服务停止重启、无源码或数据库修改、无权限配置修改、无旧单处理。
- 唯一新增资产为本诊断回执；既有PDF不重制。
- 读取源：本轮三资料岗回执、生产岗1020来源检查回执、真实内置浏览器首页/健康页、当前进程/监听、只读持久化异常及目标数据、现行源码。没有新增E2E PASS、存储清理PASS或签名核验声明。

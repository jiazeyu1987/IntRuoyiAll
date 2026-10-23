# E2E17 R2 定向维修 BDD / RED 记录

## 范围

仅分析原4234/A1激活晚49分40秒、复核跨部门同人规范及维护Root附加5测试中的2个旧夹具。已通过三人真实签收及计数3→2→1→0不重跑、不重签；原final-acceptance和manifest不修改。共享Query/Controller/生命周期/Quartz生产文件不修改。Maven由Root串行排期，本线程当前不执行。

## BDD A：已登记调度与人工排期区分

Given 原计划生效为2026-10-18 17:50:21（Asia/Shanghai），原5626暂停且指向其他文件，startupSync关闭；Root租约禁止全租户扫描。

When 旧暂停作业编辑被正式Infra守卫拒绝，Root改授权仅重建该自有作业；5627于18:39:43.905提交创建，正式页面预告首个自然cron为18:40:00，日志48025显示18:40:00—18:40:06成功返回1。

Then 原文件实际生效18:40:01应记录为晚于计划49分40秒，但不能把未登记本例任务期间归为已经运行的cron丢触发。不能回填生效时间、启动5625、改时钟或以旧三人重签补此证据。若发现“精确作业已提前在RAM注册且启用，却错过其到期cron”的独立反证，才给该缺陷补有效RED及原任务自有定向触发复验。

RED/证据：本轮没有已经确立的调度产品RED；原作业/页面/日志为环境排期证据。不得为拿到RED而改期、伪造触发或更改生产。

## BDD B：普通预览Controller必须传正式正文上下文

Given 当前Controller无distribution参数时使用`DccControlledFileBodyReadContext.daily(id)`的正式Query重载。

When `previewControlledFile_delegatesToQueryService`调用Controller。

Then fixture严格stub/verify同一个DAILY context的新重载，原watermark、Content-Disposition、Expose-Headers、access-event响应断言全部保留；旧无context重载不得被调用。

继承RED（Root实际执行，不冒充本线程执行）：`e2e17-body-open-policy-regression-process.json`记录2026-10-18 20:57:44—20:58:08、exit1；封存XML中该测试1error，Mockito PotentialStubbingProblem指出真实调用包含DAILY context而fixture stub旧重载。当前方法读取仍相同旧stub，足以定位夹具未跟上正式调用，不是要求回退Controller。

最小GREEN：只在原方法stub/verify加精确`daily(900L)`，增加旧重载never断言。运行结果须Root取得，当前不写GREEN。

## BDD C：下发身份不得隐式变成日常查看权限

Given 已投递的精确recipient仅有下发PREVIEW上下文，正式DAILY入口没有给他额外日常VIEW资格；现有W08 fixture确实未授普通VIEW。

When 以`readDistributionPreviewFile`重读原任务，再用无显式distribution的普通`readPreviewFile`重载尝试读取。

Then 两次显式下发读取均读原制品并记录原recipient opened；普通入口必须拒绝且不多读storage/不多写opened，两个ThreadLocal收口清空。不得为了旧成功断言给fixture虚授宽VIEW或修改Query把DAILY从token purpose恢复成下发授权。

继承RED（同一次Root5项命令）：W08 `readsIssuedPublishedBodyAndSavedEventRestoresOnlyThisTask`第一段精确下发读取成功，第二段旧普通重载因Current user cannot access报error；失败XML/日志已保留。现有另一个byteReadFailureAndOpenedWriteFailure测试通过，真实审计事务2项通过，合计3PASS/2ERROR。

最小GREEN：原测试保留两次显式下发重读成功，原“普通入口自动恢复下发”改为明确权限拒绝断言，校验没有第三次storage/opened。不改生产，也不把单元替身当真实前端证据。

## 校验计划

- 先按Root原5项精确命令串行复跑，证明2个旧夹具错误修复且3个原通过方法保持；可加运行W08/Controller两个完整类作与测试维护直接关联的回归，但由Root决定窗口。
- 本轮轻量结构/差异检查只验证改动范围与方法断言，不替代Maven编译和GREEN。
- 激活时间审计、跨部门口径文档无需后端运行态；当前不创建新申请或操作全租户job。跨部门原参数继续NOT_CONSTRUCTIBLE待业务确认，异常拒绝与事件幂等不等价于原跨部门成功。

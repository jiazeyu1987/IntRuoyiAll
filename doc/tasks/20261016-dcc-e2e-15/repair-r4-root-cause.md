# R3-06：过期仍待生效根因诊断（第一阶段）

## 状态与范围
ready_for_closeout — 只读诊断完成，等待Root范围review，dispatch保持hold。未实施修复或运行RED；工程无提交/推送授权，不标completed。仅新增本文，未改生产文件、Root文件或并行资产；无浏览器/E2E、构建、重启、数据库访问/写入、Git写入、Quartz操作、子Agent或线程通知。根及适用规则、R3原交接与Root问题表已复核，实际分支int_qms。

## 结论与分类
优先归类为本地验收运行配置/任务注册前置缺口，且启动器缺少关键配置回归检查；尚不能归类为生命周期产品过滤缺陷、账号资格问题或测试日期断言错误。

当前PID41508的Quartz已启动，但使用不持久化RAMJobStore。持久化infra_job行“正常”不能证明RAM中的JobDetail/Trigger存在。本轮启动路径没有恢复已批准的全局生效任务5625：全局启动同步显式关闭，专用DCC恢复开关未在已保存参数中启用；启动器还主动删除完整Quartz配置，使预期scheduler名称丢失，日志实际名称为quartzScheduler。该证据链足以定位启动配置缺口，不能把“Quartz线程已启动”当“生效任务已注册”。

保留边界：本阶段未读取运行进程环境、Quartz内存或数据库；专用开关是否另由进程环境注入未直接测量。不过当前启动日志没有专用注册/校验成功记录，实际scheduler名称也不满足专用恢复器的强制校验。因此不能声称已经观察到本轮全局扫描，更不能声称对象被扫描后过滤。Root若需运行态最终确认，应另行核对有效属性和RAM JobKey/TriggerKey，只读取白名单信息，不输出环境凭据。

## 本轮事实与原始错误
- E15：R3新file4417，完整ID2054545668044084417，A/1，BPM4b801112-cc9a-11f1-ada5-b082e25ec548。原U单次确认签名#1333，受控/签名2026-10-20 23:30:29，计划T23:40:29（均Asia/Shanghai）。23:42:36、23:43:55原U真实目录和详情仍受控待生效、浏览disabled、实际生效/启用/下发空。15-A至F PASS，15-G BLOCKED，后续正文/已生效历史NOT_RUN。
- E16既有R3 handoff的文件为2054545668044084420，业务BLOCKED；E17为2054545668044084418，T23:32:57、末次读回23:35:40仍待生效。此处只引用各原线程证据，没有操作其对象或扩大其结论。
- E15既有页面只读：5625显示正常、cron为0 * * * * ?；调度日志最近可见2026-10-08 08:35:00。本轮没有当前全局执行证据。旧日志、旧5629定向成功均不用于证明本轮全局运行。
- 首错完整保留在rerun-20261020-r3-round3/operations.json：折叠父菜单导致子菜单等待20秒；调度日志导航过渡出现两个loading遮罩导致strict mode。按真实菜单展开/全部遮罩结束后只读校准，没有重签或重复提交。不是业务请求30秒超时的证据。

## 具体源码与配置证据链
1. 持久化配置来源：IntRuoyiBackend/sql/mysql/20261003_dcc_controlled_file_activation_job_registration.sql:17–35只校验/准备infra_job行，新建为暂停；明确数据库行不是Quartz注册。JobServiceImpl.java:211–212的getJobPage只调用mapper.selectPage，页面“正常”来自记录，不查询Trigger状态。
2. 正常全局启动：IntRuoyiBackend/yudao-module-infra/src/main/java/cn/iocoder/yudao/module/infra/service/job/JobStartupSyncRunner.java:18、33，在startup-sync-enabled=true或缺省时调用syncJob；JobServiceImpl.java:148–162逐行delete/add并按STOP暂停。当前PID41508白名单命令行只读确认：profile=dcc-local-development，startup-sync-enabled=false，spring.quartz.auto-startup=true。没有输出完整命令行。
3. 专用安全恢复：IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/listener/DccLocalActivationStartupSyncRunner.java:34–35仅限定profile及显式enabled=true实例化；:60–79要求job-id5625、全局同步false、Asia/Shanghai、指定RAM scheduler=dcc-main-qms-acceptance、正式数据源及唯一注册；:89–92恢复并校验RAM注册。该组件和DccControlledFileActivationJob均存在于本轮运行Jar内，非旧包缺类。
4. 配置丢失点：doc/tasks/20261016-dcc-24-e2e-coordination/start-verified-runtime.py:23–27调用g56.environment后pop整个spring.quartz；:66–75直接复用旧arguments，仅替换Jar及日志路径。g56-local-runtime.py:16–28的environment只装配环境/Quartz名称；真正附加专用enabled/job-id在其main:63–70，当前启动器没有调用main。保存的old-process-arguments白名单同样只有auto-startup=true/global sync=false，没有DCC恢复开关。只读静态路径与当前PID白名单一致。
5. 本轮运行日志：runtime-start-rerunr2.json指向output/runtime/int_qms/logs/dcc24-rerunr2。application.log.2026-10-20.0.log:3为dcc-local-development，:65 RAMJobStore，:66–75名称quartzScheduler及非持久化，:94–95于13:04:43已启动。stdout.log:94–125同样佐证。检索该目录既有日志未见[dcc-local-activation]恢复/校验、全局同步成功或本轮全局任务执行异常；日志缺失不是独立证明，须与上述配置链合看。
6. local自动暂停不是本轮首要原因：LocalQuartzAutoPauseRunner.java仅@Profile("local")；本轮只有dcc-local-development，且主日志无自动暂停记录。application-local.yaml的默认白名单不等于本次有效注册来源。

## Bean、扫描和处理链：区分四阶段
- 未触发：当前最强证据。RAM重启不保留注册，而恢复链缺失；Quartz主scheduler启动不等于5625触发。
- 未扫描：没有本轮全局handler进入证据，不能进一步断言已进入handler但租户为空。DccControlledFileActivationJob.java:9、16–21是明确@Component Bean及@TenantJob，按dueVersions结果调用activateDue。
- 被过滤：DccControlledFileLifecycleService.java:449–460仅取当前租户、CONTROLLED_PENDING_EFFECTIVE、DELAYED且delayedEffectiveTime<=now，或历史/预设日期到期项。E15可见事实符合预期，但本阶段未读库，不宣称底层候选查询实际命中E15/16/17。
- 处理失败：activateDue(:295–335)锁逻辑文件及版本，核受控状态、签名证据/时间/BPM及到期，进入activateLocked；事务异常可阻止启用。没有本轮执行/对象异常证据，不能据仍待生效倒推出处理失败。
- 后续诊断风险（非本轮已证根因）：TenantJobAspect.java:45–56捕获单租户Throwable并写入结果字符串；JobHandlerInvoker依据抛出异常判断任务成功。所以即便将来出现“成功”日志，仍须核对租户错误结果及对象终态，不能用成功标签代替生效。全局handler对某租户首个activateDue抛错会中断该租户后续循环，需在确认真实进入后再查，当前不扩大修复。

## 最小改动边界（仅建议，尚未授权实施）
优先修正Root所有的本机启动装配及其测试，使已批准的DCC专用同步配置和完整RAM scheduler属性显式保留，且启动前验证有效profile、scheduler名称、global-sync=false、专用opt-in/job-id一致；复用现有恢复器的安全校验。不要打开全局sync、关闭本地收口、扩大租户/过滤条件、修改任务状态或直接调用activateDue来制造通过。仅补enabled仍会被实际scheduler名称校验拒绝，必须按既有完整RAM配置装配。

不建议改生命周期业务代码或前端待生效显示。若Root进一步确认有效配置已齐全且RAM触发器存在，再转入租户扫描/首个失败候选取证；该分支不能用猜测提前修补。

## Given/When/Then 与RED计划
1. Given当前启动器复用旧参数（global-sync=false/auto-startup=true）、RAM重启且infra_job正常行存在；When仅组装启动参数、不启动服务；Then必须显式产出批准的DCC恢复开关、job-id5625、完整RAM属性及指定scheduler名称。RED计划：新增针对start-verified-runtime配置构造的纯测试，当前pop整段Quartz/未设置专用opt-in应失败；不要执行顶层脚本，因为它会启动服务。应先将最小配置组装提取为可测试纯函数，保留现有凭据来源与安全门禁。
2. Given缺失专用opt-in、错误scheduler或global sync=true；When验证启动计划；Then明确拒绝，不默默开启全局任务。RED计划覆盖缺开关、缺job-id、错误名称三项及正常显式配置一项；当前组装没有该门禁应RED。既有DccLocalActivationStartupSyncRunnerTest包含默认不注册及正确RAM恢复等用例，修复阶段定向回归它，不重写已有行为。
3. Given安全配置获Root审核且测试GREEN；When未来另行授权启动/真实E2E；Then首次自然tick有本轮当前日志，原E15/16/17不重复确认，逐对象观察真实ACTIVE/实际时间/正文，再由E17核验下发和逐人签收。此步骤本阶段NOT_RUN，不用旧单文件任务替代全局证据。

## 验证与剩余限制
本文结构和关键源码/既有证据路径已通过本地只读检查；未运行测试，故无生产RED/GREEN结论。没有读取响应JSON作为业务依据，也没有HTTP/数据库探测。没有新增签名UNKNOWN；所有旧UNKNOWN和本轮首错保持。仅Root审核后进入下一阶段，当前停止等待。

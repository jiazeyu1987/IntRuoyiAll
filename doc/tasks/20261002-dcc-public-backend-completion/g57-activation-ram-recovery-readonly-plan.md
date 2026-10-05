# G57 单个已注册生效任务的RAM重启恢复审查

Status: ready_for_closeout — 2026-10-05仅只读源码/依赖字节码与官方资料核验、方案准备，未编辑生产/测试或运行Maven、DB/API、UI、服务、Git。当前实际job5625/handler/cron及已执行0结果为Root真实页面提供；本Agent未读取实际库/调度器，不假称已恢复。

## 结论：当前正式页面没有单任务重建路径

Root已通过真实前端创建唯一`infra_job.id=5625`，handler=`dccControlledFileActivationJob`，cron=`0 * * * * ?`。本机`dcc-local-development`使用RAMJobStore、Quartz auto-startup=true、全量startup-sync=false。重启会清空RAM调度对象，DB登记保留；DB状态NORMAL不证明内存JobDetail/Trigger存在。

| 原页面动作 | 当前正式调用 | 重启后的结果边界 |
|---|---|---|
| 编辑任务保存 | JobServiceImpl.updateJob73–90→SchedulerManager.updateJob69–76→rescheduleJob | 不重建JobDetail；缺Trigger时Quartz返回null，该返回被当前Manager忽略，页面可能成功但仍无调度。不能用编辑当恢复。 |
| 暂停，再开启 | updateJobStatus108–128→pauseJob/resumeJob/resumeTrigger | 不调用addJob。RAM缺任务时无trigger可暂停/恢复，可能无异常而只是no-op；DB状态和页面成功不代表恢复。直接再开启已NORMAL会先JOB_CHANGE_STATUS_EQUALS。 |
| 执行一次 | triggerJob131–143→scheduler.triggerJob | 只触发已存在Job，不建立每分钟调度；缺Job不能靠该入口恢复。 |
| 新增同handler任务 | createJob43–68 | 原handler存在则JOB_HANDLER_EXISTS；不得删旧登记/复制任务以规避。 |
| 全量同步/启startup-sync | syncJob148–165；JobStartupSyncRunner | 读取所有未逻辑删除登记，逐条删重建，STOP才额外暂停；会带入其它共享任务，不符合当前授权范围。当前job前端没有单ID同步按钮。 |

Quartz 2.5.2当前本机依赖字节码已读：`QuartzScheduler.rescheduleJob`在old trigger为空时直接return null；RAMJobStore.resumeJob仅迭代getTriggersForJob，未知key得到空集合，不会创建。与[官方2.5.2实现](https://raw.githubusercontent.com/quartz-scheduler/quartz/v2.5.2/quartz/src/main/java/org/quartz/core/QuartzScheduler.java)一致。故不是假定“开启必报错”，而是必须防DB/页面成功但RAM无任务的假恢复。本次没启动任何Scheduler或执行job。

## 本机运行配置边界

`g56-local-runtime.py`沿既有独立环境JSON建立RAM scheduler `dcc-main-qms-acceptance`，参数`--enable-activation`当前只改变`spring.quartz.auto-startup`；它没有恢复某一DB登记的入口。`JobStartupSyncRunner`由`yudao.local-job-control.startup-sync-enabled=true`且默认缺省true启用，会调用全量syncJob，因此本专属环境必须继续显式false。

`LocalQuartzAutoPauseRunner`只在`local` profile运行，且在全量注册后才暂停非白名单；不是dcc-local-development的单任务重建工具。不能靠增加local profile或先加载全部再暂停恢复5625，这还会再次混入JDBC Quartz配置与旧共享任务。JobDO为@TenantIgnore的正式系统登记，5625作为同一系统任务经已有@TenantJob遍历租户；不伪造一个新租户任务或变更既有业务身份。

## 最小有限源码建议（等待Root决定，尚未实施）

仅新增一个DCC专属Runner和一个专属测试，Root自己给现有任务启动脚本加显式参数，不改通用JobService、SchedulerManager、全量runner、infra/job前端或数据库。

- 建议生产路径：`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/listener/DccLocalActivationStartupSyncRunner.java`。
- 建议测试路径：同模块`src/test/java/.../service/file/DccLocalActivationStartupSyncRunnerTest.java`。
- Root自有`g56-local-runtime.py`或本轮后继任务脚本只在本次明确启用时追加`--dcc.local-activation-startup-sync.enabled=true`和`--dcc.local-activation-startup-sync.job-id=5625`；默认enabled=false，不提供默认ID、wildcard或通用skip门禁。此处只列拟定参数，尚无代码接受它们，不能现在加参数就冒恢复已实现。

Runner合同：

1. 独立`@Profile("dcc-local-development & !prod & !production & !backup & !test & !unit-test")`与默认false的ConditionalOnProperty双门禁。启用时核全量startup-sync仍false、单个实际Scheduler为非remote RAMJobStore、指定本机scheduler名和Asia/Shanghai。用实际DataSource metadata及只读数据库身份核本机loopback23306/已审源库UUID，不把配置字符串或另一个连接当实际来源，不输出凭据。
2. 通过既有正式JobMapper按准确5625及handler读取同一现行唯一登记，核未删除/NORMAL(1)、精确handler/cron及启用Bean implements JobHandler；读取原handlerParam/retryCount/retryInterval，不猜或修改原值。缺行/换ID/重复handler/STOP或未知状态/cron漂移/错误环境明确阻断，不override暂停、不自动创建新登记。
3. 只在RAM中没有该JobDetail和Trigger、且没有其它Quartz jobs/triggers时，复用既有`SchedulerManager.addJob`注册**原jobID**与原参数。不调用JobService.create/update/delete/sync，不写infra_job、签名、配置审计或业务行，不直接执行生命周期方法。自动分钟运行仍走原JobHandlerInvoker/租户decorator及真实生命周期事务。
4. 加入后读取实际JobDetail/Trigger核DEFAULT group/key、JobHandlerInvoker、JOB_ID=5625、handler及trigger引用、cron/参数/重试字段、timezone及真实nextFireTime。已有同ID Job+Trigger时只接受完整相符的零重复结果；部分对象/载荷冲突报错，不能delete/recreate、catch duplicate后假成功或放行其它jobs。记录脱敏恢复事实；启动异常不吞。若Root允许的现行registry参数范围与上项不同，先把准确差异写入此有限合同，不偷偷放宽。

此方案是恢复经真实UI建立的调度登记到本进程RAM，不是一条新业务写API或平台。正式prod/backup及其它开发分支不会加载它，默认启动也无读取/调度效果。真实业务到期激活仍属于原每分钟handler权限及Root已授权运行范围。

## 必要BDD/验证建议

- Given新RAM为空且准确注册NORMAL5625、正确环境/handler/cron，When显式本机启动恢复，Then仅1原ID JobDetail+1正确CronTrigger，infra_job原行精确不变，其它旧任务零注册；nextFire来自真实Scheduler。
- Given重复初始化且完整同载荷已有对象，Then零新增/零重排；Given残缺Trigger/错误JOB_ID/foreignjob或配置信息漂移，Then在新注册前明确失败，不自动覆盖。
- Given默认关闭或prod/production/backup/test/其它profile，Then组件不加载、Mapper与Scheduler零调用；Given目标STOP/缺行/handler重复/错cron/错本机库，Then零新增、准确失败，不能默认NORMAL。
- 使用隔离实际Quartz 2.5.2 RAM与现行Mapper夹具，暂停scheduler避免测试调用真实业务，仅验证配置/恢复对象。再由Root对真实重启后前端“执行日志”观察job5625的新执行时间与每个租户真实结果，禁止只看数据库NORMAL、CronUtils算出的未来时间、健康UP或outer success就写PASS。

原UI恢复路径的准确界限是：重启后可搜索同一个job5625并看登记/历史日志，但不能通过现有pause/resume/edit把它重新加入RAM。采用上述受权源码后，Root在已核新包启动时显式恢复，再通过原页面执行日志验证分钟运行；无新增复制job、SQL/业务API或全量startup-sync。

## 读时指纹与范围

- JobServiceImpl：`0040205a5b5ace6cd0ea37fc7be4f5b4721fc0311b9dbfb6b771a1d02992c56d`。
- SchedulerManager：`3ba1e8e0cb5e935e171c96da7d9808bb6b7c7a2a909abd7aa303da00024fd2cb`。
- Root `g56-local-runtime.py`：`40a2ec79cb42c0a7b174e9cf4a307fb57b360c39989f49ab7db19133168c825e`。
- 本机Quartz 2.5.2 jar：`452e418739c0da1bff255f7c1343dd3a92e1fdd3ceb126b185939edae9922091`（仅bytecode阅读，不代表Root新包依赖核验由本Agent执行）。

没有更改正在开发的G57 Java/FE文件，没有运行测试、恢复或定时业务。具体源码及Root启动参数由主管理决定并分配唯一Owner后再实施，不能把本准备文档当实际分钟恢复PASS。

## 后继授权与编译交接

Root随后已采纳并授权唯一新Runner1/test1实现；当前先落BDD及独立真实Spring/Quartz测试，等其它后端Owner归档释放Maven后才能进行行为RED与生产GREEN。测试第93行Date导入歧义曾挡住其它target编译，已仅改为java.util.Date并冻结、原失败记录保留；此错误不冒充任何业务RED。其它Source/通用JobService和实际调度/任务登记均未由此Agent修改。

## 后继实际源码交付（本机运行仍归Root）

新Runner现已实现上述专属opt-in/准确登记恢复合同，生产1/test1，行为RED1→17GREEN→最终R2最小24/2全0、18:52:39 CLI0。R2仅移除连接readOnly setter；同借用连接实际metadata+一个SELECT核源身份，不改连接池状态，positivecase verifies never setReadOnly。不改通用JobService/全量startup-sync、数据库登记或共享配置，没有actual业务恢复。源/原XML/日志/compiledclass指纹见`g57-local-activation-startup-fingerprints-r2.json`（SHA `1e1fe7d3930124c3b791eba3895b0b3e764cfb4f76add8a0d67d2a2c2a56ccb4`）；Maven已释放Root核包、启动与分钟真实页面日志验收。拟定flags现代码正式支持，Root任务launcher独占添加，不再建议UI暂停/编辑假恢复。

# G57 已注册分钟任务的本机RAM恢复 BDD

Status: ready_for_closeout — 唯一专属Runner1/test1最终R2冻结，默认关闭、仅dcc-local-development、原5625/handler/cron/status。实际RED1failure0error→GREEN17→final24/2all0 CLI0于18:52:39，2XML新archive-r2保留，Maven已交Root。无通用Job/SQL/sharedsync/业务API/DB/服务/Git/package操作；实际每分钟恢复Root另验。

- Given正确专属profile、opt-in、实际RAM scheduler为空且已注册NORMAL5625的准确handler/cron、上海同源本机连接，When应用Runner阶段执行，Then只恢复原ID的1Job/1CronTrigger，原JobDO不改、所有CRUD及global sync零调用，真实nextFire存在。
- Given没有恢复组件的当前正式启动组合且global startup-sync=false，When同配置启动，Then预期的5625实际JobKey不存在；测试以真实Scheduler.checkExists断言形成有效业务RED，不引用缺失class制造编译失败。
- Given正确恢复后的重复Runner阶段，Then完整同载荷不重建/不重排；Given既有半对象、错误jobID/handler/cron、其它RAM任务、错误source UUID/时区或global sync开启，Then在注册前失败，不自动删重建、同步共享任务或假成功。
- Given默认关闭或正式/backup/test/其它profile，Then专属组件不加载，Mapper/Connection/registration零动作。Given目标STOP，Then不自动启用或改数据库状态。

测试边界：实际隔离Quartz2.5.2 RAM、实际Spring profile/Conditional/Runner排序；Quartz保持standby，防触发业务。JobMapper与实际连接身份读端口是明确隔离夹具，不当真实MySQL或真实分钟执行。Root统一任务launcher加flags/新包运行和页面日志验收；测试不操作实际DB/服务/Git。

17:55:37当前其它Owner的原target编译被本新测试`Date`的java.util/java.sql歧义挡住：这是测试导入错误，不是其业务RED。仅第93行明确为java.util.Date，SHA `a963ebbbe15fa4543924fc3f340534929c6d4b31d2d90435421b7c0692f87887`，已告Root与Maven Owner；原失败log保留。本测试源现冻结，未再次改Java或运行Maven，等待明确编译归属交接。

Root明确移交后，旧startup组合关闭全量sync且无restorer，真实Spring+Quartz checkExists5625失败形成RED1failure0error，CLI1、原XML/log永久保留。Runner正确注册后17/1实际GREEN；首次GREEN编译误用SchedulerMetaData.isRemote已核2.5.2实际isSchedulerRemote修正，原编译log保留非业务RED。最后最小组合加旧infra JobStartupSyncRunnerTest7，共24/2全0。

Root有限连接池Review后R2只删SELECT预flight的Connection.setReadOnly，positivecase验证借用连接never改该flag，不假定Druid自动reset。24/2最终R2重核全部0于18:52:39，源码/class与两个XML新收据单独封存，旧18:46/r1源和收据仍保留历史，不能冒当前结果。当前Runner SHA `7de4bb6a306e3d0c8573aeba979e33a2156f2c3549074aeff3d32376a80acf90`；test SHA `c4b3fc7a77b8a0d99a1a5d2147db48b2f912c04e1642caacf604aa9460792480`。Root实际source UUID/job5625匹配由其只读核，本Agent没有实际库或分钟执行动作。

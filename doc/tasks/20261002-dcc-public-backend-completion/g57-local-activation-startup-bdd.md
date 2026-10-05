# G57 已注册分钟任务的本机RAM恢复 BDD

Status: in_progress — Root已授权新专属Runner1/test1，默认关闭、仅dcc-local-development、原注册5625与handler/cron/启用状态；不改JobService/SchedulerManager，不SQL或业务API、复制job、全量同步。当前其它Owner持有Maven，此阶段只写独立测试/文档，待Root明确交接后运行RED/GREEN。

- Given正确专属profile、opt-in、实际RAM scheduler为空且已注册NORMAL5625的准确handler/cron、上海同源本机连接，When应用Runner阶段执行，Then只恢复原ID的1Job/1CronTrigger，原JobDO不改、所有CRUD及global sync零调用，真实nextFire存在。
- Given没有恢复组件的当前正式启动组合且global startup-sync=false，When同配置启动，Then预期的5625实际JobKey不存在；测试以真实Scheduler.checkExists断言形成有效业务RED，不引用缺失class制造编译失败。
- Given正确恢复后的重复Runner阶段，Then完整同载荷不重建/不重排；Given既有半对象、错误jobID/handler/cron、其它RAM任务、错误source UUID/时区或global sync开启，Then在注册前失败，不自动删重建、同步共享任务或假成功。
- Given默认关闭或正式/backup/test/其它profile，Then专属组件不加载，Mapper/Connection/registration零动作。Given目标STOP，Then不自动启用或改数据库状态。

测试边界：实际隔离Quartz2.5.2 RAM、实际Spring profile/Conditional/Runner排序；Quartz保持standby，防触发业务。JobMapper与实际连接身份读端口是明确隔离夹具，不当真实MySQL或真实分钟执行。Root统一任务launcher加flags/新包运行和页面日志验收；测试不操作实际DB/服务/Git。

17:55:37当前其它Owner的原target编译被本新测试`Date`的java.util/java.sql歧义挡住：这是测试导入错误，不是其业务RED。仅第93行明确为java.util.Date，SHA `a963ebbbe15fa4543924fc3f340534929c6d4b31d2d90435421b7c0692f87887`，已告Root与Maven Owner；原失败log保留。本测试源现冻结，未再次改Java或运行Maven，等待明确编译归属交接。

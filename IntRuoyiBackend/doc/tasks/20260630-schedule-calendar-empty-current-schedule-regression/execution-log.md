# 执行日志：排程日历正式排程为空空态回归修复

- `2026-06-30 任务创建`：建立后端任务文档，目标是修复排程日历在正式排程为空时误抛异常的问题。
- `BDD: 月排程在正式排程为空时返回空态 -> Given 当前租户没有任何正式排程任务 / When 调用 getMonth / Then 返回 hasCurrentSchedule=false、totalTaskCount=0 和完整月份日期格子，而不是抛出 PRO_SCHEDULE_CALENDAR_CURRENT_SCHEDULE_REQUIRED。`
- `BDD: 日详情在正式排程为空时返回空明细 -> Given 当前租户没有任何正式排程任务 / When 调用 getDayDetail / Then 返回空 workshops、空 materialShortageSummary、空 scheduleIssueSummary，而不是抛出 PRO_SCHEDULE_CALENDAR_CURRENT_SCHEDULE_REQUIRED。`
- `BDD: 真正缺排程基础配置时仍 fail-fast -> Given 当前月存在正式排程任务但缺产线日历/产能等必要契约 / When 调用 getMonth 或 getDayDetail / Then 系统仍返回对应真实异常，不把配置问题伪装成空态。`
RED: mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dtest=MesProScheduleCalendarServiceImplTest#getMonth_shouldReturnEmptyCalendarWhenCurrentScheduleMissing+getDayDetail_shouldReturnEmptyDetailWhenCurrentScheduleMissing -Dsurefire.failIfNoSpecifiedTests=false test -> FAIL，当前模块存在与本任务无关的既有测试编译错误，尚未进入本次新增场景断言。
GREEN: mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dmaven.compiler.useIncrementalCompilation=false -Dmaven.compiler.includes=**/MesProScheduleCalendarServiceImpl.java -DskipTests compile -> PASS
GREEN: root-cause-fix-self-check -> PASS，MesProScheduleCalendarServiceImpl.buildContext(...) 已将“无正式排程”两条路径改为返回空上下文；getMonth/getDayDetail 会基于空上下文返回空态响应，而真实缺配置/缺产能/缺物料错误仍保留 fail-fast。
GREEN: javac @D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\output\mes-calendar-single-test-javac.args -> PASS，已在不触发其他历史测试源码重编译的前提下单独编译 MesProScheduleCalendarServiceImplTest。
GREEN: java @D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\output\mes-calendar-class-test-java.args -> PASS，JUnit Platform Console 执行 MesProScheduleCalendarServiceImplTest 共 32 条测试全部通过，包含 getMonth_shouldReturnEmptyCalendarWhenCurrentScheduleMissing 与 getDayDetail_shouldReturnEmptyDetailWhenCurrentScheduleMissing。
- `INFO: closeout-preview -> PASS，python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260630-schedule-calendar-empty-current-schedule-regression --mode preview 显示仅 backend-api-evidence.md 属于可清理附属产物；本次先保留证据文件。`

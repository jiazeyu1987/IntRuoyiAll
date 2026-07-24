# 任务：排程日历正式排程为空空态回归修复

- Task ID: `20260630-schedule-calendar-empty-current-schedule-regression`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-06-30`
- Current Status: `completed`
- User Request: `报错 加载月排程失败：当前正式排程为空，无法加载排程日历`

## Task Goal

修复 MES 排程日历在“当前正式排程为空”时直接抛异常导致月视图加载失败的回归，使月视图和日详情都能按空态契约正常返回，并保留 fail-fast 于真正缺配置、缺产能、缺物料等异常场景。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260630-mes-material-shortage-use-production-material-list\task.md`
- 状态：`in_progress`
- 处理说明：该任务当前只剩补充完整目标单测等收尾项；本次为独立排程日历回归修复，改动范围不同，允许单独建档推进。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 `docs\powershell-memory.md` 与 `docs\login-access.md`。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - PowerShell 5.1 不使用 `&&`；中文文件读写、命令输出统一显式 UTF-8。
- `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
  - 如需真实页面复验，先走官方 `login-preflight.mjs` 最小登录路径，不做接口旁路。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；修复接口正式契约，不新增回退逻辑。
- `是否从根因和长期维护角度解决`：是；把“正式排程为空”定义为正常空态，仍保留对真实缺数据契约错误的 fail-fast。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 月排程在正式排程为空时返回空态 -> Given 当前租户没有任何正式排程任务 / When 调用 getMonth / Then 返回 hasCurrentSchedule=false、totalTaskCount=0 和完整月份日期格子，而不是抛出 PRO_SCHEDULE_CALENDAR_CURRENT_SCHEDULE_REQUIRED。`
- `BDD: 日详情在正式排程为空时返回空明细 -> Given 当前租户没有任何正式排程任务 / When 调用 getDayDetail / Then 返回空 workshops、空 materialShortageSummary、空 scheduleIssueSummary，而不是抛出 PRO_SCHEDULE_CALENDAR_CURRENT_SCHEDULE_REQUIRED。`
- `BDD: 真正缺排程基础配置时仍 fail-fast -> Given 当前月存在正式排程任务但缺产线日历/产能等必要契约 / When 调用 getMonth 或 getDayDetail / Then 系统仍返回对应真实异常，不把配置问题伪装成空态。`

## Milestones

1. M1：建立任务文档与执行日志。`completed`
2. M2：补 RED 测试锁定空正式排程空态契约。`completed`
3. M3：实现最小修复并通过定向单测。`completed`
4. M4：如有需要执行真实登录复验并回填证据。`completed`
5. M5：完成任务收口与证据校验。`completed`

## Expected Verification

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dtest=MesProScheduleCalendarServiceImplTest#getMonth_shouldReturnEmptyCalendarWhenCurrentScheduleMissing+getDayDetail_shouldReturnEmptyDetailWhenCurrentScheduleMissing -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dtest=MesProScheduleCalendarServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260630-schedule-calendar-empty-current-schedule-regression\backend-api-evidence.md`

## Current Blockers

- 无当前阻塞；标准 `mvn test` 入口虽仍受模块内无关既有 ERP 同步测试编译失配影响，但本任务已通过隔离编译与 `JUnit Platform Console` 完成目标测试类 GREEN 验证。

## Final Verification Result

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dmaven.compiler.useIncrementalCompilation=false -Dmaven.compiler.includes=**/MesProScheduleCalendarServiceImpl.java -DskipTests compile` -> `PASS`
- `javac @D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\output\mes-calendar-single-test-javac.args` -> `PASS`
- `java @D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\output\mes-calendar-class-test-java.args` -> `PASS`，`MesProScheduleCalendarServiceImplTest` 共 32 条测试全部通过，包含：
  - `getMonth_shouldReturnEmptyCalendarWhenCurrentScheduleMissing`
  - `getDayDetail_shouldReturnEmptyDetailWhenCurrentScheduleMissing`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260630-schedule-calendar-empty-current-schedule-regression\backend-api-evidence.md` -> `PASS`

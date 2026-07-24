# 执行日志：MES 手动重排应用系统异常修复

- BDD: 手动重排应用返回明确成功或业务错误 -> Given 用户已完成有效重排预览且排产前检查无阻断 / When 调用 replanApply / Then 后端不得因内部持久化约束返回泛化系统异常，成功时写入任务和追溯日志，失败时返回可定位的业务错误。
- GREEN: experience-preflight -> PASS，已读取 `docs/powershell-memory.md` 与 `docs/login-access.md`；本轮仅允许本机验证，不访问测试服/正式服。
- BLOCKER: login-preflight -> 官方最小登录路径启动 Playwright 失败，错误为 `Invalid file descriptor to ICU data received`，发生在浏览器启动阶段，未进入登录页；因此本轮暂不执行真实写入型 E2E，转为日志、接口和单元回归定位。
- ROOT_CAUSE: runtime-log -> `E:\Int\CacheData\IntRuoyi\runtime\backend-20260706-125836.out.log` 中 `/admin-api/mes/pro/auto-schedule/replan/apply` 在写入排产任务和 eDHR 缺少批次号告警后，提交阶段抛出 `org.springframework.transaction.UnexpectedRollbackException: Transaction rolled back because it has been marked as rollback-only`。
- ROOT_CAUSE: transaction-boundary -> `openOrCreateFromScheduleCompletion` 是 `@Transactional(rollbackFor = Exception.class)`；缺少批次号属于可预期业务前置条件，但异常从事务代理抛出后即使外层捕获，当前事务也已被标记 rollback-only。
- RED: `mvn --% -pl yudao-module-mes -Dtest=MesProAutoScheduleServiceImplTest#apply_shouldNotEnterTransactionalEdhrCreationWhenSchedulePrerequisiteMissing -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL，单模块缺少上游 `yudao-module-bpm` 编译产物，无法完成 RED 执行；随后改用 `-am` 补齐依赖链。
- CHANGE: `MesProEdhrBatchExecutionService#getScheduleCompletionMissingItems` -> 新增非事务前置条件检查入口，复用批次号、产品、工艺路线、工序、绑定和候选池校验。
- CHANGE: `MesProAutoScheduleServiceImpl#createEdhrBatchExecutionsAfterScheduleCompletion` -> 创建 eDHR 批次前先执行非事务前置检查；缺少前置条件时写入 `EDHR_BATCH_CREATION` warning 并 `continue`，不再进入事务创建方法。
- CHANGE: `MesProAutoScheduleServiceImplTest#apply_shouldNotEnterTransactionalEdhrCreationWhenSchedulePrerequisiteMissing` -> 锁定缺少批次号时排产应用仍成功、任务写入、已排数量同步、产生警告且不调用事务创建方法。
- GREEN: `mvn --% -pl yudao-module-mes -am -Dtest=MesProAutoScheduleServiceImplTest#apply_shouldNotEnterTransactionalEdhrCreationWhenSchedulePrerequisiteMissing -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，1 test, 0 failures, 0 errors。
- GREEN: `mvn --% -pl yudao-module-mes -am -Dtest=MesProAutoScheduleServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，47 tests, 0 failures, 0 errors。
- GREEN: task-closeout-cleanup preview -> PASS，保留 `task.md` 与 `execution-log.md`，建议删除附属 `bug-regression-evidence.md`，blocked/warnings 均为 `<none>`。
- BLOCKER: task-closeout-cleanup apply -> 脚本只识别英文 `Current Status` 小节，中文 `当前状态` 被判定为 unknown；已补充 `Current Status: completed` 后重试。
- GREEN: task-closeout-cleanup apply -> PASS，删除附属 `bug-regression-evidence.md`，保留 `task.md` 与 `execution-log.md`，blocked/warnings 均为 `<none>`。

## 真实 E2E 验证补充 - 2026-07-06 14:18:42

BDD: 手动重排应用不再返回系统异常 -> Given 本机后端 48081 已运行包含修复的 jar 且测试租户用户通过真实登录进入前端 When 在 MES 手动重排页面执行应用重排 Then 前端不出现“应用重排失败 Error: 系统异常”，后端不再出现 UnexpectedRollbackException。
GREEN: experience-preflight -> PASS, 已读取 login-access.md、server-access.md、powershell-memory.md，真实 E2E 使用本机 http://localhost:8081 与测试租户/aoteman，写入范围限定为用户授权的“应用重排”。
状态: 准备执行官方登录预检与真实页面操作。
## 真实 E2E 应用重排结果 - 2026-07-06 14:45:12

GREEN: login-diagnostic-real-browser -> PASS, 使用系统 Chrome + Playwright 真实登录本机 `http://localhost:8081` 测试租户/aoteman，进入 `/mes/pro/schedule-order`，页面可见“排产工单”和“手动重排”。
GREEN: replan-apply-real-e2e -> PASS, 真实页面路径 勾选排产工单 -> 手动重排 -> 预览重排 -> 应用重排 已执行成功；最终候选工单为 YXN.037.011.1002 / PTCA球囊扩张导管 / S012010-4，接口 /admin-api/mes/pro/auto-schedule/replan/apply 返回 `code=0`，生成任务 `96` 条，阻断数 `0`。
GREEN: backend-log-regression-check -> PASS, 最新本机后端日志未出现 UnexpectedRollbackException 或 Transaction rolled back；旧问题“系统异常”未复现。
INFO: replan-apply-business-error -> 真实 E2E 中另有一条缺少生产用料清单的工单返回明确业务错误 1040250019：排产工单SCH-P0LZ-20260622192837-20260622-0001排产失败：工单缺少生产用料清单，该结果不是系统异常。
证据文件: D:\ProjectPackage\Int\IntRuoyi\output\playwright\replan-apply-e2e\replan-apply-e2e-multipage-report.json。

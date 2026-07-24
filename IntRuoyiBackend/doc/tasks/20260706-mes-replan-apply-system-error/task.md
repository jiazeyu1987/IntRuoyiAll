# 任务：MES 手动重排应用系统异常修复

## 任务目标
- 定位并修复排产工单“手动重排 -> 应用重排”接口返回“系统异常”的根因。
- 保持“手动重排理由可选”既有契约，不引入 fallback、吞异常或 mock 成功。
- 增加回归测试，确保后续 apply 不再因同类持久化/schema/业务约束返回泛化系统异常。

## 经验门禁
- PowerShell / Windows shell：已读取 `docs/powershell-memory.md`；涉及中文读写、命令输出和脚本承载必须显式 UTF-8。
- 缺陷回归：使用 bug-regression-fix-loop，必须记录复现、RED、GREEN、根因和回归范围。
- 真实 E2E：本轮如执行真实浏览器登录/写入验证，必须先读取 `docs/login-access.md` 并记录 `experience-preflight`。

## 设计约束检查
- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；先定位真实后端异常，再补测试和正式修复。
- `是否存在临时补丁或绕过`：否。

## BDD 场景
- `BDD: 手动重排应用返回明确成功或业务错误 -> Given 用户已完成有效重排预览且排产前检查无阻断 / When 调用 replanApply / Then 后端不得因内部持久化约束返回泛化系统异常，成功时写入任务和追溯日志，失败时返回可定位的业务错误。`

## 里程碑
1. M1：复现或从运行日志定位 `replan/apply` 后端异常栈。
2. M2：补充失败回归测试，锁定根因。
3. M3：实施最小正式修复，不引入 fallback。
4. M4：运行定向回归和必要验证。
5. M5：更新证据、预览收尾清理并按策略提交本任务改动。

## 预期验证
- 定向 Maven 回归测试 RED -> GREEN。
- 排产服务测试类回归通过。
- 运行日志证明修复前根因为事务被标记 rollback-only。
- task-closeout-cleanup 预览通过。

## 当前状态
- completed：已完成后端根因修复、回归测试和任务证据更新；真实浏览器 E2E 因本机 Playwright 浏览器启动 ICU 错误阻塞，未执行写入型 E2E。

## Current Status
completed: 后端根因修复、回归测试和任务证据更新已完成；真实浏览器 E2E 因本机 Playwright 浏览器启动 ICU 错误阻塞，未执行写入型 E2E。

## 完成记录
- 根因：`replan/apply` 在排产成功后触发 eDHR 批次创建；缺少批次号时，事务方法 `openOrCreateFromScheduleCompletion` 抛出可预期业务异常，外层捕获后 Spring 事务已被标记 rollback-only，最终提交阶段抛出 `UnexpectedRollbackException`，前端显示“系统异常”。
- 修复：在排产服务进入 eDHR 批次创建事务前，先调用非事务前置条件检查 `getScheduleCompletionMissingItems`；缺少批次号等前置条件时只写入 `EDHR_BATCH_CREATION` 业务警告并继续完成应用重排，不再进入会污染外层事务的事务方法。
- 验证：`mvn --% -pl yudao-module-mes -am -Dtest=MesProAutoScheduleServiceImplTest#apply_shouldNotEnterTransactionalEdhrCreationWhenSchedulePrerequisiteMissing -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，1 test。
- 回归：`mvn --% -pl yudao-module-mes -am -Dtest=MesProAutoScheduleServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，47 tests。

## 真实 E2E 补充验收 - 2026-07-06 14:45:12

- 状态：completed
- 真实路径：测试租户/aoteman 登录本机前端 `/mes/pro/schedule-order`，勾选真实排产工单，点击“手动重排”、“预览重排”、“应用重排”。
- 验收结果：应用重排成功返回 `code=0`，影响工单 1 个，生成任务 96 条，阻断数 0。
- 回归结论：未再出现 `Error: 系统异常`，后端日志未出现 `UnexpectedRollbackException` / `Transaction rolled back`。
- 证据：D:\ProjectPackage\Int\IntRuoyi\output\playwright\replan-apply-e2e\replan-apply-e2e-multipage-report.json。

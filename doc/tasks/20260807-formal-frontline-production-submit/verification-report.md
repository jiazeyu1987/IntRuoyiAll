# Verification Report

## Result

`PASS`

一线生产正式提交闭环已完成并通过真实 Playwright 写型路径验证。当前 `48081` 运行 Jar 已加载本任务后端关键 class；前端静态合同、类型检查、后端聚焦测试和真实浏览器提交链路均通过。

## Implemented Behavior

- 空完成数量、损耗大于完成数量、缺正式设备或缺数值参数时，前端明确阻止提交。
- 正式提交前展示订单、工序、实际员工、数量、损耗、设备和参数摘要，并提示提交后不可修改。
- 确认后只调用一次 `/mes/pro/feedback/frontline/submit`；取消或失败保留输入。
- 成功后展示报工、记录本和工序池 ID，锁定数量、损耗、设备、参数、重填和再次提交。
- 后端拒绝实际员工无启用生产组长归属或多启用组长归属。
- 后端拒绝缺失或重复数值设备参数；文本标准不要求读数，超限参数保留异常状态。

## Verification Evidence

| Check | Result | Evidence |
| --- | --- | --- |
| Frontend RED | PASS | `pnpm e2e:frontline-formal-submit:static` 按预期因缺正式校验函数失败 |
| Frontend formal contract | PASS | `pnpm e2e:frontline-formal-submit:static` |
| Frontend types | PASS | `pnpm ts:check` |
| Frontend adjacent regression | PASS | 正式载荷、超限参数、生产闭环、原型与像素布局、全屏、组长配置、组长异常参数、损耗维护 9 项合同 |
| Backend production compile | PASS | `mvn -pl yudao-module-mes '-Dmaven.test.skip=true' package` |
| Backend focused test compile | PASS | `mvn -pl yudao-module-mes -Pmes-frontline-formal-submit-targeted-tests compiler:testCompile` |
| Backend focused JUnit | PASS | `mvn -pl yudao-module-mes -Pmes-frontline-formal-submit-targeted-tests surefire:test`：6/6 |
| Skill evidence validators | PASS | frontend/backend evidence validator 与 self-test |
| Runtime class marker | PASS | 当前 `48081` Jar 的内嵌 MES 模块包含 `requireUniqueResponsibleLeaderUserId`、`duplicate parameterCode=`、`PRO_FRONTLINE_ACTUAL_EMPLOYEE_LEADER_ASSIGNMENT_INVALID` |
| Fixture preflight | PASS | `prepare-e2e-fixture.py` 语法检查和重建通过；任务自有 fixture 包含已确认工单、记录本字段定义、对应生产组长员工范围和新签名 |
| Real Playwright formal submit | PASS | `formal-frontline-submit-real.e2e.cjs`：事件 `187`，提交请求 1 次，leaderA 可见，leaderB 不可见 |
| Real Playwright formal submit rerun | PASS | `formal-frontline-submit-rerun.e2e.cjs`：事件 `191`，提交请求 1 次，leaderA 可见，leaderB 不可见 |
| Experience consolidation | PASS | 已合并到 `docs/e2e-rules.md#写入型-e2e-任务自有模拟环境门禁` 并更新 `docs/experience-index.md` 关键词路由 |

## Runtime Preflight

- `8081`：PID `51364`，归属 `E:\IntRuoyi\IntRuoyiFronted` Vite，HTTP 200。
- `48081`：PID `27904`，health `UP`，运行 `E:\IntRuoyi\output\runtime\int_main\backend-latest-20260807-2002-responsible-routes.jar`。
- 运行 Jar SHA256：`06e0025d3103abf28510cf2290545ed034aa815cc25fee28dd4bb729152edd2d`。
- Spring Boot 内嵌 `BOOT-INF/lib/yudao-module-mes-2026.04-SNAPSHOT.jar` 为 stored，满足 nested jar 加载门禁。

## Real E2E Result

- 租户：`测试租户` / tenant `122`。
- 一线员工：`ffs0807worker`；对应生产组长：`ffs0807lead1`；非对应生产组长：`ffs0807lead2`。
- 提交结果：`feedbackId=873`、`recordbookEntryId=980112`、`recordbookEventId=980112`、`processPoolEventId=187`。
- 设备参数状态：`ABOVE_UPPER`，超限读数被保留供生产组长复核。
- 结果文件：`E:\IntRuoyi\output\playwright\20260807-formal-frontline-production-submit\formal-frontline-submit-result.json`。
- 截图：`worker-formal-submit-success.png`、`leader-a-submission-visible.png`、`leader-b-submission-not-visible.png`。

## Revalidation Result

- 复跑时间：2026-08-07 22:05，本轮未复用旧 result 或截图作为通过证据。
- 复跑命令：`node doc\tasks\20260807-formal-frontline-production-submit\formal-frontline-submit-rerun.e2e.cjs`。
- 提交结果：签名 `3393`、`feedbackId=875`、`recordbookEntryId=980114`、`recordbookEventId=980114`、`processPoolEventId=191`。
- 用户路径：worker 空完成数量未发请求；合法提交只发 1 次正式接口；成功后页面锁定；`ffs0807lead1` 页面/API 可见事件 `191`；`ffs0807lead2` 页面/API 不可见且 API `total=0`。
- 数据复核：`mes_pro_process_pool_event.id=191` 存在且 `tenant_id=122/deleted=0`；`mes_pro_feedback.id=875` 状态 `2`，提交人 `914535`，审批人 `914533`。
- 新结果文件：`E:\IntRuoyi\output\playwright\20260807-formal-frontline-production-submit\formal-frontline-submit-rerun-result.json`。
- 新截图：`worker-formal-submit-success-rerun-20260807140457.png`、`leader-a-submission-visible-rerun-20260807140457.png`、`leader-b-submission-not-visible-rerun-20260807140457.png`。
- 诊断：`pageErrors=[]`、目标链路 `targetHttpErrors=[]`；仅记录非目标审批待办权限 console 与 Baidu 统计请求 abort。
- 本轮临时 helper 脚本已删除；复跑证据以任务日志、结果 JSON、截图和 DB 只读复核为准。

## Resolved Blockers

- 运行 Jar 已包含本任务关键 class marker，无需热替换。
- 生产组长测试身份与任务自有正式 fixture 已由 `prepare-e2e-fixture.py` 创建并通过真实页面验证。
- 早先真实 E2E 暴露的工单未确认、记录本 schema 为空、生产组长 scope 缺失均已作为 fixture 前置修正，不改变产品逻辑。

## Cleanup

- 当前状态：`completed`。
- `task-closeout-cleanup` preview/apply 均 PASS，blocked/warnings 均为 none。
- Cleanup 仅删除本任务一次性 helper/evidence 文件，保留 `task.md`、`execution-log.md`、`verification-report.md`。
- 本轮复验后再次确认任务目录仅保留 `task.md`、`execution-log.md`、`verification-report.md`；rerun 结果 JSON 和截图保留在 `output/playwright`。
- 当前为主工作区 `int_main`，无 linked worktree 合并/删除；未创建 Git commit、branch、worktree 或 push。

# 一线生产正式提交闭环

## Task Goal

将一线生产填写页的“提交”改造成正式、不可歧义的一次性提交：前端完成业务校验和确认，后端在单事务内校验正式上下文、生成正式报工/记录本/工序池事件，并保证提交结果只进入唯一对应生产组长的报工确认列表。

## Scope

- 一线生产填写页提交前校验、正式确认、提交 loading、成功只读和错误保留。
- `/mes/pro/feedback/frontline/submit` 正式提交契约与后端权威校验。
- 生产员工到唯一有效生产组长责任范围的正式归属门禁。
- 设备必填参数完整性、数量及损耗一致性校验。
- 前端合同测试、后端单元/合同测试和真实 Playwright 用户路径验证。

## Non-Goals

- 不修改 PQC 提交流程。
- 不修改数据库 schema、菜单、租户或生产数据。
- 不创建 fallback、默认组长或公共待认领列表。
- 不执行 Git 提交、合并或推送。

## Milestones

- [x] M1：冻结业务规则、BDD 场景和接口边界。
- [x] M2：前端 RED/GREEN，完成校验、确认与成功锁定。
- [x] M3：完成唯一生产组长归属及正式参数校验实现，聚焦 JUnit 6 项通过。
- [x] M4：完成前后端聚焦回归与技能证据校验。
- [x] M5：完成真实 Playwright 路径验证和任务收尾。

## Expected Verification

- 前端目标静态/组件合同测试 RED -> GREEN。
- `pnpm ts:check`。
- 后端目标 JUnit RED -> GREEN，覆盖成功、校验失败、归属缺失和归属不唯一。
- 后端相关正式提交、回滚、时间线可见性回归。
- 真实前端路径：空完成数量不发请求；合法数据确认后仅发一次正式提交；对应生产组长当日报工确认列表可见；提交后页面锁定。
- `frontend-feature-delivery` 与 `backend-api-delivery` evidence validator PASS。
- `task-closeout-cleanup` preview/apply PASS。

## Applicable Experience Gates

- `docs/backend-development.md#MES-生产人员档案正式工重复关联门禁`：员工候选、运行配置和提交校验必须同源于当前负责生产组长的启用生产人员档案；无归属或多组长归属必须失败，禁止回退设备账号或本人范围。
- `docs/backend-development.md#第三方报工直报正式链路门禁`：生产组长报工列表的正式事实来源是工序池 `PRODUCTION_SUBMIT` 时间线事件；仅写 `mes_pro_feedback` 不能证明列表可见。
- `docs/e2e-rules.md` 写型 E2E 门禁：必须从真实前端入口提交任务自有可追踪数据，并由对应生产组长本人登录页面确认列表可见；禁止用 API-only 或直接写库替代。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；正式上下文、人员归属和写入一致性均由后端权威校验，前端只负责交互和提前提示。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed

- 已完成前端正式校验、二次确认、单次事务接口调用、失败保留与成功锁定。
- 已完成后端实际员工唯一生产组长归属和数值设备参数完整性权威校验。
- 已确认 `48081` 当前运行 Jar 内嵌 MES 模块包含本任务三个关键 class marker，且 health 为 `UP`。
- 已补齐任务自有写型 E2E fixture：已确认工单、非空记录本字段定义、唯一生产组长员工范围和每次运行新签名。
- 真实 Playwright 写入路径 PASS：空完成数量不发请求，合法提交只发一次正式接口，事件 `187` 仅对对应生产组长可见，提交后页面锁定。
- 用户要求复跑 E2E 后，2026-08-07 22:05 再次真实 Playwright PASS：事件 `191`、feedback `875`、recordbookEntry `980114`，对应生产组长可见、非对应组长不可见。
- `task-closeout-cleanup` preview/apply 已通过；当前主工作区无需 worktree 合并或删除，未执行 Git 提交、合并或推送。
- 长期经验已沉淀到 `docs/e2e-rules.md#写入型-e2e-任务自有模拟环境门禁`，并更新 `docs/experience-index.md` 关键词路由。

## Cleanup Keep

- doc/tasks/20260807-formal-frontline-production-submit/task.md
- doc/tasks/20260807-formal-frontline-production-submit/execution-log.md
- doc/tasks/20260807-formal-frontline-production-submit/verification-report.md

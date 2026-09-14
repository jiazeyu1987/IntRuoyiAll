# 生产组长报工历史页签

## Task Goal

为生产组长工作台新增“报工历史”页签，只展示已审核通过的报工历史；列表内容复用报工管理的正式报工字段，并新增“审核通过人”和“审核通过时间”展示。

## Milestones

1. `completed` 建立 BDD/TDD 任务记录与 RED 静态合同。
2. `completed` 补齐后端时间轴读模型中的审核通过人姓名字段。
3. `completed` 前端新增报工历史页签、APPROVED 固定查询和只读历史列。
4. `completed` 运行目标静态合同、前端类型检查和后端定向验证。
5. `completed` 更新验证报告与收尾状态。

## Expected Verification

- `node IntRuoyiFronted/tests/e2e/team-leader-production-report-history-tab-static.spec.cjs`
- `node IntRuoyiBackend/yudao-module-mes/src/test/js/process-pool-timeline-mapper-static.spec.cjs`
- `pnpm ts:check`（在 `IntRuoyiFronted` 下）
- `mvn -pl yudao-module-mes -am "-Dtest=ProcessPoolTimelineRevisionSummaryTest,ProcessPoolTimelineFilterTest,ProcessPoolTimelineQueryTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`（在 `IntRuoyiBackend` 下）

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；复用正式报工分页接口和最新复核日志读模型，补齐审核人姓名字段。
- `是否存在临时补丁或绕过`：否。

## Experience Gates

- 命中 `docs/frontend-development.md` 多角色共享表格列池隔离门禁：生产组长和 PQC 组长共用报工表格时，页签专属字段必须通过列池和 tableKey 隔离，不能只靠 `v-if` 隐藏。
- 命中 `docs/e2e-rules.md` 列表可见性门禁：列表验收应锁定页面可见列和正式查询参数，不用隐藏编号或 API-only 代替页面能力。
- 命中 `docs/backend-development.md` 生产组长报工管理读模型门禁：报工列表必须来自工序池正式提交事件与组长责任员工范围，不得用前端假行或只改报工主表替代。

## Closeout Notes

- 实现与目标验证已完成。
- 报工历史实现随共享分支提交 `b9a75208853a8163d1285e9ff6c7698e33007198` 进入 `int_main`；该提交同时是本地 `int_main` 与 `origin/int_main` 的祖先，无需重复执行 merge。
- `task-closeout-cleanup` preview/apply 已通过，仅删除已归档结论的临时前后端 evidence 文件，保留正式代码、测试和核心任务记录。

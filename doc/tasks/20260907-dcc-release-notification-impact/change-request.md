# Change Request: DCC 发布通知与关联文件影响评估

## Request Summary And Source

- Source: 用户在 DCC 新文件 Windchill 生命周期完成后要求继续下一阶段。
- Request: 大版本发布后通知关联方，明确谁能看到新版，并让关联文件负责人判断关联文件是否需要升版。

## Current Baseline Reviewed

- 新文件生命周期已经支持 A/1、A/2、B/1、审批、独立发布、旧版替代和版本追溯。
- `dcc_controlled_file_related_file` 已保存版本级关联关系、相关 Master 与文件编号/名称/版本快照。
- 当前发布详情能展示 VIEW 权限决定的可见范围，但没有冻结的发布后续批次、影响评估任务或处理结论。
- 当前 DCC 已有站内消息任务、失败状态和人工重放能力，可复用消息基础设施。

## Classification

Requirement change and workflow extension after a completed lifecycle milestone.

## Impact Analysis

- Product: 增加发布后通知、影响评估、升版跟踪和文控监督页面。
- Design: 区分“完整可见范围快照”“实际通知收件人”“关联文件影响任务”三类事实。
- Data: 新增发布后续批次、通知收件人、关联影响任务和不可变处理审计；不回填历史文件。
- API: 增加后续批次查询、影响评估处理/转派和升版关联接口；复用现有通知 API 与 major-revision 流程。
- Test: 增加发布原子性、幂等、正反向关系、权限、通知失败、任务转派和真实页面闭环测试。
- Release: 需要新的增量 schema、消息模板和菜单/权限迁移；不改变既有文件生效条件。
- Operations: 通知失败需要可见状态和人工重试；不增加邮件、短信或外部平台依赖。

## Decision

Accept and split.

- 独立修复既有关联文件 SQL 的 release metadata，不与二阶段功能混合。
- 二阶段作为一个完整节点规划，按发布事实、影响任务、通知页面、审计验收四个里程碑实施。
- “可见用户”不等于“全部收件人”：冻结完整权限范围用于审计，实际消息只发给责任明确的人。

## Required Approvals

- 本轮“继续”授权需求与计划编写。
- 后续生产代码实施可继续，但启用子 Agent、数据库写入、真实 E2E、48081 重启和 Git 提交/推送仍需各自满足当轮明确授权。

## Downstream Skill Reruns

- product-requirements-docs: 生成 PRD、用户流程和验收标准。
- roadmap-node-dev-plan: 生成 development-plan、test-plan 和 task-state。
- bdd-tdd-acceptance-planner: 将每个行为映射到 RED/GREEN 和真实页面验收。
- development-plan-delivery: 获得实施授权后按 P1-P4 串行执行并要求独立 tester。

## Blockers And Next Action

- 当前无产品规则阻塞。
- 下一步完成并验证任务包；实现阶段从 P1 发布后续数据合同开始。

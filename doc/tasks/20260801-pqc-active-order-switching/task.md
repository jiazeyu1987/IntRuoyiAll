# PQC 活跃订单切换来源实现

## Task Goal

实现 PQC 检验员切换订单、工序、员工的正式数据来源：

- 订单来源必须是当前活跃订单。
- 工序来源必须是所选活跃订单对应产品的工艺路线工序。
- 员工来源必须是所有 PQC 员工 + PQC 组长。
- PQC 组长列表查看、判定、修正和日志能力不得与生产组长任务冲突。

## Milestones

- [ ] 梳理现有 PQC 填写页、组长工作台、活跃订单和工艺路线数据链路。
- [ ] 按 BDD 写出订单、工序、员工来源的 RED 测试。
- [ ] 实现后端/前端最小正式数据链路，不引入默认全量列表或静默降级。
- [ ] 运行定向验证并记录 GREEN/REGRESSION 证据。
- [ ] 完成收尾状态与验证报告。

## Expected Verification

- 前端静态契约覆盖 PQC 订单、工序、员工选择来源。
- 后端定向测试覆盖活跃订单、产品路线工序和 PQC 人员来源。
- `pnpm ts:check` 或记录无关历史阻塞。
- `mvn -pl yudao-module-mes -am` 定向测试或记录缺失前置阻塞。

## Current Status

in_progress

## Experience Gate

待读取 `docs/experience-index.md` 后补充适用经验门禁。

## Design Constraints Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是补齐正式来源接口/调用链。
- `是否存在临时补丁或绕过`：否。


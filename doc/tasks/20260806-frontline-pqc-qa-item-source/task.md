# 20260806 Frontline PQC QA Item Source

## Task Goal

优化一线 PQC 黄框字段来源，确保接收标准、检验方法、检验设备和设备编号均来自生产订单对应产品的 QA 检验项目中匹配当前工序的正式配置，不从表单槽位、工序开始、前端默认文案或 raw payload 推断。

## Milestones

- [ ] Milestone 1: 核对现有 QA 规程、PQC 填写页和后端任务快照链路
- [ ] Milestone 2: 先补 RED 回归，锁定 QA 项目设备/标准/方法来源约束
- [ ] Milestone 3: 实现最小正式链路修正，不引入 fallback
- [ ] Milestone 4: 运行定向验证并记录 GREEN / REGRESSION
- [ ] Milestone 5: 收尾记录与清理状态确认

## Expected Verification

- 后端定向测试：`mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest" test`
- 前端静态契约：覆盖 QA 规程保存时项目设备配置不能只降级为 `equipmentRequired`
- 结构校验：`git diff --check`

## Applicable Gates

- MES PQC 项目级检验快照门禁：PQC 填写、接收标准、检验方法、检验设备、设备编号必须来自发布 QA 规程项目和结构化 `itemResults[]`，禁止用整单设备、固定字段、前端文案、默认上下限、空标准、raw payload 或 API-only 展示替代正式项目级快照。
- 严格 no-fallback：缺正式 QA 项目、标准、方法或项目设备配置时必须 fail fast，不能用默认值、表单槽位、工序开始或前端拼接文案兜底。

## Current Status

in_progress

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；以 QA 规程项目和项目设备表作为唯一来源。
- `是否存在临时补丁或绕过`：否。

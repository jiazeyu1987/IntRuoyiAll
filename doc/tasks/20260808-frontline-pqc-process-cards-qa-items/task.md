# 一线 PQC 工序卡片按 QA 检验项目工序列展示

## Task Goal

一线 PQC 选择生产工单后，工序卡片不再按工艺路线工序列表展示；必须按所选生产工单对应产品的 QA 检验规程检验项目列表中的“工序”列去重展示，并继续使用正式 PQC 任务快照支撑首检/巡检、检验项目与提交。

## Milestones

- [x] 建立 BDD/TDD 证据并定位当前一线 PQC 工序候选来源
- [x] 补充回归测试，证明候选源必须排除路线额外工序
- [x] 后端按所选工单产品的 QA 检验项目工序列生成去重工序候选
- [x] 前端保持工序卡片读取正式候选，不做本地猜测或路线 fallback
- [x] 运行目标验证、相邻回归与基础 diff 校验

## Expected Verification

- 后端定向测试：覆盖“按 QA 检验项目工序列去重展示 PQC 工序候选”
- 前端源码核对：工序卡片继续读取接口正式候选 `deviceState.processOptions`，不使用工艺路线前端 fallback
- `mvn -pl yudao-module-mes "-Dtest=MesFrontlinePqcContextServiceTest#<targeted methods>" test`
- `git diff --check`

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否
- `是否从根因和长期维护角度解决`：是，按正式 QA 检验规程项目工序列建模工序候选来源，而不是前端隐藏或路线默认展示
- `是否存在临时补丁或绕过`：否

## Experience Gate

- 命中 `docs/backend-development.md#mes-pqc-项目级检验快照门禁`；旧门禁中“PQC 工序选择显示产品路线全工序”与本次用户明确口径冲突，已更新为“QA 检验项目列表工序列去重，不显示路线无 QA 项目工序”。

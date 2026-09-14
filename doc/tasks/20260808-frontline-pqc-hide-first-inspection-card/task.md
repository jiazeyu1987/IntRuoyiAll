# 一线 PQC 无首检工序隐藏首检卡片

## Task Goal

在一线 PQC 页面中，如果当前工序没有正式首检配置，则不显示首检卡片、首检页签或首检录入面板；有首检配置的工序保持现有首检展示与录入能力。

## Milestones

- [x] 建立 BDD/TDD 证据并定位一线 PQC 首检渲染链路
- [x] 补充 RED 静态回归测试，证明当前无首检工序仍会显示首检入口
- [x] 实现最小前端修复，仅根据正式首检配置控制首检卡片展示
- [x] 运行 GREEN 与相关回归验证
- [x] 完成收尾记录与验证报告

## Expected Verification

- `node tests/e2e/frontline-pqc-hide-first-inspection-card-static.spec.js`
- 受影响范围的前端静态或类型检查；若存在历史无关阻塞，记录精确 blocker
- `git diff --check`

## Current Status

completed

2026-08-08 23:10:19 +08:00 完成。实现、验证、技能 evidence validator、cleanup preview/apply 和项目经验沉淀均已完成；本任务未执行 Git 提交或推送。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否
- `是否从根因和长期维护角度解决`：是，按正式首检配置控制首检 UI，不用默认卡片或占位状态掩盖缺失
- `是否存在临时补丁或绕过`：否

## Experience Gate

- `docs/frontend-development.md#前端提交前严格验证与草稿态计算隔离门禁`：PQC 草稿态 UI 只能读取当前正式任务快照，不能用默认卡片、禁用按钮或占位状态掩盖正式配置缺失；验证需用聚焦静态合同锁定 UI 渲染来源。
- `docs/backend-development.md#mes-pqc-项目级检验快照门禁`：PQC 任务类型来自发布态正式路线与 QA 规程快照；缺正式 FIRST/PATROL 任务时不得前端猜测或默认生成。
- `docs/frontend-development.md#前端选择弹框即时反馈门禁`：一线 PQC 工序选择仍保留正式待检任务、工序和人员候选链路，本任务不修改 picker 行为。

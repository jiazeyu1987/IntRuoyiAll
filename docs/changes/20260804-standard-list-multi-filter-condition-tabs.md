# Change: 标准列表多维筛选改为条件 Tab 交集

## Request Summary And Source

- 用户基于截图反馈原“固定多条件栏”方式不好，要求在红框位置增加条件 Tab，Tab 左右有加减号；点击加号新增一个 Tab，点击查询时所有已填写/激活 Tab 条件取交集。

## Current Baseline Reviewed

- 当前 `TableMultiFilter` 按筛选定义横向铺开字段，并用“更多筛选”和 chip 展示已选条件。
- 排产工单 pilot 已接入该组件并通过真实 E2E，但 UI 仍偏“固定条件栏”，复用时需要按页面控制可见字段。

## Classification

- 产品行为变更 + 前端通用组件交互变更。

## Impact

- Product: 多维筛选从“页面配置哪些字段直接显示”改为“用户按 Tab 动态增加条件”，更适合复用。
- Design: 红框区域承载条件 Tab；下方只显示当前 Tab 的字段、操作符和值输入。
- Data/API: 不改变后端契约；仍只把已填写 Tab 映射为正式 query 参数，未映射条件仍进入显式 `multiFilters`。
- Test: 需要更新标准列表多维筛选静态合同、排产工单 pilot 静态合同，并复跑真实 E2E。
- Release/Ops: 不涉及服务端、数据库、菜单或运行端口变更。

## Decision

- Accept. 该设计替代上一版固定多条件栏，作为标准列表模板多维筛选的正式方案。

## Required Approvals

- 用户已在当前任务中明确提出并确认设计方向；无需额外审批。

## Downstream Skill Reruns

- `frontend-feature-delivery`
- `task-closeout-cleanup`
- `project-experience-consolidation` 如出现可复用新经验

## Blockers And Next Action

- 当前共享分支仍存在并行 dirty/ahead 状态，提交/推送继续阻塞。
- 下一步：先更新 BDD/静态合同形成 RED，再实现条件 Tab 和交集查询。

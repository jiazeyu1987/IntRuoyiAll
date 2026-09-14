# 智能排产问题修复与放行评审

## Task Goal

基于测试服务器问题核验结论，修复智能排产筛选状态一致性、完成状态命名、禁选原因、交期风险提示、人工强制完成语义和缺失基础数据时的操作边界，并通过逐问题 BDD/TDD、真实页面验证和独立 review-fix-loop 放行评审。

## Scope

- 前端：`IntRuoyiFronted/src/views/mes/pro/scheduleorder/`、标准多条件筛选组件及对应静态/真实 E2E。
- 后端：MES 排产工单人工完成、同步工单阻断动作及对应服务测试；仅在正式业务来源和既有工作流明确时修改。
- 不修改入池枚举值；前后端现有 `READY_TO_ADMIT / ALREADY_ADMITTED / BLOCKED` 映射保持不变。
- 不修改 `zhaojie` 或排产员的工艺权限；用户已确认这是环境权限错配，权限闭环问题退出本任务范围。
- 不执行测试服务器写操作，不修改账号权限、菜单、角色或生产数据。
- Git commit、merge、push、worktree 操作不在本任务授权范围。

## Issue Owners

1. 子 agent 1：筛选草稿状态与已执行结果一致性，并修正过期筛选测试合同。
2. 子 agent 2：“完成筛选”改为“完成状态”。
3. 子 agent 3：禁用复选框展示不可重排原因。
4. 子 agent 4：交期风险增加明确文本化提示。
5. 子 agent 5：保留人工强制关闭语义，并把“完成”入口明确改为“强制完成”。
6. 子 agent 6：物料/当前工序缺失时的操作矩阵。
7. 已取消：工艺维护权限闭环；用户确认排产员应有修改工艺权限，本任务不处理环境权限错配。

## Milestones

- [x] M1：完成问题核验、任务拆分和规则/经验门禁确认。
- [x] M2：6 个有效问题分别完成问题级 BDD、RED、最小实现或正式规则结论和 GREEN。
- [x] M3：主 agent 完成逐项代码、逻辑、易用性和 UI 初审。
- [ ] M4：独立 reviewer 按 review-fix-loop 输出三层放行单；当前因 reviewer 模型容量不足阻塞。
- [ ] M5：完成聚焦回归、真实页面 E2E、验证报告和任务收尾。

## Expected Verification

- 每个问题在 `execution-log.md` 记录独立 `BDD / RED / GREEN` 标记。
- 筛选必须验证修改未查询、点击查询后、三个状态正式请求参数和返回行状态。
- UI 问题必须通过真实页面和 Playwright 截图/可见文本验证，不以静态合同替代。
- 后端行为必须有成功与拒绝路径的聚焦 JUnit；未进入 Surefire 不得记为通过。
- 权限错配不在本任务修改范围；不得修改角色、菜单、权限或增加替代入口。
- 目标写请求与测试数据清理必须按 E2E 规则记录；只读验证写请求数为 0。

## Applicable Experience Gates

- `docs/frontend-development.md#统一列表复合工具栏布局门禁`：区分筛选草稿与已执行条件，标签不得冒充结果口径。
- `docs/e2e-rules.md#element-plus-表格选择门禁`：按可见业务行和唯一键核对选择状态及禁用原因。
- `docs/e2e-rules.md#mes-手动重排全选应用完成门禁`：手动重排需覆盖选中集合、阻断原因与真实请求链路。
- `docs/backend-development.md`：人工完成与权限逻辑必须走正式服务边界和聚焦 JUnit。
- `review-fix-loop`：reviewer 与 worker 隔离；逻辑、易用性、UI 三层无阻塞项才允许放行。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；筛选修复统一落在草稿/已执行状态模型，其余修复复用正式业务规则和权限工作流。
- 是否存在临时补丁或绕过：否。

## Current Status

blocked

- 阻塞阶段：M4 独立 reviewer 放行评审。
- 已完成：六个有效问题的 BDD/TDD、问题级实现、主 agent 聚焦回归，以及桌面端/移动端真实页面评审；Issue 7 权限错配已按用户决定退出范围。
- 历史阻塞：reviewer `019fdc3b-1917-77e3-aacb-7a164631285d` 与 `019fdc46-dc32-72c0-a1f4-ce72192ab365` 均因平台模型容量不足未启动；已保留失败审计记录。
- 当前阻塞：没有活跃隔离 reviewer，且 `.review-fix-loop/runs/20260807T061707Z-a109a4/review/report-round-1.md` 尚未生成；按 `review-fix-loop` 禁止主任务自审或静态-only 放行。
- 恢复条件：平台可成功启动隔离 reviewer，并生成 round 1 三层放行单后，再按 `pass/fail` 继续 worker 修复或最终回归。

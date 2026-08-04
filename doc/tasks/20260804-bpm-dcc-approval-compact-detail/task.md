# 20260804 BPM DCC 审批详情收敛

## Task Goal

让 BPM 流程详情中的 DCC 受控文件审批页只默认展示审核人需要的信息：审核内容是什么、当前流程走到哪一步、当前由谁处理；不再默认嵌入完整 DCC 受控文件详情页、项目代码联动、受控浏览落位、内部接口错误或管理员排障信息。

## Milestones

- [x] 记录审核人视角 BDD，明确默认视图边界。
- [x] 编写最小静态契约，先证明当前 BPM 详情无条件嵌入完整 DCC 详情为 RED。
- [x] 实施 BPM DCC 审批专用摘要视图，保留进入文控审批处理页的正式入口。
- [x] 运行聚焦静态契约和可行的前端验证，记录 GREEN/REGRESSION。
- [x] 更新验证报告和收尾状态。

## Expected Verification

- `node IntRuoyiFronted/scripts/bpm-dcc-approval-compact-detail.test.mjs`
- 可行时运行前端类型检查；若被既有无关问题阻塞，记录首个阻塞点与影响。

## Applied Experience Gates

### 前端同路由多入口分面门禁

- Trigger: 同一详情页或业务详情被多个入口复用，但审批入口只应展示某一类内容。
- Preflight check: 明确审批入口的信息范围和非目标范围；审批页不得继续加载或渲染非目标区块。
- Blocker: BPM 审批详情仍无条件嵌入完整业务详情页，或只用 CSS 隐藏非目标区块。
- Verification: 聚焦静态契约同时断言 BPM DCC 审批摘要、当前步骤、正式跳转入口，以及完整 DCC 详情组件不再无条件挂载。
- Forbidden action: 禁止吞掉接口错误、隐藏错误、改后端返回、或用空数据冒充收敛。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，按 BPM 审批嵌入边界收敛展示范围。
- `是否存在临时补丁或绕过`：否。

## Current Status

ready_for_closeout

实现和聚焦验证已完成；最终提交/推送前仍需处理工作区既有大量无关未提交改动和分支 ahead 状态。

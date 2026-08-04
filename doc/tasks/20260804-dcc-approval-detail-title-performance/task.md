# DCC 审批详情标题中文化与加载性能修复

## Task Goal

修复 BPM/DCC 审批详情页顶部标题仍显示英文 `DCC Controlled File Approval` 的问题，并定位从详情入口进入该页面加载很慢的前端链路，按根因减少非必要详情加载。

## Milestones

- [x] M0 读取项目规则、技能契约并保存开始前脏工作区基线
- [x] M1 定位审批详情标题来源、DCC 嵌入摘要/详情加载链路和现有静态合同
- [x] M2 先补 RED 静态合同，覆盖标题中文化和详情进入不应加载完整 DCC 详情的性能边界
- [x] M3 最小修改前端标题映射与详情加载条件
- [x] M4 运行目标静态合同、相邻合同和必要类型检查
- [x] M5 更新验证报告并完成收尾提交

## Expected Verification

- RED：目标静态合同在旧代码下失败，证明英文标题仍可见或 DCC 审批详情仍触发完整详情加载。
- GREEN：目标静态合同通过，证明页面标题使用中文审批名称，且 BPM 审批详情只加载审批摘要/必要动作入口。
- REGRESSION：运行相邻 BPM/DCC 审批详情静态合同或 `pnpm ts:check`；若全量检查被无关历史问题阻塞，记录隔离门禁和阻塞摘要。

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；目标是修复标题来源和详情加载边界，不用默认成功或隐藏错误掩盖慢加载。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

- 前端静态契约隔离门禁：若全量 `ts:check` 或大合同被无关历史问题阻塞，必须用任务专用最小静态合同完成 RED/GREEN，并记录无关阻塞。
- DCC 文控审批处理入口门禁：审批处理态不能被只读 viewer 或 API-only 替代，页面入口、处理控件和 DCC 链路必须保持真实可见。
- 前端同路由多入口分面门禁：BPM 审批详情通过业务表单嵌入 DCC 内容时，只应展示审批摘要和正式处理入口，不应无条件挂载完整业务详情页。

## Verification Evidence

- RED：`node tests/e2e/bpm-dcc-approval-detail-title-performance-static.spec.js` -> FAIL，旧代码缺少 `DCC Controlled File Approval` 中文标题映射。
- GREEN：`node tests/e2e/bpm-dcc-approval-detail-title-performance-static.spec.js` -> PASS。
- REGRESSION：`node tests/e2e/approval-center-chinese-copy-static.spec.js` -> PASS。
- REGRESSION：`node tests/e2e/dcc-bpm-dcc-approval-viewer-static.spec.js` -> PASS。
- REGRESSION：`node tests/e2e/form-center-bpm-dcc-approval-bypass-static.spec.js` -> PASS。
- REGRESSION：`node tests/e2e/bpm-process-timeline-current-node-green-static.spec.js` -> PASS。
- REGRESSION：`pnpm ts:check` -> PASS。
- QUALITY：`git diff --check -- <task-owned files>` -> PASS，只有 Git LF/CRLF working-copy warnings。

## Baseline Commits

- `71177c0a5`：开始前残余脏工作区基线。
- `ae0cf0d96`：并行残余脏工作区第二基线。

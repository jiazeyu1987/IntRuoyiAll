# 隐藏 PQC 提交详情红框区域

## Task Goal

隐藏生产组长工作台 PQC 提交详情中截图红框标出的“提交摘要”行和整个“PQC提交日志”区块，不改变其余 PQC 项目明细展示与数据链路。

## Milestones

- [x] M1：定位截图对应页面、模板区块与现有静态合同。
- [x] M2：先补充失败的静态回归合同，锁定两个区域不渲染。
- [x] M3：最小化修改模板并通过定向合同。
- [x] M4：完成相邻回归、约束校验与任务收尾。

## Expected Verification

- 定向静态合同先 RED 后 GREEN，证明 PQC 提交详情不再包含“提交摘要”和 `data-pqc-submission-log`。
- 运行生产组长工作台相邻静态合同，确认其余详情能力不回归。
- 运行 `pnpm ts:check`（若存在与本任务无关的既有失败，记录首个准确 blocker）。
- 运行 `git diff --check`。
- 运行 bug regression evidence validator。

## Experience Gate

- `docs/experience-index.md` 存在并已读取。
- 适用门禁：`docs/frontend-development.md#前端静态契约隔离门禁`。使用任务专用最小静态合同锁定截图目标区域，避免修改或放宽无关大合同来掩盖当前需求。
- 截图目标是可见详情区块，不涉及接口字段删除、数据源替换或 CSS 隐藏；应从 Vue 模板正式移除目标渲染入口。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；直接移除不应展示的模板区块，并用负向静态合同防回归。
- 是否存在临时补丁或绕过：否。

## Current Status

completed

目标实现、定向验证和 cleanup preview/apply 均已完成。全量 TypeScript 检查的无关既有 blocker 已在验证报告中准确记录。

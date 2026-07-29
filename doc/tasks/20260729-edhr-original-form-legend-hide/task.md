# eDHR 原表模式规则图例隐藏

## Task Goal

隐藏 eDHR 执行填写页原表模式顶部的规则类型图例，不影响表格内容、单元格类型角标、填写控件、左侧显示/填写模式切换和其他复用页面。

## Milestones

1. `completed`：定位截图红框对应组件和页面调用入口。
2. `completed`：补充聚焦静态合同并记录 RED。
3. `completed`：在执行填写页关闭规则图例。
4. `completed`：运行聚焦合同、相邻回归和类型检查。
5. `in_progress`：更新验证证据并完成收尾。

## Expected Verification

- `node tests/e2e/edhr-fill-workspace-original-rule-legend-hidden-static.spec.js`
- `node tests/e2e/edhr-fill-workspace-static.spec.js`
- `node tests/e2e/edhr-batch-template-simulate-red-box-hidden-static.spec.js`
- `pnpm ts:check`

## Applicable Gates

- 前端静态契约隔离门禁：使用独立静态合同锁定当前截图区域，不修改并发任务正在编辑的宽合同。
- 静态合同与真实 E2E 同步门禁：本次只改变展示开关，不改变真实填写路径或接口契约。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，使用组件已有显式图例开关限定页面行为。
- `是否存在临时补丁或绕过`：否。

## Current Status

ready_for_closeout

## Cleanup Keep

- doc/tasks/20260729-edhr-original-form-legend-hide/frontend-feature-evidence.md

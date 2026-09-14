# 一线生产选工序卡片密度调整

## Task Goal

根据截图反馈，缩小一线生产 / PQC 生产工序选择弹框中每个工序卡片的高度和卡片文字字号，保持现有工序选择、员工选择、返回、选中态和数据链路不变。

## Milestones

- [x] 定位目标组件、已有样式选择器和相邻静态合同。
- [x] 先更新目标字号 / 密度静态合同并跑出 RED。
- [x] 最小范围调整工序卡片高度、字号及相关返回按钮密度。
- [x] 跑目标静态合同、相邻生产布局合同和格式检查，记录 GREEN / REGRESSION。

## Expected Verification

- `node tests/e2e/mes-frontline-pqc-process-picker-production-layout-static.spec.cjs`
- `node tests/e2e/edhr-frontline-production-pixel-parity-static.spec.cjs`
- `node tests/e2e/edhr-frontline-production-fullscreen-toggle-static.spec.cjs`
- `pnpm ts:check`
- `git diff --check`

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，直接更新目标 picker 样式和锁定该选择器的静态合同。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

- `前端截图字号调整静态契约门禁`：本任务必须先更新目标选择器静态合同并跑 RED，再改最小 CSS；禁止全局字号覆盖或扩大成整页重设计。
- `前端选择弹框即时反馈门禁`：仅调整工序选择弹框视觉密度，不改变打开候选、点击选择、关闭弹框、错误暴露和候选数据来源。

## Closeout

- `task-closeout-cleanup preview -> ready`，仅计划删除临时 `frontend-feature-evidence.md`。
- `task-closeout-cleanup apply -> applied`，已删除临时 evidence 文件，保留 `task.md`、`execution-log.md`、`verification-report.md`。
- `project-experience-consolidation`：检索命中既有 `docs/frontend-development.md#前端截图字号调整静态契约门禁`，本任务无新增通用经验，无需更新长期经验文档。

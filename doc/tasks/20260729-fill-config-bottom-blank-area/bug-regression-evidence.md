# Bug Regression Evidence

## Bug Summary And Expected Behavior

- Bug: 填写配置弹窗底部存在空白占位，主区域没有填充到可用底部空间。
- Expected: 主工作区应填满全屏弹窗正文高度，底部不再保留旧页脚空白。

## Reproduction

- Path: 打开批记录表单列表中的“填写配置”，切换到“辅助表单映射”，观察三栏主区域底部。
- Static reproduction: 旧 CSS 使用 `height: clamp(520px, calc(100vh - 220px), 880px);`，保存操作已不在弹窗页脚后仍预留 220px。

## Root Cause

- 工作区高度仍按旧 footer 布局扣减视口高度，导致保存区移出页脚后底部剩余空间无法被主工作区占用。

## Regression Test

- Added: `node tests/e2e/batch-record-cell-rule-bottom-fill-static.spec.js`。
- Updated surrounding static expectations to require `height: calc(100vh - 84px);` and `flex: 1` main workspace fill behavior.

## RED / GREEN

- RED: `node tests/e2e/batch-record-cell-rule-dialog-size-static.spec.js` -> FAIL，旧实现缺少全屏正文高度占满规则。
- GREEN: `node tests/e2e/batch-record-cell-rule-bottom-fill-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。

## Risk And Regression Scope

- Risk: 仅 CSS 布局变更，主要风险为内部滚动区域高度分配。
- Regression scope: 聚焦静态合同、红框隐藏相邻合同和 TypeScript 类型检查。

## Verification

- `node tests/e2e/batch-record-cell-rule-bottom-fill-static.spec.js` -> PASS。
- `node tests/e2e/edhr-fill-config-redbox-hide-static.spec.js` -> PASS。
- `pnpm ts:check` -> PASS。

## Blockers And Follow-Up

- 旧导航/尺寸/视觉静态合同仍含顶部操作组和说明标题断言，受并行红框隐藏任务影响失败；需由红框隐藏任务统一更新旧合同口径。

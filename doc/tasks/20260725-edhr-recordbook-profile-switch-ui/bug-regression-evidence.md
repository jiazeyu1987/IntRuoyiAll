# Bug Regression Evidence

## Bug Summary And Expected Behavior

- Bug: 个人中心 eDHR 记录本全局开关卡片仍展示截图红框中的元信息块，且蓝框中只有 switch 自身可触发切换，文字区域不可点击。
- Expected: 红框元信息块删除；蓝框开关区域整体可点击并触发原有确认流程。

## Reproduction

- Path: 个人中心 > 配置 > eDHR 记录本全局开关。
- Evidence: 用户截图 `codex-clipboard-5d309620-9ec2-4984-a55e-30ae1fdc2077.png` 标注红框和蓝框。

## Root Cause

- Component rendered `el-descriptions` metadata block below the card header.
- Switch was rendered as a standalone `el-switch` with active/inactive text, without a clickable container for the surrounding blue-box area.

## Regression Test

- Updated `IntRuoyiFronted/tests/e2e/edhr-recordbook-global-setting-static.spec.js` to assert the metadata block and labels are absent, and the toggle wrapper exposes click and keyboard activation.

## RED / GREEN

- RED: `node tests/e2e/edhr-recordbook-global-setting-static.spec.js` -> FAIL on old `el-descriptions` metadata block.
- GREEN: `node tests/e2e/edhr-recordbook-global-setting-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。

## Risk And Regression Scope

- Scope is limited to the Profile recordbook global setting component and its static contract.
- Backend setting read/update behavior is unchanged.

## Verification

- `node tests\e2e\edhr-recordbook-global-setting-static.spec.js` -> PASS。
- `pnpm ts:check` -> PASS。

## Blockers

- None.

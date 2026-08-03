# Bug Regression Evidence

## Bug

DCC 受控浏览详情 viewer 只读区域仍显示截图黄框内的审批、分发、版本、修改和识别基础信息按钮。

## Expected

只读预览态的基础信息区域不渲染这些操作入口；普通详情页仍保留可编辑元数据场景下的识别基础信息能力。

## Reproduction

- 代码路径：`src/views/dcc/controlled-file/detail/index.vue` viewer 模板调用 `ControlledFileBasicInfoPanel`。
- RED: `node tests\e2e\dcc-controlled-preview-hide-basic-actions-static.spec.js` -> FAIL, viewer 基础信息面板仍传入 `show-info-actions`。

## Root Cause

viewer 只读模板复用了基础信息面板并显式传入 `show-info-actions`、`:show-edit` 和 `:show-product-recognition`，导致共享组件在只读区域仍渲染操作按钮。

## GREEN

- GREEN: `node tests\e2e\dcc-controlled-preview-hide-basic-actions-static.spec.js` -> PASS。

## Verification

- PASS: `node tests\e2e\dcc-controlled-browser-ux-optimization-static.spec.js`
- PASS: `node tests\e2e\dcc-view-preview-copy-unification-static.spec.js`
- PASS: `pnpm ts:check`

## Blockers

- 暂无。

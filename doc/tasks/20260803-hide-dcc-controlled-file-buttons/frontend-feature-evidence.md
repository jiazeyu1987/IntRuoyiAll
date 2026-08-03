# Frontend Feature Evidence

## Feature

隐藏 DCC 受控浏览 viewer 只读详情页基础信息面板中的截图黄框操作按钮。

## Acceptance

- viewer 只读基础信息面板不显示审批、分发、版本、修改、识别基础信息按钮。
- 普通详情页仍保留元数据识别入口。
- 不改变 DCC 受控浏览路由、权限、API、保存、审批或分发链路。

## BDD

- BDD: DCC 受控浏览详情只读按钮隐藏 -> Given 用户进入 DCC 受控浏览详情只读区域 When 页面展示文件基础信息 Then 截图黄框内的审批、分发、版本、修改和识别基础信息按钮不应渲染。

## RED

- RED: `node tests\e2e\dcc-controlled-preview-hide-basic-actions-static.spec.js` -> FAIL, viewer 基础信息面板仍传入 `show-info-actions`。

## GREEN

- GREEN: `node tests\e2e\dcc-controlled-preview-hide-basic-actions-static.spec.js` -> PASS。

## Verification

- PASS: `node tests\e2e\dcc-controlled-browser-ux-optimization-static.spec.js`
- PASS: `node tests\e2e\dcc-view-preview-copy-unification-static.spec.js`
- PASS: `pnpm ts:check`

## Blockers

- 暂无。

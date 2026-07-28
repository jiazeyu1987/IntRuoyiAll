# Feature

eDHR 批次详情右侧红框内表单卡片需要清晰表达当前选中项；点击主生产表或动态表单后，右侧当前卡片应与左侧工序面板一样显示浅黄色背景。

## Acceptance

- 当前选中的右侧表单卡片使用浅黄色背景 `#fff8e6`。
- 非选中卡片保留普通白色背景，hover 态不替代选中态。
- 动态表单和主生产表的查看/预览数据链路不变。

## BDD

- BDD: 右侧当前表单卡片黄底选中态 -> Given eDHR 批次详情右侧红框内存在多个表单卡片, When 用户点击其中一个表单卡片并切换中间预览, Then 当前选中卡片应显示与左侧工序面板一致的浅黄色背景，其他卡片保持普通背景。

## RED

- RED: `node tests/e2e/edhr-dynamic-form-card-preview-static.spec.js` -> FAIL，右侧卡片 `.edhr-batch-detail__rail-process-form-item.is-active` 仍为蓝色背景 `#eef5ff`，未使用左侧同款浅黄色选中反馈。

## GREEN

- GREEN: `node tests/e2e/edhr-dynamic-form-card-preview-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-batch-detail-hide-red-box-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-batch-admin-filler-visibility-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-loss-form-open-action-static.spec.js` -> PASS。

## Verification

- Source: `BatchExecutionDetailPage.vue` 仅修改 `.edhr-batch-detail__rail-process-form-item.is-active` 背景色为 `#fff8e6`。
- Contract: `edhr-dynamic-form-card-preview-static.spec.js` 同时锁定动态表单中心预览链路和右侧选中卡片黄底样式。
- Check: `git diff --check -- <task frontend files>` -> no whitespace errors; Git only reported CRLF normalization warnings.

## Blockers

- 当前工作区已有大量非本任务改动；本次未提交/推送，避免混入无关变更。

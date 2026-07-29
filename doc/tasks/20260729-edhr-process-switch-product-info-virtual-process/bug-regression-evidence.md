# Bug Regression Evidence

## Bug Summary

eDHR 填写页“切换工序”按来源 `routeProcessId` 合并任务，导致产品信息没有独立工序卡片；“切换填写人”也按相同 `routeProcessId` 收集任务，导致产品信息填写人出现在粗洗工序。

## Expected Behavior

产品信息成员任务可以保留正式批记录来源工序标识，但填写页必须按 `MAIN + BATCH_RECORD + 产品信息/80` 将其识别为独立虚拟 `80 产品信息` 工序。工序切换和填写人切换必须使用同一分组边界。

## Reproduction

- `node tests/e2e/edhr-assist-product-info-virtual-process-static.spec.js`

## Root Cause

`ExecutionPage.vue` 的 `buildAssistProcessSwitchItemKey` 只使用 `routeProcessId || routeProcessSort || processCode || processName || id`，没有产品信息专用分组键；`loadAssistFillerSwitchItems` 又只按当前任务 `routeProcessId` 过滤当前工序任务。产品信息和粗洗任务复用来源 `routeProcessId`，所以两个交互都发生错误合并。

## Regression Test

- `IntRuoyiFronted/tests/e2e/edhr-assist-product-info-virtual-process-static.spec.js`，锁定产品信息独立分组、名称/排序解析和填写人候选按显示工序分组隔离。

## RED

- `node tests/e2e/edhr-assist-product-info-virtual-process-static.spec.js` -> FAIL，首个预期失败为填写页缺少 `ASSIST_PRODUCT_INFO_PROCESS_SORT=80` 与产品信息虚拟工序识别。

## GREEN

- `node tests/e2e/edhr-assist-product-info-virtual-process-static.spec.js` -> PASS。

## Risk And Regression Scope

- 风险集中在填写页辅助模式的工序切换、状态聚合、当前项高亮和填写人候选范围。
- 不修改批次任务来源标识、后端门禁、权限、正式批记录绑定或表单槽位链路。

## Blockers And Follow-up

- 当前无阻塞。

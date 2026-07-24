# 执行日志：eDHR 详情页放行改为最后虚拟工序

BDD: 放行作为最后一个虚拟工序 -> Given 用户打开 eDHR 批次执行详情页 / When 查看左侧工序列表 / Then 列表末尾始终展示名为“放行”的虚拟工序，且不依赖后端工序任务数据。
BDD: 选中放行展示收尾状态摘要 -> Given 用户点击左侧“放行” / When 中间区域切换内容 / Then 中间不再显示普通工序空表单提示，而展示批次状态、预检摘要、放行状态、归档状态等放行摘要。
BDD: 放行参数栏承载原收尾按钮 -> Given 用户选中“放行” / When 查看右侧当前工序摘要栏 / Then 右侧展示“终态处理、归档打印、放行检查、UX检查、放行审批、追溯记录”六个操作，并保持原抽屉和动作绑定。
BDD: 底部收尾区不再重复展示 -> Given 用户查看详情页 / When 页面渲染 / Then 不再存在底部“收尾/放行归档”操作区。

GREEN: experience-preflight -> PASS, 已读取 PowerShell、经验索引、统一前端样式和前端交付门禁；本轮不执行真实 E2E、不操作服务器和数据库。

RED: node tests/e2e/edhr-release-virtual-process-static.spec.js -> FAIL, 预期失败：详情页尚未定义 `RELEASE_VIRTUAL_PROCESS`、`selectReleaseProcess`，且左侧没有“放行”虚拟工序。
GREEN: node tests/e2e/edhr-release-virtual-process-static.spec.js -> PASS
GREEN: node tests/e2e/edhr-closing-action-groups-static.spec.js -> PASS
GREEN: node tests/e2e/edhr-closing-actions-compact-copy-static.spec.js -> PASS
GREEN: node tests/e2e/edhr-batch-detail-review-fusion-static.spec.js -> PASS
GREEN: NODE_OPTIONS=--max-old-space-size=8192 pnpm ts:check -> PASS

## 完成记录

- 左侧工序列表新增固定“放行”虚拟工序，并在选中时清空真实工序选中态。
- 中间区域在选中“放行”时展示批次状态、预检摘要、放行状态和归档状态。
- 右侧当前工序摘要栏在选中“放行”时切换为“放行参数”，承载终态处理、归档打印、放行检查、UX检查、放行审批、追溯记录六个入口。
- 已移除底部 `edhr-batch-detail__closing` 收尾/放行归档区，避免入口重复。

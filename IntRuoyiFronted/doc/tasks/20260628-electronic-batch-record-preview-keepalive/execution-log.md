# Execution Log：电子批记录表单预览保活切换

BDD: 已访问报表切回不重新挂载 iframe -> Given 用户已经打开过多个电子批记录报表预览 / When 在这些已访问报表之间来回切换 / Then 页面复用已存在的 iframe，仅切换显示状态，不再次触发预览 loading。

BDD: 新报表首次访问仍正常加载 -> Given 某个报表尚未建立预览 iframe / When 用户首次点击该报表 / Then 页面仍按真实 Jimu 预览链路加载，并在首次成功后纳入保活列表。

RED: `node tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js` -> FAIL，当前右侧仅渲染单个 `IFrame`，切换报表时会重新挂载。

GREEN: `node tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js` -> PASS，页面已存在 `templatePreviewFrames` 保活列表，并通过 `v-for + v-show` 复用已访问 iframe。

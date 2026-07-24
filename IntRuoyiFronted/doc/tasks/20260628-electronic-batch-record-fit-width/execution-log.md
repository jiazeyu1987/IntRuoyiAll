# Execution Log：电子批记录表单宽度自适应

BDD: 容器变宽时表单宽度同步铺满 -> Given 电子批记录右侧预览容器宽于报表原始宽度 / When 同源 Jimu 预览完成并执行 fit-width 缩放 / Then 表单按容器宽度等比放大铺满，不再保持原始窄宽度。

BDD: 容器变窄时表单仍按比例缩小 -> Given 电子批记录右侧预览容器窄于报表原始宽度 / When 同源 Jimu 预览完成并执行 fit-width 缩放 / Then 表单继续按比例缩小，且高度同步更新。

RED: `node tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js` -> FAIL，当前 `src/components/IFrame/src/IFrame.vue` 使用 `Math.min(1, availableWidth / sourceWidth)`，宽容器场景被限制为最多 1 倍缩放，表单不会自适应铺满。

GREEN: `node tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js` -> PASS，静态契约已确认 fit-width 逻辑改为 `const scale = availableWidth / sourceWidth`，允许宽容器场景按比例放大。

GREEN: `node scripts/electronic-batch-record-jimu-list.test.mjs` -> PASS，电子批记录真实 Jimu 预览页面契约继续成立，未破坏保活 iframe 列表和右侧真实表单入口。

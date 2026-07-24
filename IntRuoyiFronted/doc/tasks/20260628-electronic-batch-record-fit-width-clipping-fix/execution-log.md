# Execution Log：电子批记录表单宽度自适应右侧裁切修复

BDD: 右侧表单最右列完整可见 -> Given 用户在电子批记录页面选择某个宽表单报表 / When 右侧真实 JMReport 预览按容器宽度自适应渲染 / Then 表单最右侧单元格边框与内容完整可见，不被容器裁切。

BDD: 宽度测量以真实内容宽度为准 -> Given 右侧真实 JMReport 预览存在超出当前 sheet 可视框的右侧单元格 / When IFrame 计算 fit-width 缩放比例 / Then 缩放基准应覆盖真实内容宽度，而不是只取局部可视宽度。

RED: 用户截图复现 -> FAIL，右侧表单最右列仍被裁切；现有 `IFrame` 仅按 `sheet.scrollWidth / offsetWidth / getBoundingClientRect().width` 估算源宽度，没有覆盖真实最右侧单元格边界。

GREEN: `node tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js` -> PASS，静态契约已确认 `IFrame` 新增 `resolveRenderedContentWidth`，并以真实表格/单元格右边界作为缩放基准。

GREEN: `node scripts/electronic-batch-record-jimu-list.test.mjs` -> PASS，电子批记录页面继续使用真实 JMReport iframe 预览，且 fit-width 逻辑已切换到真实内容宽度测量。

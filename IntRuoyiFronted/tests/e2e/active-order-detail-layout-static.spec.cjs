const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '..', '..')
const panel = fs.readFileSync(path.join(root, 'src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'), 'utf8')
const page = fs.readFileSync(path.join(root, 'src/views/mes/pro/processpool/ActiveOrderSubmissionDetailPage.vue'), 'utf8')

const tableShellCount = (panel.match(/team-leader-workbench__active-order-detail-table-shell/g) || []).length
assert.ok(tableShellCount >= 3, '生产、PQC、领料单表格都必须包在标准列表表格壳层内')
assert.ok(panel.includes(':fit="true"'), '详情表格必须启用 fit 以贴合当前页面宽度')
assert.ok(panel.includes('table-layout="fixed"'), '详情表格必须使用固定布局避免列宽撑破页面')
assert.doesNotMatch(panel, /<el-table-column[^>]+min-width=/, '详情页表格列不得继续使用 min-width 撑宽页面')
assert.match(panel, /overflow-x:\s*hidden/, '详情面板必须隐藏横向页面溢出')
assert.match(panel, /:deep\(\.el-tabs__nav-wrap\)[\s\S]*max-width:\s*100%/, '工序 Tab 导航必须限制在页面宽度内')
assert.match(panel, /:deep\(\.el-table__cell \.cell\)[\s\S]*white-space:\s*normal/, '表格单元格必须允许换行')
assert.match(page, /overflow-x:\s*hidden/, '独立详情页主体必须禁止横向溢出')

console.log('PASS: active-order detail layout static contract')

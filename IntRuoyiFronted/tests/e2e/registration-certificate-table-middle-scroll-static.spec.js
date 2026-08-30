const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const pageSource = fs.readFileSync(
  path.join(frontendRoot, 'src/views/dcc/registration-certificate/index/index.vue'),
  'utf8'
)

function extractUnifiedListByQueryForm(source, queryFormTestId) {
  const queryIndex = source.indexOf(`query-form-test-id="${queryFormTestId}"`)
  assert.ok(queryIndex >= 0, `${queryFormTestId} 必须存在。`)

  const startIndex = source.lastIndexOf('<UnifiedListTemplate', queryIndex)
  assert.ok(startIndex >= 0, `${queryFormTestId} 必须位于统一列表模板内。`)

  const endTag = '</UnifiedListTemplate>'
  const endIndex = source.indexOf(endTag, queryIndex)
  assert.ok(endIndex >= 0, `${queryFormTestId} 所在统一列表模板必须闭合。`)

  return source.slice(startIndex, endIndex + endTag.length)
}

const currentListSource = extractUnifiedListByQueryForm(
  pageSource,
  'registration-certificate-current-filter-form'
)
assert.match(
  currentListSource,
  /class="registration-certificate-current-list"/,
  '注册证当前列表必须保留页面级布局类。'
)
assert.match(
  currentListSource,
  /<template\s+#table\b[^>]*>\s*<div class="registration-certificate-current-table-scroll-region">\s*<el-table[\s\S]*height="100%"/,
  '注册证当前列表表格必须包在固定中间滚动区，并使用 Element Plus height 固定表头和底部横向滚动条。'
)
assert.match(
  currentListSource,
  /<el-table[\s\S]*height="100%"[\s\S]*scrollbar-always-on/,
  '注册证当前列表表格必须强制显示滚动条，确保横向滚动条留在当前页面内可见。'
)
assert.match(
  currentListSource,
  /<el-table[\s\S]*class="registration-certificate-current-table"[\s\S]*data-user-table-key="CURRENT_TABLE_KEY"/,
  '注册证当前列表表格必须有局部类名，避免滚动样式影响老证或其它表格。'
)

const styleSource = pageSource.match(/<style scoped>[\s\S]*<\/style>/)?.[0] || ''
assert.match(
  styleSource,
  /\.registration-certificate-current-list\s*\{[\s\S]*height:\s*calc\(100vh - 180px\);[\s\S]*min-height:\s*520px;[\s\S]*overflow:\s*hidden;[\s\S]*\}/,
  '注册证当前列表必须拥有视口计算高度，保证表头和横向滚动条同时留在页面内。'
)
assert.match(
  styleSource,
  /\.registration-certificate-current-list\s*:deep\(\.unified-list-template__query-form\)\s*\{[\s\S]*flex:\s*0 0 auto;[\s\S]*\}/,
  '注册证筛选标题区域必须固定在中间滚动区之外。'
)
assert.match(
  styleSource,
  /\.registration-certificate-current-list\s*:deep\(\.unified-list-template__table-shell\)\s*\{[\s\S]*display:\s*flex;[\s\S]*flex:\s*1 1 auto;[\s\S]*min-height:\s*0;[\s\S]*overflow:\s*hidden;[\s\S]*\}/,
  '注册证表格外壳必须成为有界 flex 子项，确保只滚动表格 body。'
)
assert.match(
  styleSource,
  /\.registration-certificate-current-list\s*:deep\(\.el-pagination\)\s*\{[\s\S]*flex:\s*0 0 auto;[\s\S]*\}/,
  '注册证分页尾部必须留在中间滚动区之外。'
)
assert.match(
  styleSource,
  /\.registration-certificate-current-table-scroll-region\s*\{[\s\S]*display:\s*flex;[\s\S]*flex:\s*1 1 auto;[\s\S]*height:\s*100%;[\s\S]*min-height:\s*0;[\s\S]*overflow:\s*hidden;[\s\S]*\}/,
  '注册证表格中间区域必须是有界滚动区。'
)
assert.match(
  styleSource,
  /\.registration-certificate-current-table\s*:deep\(\.el-table__body-wrapper\)\s*\{[\s\S]*overflow-y:\s*auto;[\s\S]*\}/,
  '注册证表格 body wrapper 必须纵向滚动。'
)
assert.match(
  styleSource,
  /\.registration-certificate-current-table\s*:deep\(\.el-scrollbar__bar\.is-horizontal\)\s*\{[\s\S]*display:\s*block;[\s\S]*opacity:\s*1;[\s\S]*\}/,
  '注册证表格底部横向滚动条必须保持可见。'
)

const oldListSource = extractUnifiedListByQueryForm(
  pageSource,
  'registration-certificate-old-filter-form'
)
assert.doesNotMatch(
  oldListSource,
  /registration-certificate-current-table-scroll-region|registration-certificate-current-table/,
  '注册证当前列表固定滚动样式不得误套到老证列表。'
)

console.log('PASS: registration certificate table fixes header/footer while only body scrolls')

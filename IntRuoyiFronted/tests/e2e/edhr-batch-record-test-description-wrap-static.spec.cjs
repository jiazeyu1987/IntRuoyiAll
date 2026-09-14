const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '..', '..')
const page = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/edhr-batch/BatchRecordTestPage.vue'),
  'utf8'
)

const descriptionColumns = Array.from(
  page.matchAll(/<el-table-column\b(?=[^>]*\bprop="description")[^>]*\/>/g)
).map((match) => match[0])

assert.equal(descriptionColumns.length, 5, '五张批记录测试列表都必须存在描述列。')
for (const [index, column] of descriptionColumns.entries()) {
  assert.match(
    column,
    /class-name="edhr-batch-record-test-page__description-column"/,
    '每个描述列都必须使用专用换行样式类。'
  )
  assert.match(
    column,
    /:show-overflow-tooltip="false"/,
    '每个描述列都必须关闭单行溢出 tooltip，让内容参与正常换行。'
  )
  assert.match(
    column,
    index === 4
      ? /getBatchRecordMappingColumnMinWidthString\('description', 280\)/
      : /ColumnMinWidthString\('description', 320\)/,
    index === 4
      ? '批记录映射描述列必须使用 280px 最小宽度，配合非固定操作列适配较窄桌面。'
      : '原有四张列表的描述列必须保持 320px 默认最小宽度。'
  )
}

assert.equal(
  (page.match(/\{\s*key:\s*'description',\s*label:\s*'描述',\s*minWidth:\s*320,/g) || []).length,
  2,
  '生产组长和两张一线列表共享定义必须保持 320px 描述列默认最小宽度。'
)
assert.match(
  page,
  /const batchRecordMappingDefaultColumns:[\s\S]*?\{\s*key:\s*'description',\s*label:\s*'业务说明',\s*minWidth:\s*280,/,
  '批记录映射业务说明列必须使用 280px 最小宽度。'
)

const wrapStyleBlock = page.match(
  /:deep\(\.edhr-batch-record-test-page__description-column \.cell\)\s*\{([\s\S]*?)\}/
)?.[1]
assert.ok(wrapStyleBlock, '必须提供描述单元格专用换行样式块。')
assert.match(wrapStyleBlock, /white-space:\s*normal;/, '描述单元格必须允许自然换行。')
assert.match(
  wrapStyleBlock,
  /overflow-wrap:\s*anywhere;/,
  '描述单元格必须允许连续长文本在边界内断行。'
)
assert.match(wrapStyleBlock, /word-break:\s*break-word;/, '描述单元格必须避免超长单词越过列边界。')

console.log('edhr batch record test description wrap static contract passed')

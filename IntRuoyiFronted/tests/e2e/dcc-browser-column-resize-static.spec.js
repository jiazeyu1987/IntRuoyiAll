const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const source = fs.readFileSync(
  path.resolve(process.cwd(), 'src/views/dcc/controlled-file/browser/index.vue'),
  'utf8'
)

const tableMatch = source.match(
  /<el-table[\s\S]*?data-user-table-key="dcc\.controlledFile\.browser\.main"[\s\S]*?>/
)

assert.ok(tableMatch, '文件查阅主表必须声明标准列表列配置 table key。')

const tableOpenTag = tableMatch[0]

assert.match(
  tableOpenTag,
  /\bborder\b/,
  '文件查阅主表必须开启 border，Element Plus 才会显示表头列宽拖拽句柄。'
)
assert.match(
  tableOpenTag,
  /@header-dragend="handleDccBrowserHeaderDragend"/,
  '文件查阅主表必须监听 header-dragend 以保存拖拽后的列宽。'
)
assert.match(
  tableOpenTag,
  /data-user-table-column-explicit/,
  '文件查阅主表必须标记显式列配置，避免全局增强器重复接管。'
)

const requiredResizableColumns = [
  ['fileName', '文件名称'],
  ['fileNumber', '文件编号'],
  ['directory', '所在目录'],
  ['productName', '产品名称'],
  ['category', '类别']
]

for (const [key, label] of requiredResizableColumns) {
  const columnPattern = new RegExp(
    `<el-table-column[\\s\\S]*?isDccBrowserColumnVisible\\('${key}'\\)[\\s\\S]*?label="${label}"[\\s\\S]*?:min-width="getDccBrowserColumnMinWidthString\\('${key}',\\s*\\d+\\)"`,
    'm'
  )
  assert.match(source, columnPattern, `${label}列必须使用用户列宽 min-width 绑定，支持拖拽后持久化。`)
}

assert.match(
  source,
  /handleDccBrowserHeaderDragend,\s*\n\s*saveConfig: saveDccBrowserColumnConfig/,
  '文件查阅主表必须从统一列宽 hook 取得拖拽保存处理函数。'
)

console.log('PASS: dcc browser column resize static contract')

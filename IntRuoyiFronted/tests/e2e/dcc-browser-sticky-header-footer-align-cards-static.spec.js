const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const source = fs.readFileSync(
  path.resolve(process.cwd(), 'src/views/dcc/controlled-file/browser/index.vue'),
  'utf8'
)

assert.match(
  source,
  /<el-table[\s\S]*?height="100%"/,
  '文件查阅主表必须设置 height="100%"，让 Element Plus 固定表头并只滚动表体。'
)

assert.match(
  source,
  /\.browser-page-layout\s*\{[\s\S]*?display:\s*flex;[\s\S]*?height:\s*calc\(100vh - 120px\);[\s\S]*?align-items:\s*stretch;/,
  '文件查阅左右卡片外层必须使用同一视口高度的 flex 拉伸布局。'
)

assert.match(
  source,
  /\.browser-page-layout\s*>\s*:deep\(\.el-col\)\s*\{[\s\S]*?display:\s*flex;[\s\S]*?height:\s*100%;/,
  '文件查阅左右 el-col 必须拉伸到同一高度。'
)

assert.match(
  source,
  /\.browser-directory-wrap\s*\{[\s\S]*?height:\s*100%;[\s\S]*?margin-bottom:\s*0\s*!important;/,
  '目录树卡片必须占满页面布局高度且去除底部外边距。'
)

assert.match(
  source,
  /\.browser-list-wrap\s*\{[\s\S]*?height:\s*100%;[\s\S]*?margin-bottom:\s*0\s*!important;/,
  '文件列表卡片必须占满目录树同高布局，避免底部错位。'
)

assert.match(
  source,
  /\.browser-list-wrap[\s\S]*?:deep\(\.el-card__body\)\s*\{[\s\S]*?display:\s*flex;[\s\S]*?height:\s*100%;[\s\S]*?min-height:\s*0;[\s\S]*?flex-direction:\s*column;/,
  '文件列表卡片 body 必须为纵向 flex，保证表格区滚动而分页固定。'
)

assert.match(
  source,
  /\.browser-list-template\s*\{[\s\S]*?display:\s*flex;[\s\S]*?height:\s*100%;[\s\S]*?min-height:\s*0;[\s\S]*?flex-direction:\s*column;/,
  '标准列表模板实例必须填满列表卡片并纵向排列筛选、表格和分页。'
)

assert.match(
  source,
  /\.browser-list-template\s*:deep\(\.unified-list-template__table-shell\)\s*\{[\s\S]*?flex:\s*1 1 auto;[\s\S]*?min-height:\s*0;[\s\S]*?overflow:\s*hidden;/,
  '表格壳层必须作为唯一伸缩滚动区域，让分页表尾固定在卡片底部。'
)

assert.doesNotMatch(
  source,
  /\.browser-page-layout\s*\{[\s\S]*?display:\s*block;/,
  '文件查阅页面布局不得继续使用 block + absolute 导致左右卡片高度脱节。'
)

assert.doesNotMatch(
  source,
  /\.browser-list-wrap\s*\{[\s\S]*?height:\s*auto;/,
  '文件列表卡片不得继续使用 auto 高度，否则无法适配目录树卡片高度。'
)

console.log('PASS: dcc browser sticky header footer and aligned cards static contract')

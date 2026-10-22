const assert = require('assert')
const fs = require('fs')
const path = require('path')

const repoRoot = path.resolve(__dirname, '..', '..', '..')
const detailPanelPath = path.join(
  repoRoot,
  'IntRuoyiFronted',
  'src',
  'views',
  'mes',
  'pro',
  'processpool',
  'components',
  'ActiveOrderSubmissionDetailPanel.vue'
)
const detailPanel = fs.readFileSync(detailPanelPath, 'utf8')

assert.match(
  detailPanel,
  /<strong>\s*批记录状态\s*<\/strong>/,
  '详情批记录区域标题必须显示“批记录状态”。'
)
assert(
  !detailPanel.includes('<span>当前状态</span>'),
  '详情批记录区域不得显示“当前状态”辅助标签。'
)
assert(
  !detailPanel.includes('正式批记录来源详情'),
  '详情批记录区域不得显示“正式批记录来源详情”辅助文案。'
)
assert.match(
  detailPanel,
  /detail\.activeOrderStatus\?\.statusLabel/,
  '详情批记录区域必须保留后端返回的批记录状态值。'
)

console.log('PASS batch-record-status-label-static')

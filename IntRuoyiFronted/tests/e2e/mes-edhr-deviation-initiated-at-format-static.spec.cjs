const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const sourcePath = path.resolve(__dirname, '../../src/views/mes/pro/edhr-deviation/index.vue')
const source = fs.readFileSync(sourcePath, 'utf8')

assert.match(
  source,
  /<template #default="\{ row \}">\{\{\s*formatTime\(row\.initiatedAt\)\s*\}\}<\/template>/,
  '偏差列表发起时间必须使用可读日期时间格式化，不能直接渲染毫秒值。'
)
assert.match(
  source,
  /const formatTime = \(value\?: string \| number \| null\) =>[\s\S]*?dayjs\(value\)[\s\S]*?format\('YYYY-MM-DD HH:mm:ss'\)/,
  '发起时间格式化必须兼容接口返回的毫秒时间戳。'
)

console.log('PASS mes-edhr-deviation-initiated-at-format-static')

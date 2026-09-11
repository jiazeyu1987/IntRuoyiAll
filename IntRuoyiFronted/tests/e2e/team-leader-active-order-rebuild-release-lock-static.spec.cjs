const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const source = fs.readFileSync(
  path.resolve(__dirname, '../../src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'),
  'utf8'
)

assert(
  source.includes('后端将禁止重建，请先完成或关闭放行链路后再操作'),
  '重建确认文案必须提示已有放行申请时后端禁止重建'
)

assert(
  !source.includes('将删除 ${preview.releaseApplicationCount} 条放行申请历史'),
  '重建确认文案不能再提示会删除放行申请历史'
)

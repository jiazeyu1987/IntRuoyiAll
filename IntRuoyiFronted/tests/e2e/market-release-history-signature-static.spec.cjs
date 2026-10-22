const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')
const historyPage = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/edhr-batch/BatchRecordHistoryPage.vue'),
  'utf8'
)

if (!historyPage.includes('releaseEvents')) {
  throw new Error('历史追溯统一时间线必须读取上市放行事务事件。')
}
if (!historyPage.includes('电子签名证据哈希=')) {
  throw new Error('历史追溯统一时间线必须明确展示上市放行电子签名证据。')
}

console.log('PASS market-release-history-signature-static')

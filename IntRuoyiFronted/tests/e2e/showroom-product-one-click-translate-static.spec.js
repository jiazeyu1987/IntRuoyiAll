const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')
const apiPath = path.join(root, 'src/api/showroom-admin/index.ts')
const indexPath = path.join(root, 'src/views/showroom-admin/index.vue')
const listPath = path.join(root, 'src/views/showroom-admin/components/ProductListTable.vue')

const apiSource = fs.readFileSync(apiPath, 'utf8')
const indexSource = fs.readFileSync(indexPath, 'utf8')
const listSource = fs.readFileSync(listPath, 'utf8')

const assertContains = (source, pattern, message) => {
  if (!pattern.test(source)) {
    throw new Error(message)
  }
}

assertContains(
  apiSource,
  /ShowroomProductTranslatePublishBatchTaskRespVO/,
  'API must expose translate publish task response type'
)
assertContains(
  apiSource,
  /batch-translate-publish\/start/,
  'API must call batch translate publish start endpoint'
)
assertContains(
  apiSource,
  /batch-translate-publish\/status/,
  'API must call batch translate publish status endpoint'
)

assertContains(
  listSource,
  /@click="handleBatchTranslatePublishClick"[\s\S]*?>[\s\S]*?一键翻译[\s\S]*?<\s*\/el-button>/,
  'product toolbar must render one-click translate button'
)
assertContains(listSource, /一键翻译任务/, 'product list must render translate task progress banner')
assertContains(listSource, /batchTranslatePublishTaskStatus/, 'product list must receive translate task status')

assertContains(indexSource, /handleStartBatchTranslatePublishTask/, 'workspace must start translate publish task')
assertContains(indexSource, /loadProductTranslatePublishTaskStatus/, 'workspace must poll translate publish task status')
assertContains(indexSource, /startBatchTranslatePublishTask/, 'workspace must call start API')
assertContains(indexSource, /getBatchTranslatePublishTaskStatus/, 'workspace must call status API')

console.log('PASS: showroom product one-click translate static contract is wired')


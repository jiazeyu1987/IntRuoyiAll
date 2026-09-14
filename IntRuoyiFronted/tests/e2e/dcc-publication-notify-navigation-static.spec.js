const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '..', '..')
const navigation = fs.readFileSync(path.join(root, 'src/utils/notifyMessageNavigation.ts'), 'utf8')
const detail = fs.readFileSync(
  path.join(root, 'src/views/system/notify/my/MyNotifyMessageDetail.vue'),
  'utf8'
)

assert.match(navigation, /type: 'dccPublication'/)
assert.match(navigation, /targetId: string/)
assert.match(navigation, /DCC_CONTROLLED_FILE_DETAIL_PATH_PATTERN/)
assert.ok(navigation.includes("/^\\/dcc\\/controlled-file\\/detail\\/(\\d+)$/"))
assert.match(navigation, /url\.origin !== window\.location\.origin/)
assert.match(navigation, /viewer: '1'/)
assert.match(navigation, /from: 'notification'/)
assert.match(navigation, /targetId: match\[1\]/)
assert.doesNotMatch(
  navigation.match(/const resolveDccPublicationTarget[\s\S]*?\n}\n/)?.[0] || '',
  /\b(?:Number|parseInt)\s*\(/
)
const unsafeId = '90071992547409931234'
const unsafeMatch = /^\/dcc\/controlled-file\/detail\/(\d+)$/.exec(
  `/dcc/controlled-file/detail/${unsafeId}`
)
assert.equal(unsafeMatch?.[1], unsafeId)

assert.match(detail, /DccPublicationNotifyTarget/)
assert.match(detail, /dccPublicationNavigation/)
assert.match(detail, /查看发布文件/)
assert.match(detail, /fileNumber: '文件编号'/)
assert.match(detail, /versionNo: '发布版本'/)
assert.match(detail, /reasonSummaries: '通知原因'/)
assert.match(navigation, /'followupUrl'/)

console.log('dcc publication notification navigation static contract passed')

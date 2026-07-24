const { readFileSync } = require('node:fs')
const { resolve } = require('node:path')
const assert = require('node:assert/strict')

const root = resolve(__dirname, '../..')
const sourcePath = resolve(root, 'src/views/mes/pro/feedback/index.vue')
const source = readFileSync(sourcePath, 'utf8')

const feedbackTemplateStart = source.indexOf('<ContentWrap v-if="activeTab === \'feedback\'">')
const feedbackTemplateEnd = source.indexOf('<ContentWrap v-else>', feedbackTemplateStart)
assert.notEqual(feedbackTemplateStart, -1, 'Production feedback main list block must exist.')
assert.notEqual(feedbackTemplateEnd, -1, 'Import-record block must remain separate from feedback main list.')

const feedbackBlock = source.slice(feedbackTemplateStart, feedbackTemplateEnd)
const importBlock = source.slice(feedbackTemplateEnd, source.indexOf('<FeedbackForm', feedbackTemplateEnd))

assert.match(
  feedbackBlock,
  /<UnifiedListTemplate[\s\S]*class="feedback-fixed-list"[\s\S]*table-key="mes\.pro\.feedback\.main"/,
  'Production feedback main list must use a dedicated fixed-list class.'
)
assert.match(
  feedbackBlock,
  /<template\s+#table\b[^>]*>\s*<div class="feedback-main-table-scroll-region">\s*<el-table[\s\S]*height="100%"/,
  'Production feedback table slot must wrap the table in a fixed middle scroll region and set el-table height to 100%.'
)
assert.match(
  feedbackBlock,
  /<el-table[\s\S]*class="feedback-main-table"[\s\S]*data-user-table-key="mes\.pro\.feedback\.main"/,
  'Production feedback table must preserve user table column persistence anchors.'
)
assert.doesNotMatch(
  importBlock,
  /feedback-fixed-list|feedback-main-table-scroll-region/,
  'Import-record attribution table must not use the main feedback fixed-scroll layout.'
)

assert.match(
  source,
  /\.feedback-fixed-list\s*\{[\s\S]*height:\s*calc\(100vh - 180px\);[\s\S]*min-height:\s*520px;[\s\S]*overflow:\s*hidden;[\s\S]*\}/,
  'Fixed feedback list must own a viewport-based bounded height.'
)
assert.match(
  source,
  /\.feedback-fixed-list\s*:deep\(\.unified-list-template__table-shell\)\s*\{[\s\S]*display:\s*flex;[\s\S]*flex:\s*1 1 auto;[\s\S]*min-height:\s*0;[\s\S]*overflow:\s*hidden;[\s\S]*\}/,
  'Unified table shell must be a bounded flex child so only the middle table body scrolls.'
)
assert.match(
  source,
  /\.feedback-main-table-scroll-region\s*\{[\s\S]*display:\s*flex;[\s\S]*flex:\s*1 1 auto;[\s\S]*height:\s*100%;[\s\S]*min-height:\s*0;[\s\S]*overflow:\s*hidden;[\s\S]*\}/,
  'Feedback table middle region must be a bounded flex scroll region.'
)
assert.match(
  source,
  /\.feedback-fixed-list\s*:deep\(\.el-pagination\)\s*\{[\s\S]*flex:\s*0 0 auto;[\s\S]*\}/,
  'Feedback pagination footer must remain outside the middle scrolling region.'
)

console.log('PASS: production feedback list fixes header/footer while only the table body scrolls')

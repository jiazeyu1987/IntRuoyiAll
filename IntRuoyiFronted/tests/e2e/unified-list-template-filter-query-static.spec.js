const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const templateSource = read('src/components/UnifiedListTemplate/index.vue')
const dccSignaturePage = read('src/views/dcc/controlled-file/signatures/index.vue')
const edhrSignaturePage = read('src/views/mes/pro/edhr/SignaturePage.vue')

const extractStyleBlock = (source, selector) => {
  const match = source.match(new RegExp(`${selector.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\s*\\{([\\s\\S]*?)\\}`))
  assert.ok(match, `${selector} style block must exist`)
  return match[1]
}

const assertQueryActionsInsideTemplate = (source, tableKey, queryHandler, resetHandler) => {
  const start = source.indexOf(`<UnifiedListTemplate`)
  assert.notEqual(start, -1, `${tableKey} must render UnifiedListTemplate`)
  const tableKeyIndex = source.indexOf(`table-key="${tableKey}"`, start)
  assert.notEqual(tableKeyIndex, -1, `${tableKey} must use the expected table key`)
  const end = source.indexOf('</UnifiedListTemplate>', tableKeyIndex)
  assert.notEqual(end, -1, `${tableKey} UnifiedListTemplate block must close`)
  const block = source.slice(start, end)

  assert.match(block, /<template #extra-filters>/, `${tableKey} must keep its filter controls`)
  assert.match(block, /<template #actions>/, `${tableKey} must keep query/reset actions`)
  assert.match(
    block,
    new RegExp(`<el-button[^>]*@click="${queryHandler}"[\\s\\S]*?>[\\s\\S]*查询[\\s\\S]*?</el-button>`),
    `${tableKey} query button must remain visible inside the standard template`
  )
  assert.match(
    block,
    new RegExp(`<el-button[^>]*@click="${resetHandler}"[\\s\\S]*?>[\\s\\S]*重置[\\s\\S]*?</el-button>`),
    `${tableKey} reset button must remain visible inside the standard template`
  )
}

const queryFormStyle = extractStyleBlock(templateSource, '.unified-list-template__query-form')
assert.match(
  queryFormStyle,
  /flex-wrap:\s*wrap;/,
  'UnifiedListTemplate filter toolbar must wrap by default so query/reset actions are not pushed off-screen.'
)
assert.doesNotMatch(
  queryFormStyle,
  /flex-wrap:\s*nowrap;/,
  'UnifiedListTemplate must not keep the filter toolbar in a single unbounded row.'
)

const toolbarActionsStyle = extractStyleBlock(
  templateSource,
  '.unified-list-template__toolbar-actions'
)
assert.match(
  toolbarActionsStyle,
  /flex:\s*0\s+0\s+auto;/,
  'UnifiedListTemplate query/reset action area must keep its intrinsic width.'
)
assert.match(
  toolbarActionsStyle,
  /margin-left:\s*auto;/,
  'UnifiedListTemplate query/reset action area should align right without shrinking to zero.'
)
assert.doesNotMatch(
  toolbarActionsStyle,
  /flex:\s*1\s+1\s+auto;/,
  'UnifiedListTemplate query/reset action area must not shrink behind long filter rows.'
)
assert.match(
  templateSource,
  /withDefaults\(defineProps[\s\S]*showQuickFilter:\s*true/,
  'UnifiedListTemplate must show quick filtering by default because Vue casts absent Boolean props to false.'
)

assertQueryActionsInsideTemplate(
  dccSignaturePage,
  'dcc.electronicSignature.records',
  'handleRecordQuery',
  'resetRecordQuery'
)
assertQueryActionsInsideTemplate(
  dccSignaturePage,
  'dcc.electronicSignature.authorizations',
  'handleAuthorizationQuery',
  'resetAuthorizationQuery'
)
assertQueryActionsInsideTemplate(
  edhrSignaturePage,
  'mes.pro.edhr.signature.history',
  'handleQuery',
  'resetQuery'
)

console.log('PASS: unified list template filter query static contract')

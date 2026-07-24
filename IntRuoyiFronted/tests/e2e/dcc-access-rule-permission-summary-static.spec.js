const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../..')
const readSource = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const packageJson = JSON.parse(readSource('package.json'))
const accessRulePage = readSource(
  'src/views/dcc/controlled-file/components/DirectoryAuthorizationTabPanel.vue'
)

const extractBetween = (source, startToken, endToken) => {
  const start = source.indexOf(startToken)
  assert.ok(start >= 0, `Missing start token: ${startToken}`)
  const end = source.indexOf(endToken, start)
  assert.ok(end > start, `Missing end token after ${startToken}: ${endToken}`)
  return source.slice(start, end)
}

const rulesTable = extractBetween(
  accessRulePage,
  'data-user-table-key="dcc.controlledFile.permission.directoryAuthorization"',
  '</el-table>'
)

const zh = (value) => JSON.parse(`"${value}"`)
const LABEL_PERMISSION_SUMMARY = zh('\u6743\u9650\u6458\u8981')
const LABEL_CHANGE_REASON = zh('\u53d8\u66f4\u539f\u56e0')
const INPUT_CHANGE_REASON = zh('\u586b\u5199\u672c\u6b21\u6388\u6743\u8c03\u6574\u539f\u56e0')
const VIEW_EXPLANATION = zh('\u67e5\u770b\uff1a\u53ef\u770b\u8be6\u60c5\u5e76\u9884\u89c8\u6587\u4ef6\u5185\u5bb9')
const DOWNLOAD_EXPLANATION = zh('\u4e0b\u8f7d\uff1a\u4ecd\u9700\u5355\u72ec\u6388\u6743')
const PREVIEW_EXPLANATION = zh('\u9884\u89c8\uff1a\u53ef\u6253\u5f00\u6587\u4ef6\u5185\u5bb9')
const REMOVED_HEADERS = [
  zh('\u53ef\u67e5\u770b'),
  zh('\u53ef\u9884\u89c8'),
  zh('\u53ef\u4e0b\u8f7d'),
  zh('\u542f\u7528')
]
const TOKENS = [
  zh('\u67e5\u770b'),
  zh('\u4e0b\u8f7d'),
  zh('\u5df2\u542f\u7528'),
  zh('\u5df2\u505c\u7528')
]

assert.strictEqual(
  packageJson.scripts['e2e:dcc:access-rule-permission-summary:static'],
  'node tests/e2e/dcc-access-rule-permission-summary-static.spec.js',
  'package.json must expose the access-rule permission summary static script'
)

assert.ok(
  rulesTable.includes('data-testid="dcc-access-rule-permission-summary"'),
  'rules table must expose a stable permission summary test id'
)
assert.ok(
  rulesTable.includes('class="access-rule-permission-summary access-rule-permission-summary--spread"'),
  'rules table must use the spread permission summary layout container'
)
assert.ok(
  !accessRulePage.includes('data-testid="dcc-access-rule-permission-help"'),
  'page header must remove the red-box help copy test id'
)
assert.ok(
  !accessRulePage.includes('data-testid="dcc-access-rule-header-context"'),
  'page header must remove the red-box selected-directory context test id'
)
assert.ok(
  !accessRulePage.includes(VIEW_EXPLANATION),
  'page header must remove the red-box view explanation'
)
assert.ok(
  !accessRulePage.includes(DOWNLOAD_EXPLANATION),
  'page header must remove the red-box download explanation'
)
assert.ok(
  !accessRulePage.includes(PREVIEW_EXPLANATION),
  'page header must not keep a standalone preview explanation'
)

assert.ok(rulesTable.includes(`label="${LABEL_PERMISSION_SUMMARY}"`), 'rules table must render the permission summary column')
assert.ok(!rulesTable.includes(`label="${LABEL_CHANGE_REASON}"`), 'rules table must not render the change-reason column')
assert.ok(!rulesTable.includes(INPUT_CHANGE_REASON), 'rules table must not render the per-row change-reason input')

for (const removedHeader of REMOVED_HEADERS) {
  assert.ok(
    !rulesTable.includes(`label="${removedHeader}"`),
    `rules table must not render the old standalone header: ${removedHeader}`
  )
}

for (const binding of ['row.canQuery', 'row.canDownload', 'row.active']) {
  assert.ok(rulesTable.includes(binding), `permission summary must keep the real binding: ${binding}`)
}
assert.ok(
  !rulesTable.includes('row.canPreview'),
  'permission summary must not expose a standalone preview toggle binding'
)
assert.ok(
  rulesTable.includes('handleQueryPermissionChange(row)'),
  'view toggle must synchronize the merged preview permission'
)
assert.ok(
  !rulesTable.includes(zh('\u9884\u89c8\u6743\u9650')),
  'rules table must not render a standalone preview switch label'
)
assert.ok(
  accessRulePage.includes('grid-template-columns: repeat(2, minmax(0, 1fr))'),
  'permission summary toggle layout must reserve only view and download columns'
)
assert.ok(
  !accessRulePage.includes('grid-template-columns: repeat(3, minmax(0, 1fr))'),
  'permission summary toggle layout must not reserve a stale preview column'
)

for (const token of TOKENS) {
  assert.ok(rulesTable.includes(token), `permission summary must render token: ${token}`)
}

for (const classToken of [
  'access-rule-permission-summary__status',
  'access-rule-permission-summary__toggles',
  'access-rule-permission-summary__toggle'
]) {
  assert.ok(
    accessRulePage.includes(classToken),
    `permission summary spread layout must keep class hook: ${classToken}`
  )
}

for (const behaviorToken of [
  'handleSubjectTypeChange(row)',
  'handleQueryPermissionChange(row)',
  'row.subjectId',
  'mergeRuleReadPermission',
  'changeReason',
  'removeRule(resolveRuleIndex(row))',
  'saveRules'
]) {
  assert.ok(accessRulePage.includes(behaviorToken), `access-rule behavior must be preserved: ${behaviorToken}`)
}

assert.ok(!/mock|placeholder data|fallback/i.test(rulesTable), 'permission summary must not introduce mock, placeholder, or fallback logic')
assert.ok(!rulesTable.includes(zh('\u964d\u7ea7')), 'permission summary must not introduce downgrade logic')
assert.ok(!rulesTable.includes(zh('\u541e\u5f02\u5e38')), 'permission summary must not introduce swallowed-exception logic')

console.log('PASS: DCC access rule permission summary static contract')

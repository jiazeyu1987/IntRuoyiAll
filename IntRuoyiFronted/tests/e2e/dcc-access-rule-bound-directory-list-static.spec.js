const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../..')
const readSource = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const packageJson = JSON.parse(readSource('package.json'))
const accessRulePage = readSource(
  'src/views/dcc/controlled-file/components/DirectoryAuthorizationTabPanel.vue'
)
const accessRuleApi = readSource('src/api/dcc/controlledFile/directories.ts')

const extractBetween = (source, startToken, endToken) => {
  const start = source.indexOf(startToken)
  assert.ok(start >= 0, `Missing start token: ${startToken}`)
  const end = source.indexOf(endToken, start)
  assert.ok(end > start, `Missing end token after ${startToken}: ${endToken}`)
  return source.slice(start, end)
}

assert.strictEqual(
  packageJson.scripts['e2e:dcc:access-rule-bound-directory-list:static'],
  'node tests/e2e/dcc-access-rule-bound-directory-list-static.spec.js',
  'package.json must expose the access-rule bound directory list static script'
)

assert.ok(
  accessRuleApi.includes('getAccessRuleDirectories'),
  'directory api must expose a bound access-rule directory list request'
)
assert.ok(
  accessRuleApi.includes('deleteDirectoryAccessRules'),
  'directory api must expose a delete-all access-rule request'
)

const leftPanel = extractBetween(accessRulePage, '<el-col :span="7">', '</el-col>')

assert.ok(
  !leftPanel.includes('<el-tree'),
  'left panel must no longer render the directory tree'
)
assert.ok(
  !leftPanel.includes('TreeExpandActions'),
  'left panel must no longer render expand/collapse tree actions'
)
assert.ok(
  leftPanel.includes('data-testid="dcc-access-rule-bound-directory-list"'),
  'left panel must expose a stable bound-directory list test id'
)
assert.ok(
  leftPanel.includes('data-testid="dcc-access-rule-bound-directory-item"'),
  'bound-directory list items must expose a stable test id'
)
assert.ok(
  leftPanel.includes('directory.directoryPath'),
  'left panel must render full bound directory paths'
)
assert.ok(
  leftPanel.includes('deleteBoundDirectory'),
  'left panel must provide inline delete for a bound directory'
)

assert.ok(
  accessRulePage.includes('新增目录'),
  'page toolbar must expose the add-directory action'
)
assert.ok(
  accessRulePage.includes('<el-tree-select'),
  'page must use tree-select for add-directory selection'
)
assert.ok(
  accessRulePage.includes('draftSelectedDirectoryId'),
  'page must track draft directory selection before save'
)
assert.ok(
  accessRulePage.includes('getAccessRuleDirectories'),
  'page must load bound directories from the dedicated API'
)
assert.ok(
  accessRulePage.includes('const boundDirectoryMap = computed(') &&
    accessRulePage.includes('parsePositiveRouteQueryId(item.id), item'),
  'page must derive bound-directory state only from the dedicated bound-directory list'
)
assert.ok(
  accessRulePage.includes('draftSelectedDirectoryId.value === selectedDirectoryId.value') &&
    accessRulePage.includes(
      '!boundDirectoryMap.value.has(parsePositiveRouteQueryId(selectedDirectoryId.value))'
    ),
  'draft state must only apply to selected directories that are absent from the bound-directory list'
)
assert.ok(
  !accessRulePage.includes('changeReason') || accessRulePage.includes('changeReason:'),
  'page must not infer manual binding state from rule change reasons'
)
assert.ok(
  !accessRulePage.includes('rules.value.some(') && !accessRulePage.includes('rules.value.every('),
  'page must not infer bound-directory state from the current rule rows'
)
assert.ok(
  accessRulePage.includes('未保存目录'),
  'page header context must indicate the unsaved draft directory state'
)
assert.ok(
  accessRulePage.includes('const boundDirectory = findBoundDirectoryByIdText(queryDirectoryId)') &&
    accessRulePage.includes('await selectDirectory(boundDirectory.id, false)') &&
    accessRulePage.includes('await selectDirectory(directory.id, true)'),
  'query-directory initialization must keep directories outside the bound list in draft mode even if rules load'
)
assert.ok(
  accessRulePage.includes(
    'if (boundDirectoryMap.value.has(parsePositiveRouteQueryId(directoryId)))'
  ) &&
    accessRulePage.includes('await selectDirectory(directoryId, false)') &&
    accessRulePage.includes('await selectDirectory(directoryId, true)'),
  'tree-select must treat directories outside the bound list as unsaved drafts before save'
)

for (const behaviorToken of [
  'addRule',
  'saveRules',
  'loadRules',
  'getDirectoryAccessRules',
  'row.canQuery',
  'row.canDownload',
  'row.active',
  'handleQueryPermissionChange(row)',
  'mergeRuleReadPermission'
]) {
  assert.ok(accessRulePage.includes(behaviorToken), `access-rule behavior must be preserved: ${behaviorToken}`)
}

assert.ok(!/mock|placeholder data|fallback/i.test(accessRulePage), 'page must not introduce mock, placeholder, or fallback logic')
assert.ok(!accessRulePage.includes('降级'), 'page must not introduce downgrade logic')
assert.ok(!accessRulePage.includes('吞异常'), 'page must not introduce swallowed-exception logic')

console.log('PASS: DCC access rule bound directory list static contract')

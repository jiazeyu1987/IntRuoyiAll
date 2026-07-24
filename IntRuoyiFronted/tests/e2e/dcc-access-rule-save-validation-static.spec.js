const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../..')
const readSource = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const accessRulePage = readSource(
  'src/views/dcc/controlled-file/components/DirectoryAuthorizationTabPanel.vue'
)

assert.ok(
  accessRulePage.includes('subjectId: 0'),
  'new rules must still start without an authorization object so the user explicitly chooses one'
)

assert.ok(
  accessRulePage.includes('const findInvalidRuleIndex = () =>'),
  'access-rule page must isolate invalid-row detection in a helper'
)

assert.ok(
  accessRulePage.includes('const buildInvalidRuleMessage = (invalidRuleIndex: number) =>'),
  'access-rule page must build a dedicated validation message for incomplete rules'
)

assert.ok(
  accessRulePage.includes('第 ${invalidRuleIndex + 1} 条规则未选择授权对象，请先选择授权对象或删除该规则后再保存'),
  'incomplete-rule warning must identify the specific row and explain how to recover'
)

assert.ok(
  accessRulePage.includes('const invalidRuleIndex = findInvalidRuleIndex()'),
  'saveRules must reuse the invalid-row helper before saving'
)

assert.ok(
  accessRulePage.includes('message.warning(buildInvalidRuleMessage(invalidRuleIndex))'),
  'saveRules must use the dedicated invalid-rule warning message'
)

assert.ok(
  accessRulePage.includes(
    "isDraftDirectory.value\n        ? '未保存目录至少新增一条规则后再保存'\n        : '当前目录没有规则，请使用左侧删除或先新增规则'"
  ),
  'saveRules must keep separate empty-rule guidance for draft directories versus saved directories'
)

assert.ok(
  accessRulePage.includes('await loadBoundDirectories()'),
  'saveRules must refresh the bound-directory list after successful save'
)

assert.ok(
  accessRulePage.includes('draftSelectedDirectoryId.value = undefined'),
  'saveRules must clear draft state after the directory is explicitly saved'
)

assert.ok(
  accessRulePage.includes('showDirectoryPicker.value = false'),
  'saveRules must close the add-directory picker after a successful explicit save'
)

assert.ok(
  accessRulePage.includes('await loadRules(selectedDirectoryId.value)'),
  'saveRules must reload the real persisted rules after saving'
)

assert.ok(
  !accessRulePage.includes("message.warning('请完善授权对象后再保存')"),
  'generic authorization-object warning must be replaced with a row-specific explanation'
)

console.log('PASS: DCC access rule save validation static contract')

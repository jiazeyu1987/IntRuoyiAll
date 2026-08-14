const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const readUtf8 = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const page = readUtf8('src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const pageWithoutComments = page.replace(/<!--[\s\S]*?-->/g, '')

const extractBlock = (source, startToken, endToken, message) => {
  const start = source.indexOf(startToken)
  assert.notEqual(start, -1, `${message}: missing start token ${startToken}`)
  const end = source.indexOf(endToken, start)
  assert.notEqual(end, -1, `${message}: missing end token ${endToken}`)
  return source.slice(start, end)
}

const personnelTabBlock = extractBlock(
  pageWithoutComments,
  '<el-tab-pane label="生产人员档案" name="productionPersonnel">',
  '<ContentWrap',
  'production personnel tab block'
)
const beforeListBlock = extractBlock(
  personnelTabBlock,
  '<el-tab-pane label="生产人员档案" name="productionPersonnel">',
  '<UnifiedListTemplate',
  'production personnel pre-list block'
)
const listTemplateBlock = extractBlock(
  personnelTabBlock,
  '<UnifiedListTemplate',
  '</UnifiedListTemplate>',
  'production personnel list template block'
)

assert.match(
  pageWithoutComments,
  /const\s+productionPersonnelAddDialogVisible\s*=\s*ref\(false\)/,
  'production personnel add dialog must use explicit visible state.'
)
assert.match(
  pageWithoutComments,
  /<el-dialog[\s\S]*data-team-leader-personnel-add-dialog[\s\S]*v-model="productionPersonnelAddDialogVisible"[\s\S]*<template\s+#header>[\s\S]*新增人员[\s\S]*<\/template>/,
  'production personnel add forms must be hosted by the 新增人员 dialog.'
)

const dialogBlock = extractBlock(
  pageWithoutComments,
  'data-team-leader-personnel-add-dialog',
  '</el-dialog>',
  'production personnel add dialog'
)

assert.match(
  listTemplateBlock,
  /<template\s+#extra-filters>[\s\S]*data-team-leader-open-personnel-dialog[\s\S]*新增人员[\s\S]*<\/template>/,
  '新增人员 button must render in the left-side list toolbar slot.'
)
assert.match(
  listTemplateBlock,
  /data-team-leader-open-personnel-dialog[\s\S]*@click="productionPersonnelAddDialogVisible = true"/,
  '新增人员 button must open the add-person dialog directly.'
)
assert.doesNotMatch(
  beforeListBlock,
  /<template\s+#header>\s*(搜索选择正式工|手动录入临时工)\s*<\/template>/,
  'formal and temporary add cards must no longer render inline before the personnel list.'
)
assert.doesNotMatch(
  beforeListBlock,
  /data-team-leader-formal-employee-select|data-team-leader-temporary-employee-form/,
  'add-person form controls must no longer be inline on the personnel page.'
)

assert.match(dialogBlock, /<template\s+#header>\s*搜索选择正式工\s*<\/template>/,
  'dialog must contain the formal employee add card.')
assert.match(dialogBlock, /data-team-leader-formal-employee-select/,
  'dialog must retain the scoped formal employee searchable select.')
assert.match(dialogBlock, /remote-method="searchFormalEmployeeCandidatesForSelect"/,
  'dialog formal employee select must keep backend-scoped remote search.')
assert.match(dialogBlock, /@click="submitLinkFormalEmployee"/,
  'dialog formal employee button must keep the existing submit handler.')
assert.match(dialogBlock, /<template\s+#header>\s*手动录入临时工\s*<\/template>/,
  'dialog must contain the temporary employee add card.')
assert.match(dialogBlock, /data-team-leader-temporary-employee-form/,
  'dialog must retain the temporary employee form.')
assert.match(dialogBlock, /v-model="temporaryEmployeeForm\.signaturePassword"/,
  'dialog temporary employee form must keep signature password input.')
assert.match(dialogBlock, /@click="submitCreateTemporaryEmployee"/,
  'dialog temporary employee button must keep the existing submit handler.')

console.log('PASS: production personnel add dialog static contract')

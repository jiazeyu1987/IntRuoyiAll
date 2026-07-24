const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const routerHelper = read('src/utils/routerHelper.ts')
const tagsViewStore = read('src/store/modules/tagsView.ts')
const feedbackPage = read('src/views/mes/pro/feedback/index.vue')
const edhrApprovalPage = read('src/views/mes/pro/edhr/ApprovalPage.vue')
const dccApprovalTasksPage = read('src/views/dcc/controlled-file/approval-tasks/index.vue')

const assertSetContains = (source, setName, values) => {
  const setPattern = new RegExp(`const\\s+${setName}\\s*=\\s*new Set\\(\\[([\\s\\S]*?)\\]\\)`)
  const match = source.match(setPattern)
  assert.ok(match, `${setName} must be declared as an explicit route identity set`)
  for (const value of values) {
    assert.match(match[1], new RegExp(`['"]${value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}['"]`))
  }
}

assert.match(feedbackPage, /route\.query\.tab/, 'feedback page must still accept tab query state')
assert.match(feedbackPage, /route\.query\.status/, 'feedback page must still accept status quick filter query')
assert.match(feedbackPage, /route\.query\.feedbackId/, 'feedback page must still accept feedbackId quick filter query')
assert.match(feedbackPage, /route\.query\.importRecordId/, 'feedback page must still accept importRecordId quick filter query')
assert.match(
  feedbackPage,
  /\(\)\s*=>\s*route\.fullPath/,
  'feedback page reacts to fullPath, so TagsView identity must ignore query-only page state'
)

assertSetContains(routerHelper, 'MES_FEEDBACK_ROUTE_PATHS', [
  'mes/pro/feedback',
  'pro/feedback'
])
assertSetContains(routerHelper, 'MES_FEEDBACK_ROUTE_COMPONENTS', [
  'mes/pro/feedback',
  'mes/pro/feedback/index'
])
assert.match(
  routerHelper,
  /MES_FEEDBACK_ROUTE_PATHS\.has\(routePath\)[\s\S]*MES_FEEDBACK_ROUTE_COMPONENTS\.has\(componentPath\)[\s\S]*meta\.tagsViewKeyMode\s*=\s*'path'/,
  'dynamic MES feedback route must use path tag identity so query filters reuse one TagsView tab'
)

assert.match(edhrApprovalPage, /route\.query\.tab/, 'eDHR approval shell still reads pending/done tab query')
assert.match(
  edhrApprovalPage,
  /router\.replace[\s\S]{0,260}path:\s*'\/approval-center'[\s\S]{0,220}moduleCode:\s*'EDHR'/,
  'eDHR approval shell must redirect to unified approval center'
)
assert.match(
  dccApprovalTasksPage,
  /router\.replace[\s\S]{0,220}path:\s*'\/approval-center'[\s\S]{0,220}moduleCode:\s*'DCC'/,
  'DCC legacy approval shell must redirect to unified approval center'
)

assertSetContains(routerHelper, 'APPROVAL_CENTER_REDIRECT_SHELL_ROUTE_PATHS', [
  'mes/pro/feedback/edhr-approval',
  'pro/feedback/edhr-approval',
  'dcc/controlled-file/approval-tasks',
  'controlled-file/approval-tasks'
])
assertSetContains(routerHelper, 'APPROVAL_CENTER_REDIRECT_SHELL_ROUTE_COMPONENTS', [
  'mes/pro/edhr/ApprovalPage',
  'dcc/controlled-file/approval-tasks',
  'dcc/controlled-file/approval-tasks/index'
])
assert.match(
  routerHelper,
  /APPROVAL_CENTER_REDIRECT_SHELL_ROUTE_PATHS\.has\(routePath\)[\s\S]*APPROVAL_CENTER_REDIRECT_SHELL_ROUTE_COMPONENTS\.has\(componentPath\)[\s\S]*meta\.noTagsView\s*=\s*true/,
  'legacy approval redirect shells must not create their own TagsView entries before replacing to approval center'
)
assert.match(
  tagsViewStore,
  /if\s*\(\s*view\.meta\?\.noTagsView\s*\)\s*return/,
  'TagsView store must honor noTagsView on redirect shell routes'
)

console.log('PASS: remaining query-only routes reuse or skip duplicate TagsView tabs')

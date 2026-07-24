const fs = require('fs')
const path = require('path')
const assert = require('assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const accessRulePagePath = path.join(
  repoRoot,
  'src',
  'views',
  'dcc',
  'controlled-file',
  'components',
  'DirectoryAuthorizationTabPanel.vue'
)

const accessRulePage = fs.readFileSync(accessRulePagePath, 'utf8')

assert(
  accessRulePage.includes('const DEFAULT_DRAFT_RULE_DEPT_NAME = \'QA\''),
  'draft directories must define QA as the default initial department rule'
)

assert(
  accessRulePage.includes('const createDefaultDraftRule = (): ControlledFileDirectoryAccessRuleVO | undefined =>'),
  'page must provide a dedicated factory for the default draft rule'
)

assert(
  accessRulePage.includes("const defaultDept = depts.value.find((item) => item.name === DEFAULT_DRAFT_RULE_DEPT_NAME)"),
  'default draft rule must resolve the QA department from the live department options'
)

assert(
  accessRulePage.includes("subjectType: 'DEPT'") &&
    accessRulePage.includes('subjectId: defaultDept.id'),
  'default draft rule must initialize as a DEPT rule bound to QA'
)

assert(
  accessRulePage.includes('const loadDraftRules = async (directoryId: number) =>') &&
    accessRulePage.includes('rules.value = defaultRule ? [defaultRule] : []'),
  'draft directories must initialize with only the default QA rule instead of loading inherited rules'
)

assert(
  accessRulePage.includes('await loadDraftRules(directoryId)'),
  'draft directory selection must route through the draft-only rule initializer'
)

assert(
  accessRulePage.includes('await loadRules(directoryId)'),
  'bound directories must continue loading their persisted rules'
)

console.log('dcc-access-rule-draft-default-qa-static: PASS')

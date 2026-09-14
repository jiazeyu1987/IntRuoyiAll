const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const readSource = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const workflowApi = readSource('src/api/dcc/controlledFile/workflow.ts')
const lifecycle = readSource('src/views/dcc/controlled-file/shared/lifecycle.ts')
const detailPage = readSource('src/views/dcc/controlled-file/detail/index.vue')
const presentation = readSource('src/views/dcc/controlled-file/detail/presentation.ts')
const browserPage = readSource('src/views/dcc/controlled-file/browser/index.vue')
const remainingRoutes = readSource('src/router/modules/remaining.ts')

assert.match(
  workflowApi,
  /export interface ControlledFilePublishReqVO/,
  'DCC publish API must expose a typed publish request contract.'
)
assert.match(
  workflowApi,
  /publishControlledFile[\s\S]*Promise<FormInstanceVO>/,
  'DCC publish API must return a form-center instance.'
)
assert.match(
  workflowApi,
  /url:\s*`\/dcc\/controlled-files\/\$\{id\}\/publish`/,
  'DCC publish API must call the official publish route.'
)
assert.match(lifecycle, /'READY_TO_PUBLISH'/, 'DCC lifecycle must expose the pending publish state.')
assert.match(
  lifecycle,
  /mapDccControlledFileProjection/,
  'DCC lifecycle must adapt backend allowedActions into the shared action projection contract.'
)
assert.match(
  presentation,
  /canPublish:\s*boolean/,
  'DCC detail action projection must include publish.'
)
assert.match(
  presentation,
  /resolveDccDetailActionProjection\(detail,\s*'PUBLISH'\)/,
  'DCC publish availability must come from the backend action projection.'
)
assert.match(
  detailPage,
  /actionCode:\s*'PUBLISH'/,
  'DCC publish dialog must create a platform PUBLISH business action context.'
)
assert.match(
  detailPage,
  /startUserSelectAssignees/,
  'DCC publish form data must carry starter-selected approvers.'
)
assert.match(
  detailPage,
  /resolveBusinessAction/,
  'DCC publish dialog must resolve the form-center action policy before submission.'
)
assert.match(
  detailPage,
  /getProcessDefinition\(undefined,\s*resolution\.bpmProcessKey\)/,
  'DCC publish dialog must resolve the BPM definition from the form-center policy key.'
)
assert.match(
  detailPage,
  /UserSelectV2[\s\S]*v-model="publishDialog\.startUserSelectAssignees\[task\.id\]"/,
  'DCC publish dialog must render a visible user selector for starter-selected approvers.'
)
assert.match(
  detailPage,
  /canSubmitPublishAction/,
  'DCC publish entry must be gated by shared backend projection.'
)
assert.match(
  detailPage,
  /instance\.status === 'EFFECTIVE'[\s\S]*当前版本已正式发布[\s\S]*发布申请已提交，等待审批通过后生效/,
  'DCC publish success copy must distinguish direct activation from BPM approval.'
)
assert.doesNotMatch(
  detailPage,
  /smokeappr1|smokeplan1|91451\d/,
  'DCC publish frontend must not hard-code E2E approver usernames or user ids.'
)
assert.match(
  browserPage,
  /getSelectedVersion\(row\)\.status === 'READY_TO_PUBLISH'[\s\S]*data-testid="dcc-controlled-browser-publish"[\s\S]*@click="openManagement\(getSelectedVersion\(row\)\.id\)"[\s\S]*发布/,
  'READY_TO_PUBLISH browser versions must expose a visible publish-management entry.'
)
assert.match(
  browserPage,
  /const openManagement = \(id: number \| string\)[\s\S]*management: '1'[\s\S]*from: 'browser'[\s\S]*returnTo:/,
  'The browser publish entry must open an explicit management route with a safe return path.'
)
assert.match(
  remainingRoutes,
  /const isBrowserManagement =[\s\S]*String\(to\.query\.management \|\| ''\) === '1'[\s\S]*String\(to\.query\.from \|\| ''\) === 'browser'[\s\S]*Boolean\(String\(to\.query\.returnTo \|\| ''\)\)/,
  'The hidden DCC detail route must explicitly allow browser management navigation.'
)
assert.match(
  remainingRoutes,
  /isApprovalHandling[\s\S]{0,100}\|\|[\s\S]{0,100}isBrowserTraceability[\s\S]{0,100}\|\|[\s\S]{0,100}isBrowserManagement/,
  'The detail route guard must admit the explicit browser management mode.'
)

for (const file of [
  'src/api/dcc/controlledFile/workflow.ts',
  'src/api/form-center/instance.ts',
  'src/views/dcc/controlled-file/detail/index.vue',
  'src/views/dcc/controlled-file/detail/presentation.ts'
]) {
  const content = readSource(file)
  if (/catch\s*\([^)]*\)\s*\{\s*\}/.test(content) || /catch\s*\{\s*\}/.test(content)) {
    throw new Error(`Empty catch is not allowed in ${file}`)
  }
}

console.log('PASS: DCC publish form-center frontend static contract')

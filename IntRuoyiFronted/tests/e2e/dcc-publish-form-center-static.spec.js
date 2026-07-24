const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const readSource = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const workflowApi = readSource('src/api/dcc/controlledFile/workflow.ts')
const lifecycle = readSource('src/views/dcc/controlled-file/shared/lifecycle.ts')
const detailPage = readSource('src/views/dcc/controlled-file/detail/index.vue')
const presentation = readSource('src/views/dcc/controlled-file/detail/presentation.ts')

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
  /发布申请已提交，等待审批通过后生效/,
  'DCC publish success copy must not claim immediate activation.'
)
assert.doesNotMatch(
  detailPage,
  /smokeappr1|smokeplan1|91451\d/,
  'DCC publish frontend must not hard-code E2E approver usernames or user ids.'
)
assert.doesNotMatch(
  detailPage,
  /当前版本已发布/,
  'DCC publish submit must not show an active terminal result before approval.'
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

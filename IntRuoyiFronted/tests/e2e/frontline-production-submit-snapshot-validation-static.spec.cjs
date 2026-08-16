const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const workspaceRoot = path.resolve(frontendRoot, '..')
const panel = fs
  .readFileSync(path.join(frontendRoot, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'), 'utf8')
  .replace(/\r\n/g, '\n')
const feedbackApi = fs
  .readFileSync(path.join(frontendRoot, 'src/api/mes/pro/feedback/index.ts'), 'utf8')
  .replace(/\r\n/g, '\n')
const backendAuthorization = fs
  .readFileSync(path.join(
    workspaceRoot,
    'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlineSubmitAuthorizationServiceImpl.java'
  ), 'utf8')
  .replace(/\r\n/g, '\n')

const extractFunctionBlock = (source, name) => {
  const asyncStart = source.indexOf(`const ${name} = async`)
  const normalStart = source.indexOf(`const ${name} = (`)
  const start = [asyncStart, normalStart]
    .filter((index) => index >= 0)
    .sort((a, b) => a - b)[0]
  assert.ok(start >= 0, `missing function ${name}`)
  const openIndex = source.indexOf('{', start)
  assert.ok(openIndex > start, `missing function body ${name}`)
  let depth = 0
  for (let index = openIndex; index < source.length; index += 1) {
    const char = source[index]
    if (char === '{') depth += 1
    if (char === '}') {
      depth -= 1
      if (depth === 0) {
        return source.slice(openIndex + 1, index)
      }
    }
  }
  assert.fail(`unterminated function ${name}`)
}

const snapshotAssertBlock = extractFunctionBlock(panel, 'assertProductionSubmitSnapshotContext')
assert.match(
  snapshotAssertBlock,
  /const selectedProcess = deviceState\.selectedProcess[\s\S]*const selectedEmployee = deviceState\.selectedEmployee[\s\S]*const snapshotContext = deviceState\.runtimeConfig\?\.productionSubmitContext/,
  'submit snapshot validation must read the current selected process, selected employee, and runtime snapshot context.'
)
assert.match(
  snapshotAssertBlock,
  /!isFrontlineProductionProcess\(selectedProcess\)[\s\S]*当前提交快照缺少正式生产工序/,
  'submit snapshot validation must fail fast when the current selection is not a formal production process.'
)
assert.match(
  snapshotAssertBlock,
  /!snapshotContext[\s\S]*当前提交快照缺少正式运行配置/,
  'submit snapshot validation must fail fast when no runtime snapshot is available.'
)
assert.match(
  snapshotAssertBlock,
  /formalContext\.routeId !== selectedProcess\.routeId[\s\S]*formalContext\.routeProcessId !== selectedProcess\.routeProcessId[\s\S]*formalContext\.processId !== selectedProcess\.processId[\s\S]*当前提交快照与所选工序不一致/,
  'submit snapshot validation must compare submit context route/process identity against the selected process snapshot.'
)
assert.match(
  snapshotAssertBlock,
  /snapshotContext\.routeId !== formalContext\.routeId[\s\S]*snapshotContext\.routeProcessId !== formalContext\.routeProcessId[\s\S]*snapshotContext\.processId !== formalContext\.processId[\s\S]*当前提交快照与运行配置不一致/,
  'submit snapshot validation must compare the formal submit context against the runtime-config snapshot.'
)
assert.match(
  snapshotAssertBlock,
  /selectedEmployee\.userId !== formalContext\.signatureEmployeeId[\s\S]*当前提交快照与所选员工不一致/,
  'submit snapshot validation must compare the submit signer against the selected employee snapshot.'
)

const buildPayloadBlock = extractFunctionBlock(panel, 'buildFrontlineFormalSubmitPayload')
assert.match(
  buildPayloadBlock,
  /const formalContext = readFrontlineFormalSubmitContext\(\)[\s\S]*assertProductionSubmitSnapshotContext\(formalContext\)[\s\S]*assertFrontlineFormalSubmitContext\(formalContext\)/,
  'formal submit payload must run snapshot identity validation before regular required-field validation.'
)
assert.match(
  feedbackApi,
  /export interface ProFrontlineFeedbackSubmitReqVO \{[\s\S]*frontlineSessionSnapshotId:\s*string[\s\S]*frontlineSessionSnapshotHash:\s*string[\s\S]*\}/,
  'formal submit API contract must require the server-issued fullscreen snapshot id and hash.'
)
assert.match(
  buildPayloadBlock,
  /const runtimeConfig = deviceState\.runtimeConfig![\s\S]*frontlineSessionSnapshotId:\s*runtimeConfig\.frontlineSessionSnapshotId[\s\S]*frontlineSessionSnapshotHash:\s*runtimeConfig\.frontlineSessionSnapshotHash/,
  'formal submit payload must send the id and hash from the selected process runtime snapshot.'
)
assert.doesNotMatch(
  buildPayloadBlock,
  /ProFeedbackApi\.getFrontlineRuntimeConfig|switchFrontlineActualEmployee/,
  'formal submit must not refresh runtime config or switch employee while validating the snapshot.'
)
assert.match(
  backendAuthorization,
  /sessionSnapshotService\.require\(command\.frontlineSessionSnapshotId\(\),[\s\S]*command\.frontlineSessionSnapshotHash\(\),[\s\S]*command\.loginUserId\(\)\)/,
  'backend submit authorization must load the original server-issued snapshot by id and hash.'
)
assert.doesNotMatch(
  backendAuthorization,
  /contextService\.requireAuthorizedProcess|contextService\.requireTeamEmployee|templateResolver\.resolve/,
  'backend submit authorization must not replace snapshot validation with submit-time live process, employee, or template reads.'
)

console.log('PASS: frontline production submit snapshot validation static contract')

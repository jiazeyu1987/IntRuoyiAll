const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '..', '..')
const workspaceRoot = path.resolve(frontendRoot, '..')
const panel = fs.readFileSync(
  path.join(frontendRoot, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'),
  'utf8'
)
const api = fs.readFileSync(
  path.join(frontendRoot, 'src/api/mes/pro/feedback/index.ts'),
  'utf8'
)
const service = fs.readFileSync(
  path.join(
    workspaceRoot,
    'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlinePqcContextServiceImpl.java'
  ),
  'utf8'
)
const requestVo = fs.readFileSync(
  path.join(
    workspaceRoot,
    'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/feedback/vo/frontline/MesFrontlinePqcSubmitReqVO.java'
  ),
  'utf8'
)
const realFlow = fs.readFileSync(
  path.join(frontendRoot, 'tests/e2e/role-requirement-matrix-real-flow.e2e.js'),
  'utf8'
)

const submitInterface = api.match(
  /export interface FrontlinePqcInspectionSubmitReqVO \{([\s\S]*?)\n\}/
)?.[1] || ''
for (const clientSourceField of ['deviceAccountId', 'deviceId', 'workstationId']) {
  assert.doesNotMatch(
    submitInterface,
    new RegExp(`\\b${clientSourceField}\\b`),
    `PQC frontend submit contract must not accept client-owned ${clientSourceField}.`
  )
}

const payloadBuilder = panel.match(
  /const buildPqcInspectionSubmitPayload = \([\s\S]*?\n\}\n\n/
)?.[0] || ''
assert.doesNotMatch(
  payloadBuilder,
  /productionSubmitEventId\s*:/,
  'PQC payload builder must not let employees manually bind a production submit event.'
)
for (const clientInference of [
  /userStore\.getUser/,
  /activeProductionDevice/,
  /process\?\.deviceId/,
  /process\?\.workstationId/,
  /missingFormalContext\.push\('deviceAccountId'\)/,
  /missingFormalContext\.push\('deviceId'\)/,
  /missingFormalContext\.push\('workstationId'\)/
]) {
  assert.doesNotMatch(
    payloadBuilder,
    clientInference,
    'PQC payload builder must use the production event root instead of inferring source device context.'
  )
}

for (const serverOwnedField of ['deviceAccountId', 'deviceId', 'workstationId']) {
  assert.doesNotMatch(
    requestVo,
    new RegExp(`private Long ${serverOwnedField};`),
    `PQC request VO must not expose server-owned ${serverOwnedField}.`
  )
}

assert.match(
  service,
  /resolveUniqueProductionSubmitEvent\(activeOrder,\s*task\)/
)
assert.match(
  service,
  /MesProProcessPoolEventDO\.EVENT_TYPE_PRODUCTION_SUBMIT/
)
assert.match(service, /command\.setProductionSubmitEventId\(productionSubmit\.eventId\(\)\)/)

assert.match(
  realFlow,
  /function buildPqcFillUrl[\s\S]*appendQueryValue\(query,\s*'routeProcessId',\s*context\.routeProcessId\)[\s\S]*appendQueryValue\(query,\s*'processId',\s*context\.processId\)/,
  'PQC fill URL must keep the exact route process and process from the production submit event context.'
)
assert.match(
  realFlow,
  /async function selectFirstFormalPqcOption[\s\S]*option:not\(\[value=""\]\)[\s\S]*selectOption/,
  'PQC real E2E must select a non-placeholder formal option from each required project-level select.'
)
assert.match(
  realFlow,
  /async function completePqcPieceDetailsForSubmission[\s\S]*selectFirstFormalPqcOption\([\s\S]*data-pqc-equipment-select[\s\S]*selectFirstFormalPqcOption\([\s\S]*data-pqc-equipment-number-select/,
  'PQC real E2E must select project-level equipment and equipment number before formal submission.'
)
assert.match(
  realFlow,
  /function collectConfiguredProductionSignatureIds[\s\S]*productionExtra\\d\+[\s\S]*function resolveNextProductionSignatureId/,
  'Repeated production-source preparation must use a dedicated configured production signature pool.'
)
assert.match(
  realFlow,
  /const reservedProductionSignatureIds = new Set\(\)[\s\S]*reservedProductionSignatureIds\.add\(signatureId\)/,
  'Production signature allocation must reserve each selected ID for the remainder of the current real E2E run.'
)
const productionContextResolver = realFlow.match(
  /async function resolveProductionSubmitContextForPqcPrereq[\s\S]*?(?=\nasync function preparePqcFormalSubmissionContext)/
)?.[0] || ''
assert.match(
  productionContextResolver,
  /actionEvidence\.find\(\(item\)\s*=>\s*item\.key === 'pqcRegulationItemsRendered'[\s\S]*item\.status === 'PASS'\)/,
  'Production source preparation must read the exact pending PQC process frozen by pqcRegulationItemsRendered.'
)
assert.match(
  productionContextResolver,
  /loadProductionProcessForPqcPrereq\(page,\s*config,\s*regulationEvidence\)/,
  'Production source preparation must resolve runtime context from the frozen PQC process identity.'
)
assert.doesNotMatch(
  productionContextResolver,
  /config\.primaryRouteProcessId/,
  'Production source preparation must not fall back to the configured primary route process.'
)
assert.match(
  productionContextResolver,
  /resolveNextProductionSignatureId\(config,\s*'productionEmployee'\)/,
  'Each production source event must request the next configured production signature ID.'
)
assert.doesNotMatch(
  productionContextResolver,
  /requireSignatureId\(config,\s*'productionEmployee'\)/,
  'Production source preparation must not reuse one fixed signature ID across multiple events.'
)

console.log('PASS: PQC submission inherits device context from the exact production submit event')

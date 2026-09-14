const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const workspaceRoot = path.resolve(__dirname, '../../..')
const read = (relativePath) => fs.readFileSync(path.join(workspaceRoot, relativePath), 'utf8')

const panel = read('IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')
const feedbackApi = read('IntRuoyiFronted/src/api/mes/pro/feedback/index.ts')
const revisionApi = read('IntRuoyiFronted/src/api/mes/pro/processpool/eventRevision.ts')
const leader = read('IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const backendSubmitVo = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/feedback/vo/frontline/MesFrontlinePqcSubmitReqVO.java'
)
const backendSubmitCommand = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlinePqcSubmitCommand.java'
)
const backendSubmitController = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/feedback/MesFrontlineDeviceAccountController.java'
)
const backendCorrectionVo = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/vo/ProcessPoolPqcInspectionCorrectionReqVO.java'
)
const backendCorrectionCommand = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesProcessPoolPqcInspectionCorrectionCommand.java'
)
const backendSubmitService = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlinePqcContextServiceImpl.java'
)
const canonicalV2 = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/CanonicalPqcSubmissionV2.java'
)

assert.doesNotMatch(panel, /data-pqc-defect-description|frontline-pqc-defect-description/)
assert.doesNotMatch(panel, /defectDescription|nonconformanceDescription/)

const submitInterface = feedbackApi.match(
  /export interface FrontlinePqcInspectionSubmitReqVO \{([\s\S]*?)\n\}/
)?.[1] || ''
assert.doesNotMatch(submitInterface, /nonconformanceDescription|defectDescription/)
assert.doesNotMatch(revisionApi, /nonconformanceDescription|defectDescription/)

assert.doesNotMatch(leader, /isSubmissionColumnVisible\(['\"]defectDescription['\"]\)/)
assert.doesNotMatch(leader, /pqcNonconformanceDescription|nonconformanceDescription|defectDescription/)
for (const [source, label] of [
  [backendSubmitVo, 'backend PQC submit request'],
  [backendSubmitCommand, 'backend PQC submit command'],
  [backendSubmitController, 'backend PQC submit controller mapping'],
  [backendCorrectionVo, 'backend PQC correction request'],
  [backendCorrectionCommand, 'backend PQC correction command']
]) {
  assert.doesNotMatch(
    source,
    /nonconformanceDescription|defectDescription/,
    label + ' must not expose the removed defect description field.'
  )
}
assert.match(
  backendSubmitService,
  /CanonicalPqcSubmissionV2[.]hash[(]task,[ ]*command,[ ]*pieceDetails[)]/,
  'new PQC submissions must use the canonical hash without defect descriptions.'
)
assert.match(
  backendSubmitService,
  /CanonicalPqcSubmissionV1[.]hash[\s\S]*readLegacyNonconformanceDescription[(]event[)][\s\S]*pieceDetails/,
  'submitted historical tasks must retain explicit V1 hash compatibility.'
)
assert.match(
  backendSubmitService,
  /payload[.]remove[(]"nonconformanceDescription"[)][\s\S]*payload[.]remove[(]"defectDescription"[)]/,
  'new PQC event payloads must strip legacy defect description keys.'
)
assert.doesNotMatch(
  canonicalV2,
  /nonconformanceDescription|defectDescription/,
  'canonical V2 hash must not include defect descriptions.'
)
const productionStart = panel.indexOf('data-frontline-production-operator')
const productionEnd = panel.indexOf('</div>', productionStart)
assert.ok(productionStart >= 0 && productionEnd > productionStart, 'production operator block must remain present.')
const productionTemplate = panel.slice(productionStart, panel.length)
assert.match(
  productionTemplate,
  /frontline-production-defect-section[\s\S]*configuredDefectReasons/,
  'production mode must retain its independent defect quantity section.'
)
assert.match(
  panel,
  /lossDetails:\s*buildProductionLossDetailsPayload\(\)/,
  'production submit payload must retain structured defect quantity details.'
)

console.log('PASS frontline PQC defect description removal static contract')

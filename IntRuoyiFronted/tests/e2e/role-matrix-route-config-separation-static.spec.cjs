const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const workspaceRoot = path.resolve(__dirname, '../../..')
const frontendRoot = path.resolve(workspaceRoot, 'IntRuoyiFronted')
const backendRoot = path.resolve(workspaceRoot, 'IntRuoyiBackend')
const read = (file) => fs.readFileSync(file, 'utf8').replace(/\r\n/g, '\n')

const routeDesigner = read(path.join(frontendRoot, 'src/views/mes/pro/route/RouteFlowGraphDesigner.vue'))
const batchExecutionService = read(path.join(
  backendRoot,
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionServiceImpl.java'
))

const normalizeSlotBlock = routeDesigner.match(
  /const normalizeRecordBindingSlotType = \([\s\S]*?\n\}/
)
assert.ok(normalizeSlotBlock, 'RouteFlowGraphDesigner must keep an explicit record-binding slot resolver.')
assert.doesNotMatch(
  normalizeSlotBlock[0],
  /return\s+['"]MAIN['"]/,
  'M5 forbids defaulting a missing route form slot to MAIN in normalizeRecordBindingSlotType.'
)
assert.match(
  normalizeSlotBlock[0],
  /return resolveRecordBindingSlotType\(formSlotType, formBindingKey\)/,
  'Missing route form slot must be delegated to the non-fallback resolver.'
)
const explicitResolverBlock = routeDesigner.match(/const resolveRecordBindingSlotType = \([\s\S]*?\n\}/)
assert.ok(explicitResolverBlock, 'RouteFlowGraphDesigner must keep the explicit non-fallback slot resolver.')
assert.match(
  explicitResolverBlock[0],
  /return undefined/,
  'Missing route form slot must remain unresolved so callers can fail fast or skip invalid formBindings.'
)

const sharedKeyBlock = routeDesigner.match(/const buildSharedRecordBindingKey = \([\s\S]*?\n\}/)
assert.ok(sharedKeyBlock, 'Shared route form binding key builder must exist.')
assert.match(
  sharedKeyBlock[0],
  /resolveRecordBindingSlotType\(binding\.formSlotType, binding\.formBindingKey\)/,
  'Shared route form binding keys must use the non-fallback slot resolver.'
)
assert.match(
  sharedKeyBlock[0],
  /if \(!formSlotType\) return null/,
  'Shared route form binding keys must not fabricate MAIN when slot metadata is absent.'
)

const additionalCountBlock = routeDesigner.match(/const getRouteNodeAdditionalFormCount = \(node: RouteFlowNodeVO\) => \{[\s\S]*?\n\}/)
assert.ok(additionalCountBlock, 'Route node additional form count helper must exist.')
assert.doesNotMatch(
  additionalCountBlock[0],
  /normalizeRecordBindingSlotType/,
  'Route node form-slot status must not use the old default-MAIN normalizer.'
)
assert.match(
  additionalCountBlock[0],
  /resolveRecordBindingSlotType\(binding\.formSlotType, binding\.formBindingKey\)/,
  'Route node form-slot status must use explicit slot resolution.'
)

const batchRecordValueBlock = routeDesigner.match(/const buildBatchRecordFormValue = \(\) => \{([\s\S]*?)\n\}/)
assert.ok(batchRecordValueBlock, 'Batch record form field must have a dedicated value builder.')
assert.doesNotMatch(
  batchRecordValueBlock[1],
  /selectedRecordBindings|getRecordBindingsBySlotType|formBindings/,
  'Batch record form value must remain isolated from formBindings.'
)

const runtimeSlotResolver = batchExecutionService.match(/private String resolveRouteFormSlotType\(String formSlotType\) \{[\s\S]*?\n    \}/)
assert.ok(runtimeSlotResolver, 'Backend eDHR runtime must keep a route form slot resolver.')
assert.doesNotMatch(
  runtimeSlotResolver[0],
  /blankToDefault\([\s\S]*FORM_SLOT_MAIN/,
  'Backend eDHR runtime must not default missing formSlotType to MAIN.'
)
assert.match(
  runtimeSlotResolver[0],
  /throw exception\(/,
  'Backend eDHR runtime must fail fast when route form slot type is missing or invalid.'
)

console.log('PASS role-matrix route config separation static contract')
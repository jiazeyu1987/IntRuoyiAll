const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const graphComponent = read('src/views/mes/pro/route/RouteFlowGraphDesigner.vue')
const formContent = read('src/views/mes/pro/route/RouteFormContent.vue')

const assertIncludes = (content, expected, label) => {
  if (!content.includes(expected)) {
    throw new Error(`${label} missing: ${expected}`)
  }
}

const assertNotIncludes = (content, expected, label) => {
  if (content.includes(expected)) {
    throw new Error(`${label} must not include: ${expected}`)
  }
}

const indexOfRequired = (content, expected, label) => {
  const index = content.indexOf(expected)
  if (index === -1) {
    throw new Error(`${label} missing: ${expected}`)
  }
  return index
}

assertNotIncludes(graphComponent, '保存关系图', 'flow graph toolbar standalone save action')
assertNotIncludes(graphComponent, '@click="handleSave"', 'flow graph toolbar standalone save click handler')
assertIncludes(graphComponent, 'validateBeforeSubmit', 'flow graph exposes pre-submit validation')
assertIncludes(graphComponent, 'saveFromParent', 'flow graph exposes parent-driven save')
assertIncludes(
  graphComponent,
  'ProRouteApi.validateRouteProcessFlowGraph(buildPayload())',
  'flow graph validates via existing validate API'
)
assertIncludes(
  graphComponent,
  'ProRouteApi.saveRouteProcessFlowGraph(buildPayload())',
  'flow graph saves via existing save API'
)
assertIncludes(graphComponent, '需点击顶部保存后才会写入', 'linear draft confirm points to toolbar save')
assertIncludes(graphComponent, '请点击顶部保存后生效', 'linear draft toast points to toolbar save')
assertIncludes(graphComponent, 'data-flow-action="save-route-flow"', 'flow graph toolbar save action')
assertIncludes(
  graphComponent,
  'defineExpose({ autoLayoutOnEntry, validateBeforeSubmit, saveFromParent })',
  'flow graph exposes only parent-driven save contract'
)
assertIncludes(graphComponent, 'applyDefaultKeyProcessLocally', 'flow graph defaults key process without implicit save')

assertIncludes(formContent, 'const shouldSaveFlowGraphOnSubmit', 'route form computes graph save eligibility')
assertIncludes(formContent, 'const requireFlowGraphDesigner', 'route form requires loaded flow graph component')
assertIncludes(formContent, 'validateFlowGraphBeforeSubmit', 'route form validates graph before main save')
assertIncludes(formContent, 'saveFlowGraphAfterRouteSave', 'route form saves graph after main save')
assertIncludes(formContent, 'requireFlowGraphDesigner().validateBeforeSubmit', 'route form calls graph validation after component check')
assertIncludes(formContent, 'requireFlowGraphDesigner().saveFromParent', 'route form calls graph save after component check')
assertIncludes(formContent, "const successMessage = formType.value === 'create' ? '新增成功' : '保存成功'", 'route form computes one final success toast')
assertIncludes(formContent, 'message.success(successMessage)', 'route form shows one final success toast')
assertNotIncludes(formContent, "message.success('修改成功')", 'route form must not show separate update success before graph save')

const submitStart = indexOfRequired(formContent, 'const submitForm = async () => {', 'submitForm start')
const submitEnd = indexOfRequired(formContent, 'const handleEnable = async () => {', 'submitForm end marker')
const submitBlock = formContent.slice(submitStart, submitEnd)
const loadGraphStart = indexOfRequired(graphComponent, 'const loadGraph = async () => {', 'loadGraph start')
const normalizeNodesStart = indexOfRequired(graphComponent, 'const normalizeNodes =', 'loadGraph end marker')
const loadGraphBlock = graphComponent.slice(loadGraphStart, normalizeNodesStart)

const formValidateIndex = indexOfRequired(submitBlock, 'await formRef.value.validate()', 'form validation order')
const shouldSaveIndex = indexOfRequired(submitBlock, 'const shouldSaveFlowGraph = shouldSaveFlowGraphOnSubmit()', 'graph save snapshot order')
const graphValidateIndex = indexOfRequired(submitBlock, 'await validateFlowGraphBeforeSubmit(shouldSaveFlowGraph)', 'graph validation order')
const updateIndex = indexOfRequired(submitBlock, 'await ProRouteApi.updateRoute(data)', 'main route update order')
const graphSaveIndex = indexOfRequired(submitBlock, 'await saveFlowGraphAfterRouteSave(shouldSaveFlowGraph)', 'graph save order')
const successIndex = indexOfRequired(submitBlock, 'message.success(successMessage)', 'success toast order')

if (!(formValidateIndex < shouldSaveIndex && shouldSaveIndex < graphValidateIndex && graphValidateIndex < updateIndex && updateIndex < graphSaveIndex && graphSaveIndex < successIndex)) {
  throw new Error('submitForm must run form validation -> graph validation -> main update -> graph save -> one success toast')
}

assertNotIncludes(loadGraphBlock, 'await updateRouteProcessKeyFlag', 'flow graph load must not write route process key flag')
assertNotIncludes(loadGraphBlock, 'await ensureDefaultKeyProcess', 'flow graph load must not call async default key save')

console.log('mes-route-bottom-save-flow-graph-static PASS')

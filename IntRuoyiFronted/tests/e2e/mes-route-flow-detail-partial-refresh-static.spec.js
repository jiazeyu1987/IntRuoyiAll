const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')
const component = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/route/RouteFlowGraphDesigner.vue'),
  'utf8'
)

const assertIncludes = (expected, label) => {
  if (!component.includes(expected)) {
    throw new Error(`${label} missing: ${expected}`)
  }
}

const assertNotIncludes = (expected, label) => {
  if (component.includes(expected)) {
    throw new Error(`${label} must not include: ${expected}`)
  }
}

const assertRegex = (pattern, label) => {
  if (!pattern.test(component)) {
    throw new Error(`${label} missing: ${pattern}`)
  }
}

assertNotIncludes(
  'v-loading="selectedProcessDetailLoading"',
  'selected process sidebar full loading mask'
)
assertIncludes(':aria-busy="', 'selected process sidebar accessibility loading state')
assertIncludes('selectedProcessDetailLoading', 'selected process detail loading contributes to aria busy')
assertIncludes('selectedProcessMachineryLoading', 'selected process machinery loading contributes to aria busy')
assertIncludes('selectedProcessAttributesLoading', 'selected process attributes loading contributes to aria busy')
assertNotIncludes('batchRecordLoading', 'batch record option loading is no longer part of read-only red-box detail')
assertIncludes('loading?: boolean', 'process detail field loading contract')
assertIncludes('selectedProcessMachineryLoading', 'machinery independent loading state')
assertIncludes('field.loading', 'field-level loading binding')
assertIncludes('<el-skeleton', 'field value loading skeleton')
assertIncludes('selectedProcessDetailRequestId', 'selected process request identity')
assertIncludes(
  'if (requestId !== selectedProcessDetailRequestId)',
  'stale process detail response guard'
)
assertIncludes(
  'data-flow-panel="selected-field-detail"',
  'selected field detail panel must not restore a full sidebar loading mask'
)
assertRegex(
  /const handleSelectProcessDetailField = \(fieldKey: ProcessDetailFieldKey\) => \{[\s\S]*?selectedProcessDetailFieldKey\.value = fieldKey[\s\S]*?\n\}/,
  'field detail click must only update field selection state'
)
const selectFieldHandler = component.match(
  /const handleSelectProcessDetailField = \(fieldKey: ProcessDetailFieldKey\) => \{([\s\S]*?)\n\}/
)
if (!selectFieldHandler) {
  throw new Error('field detail selection handler missing')
}
if (selectFieldHandler[1].includes('loadGraph(')) {
  throw new Error('field detail selection handler must not call loadGraph')
}
if (selectFieldHandler[1].includes('markGraphDraftChanged(')) {
  throw new Error('field detail selection handler must not mark graph dirty')
}
assertIncludes(
  'ProProcessApi.getProcess(node.processId, { routeId: props.routeId })',
  'process detail real API request'
)
assertIncludes(
  'ProProcessApi.getProcessMachineryList(node.processId)',
  'process machinery real API request'
)
assertNotIncludes(
  'const [processDetail, machineryList] = await Promise.all([',
  'shared process and machinery loading cycle'
)

assertIncludes(
  'const handleSelectProcessDetailField = (fieldKey: ProcessDetailFieldKey) => {',
  'field button click handler'
)
assertIncludes(
  'selectedProcessDetailFieldKey.value = fieldKey',
  'field button click only updates selected field state'
)
assertIncludes(
  'watch(selectedProcessDetailFieldKeys, (fieldKeys) => {',
  'removed fields clear stale selected field state'
)

const selectFieldHandlerBlock = selectFieldHandler[0]
for (const forbidden of ['loadGraph(', 'loadSelectedProcessDetail(', 'router.push(', 'window.location']) {
  if (selectFieldHandlerBlock.includes(forbidden)) {
    throw new Error(`field button click handler must not trigger refresh/navigation: ${forbidden}`)
  }
}

console.log('mes-route-flow-detail-partial-refresh-static PASS')

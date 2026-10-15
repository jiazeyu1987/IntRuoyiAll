const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const source = path.resolve(__dirname, '../../src/api/dcc/controlledFile/applicationRead.ts')
function load(rows, directResponse) {
  const calls = []
  const context = { exports: {}, Error, BigInt, Set, Date, require: name => {
    if (name === '@/config/axios') return { default: {
      get: async request => { calls.push(request); return directResponse === undefined ? { list: rows, total: rows.length } : directResponse },
      post: async request => { calls.push(request); return true }
    } }
    if (name.endsWith('project-attributes/state')) return { validateAttributes: value => value }
    throw new Error('Unexpected dependency: ' + name)
  } }
  vm.runInNewContext(ts.transpileModule(fs.readFileSync(source, 'utf8'),
    { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, context)
  return { api: context.exports, calls }
}
const current = '9223372036854775000'
const latest = '9223372036854775001'
const active = {
  tenantId: '1', id: current, masterId: '20', latestControlledFileId: latest,
  currentActiveControlledFileId: current, dccProjectCodeId: '5', projectName: '项目五',
  projectFolderId: '9', projectFolderName: '设计', fileName: 'SOP.docx',
  sourceOriginalFileName: 'SOP.docx', fileNumber: 'SOP-1', versionNo: 'A/1',
  status: 'ACTIVE', controlled: true, pendingEffect: false, executable: true, canPreview: false
}
const query = { keyword: '', pageNo: 1, pageSize: 20 }
test('selector accepts current active body while a newer controlled body is pending', async () => {
  const { api } = load([active])
  const result = await api.loadDccSelectorPage('1', query)
  assert.equal(result.list[0].controlledFileId, current)
  assert.equal(result.list[0].canPreview, false)
  assert.equal(result.list[0].pendingEffect, false)
})
test('default project browser selects current independently from latest', async () => {
  const { api } = load([active])
  const result = await api.loadDccProjectBrowserPage('1', query)
  assert.equal(result.list[0].controlledFileId, current)
  assert.equal(result.list[0].executable, true)
})
test('daily selector and browser reject missing, foreign or non-executable current facts', async () => {
  const invalidRows = [
    { ...active, currentActiveControlledFileId: undefined },
    { ...active, currentActiveControlledFileId: latest },
    { ...active, latestControlledFileId: current, status: 'CONTROLLED_PENDING_EFFECTIVE', pendingEffect: true, executable: false },
    { ...active, latestControlledFileId: current, controlled: false, executable: false },
    { ...active, latestControlledFileId: current, executable: false },
    { ...active, currentActiveControlledFileId: Number(current) }
  ]
  for (const row of invalidRows) {
    await assert.rejects(load([row]).api.loadDccSelectorPage('1', query))
    await assert.rejects(load([row]).api.loadDccProjectBrowserPage('1', query))
  }
})
test('source-project filter survives a selector query without a folder', async () => {
  const state = load([active])
  const result = await state.api.loadDccSelectorPage('1', { ...query, projectId: '5' })
  assert.equal(state.calls[0].params.dccProjectCodeId, '5')
  assert.equal(state.calls[0].params.projectFolderId, undefined)
  assert.equal(state.calls[0].params.directoryId, undefined)
  assert.equal(result.list[0].projectId, '5')
  await assert.rejects(load([{ ...active, dccProjectCodeId: '6' }]).api.loadDccSelectorPage('1', { ...query, projectId: '5' }))
})
test('explicit history retains its selected version even when it is neither latest nor current', async () => {
  const old = { ...active, id: '900', status: 'OBSOLETE', versionNo: 'Z/1', executable: false }
  const result = await load([old]).api.loadDccProjectBrowserPage('1', query, { latestVersionOnly: false, status: 'OBSOLETE' })
  assert.equal(result.list[0].controlledFileId, '900')
  assert.equal(result.list[0].executable, false)
})
const relation = { ...active, controlledFileId: current, projectId: '5', canEdit: false,
  hasCurrentControlledSource: true }
test('relation source preserves the separate current and latest identities', async () => {
  const result = await load([], relation).api.getControlledFileRelationPermissions(current)
  assert.equal(result.currentActiveControlledFileId, current)
  assert.equal(result.latestControlledFileId, latest)
})
test('relation projection cannot claim a current source without its exact readable identity', async () => {
  for (const id of [null, undefined, Number(current), '0', 'wrong']) {
    await assert.rejects(load([], { ...relation, currentActiveControlledFileId: id }).api.getControlledFileRelationPermissions(current))
  }
})
const attributes = { targetMarkets: ['CE'], licenseHolder: 'Y', actualManufacturer: 'N', documentTransfer: 'N' }
const working = { controlledFileId: current, projectId: '5', applicationType: 'UPLOAD',
  defaultSource: attributes, actual: attributes, canSubmit: false, unavailableReason: 'NOT_READY',
  effectiveMode: null, effectiveDate: null, needTraining: false,
  selectedSignoffDepartmentIds: null, selectedDistributionDepartmentIds: null, changeDescription: null }
test('working application preserves explicit effective mode and separately selected departments', async () => {
  const facts = { ...working, effectiveMode: 'AFTER_STANDARDIZATION_SIGNATURE',
    selectedSignoffDepartmentIds: ['8'], selectedDistributionDepartmentIds: ['9'] }
  const result = await load([], facts).api.getWorkingApplicationAttributes(current)
  assert.equal(result.effectiveMode, 'AFTER_STANDARDIZATION_SIGNATURE')
  assert.equal(result.effectiveDate, null)
  assert.equal(result.selectedSignoffDepartmentIds[0], '8')
  assert.equal(result.selectedDistributionDepartmentIds[0], '9')
})
test('working response rejects unknown modes and preserves genuine unfilled drafts', async () => {
  assert.equal((await load([], working).api.getWorkingApplicationAttributes(current)).effectiveMode, null)
  for (const facts of [{ ...working, effectiveMode: 'UNKNOWN' },
    { ...working, selectedDistributionDepartmentIds: '9' },
    { ...working, selectedDistributionDepartmentIds: [Number(current)] }]) {
    await assert.rejects(load([], facts).api.getWorkingApplicationAttributes(current))
  }
})
test('working save persists actual attributes and the complete explicitly supplied selection', async () => {
  const state = load([])
  const selection = { effectiveMode: 'AFTER_STANDARDIZATION_SIGNATURE', effectiveDate: null,
    selectedSignoffDepartmentIds: ['8'], selectedDistributionDepartmentIds: ['9'],
    needTraining: true, changeDescription: '本次修改' }
  await state.api.saveWorkingApplicationAttributes(current, attributes, selection)
  assert.equal(state.calls[0].data.actual.targetMarkets[0], 'CE')
  assert.equal(state.calls[0].data.effectiveMode, selection.effectiveMode)
  assert.equal(state.calls[0].data.effectiveDate, null)
  assert.equal(state.calls[0].data.selectedSignoffDepartmentIds[0], '8')
  assert.equal(state.calls[0].data.selectedDistributionDepartmentIds[0], '9')
  assert.equal(state.calls[0].data.needTraining, true)
})
test('invalid explicit working selections are rejected without a write', async () => {
  const valid = { effectiveMode: null, effectiveDate: null, selectedSignoffDepartmentIds: null,
    selectedDistributionDepartmentIds: null, needTraining: null, changeDescription: null }
  for (const selection of [undefined, { ...valid, effectiveMode: 'UNKNOWN' },
    { ...valid, effectiveMode: 'AFTER_STANDARDIZATION_SIGNATURE', effectiveDate: '2026-10-11' },
    { ...valid, selectedDistributionDepartmentIds: ['9','9'] },
    { ...valid, selectedSignoffDepartmentIds: [Number(current)] }]) {
    const state = load([])
    await assert.rejects(state.api.saveWorkingApplicationAttributes(current, attributes, selection))
    assert.equal(state.calls.length, 0)
  }
})

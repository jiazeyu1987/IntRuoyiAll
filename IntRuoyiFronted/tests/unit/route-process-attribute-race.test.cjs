const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')

const source = fs.readFileSync(path.resolve(__dirname,
  '../../src/views/mes/pro/route/RouteFlowGraphDesigner.vue'), 'utf8')
const start = source.indexOf('const loadSelectedProcessAttributes = async')
const end = source.indexOf('\nconst buildFormBindingSaveRows', start)
const code = ts.transpileModule(source.slice(start, end), {
  compilerOptions: { target: ts.ScriptTarget.ES2022 }
}).outputText

const editSource = fs.readFileSync(path.resolve(__dirname,
  '../../src/views/mes/pro/route/RouteEditPage.vue'), 'utf8')
const loadEditFunction = (name, context) => {
  const start = editSource.indexOf(`const ${name} = async`)
  const end = editSource.indexOf('\nconst ', start + 1)
  const code = ts.transpileModule(editSource.slice(start, end), {
    compilerOptions: { target: ts.ScriptTarget.ES2022 }
  }).outputText
  vm.createContext(context)
  vm.runInContext(`${code}; globalThis.invoke = ${name}`, context)
  return context.invoke
}

test('创建页退出前发现草稿已被其他页面保存时，不再自动取消', async () => {
  const saved = { value: false }
  let cancelled = false
  const leave = loadEditFunction('confirmUnsavedCandidateDraftBeforeExit', {
    shouldPromptUnsavedCandidateDraftBeforeExit: { value: false },
    shouldDiscardListEditCandidateDraftBeforeExit: { get value() { return !saved.value } },
    routeVersionEditContext: { value: { routeVersionId: 28 } },
    listEditCandidateDraftSaved: saved,
    ProRouteApi: { getRouteVersion: async () => ({ lifecycleStatus: 'DRAFT', lastSavedBy: 9908100850 }) },
    discardListEditCandidateDraft: async () => { cancelled = true },
    message: { error: msg => { throw Error(msg) } }
  })
  assert.equal(await leave(), undefined)
  assert.equal(saved.value, true)
  assert.equal(cancelled, false)
})

test('确认框打开后其他页面才保存时，保留服务端返回的草稿', async () => {
  const saved = { value: false }
  const discarded = { value: false }
  let localDiscarded = 0
  const notifications = []
  const discard = loadEditFunction('discardListEditCandidateDraft', {
    routeVersionEditContext: { value: { routeVersionId: 28 } },
    routeVersionActionLoading: { value: false },
    listEditCandidateDraftSaved: saved,
    listEditCandidateDraftDiscarded: discarded,
    contentRef: { value: { discardRouteCandidateDraftChanges: () => localDiscarded++ } },
    ProRouteApi: {
      getRouteVersion: async () => ({ lifecycleStatus: 'DRAFT' }),
      cancelRouteCandidateVersion: async (id, onlyUnsaved) => {
        assert.equal(id, 28)
        assert.equal(onlyUnsaved, true)
        return { lifecycleStatus: 'DRAFT', lastSavedBy: 9908100850 }
      }
    },
    message: { info: msg => notifications.push(msg), error: msg => { throw Error(msg) } }
  })
  assert.equal(await discard(), true)
  assert.equal(saved.value, true)
  assert.equal(discarded.value, false)
  assert.equal(localDiscarded, 1)
  assert.equal(notifications.length, 1)
})

test('切换工序后，旧请求的材料选项返回不能覆盖当前工序的系数', async () => {
  let selected = 1
  let request = 1
  const pending = new Map()
  const applied = []
  const loading = { value: false }
  const context = {
    selectedProcessAttributesLoading: loading,
    selectedProcessAttributesError: { value: '' },
    resetSelectedProcessAttributes() {},
    ensureSelectedProcessRouteConfigCache: async () => ({
      scheduleConfigs: [], routeScheduleConfigs: [], batchConfigs: [], readableRouteVersionId: 27
    }),
    isSelectedProcessDetailRequestCurrent: (id, processId) => id === request && processId === selected,
    findRouteProcessConfig() {},
    routeProcessRows: { value: [] },
    sameRouteProcessId: (a, b) => a === b,
    selectedProcessAttributeDrafts: {},
    selectedProcessAttributeBaselines: {},
    buildSelectedProcessAttributesDraft: node => ({ routeProcessId: node.routeProcessId }),
    cloneSelectedProcessAttributesDraft: draft => ({ ...draft }),
    serializeSelectedProcessAttributesDraft: JSON.stringify,
    loadSelectedRouteProcessMaterialOptions: () => new Promise(resolve => pending.set(selected, resolve)),
    applySelectedProcessAttributesDraft: draft => applied.push(draft.routeProcessId),
    tryOpenCapacityOverrideFromRouteQuery() {},
    message: { error: msg => { throw Error(msg) } },
    resolveErrorMessage: error => error.message
  }
  vm.createContext(context)
  vm.runInContext(`${code}; globalThis.load = loadSelectedProcessAttributes`, context)
  const first = context.load({ routeProcessId: 1 }, 1)
  await new Promise(setImmediate)
  assert.ok(pending.has(1))
  selected = 2
  request = 2
  const second = context.load({ routeProcessId: 2 }, 2)
  await new Promise(setImmediate)
  pending.get(2)()
  await second
  assert.deepEqual(applied, [2])
  pending.get(1)()
  await first
  assert.deepEqual(applied, [2])
  assert.equal(loading.value, false)
})

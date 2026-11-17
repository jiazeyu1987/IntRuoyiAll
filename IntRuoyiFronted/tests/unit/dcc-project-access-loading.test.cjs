const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const { ref, watch } = require('vue')
const source = fs.readFileSync(
  path.resolve(
    __dirname,
    '../../src/views/dcc/controlled-file/basic-data/components/ProjectCodeTabPanel.vue'
  ),
  'utf8'
)
const section = (a, b) => source.slice(source.indexOf(a), source.indexOf(b, source.indexOf(a)))
const code = ts.transpileModule(
  section('const submitProjectAccessRules =', 'const projectCodeQuickFilterDefinitions') +
    section('const openProjectAccessRules =', 'const addProjectAccessRuleRow'),
  { compilerOptions: { target: ts.ScriptTarget.ES2022 } }
).outputText
const deferred = () => {
  let resolve, reject
  const promise = new Promise((r, j) => {
    resolve = r
    reject = j
  })
  return { promise, resolve, reject }
}
function setup() {
  const state = {}
  for (const name of [
    'projectAccessRulesLoaded',
    'projectAccessRulesLoadError',
    'projectAccessRulesVisible',
    'projectAccessRulesLoading',
    'projectAccessRuleUsersLoading',
    'projectAccessRulesSaving'
  ])
    state[name] = ref(false)
  for (const name of ['projectAccessRules', 'projectAccessRuleFormRows', 'projectAccessRuleUsers'])
    state[name] = { value: [] }
  state.projectAccessRuleProject = { value: null }
  state.selectedProjectCode = { value: { id: 1 } }
  state.message = { error() {}, success() {} }
  state.buildProjectAccessRuleFormRow = (x) => ({ ...x })
  state.createProjectAccessRuleSeedRows = () => [{ seed: true }]
  state.normalizeProjectAccessRulePayload = () => []
  let saves = 0
  state.replaceProjectCodeAccessRules = async () => {
    saves++
    return []
  }
  const api = { rules: async () => [], users: async () => [] }
  state.getProjectCodeAccessRules = (id) => api.rules(id)
  state.getSimpleUserList = () => api.users()
  state.watch = watch
  const closeWatch = section('watch(projectAccessRulesVisible,', 'const projectAccessRulesSaving')
  const run = new Function(
    ...Object.keys(state),
    `let projectAccessRulesRequestVersion=0; ${code}; ${closeWatch}; return {open:openProjectAccessRules,submit:submitProjectAccessRules,close(){projectAccessRulesVisible.value=false}}`
  )(...Object.values(state))
  return { ...run, state, api, saves: () => saves }
}
for (const failed of ['rules', 'users'])
  test(`${failed} failure blocks save, retry recovers including empty results`, async () => {
    const h = setup()
    h.api[failed] = async () => {
      throw Error('unavailable')
    }
    await h.open()
    assert.equal(h.state.projectAccessRulesLoadError.value, true)
    assert.equal(h.state.projectAccessRulesLoaded.value, false)
    assert.deepEqual(h.state.projectAccessRuleFormRows.value, [])
    await h.submit()
    assert.equal(h.saves(), 0)
    h.api[failed] = async () => []
    await h.open()
    assert.equal(h.state.projectAccessRulesLoaded.value, true)
    assert.equal(h.state.projectAccessRulesLoadError.value, false)
    assert.deepEqual(h.state.projectAccessRules.value, [])
    await h.submit()
    assert.equal(h.saves(), 1)
  })
test('loading and unopened state cannot submit', async () => {
  const h = setup()
  await h.submit()
  assert.equal(h.saves(), 0)
  const d = deferred()
  h.api.rules = () => d.promise
  const pending = h.open()
  await h.submit()
  assert.equal(h.saves(), 0)
  d.resolve([])
  await pending
})
test('old project response cannot replace reopened project rows', async () => {
  const h = setup()
  const d = deferred()
  h.api.rules = (id) => (id === 1 ? d.promise : Promise.resolve([{ subjectId: 2 }]))
  const first = h.open({ id: 1 })
  h.close()
  await h.open({ id: 2 })
  d.resolve([{ subjectId: 1 }])
  await first
  assert.equal(h.state.projectAccessRuleProject.value.id, 2)
  assert.deepEqual(h.state.projectAccessRules.value, [{ subjectId: 2 }])
  assert.equal(h.state.projectAccessRulesLoaded.value, true)
})
test('closed dialog ignores delayed response and cannot save', async () => {
  const h = setup()
  const d = deferred()
  h.api.users = () => d.promise
  const pending = h.open()
  h.close()
  d.resolve([])
  await pending
  assert.equal(h.state.projectAccessRulesLoaded.value, false)
  assert.deepEqual(h.state.projectAccessRuleFormRows.value, [])
  await h.submit()
  assert.equal(h.saves(), 0)
})

test('old failure does not overwrite the new project success', async () => {
  const h = setup()
  const old = deferred()
  h.api.rules = (id) => (id === 1 ? old.promise : Promise.resolve([]))
  const pending = h.open({ id: 1 })
  await h.open({ id: 2 })
  old.reject(Error('old request failed'))
  await pending
  assert.equal(h.state.projectAccessRulesLoaded.value, true)
  assert.equal(h.state.projectAccessRulesLoadError.value, false)
})

test('old finally does not clear the new project loading indicator', async () => {
  const h = setup()
  const old = deferred()
  const current = deferred()
  h.api.rules = (id) => (id === 1 ? old.promise : current.promise)
  const first = h.open({ id: 1 })
  const second = h.open({ id: 2 })
  old.resolve([])
  await first
  assert.equal(h.state.projectAccessRulesLoading.value, true)
  assert.equal(h.state.projectAccessRuleUsersLoading.value, true)
  current.resolve([])
  await second
  assert.equal(h.state.projectAccessRulesLoading.value, false)
})

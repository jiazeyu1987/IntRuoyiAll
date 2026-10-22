const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const source = fs.readFileSync(path.resolve(__dirname,
  '../../src/views/mes/pro/processpool/QaRegulationPage.vue'), 'utf8').replace(/\r\n/g, '\n')
const body = source.match(/const initializeQaRegulationPage = async \(\) => \{([\s\S]*?)\n\}\n\nconst initializeCommonRegulationWorkspace/)[1]

function harness({ rememberedId, queryId, projects = [], loadError, selectionError } = {}) {
  const calls = [], warnings = []
  const initialize = new Function('loadDccProjectCodeOptions', 'loadQaMachineryOptions',
    'resolveInitialDccProjectCodeId', 'route', 'dccProjectCodeOptions', 'selectDccProjectCode',
    'ElMessage', 'handleDccProjectCodeChange', `return async () => {${body}}`)(
    async () => { if (loadError) throw loadError }, async () => {},
    () => queryId || rememberedId, { query: { dccProjectCodeId: queryId } }, { value: projects },
    async () => calls.push('clear'), { warning: message => warnings.push(message) },
    async id => { calls.push(id); if (selectionError) throw selectionError })
  return { initialize, calls, warnings }
}

test('a remembered inaccessible project is cleared without requesting its detail', async () => {
  const h = harness({ rememberedId: 129, projects: [{ id: 990819205 }] })
  await h.initialize()
  assert.deepEqual(h.calls, ['clear'])
  assert.match(h.warnings[0], /不在当前访问范围内/)
})

test('a remembered readable project restores its QA configuration', async () => {
  const h = harness({ rememberedId: 129, projects: [{ id: '129' }] })
  await h.initialize()
  assert.deepEqual(h.calls, [129])
  assert.deepEqual(h.warnings, [])
})

test('an explicit link preserves the server permission rejection', async () => {
  const denied = new Error('project access denied')
  const h = harness({ queryId: 129, projects: [], selectionError: denied })
  await assert.rejects(h.initialize(), error => error === denied)
  assert.deepEqual(h.calls, [129])
  assert.deepEqual(h.warnings, [])
})

test('a failed project list does not erase remembered selection or hide the failure', async () => {
  const failure = new Error('project list unavailable')
  const h = harness({ rememberedId: 129, loadError: failure })
  await assert.rejects(h.initialize(), error => error === failure)
  assert.deepEqual(h.calls, [])
  assert.deepEqual(h.warnings, [])
})

test('first visit leaves project selection empty', async () => {
  const h = harness()
  await h.initialize()
  assert.deepEqual(h.calls, [])
})

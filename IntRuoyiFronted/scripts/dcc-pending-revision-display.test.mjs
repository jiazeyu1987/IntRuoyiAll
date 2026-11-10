import assert from 'node:assert/strict'
import test from 'node:test'
import fs from 'node:fs'
import vm from 'node:vm'
import ts from 'typescript'
import { parse, compileScript, compileTemplate } from 'vue/compiler-sfc'

const root = new URL('../', import.meta.url)
const read = path => fs.readFileSync(new URL(path, root), 'utf8')
const source = read('src/views/dcc/controlled-file/revision/revision-model.ts')
const ast = ts.createSourceFile('revision-model.ts', source, ts.ScriptTarget.Latest, true)
const declarations = ast.statements.filter(node => node.declarationList?.declarations.some(
  declaration => ['revisionLockLabel', 'pendingRevisionApplications'].includes(declaration.name.text)))
const sandbox = { exports: {} }
vm.runInNewContext(ts.transpileModule(declarations.map(node => node.getText(ast)).join('\n'), {
  compilerOptions: { module: ts.ModuleKind.CommonJS }
}).outputText, sandbox)
const { revisionLockLabel, pendingRevisionApplications } = sandbox.exports

test('old A/1 shows its exact pending A/2 application, not its working body or another baseline', () => {
  const baseline = '2054545668044084438'
  const pending = { id: '2054545668044084449', revisionSourceControlledFileId: baseline,
    revisionChangeType: 'PARTIAL', status: 'PENDING_SUPERVISOR_REVIEW', versionNo: 'A/2' }
  const versions = [
    { id: baseline, revisionChangeType: 'INITIAL', status: 'ACTIVE' },
    { id: '2054545668044084448', revisionChangeType: null, status: 'WORKING' },
    pending,
    { ...pending, id: 'other', revisionSourceControlledFileId: '2054545668044084439' },
    ...['ACTIVE', 'SUPERSEDED', 'REJECTED', 'WITHDRAWN', 'OBSOLETE'].map(status => ({ ...pending, status }))
  ]
  assert.deepEqual(Array.from(pendingRevisionApplications(baseline, versions)), [pending])
  assert.equal(pendingRevisionApplications(pending.id, versions).length, 0)
  assert.equal(pendingRevisionApplications(baseline, []).length, 0)
})

test('replacement, rework and pending effectiveness remain discoverable', () => {
  for (const status of ['WORKING', 'PENDING_APPLICANT_REWORK', 'CONTROLLED_PENDING_EFFECTIVE']) {
    const row = { revisionSourceControlledFileId: '1', revisionChangeType: 'REPLACEMENT', status }
    assert.equal(pendingRevisionApplications('1', [row])[0], row)
  }
})

test('known revision locks explain next steps in Chinese and unknown errors remain visible', () => {
  assert.match(revisionLockLabel('FORMAL_REVISION_IN_PROGRESS'), /已有正式变更正在办理/)
  assert.match(revisionLockLabel('CHECKED_OUT'), /检入或撤销检出/)
  assert.equal(revisionLockLabel('UNEXPECTED_LOCK'), 'UNEXPECTED_LOCK')
  assert.equal(revisionLockLabel(null), '')
})

test('locked baseline stops before loading or preparing another approval matrix', async () => {
  const panel = parse(read('src/views/dcc/controlled-file/detail/DetailApplicationPanel.vue')).descriptor.scriptSetup.content
  const ast = ts.createSourceFile('panel.ts', panel, ts.ScriptTarget.Latest, true)
  const declaration = ast.statements.flatMap(node => Array.from(node.declarationList?.declarations || []))
    .find(node => node.name.text === 'open')
  const ref = value => ({ value })
  const options = { masterId: '100', lockedReason: 'FORMAL_REVISION_IN_PROGRESS' }
  const sandbox = { generation: 0, busy: ref(false), submitted: ref(false),
    props: { file: { id: '101', masterId: '100', dccProjectCodeId: '1' } },
    opened: ref(false), loading: ref(false), error: ref(''), preparationBlockers: ref([]),
    canSubmit: ref(true), working: ref(false), revisionOptions: ref(),
    getControlledFileRevisionOptions: async id => { assert.equal(id, '101'); return options }
  }
  vm.runInNewContext(ts.transpileModule('globalThis.open = ' + declaration.initializer.getText(ast), {
    compilerOptions: { target: ts.ScriptTarget.ES2022 }
  }).outputText, sandbox)
  await sandbox.open()
  assert.equal(sandbox.revisionOptions.value, options)
  assert.equal(sandbox.canSubmit.value, false)
  assert.equal(sandbox.loading.value, false)
  assert.equal(sandbox.error.value, '')
})

for (const path of [
  'src/views/dcc/controlled-file/detail/index.vue',
  'src/views/dcc/controlled-file/detail/DetailApplicationPanel.vue',
  'src/views/dcc/controlled-file/revision/DccRevisionPanel.vue'
]) test(`${path} compiles`, () => {
  const { descriptor, errors } = parse(read(path), { filename: path })
  assert.deepEqual(errors, [])
  const script = compileScript(descriptor, { id: 'revision-display' })
  const result = compileTemplate({ source: descriptor.template.content, filename: path,
    id: 'revision-display', compilerOptions: { bindingMetadata: script.bindings } })
  assert.deepEqual(result.errors, [])
})

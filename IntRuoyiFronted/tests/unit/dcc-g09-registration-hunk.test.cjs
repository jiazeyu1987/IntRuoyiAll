const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse: parseVue } = require('vue/compiler-sfc')
// Exercise current production code instead of an archived proposal document.
function readDeclaration(source, name) {
  const ast = ts.createSourceFile('source.ts', source, ts.ScriptTarget.Latest, true)
  const declaration = ast.statements.find(node => ts.isVariableStatement(node)
    && node.declarationList.declarations.some(item => item.name.getText(ast) === name))
  assert.ok(declaration, `Missing production declaration: ${name}`)
  return declaration.getText(ast)
}
function load() {
  const api = fs.readFileSync(path.resolve(__dirname, '../../src/api/dcc/controlledFile/workflow.ts'), 'utf8')
  const viewer = fs.readFileSync(path.resolve(__dirname, '../../src/views/dcc/controlled-file/view/index.vue'), 'utf8')
  const parsedViewer = parseVue(viewer)
  assert.equal(parsedViewer.errors.length, 0)
  assert.ok(parsedViewer.descriptor.scriptSetup)
  const script = parsedViewer.descriptor.scriptSetup.content
  const parse = readDeclaration(api, 'parseControlledFilePreviewMetadata')
  const state = ['registeredPreviewVisible', 'clearRegisteredPreview', 'registeredPreviewLabel', 'markRegisteredPreviewVisible']
    .map(name => readDeclaration(script, name)).join('\n')
  const c = { exports: {}, ...vue, Error, Date, Intl, generation: 1,
    errorMessage: vue.ref(''), resolvedPreviewUnavailableReason: vue.ref(''), resolvedPreviewMetadata: vue.ref(null),
    isCurrentRenderVersion: generation => generation === c.generation,
    DccControlledFileContractError: Error,
    assertRecordPayload: value => value, assertNoForbiddenDccFileCapabilityFields() {},
    readOptionalString: (record, key) => record[key], assertRequiredString: (record, key) => record[key],
    assertRequiredPreviewKind: (record, key) => record[key], assertRequiredObject: (record, key) => record[key],
    parseControlledPreviewWatermark: value => value,
    startedGenerations: [], closedReadingCount: 0,
    startPreviewReading: async generation => { c.startedGenerations.push(generation) },
    closePreviewReading: () => { c.closedReadingCount++ }
  }
  vm.runInNewContext(ts.transpileModule(parse + '\n' + state + '\nexports.api={parseControlledFilePreviewMetadata,registeredPreviewLabel,registeredPreviewVisible,markRegisteredPreviewVisible,clearRegisteredPreview}', {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText, c)
  return { c, ...c.exports.api, parsePreviewRegisteredAt: payload => c.exports.api.parseControlledFilePreviewMetadata(payload).previewRegisteredAt }
}

test('production metadata decoder accepts explicit offsets and rejects missing/ambiguous/impossible instants', () => {
  const r=load()
  for(const value of ['2026-10-15T00:30:10Z','2026-10-15T08:30:10+08:00','2024-02-29T00:00:00Z']) assert.equal(r.parsePreviewRegisteredAt({previewRegisteredAt:value}),value)
  for(const value of [undefined,null,'','2026-10-15T00:30:10','2026-02-30T00:00:00Z','2025-02-29T00:00:00Z','2026-13-01T00:00:00Z','2026-10-15T24:01:00Z','2026-10-15T00:00:00+19:00']) assert.throws(()=>r.parsePreviewRegisteredAt({previewRegisteredAt:value}))
})
test('production time state waits for successful exact generation, uses Shanghai and clears stale display', () => {
  const r=load()
  r.c.resolvedPreviewMetadata.value={previewRegisteredAt:'2026-10-15T00:30:10Z'}
  assert.equal(r.registeredPreviewLabel.value,'')
  r.markRegisteredPreviewVisible(0)
  assert.equal(r.registeredPreviewVisible.value,false)
  assert.equal(r.c.startedGenerations.length, 0)
  r.markRegisteredPreviewVisible(1)
  assert.deepEqual(r.c.startedGenerations, [1])
  assert.match(r.registeredPreviewLabel.value,/08:30:10/)
  assert.match(r.registeredPreviewLabel.value,/Asia\/Shanghai/)
  r.c.generation=2;r.clearRegisteredPreview()
  assert.equal(r.c.closedReadingCount, 1)
  r.markRegisteredPreviewVisible(1)
  assert.equal(r.registeredPreviewLabel.value,'')
  r.c.errorMessage.value='RENDER_FAILED';r.markRegisteredPreviewVisible(2)
  assert.equal(r.registeredPreviewVisible.value,false)
  r.c.errorMessage.value='';r.c.resolvedPreviewMetadata.value=null;r.markRegisteredPreviewVisible(2)
  assert.equal(r.registeredPreviewVisible.value,true,'successful offline body can show a missing-registration state')
  assert.equal(r.registeredPreviewLabel.value,'未记录')
})

test('actual shared metadata decoder preserves the specific unavailable reason before mandatory event/time validation', () => {
  const source=fs.readFileSync(path.resolve(__dirname,'../../src/api/dcc/controlledFile/workflow.ts'),'utf8')
  const ast=ts.createSourceFile('workflow.ts',source,ts.ScriptTarget.ES2022,true)
  const declaration=ast.statements.find(node=>ts.isVariableStatement(node)&&node.declarationList.declarations.some(d=>d.name.getText(ast)==='parseControlledFilePreviewMetadata'))
  assert.ok(declaration)
  let strictReads=0
  const c={exports:{},Error,DccControlledFileContractError:Error,
    assertRecordPayload:value=>value,assertNoForbiddenDccFileCapabilityFields(){},
    readOptionalString:(record,key)=>record[key],
    assertRequiredString(){strictReads++;throw new Error('must not validate unavailable success fields')},
    parsePreviewRegisteredAt(){strictReads++;throw new Error('must not replace original unavailable reason')}
  }
  vm.runInNewContext(ts.transpileModule(declaration.getText(ast)+'\nexports.parse=parseControlledFilePreviewMetadata',{
    compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}
  }).outputText,c)
  const unavailable={previewKind:'OFFICE',previewUnavailableReason:'OnlyOffice preview config is missing: base-url missing'}
  assert.equal(unavailable.accessEventCode,undefined)
  assert.equal(unavailable.previewRegisteredAt,undefined)
  assert.throws(()=>c.exports.parse(unavailable),error=>error.message===unavailable.previewUnavailableReason)
  assert.equal(strictReads,0)
})

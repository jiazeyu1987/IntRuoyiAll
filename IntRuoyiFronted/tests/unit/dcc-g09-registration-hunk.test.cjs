const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
// Executes the precise proposed blocks without writing another owner's production files.
const doc = fs.readFileSync(path.resolve(__dirname, '../../../doc/tasks/20261014-dcc-gap-viewer-tools/c04-integration-hunks.md'), 'utf8')
const blocks = Array.from(doc.matchAll(/```ts\n([\s\S]*?)\n```/g), match => match[1])
function load() {
  const parse = blocks.find(block => block.includes('const parsePreviewRegisteredAt ='))
  const state = blocks.find(block => block.includes('const registeredPreviewVisible ='))
  assert.ok(parse); assert.ok(state)
  const c = { exports: {}, ...vue, Error, Date, Intl, generation: 1,
    errorMessage: vue.ref(''), resolvedPreviewUnavailableReason: vue.ref(''), resolvedPreviewMetadata: vue.ref(null),
    isCurrentRenderVersion: generation => generation === c.generation,
    DccControlledFileContractError: Error,
    assertRequiredString: (payload, field) => { if (typeof payload[field] !== 'string' || !payload[field].trim()) throw new Error(field + ' required'); return payload[field] }
  }
  vm.runInNewContext(ts.transpileModule(parse + '\n' + state + '\nexports.api={parsePreviewRegisteredAt,registeredPreviewLabel,registeredPreviewVisible,markRegisteredPreviewVisible,clearRegisteredPreview}', {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText, c)
  return { c, ...c.exports.api }
}
test('proposed shared decoder block accepts explicit offsets and rejects missing/ambiguous/impossible instants', () => {
  const r=load()
  for(const value of ['2026-10-15T00:30:10Z','2026-10-15T08:30:10+08:00','2024-02-29T00:00:00Z']) assert.equal(r.parsePreviewRegisteredAt({previewRegisteredAt:value}),value)
  for(const value of [undefined,null,'','2026-10-15T00:30:10','2026-02-30T00:00:00Z','2025-02-29T00:00:00Z','2026-13-01T00:00:00Z','2026-10-15T24:01:00Z','2026-10-15T00:00:00+19:00']) assert.throws(()=>r.parsePreviewRegisteredAt({previewRegisteredAt:value}))
})
test('proposed time state waits for successful exact generation, uses Shanghai and clears stale display', () => {
  const r=load()
  r.c.resolvedPreviewMetadata.value={previewRegisteredAt:'2026-10-15T00:30:10Z'}
  assert.equal(r.registeredPreviewLabel.value,'')
  r.markRegisteredPreviewVisible(0)
  assert.equal(r.registeredPreviewVisible.value,false)
  r.markRegisteredPreviewVisible(1)
  assert.match(r.registeredPreviewLabel.value,/08:30:10/)
  assert.match(r.registeredPreviewLabel.value,/Asia\/Shanghai/)
  r.c.generation=2;r.clearRegisteredPreview()
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

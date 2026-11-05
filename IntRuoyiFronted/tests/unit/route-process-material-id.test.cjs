const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { test } = require('node:test')
const source = fs.readFileSync(path.resolve(__dirname, '../../src/views/mes/pro/route/RouteFlowGraphDesigner.vue'), 'utf8')
const script = source.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
const ast = ts.createSourceFile('designer.ts', script, ts.ScriptTarget.Latest, true)
const names = ['normalizeRouteProcessMaterialId', 'normalizeRouteProcessMaterialIds', 'mergeRouteProcessMaterialOptions', 'getSelectedRouteProcessMaterialIds', 'handleRouteProcessMaterialSuggestionSelect', 'handleRouteProcessMaterialIdsChange', 'getSelectedRouteProcessMaterialOptions', 'removeRouteProcessMaterial']
const declarations = ast.statements.filter(s => ts.isVariableStatement(s) && s.declarationList.declarations.some(d => names.includes(d.name.getText(ast))))
assert.equal(declarations.length, names.length)
const code = ts.transpileModule(declarations.map(s => s.getText(ast)).join('\n') + `\nexports.actions = { ${names.join(',')} };`, {compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText
function page() {
  const draft = {inputMaterialIds:[],outputMaterialIds:[]}
  const ctx = {exports:{}, selectedProcessAttributes:draft, selectedProcessAttributeDrafts:{p:draft}, routeProcessMaterialOptions:{value:[]}, selectedRouteProcessMaterialOptions:{value:[]}, recordBindingEditorDisabled:{value:false}, routeProcessMaterialSearchKeywords:{input:'',output:''}, ensureSelectedProcessAttributeDraft:()=>({routeProcessId:'p',draft}),markGraphDraftChanged() {}}
  vm.runInNewContext(code,ctx)
  return {ctx, ...ctx.exports.actions}
}
test('route material selection preserves neighboring long IDs through display, JSON save and removal',()=>{
  const p=page()
  const ids=['2118187830180417536','2118187830180417537']
  const items=ids.map((id,i)=>({id,code:`M${i}`,name:`material${i}`}))
  p.mergeRouteProcessMaterialOptions(items)
  items.forEach(item=>p.handleRouteProcessMaterialSuggestionSelect('input',item))
  assert.deepEqual(JSON.parse(JSON.stringify(p.ctx.selectedProcessAttributes.inputMaterialIds)),ids)
  assert.deepEqual(JSON.parse(JSON.stringify(p.getSelectedRouteProcessMaterialOptions('input'))).map(x=>x.id),ids)
  p.removeRouteProcessMaterial('input',ids[0])
  assert.deepEqual(JSON.parse(JSON.stringify(p.ctx.selectedProcessAttributes.inputMaterialIds)),[ids[1]])
  p.handleRouteProcessMaterialSuggestionSelect('output',items[0])
  assert.deepEqual(JSON.parse(JSON.stringify(p.ctx.selectedProcessAttributes.outputMaterialIds)),[ids[0]])
})
test('already rounded numeric IDs fail instead of being saved as another material',()=>{
  const p=page()
  assert.throws(()=>p.normalizeRouteProcessMaterialIds([Number('2118187830180417537')]),/安全整数/)
  assert.deepEqual(JSON.parse(JSON.stringify(p.normalizeRouteProcessMaterialIds([123,'123','456']))),['123','456'])
})

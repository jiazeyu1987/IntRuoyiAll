const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
const root = path.resolve(__dirname, '../..')
function component(file, api = {}) {
  const filename = path.join(root, file)
  const descriptor = parse(fs.readFileSync(filename, 'utf8'), { filename }).descriptor
  const script = compileScript(descriptor, { id: file })
  const template = compileTemplate({ source: descriptor.template.content, filename, id: file, compilerOptions: { bindingMetadata: script.bindings } })
  assert.deepEqual(template.errors, [])
  const code = ts.transpileModule(script.content, { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS } }).outputText
  const module = { exports: {} }, emitted = [], notices = []
  const context = { ...vue, module, exports: module.exports, console, Error, crypto: require('node:crypto').webcrypto,
    useMessage: () => ({ success: value => notices.push(value) }),
    require: name => name === 'vue' ? vue : { SharedChangeApi: api } }
  vm.runInNewContext(code, context)
  let exposed
  const state = module.exports.default.setup({}, { expose: value => exposed = value, emit: (...args) => emitted.push(args) })
  return { state, exposed, emitted, notices }
}
const plain = value => JSON.parse(JSON.stringify(value))
const dialog = api => component('src/views/mes/pro/shared/SharedChangeDialog.vue', api)
const request = () => ({ subjectType: 'PROCESS', action: 'UPDATE', sourceId: 1, expectedFingerprint: 'old', requestId: 'id', reason: '', nodes: [], process: { id: 1, name: 'new' } })
test('impact confirmation never writes on open or cancel; reason can be entered after preview', async () => {
  const writes = [], input = request()
  const { state, exposed, emitted } = dialog({ preview: async () => ({ before: {name:'old'}, after: {name:'new'}, routes: [], blockers: [], impactToken:'preview-1' }), submit: async (_, value) => { writes.push(plain(value)); return {status:'APPLIED'} } })
  await exposed.open(input)
  assert.equal(writes.length,0)
  assert.equal(state.canSubmit.value,false)
  state.visible.value = false
  assert.equal(writes.length,0)
  await exposed.open(input)
  state.draft.value.reason = '变更说明'
  assert.equal(input.reason,'')
  assert.equal(state.canSubmit.value,true)
  await state.submit()
  assert.equal(writes.length,1)
  assert.equal(writes[0].impactToken,'preview-1')
  assert.deepEqual(emitted,[['submitted']])
})
test('server conflict keeps the dialog open and does not emit success', async () => {
  const {state,exposed,emitted} = dialog({preview:async()=>({before:{},after:{},routes:[],blockers:[],impactToken:'old'}), submit:async()=>{throw new Error('引用已变化')} })
  await exposed.open(request()); state.draft.value.reason='原因'
  await assert.rejects(state.submit(),/引用已变化/)
  assert.equal(state.visible.value,true)
  assert.equal(state.error.value,'引用已变化')
  assert.equal(state.loading.value,false)
  assert.equal(emitted.length,0)
})
test('same process and parameter scheme across 1000 routes needs one selection', async () => {
  const routes=Array.from({length:1000},(_,i)=>({routeId:i+1,routeName:'路线'+i,nodes:[{routeProcessId:i+100,processId:1,processName:'热合',affectedDeviceIds:[7],productionConfig:{parameterRules:[{deviceId:7,parameterName:'温度',standardText:'180℃'}]}}]}))
  const calls=[]
  const api={source:async()=>({fingerprint:'source'}),options:async()=>[{id:2,name:'B',code:'B'}],
    crafts:async(_,id)=>{calls.push(id);return[{id:11,code:'GY-11',name:'热合B',parametersJson:'[]'}]},
    preview:async()=>({before:{},after:{},routes,blockers:[],impactToken:'new'})}
  const {state,exposed}=dialog(api)
  await exposed.openReplacement('DEVICE',1)
  state.draft.value.targetId=2
  await state.targetChanged()
  assert.equal(state.parameterGroups.value.length,1)
  assert.deepEqual(calls,[1])
  assert.equal(state.parameterGroups.value[0].members.length,1000)
  assert.equal(state.selectionsComplete.value,false)
  state.parameterGroups.value[0].selected=11
  await state.refreshPreview()
  assert.equal(state.draft.value.nodes.length,1000)
  assert.ok(state.draft.value.nodes.every(n=>n.deviceCraftId===11 && n.withoutParameters===false))
  assert.equal(state.pagedRoutes.value.length,20)
})
test('parser reuse choices are isolated until confirmation and preserve exact group binding', () => {
  const {state,exposed,emitted}=component('src/views/form-center/parser/components/SimpleReuseDialog.vue')
  const current=[{processIndex:0,craftSelections:[]}]
  const matches=[{processIndex:0,name:'热合',candidates:[{processId:10,code:'GX-10',name:'热合',workshops:[{id:1,name:'一车间'}],groups:[{groupIndex:2,parameters:[],crafts:[{id:21,code:'GY-21',name:'热合'}]},{groupIndex:3,parameters:[],crafts:[{id:22},{id:23}]}]}]}]
  exposed.open(matches,current)
  state.selectProcess(matches[0],10)
  assert.deepEqual(current,[{processIndex:0,craftSelections:[]}])
  assert.deepEqual(plain(state.selections.value[0].craftSelections),[{groupIndex:2,craftId:21}])
  assert.equal(emitted.length,0)
  state.selectCraft(matches[0],3,23); state.confirm()
  assert.deepEqual(plain(emitted[0][1][0].craftSelections),[{groupIndex:2,craftId:21},{groupIndex:3,craftId:23}])
})
test('stale parser selection cannot be confirmed until user selects an available process', () => {
  const {state,exposed,emitted}=component('src/views/form-center/parser/components/SimpleReuseDialog.vue')
  const rows=[{processIndex:0,name:'热合',candidates:[]}]
  exposed.open(rows,[{processIndex:0,processId:10,craftSelections:[]}])
  assert.equal(!!state.invalidSelection.value,true)
  state.confirm(); assert.equal(emitted.length,0)
  state.selectProcess(rows[0],0);state.confirm();assert.equal(emitted.length,1)
})

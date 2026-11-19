const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const loadUuid = require('../helpers/load-secure-uuid.cjs')
const httpCrypto = { getRandomValues: bytes => require('node:crypto').webcrypto.getRandomValues(bytes) }
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
const root = path.resolve(__dirname, '../..')
function component(file, api = {}, props = {}, crypto = httpCrypto) {
  const filename = path.join(root, file)
  const descriptor = parse(fs.readFileSync(filename, 'utf8'), { filename }).descriptor
  const script = compileScript(descriptor, { id: file })
  const template = compileTemplate({ source: descriptor.template.content, filename, id: file, compilerOptions: { bindingMetadata: script.bindings } })
  assert.deepEqual(template.errors, [])
  const code = ts.transpileModule(script.content, { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS } }).outputText
  const module = { exports: {} }, emitted = [], notices = []
  const diffModule = { exports: {} }
  vm.runInNewContext(ts.transpileModule(fs.readFileSync(path.join(root, 'src/views/mes/pro/shared/changeDifferences.ts'), 'utf8'),
    { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS } }).outputText,
    { module: diffModule, exports: diffModule.exports })
  const context = { ...vue, module, exports: module.exports, console, Error, crypto,
    useMessage: () => ({ success: value => notices.push(value) }),
    require: name => name === 'vue' ? vue : name === '@/utils/uuid' ? loadUuid(crypto) : name === './changeDifferences' ? diffModule.exports : { SharedChangeApi: api } }
  vm.runInNewContext(code, context)
  let exposed
  const state = module.exports.default.setup(props, { expose: value => exposed = value, emit: (...args) => emitted.push(args) })
  return { state, exposed, emitted, notices }
}
const plain = value => JSON.parse(JSON.stringify(value))
const dialog = api => component('src/views/mes/pro/shared/SharedChangeDialog.vue', api)
const request = () => ({ subjectType: 'PROCESS', action: 'UPDATE', sourceId: 1, expectedFingerprint: 'old', requestId: 'id', reason: '', nodes: [], process: { id: 1, name: 'new' } })

test('process steps remain a local draft until parent confirmation and detail view cannot mutate', async () => {
  const original = [{id:7,processId:1,sort:1,content:'原步骤'}]
  const {state,emitted} = component('src/views/mes/pro/process/ProProcessContentList.vue', {}, {processId:1,modelValue:original})
  state.openForm('update',original[0],0)
  state.formData.value.content='修改草稿'
  state.dialogVisible.value=false
  assert.equal(original[0].content,'原步骤')
  assert.equal(emitted.length,0)
  state.openForm('update',original[0],0)
  state.formData.value.content='修改步骤'
  state.formRef.value={validate:async()=>true}
  await state.submitForm()
  assert.equal(original[0].content,'原步骤')
  assert.equal(emitted[0][0],'update:modelValue')
  assert.equal(emitted[0][1][0].content,'修改步骤')
  assert.equal(emitted[0][1][0].id,7)
  const readonly=component('src/views/mes/pro/process/ProProcessContentList.vue', {}, {processId:1,modelValue:original,readonly:true})
  readonly.state.handleDelete(0)
  readonly.state.openForm('create')
  assert.equal(readonly.state.dialogVisible.value,false)
  assert.equal(readonly.emitted.length,0)
})
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
test('replacement candidates distinguish same-name processes by every usage workshop', async () => {
  const {state,exposed} = dialog({source:async()=>({fingerprint:'source'}),options:async()=>[]})
  await exposed.openReplacement('PROCESS',1)
  assert.equal(state.targetLabel({id:2,code:'GX-2',name:'热合',workshops:[{id:1,name:'一车间'},{id:2,name:'二车间'}]}),
    'GX-2 / 热合 / 一车间、二车间')
  assert.equal(state.targetLabel({id:3,code:'GX-3',name:'热合',workshops:[]}), 'GX-3 / 热合 / 未关联车间')
  await exposed.openReplacement('DEVICE',1)
  assert.equal(state.targetLabel({id:4,code:'SB-4',name:'热合机'}), 'SB-4 / 热合机')
})

test('HTTP replacement generates UUID v4 once and retains key and fingerprint through preview and retry', async () => {
  const writes = [], previews = []
  let fail = true
  const { state, exposed } = dialog({
    source: async () => ({ fingerprint: 'source-fingerprint' }), options: async () => [],
    preview: async (_, value) => {
      previews.push(plain(value))
      return { before: {}, after: {}, routes: [], blockers: [], impactToken: 'preview-token' }
    },
    submit: async (_, value) => {
      writes.push(plain(value))
      if (fail) throw new Error('temporary failure')
      return { status: 'APPLIED' }
    }
  })
  assert.equal(httpCrypto.randomUUID, undefined)
  await exposed.openReplacement('PROCESS', 1)
  const key = state.draft.value.requestId
  assert.match(key, /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/)
  state.draft.value.targetId = 2
  state.draft.value.reason = '替换工序'
  await state.targetChanged()
  await assert.rejects(state.submit(), /temporary failure/)
  fail = false
  await state.submit()
  for (const command of [...previews, ...writes]) {
    assert.equal(command.requestId, key)
    assert.equal(command.expectedFingerprint, 'source-fingerprint')
  }
  assert.equal(writes.length, 2)
  await exposed.openReplacement('PROCESS', 1)
  assert.notEqual(state.draft.value.requestId, key)
})

test('replacement fails before opening or writing when secure random source is unavailable or fails', async () => {
  for (const crypto of [{}, { getRandomValues() { throw new Error('entropy failed') } }]) {
    const writes = [], optionReads = []
    const { state, exposed } = component('src/views/mes/pro/shared/SharedChangeDialog.vue', {
      source: async () => ({ fingerprint: 'source-fingerprint' }),
      options: async () => { optionReads.push(true); return [] },
      submit: async value => writes.push(value)
    }, {}, crypto)
    await assert.rejects(exposed.openReplacement('PROCESS', 1), /不支持安全随机数|entropy failed/)
    assert.equal(state.visible.value, false)
    assert.equal(state.draft.value, undefined)
    assert.equal(writes.length, 0)
    assert.equal(optionReads.length, 0)
  }
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

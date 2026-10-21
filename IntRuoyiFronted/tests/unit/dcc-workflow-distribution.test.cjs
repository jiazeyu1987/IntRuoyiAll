const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const root = path.resolve(__dirname, '../../..')
const frontend = path.join(root, 'IntRuoyiFronted')
const vue = require(path.join(frontend, 'node_modules/vue'))
const ts = require(path.join(frontend, 'node_modules/typescript'))
const { parse, compileScript, compileTemplate } = require(path.join(frontend, 'node_modules/vue/compiler-sfc'))
const folder = path.join(frontend, 'src/views/dcc/controlled-file/workflow')
function component(name) {
  const { descriptor, errors } = parse(fs.readFileSync(path.join(folder, name), 'utf8'), { filename: name })
  assert.deepEqual(errors, [])
  assert.deepEqual(compileTemplate({ source: descriptor.template.content, filename: name, id: name }).errors, [])
  const compiled = ts.transpileModule(compileScript(descriptor, { id: name, inlineTemplate: true }).content,
    { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText
  const context = { exports: {}, require: name => {
    if (name === 'vue') return vue
    if (name === './distribution-facts') return factsModule()
    if (name === '@/utils/formatTime') {
      const compiled = ts.transpileModule(fs.readFileSync(path.join(frontend, 'src/utils/formatTime.ts'), 'utf8'),
        { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022, esModuleInterop: true } }).outputText
      const value = { exports: {}, require: id => { if (id === 'dayjs') return require(path.join(frontend, 'node_modules/dayjs')); throw new Error(id) }, Date, Error }
      vm.runInNewContext(compiled, value); return value.exports
    }
    if (name === './workflow-actions') {
      const compiled=ts.transpileModule(fs.readFileSync(path.join(folder,'workflow-actions.ts'),'utf8'),
        {compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText
      const actual={exports:{},require:id=>{throw new Error('Unexpected workflow-actions runtime import '+id)},Error,Date,BigInt}
      vm.runInNewContext(compiled,actual);return actual.exports
    }
    throw new Error('Unexpected runtime dependency ' + name)
  }, Error, Date, BigInt }
  vm.runInNewContext(compiled, context)
  return context.exports.default
}
function mount(name, input) {
  const node = type => ({ type, children: [], props: {} })
  const renderer = vue.createRenderer({ createElement: node, createText: text => ({ type: 'text', text }), createComment: text => ({ type: 'comment', text }),
    insert(child, parent) { if (child.parent) child.parent.children = child.parent.children.filter(n => n !== child); child.parent = parent; parent.children.push(child) },
    remove(child) { if (child.parent) child.parent.children = child.parent.children.filter(n => n !== child); child.parent = null },
    setText(n, text) { n.text = text }, setElementText(n, text) { n.text = text },
    parentNode: n => n.parent, nextSibling: n => n.parent?.children[n.parent.children.indexOf(n) + 1] || null, patchProp(n, key, _old, value) { n.props[key] = value } })
  const props = vue.reactive(input)
  const target = component(name)
  const app = renderer.createApp({ render: () => vue.h(target, props) })
  for (const tag of ['el-button', 'el-checkbox', 'el-select', 'el-option', 'el-alert', 'el-input'])
    app.component(tag, { inheritAttrs: false, setup: (_, ctx) => () => vue.h(tag, ctx.attrs, [ctx.attrs.title || '', ctx.slots.default?.()]) })
  app.component('el-table', { props: ['data'], setup: (props, ctx) => { vue.provide('rows', vue.toRef(props, 'data')); return () => vue.h('el-table', ctx.attrs, ctx.slots.default?.()) } })
  app.component('el-table-column', { inheritAttrs: false, setup: (_, ctx) => { const rows = vue.inject('rows'); return () => vue.h('el-table-column', ctx.attrs, (rows.value || []).map(row => ctx.slots.default ? ctx.slots.default({ row }) : String(row[ctx.attrs.prop] ?? ''))) } })
  app.config.warnHandler = () => {}
  app.directive('loading', {})
  const rootNode = node('root'); const view = app.mount(rootNode)
  return { app, props, rootNode, state: view.$.subTree.component.setupState }
}
function all(n) { return [n, ...(n.children || []).flatMap(all)] }
function text(n) { return [n.type === 'comment' ? '' : n.text || '', ...(n.children || []).map(text)].join(' ') }
function factsModule() {
  const compiled = ts.transpileModule(fs.readFileSync(path.join(folder, 'distribution-facts.ts'), 'utf8'),
    { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText
  const context = { exports: {}, require: () => { throw new Error('Unexpected facts runtime import') }, Error, BigInt }
  vm.runInNewContext(compiled, context); return context.exports
}
test('current failed state shows failure time but recovered sent state retains history without a failure banner', async () => {
  const automaticResult = { controlledFileId: '42', versionNo: 'B/1', approvalProcessInstanceId: 'round-42', batchId: '51',
    status: 'FAILED', errorCode: 'DELIVERY_FAILED', errorMessage: '原发送失败', failureAt: '2026-10-17 16:31:50' }
  const view = mount('WorkflowDistributionPanel.vue', { fileId: '42', processInstanceId: 'round-42', versionNo: 'B/1',
    facts: { status: 'ACTIVE' }, receipts: [], automaticResult })
  try {
    assert.match(text(view.rootNode), /实际失败时间/)
    view.props.automaticResult = { ...automaticResult, status: 'SENT', sentAt: '2026-10-17 16:31:54' }
    await vue.nextTick()
    assert.match(text(view.rootNode), /本批通知已全部送达/)
    assert.doesNotMatch(text(view.rootNode), /实际失败时间/)
    assert.equal(view.props.automaticResult.failureAt, automaticResult.failureAt)
    view.props.automaticResult = { ...automaticResult, status: 'DISPATCH_READY' }
    await vue.nextTick()
    assert.doesNotMatch(text(view.rootNode), /实际失败时间/)
  } finally { view.app.unmount() }
})
test('pending effective panel states zero receipt tasks and exposes no manual audience controls', () => {
  let calls = 0
  const view = mount('WorkflowDistributionPanel.vue', { fileId: '42', processInstanceId: 'round-42', versionNo: 'B/1',
    facts: { controlledTime: '2026-10-11 09:00', status: 'CONTROLLED_PENDING_EFFECTIVE', effectiveDate: '2026-10-12' },
    canDistribute: true, departments: [], save: async () => { calls++; return true }, loadRecipients: async () => [] })
  try {
    assert.match(text(view.rootNode), /受控且生效之后/)
    assert.match(text(view.rootNode), /不创建签收任务/)
    assert.equal(all(view.rootNode).some(n => n.type === 'el-select'), false)
    assert.equal(all(view.rootNode).some(n => text(n).includes('核对并下发')), false)
    assert.equal(calls, 0)
  } finally { view.app.unmount() }
})
test('effective panel distinguishes obligation creation from delivery and fails visibly', () => {
  const view = mount('WorkflowDistributionPanel.vue', { fileId: '42', processInstanceId: 'round-42', versionNo: 'B/1',
    facts: { controlledTime: '2026-10-11 09:00', activatedTime: '2026-10-11 09:00', distributedTime: '2026-10-11 09:01', status: 'ACTIVE', effectiveDate: '2026-10-11' },
    receipts: [], receiptError: '平台通知写入失败' })
  try { assert.match(text(view.rootNode), /义务已建立/); assert.match(text(view.rootNode), /平台通知写入失败/); assert.match(text(view.rootNode), /发送、打开和电子签名/); }
  finally { view.app.unmount() }
})
test('pending work list never offers acknowledgement handling for pre-effective versions', async () => {
  const view = mount('PendingWorkflowDistributionList.vue', { contextKey: 'tenant1-user99', canHandle: true, load: async () => [{
    id: '42', fileNumber: 'SOP-42', title: '待生效文件', versionNo: 'B/1', status: 'CONTROLLED_PENDING_EFFECTIVE',
    controlledTime: '2026-10-11 09:00', effectiveDate: '2026-10-12', distributionReminderStage: 'FUTURE' }] })
  try { await new Promise(resolve => setImmediate(resolve)); assert.equal(text(view.rootNode).includes('待生效文件'), false); }
  finally { view.app.unmount() }
})
test('receipt fact projection refuses unsigned fake times and retains failed send without completion', () => {
  const facts = factsModule()
  const row = { controlledFileId: '42', versionNo: 'B/1', distributionId: '51', recipientId: '71', userId: '99', departmentId: '61', messageStatus: 'FAILED', errorMessage: '写入失败' }
  assert.equal(facts.validateDistributionFacts([row], '42', 'B/1').length, 1)
  assert.throws(() => facts.validateDistributionFacts([{ ...row, signedAt: '2026-10-11 09:00' }], '42', 'B/1'))
  assert.throws(() => facts.validateDistributionFacts([{ ...row, controlledFileId: '43' }], '42', 'B/1'))
  assert.throws(() => facts.validateDistributionFacts([{ ...row, messageStatus: 'SENT' }], '42', 'B/1'))
})
test('receipt command preserves exact Long identity and false response cannot become success', async () => {
  const facts = factsModule(); const calls = []
  const request = { controlledFileId: '9223372036854775806', distributionId: '51', recipientId: '71', versionNo: 'B/1', password: 'test-value', comment: ' 已收到本版本 ' }
  await facts.submitDistributionReceipt(request, async input => { calls.push(input); return true })
  assert.equal(calls[0].controlledFileId, request.controlledFileId); assert.equal(calls[0].comment, '已收到本版本')
  await assert.rejects(facts.submitDistributionReceipt(request, async () => false))
  let emptyComment
  await facts.submitDistributionReceipt({ ...request, comment: undefined }, async input => { emptyComment = input.comment; return true })
  assert.equal(emptyComment, undefined)
})
function input(view, type, value) { all(view.rootNode).find(n => n.type === 'el-input' && n.props.type === type).props['onUpdate:modelValue'](value) }
test('signed receipt component invokes exact version once and rejects late old-context completion', async () => {
  let finish, calls = 0; const saved = []
  const view = mount('DistributionReceiptConfirm.vue', { controlledFileId: '42', distributionId: '51', recipientId: '71', versionNo: 'B/1',
    contextKey: 'tenant1-user99', canAcknowledge: true, onSaved: value => saved.push(value),
    save: request => { assert.equal(request.controlledFileId, '42'); calls++; return new Promise(resolve => { finish = resolve }) } })
  try {
    input(view, 'password', 'test-value'); input(view, 'textarea', '收到此版本'); await vue.nextTick()
    const button = all(view.rootNode).find(n => n.type === 'el-button')
    const first = button.props.onClick(); await button.props.onClick(); assert.equal(calls, 1)
    view.props.controlledFileId = '43'; await vue.nextTick(); finish(true); await first
    assert.equal(saved.length, 0); assert.equal(text(view.rootNode).includes('电子签名已保存'), false)
  } finally { view.app.unmount() }
})
test('receipt component false response is visible and never claims a signed time', async () => {
  const view = mount('DistributionReceiptConfirm.vue', { controlledFileId: '42', distributionId: '51', recipientId: '71', versionNo: 'B/1',
    contextKey: 'tenant1-user99', canAcknowledge: true, save: async () => false })
  try {
    input(view, 'password', 'test-value'); input(view, 'textarea', '收到此版本'); await vue.nextTick()
    await all(view.rootNode).find(n => n.type === 'el-button').props.onClick(); await vue.nextTick()
    assert.match(text(view.rootNode), /未返回正式保存成功/); assert.equal(text(view.rootNode).includes('电子签名已保存'), false)
  } finally { view.app.unmount() }
})
test('standardization signature mode has no preset date and shows actual effective fact', () => {
  const view = mount('WorkflowDistributionPanel.vue', { fileId: '42', processInstanceId: 'round-42', versionNo: 'B/1',
    facts: { effectiveMode: 'AFTER_STANDARDIZATION_SIGNATURE', effectiveDate: null, effectiveTime: '2026-10-11 08:01:02',
      controlledTime: '2026-10-11 09:00:00', activatedTime: '2026-10-11 09:00:00', status: 'ACTIVE' } })
  try {
    assert.match(text(view.rootNode), /文控标准化签名后生效/);assert.match(text(view.rootNode), /无预设日期/)
    assert.match(text(view.rootNode), /实际生效时间：2026-10-11 08:01:02/)
    assert.equal(text(view.rootNode).includes('预设生效日期：'), false)
  } finally { view.app.unmount() }
})
test('preset mode shows saved date and never replaces it with activation time', () => {
  const view = mount('WorkflowDistributionPanel.vue', { fileId: '42', processInstanceId: 'round-42', versionNo: 'B/1',
    facts: { effectiveMode: 'PRESET_DATE', effectiveDate: '2026-10-12', controlledTime: '2026-10-11 09:00', status: 'CONTROLLED_PENDING_EFFECTIVE' } })
  try { assert.match(text(view.rootNode), /预设生效日期：2026-10-12/);assert.match(text(view.rootNode), /实际生效时间：尚未记录/); }
  finally { view.app.unmount() }
})
test('missing historical mode is shown as unrecorded instead of inferred from a date', () => {
  const view = mount('WorkflowDistributionPanel.vue', { fileId: '42', processInstanceId: 'round-42', versionNo: 'B/1',
    facts: { effectiveMode: null, effectiveDate: '2026-10-12', status: 'ACTIVE' } })
  try { assert.match(text(view.rootNode), /生效方式未记录/);assert.equal(text(view.rootNode).includes('预设生效日期：'), false); }
  finally { view.app.unmount() }
})
test('preset mode with missing date exposes missing formal evidence', () => {
  const view = mount('WorkflowDistributionPanel.vue', { fileId: '42', processInstanceId: 'round-42', versionNo: 'B/1',
    facts: { effectiveMode: 'PRESET_DATE', effectiveDate: null, status: 'ACTIVE' } })
  try { assert.match(text(view.rootNode), /预设生效日期尚未记录/); }
  finally { view.app.unmount() }
})
test('historical active file missing effective time reports unrecorded fact instead of not effective', () => {
  const view = mount('WorkflowDistributionPanel.vue', { fileId: '42', processInstanceId: 'round-42', versionNo: 'B/1',
    facts: { effectiveMode: null, effectiveDate: null, activatedTime: '2026-10-11 09:00', status: 'ACTIVE' } })
  try { assert.match(text(view.rootNode), /实际生效时间：尚未记录/);assert.equal(text(view.rootNode).includes('实际生效时间：未生效'), false); }
  finally { view.app.unmount() }
})
test('pending automatic list accepts signature mode with null preset and formal effect/activation facts', async () => {
  const view=mount('PendingWorkflowDistributionList.vue',{contextKey:'tenant1-user99',canHandle:true,load:async flag=>{
    assert.equal(flag,false);return [{id:'42',fileNumber:'SOP-42',title:'签名模式待自动下发',versionNo:'B/1',status:'ACTIVE',
      effectiveMode:'AFTER_STANDARDIZATION_SIGNATURE',effectiveDate:null,effectiveTime:'2026-10-11 08:00:00',controlledTime:'2026-10-11 09:00:00',activatedTime:'2026-10-11 09:00:00',distributedTime:null}]
  }})
  try {await new Promise(resolve=>setImmediate(resolve));assert.match(text(view.rootNode),/签名模式待自动下发/);assert.match(text(view.rootNode),/无预设日期/);assert.match(text(view.rootNode),/2026-10-11 08:00:00/);assert.equal(text(view.rootNode).includes('生效事实不完整'),false);}
  finally{view.app.unmount()}
})
test('pending list preserves backend activation order despite descending preset dates and unknown historical mode', async () => {
  const base={fileNumber:'SOP',title:'待自动下发',versionNo:'B/1',status:'ACTIVE',controlledTime:'2026-10-11 09:00:00',distributedTime:null}
  const view=mount('PendingWorkflowDistributionList.vue',{contextKey:'tenant1-user99',canHandle:true,load:async()=>[
    {...base,id:'42',effectiveMode:'PRESET_DATE',effectiveDate:'2026-10-11',activatedTime:'2026-10-11 09:00:00'},
    {...base,id:'43',effectiveMode:'PRESET_DATE',effectiveDate:'2026-10-10',activatedTime:'2026-10-11 10:00:00'},
    {...base,id:'44',effectiveMode:null,effectiveDate:null,activatedTime:'2026-10-11 11:00:00'}]})
  try{await new Promise(resolve=>setImmediate(resolve));assert.equal(text(view.rootNode).includes('未按生效日期排序'),false);assert.match(text(view.rootNode),/生效方式未记录/);assert.equal(all(view.rootNode).filter(n=>n.type==='el-checkbox').length,0);assert.equal(text(view.rootNode).includes('办理下发'),false);}
  finally{view.app.unmount()}
})
test('automatic batch failure is visible even when recipient list is empty and never renders success',()=>{
  const view=mount('WorkflowDistributionPanel.vue',{fileId:'42',processInstanceId:'round-42',versionNo:'B/1',facts:{status:'ACTIVE',effectiveMode:'PRESET_DATE',effectiveDate:'2026-10-11',controlledTime:'2026-10-11 09:00',activatedTime:'2026-10-11 09:00'},receipts:[],
    automaticResult:{controlledFileId:'42',versionNo:'B/1',approvalProcessInstanceId:'round-42',batchId:'51',status:'FAILED',errorCode:'ROSTER_POLICY_UNDECIDED',errorMessage:'名单快照政策尚未确定',failureAt:'2026-10-11 09:01'}})
  try{assert.match(text(view.rootNode),/自动下发失败/);assert.match(text(view.rootNode),/名单快照政策尚未确定/);assert.equal(text(view.rootNode).includes('义务已建立'),false);}
  finally{view.app.unmount()}
})
test('automatic result not-created is explicit and mismatched old version is rejected',()=>{
  for(const result of [{controlledFileId:'42',versionNo:'B/1',approvalProcessInstanceId:'round-42',batchId:null,status:'NOT_CREATED'},
    {controlledFileId:'43',versionNo:'B/1',approvalProcessInstanceId:'round-42',batchId:'51',status:'SENT',sentAt:'2026-10-11 09:01'}]){
    const view=mount('WorkflowDistributionPanel.vue',{fileId:'42',processInstanceId:'round-42',versionNo:'B/1',facts:{status:'ACTIVE',effectiveDate:null},receipts:[],automaticResult:result})
    try{assert.match(text(view.rootNode),result.status==='NOT_CREATED'?/尚未建立自动下发批次/:/不是本次精确版本/);}
    finally{view.app.unmount()}
  }
})
test('old batch failure is rejected before showing another version error detail',()=>{
  const view=mount('WorkflowDistributionPanel.vue',{fileId:'42',processInstanceId:'round-42',versionNo:'B/1',facts:{status:'ACTIVE',effectiveDate:null},receipts:[],
    automaticResult:{controlledFileId:'43',versionNo:'B/1',approvalProcessInstanceId:'round-43',batchId:'51',status:'FAILED',errorMessage:'另一版本私有错误',failureAt:'2026-10-11 09:01'}})
  try{assert.match(text(view.rootNode),/不是本次精确版本/);assert.equal(text(view.rootNode).includes('另一版本私有错误'),false);assert.equal(text(view.rootNode).includes('实际失败时间'),false);}
  finally{view.app.unmount()}
})

const retryPanelProps = (save, extra = {}) => ({ fileId:'9007199254740993', processInstanceId:'round-retry', versionNo:'B/1',
  facts:{status:'ACTIVE',controlledTime:'2026-10-13 09:00:00',activatedTime:'2026-10-13 09:00:00',effectiveMode:'PRESET_DATE',effectiveDate:'2026-10-13'},
  automaticResult:{controlledFileId:'9007199254740993',batchId:'9223372036854775807',versionNo:'B/1',approvalProcessInstanceId:'round-retry',status:'FAILED',errorCode:'ROSTER_SOURCE_MISSING',errorMessage:'未安装正式来源',failureAt:'2026-10-13 09:00:30'},
  receipts:[],canRetryAutomatic:true,retryAutomatic:save,...extra })
function retryButton(view) { return all(view.rootNode).find(n=>n.type==='el-button' && text(n).includes('重试自动下发')) }
test('authorized automatic retry sends only four exact identity fields and preserves failed outcome',async()=>{
  const calls=[],retried=[]
  const view=mount('WorkflowDistributionPanel.vue',retryPanelProps(async request=>{calls.push(request);return {...retryPanelProps().automaticResult,status:'FAILED',errorCode:'ROSTER_POLICY_NOT_READY',errorMessage:'正式政策未就绪'}},{onRetried:result=>retried.push(result)}))
  try{assert.ok(retryButton(view));await retryButton(view).props.onClick();await vue.nextTick();assert.equal(calls.length,1)
    assert.deepEqual(Object.keys(calls[0]).sort(),['approvalProcessInstanceId','batchId','controlledFileId','versionNo']);assert.equal(calls[0].controlledFileId,'9007199254740993');assert.equal(calls[0].batchId,'9223372036854775807')
    assert.match(text(view.rootNode),/正式政策未就绪/);assert.equal(text(view.rootNode).includes('通知已全部送达'),false);assert.equal(retried.length,1)
  }finally{view.app.unmount()}
})
test('automatic retry blocks repeated clicks and ignores old response after file/round switch',async()=>{
  let finish,calls=0;const results=[]
  const view=mount('WorkflowDistributionPanel.vue',retryPanelProps(request=>{calls++;return new Promise(resolve=>{finish=resolve})},{onRetried:result=>results.push(result)}))
  try{assert.ok(retryButton(view));const button=retryButton(view),pending=button.props.onClick();await button.props.onClick();assert.equal(calls,1)
    view.props.fileId='43';view.props.processInstanceId='round43';await vue.nextTick();finish({...retryPanelProps().automaticResult,status:'SENT',sentAt:'2026-10-13 09:01:00'});await pending;await vue.nextTick()
    assert.equal(results.length,0);assert.equal(text(view.rootNode).includes('通知已全部送达'),false)
  }finally{view.app.unmount()}
})
test('retry requires explicit permission and existing non-sent batch and never replaces failure with a false response',async()=>{
  for(const extra of [{canRetryAutomatic:false},{automaticResult:{...retryPanelProps().automaticResult,status:'NOT_CREATED',batchId:null}},{automaticResult:{...retryPanelProps().automaticResult,status:'SENT',sentAt:'2026-10-13 09:01:00'}}]){
    const view=mount('WorkflowDistributionPanel.vue',retryPanelProps(async()=>{throw new Error('forbidden call')},extra));try{assert.equal(retryButton(view),undefined)}finally{view.app.unmount()}
  }
  const view=mount('WorkflowDistributionPanel.vue',retryPanelProps(async()=>false))
  try{assert.ok(retryButton(view));await retryButton(view).props.onClick();await vue.nextTick();assert.match(text(view.rootNode),/重试结果/);assert.equal(text(view.rootNode).includes('通知已全部送达'),false)}finally{view.app.unmount()}
})
test('automatic retry loses eligibility on unsafe context identity and retains server sent proof until refresh',async()=>{
  const unsafe=mount('WorkflowDistributionPanel.vue',retryPanelProps(async()=>{throw new Error('unsafe call')},{fileId:9007199254740993,automaticResult:{...retryPanelProps().automaticResult,controlledFileId:9007199254740993}}))
  try{assert.equal(retryButton(unsafe),undefined)}finally{unsafe.app.unmount()}
  const sent=mount('WorkflowDistributionPanel.vue',retryPanelProps(async()=>({...retryPanelProps().automaticResult,status:'SENT',sentAt:'2026-10-13 09:01:00'})))
  try{await retryButton(sent).props.onClick();await vue.nextTick();assert.equal(retryButton(sent),undefined)}finally{sent.app.unmount()}
})
test('retry response for previous account context cannot update the current actor',async()=>{
  let finish;const retried=[]
  const view=mount('WorkflowDistributionPanel.vue',retryPanelProps(()=>new Promise(resolve=>{finish=resolve}),{retryContextKey:'tenant1-actor99',onRetried:result=>retried.push(result)}))
  try{const pending=retryButton(view).props.onClick();view.props.retryContextKey='tenant1-actor100';await vue.nextTick();finish({...retryPanelProps().automaticResult,status:'SENT',sentAt:'2026-10-13 09:01:00'});await pending;assert.equal(retried.length,0)}finally{view.app.unmount()}
})
test('FAILED retry callback is rejected when any real failure code/message/time is missing',async()=>{
  const facts=factsModule(),expected={controlledFileId:'9007199254740993',batchId:'9223372036854775807',versionNo:'B/1',approvalProcessInstanceId:'round-retry'}
  for(const key of ['errorCode','errorMessage','failureAt']){
    const malformed={...retryPanelProps().automaticResult,[key]:key==='failureAt'?null:'   '}
    assert.throws(()=>facts.validateAutomaticDistributionRetryResult(malformed,expected),key)
    const resultEvents=[];const view=mount('WorkflowDistributionPanel.vue',retryPanelProps(async()=>malformed,{onRetried:result=>resultEvents.push(result)}))
    try{await retryButton(view).props.onClick();await vue.nextTick();assert.equal(resultEvents.length,0);assert.match(text(view.rootNode),/失败事实/)}finally{view.app.unmount()}
  }
  assert.equal(facts.validateAutomaticDistributionRetryResult(retryPanelProps().automaticResult,expected).status,'FAILED')
})

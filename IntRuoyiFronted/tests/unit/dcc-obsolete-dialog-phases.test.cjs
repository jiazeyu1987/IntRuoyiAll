const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript } = require('vue/compiler-sfc')
const path = 'src/views/dcc/controlled-file/detail/DetailObsoleteApplication.vue'
const attrs = { targetMarkets: ['CE'], licenseHolder: 'Y', actualManufacturer: 'N', documentTransfer: 'N' }
const draft = { applicationEntryMode:'DRAFT', id: '901', instanceCode: 'FORM-901', status: 'DRAFT', bpmProcessInstanceId: null,
  controlledFileId: '20', dccProjectCodeId: '5', versionNo: 'A/1', attributeRound: 2,
  applicationContextKey: 'DCC_OBSOLETE:901:FORM-901', defaultSource: attrs, actualAttributes: attrs,
  attributesSubmitted: false, reason: '实际保存原因', idempotencyKey: 'real-key',
  selectedSignoffDepartmentIds: ['51'], preparationReady: false, unavailableReason: 'DCC_OBSOLETE_SUPERVISOR_SOURCE_NOT_READY',
  approvalMatrixChanged: false, approvalMatrixBefore: null, approvalMatrixAfter: null }
const sourceFour={reworkSourceFormInstanceId:'801',reworkSourceFormInstanceCode:'FORM-801',reworkSourceBpmProcessInstanceId:'old-bpm',reworkSourceAttributeRound:1}
const sourceFive={...sourceFour,reworkSourceDigest:'a'.repeat(64)}
const matrix={formatVersion:'DCC_APPROVAL_MATRIX_BASELINE_V2',routeId:'40',routeVersionNo:1,matrixDigest:'1'.repeat(64),nodes:[{id:'41',stageType:'APPROVAL',subjectName:'原负责人',marker:'REQUIRED',approveMethod:'ALL',approveRatio:100,ruleRemark:'原M1说明'}]}
const relation={tenantId:'1',fileId:'20',masterId:'8',projectId:'5',applicationType:'OBSOLETE',attributeRound:1,bpmRound:'old-bpm',recorded:false,unavailableReason:'RELATION_STAGE_NOT_RECORDED',relationDigest:'b'.repeat(64),originalUploadSelections:[{relatedMasterId:'6',selectedControlledFileId:'61',projectId:'5',fileNumber:'SOP-6',fileName:'原关联.pdf',versionNo:'B/1',origin:'UPLOAD',supplemental:false}],completeSelections:[{relatedMasterId:'6',selectedControlledFileId:'61',projectId:'5',fileNumber:'SOP-6',fileName:'原关联.pdf',versionNo:'B/1',origin:'UPLOAD',supplemental:false}],stages:[]}
const source={...draft,...sourceFive,applicationEntryMode:'REWORK_SOURCE',id:'801',instanceCode:'FORM-801',status:'REJECTED',bpmProcessInstanceId:'old-bpm',attributeRound:1,applicationContextKey:'DCC_OBSOLETE:801:FORM-801',attributesSubmitted:true,idempotencyKey:'old-key',reason:'旧源原因',reworkSourceStatus:'REJECTED',reworkSourceVersionNo:'A/1',reworkSourceApprovalMatrix:matrix,reworkSourceRelations:relation,reworkSourceRelationDigest:relation.relationDigest}
const candidate={...sourceFive,reworkSourceStatus:'REJECTED',reworkSourceVersionNo:'A/1',reason:'旧源原因',available:true,unavailableReason:null}
const selection={applicationEntryMode:'SOURCE_SELECTION_REQUIRED',controlledFileId:'20',dccProjectCodeId:'5',versionNo:'A/1',preparationReady:false,unavailableReason:'SOURCE_SELECTION_REQUIRED',reworkSourceCandidates:[candidate,{...candidate,reworkSourceFormInstanceId:'802',reworkSourceFormInstanceCode:'FORM-802',reworkSourceBpmProcessInstanceId:'old-bpm-2',reworkSourceDigest:null,available:false,unavailableReason:'旧签名证据缺失'}]}
function mount(options = {}) {
  const calls = [], attributes = [], nodes = [], tables = [], editors=[]
  const user=vue.reactive({id:'99'})
  const mod = file => {
    const c = { exports: {}, Error }
    vm.runInNewContext(ts.transpileModule(fs.readFileSync(file, 'utf8'),
      { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, c)
    return c.exports
  }
  const deps = { vue,
    'element-plus': { ElMessageBox: { confirm: async message => {
      calls.push(['confirm', message]); if (options.confirm) await options.confirm(message);if (options.cancel) throw 'cancel'
    } } },
    '@/api/system/dept': { getSimpleDeptList: async () => [{ id: '51', name: '正式部门' }] },
    '@/api/system/user': { getSimpleUserList: async () => [] },
    '@/api/dcc/controlledFile/approvalRoutes': { previewApprovalRoute: async () => { calls.push('legacy-preview'); return [] } },
    '@/api/dcc/controlledFile/workflow': {
      getControlledFileObsoleteApplicationDraft: async (id,expected) => { calls.push(expected ? ['read',id,expected] : ['read',id]); return options.read ? options.read(id,expected) : (options.draft || draft) },
      obsoleteControlledFile: async (id, data) => { calls.push(['write', id, data]); if(options.write)return options.write(id,data); return data.phase === 'SUBMIT'
        ? { ...(options.draft || draft), status: 'IN_APPROVAL', bpmProcessInstanceId: 'exact-real-bpm', attributesSubmitted: true } : (options.draft || draft) }
    },
    '@/utils': { generateUUID: () => 'generated-key' },
    '@/utils/auth': { getTenantId: () => '1', getVisitTenantId: () => undefined },
    '@/store/modules/user': { useUserStore: () => ({ getUser: user }) },
    '../project-attributes/state': { validateAttributes: value => value },
    './obsolete-application': mod('src/views/dcc/controlled-file/detail/obsolete-application.ts'),
    '../project-attributes/ProjectApplicationAttributes.vue': { default: { props: ['savedSnapshot'], setup(p, ctx) {
      attributes.push(p); ctx.expose({ selectProject: async () => { calls.push('current-default-load'); return true },
        getSnapshot: () => ({ projectId: '5', applicationType: 'OBSOLETE', defaultSource: attrs, actual: attrs }) })
      return () => vue.h('div')
    } } },
    '../project-attributes/ProjectAttributesFields.vue': { default: { props: ['modelValue','readonly'], setup(p,ctx) { attributes.push(p);editors.push({props:p,edit:value=>ctx.emit('update:modelValue',value)}); return () => vue.h('div') } } }
  }
  const compiled = compileScript(parse(fs.readFileSync(path, 'utf8')).descriptor, { id: 'formal-obsolete-dialog', inlineTemplate: true })
  const c = { exports: {}, Error, require: name => { if (deps[name]) return deps[name]; throw new Error('Unexpected import: ' + name) } }
  vm.runInNewContext(ts.transpileModule(compiled.content,
    { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, c)
  const renderer = vue.createRenderer({ createElement: type => ({ type, children: [], props: {} }),
    createText: text => ({ text }), createComment: () => ({}), insert(n, p, anchor) {
      if(n.parent){const old=n.parent.children.indexOf(n);if(old>=0)n.parent.children.splice(old,1)}
      n.parent=p;const index=anchor?p.children.indexOf(anchor):-1
      if(index<0)p.children.push(n);else p.children.splice(index,0,n)
    }, remove(n) { if(n.parent){const i=n.parent.children.indexOf(n);if(i>=0)n.parent.children.splice(i,1);n.parent=undefined} },
    setText(n, t) { n.text = t }, setElementText(n, t) { n.text = t }, patchProp(n, k, _old, v) { n.props[k] = v },
    parentNode: n => n.parent, nextSibling: n => n.parent?.children[n.parent.children.indexOf(n)+1] || null })
  const app = renderer.createApp(c.exports.default, { modelValue: true, allowed: true,
    file: { id: '20', versionNo: 'A/1', dccProjectCodeId: '5', categoryId: '2', fileNumber: 'SOP', fileName: 'SOP.pdf' } })
  for (const name of ['el-dialog','el-form','el-form-item','el-alert','el-select','el-option','el-input','el-collapse','el-collapse-item','el-table-column']) {
    app.component(name, { setup: (_p, ctx) => () => vue.h('div', null,
      Object.values(ctx.slots).flatMap(slot => slot?.() || [])) })
  }
  app.component('el-table',{props:['data'],setup:(p,ctx)=>()=>{tables.push({data:p.data,...ctx.attrs});return vue.h('div',null,Object.values(ctx.slots).flatMap(slot=>slot?.()||[]))}})
  app.component('el-button', { props: ['disabled'], setup: (p, ctx) => () => {
    const node = vue.h('button', { ...ctx.attrs, disabled: p.disabled }, ctx.slots.default?.())
    nodes.push(node); return node
  } })
  app.directive('loading', {})
  const root = { children: [] };app.mount(root)
  return { calls, attributes, nodes, tables, editors, user, root, app, flush: async () => { for (let i=0;i<12;i++) { await Promise.resolve(); await vue.nextTick() } } }
}
const button=(state,id)=>[...state.nodes].reverse().find(node=>node.props?.['data-testid']===id)
const renderedText=node=>[node.text||'',...(node.children||[]).map(renderedText)].join(' ')
test('actual saved draft dialog reads server first and displays saved attributes without current-default replacement', async () => {
  const state = mount()
  try {
    await state.flush()
    assert.deepEqual(state.calls[0], ['read','20'])
    assert.equal(state.calls.includes('legacy-preview'), false)
    assert.equal(state.calls.includes('current-default-load'), false)
    assert.ok(state.attributes.some(p => p.modelValue?.targetMarkets[0] === 'CE' || p.savedSnapshot?.actual?.targetMarkets[0] === 'CE'))
    assert.equal(state.calls.some(call => Array.isArray(call) && call[0] === 'write'), false)
  } finally { state.app.unmount() }
})

test('IC30 real source dialog shows frozen M1 and original relationships while ordinary phases remain disabled',async()=>{
  const state=mount({draft:source})
  try{await state.flush()
    assert.ok(state.attributes.some(p=>p.modelValue?.targetMarkets[0]==='CE'))
    assert.equal(button(state,'dcc-obsolete-save-draft').props.disabled,true)
    assert.equal(button(state,'dcc-obsolete-submit').props.disabled,true)
    assert.equal(button(state,'dcc-obsolete-reopen').props.disabled,false)
    assert.ok(state.tables.some(t=>t['data-testid']==='dcc-obsolete-source-matrix' && t.data[0].ruleRemark==='原M1说明'))
    assert.ok(state.tables.some(t=>t['data-testid']==='dcc-obsolete-source-complete-relations' && t.data[0].selectedControlledFileId==='61'))
    const text=renderedText(state.root)
    assert.match(text,/旧源原因/);assert.match(text,/已拒绝/);assert.doesNotMatch(text,/old-bpm|版本身份|bbbbbbbbbbbbbbbb|RELATION_STAGE_NOT_RECORDED/)
    assert.equal(state.calls.filter(c=>c[0]==='write').length,0)
  }finally{state.app.unmount()}
})
test('IC30 actual input editing while OPEN is pending keeps edits and refuses to publish the earlier new draft',async()=>{
  let release
  const state=mount({draft:source,write:()=>new Promise(r=>release=r)})
  try{await state.flush();const pending=button(state,'dcc-obsolete-reopen').props.onClick();await state.flush()
    assert.equal(state.calls.filter(c=>c[0]==='write').length,1)
    // The production fields are disabled during transport; this exercises an externally steered input change too.
    const actual=state.editors.find(e=>e.props.modelValue?.targetMarkets?.[0]==='CE')
    actual.edit({...attrs,targetMarkets:['FDA']});await state.flush()
    release({...draft,...sourceFive,reworkSourceApprovalMatrix:matrix,reworkSourceRelations:relation,reworkSourceRelationDigest:relation.relationDigest,idempotencyKey:'generated-key'})
    await pending;await state.flush()
    assert.ok(state.attributes.some(p=>p.modelValue?.targetMarkets[0]==='FDA'))
    assert.equal(button(state,'dcc-obsolete-save-draft').props.disabled,true)
    assert.match(renderedText(state.root),/重办来源.*FORM-801/)
    assert.equal(state.calls.filter(c=>c[0]==='write').length,1)
  }finally{state.app.unmount()}
})
test('IC30 actor change during reapply confirmation cancels the earlier source and makes no old OPEN write',async()=>{
  let release
  const state=mount({draft:source,confirm:()=>new Promise(r=>release=r)})
  try{await state.flush();const pending=button(state,'dcc-obsolete-reopen').props.onClick();await state.flush()
    state.user.id='100';await state.flush();release();await pending;await state.flush()
    assert.equal(state.calls.filter(c=>c[0]==='write').length,0)
    assert.equal(state.calls.filter(c=>c[0]==='read').length,2)
  }finally{state.app.unmount()}
})
test('IC30 changed source during explicit reapply is visible and blocks write without discarding the edited attributes',async()=>{
  let reads=0
  const state=mount({read:async()=>++reads===1?source:{...source,reworkSourceDigest:'c'.repeat(64)}})
  try{await state.flush();const editable=state.editors.find(e=>!e.props.readonly)
    editable.edit({...attrs,targetMarkets:['FDA']});await state.flush()
    await button(state,'dcc-obsolete-reopen').props.onClick();await state.flush()
    assert.equal(state.calls.filter(c=>c[0]==='write').length,0)
    assert.equal(button(state,'dcc-obsolete-reopen').props.disabled,true)
    assert.ok(state.attributes.some(p=>p.modelValue?.targetMarkets[0]==='FDA'))
    assert.match(renderedText(state.root),/重新读取申请来源/)
  }finally{state.app.unmount()}
})
test('IC30 explicit reapply cancellation reads only and sends neither OPEN nor any BPM action',async()=>{
  const state=mount({draft:source,cancel:true})
  try{await state.flush();await button(state,'dcc-obsolete-reopen').props.onClick();await state.flush()
    assert.equal(state.calls.filter(c=>c[0]==='confirm').length,1);assert.equal(state.calls.filter(c=>c[0]==='write').length,0)
  }finally{state.app.unmount()}
})
test('IC30 human candidate choice sends exact source-four GET and explicit OPEN has a new key and no old target',async()=>{
  const state=mount({read:async(_id,expected)=>expected?source:selection,write:async(_id,data)=>({...draft,...sourceFive,reworkSourceApprovalMatrix:matrix,reworkSourceRelations:relation,reworkSourceRelationDigest:relation.relationDigest,reason:data.reason,actualAttributes:data.projectAttributes,idempotencyKey:data.idempotencyKey})})
  try{await state.flush()
    assert.equal(button(state,'dcc-obsolete-source-802').props.disabled,true)
    assert.equal(button(state,'dcc-obsolete-save-draft').props.disabled,true)
    assert.equal(state.calls.filter(c=>c[0]==='read').length,1)
    await button(state,'dcc-obsolete-source-801').props.onClick();await state.flush()
    assert.deepEqual(JSON.parse(JSON.stringify(state.calls[1][2])),sourceFour)
    await button(state,'dcc-obsolete-reopen').props.onClick();await state.flush()
    const write=state.calls.find(c=>c[0]==='write')[2]
    assert.equal(write.phase,'OPEN');assert.equal(write.idempotencyKey,'generated-key')
    for(const field of ['formInstanceId','formInstanceCode','attributeRound','applicationContextKey'])assert.equal(write[field],undefined)
    for(const [key,value]of Object.entries(sourceFive))assert.equal(write[key],value)
    assert.equal(button(state,'dcc-obsolete-save-draft').props.disabled,false)
    assert.equal(button(state,'dcc-obsolete-submit').props.disabled,true)
  }finally{state.app.unmount()}
})
test('actual SAVE draft button sends explicit false and not-ready submit stays disabled', async () => {
  const state = mount()
  try {
    await state.flush()
    const button = [...state.nodes].reverse().find(node => node.props?.['data-testid'] === 'dcc-obsolete-save-draft')
    assert.ok(button, 'formal draft saving button')
    await button.props.onClick(); await state.flush()
    const write = state.calls.find(call => Array.isArray(call) && call[0] === 'write')
    assert.equal(write[2].phase, 'SAVE');assert.equal(write[2].prepareExecution, false)
    assert.equal(write[2].formInstanceId, '901');assert.equal(write[2].attributeRound, 2)
    const submit = [...state.nodes].reverse().find(node => node.props?.['data-testid'] === 'dcc-obsolete-submit')
    assert.equal(submit.props.disabled, true)
  } finally { state.app.unmount() }
})
test('prepared actual SUBMIT cancellation performs no save or submission', async () => {
  const state = mount({ draft: { ...draft, preparationReady: true, unavailableReason: null }, cancel: true })
  try {
    await state.flush()
    const button = [...state.nodes].reverse().find(node => node.props?.['data-testid'] === 'dcc-obsolete-submit')
    assert.equal(button.props.disabled, false);await button.props.onClick();await state.flush()
    assert.equal(state.calls.filter(call => call[0] === 'confirm').length, 1)
    assert.equal(state.calls.filter(call => call[0] === 'write').length, 0)
  } finally { state.app.unmount() }
})
test('prepared confirmation displays complete actual attributes then sends only exact SUBMIT', async () => {
  const state = mount({ draft: { ...draft, preparationReady: true, unavailableReason: null } })
  try {
    await state.flush()
    const button = [...state.nodes].reverse().find(node => node.props?.['data-testid'] === 'dcc-obsolete-submit')
    assert.equal(button.props.disabled, false);await button.props.onClick();await state.flush()
    const confirmation = state.calls.find(call => call[0] === 'confirm')[1]
    assert.match(confirmation, /SOP/);assert.match(confirmation, /A\/1/);assert.match(confirmation, /实际保存原因/)
    assert.match(confirmation, /正式部门/);assert.match(confirmation, /目标市场.*CE/)
    assert.match(confirmation, /注册人.*是/);assert.match(confirmation, /生产方.*否/);assert.match(confirmation, /文件转移.*否/)
    const writes = state.calls.filter(call => call[0] === 'write')
    assert.equal(writes.length, 1);assert.equal(writes[0][2].phase, 'SUBMIT')
    assert.equal(writes[0][2].formInstanceId, '901');assert.equal(writes[0][2].attributeRound, 2)
    assert.deepEqual(Array.from(writes[0][2].projectAttributes.targetMarkets), ['CE'])
  } finally { state.app.unmount() }
})

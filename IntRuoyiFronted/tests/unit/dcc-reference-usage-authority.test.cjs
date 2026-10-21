const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript } = require('vue/compiler-sfc')
const root = path.resolve(__dirname,'../..')
const FILE='9007199254740993'
const source=()=>({tenantId:'31',controlledFileId:FILE,masterId:'700',fileName:'historical.pdf',fileNumber:'FIXED',versionNo:'A/1'})
const row=()=>({referenceId:'9223372036854775000',selectedControlledFileId:FILE,masterId:'700',projectId:'300',folderId:'301',
  projectName:'destination',folderName:'quality',fileName:'historical.pdf',fileNumber:'FIXED',versionNo:'A/1',status:'SUPERSEDED',
  controlled:false,pendingEffect:false,executable:false,canPreview:true})
const page=()=>({tenantId:'31',sourceControlledFileId:FILE,masterId:'700',referenceProjectCount:1,
  visibleReferenceProjectCount:1,total:1,detailsRestricted:false,list:[row()]})
const metadata=()=>({...row(),tenantId:'31',controlledFileId:FILE})
const deferred=()=>{let resolve,reject;const promise=new Promise((a,b)=>{resolve=a;reject=b});return{promise,resolve,reject}}
const drain=async()=>{for(let i=0;i<30;i++)await Promise.resolve();await vue.nextTick()}
function moduleOf(code,deps={}) {
  const result={}
  new Function('exports','require',ts.transpileModule(code,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText)(
    result,id=>{if(id==='vue')return vue;if(id in deps)return deps[id];throw Error('unexpected import '+id)})
  return result
}
function mount(options={}) {
  const file=path.join(root,'src/views/dcc/controlled-file/relations/DccReferenceUsageDialog.vue')
  const read=p=>fs.readFileSync(path.join(root,'src/views/dcc/controlled-file',p),'utf8')
  const preview=require('./dcc-fixed-version-preview-test-loader.cjs')()
  const actor=vue.reactive({id:'9007199254740993'}),calls=[],opened=[]
  let tenant='31',visit=null
  const deps={
    '@/utils/auth':{getTenantId:()=>tenant,getVisitTenantId:()=>visit},
    '@/store/modules/user':{useUserStore:()=>({getUser:actor})},
    '@/api/dcc/controlledFile/relations':{getProjectReferenceUsagePage:async context=>{calls.push(['page',context]);return options.getPage?options.getPage(context):page()}},
    '@/api/dcc/controlledFile/applicationRead':{getControlledFileRelationPermissions:async id=>{calls.push(['metadata',id]);return options.getMetadata?options.getMetadata(id):metadata()}},
    './project-reference-contract':moduleOf(read('relations/project-reference-contract.ts')),
    '../view/presentation':preview.view,'@/api/dcc/controlledFile/bodyReadContext':preview.body,
    '../shared/lifecycle':{getDccControlledFileStatusLabel:value=>value},
    '../shared/version-display':moduleOf(read('shared/version-display.ts'))}
  const desc=parse(fs.readFileSync(file,'utf8')).descriptor
  const compiled=compileScript(desc,{id:'reference-authority-actual-sfc'})
  const exports={}
  new Function('exports','require','window',ts.transpileModule(compiled.content,{
    compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText)(
    exports,id=>{if(id==='vue')return vue;if(id in deps)return deps[id];throw Error(id)},
    {open:url=>{opened.push(url);return{opener:{}}}})
  const component=exports.default;component.render=()=>vue.h('section')
  const props=vue.reactive({modelValue:true,source:source(),contextKey:'unchanged',returnTo:'/dcc/controlled-file/detail/'+FILE})
  const renderer=vue.createRenderer({createElement:type=>({type,children:[]}),createText:text=>({text}),createComment:()=>({}),
    insert:(n,p)=>p.children.push(n),remove(){},setText(){},setElementText(){},patchProp(){},parentNode:()=>null,nextSibling:()=>null})
  const app=renderer.createApp({render:()=>vue.h(component,props)}),host=app.mount({children:[]})
  return{app,props,opened,calls,preview,state:host.$.subTree.component.setupState,change:kind=>{
    if(kind==='actor')actor.id='9007199254740995'
    else if(kind==='tenant')tenant='32'
    else visit='32'
  }}
}

for(const identity of ['actor','tenant','visit']) {
  for(const outcome of ['success','failure']) {
    test(`actual load ignores old ${outcome} after ${identity} changes with identical source/props/context-key`,async()=>{
      const pending=deferred(),h=mount({getPage:()=>pending.promise})
      const original=h.props.source
      try{
        await drain();h.change(identity)
        if(outcome==='success')pending.resolve(page());else pending.reject(new Error('old page denied'))
        await drain()
        assert.equal(h.props.source,original);assert.equal(h.props.contextKey,'unchanged')
        assert.equal(h.state.page,undefined);assert.equal(h.state.error,'')
      }finally{h.app.unmount()}
    })
    test(`actual preview ignores old ${outcome} after ${identity} changes with identical source/props/context-key`,async()=>{
      const pending=deferred(),h=mount({getMetadata:()=>pending.promise})
      try{
        await drain();const request=h.state.preview(row());await drain();h.change(identity)
        if(outcome==='success')pending.resolve(metadata());else pending.reject(new Error('old preview denied'))
        await request;await drain()
        assert.equal(h.opened.length,0);assert.equal(h.state.error,'')
        assert.equal(h.props.source.controlledFileId,FILE);assert.equal(h.props.contextKey,'unchanged')
      }finally{h.app.unmount()}
    })
  }
}

test('unchanged authority keeps exact fixed Long/raw-version wire and a real permission denial remains visible',async()=>{
  const h=mount()
  try{
    await drain();await h.state.preview({...row(),selectedControlledFileId:'12'})
    assert.equal(h.calls.at(-1)[1],FILE)
    const target=new URL(h.opened[0],'https://review.invalid')
    const context=h.preview.body.readDccBodyReadContextRoute(FILE,Object.fromEntries(target.searchParams))
    assert.deepEqual(JSON.parse(JSON.stringify(context)),{intent:'VERSION_HISTORY',controlledFileId:FILE,expectedVersionNo:'A/1'})
  }finally{h.app.unmount()}
  const denied=mount({getMetadata:async()=>({...metadata(),canPreview:false})})
  try{await drain();await denied.state.preview(row());assert.equal(denied.opened.length,0);assert.match(denied.state.error,/权限|状态/)}
  finally{denied.app.unmount()}
})

test('current page and preview transport failures remain visible when authority has not changed',async()=>{
  const pageDenied=mount({getPage:async()=>{throw new Error('current page denied')}})
  try{await drain();assert.equal(pageDenied.state.page,undefined);assert.equal(pageDenied.state.error,'current page denied')}
  finally{pageDenied.app.unmount()}
  const previewDenied=mount({getMetadata:async()=>{throw new Error('current preview denied')}})
  try{await drain();await previewDenied.state.preview(row());assert.equal(previewDenied.state.error,'current preview denied');assert.equal(previewDenied.opened.length,0)}
  finally{previewDenied.app.unmount()}
})

test('a lossy fixed Long cannot send a preview metadata request even if source and saved row both contain it',async()=>{
  const h=mount()
  try{
    await drain();h.props.source.controlledFileId=Number(FILE)
    h.state.page.list[0].selectedControlledFileId=Number(FILE)
    await h.state.preview(row())
    assert.equal(h.calls.some(call=>call[0]==='metadata'),false)
    assert.equal(h.opened.length,0);assert.ok(h.state.error)
  }finally{h.app.unmount()}
})

for(const identity of ['actor','tenant','visit']) {
  test(`loaded R05 page cannot authorize a new preview after ${identity} switches`,async()=>{
    const h=mount()
    try{
      await drain();assert.ok(h.state.page)
      h.change(identity);await h.state.preview(row())
      assert.equal(h.calls.filter(call=>call[0]==='metadata').length,0)
      assert.equal(h.opened.length,0)
      assert.equal(h.state.page,undefined)
    }finally{h.app.unmount()}
  })
}
test('close clears current R05 error and direct preview cannot open after close',async()=>{
  const h=mount()
  try{
    await drain();h.state.error='old prompt';h.state.close()
    assert.equal(h.state.error,'')
    await h.state.preview(row());assert.equal(h.opened.length,0)
  }finally{h.app.unmount()}
})
test('unmounted R05 cannot start a new request from the captured refresh handler',async()=>{
  const h=mount();await drain();const count=h.calls.length;const refresh=h.state.load
  h.app.unmount();await refresh()
  assert.equal(h.calls.length,count)
})

test('explicit refresh under the new actor authorizes only the same fixed version again',async()=>{
  const h=mount()
  try{
    await drain();h.change('actor');await h.state.preview(row())
    assert.equal(h.calls.filter(call=>call[0]==='metadata').length,0)
    await h.state.load();await h.state.preview(row())
    assert.equal(h.calls.filter(call=>call[0]==='metadata').length,1)
    const target=new URL(h.opened[0],'https://review.invalid')
    assert.deepEqual(JSON.parse(JSON.stringify(h.preview.body.readDccBodyReadContextRoute(FILE,Object.fromEntries(target.searchParams)))),
      {intent:'VERSION_HISTORY',controlledFileId:FILE,expectedVersionNo:'A/1'})
  }finally{h.app.unmount()}
})

test('closed dialog refresh stays inactive and explicit reopen loads the original fixed source',async()=>{
  const h=mount()
  try{
    await drain();h.state.close();const count=h.calls.length
    await h.state.load();assert.equal(h.calls.length,count)
    h.props.modelValue=false;await drain();h.props.modelValue=true;await drain()
    assert.equal(h.calls.length,count+1)
    assert.equal(h.state.page.sourceControlledFileId,FILE)
    await h.state.preview(row());assert.equal(h.opened.length,1)
  }finally{h.app.unmount()}
})

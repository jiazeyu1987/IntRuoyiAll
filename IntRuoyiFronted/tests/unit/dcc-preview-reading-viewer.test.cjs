const { test } = require('node:test')
const assert = require('node:assert/strict'), fs = require('node:fs'), path = require('node:path'), vm = require('node:vm')
const ts = require('typescript'), vue = require('vue')
const { parse, compileTemplate } = require('vue/compiler-sfc')
const root = path.resolve(__dirname, '../..')
const compile = text => ts.transpileModule(text, { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS } }).outputText
const deferred = () => { let resolve, reject; const promise = new Promise((a,b) => { resolve=a; reject=b }); return { promise, resolve, reject } }
const flush = async () => { for(let i=0;i<16;i++) { await Promise.resolve(); await vue.nextTick() } }
const row = (id='9223372036854775001') => ({ sessionId:id, openedAt:'2026-10-15T00:00:00Z',startedAt:'2026-10-15T00:00:01Z',lastHeartbeatAt:'2026-10-15T00:00:11Z',endedAt:null,accumulatedSeconds:10,heartbeatIntervalSeconds:10,maxHeartbeatGapSeconds:15 })
const sum = seconds => ({ firstOpenedAt:'2026-10-15T00:00:01Z',lastOpenedAt:'2026-10-15T00:00:11Z',accumulatedSeconds:seconds,openedSessionCount:1 })

// Production SFC + actual API wrapper + actual lifecycle helper. Only HTTP, PDF and browser ports are isolated.
function load({kind='TEXT', source, local=false, bodyGate, metadataGate, pdfGate, startGate, summaryGate, summaryError, heartbeatError, stopError, previewError, summarySeconds, uuidError}={}) {
  const descriptor=parse(fs.readFileSync(path.join(root,'src/views/dcc/controlled-file/view/index.vue'),'utf8')).descriptor
  const calls=[], errors=[], timers=new Map(), listeners=new Map(), hooks={}
  const identity={actorId:'99',baseTenantId:'31',visitTenantId:null,token:'authenticated'}
  const emitted=[]
  let timerId=0,client=0,focused=true, api, requestNumber=0
  const props=vue.reactive(local?{title:'Controlled body',previewBlob:new Uint8Array([1]),previewKind:kind}:{title:'Controlled body',controlledFileId:'910',...(source?{previewSource:source}:{})})
  const document={visibilityState:'visible',hasFocus:()=>focused,addEventListener:(n,f)=>listeners.set('document:'+n,f),removeEventListener:(n,f)=>{if(listeners.get('document:'+n)===f)listeners.delete('document:'+n)}}
  const window={devicePixelRatio:1,addEventListener:(n,f)=>listeners.set('window:'+n,f),removeEventListener:(n,f)=>{if(listeners.get('window:'+n)===f)listeners.delete('window:'+n)},
    setInterval:(f,ms)=>{assert.equal(ms,10000);timers.set(++timerId,f);return timerId},clearInterval:id=>timers.delete(id)}
  class Element {dataset={};clientWidth=900;offsetWidth=900;offsetHeight=800;getBoundingClientRect(){return{top:0}}}
  class Canvas extends Element {style={};getContext(){return{save(){},restore(){},strokeRect(){},fillText(){}}}}
  const metadata=s=>({previewKind:kind,fileName:'selected.txt',previewRegisteredAt:'2026-10-15T00:00:00Z',viewerToken:'signed-'+(s.controlledFileId||s.fileId||'other'),accessEventCode:'event-'+(s.controlledFileId||s.fileId||'other'),
    viewerTokenId:'id-1',viewerTokenNonce:'nonce-1',watermarkTraceCode:'trace-1',watermark:{text:'controlled'},onlyofficeBaseUrl:'https://office.invalid',onlyofficeDocumentUrl:'https://preview.invalid/'+(s.controlledFileId||s.fileId||'other')})
  const request={post:async req=>{
    calls.push(['post',req]);requestNumber++
    if(req.url.endsWith('/start')&&startGate&&req.url.includes('/910/'))return startGate.promise
    if(req.url.endsWith('/heartbeat')&&heartbeatError) {
      const failure=typeof heartbeatError==='function'?heartbeatError():heartbeatError
      if(failure)throw new Error(failure)
    }
    if(req.url.endsWith('/stop')&&stopError)throw new Error(stopError)
    return row(req.data.sessionId || '922337203685477'+String(5000+requestNumber))
  },get:async req=>{
    calls.push(['summary',req])
    if(summaryError)throw new Error(summaryError)
    if(summaryGate&&req.url.includes('/910/'))return summaryGate.promise
    return sum(summarySeconds ? summarySeconds.shift() : req.url.includes('/911/')?22:10)
  }}
  const modules={}
  function module(file) {
    if(modules[file])return modules[file]
    const c={exports:{},Error,Promise,Object,Number,BigInt,Date,Intl,TextEncoder,TextDecoder,atob,btoa,require:id=>{
      if(id==='@/config/axios')return{default:request}
      if(id==='./bodyReadContext')return module('src/api/dcc/controlledFile/bodyReadContext.ts')
      throw new Error('Unexpected module '+id)
    }}
    vm.runInNewContext(compile(fs.readFileSync(path.join(root,file),'utf8')),c);return modules[file]=c.exports
  }
  const host={exports:{},...vue,Blob,Uint8Array,TextDecoder,Date,Intl,Number,Object,Error,Promise,JSON,document,window,HTMLElement:Element,HTMLCanvasElement:Canvas,
    ResizeObserver:class{observe(){}disconnect(){}},
    URL:{createObjectURL:()=> 'blob:reading-body',revokeObjectURL(){}},
    crypto:{randomUUID:()=> {if(uuidError)throw new Error(uuidError);return 'client-'+(++client)}},console:{error:(...args)=>errors.push(args.join(' '))},
    defineOptions(){},defineProps:()=>props,defineEmits:()=>(...args)=>emitted.push(args),defineExpose:value=>{hooks.exposed=value},
    watch:(value,fn,opts)=>{if(vue.isRef(value)||Array.isArray(value))return vue.watch(value,fn,opts);hooks.sourceWatch={value,fn,opts};return()=>{}},
    onMounted:fn=>{hooks.mount=fn},onBeforeUnmount:fn=>{hooks.unmount=fn},
    nextTick:async()=>{if(api)for(const page of api.pageEntries.value)api.bindCanvasRef(page.pageNumber,new Canvas())},
    require:id=>{
      if(id==='@/utils/auth')return{getAccessToken:()=>identity.token,getTenantId:()=>identity.baseTenantId,getVisitTenantId:()=>identity.visitTenantId}
      if(id==='@/hooks/web/useCache')return{CACHE_KEY:{USER:'user'},useCache:()=>({wsCache:{get:()=>({user:{id:identity.actorId}})}})}
      if(id.endsWith('.vue'))return{}
      if(id==='./vendor/pdf.min.mjs')return{GlobalWorkerOptions:{},getDocument:()=>({promise:Promise.resolve({numPages:1,getPage:async()=>({getViewport:()=>({width:400,height:600}),getTextContent:async()=>({items:[]}),render:()=>({promise:pdfGate?pdfGate.promise:Promise.resolve()}),cleanup(){}}),cleanup(){},destroy(){}}),destroy(){}})}
      if(id==='./presentation'||id==='./pdf-tools'||id==='./reading-lifecycle')return module('src/views/dcc/controlled-file/view/'+id.slice(2)+'.ts')
      if(id==='@/api/dcc/controlledFile/readingSession')return module('src/api/dcc/controlledFile/readingSession.ts')
      if(id==='@/api/dcc/controlledFile/bodyReadContext')return module('src/api/dcc/controlledFile/bodyReadContext.ts')
      if(id==='@/api/common/filePreview')return{
        buildDccControlledFilePreviewSource:id=>({type:'DCC_CONTROLLED_FILE',controlledFileId:id,bodyReadContext:{intent:'DAILY',controlledFileId:String(id)}}),
        getOnlineFilePreviewMetadata:async s=>{calls.push(['metadata',s]);if(metadataGate)await metadataGate.promise;return metadata(s)},
        previewOnlineFileWithWatermark:async(s,m)=>{calls.push(['body',s,m]);if(previewError)throw new Error(previewError);if(bodyGate)await bodyGate.promise;return{blob:new Blob([new TextEncoder().encode('real text')]),watermark:m.watermark}}
      }
      throw new Error('Unexpected SFC import '+id)
    }}
  const ast=ts.createSourceFile('viewer.ts',descriptor.scriptSetup.content,ts.ScriptTarget.ES2022,true)
  const names=ast.statements.filter(ts.isVariableStatement).flatMap(s=>Array.from(s.declarationList.declarations).filter(d=>ts.isIdentifier(d.name)).map(d=>d.name.text))
  vm.runInNewContext(compile(descriptor.scriptSetup.content.replaceAll('import.meta.env.BASE_URL',"'/'")+'\nexports.api={'+names.join(',')+'}'),host);api=host.exports.api
  hooks.mount()
  const startCalls=()=>calls.filter(c=>c[0]==='post'&&c[1].url.endsWith('/start'))
  const stops=()=>calls.filter(c=>c[0]==='post'&&c[1].url.endsWith('/stop'))
  return {api,props,calls,timers,listeners,hooks,errors,descriptor,startCalls,stops,identity,emitted,
    load:async()=>{await api.loadPreview();await flush()},
    ready:()=>{if(kind==='OFFICE')api.handleOfficeOpened({documentUrl:api.resolvedOnlyOfficeDocumentUrl.value});else api.handleMediaLoaded({currentTarget:{dataset:{previewGeneration:String(api.renderVersion.value)},getAttribute:()=>api.mediaObjectUrl.value,currentSrc:api.mediaObjectUrl.value}})},
    blur:async()=>{focused=false;listeners.get('window:blur')?.();await flush()},
    focus:async()=>{focused=true;listeners.get('window:focus')?.();await flush()},
    hidden:async()=>{document.visibilityState='hidden';listeners.get('document:visibilitychange')?.();await flush()},
    visible:async()=>{document.visibilityState='visible';listeners.get('document:visibilitychange')?.();await flush()},
    tick:async()=>{for(const fn of [...timers.values()])fn();await flush()},
    unmount:async()=>{hooks.unmount();await flush()}
  }
}
async function renderStats(r) {
  const compiled=compileTemplate({source:r.descriptor.template.content,filename:'index.vue',id:'reading-production-template'})
  assert.deepEqual(compiled.errors,[])
  const c={exports:{},require:()=>vue};vm.runInNewContext(compile(compiled.code),c)
  const renderer=vue.createRenderer({
    createElement:tag=>({tag,children:[],props:{}}),createText:text=>({text}),createComment:comment=>({comment}),
    insert:(node,parent)=>{node.parent=parent;parent.children.push(node)},remove:node=>{const i=node.parent?.children.indexOf(node);if(i>=0)node.parent.children.splice(i,1)},
    setText:(node,text)=>{node.text=text},setElementText:(node,text)=>{node.text=text;node.children=[]},
    parentNode:node=>node.parent,nextSibling:()=>null,patchProp:(node,key,old,value)=>{node.props[key]=value}
  })
  const app=renderer.createApp({setup:()=>({...r.api,...r.props}),render:c.exports.render})
  for(const name of ['el-button','el-input','el-alert','el-empty']) app.component(name,{setup:(p,{slots})=>()=>vue.h('span',slots.default?.())})
  app.component('OnlyOfficeReadOnlyViewer',{setup:()=>()=>vue.h('span')})
  app.directive('loading',{})
  const root={children:[]};app.mount(root);await flush()
  const texts=[],titles=[];const walk=node=>{if(node.text)texts.push(node.text);if(node.props?.title)titles.push(node.props.title);for(const child of node.children||[])walk(child)};walk(root)
  return {text:texts.join(' '),titles,unmount:()=>app.unmount()}
}
test('signature pause drains summary and blocks focus and late body callbacks from restarting reading',async()=>{
  const summary=deferred(),r=load({summaryGate:summary});await r.load()
  let settled=false
  const pause=r.hooks.exposed?.pauseReading()
  Promise.resolve(pause).then(()=>{settled=true})
  await r.blur();await r.focus();r.api.markRegisteredPreviewVisible(r.api.renderVersion.value);await flush()
  assert.equal(r.startCalls().length,1,'focus and body-ready must not restart a reader paused for signature')
  assert.equal(r.calls.filter(c=>c[0]==='summary').length,1,'pause must not send a new summary after stop')
  assert.equal(settled,false,'signature pause must wait for the already issued summary')
  summary.resolve(sum(10));await pause;await flush();assert.equal(settled,true);await r.unmount()
})
test('actual viewer waits for real TEXT body then starts and reads independent server summary',async()=>{
  const gate=deferred(),r=load({bodyGate:gate}),pending=r.api.loadPreview()
  await flush();assert.equal(r.startCalls().length,0);gate.resolve();await pending;await flush()
  assert.equal(r.startCalls().length,1,'production viewer must consume reading lifecycle after TEXT ready')
  assert.equal(r.calls.filter(c=>c[0]==='summary').length,1)
  assert.equal(r.api.readingSummary.value.accumulatedSeconds,10)
  assert.equal(r.api.registeredPreviewVisible.value,true)
  assert.match(r.descriptor.template.content,/累计有效阅读时间/);assert.match(r.descriptor.template.content,/首次打开/);assert.match(r.descriptor.template.content,/最近打开/)
  const rendered=await renderStats(r)
  assert.match(rendered.text,/累计有效阅读时间：10 秒/);assert.match(rendered.text,/首次打开：.*08:00:01/);assert.match(rendered.text,/最近打开：.*08:00:11/)
  assert.match(rendered.text,/本次预览登记时间：.*08:00:00/);rendered.unmount()
  await r.unmount()
})
test('PDF start waits for actual render; media starts only on matching DOM load',async()=>{
  const gate=deferred(),pdf=load({kind:'PDF',pdfGate:gate}),pending=pdf.api.loadPreview();await flush();assert.equal(pdf.startCalls().length,0)
  gate.resolve();await pending;await flush();assert.equal(pdf.startCalls().length,1);await pdf.unmount()
  for(const kind of ['IMAGE','AUDIO','VIDEO']) {
    const r=load({kind});await r.load();assert.equal(r.startCalls().length,0)
    r.api.handleMediaLoaded({currentTarget:{dataset:{previewGeneration:'-1'},getAttribute:()=>r.api.mediaObjectUrl.value,currentSrc:r.api.mediaObjectUrl.value}})
    await flush();assert.equal(r.startCalls().length,0)
    r.ready();await flush();assert.equal(r.startCalls().length,1,kind+' real ready must start reading');await r.unmount()
  }
})
test('Office starts once only for current opened URL and error stops its session',async()=>{
  const r=load({kind:'OFFICE'});await r.load()
  r.api.handleOfficeOpened({documentUrl:'old-office'});await flush();assert.equal(r.startCalls().length,0)
  r.ready();r.ready();await flush();assert.equal(r.startCalls().length,1)
  r.api.handleOfficeError({documentUrl:r.api.resolvedOnlyOfficeDocumentUrl.value,message:'office render failed'});await flush()
  assert.equal(r.stops().length,1);assert.equal(r.timers.size,0);await r.unmount()
})
test('noncontract sources, local files, failed loads and download-only never acquire ordinary reading sessions',async()=>{
  for(const source of [
    {type:'DCC_CONTROLLED_FILE',controlledFileId:'910',distribution:{distributionId:'1',recipientId:'2'}},
    {type:'DCC_CONTROLLED_FILE_ATTACHMENT',controlledFileId:'910',attachmentId:'1'},
    {type:'DCC_REGISTRATION_CERTIFICATE',businessFileId:'910'}]) {
    const r=load({source});await r.load();assert.equal(r.startCalls().length,0);assert.equal(r.calls.filter(c=>c[0]==='summary').length,0);await r.unmount()
  }
  for(const args of [{local:true},{kind:'DOWNLOAD_ONLY'},{previewError:'denied'}]) {const r=load(args);await r.load();assert.equal(r.startCalls().length,0);await r.unmount()}
})
test('real visibility/focus listeners pause and resume, and unmount removes listeners and timer',async()=>{
  const r=load();await r.load();assert.equal(r.startCalls().length,1)
  await r.blur();assert.equal(r.timers.size,0);assert.equal(r.stops().length,1)
  await r.focus();assert.equal(r.startCalls().length,2);await r.hidden();assert.equal(r.timers.size,0)
  await r.focus();assert.equal(r.startCalls().length,2)
  await r.visible();assert.equal(r.startCalls().length,3);await r.unmount()
  assert.equal(r.timers.size,0);assert.equal(r.listeners.size,0)
})
test('switch while start is pending waits for old stop before starting new exact source',async()=>{
  const gate=deferred(),r=load({startGate:gate});await r.load();assert.equal(r.startCalls().length,1)
  r.props.controlledFileId='911';await r.load();assert.equal(r.startCalls().length,1)
  gate.resolve(row());await flush()
  assert.equal(r.stops()[0][1].url,'/dcc/controlled-files/910/reading-session/stop')
  assert.equal(r.startCalls().length,2);assert.equal(r.startCalls()[1][1].url,'/dcc/controlled-files/911/reading-session/start')
  assert.equal(r.startCalls()[1][1].headers['X-DCC-Viewer-Token'],'signed-911');await r.unmount()
})
test('late summary from old version cannot replace current statistics',async()=>{
  const gate=deferred(),r=load({summaryGate:gate});await r.load()
  r.props.controlledFileId='911';await r.load();await flush()
  assert.equal(r.api.readingSummary.value.accumulatedSeconds,22)
  gate.resolve(sum(99));await flush();assert.equal(r.api.readingSummary.value.accumulatedSeconds,22);await r.unmount()
})
test('expired token is visible, stops heartbeat and does not auto retry on focus; explicit reload gets fresh lifecycle',async()=>{
  const r=load({heartbeatError:'viewer token expired'});await r.load();await r.tick()
  assert.match(r.api.readingError.value,/viewer token expired/);assert.equal(r.timers.size,0)
  const starts=r.startCalls().length;await r.blur();await r.focus();assert.equal(r.startCalls().length,starts)
  assert.match(r.descriptor.template.content,/@click="loadPreview"/);await r.load();assert.equal(r.startCalls().length,starts+1);await r.unmount()
})
test('summary failure is explicit and does not fabricate zero duration or lose accepted session cleanup',async()=>{
  const r=load({summaryError:'reading schema unavailable'});await r.load()
  assert.match(r.api.readingError.value,/reading schema unavailable/);assert.equal(r.api.readingSummary.value,null)
  await r.unmount();assert.equal(r.stops().length,1);assert.equal(r.timers.size,0)
})
test('unmount during pending start closes the returned original session without reviving UI or timers',async()=>{
  const gate=deferred(),r=load({startGate:gate});await r.load();await r.unmount()
  gate.resolve(row());await flush();assert.equal(r.stops().length,1);assert.equal(r.timers.size,0);assert.equal(r.listeners.size,0)
  assert.equal(r.api.readingSummary.value,null)
})
test('pause/stop failure during pagehide remains visible on the current viewer owner',async()=>{
  const r=load({stopError:'viewer token expired while closing'})
  await r.load();assert.equal(r.startCalls().length,1)
  await r.blur()
  assert.match(r.api.readingError.value,/expired while closing/)
  assert.equal(r.timers.size,0)
  await r.focus()
  assert.equal(r.startCalls().length,1,'failed stop must not silently resume the old capability')
  await r.unmount()
})
test('dispose stop failure while switching source stays visible as unconfirmed cleanup instead of false success',async()=>{
  const r=load({stopError:'old capability stop denied'})
  await r.load();r.props.controlledFileId='911';await r.load();await flush()
  assert.match(r.api.readingCloseError?.value || '',/old capability stop denied/)
  assert.match(r.descriptor.template.content,/readingCloseError/)
  assert.equal(r.api.readingSummary.value.accumulatedSeconds,22,'new source summary remains its own server facts')
  assert.equal(r.stops()[0][1].url,'/dcc/controlled-files/910/reading-session/stop')
  const rendered=await renderStats(r)
  assert.ok(rendered.titles.some(title=>title.includes('上一正文阅读会话结束未确认：old capability stop denied')))
  rendered.unmount()
  await r.unmount()
})
test('successful heartbeat and pause refresh persisted totals rather than inventing client elapsed time',async()=>{
  const r=load({summarySeconds:[10,20,23]});await r.load();assert.equal(r.api.readingSummary.value.accumulatedSeconds,10)
  await r.tick();assert.equal(r.api.readingSummary.value.accumulatedSeconds,20)
  await r.blur();assert.equal(r.api.readingSummary.value.accumulatedSeconds,23);assert.equal(r.api.readingError.value,'');await r.unmount()
})
test('ready while hidden reads existing server facts but waits for focus and visibility to start',async()=>{
  const r=load();await r.hidden();await r.load();assert.equal(r.startCalls().length,0);assert.equal(r.api.readingSummary.value.accumulatedSeconds,10)
  await r.visible();assert.equal(r.startCalls().length,1);await r.unmount()
})
test('unified storage preview uses its independent lifecycle route and never guesses DCC body identity',async()=>{
  const r=load({source:{type:'EDHR_SPECIAL_NODE_ATTACHMENT',fileId:'7001'}});await r.load()
  assert.equal(r.startCalls().length,1);assert.equal(r.startCalls()[0][1].url,'/dcc/file-preview/files/7001/reading-session/start')
  assert.equal(r.startCalls()[0][1].headers['X-DCC-Body-Read-Context'],undefined);await r.unmount()
})
test('browser pagehide pauses even before visibility changes, and pageshow resumes the same formal source',async()=>{
  const r=load();await r.load()
  assert.equal(typeof r.listeners.get('window:pagehide'),'function')
  r.listeners.get('window:pagehide')();await flush();assert.equal(r.stops().length,1);assert.equal(r.timers.size,0)
  r.listeners.get('window:pageshow')();await flush();assert.equal(r.startCalls().length,2)
  await r.unmount();assert.equal(r.listeners.size,0)
})
test('missing client-session identity dependency stays a reported failure and focus cannot retry it',async()=>{
  const r=load({uuidError:'secure random UUID unavailable'});await r.load()
  assert.match(r.api.readingError.value,/secure random UUID unavailable/);assert.equal(r.startCalls().length,0)
  await r.blur();await assert.doesNotReject(()=>r.focus());assert.equal(r.startCalls().length,0)
  await r.unmount()
})
test('server expiry at the fifteen-minute boundary stops timing without silently renewing metadata',async()=>{
  const clock={ageSeconds:0},r=load({heartbeatError:()=>clock.ageSeconds>=900?'viewer token expired at 900 seconds':undefined})
  await r.load();clock.ageSeconds=899;await r.tick();assert.equal(r.api.readingError.value,'')
  clock.ageSeconds=900;await r.tick();assert.match(r.api.readingError.value,/expired at 900 seconds/);assert.equal(r.timers.size,0)
  assert.equal(r.calls.filter(c=>c[0]==='metadata').length,1)
  await r.blur();await r.focus();assert.equal(r.startCalls().length,1)
  await r.unmount()
})
test('pagehide remains suspended through queued focus until pageshow explicitly restores the page',async()=>{
  const r=load();await r.load()
  r.listeners.get('window:pagehide')();await flush()
  assert.equal(r.stops().length,1);assert.equal(r.timers.size,0)
  await r.focus()
  assert.equal(r.startCalls().length,1,'pagehide must not be undone by stale focus before pageshow')
  assert.equal(r.timers.size,0)
  r.listeners.get('window:pageshow')();await flush()
  assert.equal(r.startCalls().length,2);await r.unmount()
})
test('body becoming ready after pagehide cannot start reading before pageshow',async()=>{
  const gate=deferred(),r=load({bodyGate:gate}),pending=r.api.loadPreview()
  await flush();r.listeners.get('window:pagehide')();await flush()
  gate.resolve();await pending;await flush()
  assert.equal(r.startCalls().length,0,'late ready must respect pagehide even while document visibility still reads visible')
  await r.focus();assert.equal(r.startCalls().length,0)
  r.listeners.get('window:pageshow')();await flush();assert.equal(r.startCalls().length,1);await r.unmount()
})
test('changed cached actor or tenant while metadata is pending cannot publish or read the old preview',async()=>{
  for(const [field,value] of [['actorId','100'],['baseTenantId','32'],['visitTenantId','33']]) {
    const gate=deferred(),r=load({metadataGate:gate}),pending=r.api.loadPreview()
    await flush();r.identity[field]=value;gate.resolve();await pending;await flush()
    assert.equal(r.calls.filter(c=>c[0]==='body').length,0,field+' stale metadata must not read body')
    assert.equal(r.api.resolvedPreviewMetadata.value,null)
    assert.match(r.api.errorMessage.value,/用户|租户|身份/)
    assert.equal(r.startCalls().length,0);await r.unmount()
  }
})
test('changed cached actor or tenant during binary read cannot publish old text or registration',async()=>{
  for(const [field,value] of [['actorId','100'],['baseTenantId','32'],['visitTenantId','33']]) {
    const gate=deferred(),r=load({bodyGate:gate}),pending=r.api.loadPreview()
    await flush();r.identity[field]=value;gate.resolve();await pending;await flush()
    assert.equal(r.api.textPreviewContent.value,'',field+' stale bytes must not be published')
    assert.equal(r.api.registeredPreviewVisible.value,false)
    assert.equal(r.startCalls().length,0);await r.unmount()
  }
})
test('active owner invalidates old content and counters before tick when cached identity changes',async()=>{
  for(const [field,value] of [['actorId','100'],['baseTenantId','32'],['visitTenantId','33']]) {
    const r=load();await r.load();r.identity[field]=value;await r.tick()
    assert.equal(r.calls.filter(c=>c[0]==='post'&&c[1].url.endsWith('/heartbeat')).length,0,field)
    assert.equal(r.stops().length,0,'do not send old stop capability under changed actor/tenant')
    assert.equal(r.api.textPreviewContent.value,'')
    assert.equal(r.api.readingSummary.value,null)
    assert.equal(r.timers.size,0)
    await r.focus();assert.equal(r.startCalls().length,1);await r.unmount()
  }
})
test('old summary after cached actor change is discarded and cannot keep old content visible',async()=>{
  const gate=deferred(),r=load({summaryGate:gate});await r.load()
  r.identity.actorId='100';gate.resolve(sum(99));await flush()
  assert.equal(r.api.readingSummary.value,null)
  assert.equal(r.api.textPreviewContent.value,'')
  assert.equal(r.timers.size,0);await r.unmount()
})
test('Office opened after cached tenant change cannot emit opened or retain its old document URL',async()=>{
  const r=load({kind:'OFFICE'});await r.load();r.identity.visitTenantId='33';r.ready();await flush()
  assert.equal(r.emitted.filter(e=>e[0]==='opened').length,0)
  assert.equal(r.api.resolvedOnlyOfficeDocumentUrl.value,'')
  assert.equal(r.startCalls().length,0);await r.unmount()
})
test('storage identity invalidation clears current body without waiting for the next heartbeat',async()=>{
  const r=load();await r.load();r.identity.actorId='100'
  r.listeners.get('window:storage')();await flush()
  assert.equal(r.api.textPreviewContent.value,'');assert.equal(r.api.readingSummary.value,null)
  assert.equal(r.timers.size,0);await r.unmount()
})
test('base tenant changes invalidate owner even if visit tenant stays the same',async()=>{
  const r=load();r.identity.visitTenantId='33';await r.load();r.identity.baseTenantId='32';await r.tick()
  assert.equal(r.calls.filter(c=>c[0]==='post'&&c[1].url.endsWith('/heartbeat')).length,0)
  assert.equal(r.api.textPreviewContent.value,'');assert.equal(r.api.readingSummary.value,null);await r.unmount()
})
test('removing the access token invalidates cached user content and never sends the old capability',async()=>{
  const r=load();await r.load();r.identity.token=null;await r.tick()
  assert.equal(r.api.textPreviewContent.value,'');assert.equal(r.api.readingSummary.value,null)
  assert.equal(r.stops().length,0);assert.equal(r.timers.size,0);await r.unmount()
})
test('identity loss remains latched until explicit reload captures the new identity',async()=>{
  const r=load();await r.load();r.identity.actorId='100';await r.tick()
  await r.focus();assert.equal(r.startCalls().length,1);assert.equal(r.api.textPreviewContent.value,'')
  await r.load();assert.equal(r.startCalls().length,2);assert.equal(r.api.errorMessage.value,'')
  assert.equal(r.api.textPreviewContent.value,'real text');await r.unmount()
})
test('late start after actor change cannot restore owner or send old stop under the new identity',async()=>{
  const gate=deferred(),r=load({startGate:gate});await r.load();r.identity.actorId='100'
  gate.resolve(row());await flush()
  assert.equal(r.api.textPreviewContent.value,'');assert.equal(r.api.readingSummary.value,null)
  assert.equal(r.stops().length,0);assert.equal(r.timers.size,0)
  assert.match(r.api.errorMessage.value,/用户|租户|身份/);await r.unmount()
})

const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs'), vm = require('node:vm'), ts = require('typescript'), vue = require('vue')
const { parse } = require('vue/compiler-sfc')
const script = parse(fs.readFileSync('src/views/dcc/controlled-file/view/index.vue', 'utf8')).descriptor.scriptSetup.content
const ast = ts.createSourceFile('viewer.ts', script, ts.ScriptTarget.Latest, true)
const deferred = () => { let resolve, reject; const promise = new Promise((a,b) => { resolve=a; reject=b }); return { promise, resolve, reject } }
function load({ kind='TEXT', metadataTime='2026-10-15T00:30:10Z', bodyGate, local=false }={}) {
  const names=['registeredPreviewVisible','clearRegisteredPreview','registeredPreviewLabel','markRegisteredPreviewVisible','handleMediaLoaded','handleMediaError','handleOfficeOpened','handleOfficeError','loadPreview']
  const statements=names.map(name => {
    const statement=ast.statements.find(node=>ts.isVariableStatement(node)&&node.declarationList.declarations.some(d=>d.name.text===name))
    assert.ok(statement,'actual viewer declaration missing: '+name)
    return statement.getText(ast)
  }).join('\n')
  const metadata={previewKind:kind,fileName:'selected-body',previewRegisteredAt:metadataTime,
    onlyofficeDocumentUrl:'signed-office-url',watermark:{timestamp:'1999-01-01T00:00:00Z'},watermarkTraceCode:'trace'}
  const host={exports:{},...vue,TextDecoder,Intl,Date,Number,URL:{createObjectURL:()=> 'blob:exact'},
    props:{title:'所选正文',...(local?{previewKind:kind,previewBlob:new Uint8Array([1,2])}: {})},
    previewUnmounted:false,renderVersion:vue.ref(0),loading:vue.ref(false),errorMessage:vue.ref(''),
    resolvedPreviewMetadata:vue.shallowRef(null),resolvedPreviewKind:vue.ref(''),resolvedOnlyOfficeBaseUrl:vue.ref(''),
    resolvedOnlyOfficeDocumentUrl:vue.ref(''),resolvedPreviewUnavailableReason:vue.ref(''),resolvedFileName:vue.ref(''),
    resolvedWatermark:vue.ref(null),mediaObjectUrl:vue.ref(''),textPreviewContent:vue.ref(''),currentPdfBytes:vue.ref(null),
    pdfDocument:vue.ref(null),viewerFrameRef:vue.ref(null),resolvedOnlinePreviewSource:vue.ref(local?null:{type:'DCC_CONTROLLED_FILE',controlledFileId:'910'}),
    isCurrentRenderVersion:generation=>!host.previewUnmounted&&generation===host.renderVersion.value,
    resetViewerTransformState(){},resetPreviewState:async()=>{},normalizePreviewKind:value=>value,
    getOnlineFilePreviewMetadata:async()=>metadata,withTraceableWatermark:value=>value,
    resolvePreviewBlob:async()=>bodyGate?bodyGate.promise:{bytes:new TextEncoder().encode('所选正文'),blob:{}},
    getProtectedViewerRenderScale:()=>1,renderPdfPages:async()=>{host.pdfDocument.value={}},loadPdfTextIndex:async()=>{},
    resolveViewerErrorMessage:error=>error.message,emit(){} }
  vm.runInNewContext(ts.transpileModule(statements+'\nexports.api={'+names.join(',')+'}',{
    compilerOptions:{target:ts.ScriptTarget.ES2022,module:ts.ModuleKind.CommonJS}}).outputText,host)
  return { host,api:host.exports.api,metadata }
}
test('formal registration stays hidden while only metadata exists; real text success shows the exact Shanghai instant',async()=>{
  const gate=deferred(),r=load({bodyGate:gate}),pending=r.api.loadPreview()
  await vue.nextTick();await vue.nextTick()
  assert.equal(r.api.registeredPreviewVisible.value,false)
  gate.resolve({bytes:new TextEncoder().encode('所选正文'),blob:{}});await pending
  assert.equal(r.api.registeredPreviewVisible.value,true)
  assert.match(r.api.registeredPreviewLabel.value,/08:30:10/)
  assert.match(r.api.registeredPreviewLabel.value,/Asia\/Shanghai/)
  assert.equal(r.api.registeredPreviewLabel.value.includes('1999'),false)
})
test('loading a new source clears time before await; failed reads and local blobs cannot fabricate registration',async()=>{
  const r=load();await r.api.loadPreview();assert.equal(r.api.registeredPreviewVisible.value,true)
  const gate=deferred();r.host.resolvePreviewBlob=()=>gate.promise
  const pending=r.api.loadPreview();assert.equal(r.api.registeredPreviewVisible.value,false)
  gate.reject(new Error('正式读取失败'));await pending
  assert.equal(r.api.registeredPreviewVisible.value,false);assert.equal(r.host.errorMessage.value,'正式读取失败')
  const local=load({local:true});await local.api.loadPreview();assert.equal(local.api.registeredPreviewLabel.value,'未记录')
  assert.equal(local.host.resolvedPreviewMetadata.value,null)
})
test('media needs actual matching DOM load; stale generation, another src and error do not show success',async()=>{
  const r=load({kind:'IMAGE'});await r.api.loadPreview();assert.equal(r.api.registeredPreviewVisible.value,false)
  const element={dataset:{previewGeneration:String(r.host.renderVersion.value)},getAttribute:()=> 'blob:exact',currentSrc:'blob:exact'}
  r.api.handleMediaLoaded({currentTarget:{...element,dataset:{previewGeneration:'0'}}});assert.equal(r.api.registeredPreviewVisible.value,false)
  r.api.handleMediaLoaded({currentTarget:{...element,currentSrc:'blob:other'}});assert.equal(r.api.registeredPreviewVisible.value,false)
  r.api.handleMediaLoaded({currentTarget:element});assert.equal(r.api.registeredPreviewVisible.value,true)
  r.api.handleMediaError({currentTarget:element});assert.equal(r.api.registeredPreviewVisible.value,false)
  assert.ok(r.host.errorMessage.value)
})
test('PDF renders before showing registration, Office only its precise opened URL, and missing formal time stays unrecorded',async()=>{
  const pdf=load({kind:'PDF'});await pdf.api.loadPreview();assert.equal(pdf.api.registeredPreviewVisible.value,true)
  const office=load({kind:'OFFICE'});await office.api.loadPreview();assert.equal(office.api.registeredPreviewVisible.value,false)
  office.api.handleOfficeOpened({documentUrl:'old-url'});assert.equal(office.api.registeredPreviewVisible.value,false)
  office.api.handleOfficeOpened({documentUrl:'signed-office-url'});assert.equal(office.api.registeredPreviewVisible.value,true)
  office.api.handleOfficeError({documentUrl:'old-url',message:'旧错误'});assert.equal(office.api.registeredPreviewVisible.value,true)
  office.api.handleOfficeError({documentUrl:'signed-office-url',message:'当前Office错误'});assert.equal(office.api.registeredPreviewVisible.value,false)
  assert.equal(office.host.errorMessage.value,'当前Office错误')
  office.host.previewUnmounted=true;office.api.clearRegisteredPreview();office.api.handleOfficeOpened({documentUrl:'signed-office-url'})
  assert.equal(office.api.registeredPreviewVisible.value,false)
  const missing=load({metadataTime:null});await missing.api.loadPreview();assert.equal(missing.api.registeredPreviewLabel.value,'未记录')
})

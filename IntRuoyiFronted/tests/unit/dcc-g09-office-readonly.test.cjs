const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
function office(onEvent) {
  const source = fs.readFileSync(path.resolve(__dirname, '../../src/views/dcc/controlled-file/view/OnlyOfficeReadOnlyViewer.vue'), 'utf8')
  const descriptor = parse(source).descriptor
  const props = vue.reactive({ baseUrl: 'https://office.example.test', documentUrl: 'https://doc.example.test/exact-1.docx?ticket=a', documentTitle: '正式.docx' })
  const configs = [], emitted = [], lifecycle = {}, destroyed = []
  const host = { exports: {}, ...vue, Error, props, window: {
    __dccOnlyOfficeScriptBaseUrl: props.baseUrl,
    DocsAPI: { DocEditor: class { constructor(id, config) { configs.push(config); this.id = configs.length } destroyEditor() { destroyed.push(this.id) } } },
    addEventListener() {}, removeEventListener() {}
  }, document: { createElement() { throw new Error('unexpected script load') } },
    defineProps: () => props, defineEmits: () => (...args) => { emitted.push(args); onEvent?.(...args) },
    watch() {}, onMounted() {}, onBeforeUnmount: callback => { lifecycle.unmount = callback }
  }
  const names=['mountEditor','increaseZoom','previewZoomValue','handleWheelZoom','isPointerInsideStage','isZoomModifierPressed','errorMessage','previewRotationDegrees','rotatePreview','resetView','scalerStyle','viewportStyle','mountRef','stageRef']
  vm.runInNewContext(ts.transpileModule(descriptor.scriptSetup.content + '\nexports.api={'+names.map(name=>`${name}:typeof ${name} === "undefined" ? undefined : ${name}`).join(',')+'}', {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText, host)
  return { api: host.exports.api, props, configs, emitted, lifecycle, destroyed, descriptor }
}

test('actual Office editor receives only view permissions, with no persistence callback', async () => {
  const r = office()
  await r.api.mountEditor()
  const config = r.configs[0]
  assert.equal(config.height,'100%','native editor must fit the rotated/scaled stage height')
  assert.equal(config.editorConfig.mode, 'view')
  for (const name of ['edit', 'comment', 'review', 'download', 'print', 'copy']) assert.equal(config.document.permissions[name], false)
  assert.equal(config.document.url, r.props.documentUrl)
  assert.equal(config.editorConfig.callbackUrl, undefined)
  assert.equal(config.events.onRequestSaveAs, undefined)
  config.events.onDocumentReady()
  config.events.onDocumentReady()
  assert.equal(r.emitted.length, 1)
  assert.equal(r.emitted[0][1].documentUrl, r.props.documentUrl)
})

test('Office switching destroys old editor, resets view controls and rejects stale opened events', async () => {
  const r = office()
  await r.api.mountEditor()
  r.api.increaseZoom()
  r.api.isPointerInsideStage.value = true
  r.api.isZoomModifierPressed.value = true
  const first = r.configs[0]
  r.props.documentUrl = 'https://doc.example.test/exact-2.docx?ticket=b'
  await r.api.mountEditor()
  assert.deepEqual(r.destroyed, [1])
  assert.equal(r.api.previewZoomValue.value, 100)
  assert.equal(r.api.isZoomModifierPressed.value, false)
  first.events.onDocumentReady()
  assert.equal(r.emitted.length, 0)
  r.lifecycle.unmount()
  r.configs[1].events.onDocumentReady()
  assert.equal(r.emitted.length, 0)
})

test('Office visible rotation and reset only change viewport geometry and preserve native document identity', async () => {
  const r=office()
  await r.api.mountEditor()
  assert.equal(typeof r.api.rotatePreview,'function','visible read-only rotation must be implemented')
  r.api.stageRef.value={clientWidth:1000}
  const before=r.configs[0]
  r.api.rotatePreview()
  r.api.increaseZoom()
  assert.equal(r.api.previewRotationDegrees.value,90)
  assert.match(r.api.scalerStyle.value.transform,/rotate\(90deg\)/)
  assert.equal(r.api.scalerStyle.value.width,'1000px','native document keeps its logical viewport width')
  assert.equal(r.api.scalerStyle.value.height,'720px','native document keeps its logical viewport height')
  assert.equal(r.api.viewportStyle.value.width,'792px','scrollable width contains the rotated height at 110%')
  assert.equal(r.api.viewportStyle.value.height,'1100px','scrollable height contains the rotated width at 110%')
  assert.equal(r.configs.length,1,'visual tools must not recreate editor or request another file')
  assert.equal(before.document.url,r.props.documentUrl)
  for(const name of ['edit','comment','review','download','print','copy']) assert.equal(before.document.permissions[name],false)
  r.api.resetView()
  assert.equal(r.api.previewZoomValue.value,100)
  assert.equal(r.api.previewRotationDegrees.value,0)
  r.api.rotatePreview();r.api.rotatePreview();r.api.rotatePreview();r.api.rotatePreview()
  assert.equal(r.api.previewRotationDegrees.value,0)
  r.api.rotatePreview()
  r.props.documentUrl='https://doc.example.test/next.docx?ticket=exact'
  await r.api.mountEditor()
  assert.equal(r.api.previewRotationDegrees.value,0)
  assert.equal(r.api.previewZoomValue.value,100)
  const elements=[]
  const walk=node=>{if(node.type===1)elements.push(node);for(const child of node.children||[])walk(child)}
  walk(r.descriptor.template.ast)
  for(const [label,handler] of [['旋转预览','rotatePreview'],['复原预览','resetView']]){
    const control=elements.find(n=>n.props.some(p=>p.type===6&&p.name==='aria-label'&&p.value?.content===label))
    assert.ok(control,label+' must exist in actual template')
    assert.ok(control.props.some(p=>p.type===7&&p.name==='on'&&p.exp?.content===handler))
  }
})

test('Office document-service errors remain visible and never emit success', async () => {
  const r = office()
  await r.api.mountEditor()
  r.configs[0].events.onError({ data: { errorCode: -4, errorDescription: 'exact permission denied' } })
  r.configs[0].events.onDocumentReady()
  assert.match(r.api.errorMessage.value, /exact permission denied/)
  assert.equal(r.emitted.filter(event => event[0] === 'opened').length, 0)
  assert.deepEqual(r.destroyed, [1])
})

test('Office real service error after opened emits the exact URL and displayed message once', async () => {
  const r=office()
  await r.api.mountEditor()
  const config=r.configs[0]
  const documentUrl=r.props.documentUrl
  config.events.onDocumentReady()
  assert.equal(r.emitted.length,1)
  assert.equal(r.emitted[0][0],'opened')
  const failure={data:{errorCode:-4,errorDescription:'exact service failure after opened'}}
  config.events.onError(failure)
  assert.equal(r.emitted.length,2,'the subscribed parent must receive the failure after a successful opened event')
  assert.equal(r.emitted[1][0],'error')
  assert.equal(r.emitted[1][1].documentUrl,documentUrl)
  assert.equal(r.emitted[1][1].message,r.api.errorMessage.value)
  assert.match(r.api.errorMessage.value,/错误码 -4，exact service failure after opened/)
  config.events.onError(failure)
  config.events.onDocumentReady()
  assert.equal(r.emitted.length,2,'failed generation cannot replay error or opened')
  assert.deepEqual(r.destroyed,[1])
})

test('Office late old-URL errors before remount and late old-generation errors cannot poison current view', async () => {
  const r=office()
  await r.api.mountEditor()
  const first=r.configs[0]
  r.props.documentUrl='https://doc.example.test/exact-2.docx?ticket=b'
  first.events.onError({data:{errorDescription:'old URL before watch'}})
  assert.equal(r.api.errorMessage.value,'','URL change must invalidate error even before watcher mounts the new editor')
  assert.equal(r.emitted.length,0)
  assert.deepEqual(r.destroyed,[])
  await r.api.mountEditor()
  first.events.onError({data:{errorDescription:'old URL after watch'}})
  assert.equal(r.api.errorMessage.value,'')
  assert.equal(r.emitted.length,0)
  const second=r.configs[1]
  await r.api.mountEditor() // same URL, new generation
  second.events.onError({data:{errorDescription:'same URL old generation'}})
  assert.equal(r.emitted.length,0)
  assert.equal(r.api.errorMessage.value,'')
  assert.deepEqual(r.destroyed,[1,2])
  r.configs[2].events.onError({data:{errorDescription:'current service failure'}})
  assert.equal(r.emitted.length,1)
  assert.equal(r.emitted[0][0],'error')
  assert.equal(r.emitted[0][1].documentUrl,r.props.documentUrl)
  assert.match(r.emitted[0][1].message,/current service failure/)
})

test('Office late service errors after unmount emit nothing and do not restore error state', async () => {
  const r=office()
  await r.api.mountEditor()
  const current=r.configs[0]
  current.events.onDocumentReady()
  r.lifecycle.unmount()
  const count=r.emitted.length
  current.events.onError({data:{errorDescription:'unmounted failure'}})
  assert.equal(r.emitted.length,count)
  assert.equal(r.api.errorMessage.value,'')
  assert.deepEqual(r.destroyed,[1])
})

test('actual child service error composes with the subscribed real viewer handler to clear registered time', async () => {
  const parentFile=path.resolve(__dirname,'../../src/views/dcc/controlled-file/view/index.vue')
  const parent=parse(fs.readFileSync(parentFile,'utf8')).descriptor
  const ast=ts.createSourceFile('viewer.ts',parent.scriptSetup.content,ts.ScriptTarget.ES2022,true)
  const declaration=ast.statements.find(node=>ts.isVariableStatement(node)&&node.declarationList.declarations.some(d=>d.name.getText(ast)==='handleOfficeError'))
  assert.ok(declaration,'shared owner already supplied the formal parent handler')
  const registered=vue.ref(true), errorMessage=vue.ref(''), documentUrl=vue.ref('https://doc.example.test/exact-1.docx?ticket=a')
  const c={exports:{},previewUnmounted:false,resolvedOnlyOfficeDocumentUrl:documentUrl,resolvedPreviewKind:vue.ref('OFFICE'),errorMessage,clearRegisteredPreview:()=>{registered.value=false}}
  vm.runInNewContext(ts.transpileModule(declaration.getText(ast)+'\nexports.handle=handleOfficeError',{
    compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}
  }).outputText,c)
  const r=office((name,payload)=>{if(name==='error') c.exports.handle(payload)})
  await r.api.mountEditor()
  r.configs[0].events.onDocumentReady()
  assert.equal(registered.value,true)
  r.configs[0].events.onError({data:{errorDescription:'actual Office body failed'}})
  assert.equal(registered.value,false)
  assert.equal(errorMessage.value,r.api.errorMessage.value)
  assert.match(errorMessage.value,/actual Office body failed/)
  documentUrl.value='https://doc.example.test/exact-2.docx?ticket=b'
  r.props.documentUrl=documentUrl.value
  await r.api.mountEditor()
  r.configs[1].events.onDocumentReady()
  registered.value=true;errorMessage.value=''
  r.configs[0].events.onError({data:{errorDescription:'old failure must not clear new time'}})
  assert.equal(registered.value,true)
  assert.equal(errorMessage.value,'')
  c.previewUnmounted=true;r.lifecycle.unmount()
  r.configs[1].events.onError({data:{errorDescription:'unmounted failure'}})
  assert.equal(errorMessage.value,'')
})

test('Office SFC retains watermarks and compiles', () => {
  const r = office()
  const script = compileScript(r.descriptor, { id: 'g09-office' })
  const compiled = compileTemplate({ source: r.descriptor.template.content, filename: 'OnlyOfficeReadOnlyViewer.vue', id: 'g09-office', compilerOptions: { bindingMetadata: script.bindings } })
  assert.deepEqual(compiled.errors, [])
  const classes = []
  const walk = node => { if (node.type === 1) classes.push(...node.props.filter(p => p.type === 6 && p.name === 'class').map(p => p.value?.content)); for (const child of node.children || []) walk(child) }
  walk(r.descriptor.template.ast)
  assert.ok(classes.includes('onlyoffice-viewer-frame__overlay'))
  assert.ok(classes.includes('onlyoffice-viewer-frame__watermark'))
})

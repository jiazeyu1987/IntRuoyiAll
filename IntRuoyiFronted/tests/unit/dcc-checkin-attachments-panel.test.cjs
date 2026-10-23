const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
const filename = 'src/views/dcc/controlled-file/browser/CheckinAttachmentsPanel.vue'
async function mount(port = {}) {
  const source = fs.readFileSync(filename, 'utf8')
  const { descriptor } = parse(source)
  const script = compileScript(descriptor, { id: 'g06-checkin-attachments' })
  const template = compileTemplate({ source: descriptor.template.content, filename, id: 'g06-checkin-attachments', compilerOptions: { bindingMetadata: script.bindings } })
  assert.deepEqual(template.errors, [])
  const uploads = [], cleanups = [], events = []
  let sequence = 0
  const context = { exports: {}, require: id => {
    if (id === 'vue') return vue
    if (id === 'element-plus') return {}
    if (id.endsWith('/workflow')) return {
      createControlledFileUploadSessionId: () => `client-${++sequence}`,
      uploadControlledFilePreview: async (file, purpose, uploadContext) => {
        uploads.push({ file, purpose, context: uploadContext })
        if (port.upload) return port.upload(file, purpose, uploadContext)
        return { uploadTicket: `ticket-${sequence}`, sessionId: `dcc-checkin:${uploadContext.controlledFileId}:${String(sequence).padStart(32,'a')}`,
          requestId: `request-${sequence}`, fileName: file.name, contentType: file.type, fileSize: file.size }
      },
      cleanupControlledFileUploadTicket: async (...args) => { cleanups.push(args);return port.cleanup ? port.cleanup(...args) : { cleanupStatus: 'CLEANED', bindable: false } }
    }
    throw Error(`Unexpected import: ${id}`)
  }, Error, String, Number, BigInt, Set, Promise, JSON }
  vm.runInNewContext(ts.transpileModule(script.content,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText,context)
  const component = context.exports.default
  const render = { exports: {}, require: () => vue }
  vm.runInNewContext(ts.transpileModule(template.code,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText,render)
  component.render = render.exports.render
  const props = vue.reactive({ context: { categoryId: 2, controlledFileId: '9223372036854775701', uploadContext: 'CHECKIN', sessionId: 'source-client' },
    contextKey: 'tenant1-user99-dialog1', disabled: false })
  const root = { children: [] }
  const renderer = vue.createRenderer({ createElement: type => ({ type, children: [], props: {} }), createText: text => ({ text }), createComment: text => ({ text }),
    insert: (node,parent,anchor) => {if(node.parent){const i=node.parent.children.indexOf(node);if(i>=0)node.parent.children.splice(i,1)}node.parent=parent;const i=anchor?parent.children.indexOf(anchor):-1;if(i>=0)parent.children.splice(i,0,node);else parent.children.push(node)},
    remove(node){if(node.parent){const i=node.parent.children.indexOf(node);if(i>=0)node.parent.children.splice(i,1);node.parent=null}},
    setText: (node,text) => {node.text=text}, setElementText: (node,text) => {node.text=text;node.children=[]},
    patchProp: (node,key,_old,value) => {node.props[key]=value}, parentNode:node=>node.parent,nextSibling:node=>node.parent?.children[node.parent.children.indexOf(node)+1]||null })
  const app = renderer.createApp({render:()=>vue.h(component,{...props,onState:state=>events.push(state)})})
  app.config.warnHandler=()=>{}
  for(const tag of ['el-upload','el-button','el-alert','el-table','el-table-column','el-tag']) app.component(tag,{setup:(_p,c)=>()=>vue.h(tag,c.attrs,c.slots.default?.())})
  const host=app.mount(root);await vue.nextTick()
  return { app,props,uploads,cleanups,events,root,state:host.$.subTree.component.setupState,exposed:host.$.subTree.component.exposed }
}
const file=(uid,name=`${uid}.pdf`)=>({uid,name,type:'application/pdf',size:12})
const uploadOptions=f=>({file:f,onSuccess(){},onError(){}})
test('actual component uploads two ordinary files using original CHECKIN API with independent slots and visible-order manifest',async()=>{
  const h=await mount();try{
    await h.state.uploadAttachment(uploadOptions(file(1)));await h.state.uploadAttachment(uploadOptions(file(2)))
    assert.equal(h.uploads.length,2);assert.ok(h.uploads.every(x=>x.purpose==='ATTACHMENT'&&x.context.uploadContext==='CHECKIN'))
    assert.equal(h.uploads[0].context.controlledFileId,'9223372036854775701');assert.notEqual(h.uploads[0].context.sessionId,h.uploads[1].context.sessionId)
    const manifest=h.exposed.readReadyManifest();assert.equal(manifest.length,2);assert.deepEqual(Array.from(manifest,x=>x.fileName),['1.pdf','2.pdf'])
    assert.equal(h.events.at(-1).ready,true)
  }finally{h.app.unmount()}
})
test('in-progress and failed upload block actual ready manifest with a named error',async()=>{
  let finish;const h=await mount({upload:()=>new Promise(r=>finish=r)});try{
    const pending=h.state.uploadAttachment(uploadOptions(file(1)));await vue.nextTick()
    assert.throws(()=>h.exposed.readReadyManifest(),/1.pdf|上传/)
    finish({uploadTicket:'bad-ticket',sessionId:'dcc-checkin:wrong',requestId:'r',fileName:'1.pdf',fileSize:12,contentType:'application/pdf'})
    await pending;assert.throws(()=>h.exposed.readReadyManifest(),/1.pdf|会话|上传/);assert.equal(h.events.at(-1).ready,false)
  }finally{h.app.unmount()}
})
test('formal upload failure keeps the selected filename and prevents partial-ready success',async()=>{
  const h=await mount({upload:async()=>{throw Error('Storage save failed')}});try{
    await h.state.uploadAttachment(uploadOptions(file(1)));assert.throws(()=>h.exposed.readReadyManifest(),/1.pdf|Storage/)
    assert.match(h.state.error,/Storage save failed/);assert.equal(h.state.entries.length,1)
  }finally{h.app.unmount()}
})
test('cleanup failure retains exact ticket and retry cleans it before removal',async()=>{
  let denied=true;const h=await mount({cleanup:async()=>{if(denied)throw Error('Cleanup denied');return{cleanupStatus:'CLEANED',bindable:false}}});try{
    await h.state.uploadAttachment(uploadOptions(file(1)));assert.equal(await h.state.removeAttachment({uid:1}),false)
    assert.equal(h.state.entries.length,1);assert.match(h.state.error,/Cleanup denied/);assert.throws(()=>h.exposed.readReadyManifest(),/清理|Cleanup/)
    denied=false;assert.equal(await h.state.removeAttachment({uid:1}),true);assert.equal(h.exposed.readReadyManifest().length,0)
  }finally{h.app.unmount()}
})
test('context switch clears old manifest and cleans a late response without adding it to the new version',async()=>{
  let finish;const h=await mount({upload:()=>new Promise(r=>finish=r)});try{
    const pending=h.state.uploadAttachment(uploadOptions(file(1)));h.props.context={...h.props.context,controlledFileId:'22'};await vue.nextTick()
    finish({uploadTicket:'old-ticket',sessionId:'dcc-checkin:9223372036854775701:'+'a'.repeat(32),requestId:'old-r',fileName:'1.pdf',fileSize:12,contentType:'application/pdf'})
    await pending;assert.equal(h.exposed.readReadyManifest().length,0);assert.equal(h.cleanups.length,1);assert.equal(h.cleanups[0][1],'old-ticket')
  }finally{h.app.unmount()}
})
test('saving prevents upload and removal and preserves full frozen selection',async()=>{
  const h=await mount();try{await h.state.uploadAttachment(uploadOptions(file(1)));h.props.disabled=true;await vue.nextTick()
    assert.equal(await h.state.removeAttachment({uid:1}),false);await h.state.uploadAttachment(uploadOptions(file(2)))
    assert.equal(h.uploads.length,1);assert.equal(h.exposed.readReadyManifest().length,1)
  }finally{h.app.unmount()}
})
test('missing source context and unsafe identities reject before invoking original API',async()=>{
  const h=await mount();try{for(const context of [undefined,{categoryId:2,controlledFileId:9007199254740992,uploadContext:'CHECKIN',sessionId:'source'}]){
    h.props.context=context;await vue.nextTick();await h.state.uploadAttachment(uploadOptions(file(1)));assert.equal(h.uploads.length,0);assert.equal(h.events.at(-1).ready,false)
  }}finally{h.app.unmount()}
})
test('Vue template has a real multi-file uploader whose handler is the actual upload action',async()=>{
  const h=await mount();try{
    const walk=n=>[n,...(n.children||[]).filter(Boolean).flatMap(walk)];const uploader=walk(h.root).find(n=>n.type==='el-upload')
    assert.ok(uploader);assert.equal(uploader.props.multiple,true);assert.equal(uploader.props['http-request'],h.state.uploadAttachment)
  }finally{h.app.unmount()}
})
test('duplicate formal response slots cannot form a ready manifest even if both uploads respond successfully',async()=>{
 const h=await mount({upload:async(file,_purpose,context)=>({uploadTicket:'reused-ticket',sessionId:`dcc-checkin:${context.controlledFileId}:`+'a'.repeat(32),requestId:'r',fileName:file.name,contentType:file.type,fileSize:file.size})})
 try{await h.state.uploadAttachment(uploadOptions(file(1)));await h.state.uploadAttachment(uploadOptions(file(2)));assert.throws(()=>h.exposed.readReadyManifest(),/票据|会话|重复/)}finally{h.app.unmount()}
})
test('a successful response after caller marks the selection saved does not delete a bound attachment ticket',async()=>{
 const h=await mount();try{await h.state.uploadAttachment(uploadOptions(file(1)));h.exposed.markSaved();h.app.unmount();await vue.nextTick();assert.equal(h.cleanups.length,0)}catch(e){h.app.unmount();throw e}
})
test('unmount while a checkin write is pending does not discard tickets that may be binding',async()=>{
 const h=await mount();await h.state.uploadAttachment(uploadOptions(file(1)));h.props.disabled=true;await vue.nextTick();h.app.unmount();await vue.nextTick();assert.equal(h.cleanups.length,0)
})
test('context switch while checkin save is pending does not clean possibly bound tickets',async()=>{
 const h=await mount();try{await h.state.uploadAttachment(uploadOptions(file(1)));h.props.disabled=true;h.props.contextKey='tenant1-user99-dialog2';await vue.nextTick();assert.equal(h.cleanups.length,0)}finally{h.app.unmount()}
})
test('cleanupAll uses the domain removal action for all ready files and retains the exact failed ticket until retry',async()=>{
 let failSecond=true;const h=await mount({cleanup:async(_session,ticket)=>{if(failSecond&&ticket==='ticket-2')throw Error('second cleanup unavailable');return{cleanupStatus:'CLEANED',bindable:false}}})
 try{
  await h.state.uploadAttachment(uploadOptions(file(1)));await h.state.uploadAttachment(uploadOptions(file(2)))
  assert.equal(await h.exposed.cleanupAll(),false);assert.equal(h.state.entries.length,1);assert.equal(h.state.entries[0].upload.uploadTicket,'ticket-2')
  assert.match(h.state.error,/second cleanup unavailable/);assert.throws(()=>h.exposed.readReadyManifest(),/清理|unavailable/)
  failSecond=false;assert.equal(await h.exposed.cleanupAll(),true);assert.equal(h.state.entries.length,0)
 }finally{h.app.unmount()}
})
test('cleanupAll cannot remove files while an upload or checkin save is active',async()=>{
 let finish;const h=await mount({upload:()=>new Promise(r=>finish=r)})
 try{
  const pending=h.state.uploadAttachment(uploadOptions(file(1)));assert.equal(await h.exposed.cleanupAll(),false);assert.equal(h.cleanups.length,0)
  finish({uploadTicket:'ticket-pending',sessionId:'dcc-checkin:9223372036854775701:'+'a'.repeat(32),requestId:'r',fileName:'1.pdf',contentType:'application/pdf',fileSize:12});await pending
  h.props.disabled=true;await vue.nextTick();assert.equal(await h.exposed.cleanupAll(),false);assert.equal(h.cleanups.length,0)
 }finally{h.app.unmount()}
})

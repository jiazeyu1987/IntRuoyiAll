const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),ts=require('typescript')
const existing=fs.readFileSync('tests/unit/dcc-browser-checkin-minor-only.test.cjs','utf8')
const setup=existing.slice(0,existing.indexOf("test('actual submit always"))
const {stateFor,ref}=new Function('require',setup+'\nreturn {stateFor,ref}')(require)
function host(manifest=[{uploadTicket:'attachment-a',sessionId:'dcc-checkin:9223372036854775701:'+'a'.repeat(32),fileName:'A.pdf'},
 {uploadTicket:'attachment-b',sessionId:'dcc-checkin:9223372036854775701:'+'b'.repeat(32),fileName:'B.pdf'}]){
 const h=stateFor();h.state.checkinAttachmentState=ref({ready:true,uploading:false,cleaning:false,error:'',count:manifest.length});h.state.checkinAttachmentsRef=ref({readReadyManifest:()=>manifest,markSaved:()=>{h.marked=true},cleanupAll:async()=>true});
 const write=h.state.checkinControlledFile;h.state.checkinControlledFile=async(...args)=>{const result=await write(...args);return{...result,attachments:manifest.map((item,i)=>({attachmentId:String(600+i),controlledFileId:result.id,fileName:item.fileName,fileSize:item.fileSize,contentType:item.contentType,attachmentPurpose:'ATTACHMENT',sortNo:i}))}};return h
}
test('actual browser checkin sends all ordinary attachment tickets with independent sessions and marks saved after success',async()=>{
 const h=host();await h.state.exports.submit();assert.equal(h.calls.writes.length,1)
 assert.deepEqual(JSON.parse(JSON.stringify(h.calls.writes[0][1].attachmentUploadTickets)),[
  {uploadTicket:'attachment-a',sessionId:'dcc-checkin:9223372036854775701:'+'a'.repeat(32)},
  {uploadTicket:'attachment-b',sessionId:'dcc-checkin:9223372036854775701:'+'b'.repeat(32)}])
 assert.equal(h.marked,true)
})
test('any pending or failed ordinary attachment blocks actual browser checkin before writing',async()=>{
 for(const state of [{ready:false,uploading:true,error:''},{ready:false,uploading:false,error:'B.pdf 保存失败'},{ready:false,cleaning:true,error:''}]){
  const h=host();Object.assign(h.state.checkinAttachmentState.value,state);await h.state.exports.submit();assert.equal(h.calls.writes.length,0);assert.equal(h.calls.successes.length,0)
 }
})
test('absent component or a manifest that fails final readiness is rejected explicitly with zero write',async()=>{
 for(const ref of [undefined,{readReadyManifest:()=>{throw Error('第二份未完成')}}]){
  const h=host();h.state.checkinAttachmentsRef.value=ref;await h.state.exports.submit();assert.equal(h.calls.writes.length,0)
 }
})
test('actual checkin save rejection preserves the entire ordinary selection for retry',async()=>{
 const h=host();h.state.checkinControlledFile=async()=>{throw Error('DB attachment save failed')};await h.state.exports.submit();assert.equal(h.marked,undefined)
 assert.equal(h.state.checkinDialogVisible.value,true);assert.equal(h.state.checkinAttachmentsRef.value.readReadyManifest().length,2)
})
test('successful checkin invalidates its saved attachment tickets before subsequent list read can fail',async()=>{
 const h=host();h.state.getList=async()=>{assert.equal(h.marked,true);throw Error('list unavailable')};await h.state.exports.submit();assert.equal(h.calls.writes.length,1);assert.equal(h.marked,true)
 assert.equal(h.state.checkinDialogVisible.value,false)
})
test('actual cancel awaits attachment cleanup and retains the dialog if exact-ticket cleanup fails',async()=>{
 const h=host();const source=fs.readFileSync('src/views/dcc/controlled-file/browser/index.vue','utf8');const from=source.indexOf('const cancelCheckin ='),to=source.indexOf('const beforeCloseCheckin =',from)
 h.state.clearCheckinUpload=async()=>true;h.state.exports.cancel=undefined
 vm.runInContext(ts.transpileModule(source.slice(from,to)+'\nexports.cancel=cancelCheckin',{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,h.state)
 h.state.checkinAttachmentsRef.value.cleanupAll=async()=>false;await h.state.exports.cancel();assert.equal(h.state.checkinDialogVisible.value,true);assert.equal(h.calls.writes.length,0)
 h.state.checkinAttachmentsRef.value.cleanupAll=async()=>true;await h.state.exports.cancel();assert.equal(h.state.checkinDialogVisible.value,false)
})
test('saved source response that omits ordinary attachments must never be announced as complete checkin',async()=>{
 const h=host();const write=h.state.checkinControlledFile;h.state.checkinControlledFile=async(...args)=>{const result=await write(...args);return{...result,attachments:[]}}
 await h.state.exports.submit();assert.equal(h.calls.writes.length,1);assert.equal(h.calls.successes.length,0);assert.equal(h.marked,undefined)
 assert.ok(h.calls.errors.some(x=>/附件/.test(x)))
})

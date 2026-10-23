const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),ts=require('typescript'),crypto=require('node:crypto').webcrypto
const src=fs.readFileSync('src/api/dcc/controlledFile/workflow.ts','utf8'),ast=ts.createSourceFile('workflow.ts',src,ts.ScriptTarget.Latest,true)
const names=['DccControlledFileContractError','DCC_DOWNLOAD_REQUIRED_RESPONSE_HEADERS','getAxiosHeader','assertControlledFileRequestIdentity','decodeContentDispositionFileName','assertControlledFileDownloadHeaders','createControlledFileDownloadRequestId','assertControlledFileDownloadBusinessResult','downloadControlledFileAttachmentWithName']
function host(makeResponse){
 const selected=ast.statements.filter(s=>names.includes(s.name?.text)||s.declarationList?.declarations.some(d=>names.includes(d.name?.text)))
 const c={exports:{},Blob,Response,Error,Number,String,JSON,Array,Uint8Array,BigInt,crypto,axiosConfig:{base_url:'/admin-api',request_timeout:30000},buildControlledFileBinaryHeaders:()=>({}),calls:[]}
 c.axios={get:async(url,options)=>{c.calls.push({url,options});return makeResponse(options.params.downloadRequestId)}}
 vm.runInNewContext(ts.transpileModule(selected.map(s=>s.getText(ast).replace(/^export /,'')).join('\n')+'\nthis.download=typeof downloadControlledFileAttachmentWithName==="function"?downloadControlledFileAttachmentWithName:undefined',{compilerOptions:{target:ts.ScriptTarget.ES2022,module:ts.ModuleKind.CommonJS}}).outputText,c)
 return c
}
function file(id){return{data:new Blob(['{"ok":true}'],{type:'application/json'}),headers:{'content-type':'application/json','content-disposition':'attachment; filename="notes.json"','x-dcc-access-event-code':'AE-actual','x-dcc-download-request-id':id,'x-dcc-plain-sha256':'a'.repeat(64)}}}
test('attachment download uses exact long identities and preserves actual filename/audit headers',async()=>{
 const h=host(file);assert.equal(typeof h.download,'function','formal attachment download is missing')
 const result=await h.download('9223372036854775000','9223372036854775001',{nonControlledWarningConfirmed:true})
 assert.equal(h.calls[0].url,'/admin-api/dcc/controlled-files/9223372036854775000/attachments/9223372036854775001/download')
 assert.equal(result.fileName,'notes.json');assert.equal(await result.blob.text(),'{"ok":true}');assert.equal(h.calls[0].options.params.nonControlledWarningConfirmed,true)
})
test('revoked download rejects formal JSON error, even when HTTP status is 200',async()=>{
 const h=host(()=>({data:new Blob([JSON.stringify({code:1034000,msg:'download denied'})],{type:'application/json'}),headers:{'content-type':'application/json'}}))
 assert.equal(typeof h.download,'function');await assert.rejects(()=>h.download('20','700',{nonControlledWarningConfirmed:true}),/download denied/)
})
test('invalid or lossy attachment identity never sends a request',async()=>{
 const h=host(file);assert.equal(typeof h.download,'function')
 for(const id of ['0',Number('9223372036854775001'),'../other'])await assert.rejects(()=>h.download('20',id,{nonControlledWarningConfirmed:true}))
 assert.equal(h.calls.length,0)
})

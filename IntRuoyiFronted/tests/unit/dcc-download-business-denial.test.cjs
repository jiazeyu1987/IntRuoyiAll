const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),ts=require('typescript'),crypto=require('node:crypto').webcrypto
const src=fs.readFileSync('src/api/dcc/controlledFile/workflow.ts','utf8'),ast=ts.createSourceFile('workflow.ts',src,ts.ScriptTarget.Latest,true)
const names=['DccControlledFileContractError','DCC_DOWNLOAD_REQUIRED_RESPONSE_HEADERS','getAxiosHeader','buildControlledFileDownloadEndpoint','decodeContentDispositionFileName','assertControlledFileDownloadHeaders','assertNoClientProvidedDownloadName','createControlledFileDownloadRequestId','assertControlledFileDownloadBusinessResult','downloadControlledFileWithName']
const selected=ast.statements.filter(s=>names.includes(s.name?.text)||(s.declarationList?.declarations.some(d=>names.includes(d.name?.text))))
function host(makeResponse){
 const c={Blob,Response,Error,Number,String,JSON,Array,Uint8Array,crypto,axiosConfig:{base_url:'/admin-api',request_timeout:30000},buildControlledFileBinaryHeaders:()=>({}),calls:[],axios:{get:async(url,options)=>{c.calls.push({url,options});return makeResponse(options.params.downloadRequestId)}}}
 const code=selected.map(s=>s.getText(ast).replace(/^export /,'')).join('\n')+'\nthis.download=downloadControlledFileWithName;this.headers=DCC_DOWNLOAD_REQUIRED_RESPONSE_HEADERS'
 vm.runInNewContext(ts.transpileModule(code,{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,c);return c
}
function evidence(h,id){const x={};for(const item of h.headers)x[item.name.toLowerCase()]=item.target==='downloadRequestId'?id:'actual-evidence';x['content-disposition']="attachment; filename*=UTF-8''file.pdf";return x}
test('HTTP200 JSON permission denial preserves business message, code and details instead of missing binary headers',async()=>{
 const h=host(()=>({data:new Blob([JSON.stringify({code:40301,msg:'文件下载权限已撤回',data:{action:'DOWNLOAD'}})],{type:'application/json'}),headers:{'content-type':'application/json;charset=UTF-8'}}))
 await assert.rejects(h.download('42'),e=>e.message==='文件下载权限已撤回'&&e.code===40301&&e.details.action==='DOWNLOAD');assert.equal(h.calls.length,1)
})
test('invalid JSON or a successful nonattachment JSON envelope fails explicitly rather than becoming a downloaded file',async()=>{
 for(const text of ['not-json',JSON.stringify({code:0,msg:'success'}),JSON.stringify({code:40301,msg:''})]){
  const h=host(()=>({data:new Blob([text],{type:'application/json'}),headers:{'content-type':'application/json'}}));await assert.rejects(h.download('42'),/下载错误响应/)
 }
})
test('genuine PDF keeps evidence/request-id/filename checks and exact binary bytes',async()=>{
 const blob=new Blob(['%PDF-1.7 real body'],{type:'application/pdf'});let h;h=host(id=>({data:blob,headers:{...evidence(h,id),'content-type':'application/pdf'}}));const result=await h.download('42');assert.equal(result.blob,blob);assert.equal(result.fileName,'file.pdf');assert.equal(result.evidence.downloadRequestId,h.calls[0].options.params.downloadRequestId)
})
test('genuine JSON attachment is retained as a binary source with all evidence headers',async()=>{
 const blob=new Blob([JSON.stringify({code:40301,msg:'document content is not a business error'})],{type:'application/json'});let h;h=host(id=>({data:blob,headers:{...evidence(h,id),'content-type':'application/json'}}));assert.equal((await h.download('42')).blob,blob)
})
test('binary success with missing evidence or wrong request identity remains a strict contract failure',async()=>{
 for(const kind of ['missing','mismatch']){let h;h=host(id=>({data:new Blob(['%PDF-1.7'],{type:'application/pdf'}),headers:kind==='missing'?{'content-type':'application/pdf'}:evidence(h,'another-request')}));await assert.rejects(h.download('42'),kind==='missing'?/missing required header/:/request id mismatch/)}
})

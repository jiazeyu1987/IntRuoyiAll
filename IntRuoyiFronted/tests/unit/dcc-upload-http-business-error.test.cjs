const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),ts=require('typescript')
const {AxiosError}=require('axios')
const context={exports:{},Error,require:id=>{assert.ok(id.endsWith('project-attributes/state'));return{validateAttributes:value=>value}}}
vm.runInNewContext(ts.transpileModule(fs.readFileSync('src/views/dcc/controlled-file/upload/submitter.ts','utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText,context)
const {resolveUploadPreviewErrorMessage,buildSubmitFailureFeedback}=context.exports
const generic='Request failed with status code 400'
const http=body=>new AxiosError(generic,'ERR_BAD_REQUEST',{},null,{status:400,data:body,headers:{},config:{},statusText:'Bad Request'})
test('actual AxiosError prioritizes formal response data msg over generic HTTP text in preview and submit feedback',()=>{
 const business='请求参数不正确: exactly one file is required'
 const error=http({code:400,msg:business,data:null});assert.ok(error instanceof Error)
 assert.equal(resolveUploadPreviewErrorMessage(error,'上传失败'),business)
 assert.ok(JSON.stringify(buildSubmitFailureFeedback(error,'送审失败')).includes(business))
})
test('missing or blank business msg retains the precise transport error and original other nested contracts',()=>{
 for(const body of [undefined,{}, {msg:''},{msg:'   '},{msg:null}])assert.equal(resolveUploadPreviewErrorMessage(http(body),'上传失败'),generic)
 assert.equal(resolveUploadPreviewErrorMessage(new Error('本机请求被取消'),'上传失败'),'本机请求被取消')
 assert.equal(resolveUploadPreviewErrorMessage({data:{detail:'正式附件读取失败'}},'上传失败'),'正式附件读取失败')
})
test('actual formal storage and format business messages still follow existing actionable mapping',()=>{
 const storage=resolveUploadPreviewErrorMessage(http({msg:'MinIO bucket connection refused'}),'上传失败');assert.match(storage,/文件存储服务不可用/);assert.match(storage,/MinIO bucket connection refused/)
 const format=resolveUploadPreviewErrorMessage(http({msg:'unsupported file format'}),'上传失败');assert.match(format,/文件格式不受支持/);assert.match(format,/unsupported file format/)
})
test('exact NameClaim code or known full message explains name retention and existing-file routes without number or disposal advice',()=>{
 const original='文件名称已存在，请先走作废或者升版路线'
 for(const error of [http({code:1080000348,msg:original,data:null}),new Error(original)]){
  const shown=resolveUploadPreviewErrorMessage(error,'上传失败')
  assert.match(shown,/文件名称.*占用/);assert.match(shown,/20年/);assert.match(shown,/引用/);assert.match(shown,/既有文件链.*升版/)
  assert.doesNotMatch(shown,/文件编号|调整.*编号|先走作废|先作废|原始错误/)
 }
})
test('real number and unknown duplicate messages retain their existing classification rather than fabricated NameClaim identity',()=>{
 for(const msg of ['file number already exists','文件编号已存在','duplicate upload']){
  const shown=resolveUploadPreviewErrorMessage(http({code:400,msg}),'上传失败');assert.match(shown,/文件编号已存在/);assert.doesNotMatch(shown,/20年/)
 }
})

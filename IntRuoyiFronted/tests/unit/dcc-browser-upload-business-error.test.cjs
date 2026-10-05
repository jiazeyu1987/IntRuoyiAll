const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),ts=require('typescript'),vue=require('vue')
const {AxiosError}=require('axios'),{parse}=require('vue/compiler-sfc')
const {descriptor}=parse(fs.readFileSync('src/views/dcc/controlled-file/browser/index.vue','utf8')),ast=ts.createSourceFile('browser.ts',descriptor.scriptSetup.content,ts.ScriptTarget.Latest,true)
const declaration=name=>{const n=ast.statements.find(s=>ts.isVariableStatement(s)&&s.declarationList.declarations.some(d=>d.name.text===name));assert.ok(n,name);return n.getText(ast)}
const generic='Request failed with status code 400',business='受控文件上传票据校验失败：原撤回升版正文上下文不符合要求'
const http=body=>new AxiosError(generic,'ERR_BAD_REQUEST',{},null,{status:400,data:body,config:{},headers:{},statusText:'Bad Request'})
const execute=c=>vm.runInNewContext(ts.transpileModule(declaration('resolveBrowserErrorMessage')+'\nexports.resolve=resolveBrowserErrorMessage;',{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,c)
test('actual browser resolver prefers formal business msg in a real AxiosError and retains old missing-msg semantics',()=>{
 const c={exports:{},Error};execute(c);assert.equal(c.exports.resolve(http({code:400,msg:business}),'检入上传失败'),business)
 for(const body of [undefined,{}, {msg:''},{msg:'   '},{msg:null}])assert.equal(c.exports.resolve(http(body),'检入上传失败'),generic)
 assert.equal(c.exports.resolve(new Error('当前网络不可用'),'检入上传失败'),'当前网络不可用')
 assert.equal(c.exports.resolve('实际业务失败','检入上传失败'),'实际业务失败');assert.equal(c.exports.resolve(undefined,'检入上传失败'),'检入上传失败')
})
test('actual checkin source handler preserves exact CHECKIN identity and exposes formal failure without ready or ticket success',async()=>{
 const calls=[],errors=[],failures=[],success=[],target={id:'9007199254740993',status:'WITHDRAWN'}
 const c={exports:{},Error,checkinTarget:vue.ref(target),findBrowserRowForVersion:id=>{assert.equal(id,target.id);return{categoryId:908710}},checkinUploadSessionId:vue.ref('actual-client-session'),checkinSourceRequestSequence:0,checkinSourceState:vue.ref('idle'),checkinUpload:vue.ref({uploadTicket:'old'}),checkinUploadContext:vue.ref({fileId:'old'}),checkinUploadLoading:vue.ref(false),clearCheckinDrawingPdf:()=>{},uploadControlledFilePreview:async(file,purpose,context)=>{calls.push({file,purpose,context});throw http({code:400,msg:business})},message:{error:value=>errors.push(value)}}
 execute(c);vm.runInNewContext(ts.transpileModule(declaration('uploadCheckinSource')+'\nexports.upload=uploadCheckinSource;',{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,c)
 const raw={name:'actual-source.pdf'};await c.exports.upload({file:raw,onSuccess:value=>success.push(value),onError:value=>failures.push(value)})
 assert.deepEqual(JSON.parse(JSON.stringify(calls[0].context)),{categoryId:908710,uploadContext:'CHECKIN',controlledFileId:target.id,sessionId:'actual-client-session'})
 assert.equal(calls[0].file,raw);assert.equal(calls[0].purpose,'SOURCE');assert.equal(errors[0],business)
 assert.equal(c.checkinSourceState.value,'failed');assert.equal(c.checkinUpload.value,undefined);assert.equal(c.checkinUploadContext.value,undefined);assert.equal(c.checkinUploadLoading.value,false);assert.equal(success.length,0);assert.equal(failures.length,1)
})

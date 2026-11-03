const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict'),ts=require('typescript'),vue=require('vue/compiler-sfc');
async function test(name,change,ok=true){
 const source=fs.readFileSync('src/views/system/user/'+name+'.vue','utf8');const p=vue.parse(source);assert.deepEqual(p.errors,[]);vue.compileScript(p.descriptor,{id:name});
 const a=source.indexOf('const submitForm = async'),b=source.indexOf('/**',a);let finish,count=0,calls=0,tenant=171,visit=172;const ref=value=>({value}),file={name:'a.xlsx',raw:new Blob(['data'])};
 const preview={rowCount:1,personnel:['user1'],organizations:['集团/生产']};
 const readPreview=async()=>{if(change==='previewfail')throw new Error('preview failed');return preview};
 const c={fileList:ref([file]),formLoading:ref(false),dialogVisible:ref(true),dialogGeneration:0,updateSupport:ref(true),uploadHeaders:ref(),message:{error(){}},getTenantId:()=>tenant,getVisitTenantId:()=>visit,getAccessToken:()=> 'token',nextTick:async()=>{},confirmUserImport:(filename,mode,id,range)=>{assert.equal(range,preview);count++;assert.equal(filename,'a.xlsx');assert.equal(id,172);return new Promise(r=>finish=r)},uploadRef:ref({submit(){calls++;assert.equal(c.uploadHeaders.value.Authorization,'Bearer token');assert.equal(c.uploadHeaders.value['tenant-id'],171);assert.equal(c.uploadHeaders.value['visit-tenant-id'],172);assert.equal(c.fileList.value[0],file)}}),FormData,UserApi:{importUserPreview:readPreview,importDingTalkUserPreview:readPreview,importDingTalkUsers:async data=>{calls++;assert.equal(await data.get('file').text(),'data');return {}}},submitFormSuccess(){}};
 vm.createContext(c);vm.runInContext(ts.transpile(source.slice(a,b),{target:ts.ScriptTarget.ES2022})+'\nthis.submit=submitForm',c);const pending=c.submit();if(change==='previewfail'){await assert.rejects(pending,/preview failed/);assert.equal(calls,0);assert.equal(count,0);return}await c.submit();await Promise.resolve();await Promise.resolve();if(change==='file')c.fileList.value=[{name:'b.xlsx'}];if(change==='tenant')tenant=2;if(change==='visit')visit=4;if(change==='close'){c.dialogVisible.value=false;c.dialogGeneration++}finish(ok);await pending;assert.equal(count,1);assert.equal(calls,ok&&!change?1:0);
}
(async()=>{for(const n of ['UserImportForm','UserDingTalkImportForm']){await test(n,null,false);for(const m of [null,'file','tenant','close'])await test(n,m);await test(n,'visit');await test(n,'previewfail');console.log(n+': actual handler cancellation, snapshots, duplicate guard and SFC PASS')}})().catch(e=>{console.error(e);process.exitCode=1});
async function testPreviewEnvelopeAndRendering() {
 const data={rowCount:2,personnel:['alice','bob'],organizations:['集团/生产']};
 const api=fs.readFileSync('src/api/system/user/index.ts','utf8');
 const code=api.slice(api.indexOf('interface UserImportPreviewResponse'));
 const c={exports:{},request:{upload:async()=>({code:0,msg:'',data})},Error};vm.createContext(c);
 vm.runInContext(ts.transpile(code,{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}),c);
 assert.equal(await c.exports.importUserPreview(new FormData()),data);
 assert.equal(await c.exports.importDingTalkUserPreview(new FormData()),data);
 c.request.upload=async()=>({code:0,msg:''});
 await assert.rejects(c.exports.importUserPreview(new FormData()),/./);
 const helper=fs.readFileSync('src/views/system/user/userImportConfirmation.ts','utf8');
 let rendered='';const h=(tag,props,children)=>{const value=children===undefined?props:children;return Array.isArray(value)?value.join(''):String(value??'')};
 const d={exports:{},ElMessageBox:{confirm:async node=>{rendered=node}},h,useUserStore:()=>({getUser:{username:'admin',deptId:9}})};vm.createContext(d);
 const helperCode=helper.replace(/^import .*$/gm,'');
 vm.runInContext(ts.transpile(helperCode,{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}),d);
 assert.equal(await d.exports.confirmUserImport('a.xlsx','create',172,data),true);
 for(const text of ['2','alice','bob','集团/生产','172'])assert.ok(rendered.includes(text));
 console.log('Preview API envelope extraction, missing data rejection and actual confirmation render PASS');
}
testPreviewEnvelopeAndRendering().catch(e=>{console.error(e);process.exitCode=1});

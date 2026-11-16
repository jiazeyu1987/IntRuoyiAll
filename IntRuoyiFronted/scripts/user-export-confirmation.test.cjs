const fs=require('fs'),assert=require('node:assert/strict'),vm=require('vm'),ts=require('typescript'),vue=require('vue/compiler-sfc');
async function run(generic,change,cancel=false){
 const s=fs.readFileSync('src/views/system/user/index.vue','utf8');const p=vue.parse(s);assert.deepEqual(p.errors,[]);vue.compileScript(p.descriptor,{id:'user'});
 const code=s.slice(s.indexOf('const handleUserExport ='),s.indexOf('const handleAdvancedCommand'));
 let finish,calls=0,downloads=0,tenant=171;const scope={active:true,ids:Array.from({length:125},(_,i)=>i+1),state:'applied'};
 const c={exportLoading:{value:false},queryParams:{pageNo:3,pageSize:10,deptId:5},userListTemplateRef:{value:{getExportScope:()=>scope}},getTenantId:()=>tenant,getVisitTenantId:()=>172,getAccessToken:()=> 'token',message:{confirm:()=>new Promise((r,j)=>finish=()=>cancel?j('cancel'):r()),error(){}},UserApi:{exportUser:async params=>{calls++;assert.equal(params.ids.length,125);assert.equal(params.pageNo,undefined);assert.equal(params.deptId,5);return 'blob'},exportGenericAccountUsers:async()=>{calls++;return 'blob'}},download:{excel:()=>downloads++}};
 if(change==='empty')scope.ids=[];
 vm.createContext(c);vm.runInContext(ts.transpile(code,{target:ts.ScriptTarget.ES2022})+'\nthis.run=handleUserExport',c);let task=c.run(generic);if(change==='empty'){await assert.rejects(task,/为空/);assert.equal(calls,0);return}await c.run(generic);if(change==='tenant')tenant=173;if(change==='filter')c.queryParams.deptId=6;finish();if(change && (!generic||change==='tenant'))await assert.rejects(task,/变化/);else await task;assert.equal(calls,cancel||change&&(!generic||change==='tenant')?0:1);assert.equal(downloads,calls);
}
(async()=>{for(const generic of [false,true]){await run(generic,null);await run(generic,null,true);await run(generic,'tenant');await run(generic,'filter');if(!generic)await run(generic,'empty')}console.log('Actual export handler: full filtered IDs, cancel, double click, scope/tenant guard PASS')})().catch(e=>{console.error(e);process.exitCode=1});

;(async()=>{
 const api=fs.readFileSync('src/api/system/user/index.ts','utf8');
 const a=api.indexOf('export const exportUser ='),b=api.indexOf('// 下载用户导入模板',a);
 const context={exports:{},request:{download:async config=>config}};vm.createContext(context);
 vm.runInContext(ts.transpile(api.slice(a,b),{module:ts.ModuleKind.CommonJS}),context);
 const params={ids:Array.from({length:5000},(_,i)=>i+1)};
 const normal=await context.exports.exportUser(params);assert.equal(normal.method,'POST');assert.equal(normal.data,params);assert.equal(normal.params,undefined);
 const generic=await context.exports.exportGenericAccountUsers();assert.equal(generic.method,undefined);assert.equal(generic.data,undefined);
 console.log('Actual API: large ID export uses JSON POST; generic retains default GET PASS');
})().catch(e=>{console.error(e);process.exitCode=1});
async function pendingExportContextTest(generic, mutation) {
 const source=fs.readFileSync('src/views/system/user/index.vue','utf8');
 const code=source.slice(source.indexOf('const handleUserExport ='),source.indexOf('const handleAdvancedCommand'));
 let release,tenant=171,visit=172,token='original',downloads=0,errors=[];
 const scope={active:true,ids:[1,2],state:'original'};
 const c={exportLoading:{value:false},queryParams:{deptId:5},userListTemplateRef:{value:{getExportScope:()=>scope}},getTenantId:()=>tenant,getVisitTenantId:()=>visit,getAccessToken:()=>token,message:{confirm:async()=>{},error:m=>errors.push(m)},UserApi:{exportUser:()=>new Promise(r=>release=r),exportGenericAccountUsers:()=>new Promise(r=>release=r)},download:{excel:()=>downloads++}};
 vm.createContext(c);vm.runInContext(ts.transpile(code,{target:ts.ScriptTarget.ES2022})+'\nthis.run=handleUserExport',c);
 const pending=c.run(generic);await new Promise(resolve=>setImmediate(resolve));assert.equal(typeof release,'function');
 if(mutation==='tenant')tenant=173;
 if(mutation==='visit')visit=174;
 if(mutation==='token')token='changed';
 if(mutation==='filter')c.queryParams.deptId=8;
 if(mutation==='scope')scope.state='changed';
 release('blob');
 if(mutation){await assert.rejects(pending,/变化/);assert.equal(downloads,0);assert.equal(errors.length,1)}
 else {await pending;assert.equal(downloads,1)}
}
;(async()=>{for(const generic of [false,true])for(const mutation of [null,'tenant','visit','token',...(!generic?['filter','scope']:[])])await pendingExportContextTest(generic,mutation);console.log('Actual pending export response context races: tenant/visit/login/filter/scope zero download PASS')})().catch(e=>{console.error(e);process.exitCode=1});

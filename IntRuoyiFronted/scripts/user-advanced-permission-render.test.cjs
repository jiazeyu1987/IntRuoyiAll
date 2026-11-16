const fs=require('node:fs'),assert=require('node:assert/strict'),vm=require('node:vm'),ts=require('typescript'),Vue=require('vue'),{parse,compileScript,compileTemplate}=require('vue/compiler-sfc'),{renderToString}=require('vue/server-renderer');
(async()=>{
 const source=fs.readFileSync('src/views/system/user/index.vue','utf8'),parsed=parse(source);assert.deepEqual(parsed.errors,[]);compileScript(parsed.descriptor,{id:'user-page'});
 const start=source.indexOf('<el-dropdown'),end=source.indexOf('</el-dropdown>',start)+'</el-dropdown>'.length;
 const result=compileTemplate({source:source.slice(start,end),filename:'index.vue',id:'permission-test'});assert.deepEqual(result.errors,[]);
 const mod={exports:{},require:n=>{assert.equal(n,'vue');return Vue}};vm.createContext(mod);vm.runInContext(ts.transpile(result.code,{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}),mod);
 const auth=fs.readFileSync('src/directives/permission/hasPermi.ts','utf8');const fn=auth.slice(auth.indexOf('export const hasPermission'));
 const components={ElDropdown:{setup:(_,c)=>()=>Vue.h('section',null,[c.slots.default?.(),c.slots.dropdown?.()])},ElDropdownMenu:{setup:(_,c)=>()=>Vue.h('ul',null,c.slots.default?.())},ElDropdownItem:{props:['command','disabled'],setup:(p,c)=>()=>Vue.h(Vue.Fragment,null,[Vue.h('li',{'data-command':p.command},c.slots.default?.()),Vue.h('span')])},ElButton:{setup:(_,c)=>()=>Vue.h('button',null,c.slots.default?.())},Icon:{render:()=>null}};
 const expected={exportUsers:'system:user:export',exportGeneric:'system:user:export',create:'system:user:create',import:'system:user:import',dingTalkImport:'system:user:import',deleteBatch:'system:user:delete',deleteDept:'system:dept:delete'};
 for(const permissions of [[],['system:user:export'],['system:user:create'],['system:user:import'],['system:user:delete','system:dept:delete'],Object.values(expected),['*:*:*'],['unrelated']]){
  const authContext={exports:{},userStore:{permissions:new Set(permissions)},all_permission:'*:*:*'};vm.createContext(authContext);vm.runInContext(ts.transpile(fn,{module:ts.ModuleKind.CommonJS}),authContext);
  const app=Vue.createSSRApp({components,data:()=>({selectedDeptId:1,exportLoading:false}),methods:{checkPermi:p=>authContext.exports.hasPermission(p),handleAdvancedCommand(){}},render:mod.exports.render});let warnings=[];app.config.warnHandler=m=>warnings.push(m);const html=await renderToString(app);
  const permitted=Object.entries(expected).filter(([,p])=>permissions.includes(p)||permissions.includes('*:*:*')).map(([k])=>k);
  assert.equal(html.includes('<section'),permitted.length>0);
  for(const command of Object.keys(expected))assert.equal(html.includes('data-command="'+command+'"'),permitted.includes(command),permissions+' '+command);
  assert.deepEqual(warnings,[]);
 }
 console.log('Actual advanced template compiled/rendered with Fragment menu items: deny/allow/mixed/super permissions PASS');
})().catch(e=>{console.error(e);process.exitCode=1});



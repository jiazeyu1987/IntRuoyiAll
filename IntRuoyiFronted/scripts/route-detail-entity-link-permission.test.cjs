const fs=require('fs'),assert=require('node:assert/strict'),vm=require('node:vm'),ts=require('typescript')
const c=require(require.resolve('@vue/compiler-sfc',{paths:[require.resolve('vue')]}))
const d=c.parse(fs.readFileSync('src/views/mes/pro/route/RouteFlowGraphDesigner.vue','utf8')).descriptor
c.compileScript(d,{id:'route'})
assert.deepEqual(c.compileTemplate({source:d.template.content,filename:'route.vue',id:'route'}).errors,[])
assert(d.template.content.includes('<span v-else>{{ link.label }}</span>'))
assert(d.template.content.includes('!link.permission || checkPermi([link.permission])'))
const script=ts.transpile(d.scriptSetup.content,{target:ts.ScriptTarget.ES2022})
const extract=name=>{const start=script.indexOf('const '+name+' ='),end=script.indexOf('\n};',start);assert(start>=0&&end>start);return script.slice(start,end+3)}
;(async()=>{
for(const name of ['openProcessTargetLink','openWorkstationTargetLink','openMachineryTargetLink','handleProcessDetailLinkClick']){
 const ctx={checkPermi:()=>false};vm.createContext(ctx);vm.runInContext(extract(name)+'\nglobalThis.run='+name,ctx)
 await ctx.run({permission:'mes:dv-machinery:query',onClick(){throw Error('unauthorized callback')}})
}
for(const [name,args,permission] of [['buildSelectedProcessTargetLinks',["processCode","P1"],'mes:pro-process:query'],['buildWorkstationTargetLink',[{workstationId:1}],'mes:md-workstation:query'],['buildWorkstationMachineryLinks',[{machineryList:[{machineryId:2,machineryCode:'D2'}]}],'mes:dv-machinery:query']]){
const ctx={buildProcessDetailTextLink:(key,label,onClick,permission)=>[{key,label,onClick,permission}],formatProcessDetailText:x=>x,formatRouteProcessWorkstation:()=> 'W1',formatMachineryTargetLabel:x=>x.machineryCode};vm.createContext(ctx);vm.runInContext(extract(name)+'\nglobalThis.run='+name,ctx);assert.equal(ctx.run(...args)[0].permission,permission)
}
console.log('PASS route link permission metadata, denied handlers, retained labels and Vue compilation')
})().catch(e=>{console.error(e);process.exitCode=1})

const fs=require('fs'),assert=require('node:assert/strict'),vm=require('node:vm'),ts=require('typescript')
const compiler=require(require.resolve('@vue/compiler-sfc',{paths:[require.resolve('vue')]}))
const source=fs.readFileSync('src/views/mes/md/workstation/index.vue','utf8'), d=compiler.parse(source).descriptor
compiler.compileScript(d,{id:'workstation'})
assert.deepEqual(compiler.compileTemplate({source:d.template.content,filename:'workstation.vue',id:'workstation'}).errors,[])
for(const field of ['machineryCode','machineryName']) assert(source.includes('<span v-else>{{ scope.row.'+field+' }}</span>'))
assert.equal((source.match(/v-if="checkPermi\(\['mes:dv-machinery:query'\]\)" type="primary" @click="openMachineryLedger/g)||[]).length,2)
const script=ts.transpile(d.scriptSetup.content,{target:ts.ScriptTarget.ES2022})
const extract=name=>{const start=script.indexOf('const '+name+' ='),end=script.indexOf('\n};',start);assert(start>=0&&end>start);return script.slice(start,end+3)}
;(async()=>{
let allowed=false; const calls=[]
const ctx={checkPermi:()=>allowed,pendingMachineryLedgerId:{value:undefined},workstationMachineDialogVisible:{value:true},router:{push:x=>calls.push(x)}}
vm.createContext(ctx);vm.runInContext(extract('openMachineryLedger')+'\n'+extract('handleWorkstationMachineDialogClosed')+'\nglobalThis.open=openMachineryLedger;globalThis.closed=handleWorkstationMachineDialogClosed;',ctx)
await ctx.open(7);assert.equal(ctx.workstationMachineDialogVisible.value,true);assert.equal(ctx.pendingMachineryLedgerId.value,undefined)
allowed=true;await ctx.open(7);assert.equal(ctx.workstationMachineDialogVisible.value,false);assert.equal(calls.length,0)
allowed=false;await ctx.closed();assert.equal(calls.length,0);assert.equal(ctx.pendingMachineryLedgerId.value,undefined)
allowed=true;await ctx.open(7);await ctx.closed();assert.equal(calls.length,1);assert.equal(calls[0].path,'/mes/dv/machinery');assert.equal(calls[0].query.openId,'7')
await ctx.closed();assert.equal(calls.length,1);await assert.rejects(ctx.open(0),/设备绑定缺少设备编号/)
console.log('PASS machinery text branches, denied click, revoked permission, deferred navigation, invalid ID and Vue compilation')
})().catch(e=>{console.error(e);process.exitCode=1})

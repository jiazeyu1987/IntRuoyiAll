const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript } = require('vue/compiler-sfc')
const {descriptor}=parse(fs.readFileSync('src/views/dcc/controlled-file/shared/ControlledFileBasicInfoPanel.vue','utf8'))
const ast=ts.createSourceFile('panel.ts',descriptor.scriptSetup.content,ts.ScriptTarget.Latest,true)
const helper=ast.statements.find(n=>ts.isVariableStatement(n)&&n.declarationList.declarations.some(d=>ts.isIdentifier(d.name)&&d.name.text==='formatDccProjectCodeLink'))
assert.ok(helper)
const c={exports:{},String}
vm.runInNewContext(ts.transpileModule(helper.getText(ast)+'\nexports.format=formatDccProjectCodeLink',{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,c)
const format=c.exports.format
const find=(n,label)=>{if(n.type===1&&n.tag==='el-descriptions-item'&&n.props.some(p=>p.type===6&&p.name==='label'&&p.value?.content===label))return n;for(const child of n.children||[]){const found=find(child,label);if(found)return found}}
function render(label,file){
 const node=find(descriptor.template.ast,label);assert.ok(node,'real field '+label)
 const events=[]
 const compiled=compileScript(parse('<template>'+node.loc.source+'</template><script setup>const file=__host.file;const formatDccProjectCodeLink=__host.format;const emit=__host.emit;</script>').descriptor,{id:'actual-preview-facts',inlineTemplate:true})
 const host={exports:{},require:()=>vue,__host:{file,format,emit:(...args)=>events.push(args)}}
 vm.runInNewContext(ts.transpileModule(compiled.content,{compilerOptions:{module:ts.ModuleKind.CommonJS}}).outputText,host)
 const renderer=vue.createRenderer({createElement:type=>({type,children:[],props:{}}),createText:text=>({text}),createComment:()=>({}),insert:(n,p)=>p.children.push(n),remove(){},setText:(n,t)=>{n.text=t},setElementText:(n,t)=>{n.text=t},patchProp:(n,k,_old,v)=>{n.props[k]=v},parentNode:()=>null,nextSibling:()=>null})
 const app=renderer.createApp(host.exports.default)
 app.component('el-descriptions-item',{setup:(_p,ctx)=>()=>vue.h('section',ctx.attrs,ctx.slots.default?.())})
 app.component('el-link',{setup:(_p,ctx)=>()=>vue.h('a',ctx.attrs,ctx.slots.default?.())})
 const root={children:[]};app.mount(root)
 const text=n=>(n.text||'')+(n.children||[]).map(text).join('')
 const anchor=n=>n.type==='a'?n:(n.children||[]).map(anchor).find(Boolean)
 return {text:text(root),click:()=>anchor(root)?.props.onClick(),events,close:()=>app.unmount()}
}
const file={dccProjectCodeId:'9007199254740993',projectName:'正式项目甲',projectCode:'PROJECT-A',productName:'实际产品乙',productCode:'PRODUCT-B',versionNo:'A/2',status:'CONTROLLED_PENDING_EFFECTIVE',currentActiveVersionNo:'A/1'}
test('actual preview project helper uses formal project facts and keeps unbound or missing facts explicit',()=>{
 assert.equal(format(file),'正式项目甲 / PROJECT-A')
 assert.equal(format({...file,dccProjectCodeId:null,projectName:null,projectCode:null}),'未绑定 DCC 项目代码')
 assert.equal(format({...file,projectName:null,projectCode:null}),'项目名称及编码未记录')
})
test('actual preview project link renders formal project and navigates by the original Long ID',()=>{
 const r=render('DCC基础条目',file)
 try{assert.equal(r.text,'正式项目甲 / PROJECT-A');r.click();assert.equal(r.events[0][0],'openDccProjectCode');assert.equal(r.events[0][1],'9007199254740993')}finally{r.close()}
 const old=render('DCC基础条目',{...file,dccProjectCodeId:null,projectName:null,projectCode:null})
 try{assert.equal(old.text,'未绑定 DCC 项目代码');assert.equal(old.events.length,0)}finally{old.close()}
})
test('actual preview execution field labels old A/1 as execution while selected A/2 remains independent',()=>{
 const r=render('当前执行受控版本',file)
 try{assert.equal(r.text,'A/1')}finally{r.close()}
 assert.equal(file.versionNo,'A/2')
})
test('actual original product-number template still displays actual product rather than project code',()=>{
 const r=render('产品编号',file)
 try{assert.equal(r.text,'PRODUCT-B')}finally{r.close()}
})

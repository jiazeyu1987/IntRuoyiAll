const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript } = require('vue/compiler-sfc')
const {descriptor}=parse(fs.readFileSync('src/views/dcc/controlled-file/detail/index.vue','utf8'))
const ast=ts.createSourceFile('detail.ts',descriptor.scriptSetup.content,ts.ScriptTarget.Latest,true)
const declarations=['formatDetailPath','currentDccProjectCodeText'].map(name=>{
  const st=ast.statements.find(n=>ts.isVariableStatement(n)&&n.declarationList.declarations.some(d=>ts.isIdentifier(d.name)&&d.name.text===name));assert.ok(st);return st.getText(ast)
}).join('\n')
const projectText=file=>{
  const c={exports:{},computed:vue.computed,fileDetail:vue.ref(file)}
  vm.runInNewContext(ts.transpileModule(declarations+'\nexports.value=currentDccProjectCodeText.value',{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,c)
  return c.exports.value
}
test('actual DCC project computed reads formal project name and code independently of actual product fields',()=>{
  assert.equal(projectText({projectName:'正式项目甲',projectCode:'PROJECT-A',productName:'实际产品乙',productCode:'PRODUCT-B'}),'正式项目甲 / PROJECT-A')
})
test('actual historical unbound project never borrows product name or code and keeps existing empty wording',()=>{
  assert.equal(projectText({projectName:null,projectCode:null,productName:'产品乙',productCode:'PRODUCT-B'}),'-')
  assert.equal(projectText({projectName:'正式项目甲',projectCode:null,productName:'产品乙'}),'正式项目甲')
})
test('actual DCC project descriptions subtree renders project facts while original product and navigation identity remain',()=>{
  const find=n=>{if(n.type===1&&n.tag==='el-descriptions-item'&&n.props.some(p=>p.type===6&&p.name==='label'&&p.value?.content==='DCC 项目'))return n;for(const child of n.children||[]){const found=find(child);if(found)return found}}
  const node=find(descriptor.template.ast);assert.ok(node)
  const compiled=compileScript(parse('<template>'+node.loc.source+'</template><script setup>const currentDccProjectCodeText=__value;</script>').descriptor,{id:'actual-project-facts',inlineTemplate:true})
  const c={exports:{},require:()=>vue,__value:projectText({dccProjectCodeId:'271',projectName:'正式项目甲',projectCode:'PROJECT-A',productName:'产品乙',productCode:'PRODUCT-B'})}
  vm.runInNewContext(ts.transpileModule(compiled.content,{compilerOptions:{module:ts.ModuleKind.CommonJS}}).outputText,c)
  const renderer=vue.createRenderer({createElement:type=>({type,children:[]}),createText:text=>({text}),createComment:()=>({}),insert:(n,p)=>p.children.push(n),remove(){},setText:(n,t)=>{n.text=t},setElementText:(n,t)=>{n.text=t},patchProp(){},parentNode:()=>null,nextSibling:()=>null})
  const app=renderer.createApp(c.exports.default);app.component('el-descriptions-item',{setup:(_p,ctx)=>()=>vue.h('section',ctx.slots.default?.())});const root={children:[]};app.mount(root)
  const text=n=>(n.text||'')+(n.children||[]).map(text).join('')
  try{assert.equal(text(root),'正式项目甲 / PROJECT-A')}finally{app.unmount()}
  assert.ok(descriptor.template.content.includes("fileDetail?.productCode || '-'") )
  assert.ok(descriptor.template.content.includes('openDccProjectCode(fileDetail.dccProjectCodeId)'))
  assert.ok(descriptor.template.content.includes('未绑定 DCC 项目代码'))
})

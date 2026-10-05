const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),ts=require('typescript'),vue=require('vue')
const {parse,compileScript}=require('vue/compiler-sfc')
const {descriptor}=parse(fs.readFileSync('src/views/dcc/controlled-file/detail/index.vue','utf8'))
const ast=ts.createSourceFile('detail.ts',descriptor.scriptSetup.content,ts.ScriptTarget.Latest,true)
const st=ast.statements.find(n=>ts.isVariableStatement(n)&&n.declarationList.declarations.some(d=>ts.isIdentifier(d.name)&&d.name.text==='formatStageSignatureStatus'));assert.ok(st)
function host(){const c={exports:{},String,approvalProgressScope:vue.ref({fileId:'10',bpmRound:'round',applicationType:'UPLOAD'}),approvalProcessInstanceId:vue.ref('round'),isWorkingBrowserDetailCurrent:vue.ref(true),fileDetail:vue.ref({id:'10',needTraining:true,trainingRecordAvailable:false}),getStageSignatures:()=>[]};vm.runInNewContext(ts.transpileModule(st.getText(ast)+'\nexports.format=formatStageSignatureStatus',{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,c);return{c,format:c.exports.format}}
const stage=stageCode=>({stageCode,isCompleted:true,isCurrent:false})
test('actual native record-stage formatter shows upload/control/distribution facts instead of invented signature waiting',()=>{
 const h=host();assert.equal(h.format(stage('APPLICANT_TRAINING_RECORD')),'等待文控上传线下培训记录');assert.equal(h.format(stage('CONTROLLED')),'等待生成受控记录');assert.equal(h.format(stage('DISTRIBUTED')),'等待文控下发')
 h.c.fileDetail.value={...h.c.fileDetail.value,trainingRecordAvailable:true,controlledTime:'2026-10-05 11:51:36',distributedTime:'2026-10-05 12:11:29'}
 assert.equal(h.format(stage('APPLICANT_TRAINING_RECORD')),'线下培训记录已上传');assert.equal(h.format(stage('CONTROLLED')),'受控记录已生成');assert.equal(h.format(stage('DISTRIBUTED')),'下发记录已保存')
})
test('actual unknown or mismatched native context does not report record completion',()=>{
 const h=host();h.c.fileDetail.value.controlledTime='2026-10-05 11:51:36';h.c.approvalProgressScope.value=undefined;assert.equal(h.format(stage('CONTROLLED')),'阶段证据未记录')
 h.c.approvalProgressScope.value={fileId:'11',bpmRound:'round',applicationType:'UPLOAD'};assert.equal(h.format(stage('CONTROLLED')),'阶段证据未记录')
})
test('actual approval and legacy stages retain their existing signature-count and waiting behavior',()=>{
 const h=host();assert.equal(h.format(stage('MATRIX_APPROVAL')),'已完成，签名证据待同步');h.c.getStageSignatures=()=>[{},{}];assert.equal(h.format(stage('MATRIX_REVIEW')),'2 条签名证据')
 h.c.getStageSignatures=()=>[];h.c.approvalProgressScope.value={fileId:'10',bpmRound:'round',applicationType:'LEGACY'};assert.equal(h.format({stageCode:'DOC_CONTROL_APPROVAL',isCompleted:false,isCurrent:true}),'待当前节点签名')
})
test('actual stage-grid metadata labels record evidence without implying an extra signing node',()=>{
 const h=host();h.c.fileDetail.value.controlledTime='2026-10-05 11:51:36'
 const find=n=>{if(n.type===1&&n.props.some(p=>p.type===6&&p.name==='class'&&p.value?.content==='stage-card__meta'))return n;for(const child of n.children||[]){const found=find(child);if(found)return found}}
 const node=find(descriptor.template.ast);assert.ok(node)
 const compiled=compileScript(parse('<template>'+node.loc.source+'</template><script setup>const stage=__host.stage;const formatStageSignatureStatus=__host.format;const formatStageProgressActors=()=>"";const formatStageProgressTime=()=>"";</script>').descriptor,{id:'native-stage-evidence',inlineTemplate:true})
 const c={exports:{},require:()=>vue,__host:{stage:stage('CONTROLLED'),format:h.format}};vm.runInNewContext(ts.transpileModule(compiled.content,{compilerOptions:{module:ts.ModuleKind.CommonJS}}).outputText,c)
 const renderer=vue.createRenderer({createElement:type=>({type,children:[]}),createText:text=>({text}),createComment:()=>({}),insert:(n,p)=>p.children.push(n),remove(){},setText:(n,t)=>{n.text=t},setElementText:(n,t)=>{n.text=t},patchProp(){},parentNode:()=>null,nextSibling:()=>null})
 const app=renderer.createApp(c.exports.default),root={children:[]};app.mount(root);const text=n=>(n.text||'')+(n.children||[]).map(text).join('')
 try{const shown=text(root);assert.ok(shown.includes('阶段证据：受控记录已生成'));assert.equal(shown.includes('签名证据待同步'),false)}finally{app.unmount()}
})
